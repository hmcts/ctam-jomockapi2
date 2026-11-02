# Data Model: Reference Data API — Ticket Category Types

**Feature**: `012-ticket-category-types-reference-data` | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one type, TicketCategoryType, through configuration and data only. Every field, rule and capability it uses already exists; 002's to 011's data models are unchanged.

---

## 1. The type declaration

TicketCategoryType is declared in `application.yml` (research R1):

| Element | Value |
|---------|-------|
| `name` | `ticket_category_types` |
| `aliases` | none (`ticket_category_type` is not served) |
| `fixture` | `classpath:reference-data/ticket_category_types.json` |
| `reference-fields` | `[jurisdiction_id]` |
| `nullable-reference-fields` | none |
| `boolean-fields` | none |
| `text-fields` | `[integration_code]` |
| `unique-names` | `false` ("Court Type" repeats) |

Reference fields after this feature, with the build's targets (unchanged from 011, except that the override's target is now served):

| Field | Usual target | Declared by |
|-------|--------------|-------------|
| `type_id` | `location_types`; for `ticket_categories`, `ticket_category_types` | `base_locations`, `locations`, `ticket_categories` |
| `parent_id` | `locations` | `base_locations`, `locations` (nullable) |
| `jurisdiction_id` | `jurisdictions` | `base_locations`, `locations`, `location_types`, `ticket_categories`, **`ticket_category_types`** |
| `ticket_category_id` | `ticket_categories` | `tickets` |
| `parent_category_id` | `ticket_categories` | `ticket_categories` (nullable) |

---

## 2. Ticket-category-type records

Response field order: `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005), as for location types without `type_id`.

| Field | Source | Rule |
|-------|--------|------|
| `id` | extract, unchanged | positive, unique; 1 to 5, no gaps (DR-1) |
| `name` | extract, unchanged | trimmed, non-blank; 4 distinct across 5 records |
| `jurisdiction_id` | extract, unchanged | positive integer, never null; 30 (1), 34 (2), 84 (1), 87 (1) |
| `integration_code` | extract, unchanged | non-empty, trimmed, `[A-Z_]+` (008); distinct in the extract |
| dates | synthetic (002) | see § 3 |

**Relationships**:
- **TicketCategoryType `jurisdiction_id` → Jurisdiction**: all 5 resolve. 86 Coroners is not used; 87 Skills is used by type 5.
- **TicketCategory `type_id` → TicketCategoryType**: 54 records, all resolve (closes 011 DF-10). Every type is used: 1 by 3 categories (31, 32, 33), 2 by 3 (35, 43, 48), 3 by 46 (every child category), 4 by 1 (85) and 5 by 1 (88).
- **Agreement**: every ticket category's `jurisdiction_id` equals its type's (54 compared, 0 mismatches).
- **Skills**: jurisdiction 87 is referenced by ticket category 88 (011) and ticket category type 5; still not by the location data.

---

## 3. Fixture generation rules

`src/main/resources/reference-data/ticket_category_types.json` is generated **once** during implementation and then checked in (FR-007).

1. **Input**: the local, gitignored `joh-elinks-api/ReferenceData/REF_TicketCategoryType.csv`, read as UTF-8 with a CSV parser: a header and 5 rows of 8 columns. Use `id`, `name`, `jurisdiction_id` and `integration_code`; ignore the dates (spec EC-005).
2. **Every row, exactly**: one record per row; `id` and `jurisdiction_id` as JSON integers; names and codes unchanged.
3. **Order**: ascending `id`. Positions run from 1 to 5 in that order (DR-1).
4. **Records where `position % 7 == 0`**: none.
5. **All records**: `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`, `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
6. **Layout**: a JSON array, two-space indented, fields in § 2's order, every record with all eight keys, UTF-8 without a BOM, no `\u` escapes, with a trailing newline.

**Records** (the spec's table; every record is a reference point):

| Position | `id` | `name` | `jurisdiction_id` | `integration_code` | `end_date` |
|----------|------|--------|-------------------|--------------------|------------|
| 1 | 1 | Court Type | 30 | `COURTS_COURT_TYPE` | null |
| 2 | 2 | Tribunal Type | 34 | `TRIBS_TYPE` | null |
| 3 | 3 | Tribunal Name | 34 | `TRIBS_NAME` | null |
| 4 | 4 | Court Type | 84 | `MAGS_COURT_TYPE` | null |
| 5 | 5 | Skills Category | 87 | `SKILLS_CATEGORY` | null |

**Unknown ids**: `6`, `30`, `31` and `999999`.

---

## 4. Verifying the fixture

**Once, against the extract** (research R2): the generating script checks the extract's shape, that the four copied values equal the extract's row for row, that no record is ended, that every `jurisdiction_id` is served, that every ticket category's `type_id` is in the new file with the same `jurisdiction_id`, and every Records-table row. The plan's implementation notes record the result.

**On every build, without the extract**:
- `ReferenceDataFixtureRulesTest`: the date rule by position, field order, counts, all five records as reference points, the `integration_code` format, the jurisdiction pin and the literal-character check (research R3).
- `ReferenceDataReferencesTest`: the two new resolution rows, the type-jurisdiction agreement, and the completeness check, which now demands both rows (research R4).
- Start-up validation rejects any record that breaks 002's field rules, lacks `jurisdiction_id` or `integration_code`, or carries an undeclared field.
- The functional suite's golden file pins the whole served collection.
