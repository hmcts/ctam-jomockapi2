# Data Model: Reference Data API — Base Locations

**Feature**: `003-base-locations-reference-data` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds no new classes, fields or DTOs. It adds one **instance** of each concept [002's data model](../002-reference-data-api/data-model.md) defines: one more `ReferenceDataType` and its set of `ReferenceDataRecord`s. Field rules, load-time validation, DTO shapes and mapping are 002's, unchanged.

---

## 1. The new ReferenceDataType

| Field | Value |
|-------|-------|
| `name` | `base_locations` |
| `aliases` | none: `base_location` is not served (spec Clarifications) |
| `fixture` | `classpath:reference-data/base_locations.json` |

**Shipped configuration** (`jo.reference-data.types`, in this order):

| # | `name` | `aliases` | Records |
|---|--------|-----------|---------|
| 1 | `appointment_titles` | `appointment_title` | 194 |
| 2 | `base_locations` | none | 1,462 |

The order decides the order of names in the OpenAPI `enum`: canonical names in configuration order, then aliases (`appointment_titles`, `base_locations`, `appointment_title`). The registry's existing start-up rules hold: the three strings are distinct, and no type lists its own name as an alias.

Every other E-Links attribute name (nine canonical, their nine aliases, and `base_location`) stays unsupported and resolves to `400` (spec FR-002).

---

## 2. BaseLocation records

Each record is a `ReferenceDataRecord` with 002's six fields and rules (positive unique `id`, trimmed non-blank `name` that need not be unique (spec FR-017, research R10), whole-second UTC timestamps, `created_at ≤ updated_at`, `end_date ≥ start_date` when present). No field is added: the extract's `type_id`, `parent_id` and `jurisdiction_id` are not stored (spec DF-1, FR-005).

**Identity**: `id` is unique within `base_locations` only, and is the only identity: 120 names are shared by several records (e.g. `Bedfordshire LJA`, ids 540 and 550; `National`, 40 records from id 7200 to 7590), each a different real location (spec EC-001). The same number in `appointment_titles` is a different record (spec AC-008, EC-005); lookups are always made within the resolved type.

**Relationships**: none in this feature (DF-1). The ids are stable (FR-011), so a later feature can refer to them. When parent references arrive, they are what tell records with the same name apart.

**State**: none; records are immutable and read-only.

---

## 3. Fixture generation rules

`src/main/resources/reference-data/base_locations.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth; it is not regenerated during builds (FR-007).

1. **Input**: the `name` column of the local, gitignored extract `joh-elinks-api/ReferenceData/REF_BaseLocation.csv` (1,462 rows), read as UTF-8. Every other column is ignored (FR-006).
2. **Every row**: one name per row, **1,462** in all, with repeats kept (Clarifications; FR-008). Names are compared exactly — character-for-character and case-sensitively, with no trimming beyond confirming none is needed, and no collapsing of internal spaces — which gives **1,276** distinct names. Two pairs that differ only by spacing or capitalisation are different names:
   - `Cheshire  LJA` (two spaces) and `Cheshire LJA`
   - `City of Westminster Sub Committee` and `City of Westminster Sub committee`
3. **No filtering**: the seven placeholder-style names ("Unknown" (two rows), "Unknown Sub Committee S2", "S3", "S4", "S5", "S9", "S94") are kept (Clarifications).
4. **Characters**: names are copied unchanged. The one non-ASCII name keeps its en dash, U+2013 (Clarifications Q3). The file is UTF-8 without a byte-order mark, with characters written literally rather than as `\u` escapes (research R2).
5. **Order**: code-point order. Positions run from 1 to 1,462. Rows with the same name are adjacent; which of them comes first doesn't matter, because they produce identical records apart from position.
6. **`id`**: `10 × position` — 10, 20, … 14,620.
7. **Records where `position % 7 == 0`** (208 records): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
8. **All other records** (1,254): `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
9. **Every record**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
10. **Layout**: a JSON array, two-space indented, fields in the order `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`, with a trailing newline — the same layout as `appointment_titles.json`.

Rules 5 to 9 are 002's rules unchanged. Every value meets 002's FR-022 rules; only 002's data-model rule that names are unique is dropped (FR-017).

**Known reference points** (spec § Reference Points; used by tests and Postman):

| Position | `id` | `name` | `end_date` | `updated_at` |
|----------|------|--------|------------|--------------|
| 1 | 10 | Aberconwy | null | 2024-06-03T10:30:00Z |
| 7 | 70 | Aldridge and Brownhills | 2025-03-31 | 2025-04-01T08:00:00Z |
| 1,029 | 10290 | Royal Courts of Justice – Office of the Judge Advocate General | null | 2024-06-03T10:30:00Z |
| 1,462 | 14620 | Yorkshire & Humberside | null | 2024-06-03T10:30:00Z |

**Placeholder rows**: the eight "Unknown…" rows sit at positions 1,313 to 1,320 (ids 13130 to 13200) and follow rules 7 and 8 like every other record. Position 1,316 is `7 × 188`, so `Unknown Sub Committee S3` (id 13160) happens to be an ended record; tests need not single it out, because the rule test (research R4) covers every record.

**Id gaps**: every id is a multiple of 10, so `15`, `8885` and anything above `14620` are unknown ids (`404`).

---

## 4. Verifying the fixture without the extract

The rule test (research R4) checks rules 5 to 9 and the reference points directly against the checked-in file, so a hand edit that breaks the pattern fails the unit suite. Start-up validation (002) separately rejects any record that breaks the field rules.
