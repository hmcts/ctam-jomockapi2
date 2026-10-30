# Data Model: Reference Data API — Ticket Categories

**Feature**: `011-ticket-categories-reference-data` | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one declarable reference field, `parent_category_id`, and one type, TicketCategory. Everything else in 002's to 010's data models is unchanged.

---

## 1. Reference fields (extended, generic)

A type may declare any of these reference fields in `reference-fields`, and any declared one in `nullable-reference-fields` (003, 007). The set is closed; an unknown name stops start-up.

| Field | Added by | Usual target (E-Links numbering) | Declared by |
|-------|----------|----------------------------------|-------------|
| `type_id` | 003 | `location_types`; for `ticket_categories`, `ticket_category_types` (not served yet; DF-10) | `base_locations`, `locations`, `ticket_categories` |
| `parent_id` | 003 | `locations` | `base_locations`, `locations` (nullable) |
| `jurisdiction_id` | 003 | `jurisdictions` | `base_locations`, `locations`, `location_types`, `ticket_categories` |
| `ticket_category_id` | 010 | `ticket_categories` | `tickets` |
| `parent_category_id` | **011** | `ticket_categories` | `ticket_categories` (nullable) |

The targets are known only to the build's reference checks: a default per field, with a per-type override for `ticket_categories.type_id` (research R8; spec FR-020). The service itself never resolves references.

Rules for `parent_category_id` are those of every reference field (spec FR-019): present in every record of a declaring type as a positive JSON integer, or `null` only if the type also declares it nullable; absent from every record of any other type. Violations stop start-up with `… record id <id>: parent_category_id is required`, `… must be greater than 0` or `… is not a reference field of <type>`. In responses it follows `ticket_category_id` (research R3).

---

## 2. The type declaration

TicketCategory is declared in `application.yml` (research R1):

| Element | Value |
|---------|-------|
| `name` | `ticket_categories` |
| `aliases` | none (`ticket_category` is not served) |
| `fixture` | `classpath:reference-data/ticket_categories.json` |
| `reference-fields` | `[type_id, jurisdiction_id, parent_category_id]` |
| `nullable-reference-fields` | `[parent_category_id]` |
| `boolean-fields` | none |
| `text-fields` | none |
| `unique-names` | `false` (one name repeats) |

---

## 3. Ticket-category records

