# Data Model: Reference Data API — Contract Types

**Feature**: `004-contract-types-reference-data` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one more **instance** of each concept [002's data model](../002-reference-data-api/data-model.md) defines: a `ReferenceDataType` and its `ReferenceDataRecord`s. It also extends those concepts generically with optional **boolean fields** (§ 1a; spec FR-017, FR-018), alongside [003's reference fields](../003-base-locations-reference-data/data-model.md#1a-reference-fields-generic). Every other field rule, load-time check, DTO property and mapping is 002's and 003's, unchanged.

---

## 1. The new ReferenceDataType

| Field | Value |
|-------|-------|
| `name` | `contract_types` |
| `aliases` | none: `contract_type` is not served (spec Clarifications) |
| `fixture` | `classpath:reference-data/contract_types.json` |
| `reference-fields` | none |
| `boolean-fields` | `salaried` (§ 1a) |
| `unique-names` | default (`true`) |

**Shipped configuration** (`jo.reference-data.types`, in this order):

| # | `name` | `aliases` | `reference-fields` | `boolean-fields` | `unique-names` | Records |
|---|--------|-----------|--------------------|------------------|----------------|---------|
| 1 | `appointment_titles` | `appointment_title` | none | none | `true` | 194 |
| 2 | `base_locations` | none | `type_id`, `parent_id`, `jurisdiction_id` | none | `false` | 1,462 |
| 3 | `contract_types` | none | none | `salaried` | `true` | 14 |

OpenAPI `enum` order: `appointment_titles`, `base_locations`, `contract_types`, `appointment_title`.

Every other E-Links attribute name (eight canonical, their eight aliases, `base_location` and `contract_type`) stays unsupported and resolves to `400` (spec FR-002).

---

## 1a. Boolean fields (generic)

A **boolean field** holds a yes/no attribute of a record. There is one, modelled by the `BooleanField` enum (`entity/`):

| `BooleanField` | JSON / config name | Java (`ReferenceDataRecord`, `ReferenceDataResponse`) | Meaning |
|----------------|--------------------|--------------------------------------------------------|---------|
| `SALARIED` | `salaried` | `Boolean salaried` | The contract is salaried, including salaried part-time working |

- **Declaration**: a type's configuration entry lists the boolean fields its records carry under `boolean-fields` (`ReferenceDataProperties.TypeProperties.booleanFields`, default empty). `ReferenceDataTypeRegistry` resolves the names into `ReferenceDataType.booleanFields` (a `Set<BooleanField>`). An unknown name (exact, case-sensitive match), a repeated name or an empty entry stops start-up with `IllegalStateException`.
- **Load-time validation** (`FixtureReferenceDataRepository`, after 002's field rules and 003's reference-field rules): the value MUST be a JSON boolean — a number, string, empty string or `null` fails while reading (research R3). A record of a type that declares the field MUST carry it (`salaried is required`); a record of a type that doesn't MUST NOT (`salaried is not a boolean field of <type>`).
- **Response**: `ReferenceDataResponse.salaried`, a nullable `Boolean` annotated `@JsonInclude(NON_NULL)`, after the reference fields and before `created_at`. It is written only when the record carries it, so appointment-title and base-location responses are unchanged. In the generated OpenAPI schema it is optional (`type: boolean`, not nullable, not in `required`) and described as deviation D-3.

---

## 2. ContractType records

Each record is a `ReferenceDataRecord` with 002's six fields and rules (positive unique `id`, trimmed non-blank `name`, unique within the type, whole-second UTC timestamps, `created_at ≤ updated_at`, `end_date ≥ start_date` when present), plus `salaried` (§ 1a). Response field order: `id`, `name`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

**Identity**: `id` is unique within `contract_types` only. The same number in another type is a different record (spec AC-009, EC-006).

**Relationships**: none outgoing. Incoming, later: appointments will carry `contract_type_id` and `contract_type`, which MUST equal a served record's `id` and `name` (spec DF-2).

**State**: none; records are immutable and read-only.

---

## 3. Fixture generation rules

`src/main/resources/reference-data/contract_types.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth; it is not regenerated during builds (FR-007).

1. **Input**: the `name` and `salaried` columns of the local, gitignored extract `joh-elinks-api/ReferenceData/REF_ContractType.csv` (14 rows), read as UTF-8. Every other column is ignored, including `id` (blank for the "Salaried" row) and the dates (spec EC-001, EC-004).
2. **Every row**: one record per row, 14 in all, 14 distinct names, all ASCII and already trimmed. No filtering: "Unknown" is kept (EC-003).
3. **`salaried`**: `1` → `true`; blank → `false`; any other value aborts generation (FR-008).
4. **Order**: code-point order of `name` (upper-case before lower-case, so `SPTW-…` precedes `Salaried`). Positions 1 to 14.
5. **`id`**: `10 × position` — 10, 20, … 140.
6. **Records where `position % 7 == 0`** (positions 7 and 14): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
7. **All other records** (12): `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
8. **Every record**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
9. **Layout**: a JSON array, two-space indented, fields in the order `id`, `name`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date`, with a trailing newline.

Rules 4 to 8 are 002's rules unchanged.

**Full dataset**:

| Position | `id` | `name` | `salaried` | `end_date` |
|----------|------|--------|------------|------------|
| 1 | 10 | Fee-paid | false | null |
| 2 | 20 | SPTW-10% | true | null |
| 3 | 30 | SPTW-20% | true | null |
| 4 | 40 | SPTW-30% | true | null |
| 5 | 50 | SPTW-40% | true | null |
| 6 | 60 | SPTW-50% | true | null |
| 7 | 70 | SPTW-60% | true | 2025-03-31 |
| 8 | 80 | SPTW-70% | true | null |
| 9 | 90 | SPTW-75% | true | null |
| 10 | 100 | SPTW-80% | true | null |
| 11 | 110 | SPTW-90% | true | null |
| 12 | 120 | Salaried | true | null |
| 13 | 130 | Unknown | false | null |
| 14 | 140 | Voluntary | false | 2025-03-31 |

**Known reference points** (spec § Reference Points; used by tests and Postman): ids 10, 20, 70, 120, 130 and 140.

**Id gaps**: every id is a multiple of 10, so `15`, `150` and `999999` are unknown ids (`404`).

---

## 4. Verifying the fixture without the extract

The rule test (research R4) checks rules 3 to 9 and the reference points directly against the checked-in file, so a hand edit that breaks the pattern fails the unit suite. Start-up validation (§ 1a and 002) separately rejects any record that breaks the field rules.
