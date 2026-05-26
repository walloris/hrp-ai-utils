#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# GigaCode Notify Plugin v1.1.0
# macOS notification hook for gigacode-cli (fork of qwen-code)
#
# GitHub: https://github.com/YOUR_USERNAME/gigacode-notify
# Install: ./install.sh
# Docs:    README.md
#
# Hook events supported:
#   PreToolUse, PostToolUse, Notification, PermissionRequest,
#   Stop, SubagentStop, StopFailure, SessionEnd
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

# ── Config ───────────────────────────────────────────────────────────────────
DEBUG_LOG="${GIGACODE_NOTIFY_LOG:-}"           # задайте путь для дебаг-лога

# ── Read stdin ────────────────────────────────────────────────────────────────
INPUT=$(cat)

# ── Debug log (опционально) ───────────────────────────────────────────────────
if [[ -n "$DEBUG_LOG" ]]; then
  echo "$(date '+%H:%M:%S') RAW: $INPUT" >> "$DEBUG_LOG"
fi

# ── Parse JSON ────────────────────────────────────────────────────────────────
HOOK_EVENT=$(echo "$INPUT" | jq -r '.hook_event_name // "Unknown"')

if [[ -n "$DEBUG_LOG" ]]; then
  echo "$(date '+%H:%M:%S') EVENT: $HOOK_EVENT" >> "$DEBUG_LOG"
fi

AGENT_ID=$(echo "$INPUT" | jq -r '.agent_id // ""')
AGENT_TYPE=$(echo "$INPUT" | jq -r '.agent_type // ""')
TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // ""')
NOTIF_TYPE=$(echo "$INPUT" | jq -r '.notification_type // ""')
NOTIF_MSG=$(echo "$INPUT" | jq -r '.message // ""')
LAST_MSG=$(echo "$INPUT" | jq -r '.last_assistant_message // ""' | head -c 120)

# ── Agent label ───────────────────────────────────────────────────────────────
if [[ -n "$AGENT_ID" ]]; then
  AGENT_LABEL="Agent #${AGENT_ID:0:6}"
else
  AGENT_LABEL="GigaCode"
fi

# ── Notification helper ───────────────────────────────────────────────────────
# notify <title> <subtitle> <message> [sound]
notify() {
  local title="$1"
  local subtitle="$2"
  local message="$3"
  local sound="${4:-default}"

  # Экранируем кавычки для osascript
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
    REQUIRES_APPROVAL=$(echo "$INPUT" | jq -r '.requires_approval // false')
    if [[ "$REQUIRES_APPROVAL" == "true" ]]; then
      TOOL_INFO=$(tool_label)
      notify "⏳ Требуется подтверждение" "${AGENT_LABEL}${TOOL_INFO}" \
             "Агент ждёт вашего апрува. Вернитесь в терминал." "Ping"
    fi
    ;;

  "PermissionRequest")
    TOOL_INFO=$(tool_label)
    notify "⏳ Требуется подтверждение" "${AGENT_LABEL}${TOOL_INFO}" \
           "Агент ждёт вашего апрува. Вернитесь в терминал." "Ping"
    ;;

  "Stop")
    SHORT_MSG="${LAST_MSG:0:80}"
    [[ ${#LAST_MSG} -gt 80 ]] && SHORT_MSG="${SHORT_MSG}…"
    notify "✅ Агент завершил работу" "$AGENT_LABEL" \
           "${SHORT_MSG:-Задача выполнена}" "Glass"
    ;;

  "SubagentStop")
    SHORT_MSG="${LAST_MSG:0:80}"
    [[ ${#LAST_MSG} -gt 80 ]] && SHORT_MSG="${SHORT_MSG}…"
    SUBTITLE="$AGENT_LABEL"
    [[ -n "$AGENT_TYPE" ]] && SUBTITLE="${AGENT_LABEL} (${AGENT_TYPE})"
    notify "✅ Субагент завершил задачу" "$SUBTITLE" \
           "${SHORT_MSG:-Подзадача выполнена}" "Bottle"
    ;;

  "StopFailure")
    ERROR_TYPE=$(echo "$INPUT" | jq -r '.error // "unknown"')
    ERROR_DETAIL=$(echo "$INPUT" | jq -r '.error_details // ""' | head -c 100)
    notify "❌ Ошибка агента" "${AGENT_LABEL} (${ERROR_TYPE})" \
           "${ERROR_DETAIL:-Неизвестная ошибка}" "Basso"
    ;;

esac

# Всегда выходим с кодом 0 — не блокируем работу агента
exit 0
