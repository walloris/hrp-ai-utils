# Эталон: 3 SP

**Название задачи**: HRPLRP-7732. Бесплатное обучение не должно учитываться для ограничения по 2 неявкам

**Описание**: Фича для учета неявок по платному обучению. Бесплатные мероприятия/программы (где is_payed = false) больше не учитываются при подсчёте неявок для ограничения записи на новое платное обучение. Проверка применяется при:
1. Подаче ЗВК
2. Записи на платное обучение (Warning при 1 неявке / сообщение при 2 неявках)
3. Регистрации со стороны профа файлом (HRPLRP-7431)
4. Регистрации со стороны профа вручную (HRPLRP-7395)

**Часы**: 2-4 часа

**Почему 3 SP**: 
- Затронуто 4 файла: 2 репозитория с SQL, 2 теста
- SQL-изменения: добавлен фильтр `is_payed = true` в 2 запроса
- Семантическая простота: добавить условие в существующий запрос
- Тесты: 6 строк изменено (null → true/false для isPayed/isUsedInStream)
- Нет новых компонентов, только доработка фильтрации

**Контекст и ссылки**:
- **Jira**: https://jira.sberbank.ru/browse/HRPLRP-7732
- **Коммит**: a85e52edd829d8febfb37f4fe74915e128e3f814 в app-onsite-activities

**Файлы, которые изменились**:
```
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/repository/onsite/OnsiteRegistrationRepository.kt (+1 строка)
app-activities-streams/src/main/kotlin/ru/sberbank/hrp/app/activities/streams/subscription/repository/streams/StreamsRegistrationRepository.kt (+2 строки, -1 строка)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/learning/streams/controller/user/UserControllerTest.kt (+3 строки, -3 строк)
app-onsite-activities-application/src/test/kotlin/ru/sberbank/hrp/app/onsite/service/admin/UserServiceTest.kt (+1 строка, -1 строка)
```

**Технические детали**:
- OnsiteRegistrationRepository.findRegistrationsByPersonIdsWithStatusesBetweenDates(): добавлен `AND a.is_payed = true`
- StreamsRegistrationRepository.findRegistrationsByPersonIdsWithStatusesBetweenDates(): добавлен `AND s.is_payed = true`
- Тесты: isPayed = null → isPayed = true (теперь фильтруем только платное)

**Определение бесплатного обучения**:
- Регистрация в группу/поток без флага "платное обучение" (is_payed = false)
- Даже если цена указана, но посещаемость по сотруднику нулевая → всё равно платное

---