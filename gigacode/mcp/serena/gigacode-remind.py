#!/usr/bin/env python3
"""
remind.py — GigaCode hook: блокирует повторные low-level tool calls,
направляя агента к Serena MCP инструментам.

Счётчики (invariant):
  read_count   — растёт при read_tool, сбрасывается при search_tool или non-low-level
  search_count — растёт при search_tool, сбрасывается при read_tool или non-low-level
  mixed_count  — растёт при любом low-level tool, сбрасывается при non-low-level или serena
  Смысл: read и search — взаимоисключающие серии; mixed считает суммарную длину серии.
"""

import fcntl
import json
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta, timezone
from typing import Optional


# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------

STATE_DIR = Path(os.path.expanduser("~/.gigacode/hooks"))
LOG_FILE  = STATE_DIR / "remind.log"


def _session_file(session_id: str) -> Path:
    """Один файл на сессию — исключает O(n) перезапись и конкуренцию между сессиями."""
    safe = session_id.replace("/", "_").replace("\\", "_")[:64]
    return STATE_DIR / f"session_{safe}.json"


# ---------------------------------------------------------------------------
# Tool classification
# ---------------------------------------------------------------------------

READ_TOOLS:   set[str] = {"read_file", "list_directory"}
SEARCH_TOOLS: set[str] = {"grep", "search", "glob"}
SERENA_TOOL_PREFIXES: tuple[str, ...] = ("mcp__serena__",)


def is_serena_tool(name: str) -> bool:
    return any(name.startswith(p) for p in SERENA_TOOL_PREFIXES)


def is_read_tool(name: str) -> bool:
    return name in READ_TOOLS


def is_search_tool(name: str) -> bool:
    return name in SEARCH_TOOLS


def is_low_level_tool(name: str) -> bool:
    return is_read_tool(name) or is_search_tool(name)


# ---------------------------------------------------------------------------
# Thresholds (env-configurable)
# ---------------------------------------------------------------------------

READ_THRESHOLD   = int(os.getenv("GIGACODE_REMIND_READ_THRESHOLD",   "3"))
SEARCH_THRESHOLD = int(os.getenv("GIGACODE_REMIND_SEARCH_THRESHOLD", "3"))
MIXED_THRESHOLD  = int(os.getenv("GIGACODE_REMIND_MIXED_THRESHOLD",  "4"))
COOLDOWN_SECONDS = int(os.getenv("GIGACODE_REMIND_COOLDOWN_SECONDS", "120"))
RESET_MINUTES    = int(os.getenv("GIGACODE_REMIND_RESET_MINUTES",    "15"))
ENABLE_LOG = os.getenv("GIGACODE_REMIND_LOG", "0") == "1"


# ---------------------------------------------------------------------------
# Skip rules (расширяемые через env)
# ---------------------------------------------------------------------------

SKIP_FILENAMES: set[str] = {
    "pom.xml", "docker-compose.yml", "docker-compose.yaml",
    "compose.yml", "compose.yaml",
}

SKIP_PREFIXES: tuple[str, ...] = (
    ".gigacode/memory/", ".idea/", ".git/",
)

SKIP_EXTENSIONS: set[str] = {
    ".md", ".txt", ".yaml", ".yml", ".properties",
    ".json", ".toml", ".xml", ".lock", ".env",
}

CODE_EXTENSIONS: set[str] = {
    ".kt", ".java", ".groovy",
    ".py",
    ".js", ".jsx", ".mjs", ".cjs",
    ".ts", ".tsx",
    ".go", ".rs",
    ".cpp", ".cc", ".c", ".h", ".hpp",
    ".cs", ".php", ".rb", ".swift", ".scala", ".sql",
}

# GIGACODE_REMIND_EXTRA_CODE_EXT=.vue,.svelte
_extra_code = os.getenv("GIGACODE_REMIND_EXTRA_CODE_EXT", "")
if _extra_code:
    CODE_EXTENSIONS |= {e.strip() for e in _extra_code.split(",") if e.strip()}

# GIGACODE_REMIND_EXTRA_SKIP_EXT=.graphql,.proto
_extra_skip = os.getenv("GIGACODE_REMIND_EXTRA_SKIP_EXT", "")
if _extra_skip:
    SKIP_EXTENSIONS |= {e.strip() for e in _extra_skip.split(",") if e.strip()}


# ---------------------------------------------------------------------------
# Deny messages — единое место для всех текстов
# ---------------------------------------------------------------------------

