# XML format mapping

How test-case fields map to XML nodes. **The example file `assets/test-cases-example.xml` is the source of truth.** This document describes the *current placeholder* schema — update it together with the example when the real project format is provided.

## Current (placeholder) schema

```
<test-suite name="..." feature="...">      ← набор тестов
  <test-case id="..." priority="..." type="...">   ← один кейс
    <title>...</title>                      ← заголовок
    <requirement>R1</requirement>           ← трассируемость к требованию (ОБЯЗАТЕЛЬНО)
    <preconditions>...</preconditions>      ← предусловия
    <test-data>...</test-data>              ← тестовые данные
    <steps>
      <step number="N">
        <action>...</action>                ← действие
        <expected>...</expected>            ← ожидаемый результат шага
      </step>
    </steps>
    <expected-result>...</expected-result>  ← итоговый результат кейса
  </test-case>
</test-suite>
```

## Field mapping

| Поле кейса | XML-узел | Атрибут/тег |
|------------|----------|-------------|
| ID кейса | `<test-case>` | `id` |
| Приоритет | `<test-case>` | `priority` (`P1..P4`) |
| Тип | `<test-case>` | `type` (`positive`/`negative`) |
| Заголовок | `<title>` | text |
| Требование (трассировка) | `<requirement>` | text (`R1`, или несколько узлов) |
| Предусловия | `<preconditions>` | text |
| Тестовые данные | `<test-data>` | text |
| Шаг | `<step>` | `number` + `<action>`/`<expected>` |
| Итоговый результат | `<expected-result>` | text |

## When the real example arrives

1. Replace `assets/test-cases-example.xml` with the real file.
2. Rewrite the schema block + mapping table above to match it exactly (rename nodes, reorder, add/remove fields).
3. If the real format encodes traceability differently (e.g. a `<refs>` or custom field), point the "Требование" mapping there.
4. Update `scripts/validate_xml.py` only if the new format needs deeper rules — by default it auto-derives the allowed structure from the example, so usually no code change is needed.
5. Re-run validation on a sample to confirm `VALID`.

## Mapping hints for common tools (pick when example is provided)

- **TestRail XML**: `<sections>/<section>/<cases>/<case>` with `<custom><steps_separated>`.
- **Test IT**: sections + steps with `action`/`expected`.
- **Zephyr/Xray**: usually CSV/JSON, but XML variants use `<testcase>` with `<step>/<data>/<result>`.
- **Allure**: typically generated from code, not authored XML.

Match whatever the provided example uses — do not impose one of these if it differs.
