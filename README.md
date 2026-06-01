# hrp-ai-utils

Набор **субагентов, скиллов и утилит** для **GigaCode CLI** в контуре HRP (HR Platform). Материалы сгруппированы по этапам производственного процесса (ПП): аналитика, разработка, тестирование, оценка, эксплуатация.

> Публичная копия для личного использования. Корпоративный upstream: Stash `hrplatform/hrp-ai-utils`. Не публикуйте секреты, токены и внутренние данные в issues/PR.

## Быстрый старт

### 1. Клонировать репозиторий

```bash
git clone https://github.com/walloris/hrp-ai-utils.git
cd hrp-ai-utils
```

### 2. Подключить субагента или скилл

**Глобально** (доступно во всех проектах):

```bash
# Субагент
cp gigacode/manual-qa/agents/senior-manual-qa.md ~/.gigacode/agents/

# Скиллы (пример: manual-qa)
cp -R gigacode/manual-qa/skills/requirements-analysis ~/.gigacode/skills/
cp -R gigacode/manual-qa/skills/test-case-writer ~/.gigacode/skills/
# … остальные скиллы модуля по необходимости
```

**В проекте** (только для текущего репозитория):

```bash
mkdir -p .gigacode/agents .gigacode/skills
cp gigacode/senior-backend-developer/agents/senior-backend-developer.md .gigacode/agents/
cp -R gigacode/senior-backend-developer/skills/git-workflow .gigacode/skills/
```

### 3. Запустить GigaCode

```bash
cd /path/to/your-microservice
gigacode
```

В чате вызовите субагента, например:

```text
@senior-manual-qa

Jira: HRM-12345
PR: ветка feature/user/HRM-12345
Confluence: pageId=...

Сформируй тест-кейсы (XML), чек-лист и тест-план в doc-as-code/.
```

## Структура репозитория

```
hrp-ai-utils/
├── README.md                 # этот файл
├── ai-code-reviewer/         # заготовка под AI code review
└── gigacode/                 # основной каталог GigaCode
    ├── README.md             # общая схема skills / tasks
    ├── senior-backend-developer/
    ├── system-analyst/
    ├── manual-qa/            # субагент Senior Manual QA
    ├── unit-tests/
    ├── estimation/
    ├── mcp/                  # MCP OpenSearch, Serena hooks
    ├── notify/               # macOS-уведомления для GigaCode
    └── …
```

Каждый модуль ПП обычно содержит:

| Путь | Назначение |
|------|------------|
| `agents/<name>.md` | Субагент: frontmatter + системный промпт |
| `skills/<skill>/SKILL.md` | Скилл: инструкции + триггеры |
| `skills/<skill>/scripts/` | Скрипты (валидация, fetch, bash) |
| `skills/<skill>/references/` | Справочники (подгружаются по необходимости) |
| `skills/<skill>/assets/` | Шаблоны и образцы (не в контекст) |

Подробнее о формате скиллов: `gigacode/*/skills/.system/skill-creator/`.

## Модули

| Модуль | Назначение | Документация |
|--------|------------|--------------|
| **manual-qa** | Тест-кейсы (Zephyr XML HRPQA), чек-листы, тест-план | [README](gigacode/manual-qa/README.md) |
| **senior-backend-developer** | Java / Spring Boot / Maven, сборка offline | [README](gigacode/senior-backend-developer/README.md) |
| **system-analyst** | OpenAPI, DBML, PlantUML | [README](gigacode/system-analyst/README.md) |
| **unit-tests** | JUnit 5, Mockito, JaCoCo | [README](gigacode/unit-tests/README.md) |
| **estimation** | Story points, груминг (Альфа/Омега) | [README](gigacode/estimation/README.md) |
| **stacktrace-analize** | Локализация бага по stack trace (readonly) | [README](gigacode/stacktrace-analize/README.md) |
| **liquibase** | Миграции БД + синхронизация кода | [README](gigacode/liquibase/README.md) |
| **mcp** | MCP OpenSearch (логи hr-log), Serena remind | [README](gigacode/mcp/mcp-opensearch/README.md) |
| **notify** | Уведомления macOS при апруве/стопе агента | [README](gigacode/notify/gigacode-notify/README.md) |
| **code-analyzer** | Память проекта `.gigacode/memory/` | [README](gigacode/code-analyzer/commands/memory/README.md) |

Модули-заготовки (только README): `code-writer`, `swagger`, `sonar`, `log`, `controller-dto-generation`, `db-repository-generation`.

## Senior Manual QA (кратко)

Субагент `senior-manual-qa` проектирует ручное тестирование по **Jira + PR + Confluence**:

1. `requirements-analysis` — требования и AC  
2. `test-design-techniques` — техники тест-дизайна  
3. `test-case-writer` — XML в формате **Zephyr export (HRPQA)**  
4. `checklist-writer` / `test-plan-writer` — Markdown  

Критерий готовности — **Coverage Gate** (трассируемость + `validate_xml.py` → `VALID`).

Образец XML: `gigacode/manual-qa/skills/test-case-writer/assets/test-cases-example.xml`.

## Интеграции

| Сервис | Как подключить |
|--------|----------------|
| **Jira** | Скилл `web-fetcher` + личный токен в заголовке |
| **Confluence** | MCP Atlassian (`confluence_get_page`) |
| **Stash / Bitbucket PR** | Локальный `git diff` или REST через `web-fetcher` |
| **OpenSearch / логи** | MCP `mcp-opensearch` + cookie из Kibana — см. [инструкцию](gigacode/mcp/mcp-opensearch/README.md) |

Пример фрагмента `~/.gigacode/settings.json` для MCP — в README соответствующего модуля.

## Память проекта (onboarding)

Команды из `gigacode/code-analyzer/commands/memory/` скопируйте в `~/.gigacode/commands/`:

- `memory:init` — первичное изучение репозитория  
- `memory:update` — обновление контекста  
- `memory:repair` — восстановление `.gigacode/memory/`  

## Разработка и проверка скиллов

```bash
# Валидация frontmatter скилла
python3 gigacode/manual-qa/skills/.system/skill-creator/scripts/validate_skill.py \
  gigacode/manual-qa/skills/test-case-writer

# Валидация XML тест-кейсов против образца
python3 gigacode/manual-qa/skills/test-case-writer/scripts/validate_xml.py \
  doc-as-code/test-cases/feature.xml \
  gigacode/manual-qa/skills/test-case-writer/assets/test-cases-example.xml
```

## Ветки и remotes

| Remote | URL |
|--------|-----|
| `github` | https://github.com/walloris/hrp-ai-utils.git |
| `origin` | корпоративный Stash (при наличии VPN) |

Актуальная разработка: ветка `tech/nikonovagent` (в т.ч. модуль `manual-qa`).

## Лицензия и ответственность

Внутренние наработки HRP. Используйте в соответствии с политиками ИБ вашей организации. Перед публикацией в открытый GitHub проверьте, что в коммитах нет персональных данных, production URL и токенов.

## См. также

- [gigacode/README.md](gigacode/README.md) — схема каталогов skills / tasks  
- [manual-qa](gigacode/manual-qa/README.md) — запуск субагента QA  
- [senior-backend-developer](gigacode/senior-backend-developer/README.md) — backend-агент и Maven offline  
