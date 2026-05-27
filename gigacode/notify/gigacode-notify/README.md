# GigaCode Notify Plugin 🔔

macOS уведомления для gigacode-cli.

При параллельной работе нескольких агентов получай уведомления когда:
- ⏳ Агент ждёт **апрува**
- ✅ Агент **завершил** работу
- ❌ Произошла **ошибка**

## Пример
![Пример уведомлений](notification-example.jpg)

## Требования

- `python3` должен стоять на маках по умолчанию
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

```bash
mkdir ~/.local/plugin # любая папка где будет лежать плагин 
cd ~/.local/plugin
git clone https://stash.sigma.sbrf.ru/projects/HRPLATFORM/repos/hrp-ai-utils/browse/gigacode/notify/gigacode-notify
cd gigacode-notify
./install.sh
```

Добавь алиас в `~/.zshrc`:
```bash
alias gigacode='gigacode --experimental-hooks'
```