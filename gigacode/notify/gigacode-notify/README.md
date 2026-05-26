# GigaCode Notify Plugin 🔔

macOS уведомления для gigacode-cli.

При параллельной работе нескольких агентов получай уведомления когда:
- ⏳ Агент ждёт **апрува**
- ✅ Агент **завершил** работу
- ❌ Произошла **ошибка**

## Пример
![Пример уведомлений](notification-example.jpg)

## Требования

- `jq`
- `gigacode --experimental-hooks` включает хуки
- `Редактор скриптов` допуск к уведомлениям.

![Настройка уведомлений](notification-settings.jpg)

## Запуск
Если нету alias
```bash
gigacode --experimental-hooks
```
Если добавить алиас в `~/.zshrc`:
```bash
alias gigacode='gigacode --experimental-hooks'
```
Тогда запуск как обычно
```bash
gigacode
```

## Установка

### Установка jq
- `jq` — https://github.com/jqlang/jq/releases
> скачать нужный релиз. если у вас mac apple silicon то качаем `jq-macos-arm64`
> После скачивания просто положи файл в любую папку например (например ~/local/bin)
> Переименовать jq-linux-arm64 в jq
```bash
# Сделать файл исполняемым
chmod +x ~/local/bin/jq

# Прописать в PATH ~/bin
echo 'export PATH="$HOME/local/bin:$PATH"' >> ~/.zshrc

# Перечитать настройки (чтобы не перезапускать терминал)
source ~/.zshrc
```

### Установка плагина уведомлений
```bash
git clone https://stash.sigma.sbrf.ru/projects/HRPLATFORM/repos/hrp-ai-utils/browse/gigacode/notify/gigacode-notify
cd gigacode-notify
./install.sh
```

Добавь алиас в `~/.zshrc`:
```bash
alias gigacode='gigacode --experimental-hooks'
```

## Дебаг

```bash
# Включить лог
export GIGACODE_NOTIFY_LOG=/tmp/gigacode-notify-debug.log
tail -f /tmp/gigacode-notify-debug.log
```