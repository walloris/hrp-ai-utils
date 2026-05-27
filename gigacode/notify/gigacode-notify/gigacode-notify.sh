#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# GigaCode Notify Plugin v1.2.0
# macOS notification hook for gigacode-cli (fork of qwen-code)
# Зависимости: python3 (встроен в macOS), osascript
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
DEBUG_LOG="${GIGACODE_NOTIFY_LOG:-}"

# ── Read stdin ────────────────────────────────────────────────────────────────
INPUT=$(cat)

# ── Debug log ─────────────────────────────────────────────────────────────────
if [[ -n "$DEBUG_LOG" ]]; then
  echo "$(date '+%H:%M:%S') RAW: $INPUT" >> "$DEBUG_LOG"
fi

# json_get <json> <key> [default]
# Извлекает строковое или булево значение по ключу верхнего уровня.
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
        # bool -> lowercase string, всё остальное -> str
        print(str(val).lower() if isinstance(val, bool) else str(val))
except Exception:
    print('$default')
" "$json"
}

# ── Parse JSON ────────────────────────────────────────────────────────────────
HOOK_EVENT=$(json_get "$INPUT" "hook_event_name" "Unknown")

if [[ -n "$DEBUG_LOG" ]]; then
  echo "$(date '+%H:%M:%S') EVENT: $HOOK_EVENT" >> "$DEBUG_LOG"
fi

AGENT_ID=$(json_get "$INPUT" "agent_id")
AGENT_TYPE=$(json_get "$INPUT" "agent_type")
TOOL_NAME=$(json_get "$INPUT" "tool_name")
NOTIF_TYPE=$(json_get "$INPUT" "notification_type")
NOTIF_MSG=$(json_get "$INPUT" "message")
LAST_MSG=$(json_get "$INPUT" "last_assistant_message" | head -c 120)

# ── Agent label ───────────────────────────────────────────────────────────────
if [[ -n "$AGENT_ID" ]]; then
  AGENT_LABEL="Agent #${AGENT_ID:0:6}"
else
  AGENT_LABEL="GigaCode"
fi

# ── Notification helper ───────────────────────────────────────────────────────
notify() {
  local title="$1"
  local subtitle="$2"
  local message="$3"
  local sound="${4:-default}"

  title="${title//\"/\\\"}"
  subtitle="${subtitle//\"/\\\"}"
  message="${message//\"/\\\"}"

  /usr/bin/osascript \
    -e "display notification \"$message\" with title \"$title\" subtitle \"$subtitle\" sound name \"$sound\""
}

# ── Tool label helper ─────────────────────────────────────────────────────────
tool_label() {
  if [[ -n "$TOOL_NAME" ]]; then
    echo " — $TOOL_NAME"
  fi
}

# ── Event handlers ────────────────────────────────────────────────────────────
case "$HOOK_EVENT" in

  "PreToolUse")
    REQUIRES_APPROVAL=$(json_get "$INPUT" "requires_approval" "false")
    if [[ "$REQUIRES_APPROVAL" == "true" ]]; then
      TOOL_INFO=$(tool_label)
      notify "⏳ Требуется подтверждение" "${AGENT_LABEL}${TOOL_INFO}" \
             "Агент ждёт вашего апрува. Вернитесь в терминал." "Tink"
    fi
    ;;

  "PermissionRequest")
    TOOL_INFO=$(tool_label)
    notify "⏳ Требуется подтверждение" "${AGENT_LABEL}${TOOL_INFO}" \
           "Агент ждёт вашего апрува. Вернитесь в терминал." "Tink"
    ;;

  "Stop")
    SHORT_MSG="${LAST_MSG:0:80}"
    [[ ${#LAST_MSG} -gt 80 ]] && SHORT_MSG="${SHORT_MSG}…"
    notify "✅ Агент завершил работу" "$AGENT_LABEL" \
           "${SHORT_MSG:-Задача выполнена}" "Blow"
    ;;

  "SubagentStop")
    SHORT_MSG="${LAST_MSG:0:80}"
    [[ ${#LAST_MSG} -gt 80 ]] && SHORT_MSG="${SHORT_MSG}…"
    SUBTITLE="$AGENT_LABEL"
    [[ -n "$AGENT_TYPE" ]] && SUBTITLE="${AGENT_LABEL} (${AGENT_TYPE})"
    notify "✅ Субагент завершил задачу" "$SUBTITLE" \
           "${SHORT_MSG:-Подзадача выполнена}" "Blow"
    ;;

  "StopFailure")
    ERROR_TYPE=$(json_get "$INPUT" "error" "unknown")
    ERROR_DETAIL=$(json_get "$INPUT" "error_details" | head -c 100)
    notify "❌ Ошибка агента" "${AGENT_LABEL} (${ERROR_TYPE})" \
           "${ERROR_DETAIL:-Неизвестная ошибка}" "Basso"
    ;;

esac

exit 0