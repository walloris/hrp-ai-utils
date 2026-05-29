#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta


STATE_DIR = Path(os.path.expanduser("~/.gigacode/hooks"))
STATE_FILE = STATE_DIR / "remind_state.json"
LOG_FILE = STATE_DIR / "remind.log"

READ_TOOLS = {
    "read_file",
    "ReadFile",
}

SEARCH_TOOLS = {
    "grep",
    "search",
    "SearchInFiles",
    "FindInFiles",
    "Glob",
}

SERENA_TOOL_PREFIXES = (
    "mcp__serena__",
    "serena__",
)

READ_THRESHOLD = int(os.getenv("GIGACODE_REMIND_READ_THRESHOLD", "3"))
SEARCH_THRESHOLD = int(os.getenv("GIGACODE_REMIND_SEARCH_THRESHOLD", "3"))
MIXED_THRESHOLD = int(os.getenv("GIGACODE_REMIND_MIXED_THRESHOLD", "4"))
COOLDOWN_SECONDS = int(os.getenv("GIGACODE_REMIND_COOLDOWN_SECONDS", "120"))
RESET_MINUTES = int(os.getenv("GIGACODE_REMIND_RESET_MINUTES", "15"))
ENABLE_LOG = os.getenv("GIGACODE_REMIND_LOG", "1") == "1"

SKIP_FILENAMES = {
    "pom.xml",
    "docker-compose.yml",
    "docker-compose.yaml",
    "compose.yml",
    "compose.yaml",
}

SKIP_PREFIXES = (
    ".gigacode/memory/",
    ".idea/",
    ".git/",
)

SKIP_EXTENSIONS = {
    ".md",
    ".txt",
    ".yaml",
    ".yml",
    ".properties",
    ".json",
    ".toml",
    ".xml",
    ".lock",
    ".env",
}

CODE_EXTENSIONS = {
    ".kt", ".java", ".groovy",
    ".py",
    ".js", ".jsx", ".mjs", ".cjs",
    ".ts", ".tsx",
    ".go",
    ".rs",
    ".cpp", ".cc", ".c", ".h", ".hpp",
    ".cs",
    ".php",
    ".rb",
    ".swift",
    ".scala",
    ".sql",
}


def utcnow():
    return datetime.utcnow()


def parse_dt(value: str | None):
    if not value:
        return None
    try:
        return datetime.fromisoformat(value)
    except Exception:
        return None


def log(message: str):
    if not ENABLE_LOG:
        return
    try:
        STATE_DIR.mkdir(parents=True, exist_ok=True)
        with LOG_FILE.open("a", encoding="utf-8") as f:
            f.write(f"{utcnow().isoformat()} {message}\n")
    except Exception:
        pass


def load_state():
    try:
        with STATE_FILE.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}


def save_state(state):
    try:
        STATE_DIR.mkdir(parents=True, exist_ok=True)
        tmp = STATE_FILE.with_suffix(".tmp")
        with tmp.open("w", encoding="utf-8") as f:
            json.dump(state, f)
        tmp.replace(STATE_FILE)
    except Exception:
        pass


def empty_session_state():
    return {
        "read_count": 0,
        "search_count": 0,
        "mixed_count": 0,
        "last_seen_at": None,
        "last_deny_at": None,
        # сюда можно будет добавить blocked_files, если захочешь per-file hard block
    }


def is_serena_tool(tool_name: str) -> bool:
    return any(tool_name.startswith(prefix) for prefix in SERENA_TOOL_PREFIXES)


def is_read_tool(tool_name: str) -> bool:
    return tool_name in READ_TOOLS


def is_search_tool(tool_name: str) -> bool:
    return tool_name in SEARCH_TOOLS


def is_low_level_tool(tool_name: str) -> bool:
    return is_read_tool(tool_name) or is_search_tool(tool_name)


def reset_counts(sess: dict):
    sess["read_count"] = 0
    sess["search_count"] = 0
    sess["mixed_count"] = 0


def deny(reason: str, additional_context: str):
    payload = {
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
            "additionalContext": additional_context,
        }
    }
    sys.stdout.write(json.dumps(payload))
    sys.stdout.flush()


