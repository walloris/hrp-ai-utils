---
name: onboarding
description: Используй, когда пользователь просит провести онбординг проекта, изучить новый проект, обновить контекст проекта, пересобрать память проекта, восстановить .gigacode/memory/ или починить .gigacode/settings.json. Скилл исследует проект и создаёт или обновляет память агента в .gigacode/memory/.
---

# Skill: onboarding

Создаёт или обновляет память проекта в `.gigacode/memory/`.

## Режим

Определи режим:
- `init` — нет `.gigacode/memory/`
- `update` — память есть, нужно обновить контекст
- `repair` — не хватает memory-файлов, нет `.gigacode/settings.json` или сломаны пути

## Шаг 1 — Исследование проекта

Собери только подтверждённые факты:

1. Прочитай `README*`, `docs/` и корневые markdown-файлы
2. Изучи структуру директорий на глубину 2–3 уровня
3. Найди build/config файлы:
   - `build.gradle.kts`, `build.gradle`, `settings.gradle`, `pom.xml`
   - `pyproject.toml`, `requirements.txt`, `poetry.lock`
   - `package.json`, lockfiles
   - `Cargo.toml`, `go.mod`
   - `Makefile`, `Taskfile.yml`
   - `docker-compose.yml`, `compose.yml`, `Dockerfile`
   - `.env.example`, `.env.sample`
4. Найди тесты, линтеры, форматтеры, CI/CD и infra-файлы
5. Определи:
   - название проекта
   - назначение
   - стек
   - точки входа
   - команды запуска и тестов
   - основные модули
   - интеграции
   - архитектурные паттерны

Если факт не найден — используй `<не определено>`.

## Шаг 2 — Полнота анализа

Проведи 2 прохода.

### Проход 1
Найди конечные наборы сущностей, если они существуют:
- типы шаблонов
- модули
- сервисы
- пайплайны
- джобы
- воркеры
- команды CLI
- интеграции
- entrypoints
- конфигурационные профили

Для каждого набора:
1. Найди источник истины: enum, sealed class, config, schema, registry, factory, switch/match, docs
2. Составь полный список
3. Зафиксируй количество

### Проход 2
Сравни найденный список с памятью.
Если найдено 6 сущностей, а описано 4 — дополни память.
Не завершай онбординг, пока покрытие не станет полным или пробелы не будут помечены как `<не определено>`.

Фиксируй покрытие явно:
- `Template types: n из n`
- `Entrypoints: n из n`
- `Main modules: n из n`

## Шаг 3 — Извлечение соглашений

Найди до 3 representative files для каждого слоя:
- controller / api
- service / usecase / handler
- repository / dao / gateway
- config / infrastructure
- dto / model / schema
- tests

Для каждого слоя извлеки:
- naming patterns
- package/layout
- constructor/API shape
- dependency injection style
- error handling style
- logging style
- test naming and structure

Сохраняй примеры в:

```text
.gigacode/memory/references/
├── naming.md
├── layering.md
├── error-handling.md
├── testing.md
└── api-patterns.md
```

Confidence:
- 1 пример → `low`
- 2–3 совпадающих примера → `medium`
- устойчивый паттерн → `high`

Если локальный модуль отличается от общего стиля проекта — зафиксируй это и следуй локальному стилю модуля.

## Шаг 4 — Структура памяти

Создай или обнови:

```text
.gigacode/
├── settings.json
└── memory/
    ├── project.md
    ├── architecture.md
    ├── stack.md
    ├── conventions.md
    └── references/
        ├── naming.md
        ├── layering.md
        ├── error-handling.md
        ├── testing.md
        └── api-patterns.md
```

## Шаг 5 — Содержимое файлов

### `project.md`
Обязательно включи:
- описание проекта
- цель
- дату обновления памяти
- точки входа
- команды запуска/тестов/линта
- ключевые директории
- покрытие (`Entrypoints`, `Main modules`, `External integrations`)
- короткий раздел `Использование памяти`:
   - для общих сведений читай `project.md`, `architecture.md`, `stack.md`, `conventions.md`

