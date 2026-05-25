# MCP OpenSearch Server

MCP сервер для доступа к корпоративному OpenSearch через Kibana (hr-log.sberbank.ru).

## Основное использование в Gigacode CLI

### Шаг 1: Сборка проекта

Соберите JAR файл:

```bash
./gradlew build
```

**Или** используйте fat JAR из папки `compiled`, если он уже готов.

### Шаг 2: Получение X-HRP-PERSON-ID и set-cookie

1. Перейдите на https://hr-log.sberbank.ru/kibana/app/opensearch_dashboards_overview#/
2. Откройте DevTools (F12)
3. Найдите запрос `POST https://hr-log.sberbank.ru/kibana/internal/search/opensearch`
4. Из `requestHeaders` скопируйте значение заголовка `x-hrp-person-id`
5. Сохраните его в переменной окружения `X_HRP_PERSON_ID`
6. **Из `responseHeaders` скопируйте значение заголовка `set-cookie`** — он понадобится для инициализации cookie в шаге 5

### Шаг 3: Настройка .gigacode/settings.json

Добавьте в `.gigacode/settings.json`:

```json
{
  "mcpServers": {
    "nzhilik": {
      "command": "java",
      "args": [
        "-jar",
        "${PATH_TO_DIRECTORY_WITH_JARS}/mcp-opensearch-all-v.0.1-alpha.jar"
      ],
      "env": {
        "COOKIE_SERVER_PORT": "8093",
        "X_HRP_PERSON_ID": "${copied x_hrp_person_id}"
      }
    }
  }
}
```

> **Замените** путь к JAR файлу на актуальный и укажите ваш `X_HRP_PERSON_ID`.

### Шаг 4: Проверка

1. Запустите Gigacode
2. Проверьте, что команда `/mcp` отображает добавленный сервер
3. MCP сервер должен быть **зелёным**, если нет проблем

### Шаг 5: Инициализация cookie через локальный сервер

Только после успешного запуска MCP сервера, инициализируйте cookie:

```bash
curl --request POST \
  --url http://localhost:${COOKIE_SERVER_PORT}/update_cookie \
  --header 'Content-Type: application/json' \
  --data '{
    "cookie": "${copied set-cookie header}"
  }'
```

> **Важно**: Cookie берется из `responseHeaders` того же запроса, который использовался для получения `x-hrp-person-id`.

> **Важно**: Инициализацию cookie нужно повторять в каждой новой сессии Gigacode, либо когда приходит ответ `401` в результате запроса к `search_logs` или `get_messages`.

### Шаг 6: Использование

Теперь можно использовать инструменты, например:

```
Есть ли за сегодня ошибки в сервисе app-onboarding?
```

Пример результата:

```
  ╭─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╮
  │ ✓  search_logs (nzhilik MCP Server) {"index":"log_ci04183799-app-onboarding*","startTime":"2026-05-25T00:00:00+03:00","endTime":"2026-05-25T23:59:59+03:00","dql":"severity:ERROR OR severity:FATAL"}       │
  │                                                                                                                                                                                                             │
  │    {"total":226052,"aggregation":[{"date":"2026-05-25T00:00:00.000+03:00","count":226052}],"dql":"severity:ERROR OR severity:FATAL"}                                                                        │
  ╰─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╯
```

> **Подсказка**: При первом запросе укажите index явно (например, `log_ci04183799-app-onboarding*`), если модель не догадывается сама.

---


## Обновление cookie в рантайме

После запуска сервер доступен по адресу `http://localhost:8093` (или указанный в `COOKIE_SERVER_PORT`).

**Инициализация cookie (один раз после запуска):**
```bash
curl --request POST \
  --url http://localhost:${COOKIE_SERVER_PORT}/update_cookie \
  --header 'Content-Type: application/json' \
  --data '{
    "cookie": "security_authentication=Fe26.2...; HttpOnly; Path=/kibana"
  }'
```

> **Важно**: Инициализацию cookie нужно повторять в каждой новой сессии Gigacode, либо когда приходит ответ `401`.

## Известные ограничения

- MCP сервер, кажется, работает только в одном из поднятых экземпляров gigacode-cli. Если инструменты не работают, проверьте, что запросы идут в нужный экземпляр.
- Cookie для авторизации действует ограниченное время — при получении `401` ошибки нужно повторно выполнить инициализацию cookie.

## Доступные MCP инструменты

#### 1. search_logs
Поиск логов и получение количества совпадений с агрегацией по времени.

**Особенности:**
- Возвращает количество совпадений и агрегацию по временным интервалам
- Использует `size: 0` для эффективного подсчёта
- Добавляет date histogram aggregation по умолчанию

**Аргументы:**
- `index` (обязательный): индекс для поиска
- `startTime` (опциональный): время начала в ISO 8601 формате (MSK), по умолчанию: 90 дней назад
- `endTime` (опциональный): время окончания в ISO 8601 формате (MSK), по умолчанию: сейчас
- `dql` (опциональный): DQL запрос для фильтрации
- `bucketSize` (опциональный): интервал агрегации (например, '1d', '30m', '5m'), по умолчанию: '1d'
- `withoutAgg` (опциональный): если true, пропустить агрегацию и вернуть только count хитов, удовлетворяющих фильтру (без динамики по времени), по умолчанию: false

**Пример использования:**
```
search_logs(index="log_ci04183799-app-onboarding*", dql="severity:ERROR", bucketSize="1h")
```

#### 2. get_messages
Получение детальных сообщений из OpenSearch.

**Особенности:**
- Возвращает сами содержимые сообщения (включая поле message)
- Сортирует по времени убыванию (новые первыми)
- Поддерживает лимит количества результатов

**Аргументы:**
- `index` (обязательный): индекс для поиска
- `startTime` (обязательный): время начала в ISO 8601 формате (MSK)
- `endTime` (обязательный): время окончания в ISO 8601 формате (MSK)
- `dql` (опциональный): DQL запрос для фильтрации
- `limit` (опциональный): максимум результатов, по умолчанию: 5

**Пример использования:**
```
get_messages(index="log_ci04183799-app-onboarding*", startTime="2026-05-25T00:00:00+03:00", endTime="2026-05-25T23:59:59+03:00", dql="severity:ERROR", limit=10)
```

## Структура проекта

```
src/main/java/ru/nzhilik/
├── Main.java              # Точка входа, обработка JSON-RPC
├── infra/                 # Инфраструктура
│   └── CookieServer.java  # HTTP сервер для обновления cookie (безопасное обновление без JSON)
└── mcp/                   # MCP слой
│   ├── OpenSearchClient.java  # Клиент для запросов к OpenSearch
│   └── tools/             # MCP инструменты
│       ├── SearchLogsTool.java    # Поиск логов с агрегацией по времени (только count)
│       └── GetMessagesTool.java   # Получение детальных сообщений из OpenSearch
└── dql/                   # Парсинг DQL запросов
    └── DqlParser.java
```
