# Эталон: 1 SP

**Название задачи**: HRPLRP-7642. Замена цепочек согласования в 2026 v3

**Описание**: Техническая задача по миграции цепочек согласования для мероприятий и потоков, стартующих в 2026 году. Заменяются 4 старых маршрута согласования на новые цепочки chain5 и chain15.

**Часы**: 1-2 часа

**Почему 1 SP**: 
- Простая SQL-миграция: 2 файла по 15 строк each
- 6 строк добавлено в Liquibase changelog
- Нет изменения бизнес-логики, только data fix
- Нет изменений в коде Java/Kotlin
- Нет сложных тестов (тесты — просто проверка SQL)
- Тип задачи: Story (может и bug)

**Контекст и ссылки**:
- **Jira**: https://jira.sberbank.ru/browse/HRPLRP-7642
- **Коммит**: 05e43c4769f12b76416c9bc253baa9a9a1933232 в app-onsite-activities

**Мапинг цепочек**:
| Согласующие | ID маршрута | ID цепочки |
|------------|-------------|------------|
| Линейный руководитель, HR-партнёр | 37dc5253-baba-4f8c-9554-585dfd2a76aa | chain5 |
| Линейный руководитель, HR-партнёр | b80ff118-5fd6-4476-9214-5eeb6335325a | chain5 |
| Линейный руководитель | 77b25850-998b-4b18-b53b-f0a88cbcd64a | chain15 |
| Линейный руководитель | 0ac23fff-98c3-4a88-a25a-6dc988a3dc86 | chain15 |

**Файлы, которые изменились**:
```
HRP-pipeline/app-onsite-activities/dbscripts/liquibase/osactiv/changelog/2026-04.xml (+6 строк)
HRP-pipeline/app-onsite-activities/dbscripts/liquibase/osactiv/sql/2026/04/13-update-chain.sql (+15 строк, новая)
HRP-pipeline/app-onsite-activities/dbscripts/liquibase/osactiv/sql/2026/04/14-update-chain-stream.sql (+15 строк, новая)
```

**Технические детали**:
- Изменения: ` onsite.activity ` и ` streams.stream ` таблицы
- Условие: ` approval_procedure_id IN (...) ` — только 4 конкретных маршрута
- Другие старые маршруты не затрагиваются (через явный перечень)

---