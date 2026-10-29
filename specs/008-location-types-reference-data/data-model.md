# Data Model: Reference Data API — Location Types

**Feature**: `008-location-types-reference-data` | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This feature adds one type, LocationType, and one generic declaration, text fields. Everything else in 002's to 007's data models is unchanged.

---

## 1. The type declaration

LocationType is declared in `application.yml` (research R1):

| Element | Value |
|---------|-------|
| `name` | `location_types` |
| `aliases` | none (`location_type` is not served) |
| `fixture` | `classpath:reference-data/location_types.json` |
| `reference-fields` | `[jurisdiction_id]` |
| `nullable-reference-fields` | none |
| `boolean-fields` | none |
| `text-fields` | `[integration_code]` (new, § 2) |
| `unique-names` | `false` |

---

## 2. Text fields (new, generic)

A type's declaration gains one optional list, `text-fields`, of known text-field names. Today the only one is `integration_code` (`TextField.INTEGRATION_CODE`). It defaults to none (research R2).

**Declaration rules** (start-up; the service doesn't start otherwise):
- every entry names a known text field; the match is exact and case-sensitive;
- no entry is empty or repeated.

**Record rules** (start-up; research R3):

| The type… | Key missing | `null` | Number, boolean, object or array | `""` or blank | Leading or trailing whitespace | Non-empty, trimmed string |
|-----------|-------------|--------|----------------------------------|---------------|--------------------------------|---------------------------|
| declares it | invalid: `<field> is required` | invalid (fails while reading) | invalid (fails while reading) | invalid: `<field> must not be blank` | invalid: `<field> must be trimmed` | valid |
| doesn't declare it | valid | invalid (fails while reading) | invalid (fails while reading) | invalid: `<field> is not a text field of <type>` | invalid: same message | invalid: same message |

Text fields are never nullable and need not be unique (spec Clarifications).

**Response rule**: a text field is written for every record of a type that declares it and left out for every other type, so the other six types' responses are byte-for-byte unchanged.

**OpenAPI** (`ReferenceDataResponse`): `integration_code` is an optional, non-nullable `string`, placed after `jurisdiction_id`, whose description cites deviation D-4. No other property changes.

**Response field order** (all types): `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `integration_code`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date`. Each type writes only the fields it declares.

---

## 3. Location-type records

Each record is a `ReferenceDataRecord` with 002's field rules, one reference field and one text field. Response field order: `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005).

| Field | Source | Rule |
|-------|--------|------|
| `id` | extract, unchanged | positive, unique, 1 to 16 with no gaps (DR-1) |
| `name` | extract, unchanged | trimmed, non-blank, repeats allowed (13 distinct over 16 records) |
| `jurisdiction_id` | extract, unchanged | one of 30, 34, 84, 86; forward reference to `jurisdictions` (DF-7) |
| `integration_code` | extract, unchanged | non-empty, trimmed string; 7 to 22 upper-case letters and underscores; not required to be unique |
| `created_at`, `updated_at`, `start_date`, `end_date` | synthetic (002) | see § 4 |

**Relationships**:
- **Location `type_id` → LocationType**: 2,000 records, 16 distinct values (1 to 16), every one an id here. Closes 007 DF-5 for `type_id`.
- **BaseLocation `type_id` → LocationType**: 1,462 records, 8 distinct values (2, 5, 8, 9, 11, 12, 13, 16), every one an id here. Closes 003 DF-1 for `type_id`.
- **Jurisdiction agreement**: every Location's and BaseLocation's `jurisdiction_id` equals the `jurisdiction_id` of the location type its `type_id` names (spec FR-012). Checked on the shipped fixtures: 0 mismatches.
- **`jurisdiction_id` → Jurisdiction**: a forward reference (DF-7), not checked until `jurisdictions` is served.

**State**: none. Records are read-only. `end_date` marks a record as ended, by the synthetic rule only; 101 open locations have an ended type (7 or 14), and that is allowed (spec EC-005).

---

## 4. Fixture generation rules

`src/main/resources/reference-data/location_types.json` is generated **once** during implementation and then checked in. After that, the checked-in file is the source of truth (FR-007).

1. **Input**: the local, gitignored `joh-elinks-api/ReferenceData/REF_LocationType.csv` (16 rows), read as UTF-8 with a CSV reader. Use `id`, `name`, `jurisdiction_id` and `integration_code`; ignore `start_date`, `end_date`, `created_at`, `updated_at` and the empty ninth column (spec EC-004, EC-005).
2. **Every row, exactly**: one record per row, with the four columns copied as they are. Nothing is trimmed, merged, case-folded or corrected.
3. **Order**: ascending `id`. Positions run from 1 to 16 in that order, so position equals id (DR-1).
4. **Records where `position % 7 == 0`** (ids 7 and 14): `end_date = 2025-03-31`, `updated_at = 2025-04-01T08:00:00Z`.
5. **All other records** (14): `end_date = null`, `updated_at = 2024-06-03T10:30:00Z`.
6. **Every record**: `created_at = 2024-01-15T09:00:00Z`, `start_date = 2024-01-01`.
7. **Layout**: a JSON array, two-space indented, fields in § 3's order, every record with all eight keys, UTF-8 without a BOM, with a trailing newline.

Rules 4 to 7 are 002's. Rules 1 to 3 are 007's extract-id rule.

**Records** (the whole dataset; the spec's Records table):

| `id` | `name` | `jurisdiction_id` | `integration_code` | Ended |
|------|--------|-------------------|--------------------|-------|
| 1 | Region | 30 | `COURTS_REGION` | |
| 2 | Court | 30 | `COURTS_COURT_NAME` | |
| 3 | Tier | 34 | `TRIBS_TIER` | |
| 4 | Chamber | 34 | `TRIBS_CHAMBER` | |
| 5 | Location | 34 | `TRIBS_LOCATION` | |
| 6 | Region | 84 | `MAGS_REGION` | |
| 7 | AC | 84 | `MAGS_AC` | yes |
| 8 | LJA | 84 | `MAGS_AC_LJA` | |
| 9 | Court Name | 84 | `MAGS_AC_COURT_NAME` | |
| 10 | Area or Parent AC | 84 | `MAGS_AREA_OR_PARENT_AC` | |
| 11 | AC | 84 | `MAGS_AREA_AC` | |
| 12 | Subcommittee | 84 | `MAGS_AREA_SUBCOMMITTEE` | |
| 13 | Committee | 84 | `MAGS_AREA_COMMITTEE` | |
| 14 | Region | 86 | `CORONERS_REGION` | yes |
| 15 | Local Authority | 86 | `CORONERS_LOCAL_AUTH` | |
| 16 | Geographic Area | 86 | `CORONERS_AREA` | |

**Unknown ids**: `17`, `999` and `999999`.

---

## 5. Verifying the fixture

**Once, against the extract** (research R5): the generating script checks the counts, that the four copied columns equal the extract's row for row, the code format, the ended ids, that every Location and BaseLocation `type_id` resolves with agreeing jurisdictions, and every row of § 4. It does this before the file is checked in, and the plan's implementation notes record the result. This is how SC-003 is met.

**On every build, without the extract**:
- `ReferenceDataFixtureRulesTest` checks every fixture, including this one under the `SOURCE` id rule: the date rule by position, the field order (with text fields), the reference and text-field shapes, the integration-code format, the jurisdiction set, the counts, every reference point and that no `\u` escape appears (research R6).
- `ReferenceDataReferencesTest` checks that every Location and BaseLocation `type_id` resolves to a location type, that their jurisdictions agree with their types', and that every reference between served types is listed (research R7).
- Start-up validation (§ 2) rejects any record that breaks the field rules.
- The functional suite's golden file pins the whole served collection.
