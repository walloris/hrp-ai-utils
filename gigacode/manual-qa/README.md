# Суб-агент Senior Manual QA

**Агент:** `senior-manual-qa`
**Назначение:** Проектирование тест-артефактов под изменение — тест-кейсы (XML по образцу), чек-листы и тест-план
**Источники:** Jira-задача + Pull Request + Confluence
**Цвет:** Жёлтый

---

## Обзор

Агент превращает изменение (Jira + PR + Confluence) в трассируемые артефакты ручного тестирования. Он **проектирует покрытие, но не меняет код продукта**. Аналог `BUILD SUCCESS` у backend-агента здесь — **Coverage Gate**: задача не закрывается, пока каждое требование не покрыто кейсами и XML не проходит валидацию.

---

## Когда использовать

- Нужно написать тест-кейсы / чек-листы / тест-план под задачу.
- Нужно проверить Pull Request на соответствие критериям приёмки.
- Нужно спроектировать покрытие до начала разработки.

---

## Структура модуля

```
manual-qa/
├── README.md
├── agents/
│   └── senior-manual-qa.md            # сам субагент (промпт + scope + Coverage Gate)
└── skills/
    ├── .system/                       # служебные скиллы (skill-creator/init/review/simplify)
    ├── requirements-analysis/         # требования/AC из Jira+PR+Confluence, риски, пробелы
    ├── test-design-techniques/        # эквив.классы, границы, decision table, pairwise, state-transition
    ├── test-case-writer/              # ЯДРО: генерация XML строго по образцу + валидатор
    ├── checklist-writer/              # smoke/regression чек-листы (Markdown)
    ├── test-plan-writer/              # тест-план (Markdown)
    └── web-fetcher/                   # Jira/Stash через корпоративный SSL
```

---

## Источники требований

| Источник | Как читается |
|----------|--------------|
| Jira-задача | скилл `web-fetcher` (Jira REST + токен) |
| Pull Request | локальный `git diff` (приоритет), fallback — Stash REST через `web-fetcher` |
| Confluence | MCP Atlassian (`confluence_get_page`), fallback — `web-fetcher` |

---

## Рабочий процесс

1. **requirements-analysis** — собрать и приоритизировать требования/AC, найти пробелы и противоречия.
2. **test-design-techniques** — выбрать техники тест-дизайна под каждое требование.
3. **test-case-writer** — сгенерировать XML-кейсы **строго по образцу**, ≥1 позитив + ≥1 негатив на требование.
4. **checklist-writer** — smoke + regression чек-листы (Markdown).
5. **test-plan-writer** — тест-план (Markdown).
6. **Coverage Gate** — проверка покрытия и валидация XML.

---

## Форматы вывода

| Артефакт | Формат | Куда |
|----------|--------|------|
| Тест-кейсы | **XML по образцу** | `doc-as-code/test-cases/<feature>.xml` |
| Чек-листы | Markdown | `doc-as-code/checklists/<feature>.md` |
| Тест-план | Markdown | `doc-as-code/test-plan/<feature>.md` |

Образец XML: **Zephyr/Jira export** (`HRPQA`) — `skills/test-case-writer/assets/test-cases-example.xml`.

---

## Coverage Gate (критерий «успешного успеха»)

Перед сдачей агент проверяет:

1. **Трассируемость** — каждое требование/AC → ≥1 тест-кейс.
2. **Позитив + негатив** — у каждого требования есть оба.
3. **Границы** — применён boundary value analysis там, где есть диапазоны/лимиты.
4. **Валидность XML**:
   ```bash
   python3 skills/test-case-writer/scripts/validate_xml.py <generated.xml> skills/test-case-writer/assets/test-cases-example.xml
   ```
   Должно вывести `VALID`.
5. **Нет «сирот»** — нет кейсов на несуществующее требование; все допущения помечены.

Итог: `COVERAGE OK` (+ таблица трассируемости) или `COVERAGE FAILED: [чего не хватает]`.

---

## Skills

- `requirements-analysis` — анализ требований из Jira/PR/Confluence
- `test-design-techniques` — техники тест-дизайна
- `test-case-writer` — генерация XML-кейсов по образцу (+ `validate_xml.py`)
- `checklist-writer` — чек-листы
- `test-plan-writer` — тест-план
- `web-fetcher` — HTTP к Jira/Stash через корпоративный SSL

---

*Агент: senior-manual-qa | Назначение: проектирование ручного тестового покрытия с трассируемостью*
