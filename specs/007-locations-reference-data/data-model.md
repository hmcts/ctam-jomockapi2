# Data Model: Reference Data API — Locations

**Feature**: `007-locations-reference-data` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one more **instance** of each concept [002's data model](../002-reference-data-api/data-model.md) defines, a `ReferenceDataType` and its `ReferenceDataRecord`s, with 003's reference fields. It **extends** one concept: a type may declare reference fields that can be null (§ 2). Everything else (the shared field rules, load-time checks, `boolean-fields` and `unique-names`) is unchanged.

---

## 1. The new ReferenceDataType

| Field | Value |
|-------|-------|
| `name` | `locations` |
| `aliases` | none: `location` is not served (spec FR-001) |
| `fixture` | `classpath:reference-data/locations.json` |
| `reference-fields` | `type_id`, `parent_id`, `jurisdiction_id` |
| `nullable-reference-fields` | `parent_id` (new; § 2) |
| `boolean-fields` | none |
| `unique-names` | `false` |

**Shipped configuration** (`jo.reference-data.types`, in this order):

| # | `name` | `aliases` | `reference-fields` | `nullable-reference-fields` | `boolean-fields` | `unique-names` | Records |
|---|--------|-----------|--------------------|-----------------------------|------------------|----------------|---------|
| 1 | `appointment_titles` | `appointment_title` | none | none | none | `true` | 194 |
| 2 | `base_locations` | none | `type_id`, `parent_id`, `jurisdiction_id` | none | none | `false` | 1,462 |
| 3 | `contract_types` | none | none | none | `salaried` | `true` | 14 |
| 4 | `genders` | none | none | none | none | `true` | 4 |
| 5 | `judiciary_roles` | none | none | none | none | `true` | 164 |
| 6 | `locations` | none | `type_id`, `parent_id`, `jurisdiction_id` | `parent_id` | none | `false` | 2,000 |

OpenAPI `enum` order: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `appointment_title`.

Every other E-Links attribute name stays unsupported and resolves to `400` (spec FR-002). That is the five remaining canonical names, their five singular forms, and `base_location`, `contract_type`, `gender`, `judiciary_role` and `location`.

---

## 2. Nullable reference fields (generic; spec FR-011)

**Declaration**: `nullable-reference-fields`, a list of reference-field names (`type_id`, `parent_id`, `jurisdiction_id`), defaulting to empty. Start-up fails with `IllegalStateException` (research R2) if an entry is:
- empty;
- unknown;
- repeated;
- not also in the type's `reference-fields`.

**Model changes**:

| Element | Change |
|---------|--------|
| `ReferenceDataProperties.TypeProperties` | + `List<String> nullableReferenceFields` (null → empty) |
| `ReferenceDataType` | + `Set<ReferenceField> nullableReferenceFields` (unmodifiable `EnumSet`; always a subset of `referenceFields`) |
| `ReferenceDataRecord` | `type_id`, `parent_id` and `jurisdiction_id` no longer fail inside Jackson on an explicit `null`; the repository decides (below) |
| `ReferenceDataResponse` | `typeId`, `parentId` and `jurisdictionId` become `ReferenceFieldValue` (new, `domain/`), a single `@JsonValue Long value` |
| `ReferenceDataMapper` | takes the `ReferenceDataType` as context: `null` for an undeclared field, `new ReferenceFieldValue(value)` for a declared one |

**Fixture rule** (per record, per reference field; research R3):

| The type… | Key absent | `null` | Positive integer |
|-----------|-----------|--------|------------------|
| declares it nullable | invalid: `<field> is required` | valid | valid |
| declares it, not nullable | invalid: `<field> is required` | invalid: `<field> is required` | valid |
| doesn't declare it | valid | invalid: `<field> is not a reference field of <type>` | invalid: same message |

Zero, negative, fractional and string values stay invalid for every type, with the existing messages.

**Response rule**:

| The type… | Record value | Response |
|-----------|--------------|----------|
| doesn't declare the field | — | key left out (as today) |
| declares it | a positive id | `"<field>": <id>` (as today) |
| declares it nullable | null | `"<field>": null` |

So `appointment_titles`, `base_locations`, `contract_types`, `genders` and `judiciary_roles` responses are byte-for-byte unchanged.

**OpenAPI** (`ReferenceDataResponse`): `type_id`, `parent_id` and `jurisdiction_id` stay optional `int64` integers. `parent_id` becomes nullable, and its description says it is null for a top-level record. No `ReferenceFieldValue` schema is published.

---

## 3. Location records

Each record is a `ReferenceDataRecord` with 002's field rules and 003's reference fields. Response field order: `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

| Field | Source | Rule |
|-------|--------|------|
| `id` | extract, unchanged | positive, unique, 1 to 2433 with gaps (DR-1) |
| `name` | extract, unchanged | trimmed, non-blank, repeats allowed; internal spacing and the en dash kept |
| `type_id` | extract, unchanged | positive; 16 distinct values (1 to 16); forward reference to `location_types` (DF-5) |
| `parent_id` | extract, unchanged; empty → `null` | positive or null; when non-null, the `id` of a location (FR-012) |
| `jurisdiction_id` | extract, unchanged | one of 30, 34, 84, 86; forward reference to `jurisdictions` (DF-5) |
| `created_at`, `updated_at`, `start_date`, `end_date` | synthetic (002) | see § 4 |

**Relationships**:
- **`parent_id` → Location** (self-reference): 1,858 non-null values over 538 distinct parents; 142 top-level records with a null parent, all of types 1, 3, 6 or 14. The maximum depth is 2, and there are no cycles or self-parents.
- **BaseLocation `parent_id` → Location** (003 DF-1, closed for `parent_id` here): 408 distinct values in the checked-in `base_locations.json`, every one a location id.
- **`type_id` → LocationType** and **`jurisdiction_id` → Jurisdiction**: forward references (DF-5), not checked until those types are served.
- **BaseLocation ids**: not related. BaseLocation keeps `10 × position` (spec EC-009).

**State**: none. Records are read-only. `end_date` marks a record as ended, by the synthetic rule only, and a parent may be ended while its children are open (spec EC-006).

---

## 4. Fixture generation rules

`src/main/resources/reference-data/locations.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth (FR-007).

1. **Input**: the local, gitignored `joh-elinks-api/ReferenceData/REF_Location.csv` (2,000 rows), read as UTF-8. Use the columns `id`, `name`, `type_id`, `parent_id` and `jurisdiction_id`; ignore `start_date`, `end_date`, `created_at` and `updated_at` (spec EC-007).
2. **Every row, exactly**: one record per row, with the five columns copied as they are. An empty `parent_id` becomes `null`. Names are not trimmed, merged, case-folded, corrected or normalised.
3. **Order**: ascending `id`. Positions run from 1 to 2,000 in that order (DR-1).
4. **Records where `position % 7 == 0`** (285 records, ids 9 … 2364): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
5. **All other records** (1,715): `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
6. **Every record**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
7. **Layout**: a JSON array, two-space indented, fields in § 3's order, every record with all nine keys (including `"parent_id": null`), UTF-8 without a BOM, non-ASCII written literally, with a trailing newline. That is about 534 KB.

Rules 4 to 7 are 002's (and 003's field order). Rules 1 to 3 differ from 002's: the extract's ids are kept and records are ordered by them, not by name.

**Reference points**: the spec's Reference Points table (12 rows). Ended records have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z.

| Position | `id` | `name` | `type_id` | `parent_id` | `jurisdiction_id` | Ended |
|----------|------|--------|-----------|-------------|-------------------|-------|
| 1 | 1 | Aberconwy | 8 | 1722 | 84 | |
| 7 | 9 | Ashby-de-la-Zouch and Market Bosworth | 8 | 1732 | 84 | yes |
| 8 | 10 | Atherstone and Coleshill | 8 | 1751 | 84 | |
| 989 | 1176 | `Cheshire  LJA` (two spaces) | 8 | 1697 | 84 | |
| 1102 | 1323 | Cheshire LJA | 8 | 1476 | 84 | |
| 1225 | 1466 | London | 6 | null | 84 | yes |
| 1241 | 1482 | London | 1 | null | 30 | |
| 1481 | 1722 | Old Gwynedd | 7 | 2141 | 84 | |
| 1900 | 2141 | Gwynedd | 6 | null | 84 | |
| 1995 | 2364 | Newcastle and North Tyneside | 16 | 1820 | 86 | yes |
| 1997 | 2398 | Royal Courts of Justice – Office of the Judge Advocate General | 2 | 1482 | 30 | |
| 2000 | 2433 | Kent and Medway | 16 | 1925 | 86 | |

**Id gaps**: `2`, `4` and `15` are unknown inside the range; `2434` and `999999` are past it.

---

## 5. Verifying the fixture

**Once, against the extract** (research R5): the generating script checks the counts, that the five copied columns equal the extract's row for row, the null parents, that references resolve (within locations and from the checked-in base locations), the ended ids, the literal characters and every reference point. It does this before the file is checked in, and the plan's implementation notes record the result. This is how SC-003 is met.

**On every build, without the extract**:
- `ReferenceDataFixtureRulesTest` checks every fixture:
  - with its id rule: `SOURCE` for this fixture, so ids are positive and strictly ascending; `POSITIONAL` for the others (research R6);
  - the date rule by position, the field order, and the reference-field shapes (null allowed only for declared-nullable fields);
  - the jurisdiction set, the counts and every reference point;
  - that no `\u` escape appears.
- `ReferenceDataReferencesTest` checks that every Location and BaseLocation `parent_id` resolves to a location, and that following parents has no cycles. It also fails if a reference field's target type is served but not listed (research R7).
- Start-up validation (§ 2) rejects any record that breaks the field rules.
- The functional suite's golden file pins the whole served collection.
