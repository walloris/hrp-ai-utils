#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# GigaCode Notify Plugin — Installer
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOK_SCRIPT="$SCRIPT_DIR/gigacode-notify.sh"

# ── Проверяем зависимости ─────────────────────────────────────────────────────
for dep in python3 osascript; do
  if ! command -v "$dep" &>/dev/null; then
    echo "❌ Зависимость не найдена: $dep"
    exit 1
  fi
done
echo "✅ Зависимости в порядке (python3, osascript)"

# ── Режим установки ───────────────────────────────────────────────────────────
SETTINGS_DIR="$HOME/.gigacode"
SETTINGS_FILE="$SETTINGS_DIR/settings.json"
echo "📁 Устанавливаем глобально: $SETTINGS_FILE"

mkdir -p "$SETTINGS_DIR"

# ── Мержим / создаём settings.json через python3 ─────────────────────────────
python3 - "$SETTINGS_FILE" "$HOOK_SCRIPT" << 'PYEOF'
import sys, json, os, shutil, time

settings_file = sys.argv[1]
hook_script   = sys.argv[2]

def make_hook(name):
    return {
        "type": "command",
        "command": hook_script,
        "name": name,
        "async": True,
        "timeout": 5000
    }

hook_config = {
    "PreToolUse": [
        {"matcher": "*", "hooks": [make_hook("gigacode-notify-pretool")]}
    ],
    "PermissionRequest": [
        {"matcher": "*", "hooks": [make_hook("gigacode-notify-permission")]}
    ],
    "Stop": [
        {"matcher": "", "hooks": [make_hook("gigacode-notify-stop")]}
    ],
    "SubagentStop": [
        {"matcher": "*", "hooks": [make_hook("gigacode-notify-subagent-stop")]}
    ],
    "StopFailure": [
        {"matcher": "*", "hooks": [make_hook("gigacode-notify-failure")]}
    ]
}

if os.path.isfile(settings_file):
    print(f"🔄 Найден существующий {settings_file}, мержим hooks...")
    try:
        with open(settings_file, "r", encoding="utf-8") as f:
            existing = json.load(f)
    except json.JSONDecodeError:
        print("⚠️  settings.json невалиден! Создаём резервную копию...")
        backup = f"{settings_file}.backup.{int(time.time())}"
        shutil.copy2(settings_file, backup)
        existing = {}
else:
    print(f"📝 Создаём новый {settings_file}...")
    existing = {}

existing["hooks"] = hook_config

with open(settings_file, "w", encoding="utf-8") as f:
    json.dump(existing, f, indent=2, ensure_ascii=False)
    f.write("\n")
PYEOF

# ── Предлагаем добавить helper в ~/.zshrc ─────────────────────────────────────
ZSHRC="$HOME/.zshrc"
START_BLOCK_BEGIN="# >>> gigacode-notify >>>"
START_BLOCK_END="# <<< gigacode-notify <<<"

read -r -d '' ZSHRC_BLOCK << 'EOF' || true
# >>> gigacode-notify >>>
gigacode() {
  command gigacode --experimental-hooks "$@"
}
# <<< gigacode-notify <<<
EOF

echo ""
echo "💡 Опция: добавить helper в ~/.zshrc"
echo "   Что он сделает: команда gigacode будет запускаться с --experimental-hooks."
echo "   Это нужно для автоматического включения hooks."
echo ""

read -r -p "Добавить helper в ~/.zshrc? [y/N] " ADD_ZSHRC

if [[ "$ADD_ZSHRC" =~ ^[Yy]$ ]]; then
  if [[ -f "$ZSHRC" ]] && grep -Fq "$START_BLOCK_BEGIN" "$ZSHRC"; then
    echo "ℹ️  Блок gigacode-notify уже есть в $ZSHRC"
  else
    touch "$ZSHRC"
    {
      echo ""
      echo "$ZSHRC_BLOCK"
    } >> "$ZSHRC"

    echo "✅ Helper добавлен в $ZSHRC"
    echo "🔄 Применить изменения:"
    echo "   source ~/.zshrc"
  fi
else
  echo "⏭️  Пропускаем изменение ~/.zshrc"
fi

# ── Итог ─────────────────────────────────────────────────────────────────────
echo ""
echo "✅ Установка завершена!"
echo ""
echo "📄 Настройки: $SETTINGS_FILE"
echo "🔨 Скрипт:    $HOOK_SCRIPT"
echo ""
echo "🚀 Запуск:"
if [[ "$ADD_ZSHRC" =~ ^[Yy]$ ]]; then
  echo "   gigacode"
else
  echo "   gigacode --experimental-hooks"
fi