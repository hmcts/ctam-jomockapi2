# Data Model: Reference Data API — Genders

**Feature**: `005-genders-reference-data` | **Date**: 2026-10-21 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one more **instance** of each concept [002's data model](../002-reference-data-api/data-model.md) defines: a `ReferenceDataType` and its `ReferenceDataRecord`s. It extends no concept: Gender records have only 002's fields, so every field rule, load-time check, DTO property and mapping is 002's, unchanged.

---

## 1. The new ReferenceDataType

| Field | Value |
|-------|-------|
| `name` | `genders` |
| `aliases` | none: `gender` is not served (spec Clarifications) |
| `fixture` | `classpath:reference-data/genders.json` |
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

OpenAPI `enum` order: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `appointment_title`.

Every other E-Links attribute name (seven canonical, their seven aliases, `base_location`, `contract_type` and `gender`) stays unsupported and resolves to `400` (spec FR-002).

---

## 2. Gender records

Each record is a `ReferenceDataRecord` with 002's six fields and rules (positive unique `id`, trimmed non-blank `name`, unique within the type, whole-second UTC timestamps, `created_at ≤ updated_at`, `end_date ≥ start_date` when present). Response field order: `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

**Identity**: `id` is unique within `genders` only. The same number in another type is a different record (spec AC-008, EC-006).

**Relationships**: none outgoing. Incoming, later: current office holders will carry `gender_id`, which MUST equal a served record's `id` (spec DF-3).

**State**: none; records are immutable and read-only. No record is ended (§ 3 rule 5).

---

## 3. Fixture generation rules

`src/main/resources/reference-data/genders.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth; it is not regenerated during builds (FR-007).

1. **Input**: the `name` column of the local, gitignored gender reference extract (4 rows), read as UTF-8. Every other column is ignored, including the extract's ids (1, 3, 4, 5) and dates (spec EC-001).
2. **Every row**: one record per row, 4 in all, 4 distinct names, all ASCII and already trimmed. No filtering: "Other" and "Prefer not to say" are kept (EC-003).
3. **Order**: code-point order of `name`. Positions 1 to 4.
4. **`id`**: `10 × position` — 10, 20, 30, 40.
5. **Records where `position % 7 == 0`**: none. So no record is ended (spec Clarifications, EC-004).
6. **All records**: `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
7. **Every record**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
8. **Layout**: a JSON array, two-space indented, fields in the order `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`, with a trailing newline — `appointment_titles.json`'s layout.

Rules 3 to 7 are 002's rules unchanged.

**Full dataset** (also the reference points):

| Position | `id` | `name` | `end_date` |
|----------|------|--------|------------|
| 1 | 10 | Female | null |
| 2 | 20 | Male | null |
| 3 | 30 | Other | null |
| 4 | 40 | Prefer not to say | null |

**Id gaps**: every id is a multiple of 10, so `15`, `50` and `999999` are unknown ids (`404`).

---

## 4. Verifying the fixture without the extract

The rule test (research R2) checks rules 3 to 8 and every record directly against the checked-in file, so a hand edit that breaks the pattern fails the unit suite. Start-up validation (002) separately rejects any record that breaks the field rules.