def normalize_rel_path(file_path: str, cwd: str | None):
    try:
        p = Path(file_path)
        if cwd:
            cwd_path = Path(cwd)
            try:
                return str(p.relative_to(cwd_path))
            except Exception:
                return str(p)
        return str(p)
    except Exception:
        return file_path or ""


def should_skip_read(file_path: str, cwd: str | None) -> bool:
    if not file_path:
        return False

    rel = normalize_rel_path(file_path, cwd).replace("\\", "/")
    name = Path(file_path).name.lower()
    suffix = Path(file_path).suffix.lower()

    if any(rel.startswith(prefix) for prefix in SKIP_PREFIXES):
        return True

    if name in SKIP_FILENAMES:
        return True

    if name.startswith("readme"):
        return True

    if name.startswith("application.") or name.startswith("application-"):
        return True

    if suffix in SKIP_EXTENSIONS and suffix not in CODE_EXTENSIONS:
        return True

    return False


def is_code_read(file_path: str) -> bool:
    if not file_path:
        return False
    suffix = Path(file_path).suffix.lower()
    return suffix in CODE_EXTENSIONS


def extract_file_path(payload: dict) -> str:
    tool_input = payload.get("tool_input") or payload.get("toolInput") or {}
    if isinstance(tool_input, dict):
        return str(
            tool_input.get("file_path")
            or tool_input.get("filePath")
            or ""
        ).strip()
    return ""


def build_read_context(file_path: str | None) -> str:
    # Для подсказки имени символа возьмём stem файла
    symbol_hint = Path(file_path).stem if file_path else "the target symbol"
    rel_hint = file_path or "<relative_path_to_file>"

    return (
        "You are repeatedly reading source files directly. "
        "Do NOT call ReadFile again for this code exploration task.\n"
        "Use Serena MCP tools instead, choosing one of these real tools:\n"
        f"- mcp__serena__get_symbols_overview with relative_path set to {rel_hint}, "
        "to inspect top-level symbols in the file\n"
        f"- mcp__serena__find_symbol with name_path_pattern set to {symbol_hint} and include_body=true, "
        "to read that symbol body directly\n"
        "- mcp__serena__search_for_pattern if you do not know the exact symbol name\n"
        "There is no Serena read_file tool. Do not invent tool names such as mcp__serena__read_file."
    )


