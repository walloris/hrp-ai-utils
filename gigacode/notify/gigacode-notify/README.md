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

Копируем папку `gigacode-notify` в любую директорию

```bash
cd gigacode-notify
./install.sh
```

## Добавить переменные руками
Добавить функцию в `~/.zshrc`:
```bash
echo '
# >>> gigacode-notify >>>
gigacode() {
  command gigacode --experimental-hooks "$@"
}
# <<< gigacode-notify <<<' >> ~/.zshrc
```

Или добавить алиас в `~/.zshrc` но могут быть рекурсии, лучше дать другое название просто:
```bash
alias gigacode='gigacode --experimental-hooks'
```