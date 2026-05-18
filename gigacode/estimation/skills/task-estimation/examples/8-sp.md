# Эталон: 8 SP

**Название задачи**: HRPLRP-7685. Исправление логики выбора табельника при создании запросов

**Описание**: Баг с неправильным выбором табельника (Персонального номера) при создании запросов (ЗВК, регистрация и проч.). При наличии у сотрудника нескольких табельников система всегда выбирает первый попавшийся из РР (регистрационного профиля) вместо того, чтобы использовать соответствующий табельник, под которым сотрудник оформляет запрос.

**Часы**: 8-12 часов

**Почему 8 SP**: 
- Затронуто 49 файлов в коммите 89879ac8f75cda0222e297b26835b9c339840dac
- Критичная бизнес-логика: выбор creator/author запроса в RequestServiceImpl.saveRequest()
- Метод getOrCreateEmployeeHistoryByPersonIdAndPersonNumber() заменяет устаревший getOrCreate()
- Изменения в PersonProfileHelper.getPerson() с перегрузкой для RegistrationPersonRequest
- 350+ строк новых тестов в RequestServiceImplFindCreatorTest.kt
- Риск каскадных эффектов в интеграционных компонентах (email-уведомления, отчеты, миграции)

**Контекст и ссылки**:
- **Jira**: https://jira.sberbank.ru/browse/HRPLRP-7685
- **Коммит**: 89879ac8f75cda0222e297b26835b9c339840dac в app-learning-requests

**Файлы, которые изменились** (выборка из коммита):
```
app-learning-requests-rest/src/main/kotlin/ru/sberbank/hrp/app/learning/requests/integration/personprofile/PersonProfileHelper.kt
app-learning-requests-rest/src/main/kotlin/ru/sberbank/hrp/app/learning/requests/service/impl/RequestServiceImpl.kt
app-learning-requests-rest/src/main/kotlin/ru/sberbank/hrp/app/learning/requests/service/impl/EmployeeHistoryServiceImpl.kt
app-learning-requests-rest/src/main/kotlin/ru/sberbank/hrp/app/learning/requests/mapper/FutureTransitionsPredictor.kt
app-learning-requests-rest/src/test/kotlin/ru/sberbank/hrp/app/learning/requests/service/impl/RequestServiceImplFindCreatorTest.kt (новый файл, 344 строки)
+ 45 других файлов с обновлением моков и тестов
```

**Технические детали**:
- Старое поведение: `employeeHistoryService.getOrCreate(personId)` - берет первый попавшийся табельник
- Новое поведение: `employeeHistoryService.getOrCreateEmployeeHistoryByPersonIdAndPersonNumber(personId, personNumber)` - точное совпадение
- Паттерн перегрузки метода с null для backward compatibility
- Обновление person.profile.lib до версии 1.8.1-SNAPSHOT

**Тесты**:
- RequestServiceImplFindCreatorTest.kt: 6 новых тестов на разные сценарии
- Обновленные тесты: EmployeeHistoryServiceTest, ProcessManagementConsumerTest, CommonEmailNotificationServiceTest
- Сценарии: с personNumber/без personNumber, new request/existing request

---