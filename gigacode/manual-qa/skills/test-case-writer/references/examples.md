# Filled examples

Worked examples for the **placeholder** schema. Regenerate these against the real example once it is provided. Scenario source: a search endpoint that returns 404 for a missing author (JIRA HRM-11330).

## Requirement

| ID | Требование | Приоритет |
|----|-----------|-----------|
| R1 | Поиск автора по имени в реалме; несуществующий → 404 | P1 |

## Positive case

```xml
<test-case id="TC-R1-01" priority="P1" type="positive">
  <title>[P1] Поиск автора — существующий автор найден</title>
  <requirement>R1</requirement>
  <preconditions>В реалме "sales" существует автор "Иванов И.И."</preconditions>
  <test-data>realm=sales, query=Иванов</test-data>
  <steps>
    <step number="1">
      <action>Открыть страницу поиска авторов в реалме "sales"</action>
      <expected>Поле поиска доступно</expected>
    </step>
    <step number="2">
      <action>Ввести "Иванов" и выполнить поиск</action>
      <expected>В списке отображается "Иванов И.И."</expected>
    </step>
  </steps>
  <expected-result>HTTP 200, автор найден и показан в результатах</expected-result>
</test-case>
```

## Negative case (404)

```xml
<test-case id="TC-R1-02" priority="P1" type="negative">
  <title>[P1] Поиск автора — несуществующий автор возвращает 404</title>
  <requirement>R1</requirement>
  <preconditions>В реалме "sales" нет автора "Петров"</preconditions>
  <test-data>realm=sales, query=Петров</test-data>
  <steps>
    <step number="1">
      <action>Выполнить поиск "Петров" в реалме "sales"</action>
      <expected>Система обрабатывает запрос корректно</expected>
    </step>
  </steps>
  <expected-result>HTTP 404 с сообщением об отсутствии автора; приложение не падает</expected-result>
</test-case>
```

## Boundary case

```xml
<test-case id="TC-R1-03" priority="P2" type="negative">
  <title>[P2] Поиск автора — запрос длиннее 255 символов</title>
  <requirement>R1</requirement>
  <preconditions>Открыта страница поиска</preconditions>
  <test-data>query = строка из 256 символов</test-data>
  <steps>
    <step number="1">
      <action>Ввести строку из 256 символов и выполнить поиск</action>
      <expected>Ввод отклонён или усечён по правилу валидации</expected>
    </step>
  </steps>
  <expected-result>HTTP 400 (валидация) либо корректное усечение; ошибки сервера нет</expected-result>
</test-case>
```

## Traceability output

| Требование | Тест-кейсы |
|------------|-----------|
| R1 | TC-R1-01 (позитив), TC-R1-02 (негатив), TC-R1-03 (граница) |
