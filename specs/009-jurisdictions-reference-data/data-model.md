# Data Model: Reference Data API — Jurisdictions

**Feature**: `009-jurisdictions-reference-data` | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one type, Jurisdiction, and no new declaration. Everything else in 002's to 008's data models is unchanged.

---

## 1. The type declaration

Jurisdiction is declared in `application.yml` (research R1):

| Element | Value |
|---------|-------|
| `name` | `jurisdictions` |
| `aliases` | none (`jurisdiction` is not served) |
| `fixture` | `classpath:reference-data/jurisdictions.json` |
| `reference-fields` | none |
| `nullable-reference-fields` | none |
| `boolean-fields` | none |
| `text-fields` | none |
| `unique-names` | default (`true`) |

---

## 2. Jurisdiction records

Each record is a `ReferenceDataRecord` with 002's field rules and no declared fields. Response field order: `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

| Field | Source | Rule |
|-------|--------|------|
| `id` | extract, unchanged | positive, unique; 30, 34, 84, 86, 87, with gaps (DR-1) |
| `name` | extract, unchanged | trimmed, non-blank, unique (5 distinct) |
| `created_at`, `updated_at`, `start_date`, `end_date` | synthetic (002) | see § 3 |

**Relationships** (all checked by `ReferenceDataReferencesTest`):
- **Location `jurisdiction_id` → Jurisdiction**: 2,000 records, 4 distinct values (30, 34, 84, 86), every one an id here. Closes 007 DF-5.
- **BaseLocation `jurisdiction_id` → Jurisdiction**: 1,462 records, the same 4 values. Closes 003 DF-1.
- **LocationType `jurisdiction_id` → Jurisdiction**: 16 records, the same 4 values. Closes 008 DF-7.
- **Jurisdiction → anything**: none. Jurisdiction carries no reference field.
- **Unreferenced**: 87 "Skills" is referenced by no shipped record (spec EC-003).

**State**: none. Records are read-only; none is ended.

---

## 3. Fixture generation rules

`src/main/resources/reference-data/jurisdictions.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth (FR-007).

1. **Input**: the local, gitignored `joh-elinks-api/ReferenceData/REF_Jurisdiction.json`, read as UTF-8 with a JSON parser: an object whose `results` array holds 5 objects. Use `id` and `name`; ignore `start_date`, `end_date`, `created_at` and `updated_at` (spec EC-005).
2. **Every entry, exactly**: one record per entry, with `id` and `name` copied as they are. Nothing is trimmed, case-folded or corrected.
3. **Order**: ascending `id`. Positions run from 1 to 5 in that order (DR-1).
4. **Records where `position % 7 == 0`**: none, for 5 records.
5. **All records**: `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`, `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
6. **Layout**: a JSON array, two-space indented, fields in § 2's order, every record with all six keys, UTF-8 without a BOM, with a trailing newline.

Rules 4 to 6 are 002's. Rules 1 to 3 are 007's extract-id rule.

**Records** (the whole dataset; the spec's Records table):

| Position | `id` | `name` | Referenced by (locations / base locations / location types) |
|----------|------|--------|-------------------------------------------------------------|
| 1 | 30 | Courts | 380 / 368 / 2 |
| 2 | 34 | Tribunals | 221 / 169 / 3 |
| 3 | 84 | Magistrates | 1,207 / 826 / 8 |
| 4 | 86 | Coroners | 192 / 99 / 3 |
| 5 | 87 | Skills | 0 / 0 / 0 |

**Unknown ids**: `1`, `31`, `88`, `999` and `999999`.

---

## 4. Verifying the fixture

**Once, against the extract** (research R2): the generating script checks the extract's shape, that the two copied values equal the extract's entry for entry, that no record is ended, that every `jurisdiction_id` in the shipped location fixtures resolves, and every row of § 3. It does this before the file is checked in, and the plan's implementation notes record the result. This is how SC-003 is met.

**On every build, without the extract**:
- `ReferenceDataFixtureRulesTest` checks every fixture, including this one under the `SOURCE` id rule: the date rule by position, the field order, the counts, every reference point and that no `\u` escape appears (research R3).
- `ReferenceDataReferencesTest` checks that every Location, BaseLocation and LocationType `jurisdiction_id` resolves to a jurisdiction, and that every reference between served types is listed (research R4).
- Start-up validation rejects any record that breaks 002's field rules or carries an undeclared field.
- The functional suite's golden file pins the whole served collection.
