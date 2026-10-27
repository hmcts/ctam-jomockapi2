# Data Model: Reference Data API — Judiciary Roles

**Feature**: `006-judiciary-roles-reference-data` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one more **instance** of each concept [002's data model](../002-reference-data-api/data-model.md) defines: a `ReferenceDataType` and its `ReferenceDataRecord`s. It extends no concept: JudiciaryRole records have only 002's fields, so every field rule, load-time check, DTO property and mapping is 002's, unchanged, as for Gender in [005](../005-genders-reference-data/data-model.md).

---

## 1. The new ReferenceDataType

| Field | Value |
|-------|-------|
| `name` | `judiciary_roles` |
| `aliases` | none: `judiciary_role` is not served (spec FR-001) |
| `fixture` | `classpath:reference-data/judiciary_roles.json` |
| `reference-fields` | none |
| `boolean-fields` | none |
| `unique-names` | default (`true`) |

**Shipped configuration** (`jo.reference-data.types`, in this order):

| # | `name` | `aliases` | `reference-fields` | `boolean-fields` | `unique-names` | Records |
|---|--------|-----------|--------------------|------------------|----------------|---------|
| 1 | `appointment_titles` | `appointment_title` | none | none | `true` | 194 |
| 2 | `base_locations` | none | `type_id`, `parent_id`, `jurisdiction_id` | none | `false` | 1,462 |
| 3 | `contract_types` | none | none | `salaried` | `true` | 14 |
| 4 | `genders` | none | none | none | `true` | 4 |
| 5 | `judiciary_roles` | none | none | none | `true` | 164 |

OpenAPI `enum` order: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `appointment_title`.

Every other E-Links attribute name (six canonical, their six singular forms, `base_location`, `contract_type`, `gender` and `judiciary_role`) stays unsupported and resolves to `400` (spec FR-002).

---

## 2. JudiciaryRole records

Each record is a `ReferenceDataRecord` with 002's six fields and rules (positive unique `id`, trimmed non-blank `name`, unique within the type, whole-second UTC timestamps, `created_at ≤ updated_at`, `end_date ≥ start_date` when present). Response field order: `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

**Name**: any Unicode text that passes 002's rules. 002's trim check uses `String.strip()`, which every name passes (none has leading or trailing whitespace), and nothing restricts names to ASCII, so `’` and `–` need no change to start-up validation. Uniqueness is exact string equality, so names that differ only by apostrophe or dash would be distinct; none do (spec EC-003).

**Identity**: `id` is unique within `judiciary_roles` only. The same number in another type is a different record (spec AC-009, EC-010).

**Relationships**: none outgoing. Incoming, later: office holders' `judiciary_roles` entries will refer to these records through `judiciary_role_id` or `judiciary_role_name_id`, which the People feature chooses and which MUST equal a served record's `id` (spec DF-4).

**State**: none; records are immutable and read-only. 23 records are ended by § 3 rule 5; that is a fixed attribute of the record, not a transition.

---

## 3. Fixture generation rules

`src/main/resources/reference-data/judiciary_roles.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth; it is not regenerated during builds (FR-007).

1. **Input**: the `name` column of the local, gitignored extract `joh-elinks-api/ReferenceData/REF_JudiciaryRole.csv` (164 rows), read as UTF-8. Every other column is ignored, including the extract's ids (90000–90163), `start_date`, `end_date`, `created_at` and `updated_at` (spec EC-001, EC-007).
2. **Every row, exactly**: one record per row, 164 in all, 164 distinct names. Each name is copied as its code points: nothing trimmed, case-folded, corrected, shortened, normalised (no Unicode normalisation, no apostrophe or dash replacement) or filtered (spec FR-008, EC-002 to EC-006).
3. **Order**: code-point order of `name`. Positions 1 to 164.
4. **`id`**: `10 × position` — 10 to 1640.
5. **Records where `position % 7 == 0`** (23 records, ids 70, 140, …, 1610): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
6. **All other records** (141): `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
7. **Every record**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
8. **Layout**: a JSON array, two-space indented, fields in the order `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`, UTF-8 without a byte-order mark, non-ASCII characters written literally (never as `\u` escapes), with a trailing newline — `appointment_titles.json`'s layout. About 34 KB.

Rules 3 to 7 are 002's rules unchanged.

**Reference points** (spec Reference Points; ended rows marked):

| Position | `id` | `name` | Ended |
|----------|------|--------|-------|
| 1 | 10 | Acting Designated Civil Judge | |
| 7 | 70 | Acting Senior Costs Judge | yes |
| 14 | 140 | Boundary Commission for England – Deputy Chair | yes |
| 15 | 150 | Boundary Commission for Wales – Deputy Chair | |
| 17 | 170 | Business & Property Courts Supervising | |
| 34 | 340 | Course Director for COP (JC) | |
| 96 | 960 | Judge in Charge of Jury List (King’s Bench) | |
| 97 | 970 | Judge in Charge of Jury List (Queen's Bench) | |
| 107 | 1070 | Judicial Commissioner Investigatory Powers Commiss | |
| 128 | 1280 | Principle Judge - Land Registration | |
| 140 | 1400 | Senior Family Liaison Judge | yes |
| 161 | 1610 | Vice President of the Unified Tribunals | yes |
| 164 | 1640 | Wales Training Course Director (JC) | |

**Non-ASCII names** (7, all kept literally): "Boundary Commission for England – Deputy Chair" (140), "Boundary Commission for Wales – Deputy Chair" (150), "Deputy Chairman – Advisory Committee" (450), "Director of Magistrates’ Training (JC)" (580), "Judge in Charge of Jury List (King’s Bench)" (960), "Judge in Charge of Non-Jury List (King’s Bench)" (990) and "Magistrates’ Liaison Judge" (1180).

**Id gaps**: every id is a multiple of 10, so `15`, `1650` and `999999` are unknown ids (`404`).

---

## 4. Verifying the fixture

**Once, against the extract** (research R2): the generating script confirms the names equal the extract's `name` column as code points, the counts (164 records, 164 distinct names, 7 non-ASCII, 23 ended), and every reference point, before the file is checked in. The result is recorded in the plan's implementation notes. This is how SC-003 is met; the extract is not available to the build.

**On every build, without the extract**:
- `ReferenceDataFixtureRulesTest` checks rules 3 to 7, the field order of rule 8, the record and distinct-name counts and every reference point against the checked-in file, so a hand edit that breaks the pattern fails the unit suite.
- Its new generic check (research R3) fails if any fixture contains a `\u` escape, which enforces rule 8's "written literally". The file's indent and trailing newline are still not checked: they don't affect the API, whose responses are re-serialised.
- The functional suite's golden file pins all 164 names as served (research R5).
- Start-up validation (002) separately rejects any record that breaks the field rules.
