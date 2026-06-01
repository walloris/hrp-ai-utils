---
name: requirements-analysis
description: Extracts and prioritizes testable requirements and acceptance criteria from a Jira task, a Pull Request diff, and Confluence pages. Builds a requirement list with stable IDs, risk and priority, and flags gaps, ambiguities, and contradictions. Trigger as the first step before designing or writing test cases.
---

# Requirements Analysis

Turn scattered inputs (Jira + Pull Request + Confluence) into one normalized, prioritized, **testable** requirements list. This list is the single source of truth for traceability in test cases, checklists, and the test plan.

## Inputs and how to read them

| Source | Tool | What to extract |
|--------|------|-----------------|
| Jira task | `web-fetcher` → Jira REST API (`/rest/api/2/issue/<KEY>`) | Summary, description, AC, labels, linked issues, attachments |
| Pull Request | Local `git` first; Stash REST via `web-fetcher` as fallback | Changed files, endpoints, DTOs, config → **scope of change** |
| Confluence | `web-fetcher` → Confluence REST API (`/rest/api/content/<pageId>`) | Business rules, flows, edge cases, NFRs (`body.storage`) |

**Не использовать:** встроенный `WebFetch`, MCP Atlassian. Только `scripts/web_fetch.py` (как `senior-backend-developer`).

See `references/sources.md` for concrete commands and request recipes.

## Output: requirements table

Produce a Markdown table the other skills consume:

| ID | Требование / AC | Источник | Тип | Риск | Приоритет |
|----|------------------|----------|-----|------|-----------|
| R1 | Пользователь видит ошибку 404 при поиске несуществующего автора | JIRA HRM-11330 | Функц. | Выс. | P1 |
| R2 | Поиск чувствителен к полному совпадению | Confluence | Функц. | Сред. | P2 |
| R3 | Изменён DTO `AuthorDto`: поле `realmName` стало обязательным | PR diff | Контракт | Выс. | P1 |

Rules for the table:
- **ID** — stable `R<n>`. Test cases will reference these IDs for traceability.
- **Тип** — Функциональное / Контракт (API/DTO) / НФТ / Негативное / Граничное.
- **Риск** — Выс./Сред./Низ. by user impact and likelihood.
- **Приоритет** — P1 (критично) … P4 (низкое). Drives test depth.

## Method

1. **Collect** from all three sources; note which source each requirement came from.
2. **Normalize** — split compound statements into atomic, individually verifiable requirements.
3. **Derive from PR** — every changed endpoint/field/config is a requirement even if not written in Jira (mark источник = PR diff).
4. **Cross-check** — find contradictions between Jira, Confluence, and the actual code change. List them under "⚠️ Противоречия".
5. **Gaps** — anything implied but unspecified (empty input, limits, permissions, error paths) goes under "❓ Пробелы и допущения".
6. **Prioritize** — assign risk + priority to focus test design.

## Output sections (Markdown)

```markdown
## Требования и AC
<таблица выше>

## ⚠️ Противоречия
- [R2 vs PR]: Confluence требует частичный поиск, в PR реализован поиск по полному совпадению.

## ❓ Пробелы и допущения
- Не указано поведение при пустой строке поиска → ДОПУЩЕНИЕ: вернуть пустой список, 200 OK.
- Не указаны права доступа к эндпоинту → требует уточнения.
```

## Rules

- **DO NOT** invent requirements — every row traces to Jira, PR, or Confluence.
- **DO** treat each PR change as a requirement (contract/regression coverage).
- **DO** keep IDs stable — downstream artifacts reference them.
- **DO** flag every ambiguity instead of silently guessing; if you must proceed, record it as a marked ДОПУЩЕНИЕ.
- **DO NOT** log tokens/cookies.