def _build_read_context(file_path: str) -> str:
    symbol_hint = Path(file_path).stem if file_path else "the target symbol"
    rel_hint    = file_path or "<relative_path_to_file>"
    return (
        "You are repeatedly reading source files directly. "
        "Do NOT call ReadFile again for this code exploration task.\n"
        "Use Serena MCP tools instead, choosing one of these real tools:\n"
        f"- mcp__serena__get_symbols_overview with relative_path set to {rel_hint}, "
        "to inspect top-level symbols in the file\n"
        f"- mcp__serena__find_symbol with name_path_pattern set to {symbol_hint} and include_body=true, "
        "to read that symbol body directly\n"
        "- mcp__serena__search_for_pattern if you do not know the exact symbol name\n"
        "There is no Serena read_file tool. "
        "Do not invent tool names such as mcp__serena__read_file."
    )


_DENY_SEARCH = (
    "Too many consecutive grep/search calls without using symbolic tools.",
    "You are repeatedly using search-like tools. Switch to Serena symbolic search tools, "
    "such as mcp__serena__search_for_pattern or mcp__serena__find_symbol, "
    "or use a more targeted exploration strategy.",
)
_DENY_MIXED = (
    "Too many consecutive low-level code exploration tool calls without using symbolic tools.",
    "You have been alternating between low-level read/search tools for code exploration. "
    "Use Serena semantic navigation (find_symbol, get_symbols_overview, find_referencing_symbols) "
    "instead of more ReadFile/Glob.",
)
_DENY_READ_REASON = (
    "Too many consecutive code-file read calls. "
    "ReadFile is blocked for this task; use Serena tools get_symbols_overview or find_symbol instead."
)


def _check_thresholds(sess: dict, file_path: str) -> Optional[tuple[str, str]]:
    """Возвращает (reason, additional_context) если счётчик превысил порог, иначе None."""
    if sess["read_count"] >= READ_THRESHOLD:
        return _DENY_READ_REASON, _build_read_context(file_path)
    if sess["search_count"] >= SEARCH_THRESHOLD:
        return _DENY_SEARCH
    if sess["mixed_count"] >= MIXED_THRESHOLD:
        return _DENY_MIXED
    return None


# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------

def log(message: str) -> None:
    if not ENABLE_LOG:
        return
    try:
        STATE_DIR.mkdir(parents=True, exist_ok=True)
        with LOG_FILE.open("a", encoding="utf-8") as f:
            f.write(f"{_now().isoformat()} {message}\n")
    except Exception:
        pass


# ---------------------------------------------------------------------------
# Time helpers
# ---------------------------------------------------------------------------

def _now() -> datetime:
    return datetime.now(timezone.utc)


def _parse_dt(value: Optional[str]) -> Optional[datetime]:
    if not value:
        return None
    try:
        dt = datetime.fromisoformat(value)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)  # совместимость со старыми naive записями
        return dt
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Session state (per-file, with exclusive lock)
# ---------------------------------------------------------------------------

def _empty_session() -> dict:
    return {
        "read_count":   0,
        "search_count": 0,
        "mixed_count":  0,
        "last_seen_at": None,
        "last_deny_at": None,
    }


def _reset_counts(sess: dict) -> None:
    sess["read_count"]   = 0
    sess["search_count"] = 0
    sess["mixed_count"]  = 0


def _load_session(session_id: str) -> dict:
    path = _session_file(session_id)
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return _empty_session()


def _save_session(session_id: str, sess: dict) -> None:
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    path = _session_file(session_id)
    tmp  = path.with_suffix(".tmp")
    try:
        with tmp.open("w", encoding="utf-8") as f:
            json.dump(sess, f)
        tmp.replace(path)
    except Exception:
        pass


class _SessionLock:
    """Эксклюзивная блокировка файла сессии на время read-modify-write."""

    def __init__(self, session_id: str) -> None:
        self._lock_path = _session_file(session_id).with_suffix(".lock")
        self._fh = None

    def __enter__(self) -> "_SessionLock":
        STATE_DIR.mkdir(parents=True, exist_ok=True)
        self._fh = self._lock_path.open("a")
        fcntl.flock(self._fh, fcntl.LOCK_EX)
        return self

    def __exit__(self, *_) -> None:
        if self._fh:
            fcntl.flock(self._fh, fcntl.LOCK_UN)
            self._fh.close()


# ---------------------------------------------------------------------------
# File path helpers
# ---------------------------------------------------------------------------

def _extract_file_path(payload: dict) -> str:
    tool_input = payload.get("tool_input") or payload.get("toolInput") or {}
    if isinstance(tool_input, dict):
        return str(
            tool_input.get("file_path")
            or tool_input.get("filePath")
            or tool_input.get("path")
            or ""
        ).strip()
    return ""


