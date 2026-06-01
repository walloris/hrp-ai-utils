# Source extraction recipes

Корпоративные HTTP-запросы — **только** через скилл `web-fetcher` (`scripts/web_fetch` / `scripts/web_fetch.py`).  
По аналогии с `senior-backend-developer`: самоподписанный SSL, без встроенного `web_fetch` и без MCP Atlassian.

**Авторизация** (один из вариантов, не логировать в консоль):

```bash
-H 'Authorization: Bearer <TOKEN>'
# или
-H 'Authorization: Basic <base64(user:password_or_token)>'
```

Путь к скрипту из корня скилла: `skills/web-fetcher/scripts/web_fetch`.

---

## 1. Jira — REST API

### Задача по ключу

```bash
./scripts/web_fetch \
  "https://jira.sberbank.ru/rest/api/2/issue/HRM-11330?fields=summary,description,status,issuetype,issuelinks,labels,attachment,comment" \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -o jira-issue.json
```

Из ответа извлеки:
- `fields.summary`, `fields.description` — текст требований.
- Критерии приёмки — в `description` или custom field (ищи «AC», «Критерии приёмки», нумерованные списки).
- `fields.issuelinks` — связанные истории/баги.
- `fields.attachment` — вложения (при необходимости отдельный запрос на контент).

### URL → ключ

`https://jira.sberbank.ru/browse/HRM-11330` → `HRM-11330`

### Комментарии (опционально)

```bash
./scripts/web_fetch \
  "https://jira.sberbank.ru/rest/api/2/issue/HRM-11330/comment" \
  -H 'Authorization: Bearer <TOKEN>' \
  -o jira-comments.json
```

---

## 2. Confluence — REST API

### Страница по `pageId`

ID берётся из URL:  
`https://confluence.sberbank.ru/pages/viewpage.action?pageId=23021781342` → `23021781342`

```bash
./scripts/web_fetch \
  "https://confluence.sberbank.ru/rest/api/content/23021781342?expand=body.storage,body.view,space,version,ancestors" \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -o confluence-page.json
```

Из ответа:
- `title` — заголовок страницы.
- `body.storage.value` — HTML-контент (основной источник правил).
- `body.view` — если есть, удобнее для чтения.
- `ancestors` — контекст иерархии.

### Поиск страницы (если известны space + заголовок)

```bash
./scripts/web_fetch \
  "https://confluence.sberbank.ru/rest/api/content/search?cql=space=HRP+and+title~\"Название+страницы\"" \
  -H 'Authorization: Bearer <TOKEN>' \
  -o confluence-search.json
```

Возьми `results[0].id` как `pageId` и запроси страницу полностью (команда выше).

### Дочерние страницы (опционально)

```bash
./scripts/web_fetch \
  "https://confluence.sberbank.ru/rest/api/content/23021781342/child/page?limit=50" \
  -H 'Authorization: Bearer <TOKEN>' \
  -o confluence-children.json
```

---

## 3. Pull Request

### Предпочтительно: локальный git

```bash
git diff --name-status origin/master...HEAD
git diff origin/master...HEAD
git log --oneline origin/master..HEAD
```

Маппинг изменений → требования:
- новый/изменённый REST endpoint → функциональное + контрактное;
- DTO / обязательные поля → контракт + регресс;
- конфиг / feature flag → окружение и переключатели;
- миграции → данные и обратная совместимость.

### Fallback: Stash / Bitbucket REST

```bash
./scripts/web_fetch \
  "https://stash.sigma.sbrf.ru/rest/api/1.0/projects/HRPLATFORM/repos/<repo>/pull-requests/<PR_ID>" \
  -H 'Authorization: Bearer <TOKEN>' -o pr.json

./scripts/web_fetch \
  "https://stash.sigma.sbrf.ru/rest/api/1.0/projects/HRPLATFORM/repos/<repo>/pull-requests/<PR_ID>/changes" \
  -H 'Authorization: Bearer <TOKEN>' -o pr-changes.json
```

---

## Merge order

При противоречиях фиксируй в «⚠️ Противоречия», не выбирай молча:

1. **PR / код** — что реально сделано.
2. **Jira** — что заказано.
3. **Confluence** — бизнес-правила и контекст.

---

## Запрещено

- ❌ Встроенный `web_fetch` / `WebFetch` GigaCode (SSL в корп. сети).
- ❌ MCP Atlassian для Jira/Confluence в workflow `senior-manual-qa`.
- ❌ Вывод токенов и паролей в лог или ответ пользователю.
