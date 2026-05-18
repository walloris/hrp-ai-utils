# Эталон: 2 SP

**Название задачи**: HRPLRP-7609. Subscription. Исправление поиска по не полному совпадению

**Описание**: Баг с фильтрацией подписок в админке — поиск требовал полного точного совпадения и учитывал регистр. Нужно добавить LIKE-поиск по неполному совпадению (case-insensitive) для itemName и изменить countSent на диапазон (countSentFrom/countSentTo) для фильтрации по количеству отправленных сообщений.

**Часы**: 2-4 часа

**Почему 2 SP**: 
- Затронуто 3 файла: 2 модели/спецификации + 1 тест
- Изменения в SubscriptionViewSpecification: 10 строк изменено (equals → like)
- Модель SubscriptionFilter: 6 строк изменено (добавлены countSentFrom/countSentTo)
- Тесты: 6 строк изменено (обновлены моки)
- Простая логика: LIKE с lowercase() + диапазон для countSent
- Нет сложных интеграций или новых компонентов

**Контекст и ссылки**:
- **Jira**: https://jira.sberbank.ru/browse/HRPLRP-7609
- **Коммит**: 3b6db1038fe1c4337c13d1882f9094a25ff1af9f в app-onsite-activities

**Файлы, которые изменились**:
```
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/model/filter/SubscriptionFilter.kt (+6 строк)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/repository/specification/SubscriptionViewSpecification.kt (+10 строк, -3 строк)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/controller/admin/AdminSubscriptionControllerTest.kt (+6 строк, -2 строк)
```

**Технические детали**:
- itemName: `builder.equal(key, it)` → `builder.like(builder.lower(key), "%${it.lowercase()}%")`
- countSent: int → countSentFrom + countSentTo (диапазон)
- Глобальный LIKE-поиск для fullName, position, subdivision, department
- Регистронезависимый поиск через lowercase()

---