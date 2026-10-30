# Data Model: Reference Data API — Tickets

**Feature**: `010-tickets-reference-data` | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one declarable reference field, `ticket_category_id`, and one type, Ticket. Everything else in 002's to 009's data models is unchanged.

---

## 1. Reference fields (extended, generic)

A type may declare any of these reference fields in `reference-fields`, and any declared one in `nullable-reference-fields` (003, 007). The set is closed; an unknown name stops start-up.

| Field | Added by | Refers to (E-Links numbering) | Declared by |
|-------|----------|-------------------------------|-------------|
| `type_id` | 003 | `location_types` | `base_locations`, `locations` |
| `parent_id` | 003 | `locations` | `base_locations`, `locations` (nullable) |
| `jurisdiction_id` | 003 | `jurisdictions` | `base_locations`, `locations`, `location_types` |
| `ticket_category_id` | **010** | `ticket_categories` (not served yet; DF-8) | `tickets` |

Rules for `ticket_category_id` are those of every reference field (spec FR-019; research R2):
- **Declaring type**: present in every record as a positive JSON integer; `null` only if the type also declares it nullable (Ticket doesn't).
- **Any other type**: absent from every record, not even `null`.
- **Violation**: start-up fails with `Invalid reference-data fixture <fixture>: record id <id>: ticket_category_id is required`, `… must be greater than 0` or `… is not a reference field of <type>`.
- **Response**: written after `jurisdiction_id` and before the text and boolean fields, only for declaring types (research R3).

---

## 2. The type declaration

Ticket is declared in `application.yml` (research R1):

| Element | Value |
|---------|-------|
| `name` | `tickets` |
| `aliases` | none (`ticket` is not served) |
| `fixture` | `classpath:reference-data/tickets.json` |
| `reference-fields` | `[ticket_category_id]` |
| `nullable-reference-fields` | none |
| `boolean-fields` | none |
| `text-fields` | none |
| `unique-names` | `false` (7 names repeat) |

---

## 3. Ticket records

Each record is a `ReferenceDataRecord` with 002's field rules plus the declared reference field. Response field order: `id`, `name`, `ticket_category_id`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

| Field | Source | Rule |
|-------|--------|------|
| `id` | extract, unchanged | positive, unique; 289 to 1709, with gaps (DR-1) |
| `name` | extract, unchanged | trimmed, non-blank; 152 distinct across 159 records |
| `ticket_category_id` | extract, unchanged | positive integer, never null; 41 distinct values, 31 to 88 |
| `created_at`, `updated_at`, `start_date`, `end_date` | synthetic (002) | see § 4 |

**Relationships**:
- **Ticket `ticket_category_id` → TicketCategory**: 159 records, 41 distinct values. A forward reference: `ticket_categories` is not served (DF-8). Confirmed once, at generation, against the local ticket-category extract; pinned on every build by `TICKET_CATEGORY_IDS` (research R6, R7).
- **People authorisation `ticket_id` → Ticket**: future (DF-9).
- **Anything served → Ticket**: none today.

**Repeated names**: each repeated name is held by two records in different categories (spec EC-002):

| Name | Ids (category) |
|------|----------------|
| Claims Management Services | 323 (39), 1561 (47) |
| Community Rights to Bid | 324 (42), 1493 (39) |
| Consumer Credit | 326 (39), 1560 (47) |
| Financial Remedy | 409 (31), 410 (33) |
| National Security | 346 (39), 1527 (78) |
| Race Panel | 351 (39), 1526 (56) |
| Reserve Forces | 353 (41), 1491 (56) |

**State**: none. Records are read-only.

---

## 4. Fixture generation rules

`src/main/resources/reference-data/tickets.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth (FR-007).

1. **Input**: the local, gitignored `joh-elinks-api/ReferenceData/REF_Ticket.csv`, read as UTF-8 with a CSV parser: a header and 159 rows of 7 columns. Use `id`, `name` and `ticket_category_id`; ignore `start_date`, `end_date`, `created_at` and `updated_at` (spec EC-005).
2. **Every row, exactly**: one record per row, with `id`, `name` and `ticket_category_id` copied as they are (ids and category ids as JSON integers). Nothing is trimmed, case-folded or corrected.
3. **Order**: ascending `id`. Positions run from 1 to 159 in that order (DR-1).
4. **Records where `position % 7 == 0`** (22 records; ids in spec AC-003): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
5. **All other records**: `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
6. **All records**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
7. **Layout**: a JSON array, two-space indented, fields in § 3's order, every record with all seven keys, UTF-8 without a BOM, no `\u` escapes, with a trailing newline.

Rules 4 to 7 are 002's. Rules 1 to 3 are 007's extract-id rule.

**Reference points** (the spec's table):

| Position | `id` | `name` | `ticket_category_id` | `end_date` | `updated_at` |
|----------|------|--------|----------------------|------------|--------------|
| 1 | 289 | Welsh Language | 88 | null | 2024-06-03T10:30:00Z |
| 2 | 290 | Administrative Court | 31 | null | 2024-06-03T10:30:00Z |
| 6 | 294 | Civil Authorisation | 31 | null | 2024-06-03T10:30:00Z |
| 7 | 295 | Commercial | 31 | 2025-03-31 | 2025-04-01T08:00:00Z |
| 75 | 365 | 03 - Disability Living Allowance | 36 | null | 2024-06-03T10:30:00Z |
| 77 | 367 | 05 - Industrial Injuries | 36 | 2025-03-31 | 2025-04-01T08:00:00Z |
| 112 | 409 | Financial Remedy | 31 | 2025-03-31 | 2025-04-01T08:00:00Z |
| 113 | 410 | Financial Remedy | 33 | null | 2024-06-03T10:30:00Z |
| 159 | 1709 | Valuation Tribunal Wales | 83 | null | 2024-06-03T10:30:00Z |

**Unknown ids**: `1`, `288`, `297`, `1710` and `999999`.

**Category ids** (`TICKET_CATEGORY_IDS`, 41 values): 31, 32, 33, 36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 49, 50, 52, 54, 55, 56, 57, 58, 59, 62, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 80, 82, 83, 85, 88.

---

## 5. Verifying the fixture

**Once, against the extracts** (research R6): the generating script checks the extract's shape, that the three copied values equal the extract's row for row, the 22 ended ids, that the 41 category ids are exactly the set above and each is an id in the local ticket-category extract, the 7 repeated-name pairs, and every reference point. It does this before the file is checked in, and the plan's implementation notes record the result. This is how SC-003 and SC-004 are met.

**On every build, without the extracts**:
- `ReferenceDataFixtureRulesTest` checks every fixture, including this one under the `SOURCE` id rule: the date rule by position, the field order (with the declared reference field), the counts, every reference point, the category-id set and that no `\u` escape appears (research R7).
- `ReferenceDataReferencesTest`'s completeness check requires a target type for `ticket_category_id`, and will require a resolution row once `ticket_categories` is served (research R8).
- Start-up validation rejects any record that breaks 002's field rules, lacks `ticket_category_id` or carries an undeclared field.
- The functional suite's golden file pins the whole served collection.