def main():
    raw = sys.stdin.read()
    if not raw.strip():
        return 0

    try:
        payload = json.loads(raw)
    except Exception:
        log("invalid-json")
        return 0

    session_id = str(payload.get("session_id") or payload.get("sessionId") or "").strip()
    tool_name = str(payload.get("tool_name") or payload.get("toolName") or "").strip()
    cwd = str(payload.get("cwd") or "").strip() or None
    file_path = extract_file_path(payload)

    if not session_id or not tool_name:
        log(f"skip missing-fields session_id={session_id!r} tool_name={tool_name!r}")
        return 0

    state = load_state()
    sess = state.get(session_id, empty_session_state())
    now = utcnow()

    last_seen_at = parse_dt(sess.get("last_seen_at"))
    last_deny_at = parse_dt(sess.get("last_deny_at"))

    # Idle reset
    if last_seen_at and now - last_seen_at > timedelta(minutes=RESET_MINUTES):
        reset_counts(sess)
        log(f"reset session={session_id} reason=idle-timeout")

    sess["last_seen_at"] = now.isoformat()

    # Полный reset на любом инструменте Serena
    if is_serena_tool(tool_name):
        reset_counts(sess)
        sess["last_deny_at"] = None
        state[session_id] = sess
        save_state(state)
        log(f"reset session={session_id} reason=serena-tool tool={tool_name}")
        return 0

    # Skip non-code / config reads
    if is_read_tool(tool_name):
        if should_skip_read(file_path, cwd):
            state[session_id] = sess
            save_state(state)
            log(f"skip-read session={session_id} tool={tool_name} file={file_path}")
            return 0

        if not is_code_read(file_path):
            state[session_id] = sess
            save_state(state)
            log(f"skip-noncode-read session={session_id} tool={tool_name} file={file_path}")
            return 0

    # Non-low-level tools полностью сбрасывают счётчики
    if not is_low_level_tool(tool_name):
        reset_counts(sess)
        state[session_id] = sess
        save_state(state)
        log(f"reset session={session_id} reason=non-low-level tool={tool_name}")
        return 0

    # Проверяем, уже ли мы в состоянии "over threshold"
    over_read = sess["read_count"] >= READ_THRESHOLD
    over_search = sess["search_count"] >= SEARCH_THRESHOLD
    over_mixed = sess["mixed_count"] >= MIXED_THRESHOLD

    if over_read or over_search or over_mixed:
        # cooldown управляет только тем, хотим ли мы спамить длинным текстом,
        # но deny остаётся deny
        in_cooldown = last_deny_at and (now - last_deny_at).total_seconds() < COOLDOWN_SECONDS

        reason = None
        additional_context = None

        if over_read:
            reason = (
                "Too many consecutive code-file read calls. "
                "ReadFile is blocked for this task; use Serena tools get_symbols_overview or find_symbol instead."
            )
            additional_context = build_read_context(file_path)
        elif over_search:
            reason = "Too many consecutive grep/search calls without using symbolic tools."
            additional_context = (
                "You are repeatedly using search-like tools. Switch to Serena symbolic search tools, "
                "such as mcp__serena__search_for_pattern or mcp__serena__find_symbol, "
                "or use a more targeted exploration strategy."
            )
        elif over_mixed:
            reason = "Too many consecutive low-level code exploration tool calls without using symbolic tools."
            additional_context = (
                "You have been alternating between low-level read/search tools for code exploration. "
                "Use Serena semantic navigation (find_symbol, get_symbols_overview, find_referencing_symbols) "
                "instead of more ReadFile/Glob."
            )

        sess["last_deny_at"] = now.isoformat()
        state[session_id] = sess
        save_state(state)
        log(
            f"deny session={session_id} tool={tool_name} "
            f"over_threshold=True cooldown={bool(in_cooldown)}"
        )
        deny(reason, additional_context)
        return 0

    # --- Normal counting (under threshold) ---
    if is_read_tool(tool_name):
        sess["read_count"] += 1
        sess["search_count"] = 0
        sess["mixed_count"] += 1
        log(
            f"count session={session_id} tool={tool_name} "
            f"read={sess['read_count']} search={sess['search_count']} mixed={sess['mixed_count']} file={file_path}"
        )
    elif is_search_tool(tool_name):
        sess["search_count"] += 1
        sess["read_count"] = 0
        sess["mixed_count"] += 1
        log(
            f"count session={session_id} tool={tool_name} "
            f"read={sess['read_count']} search={sess['search_count']} mixed={sess['mixed_count']}"
        )

    # Проверяем, не ударили ли порог только что
    reason = None
    additional_context = None

    if sess["read_count"] >= READ_THRESHOLD:
        reason = (
            "Too many consecutive code-file read calls. "
            "ReadFile is blocked for this task; use Serena tools get_symbols_overview or find_symbol instead."
        )
        additional_context = build_read_context(file_path)
    elif sess["search_count"] >= SEARCH_THRESHOLD:
        reason = "Too many consecutive grep/search calls without using symbolic tools."
        additional_context = (
            "You are repeatedly using search-like tools. Switch to Serena symbolic search tools, "
            "such as mcp__serena__search_for_pattern or mcp__serena__find_symbol, "
            "or use a more targeted exploration strategy."
        )
    elif sess["mixed_count"] >= MIXED_THRESHOLD:
        reason = "Too many consecutive low-level code exploration tool calls without using symbolic tools."
        additional_context = (
            "You have been alternating between low-level read/search tools for code exploration. "
            "Use Serena semantic navigation (find_symbol, get_symbols_overview, find_referencing_symbols) "
            "instead of more ReadFile/Glob."
        )

    if reason:
        sess["last_deny_at"] = now.isoformat()
        # Важно: НЕ сбрасываем счётчики, чтобы последующие попытки тоже блокировались
        state[session_id] = sess
        save_state(state)
        log(f"deny session={session_id} tool={tool_name} just_hit_threshold")
        deny(reason, additional_context)
        return 0

    state[session_id] = sess
    save_state(state)
    return 0


if __name__ == "__main__":
    sys.exit(main())