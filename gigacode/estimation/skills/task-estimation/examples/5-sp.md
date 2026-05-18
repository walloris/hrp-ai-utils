# Эталон: 5 SP

**Название задачи**: HRPLRP-7221. Админка подписок. Доработки по полям

**Описание**: Новая фича для админки подписок — добавлены новые поля для фильтрации и отображения, а также рефакторинг сущности Subscription: itemId заменён на связь с Item (ManyToOne), добавлены новые поля в EmployeeHistoryResponse (department, orgUnitId). Коммит включает создание нового сервиса ItemSyncService для синхронизации item из каталога.

**Часы**: 4-8 часов

**Почему 5 SP**: 
- Затронуто 14 файлов: 2 модели сущностей, 3 response DTO, 1 сервис, 5 тестов, 1 фабрика
- 173 строки добавлено, 64 удалено
- Новая сущность ItemSyncService.kt (27 строк)
- Миграция с itemId (UUID) → item (Item entity с ManyToOne)
- 10+ тестовых файлов обновлены под новую структуру
- Добавлены новые поля в фильтрацию и отображение
- Нет сложной бизнес-логики, но много связанных изменений

**Контекст и ссылки**:
- **Jira**: https://jira.sberbank.ru/browse/HRPLRP-7221
- **Коммит**: 4b88741fc7046d9fb80edf7bf6410329b9462f02 в app-onsite-activities

**Файлы, которые изменились**:
```
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/model/entity/Subscription.kt (+11 строк)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/model/response/EmployeeHistoryResponse.kt (+19 строк, -2 строк)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/model/response/SubscriptionResponse.kt (+5 строк, -1 строка)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/service/ItemService.kt (+2 строки, -1 строка)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/service/ItemServiceImpl.kt (+69 строк, -12 строк)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/service/ItemSyncService.kt (+27 строк, новая)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/service/SubscriptionService.kt (+4 строки, -1 строка)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/background/SubscriptionBackgroundTest.kt (+8 строк, -1 строка)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/service/SubscriptionServiceTest.kt (+3 строки)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/consumer/CatalogItemArchivedConsumerTest.kt (+9 строк, -1 строка)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/consumer/OnsiteEmployeeChangedConsumerTest.kt (+11 строк, -1 строка)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/controller/admin/AdminSubscriptionControllerTest.kt (+20 строк, -2 строк)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/controller/user/SubscriptionControllerTest.kt (+10 строк, -1 строка)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/utils/OnsiteEntitiesFactory.kt (+39 строк, -1 строка)
```

**Технические детали**:
- Subscription.entity: itemId: UUID → item: Item (ManyToOne)
- SubscriptionResponse: добавлено itemName из item.name
- EmployeeHistoryResponse: добавлены department, orgUnitId
- ItemSyncService: новый сервис для syncItem(itemId) → Item
- CatalogHelper.getFullItemById() используется для синхронизации

**Критерии из Jira**:
- Проф в админке может перейти в раздел "подписки"
- Видит сведения по подпискам (14 полей)
- Может отфильтровать поitemId, itemName, personId, fullName, position и др.
- Может отсортировать по дате оформления, количеству уведомлений и др.

---