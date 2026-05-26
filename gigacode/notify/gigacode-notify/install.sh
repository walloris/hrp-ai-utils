#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# GigaCode Notify Plugin — Installer
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOK_SCRIPT="$SCRIPT_DIR/gigacode-notify.sh"

# ── Проверяем зависимости ─────────────────────────────────────────────────────
for dep in jq osascript; do
  if ! command -v "$dep" &>/dev/null; then
    echo "❌ Зависимость не найдена: $dep"
    [[ "$dep" == "jq" ]] && echo "   Установите: brew install jq"
    exit 1
  fi
done
echo "✅ Зависимости в порядке (jq, osascript)"

# ── Режим установки ───────────────────────────────────────────────────────────
SETTINGS_DIR="$HOME/.gigacode"
SETTINGS_FILE="$HOME/.gigacode/settings.json"
echo "📁 Устанавливаем глобально: $SETTINGS_FILE"

mkdir -p "$SETTINGS_DIR"

# ── Конфиг хуков ─────────────────────────────────────────────────────────────
HOOK_CONFIG=$(cat << JSON
{
  "PreToolUse": [
    {
      "matcher": "*",
      "hooks": [
        {
          "type": "command",
          "command": "$HOOK_SCRIPT",
          "name": "gigacode-notify-pretool",
          "async": true,
          "timeout": 5000
        }
      ]
    }
  ],
  "PermissionRequest": [
    {
      "matcher": "*",
      "hooks": [
        {
          "type": "command",
          "command": "$HOOK_SCRIPT",
          "name": "gigacode-notify-permission",
          "async": true,
          "timeout": 5000
        }
      ]
    }
  ],
  "Stop": [
    {
      "matcher": "",
      "hooks": [
        {
          "type": "command",
          "command": "$HOOK_SCRIPT",
          "name": "gigacode-notify-stop",
          "async": true,
          "timeout": 5000
        }
      ]
    }
  ],
  "SubagentStop": [
    {
      "matcher": "*",
      "hooks": [
        {
          "type": "command",
          "command": "$HOOK_SCRIPT",
          "name": "gigacode-notify-subagent-stop",
          "async": true,
          "timeout": 5000
        }
      ]
    }
  ],
  "StopFailure": [
    {
      "matcher": "*",
      "hooks": [
        {
          "type": "command",
          "command": "$HOOK_SCRIPT",
          "name": "gigacode-notify-failure",
          "async": true,
          "timeout": 5000
        }
      ]
    }
  ]
}
JSON
)

# ── Мержим с существующим settings.json ───────────────────────────────────────
if [[ -f "$SETTINGS_FILE" ]]; then
  echo "🔄 Найден существующий $SETTINGS_FILE, мержим hooks..."
  EXISTING=$(cat "$SETTINGS_FILE")
  if ! echo "$EXISTING" | jq empty 2>/dev/null; then
    echo "⚠️  settings.json невалиден! Создаём резервную копию..."
    cp "$SETTINGS_FILE" "${SETTINGS_FILE}.backup.$(date +%s)"
    EXISTING="{}"
  fi
  echo "$EXISTING" | jq --argjson hooks "$HOOK_CONFIG" '. + {hooks: $hooks}' > "$SETTINGS_FILE"
else
  echo "📝 Создаём новый $SETTINGS_FILE..."
  echo "{\"hooks\": $HOOK_CONFIG}" | jq . > "$SETTINGS_FILE"
fi

echo ""
echo "✅ Установка завершена!"
echo ""
echo "📄 Настройки: $SETTINGS_FILE"
echo "🔨 Скрипт:    $HOOK_SCRIPT"
echo ""
echo "⚙️  Дебаг-лог (опционально, добавь в ~/.zshrc):"
echo "   export GIGACODE_NOTIFY_LOG=/tmp/gigacode-notify-debug.log"
echo ""
echo "🚀 Запускай gigacode с флагом --experimental-hooks"
echo "   Или добавь алиас в ~/.zshrc:"
echo "   alias gigacode='gigacode --experimental-hooks'"