Response field order: `id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

| Field | Source | Rule |
|-------|--------|------|
| `id` | extract, unchanged | positive, unique; 31 to 88 with gaps (34, 84, 86, 87 missing) (DR-1) |
| `name` | extract, unchanged | trimmed, non-blank; 53 distinct across 54 records |
| `type_id` | extract, unchanged | positive integer, never null; 1 (3 records), 2 (3), 3 (46), 4 (1), 5 (1) |
| `jurisdiction_id` | extract, unchanged | positive integer, never null; 30 (3), 34 (49), 84 (1), 87 (1) |
| `parent_category_id` | extract, unchanged (blank → `null`) | `null` for 8 records; otherwise 35 (7), 43 (4) or 48 (35) |
| dates | synthetic (002) | see § 4 |

**Relationships**:
- **Ticket `ticket_category_id` → TicketCategory**: 159 records, 41 distinct values, all resolve (closes 010 DF-8). 13 categories are unreferenced: 35, 43, 48, 51, 53, 60, 61, 63, 64, 65, 66, 79, 81.
- **TicketCategory `parent_category_id` → TicketCategory**: 46 children; every parent is top-level, so the depth is at most 1; no cycles.
- **TicketCategory `jurisdiction_id` → Jurisdiction**: all resolve; 87 "Skills" is referenced only here, by 88 "Languages".
- **Agreement**: every child's `jurisdiction_id` equals its parent's (0 mismatches). Top-level records have no parent to agree with.
- **TicketCategory `type_id` → TicketCategoryType**: a forward reference (DF-10). Every value is an id in the local type extract, and every category's `jurisdiction_id` equals its type's there (0 mismatches), which DF-10's closing feature will check.

**The hierarchy**:

| Top-level category | `type_id` | `jurisdiction_id` | Children |
|--------------------|-----------|-------------------|----------|
| 31 Civil | 1 | 30 | none |
| 32 Crime | 1 | 30 | none |
| 33 Family | 1 | 30 | none |
| 35 First tier Tribunal | 2 | 34 | 7 (36–42) |
| 43 Upper Tier | 2 | 34 | 4 (44–47) |
| 48 Other Tribunal | 2 | 34 | 35 (49–83) |
| 85 Court Type | 4 | 84 | none |
| 88 Languages | 5 | 87 | none |

**Repeated name**: "Immigration and Asylum Chamber" is 38 (parent 35) and 45 (parent 43).

---

## 4. Fixture generation rules

`src/main/resources/reference-data/ticket_categories.json` is generated **once** during implementation and then checked in (FR-007).

1. **Input**: the local, gitignored `joh-elinks-api/ReferenceData/REF_TicketCategory.csv`, read as UTF-8 with a CSV parser: a header and 54 rows of 9 columns. Use `id`, `name`, `type_id`, `jurisdiction_id` and `parent_category_id`; ignore the dates (spec EC-005).
2. **Every row, exactly**: one record per row; ids and reference values as JSON integers; a blank `parent_category_id` as `null`. Names unchanged.
3. **Order**: ascending `id`. Positions run from 1 to 54 in that order (DR-1).
4. **Records where `position % 7 == 0`** (ids 38, 45, 52, 59, 66, 73, 80): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
5. **All other records**: `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
6. **All records**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
7. **Layout**: a JSON array, two-space indented, fields in § 3's order, every record with all nine keys, UTF-8 without a BOM, no `\u` escapes, with a trailing newline.

**Reference points** (the spec's table):

| Position | `id` | `name` | `type_id` | `jurisdiction_id` | `parent_category_id` | `end_date` |
|----------|------|--------|-----------|-------------------|----------------------|------------|
| 1 | 31 | Civil | 1 | 30 | null | null |
| 4 | 35 | First tier Tribunal | 2 | 34 | null | null |
| 7 | 38 | Immigration and Asylum Chamber | 3 | 34 | 35 | 2025-03-31 |
| 13 | 44 | Administrative Appeals Chamber | 3 | 34 | 43 | null |
| 14 | 45 | Immigration and Asylum Chamber | 3 | 34 | 43 | 2025-03-31 |
| 17 | 48 | Other Tribunal | 2 | 34 | null | null |
| 25 | 56 | Employment Tribunal (England & Wales) | 3 | 34 | 48 | null |
| 50 | 81 | Transport Tribunal | 3 | 34 | 48 | null |
| 53 | 85 | Court Type | 4 | 84 | null | null |
| 54 | 88 | Languages | 5 | 87 | null | null |

**Unknown ids**: `1`, `30`, `34`, `84`, `89` and `999999`.

**Pinned type ids** (`TICKET_CATEGORY_TYPE_IDS`): 1, 2, 3, 4, 5.

---

## 5. Verifying the fixture

**Once, against the extracts** (research R6): the generating script checks the extract's shape, that the five copied values equal the extract's row for row, the 7 ended ids, the hierarchy and parent-jurisdiction agreement, that every `jurisdiction_id` is served, that every `type_id` is in the type extract, that every ticket's `ticket_category_id` is in the new file, and every reference point. The plan's implementation notes record the result.

**On every build, without the extracts**:
- `ReferenceDataFixtureRulesTest`: the date rule by position, field order, counts, reference points, the per-type `type_id` pin, the jurisdiction pin (now with 87) and the literal-character check (research R7).
- `ReferenceDataReferencesTest`: the three new resolution rows, the cycle check, the parent-jurisdiction agreement, and the completeness check with the per-type target override (research R8).
- Start-up validation rejects any record that breaks 002's field rules, lacks a declared reference field, holds `null` where it isn't allowed, or carries an undeclared field.
- The functional suite's golden file pins the whole served collection.