def _should_skip_read(file_path: str, cwd: Optional[str]) -> bool:
    if not file_path:
        return True  # пустой path — нечего анализировать

    try:
        p   = Path(file_path)
        rel = (str(p.relative_to(cwd)) if cwd else str(p)).replace("\\", "/")
    except Exception:
        rel = file_path

    name   = Path(file_path).name.lower()
    suffix = Path(file_path).suffix.lower()

    if any(rel.startswith(prefix) for prefix in SKIP_PREFIXES):
        return True
    if name in SKIP_FILENAMES:
        return True
    if name.startswith("readme") or name.startswith("application.") or name.startswith("application-"):
        return True
    if suffix in SKIP_EXTENSIONS and suffix not in CODE_EXTENSIONS:
        return True

    return False


def _is_code_read(file_path: str) -> bool:
    return bool(file_path) and Path(file_path).suffix.lower() in CODE_EXTENSIONS


# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------

def _deny(reason: str, additional_context: str) -> None:
    payload = {
        "hookSpecificOutput": {
            "hookEventName":            "PreToolUse",
            "permissionDecision":       "deny",
            "permissionDecisionReason": reason,
            "additionalContext":        additional_context,
        }
    }
    sys.stdout.write(json.dumps(payload))
    sys.stdout.flush()


# ---------------------------------------------------------------------------
# Input parsing
# ---------------------------------------------------------------------------

def _parse_input() -> Optional[dict]:
    try:
        raw = sys.stdin.read()
    except (IOError, OSError):
        return None
    if not raw.strip():
        return None
    try:
        return json.loads(raw)
    except Exception:
        log("invalid-json")
        return None


# ---------------------------------------------------------------------------
# Session update helpers
# ---------------------------------------------------------------------------

def _apply_idle_reset(sess: dict, now: datetime) -> bool:
    last_seen = _parse_dt(sess.get("last_seen_at"))
    if last_seen and now - last_seen > timedelta(minutes=RESET_MINUTES):
        _reset_counts(sess)
        return True
    return False


def _update_counters(sess: dict, tool_name: str) -> None:
    """Инкрементирует нужный счётчик и сбрасывает взаимоисключающий."""
    if is_read_tool(tool_name):
        sess["read_count"]   += 1
        sess["search_count"]  = 0   # серия read сбрасывает серию search
        sess["mixed_count"]  += 1
    elif is_search_tool(tool_name):
        sess["search_count"] += 1
        sess["read_count"]    = 0   # серия search сбрасывает серию read
        sess["mixed_count"]  += 1


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> int:
    payload = _parse_input()
    if payload is None:
        return 0

    session_id = str(payload.get("session_id") or payload.get("sessionId") or "").strip()
    tool_name  = str(payload.get("tool_name")  or payload.get("toolName")  or "").strip()
    cwd        = str(payload.get("cwd") or "").strip() or None
    file_path  = _extract_file_path(payload)

    if not session_id or not tool_name:
        log(f"skip missing-fields session_id={session_id!r} tool_name={tool_name!r}")
        return 0

    with _SessionLock(session_id):
        sess = _load_session(session_id)
        now  = _now()

        if _apply_idle_reset(sess, now):
            log(f"reset session={session_id} reason=idle-timeout")

        sess["last_seen_at"] = now.isoformat()

        if is_serena_tool(tool_name):
            _reset_counts(sess)
            sess["last_deny_at"] = None
            _save_session(session_id, sess)
            log(f"reset session={session_id} reason=serena-tool tool={tool_name}")
            return 0

        if is_read_tool(tool_name):
            if tool_name == "read_file":
                if _should_skip_read(file_path, cwd):
                    _save_session(session_id, sess)
                    log(f"skip-read session={session_id} tool={tool_name} file={file_path}")
                    return 0
                if not _is_code_read(file_path):
                    _save_session(session_id, sess)
                    log(f"skip-noncode-read session={session_id} tool={tool_name} file={file_path}")
                    return 0
            # list_directory: пропускаем _is_code_read, идём дальше к _update_counters

        if not is_low_level_tool(tool_name):
            _reset_counts(sess)
            _save_session(session_id, sess)
            log(f"reset session={session_id} reason=non-low-level tool={tool_name}")
            return 0

        _update_counters(sess, tool_name)
        log(
            f"count session={session_id} tool={tool_name} "
            f"read={sess['read_count']} search={sess['search_count']} "
            f"mixed={sess['mixed_count']} file={file_path}"
        )

        threshold_result = _check_thresholds(sess, file_path)
        if threshold_result:
            reason, additional_context = threshold_result
            last_deny   = _parse_dt(sess.get("last_deny_at"))
            in_cooldown = last_deny and (now - last_deny).total_seconds() < COOLDOWN_SECONDS

            sess["last_deny_at"] = now.isoformat()
            _save_session(session_id, sess)

            ctx = "See previous message for guidance." if in_cooldown else additional_context
            _deny(reason, ctx)
            log(f"deny session={session_id} tool={tool_name} cooldown={bool(in_cooldown)}")
            return 0

        _save_session(session_id, sess)
        return 0


if __name__ == "__main__":
    sys.exit(main())