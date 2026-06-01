---
name: test-case-writer
description: Generates manual test cases as an XML file that strictly mirrors a provided example (assets/test-cases-example.xml) — same element names, order, and nesting. Maps scenarios from test-design-techniques into well-formed XML with steps, test data, and expected results, keeps requirement traceability, and validates the output. Trigger when test cases must be produced in the project XML format.
---

# Test Case Writer

Produce manual test cases as **XML that mirrors the example exactly**. The example file is the contract — element names, order, and nesting must match 1:1. Only values change.

> ⚠️ The canonical example lives at `assets/test-cases-example.xml`. It is currently a **TODO placeholder**. Replace it with the real project example before production use; the structure below adapts automatically once the example is updated. See `references/xml-format.md` for the field mapping.

## Golden rule

**Copy the example, then fill values.** Do not redesign the schema, do not rename elements, do not reorder them. If the example has an element you don't have data for, keep the element and leave it empty (or per the example's convention) rather than dropping it.

## Inputs

- Requirements table (with IDs) from `requirements-analysis`.
- Scenario list (priority, polarity, technique) from `test-design-techniques`.

## Per-case content

Every test case must carry:

| Field | Rule |
|-------|------|
| ID | Stable, unique (e.g. `TC-R1-01`). Encodes the requirement ID for traceability. |
| Заголовок | `[<приоритет>] <что проверяем> — <условие>` |
| Требование | The requirement ID(s) it covers (`R1`). **Mandatory** — no orphan cases. |
| Предусловия | State/data needed before steps. |
| Шаги | Numbered, atomic, imperative actions. |
| Тестовые данные | Concrete values (boundary/equivalence values when relevant). |
| Ожидаемый результат | Observable, unambiguous outcome per step / at the end. |
| Приоритет | P1..P4 from the scenario. |
| Тип | позитивный / негативный / граничный. |

## Workflow

1. Read `assets/test-cases-example.xml` and `references/xml-format.md` to learn the exact structure.
2. For each scenario, create one `<test-case>` (or the example's equivalent node) by cloning the example node.
3. Fill steps/data/expected; encode the requirement ID into the case ID and into the traceability field if the example has one.
4. Ensure every requirement ID from the table appears in at least one case (≥1 positive + ≥1 negative).
5. Write the file to `doc-as-code/test-cases/<feature>.xml` (create the directory if missing).
6. **Validate** (mandatory):
   ```bash
   python3 scripts/validate_xml.py doc-as-code/test-cases/<feature>.xml assets/test-cases-example.xml
   ```
   Fix and re-run until it prints `VALID`.
7. Output a traceability table: `Требование → Тест-кейсы`.

## XML hygiene

- Escape `&` `<` `>` `"` as `&amp; &lt; &gt; &quot;` inside text.
- UTF-8 encoding; keep the XML declaration if the example has one.
- One logical scenario per case — do not merge unrelated checks.
- Keep Russian text in titles/steps/expected results.

## Rules

- ✅ **MIRROR** the example structure exactly (names, order, nesting).
- ✅ **TRACE** every case to a requirement ID; no orphan cases.
- ✅ **COVER** each requirement with positive + negative + boundary where applicable.
- ✅ **VALIDATE** with `validate_xml.py` before finishing — must be `VALID`.
- ❌ **DO NOT** invent schema elements not present in the example.
- ❌ **DO NOT** drop elements that exist in the example.

## References

- `references/xml-format.md` — element-by-element mapping of case fields → XML nodes.
- `references/examples.md` — filled positive/negative/boundary case examples.
