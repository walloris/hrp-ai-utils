#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# GigaCode Notify Plugin
# Зависимости: python3 (встроен в macOS), osascript
# ──────────────────────────────────────────────────────────────────────────────

set -uo pipefail

INPUT=$(cat)

json_get() {
  local json="$1"
  local key="$2"
  local default="${3:-}"
  python3 -c "
import sys, json
try:
    data = json.loads(sys.argv[1])
    val = data.get('$key')
    if val is None:
        print('$default')
    else:
        print(str(val).lower() if isinstance(val, bool) else str(val))
except Exception:
    print('$default')
" "$json" 2>/dev/null || echo "$default"
}

HOOK_EVENT=$(json_get "$INPUT" "hook_event_name" "Unknown")
AGENT_ID=$(json_get "$INPUT" "agent_id")
AGENT_TYPE=$(json_get "$INPUT" "agent_type")
TOOL_NAME=$(json_get "$INPUT" "tool_name")
LAST_MSG=$(json_get "$INPUT" "last_assistant_message" | head -c 120)

if [[ -n "$AGENT_ID" ]]; then
  AGENT_LABEL="Agent #${AGENT_ID:0:6}"
else
  AGENT_LABEL="GigaCode"
fi

notify() {
  local title="$1" subtitle="$2" message="$3" sound="${4:-default}"
  /usr/bin/osascript \
    -e 'on run argv' \
    -e 'display notification (item 3 of argv) with title (item 1 of argv) subtitle (item 2 of argv) sound name (item 4 of argv)' \
    -e 'end run' \
    -- "$title" "$subtitle" "$message" "$sound"
}

ring_bell() { printf '\a' > /dev/tty 2>/dev/null || true; }

tool_label() { [[ -n "$TOOL_NAME" ]] && echo " — $TOOL_NAME"; }

case "$HOOK_EVENT" in
  "PreToolUse")
    REQUIRES_APPROVAL=$(json_get "$INPUT" "requires_approval" "false")
    if [[ "$REQUIRES_APPROVAL" == "true" ]]; then
      ring_bell
      notify "⏳ Требуется подтверждение" "${AGENT_LABEL}$(tool_label)" \
             "Агент ждёт вашего апрува. Вернитесь в терминал." "Ping"
    fi ;;
  "PermissionRequest")
    ring_bell
    notify "⏳ Требуется подтверждение" "${AGENT_LABEL}$(tool_label)" \
           "Агент ждёт вашего апрува. Вернитесь в терминал." "Ping" ;;
  "Stop")
    SHORT_MSG="${LAST_MSG:0:80}"; [[ ${#LAST_MSG} -gt 80 ]] && SHORT_MSG="${SHORT_MSG}…"
    ring_bell
    notify "✅ Агент завершил работу" "$AGENT_LABEL" "${SHORT_MSG:-Задача выполнена}" "Blow" ;;
  "SubagentStop")
    SHORT_MSG="${LAST_MSG:0:80}"; [[ ${#LAST_MSG} -gt 80 ]] && SHORT_MSG="${SHORT_MSG}…"
    SUBTITLE="$AGENT_LABEL"; [[ -n "$AGENT_TYPE" ]] && SUBTITLE="${AGENT_LABEL} (${AGENT_TYPE})"
    ring_bell
    notify "✅ Субагент завершил задачу" "$SUBTITLE" "${SHORT_MSG:-Подзадача выполнена}" "Blow" ;;
  "StopFailure")
    ERROR_TYPE=$(json_get "$INPUT" "error" "unknown")
    ERROR_DETAIL=$(json_get "$INPUT" "error_details" | head -c 100)
    ring_bell
    notify "❌ Ошибка агента" "${AGENT_LABEL} (${ERROR_TYPE})" \
           "${ERROR_DETAIL:-Неизвестная ошибка}" "Basso" ;;
esac

exit 0