### `architecture.md`
Обязательно включи:
- тип архитектуры
- основные компоненты
- поток данных
- интеграции
- конечные наборы сущностей с покрытием
- краткую ASCII-диаграмму или структурное описание

### `stack.md`
Обязательно включи:
- язык и рантайм
- основные зависимости
- базы данных/хранилища
- сборку, тесты, CI, контейнеризацию
- dev-tools

### `conventions.md`
Обязательно включи:
- только подтверждённые соглашения
- observed conventions:
   - общий стиль
   - форматирование и инструменты
   - именование
   - структура кода
   - dependency injection / wiring
   - error handling
   - работа с данными
   - тестирование
   - API и контракты
- confidence по основным категориям
- локальное правило: внутри модуля следовать локальному стилю модуля

#### Когда читать references

Читай только при необходимости — не перед каждой задачей.
Если задача затрагивает несколько аспектов (например, новый endpoint + DTO + тесты),
читай все соответствующие reference-файлы, но не открывай нерелевантные.

| Ситуация | Файл |
|---|---|
| Создаёшь новый класс, функцию, переменную | `references/naming.md` |
| Добавляешь слой, модуль, компонент | `references/layering.md` |
| Пишешь обработку ошибок / Result / try-catch | `references/error-handling.md` |
| Пишешь или правишь тесты | `references/testing.md` |
| Добавляешь endpoint, DTO, request/response | `references/api-patterns.md` |

#### Coding rules

- Think Before Coding:
   - явно проговаривай допущения
   - не принимай скрытых решений за пользователя
   - если что-то неясно, фиксируй неопределённость

- Simplicity First:
   - пиши минимально необходимый код
   - не добавляй абстракции, интерфейсы, конфиги и слои "на будущее"
   - не усложняй решение без необходимости

- Surgical Changes:
   - меняй только то, что нужно для задачи
   - не рефактори соседний код "заодно"
   - не форматируй и не переписывай несвязанные части файла
   - не удаляй код вне задачи

- Goal-Driven Execution:
   - перед изменениями формулируй критерий успеха
   - работай маленькими шагами
   - проверяй результат по тесту, сценарию или ожидаемому поведению

### `references/*.md`
Каждый файл должен содержать:
- `Observed patterns`
- `Good examples`
- `Bad fit`

Не копируй большие куски кода без необходимости.
Указывай паттерны и пути к файлам.

## Шаг 6 — Настройка контекста

Создай или обнови `.gigacode/settings.json`:

```json
{
  "context": {
    "fileName": [
      ".gigacode/memory/project.md",
      ".gigacode/memory/architecture.md",
      ".gigacode/memory/stack.md",
      ".gigacode/memory/conventions.md"
    ]
  }
}
```

Не добавляй `references/*` в preload.
Они должны читаться по необходимости через инструкции из памяти.

## Шаг 7 — Завершение и валидация

1. Убедись, что `.gigacode/` добавлен в `.gitignore`
2. Проверь:
   - все memory-файлы существуют
   - в `settings.json` корректные пути
   - неизвестные данные помечены `<не определено>`
   - покрытие сущностей полное
   - `conventions.md` основан на реальных примерах
   - для conventions выставлен confidence
   - в project.md нет раздела про references — он только в conventions.md
   - в conventions.md нет дублирующего раздела ## References

3. Ответь пользователю:

```text
✅ Онбординг завершён

Режим: <init / update / repair>

Создано/обновлено:
- .gigacode/settings.json
- .gigacode/memory/project.md
- .gigacode/memory/architecture.md
- .gigacode/memory/stack.md
- .gigacode/memory/conventions.md

Проект: <название>
Стек: <основные технологии>
Покрытие:
- Entrypoints: <n из n>
- Main modules: <n из n>
- Key finite sets: <n из n>
```

## Жёсткие правила

- не придумывай информацию
- подтверждай факты только файлами проекта
- если факт не найден, используй `<не определено>`
- не меняй пользовательский код, если задача только в онбординге
- не считай анализ завершённым без проверки полноты покрытия
- не считай conventions установленными без реальных примеров