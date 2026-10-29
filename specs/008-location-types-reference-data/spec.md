# Feature Specification: Reference Data API — Location Types

**Feature Branch**: `008-location-types-reference-data`

**Created**: 2026-10-29

**Status**: Implemented

**Input**: User description: "Add the `location_types` reference-data type to the generic reference-data API (feature 002), through configuration and a checked-in fixture only, with no main-code change. Deprecated versions are not to be considered. Contract (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `location_types` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` (`ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` requires `id`, `updated_at`, `created_at`, `start_date` and `end_date`. Scope: serve `location_types` on `GET /api/v1/reference_data/location_types` and `/{reference_id}`. Authentication, validation, error shape, ordering and edge cases are as in 002. Record shape: the Swagger fields, plus `name` (deviation D-1) and one reference field, `jurisdiction_id`, after `name` (deviation D-2), and nothing else. Type configuration: `reference-fields: [jurisdiction_id]`, `unique-names: false`, no boolean fields and no nullable reference fields. The singular `location_type` is not served and returns `400`, like every other unsupported E-Links name. Ids (DR, as in 007's DR-1): records keep the extract's ids, 1 to 16 with no gaps, not 002's 10 × position, so that every location and base location `type_id` resolves (Constitution Principle IV, 003 DF-1, 007 DF-5). This is a contract-neutral deviation from 002's id rule, limited to this type. Records are ordered by ascending id, and 002's date rule is applied by position in that order: ids 7 ("AC") and 14 ("Region") are ended (`end_date` 2025-03-31, `updated_at` 2025-04-01T08:00:00Z); the other 14 have no end date and `updated_at` 2024-06-03T10:30:00Z; all have `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates are not reproduced (it has no end dates), so the two ended records are synthetic. Fixture: generated once from the local extract REF_LocationType.csv. It has 16 rows; its columns are `id`, `name`, `jurisdiction_id`, `integration_code`, `start_date`, `end_date`, `created_at` and `updated_at`, plus an empty trailing column on every row. It is ASCII, has no BOM and no newline after the last row, and has unique ids and no blank names. Take `id`, `name` and `jurisdiction_id` and ignore every other column. Leave out `integration_code` (e.g. `COURTS_REGION`, `MAGS_AC_LJA`): serving it would need a new generic text-field capability and a new deviation, which is out of scope. Names are copied exactly. There are 13 distinct names: "Region" appears 3 times (ids 1, 6, 14) and "AC" twice (ids 7, 11), each a separate type. Records, as id name (jurisdiction): 1 Region (30), 2 Court (30), 3 Tier (34), 4 Chamber (34), 5 Location (34), 6 Region (84), 7 AC (84), 8 LJA (84), 9 Court Name (84), 10 Area or Parent AC (84), 11 AC (84), 12 Subcommittee (84), 13 Committee (84), 14 Region (86), 15 Local Authority (86), 16 Geographic Area (86). References: - `jurisdiction_id` (30 Courts ×2, 34 Tribunals ×3, 84 Magistrates ×8, 86 Coroners ×3) is a forward reference to `jurisdictions`, which isn't served yet. It is copied unchanged and recorded as a deferred item (Constitution IV); every value exists in the jurisdiction extract. - MUST test, as rows in ReferenceDataReferencesTest, that every `locations.type_id` (values 1–16) and every `base_locations.type_id` (2, 5, 8, 9, 11, 12, 13, 16) resolves to a served location type. This closes 007 DF-5 and 003 DF-1 for `type_id`; the test's completeness check already requires both rows once the type is configured. - MUST test that every location's and base location's `jurisdiction_id` equals the `jurisdiction_id` of its location type. This holds for every shipped record. Build checks without the extract: ReferenceDataFixtureRulesTest covers the fixture with the extract-id rule (`SOURCE`) and the reference-field checks. NoTypeSpecificCodeTest needs no change, because `location` already covers `location_type`. Remove `location_types` from the unsupported-name examples (ReferenceDataFunctionalTest; `jurisdictions` stays). Update the README text saying `location_types` isn't served. Reference points: 1 "Region" (30); 6 "Region" (84); 14 "Region" (86, ended); 7 "AC" (84, ended); 11 "AC" (84); 10 "Area or Parent AC" (84, longest name); 16 "Geographic Area" (86, last). Unknown ids: 17 (just past the last) and 999. Deferred: the `jurisdictions` feature must make every location type `jurisdiction_id` resolve, with a test, alongside its existing duties for locations and base locations. Deliver unit, integration, functional and smoke coverage; generated OpenAPI documentation (the `attribute_name` enum gains `location_types`); and Postman artifacts (collection and environment, with test scripts). Acceptance scenarios mirror the existing reference-data features."

> **Superseded in part**: Clarifications Q1 (2026-10-29) reversed the description's exclusion of `integration_code`. Records carry it after `jurisdiction_id`. That needs one generic main-code change (declared text fields, FR-019) and a new contract deviation, D-4. Where the description and the requirements below differ, the requirements apply.

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003` to `007` added BaseLocation, ContractType, Gender, JudiciaryRole and Location through configuration and fixtures. 003 added per-type reference fields (`type_id`, `parent_id`, `jurisdiction_id`) and repeated names (`unique-names: false`); 007 kept the extract's own ids for a type that other types refer to (007 DR-1) and added nullable reference fields.

This feature adds LocationType, the seventh of the eleven E-Links reference-data types: the 16 kinds of location (region, court, tier, chamber, local justice area and so on) that the location hierarchy is built from, each belonging to one jurisdiction. Like Location, it is the **target** of existing references: every Location and BaseLocation `type_id` names a location type. That shapes the feature:

- **Ids are the extract's own** (FR-009; DR-1). Constitution Principle IV, 003 DF-1 and 007 DF-5 require LocationType to be served with the E-Links ids, so that every location's and base location's `type_id` resolves. Every other value stays synthetic.
- **References within served types must resolve and agree** (FR-011, FR-012). Every Location and BaseLocation `type_id` resolves to a served location type, closing 007 DF-5 and 003 DF-1 for `type_id`; and every location's and base location's `jurisdiction_id` equals its location type's.
- **One generic change** (FR-003, FR-019). The extract's ids, repeated names and a single declared reference field are already supported. Serving the extract's `integration_code` needs one new type-agnostic capability: a type can declare text fields, as 004 let a type declare boolean fields. No type-specific code is added.

Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to LocationType unchanged and is not restated here except where this feature tests it. The mock doesn't serve the deprecated alias `location_type` (FR-001; Constitution Principle VII).

## Clarifications

### Session 2026-10-29

- Q: Should location-type records also return the extract's `integration_code` (e.g. `COURTS_REGION`, `MAGS_AC_LJA`)? → A: Yes (first answered No, revised on review). Every location-type record carries `integration_code`, copied unchanged from the extract, after `jurisdiction_id`. This needs one generic capability, declared text fields (FR-019), and a new contract deviation, D-4.
- Q: Should location types 7 ("AC") and 14 ("Region") be ended by 002's every-seventh-record rule, although the extract ends none and open locations use them? → A: Yes. 002's rule applies unchanged, so ids 7 and 14 are ended; open locations may have an ended type, and no test requires otherwise (FR-009, EC-005).
- Q: Must every location type's `integration_code` be different from every other's, and how should that be enforced? → A: Not required. The 16 shipped codes happen to be unique, but neither the service nor any test checks it; consumers MUST NOT rely on uniqueness (EC-004, FR-019).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `location_types` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. The local extract shows that real location-type data carries a name, a `jurisdiction_id` and an `integration_code`. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `location_types` | Same |
| Deprecated alias | Out of scope | Not served: `location_type` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (D-1), `jurisdiction_id` (D-2) and `integration_code` (D-4) |
| Ids | E-Links' own location-type ids | Same as the extract (1 to 16); see DR-1 |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | LocationType records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of location types without names has no practical use. | Permanent, scope-justified (unchanged from 002) |
| D-2 (from 003) | LocationType records include `jurisdiction_id`, which the Swagger schema does not list; they carry neither `type_id` nor `parent_id` | The extract shows the real location-type data carries a jurisdiction, and it is what ties a location type to its jurisdiction. The field appears only on records of types that declare it, so the other types' responses are unchanged. | Permanent, scope-justified (extended to this type) |
| D-4 | LocationType records include `integration_code`, which the Swagger `ReferenceDataResponse` schema does not list | The extract shows the real location-type data carries a stable machine-readable code for each type (e.g. `MAGS_AC_LJA`), which in the shipped data also tells apart types with the same name ("Region" ×3, "AC" ×2) without relying on ids; codes are not required to be unique (Clarifications). The field is optional in the generated schema and appears only on records of a type that declares it (FR-019), so the other types' responses are unchanged. | Permanent, scope-justified |

### Data-Rule Deviation

| # | Deviation | Reason |
|---|-----------|--------|
| DR-1 | LocationType ids are the extract's own ids, not 002's synthetic `10 × position`, and records are numbered for the date rule by position in ascending-id order, not code-point name order | Constitution Principle IV, 003 DF-1 and 007 DF-5: Location and BaseLocation `type_id` values are E-Links location-type ids, so they only resolve if LocationType keeps those ids. This is not a contract deviation (the contract defines no id values); it applies to this type only, as 007's DR-1 does to Location, and every other generated value follows 002's rules. |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-1 (from 003), partly closed | BaseLocation's `type_id` → `location_types`; `jurisdiction_id` → `jurisdictions` (`parent_id` closed by 007) | This feature serves `location_types`, so it closes DF-1 for `type_id` (FR-011, AC-010). `jurisdictions` is not served yet. | `type_id`: closed by this feature's test. `jurisdiction_id`: unchanged; the feature serving `jurisdictions` closes it. |
| DF-5 (from 007), partly closed | Location's `type_id` → `location_types`; `jurisdiction_id` → `jurisdictions` | This feature closes DF-5 for `type_id` (FR-011, AC-010). | `type_id`: closed by this feature's test. `jurisdiction_id`: unchanged; the feature serving `jurisdictions` closes it. |
| DF-7 | LocationType's `jurisdiction_id` (30, 34, 84, 86) refers to `jurisdictions`: a forward reference (Constitution Principle IV) | `jurisdictions` is not served yet. In the local extracts every value exists in the jurisdiction extract, so the references will resolve once that type is served with the extract's ids. | The feature that serves `jurisdictions` MUST serve it with the extract's ids and MUST add a test that every LocationType `jurisdiction_id` resolves to a served record, alongside its duties under DF-1 and DF-5; it is incomplete until that test passes. Closed by 009 ([009 FR-011](../009-jurisdictions-reference-data/spec.md); `ReferenceDataReferencesTest`). |

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Location Types (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of location types so that my system can load them exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving location types can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/location_types` and confirm a `200 OK` JSON response whose `results` array holds all 16 location-type records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/location_types`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every LocationType record in the configured dataset (16 by default), in ascending `id` order, each with exactly the fields `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date` and `end_date`, in that order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** every record equals the Records table (values for `id`, `name`, `jurisdiction_id`, `integration_code`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** repeated names stay separate records: "Region" three times (ids 1, 6 and 14) and "AC" twice (ids 7 and 11), each with its own id, jurisdiction and integration code (EC-002).
4. **AC-004**: **Given** the default dataset, **When** the collection is retrieved, **Then** exactly 2 records are ended — ids 7 and 14, at positions 7 and 14 — each with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z, and every other record has a null `end_date` and `updated_at` 2024-06-03T10:30:00Z (EC-005).
5. **AC-005**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Location Type by ID (Priority: P1)

As an integrator, I want to fetch one location type by its reference identifier so that my system can resolve an id it holds — such as a location's `type_id` — to its full record.

**Why this priority**: Part of the same contract as the collection route, and the way a consumer looks up the type of a location it holds.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/location_types/{id}` for an id in the Records table and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/location_types/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including an ended record (e.g. `14`, "Region").
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no location type (`17`, just past the last id; `999`; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `1`, which is "Aberconwy" under `locations`, or `10`, which is "Aberconwy" under `base_locations`), **When** it is requested under `location_types`, **Then** the result depends only on the LocationType dataset (`1` is "Region", `10` is "Area or Parent AC"): identifiers are scoped to their type and never resolve across types (EC-007).

---

### User Story 3 - Integrator Resolves Every Location's Type (Priority: P1)

As an integrator, I want every `type_id` a location or base location carries to name a location type the mock serves, with the same jurisdiction, so that my system can label every location with its kind without hitting a dangling or contradictory reference.

**Why this priority**: Resolving types is the reason LocationType is served with the extract's ids, and Constitution Principle IV requires references between served types to resolve and the data to be internally consistent. It closes 003 DF-1 and 007 DF-5 for `type_id`.

**Independent Test**: Load the LocationType, Location and BaseLocation collections and check that every `type_id` names a served location type whose `jurisdiction_id` equals the referring record's.

**Acceptance Scenarios**:

1. **AC-010**: **Given** the default datasets, **When** the LocationType, Location and BaseLocation collections are retrieved, **Then** every Location `type_id` (2,000 records, 16 distinct values, 1 to 16) and every BaseLocation `type_id` (1,462 records, 8 distinct values: 2, 5, 8, 9, 11, 12, 13 and 16) is the `id` of a served location type, closing 003 DF-1 and 007 DF-5 for `type_id`.
2. **AC-011**: **Given** the default datasets, **When** the same collections are retrieved, **Then** every Location's and every BaseLocation's `jurisdiction_id` equals the `jurisdiction_id` of the location type its `type_id` names.
3. **AC-012**: **Given** the default dataset, **When** the LocationType collection is retrieved, **Then** every `jurisdiction_id` is one of 30, 34, 84 and 86, non-null on every record: 2 records in 30, 3 in 34, 8 in 84 and 3 in 86. The values are copied unchanged from the extract and await `jurisdictions` (DF-7).

---

### User Story 4 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `location_type` rejected clearly, so that my system uses the canonical name `location_types` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested.

**Independent Test**: Call `location_type` and `location_type/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-013**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/location_type`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-014**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/location_type/{reference_id}` for any `reference_id` (one that exists under `location_types` such as `1`, an unknown one such as `17`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-015**: **Given** a request to a `location_type` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name.

---

### User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want location-type requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each location-type route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-016**: **Given** a request with no credentials, **When** it is sent to any location-type route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-017**: **Given** a request with present but invalid credentials, **When** it is sent to any location-type route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using the six served types, I want adding location types to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: Every feature must hold the regression boundary; this feature changes main code again, so every existing response must be shown to be unchanged.

**Independent Test**: Re-run 002's to 007's acceptance tests unchanged, compare the existing golden responses byte for byte, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-018**: **Given** the delivered feature, **When** 002's to 007's acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature. Shared test lists of unsupported names may lose `location_types`; nothing else asserted changes.
2. **AC-019**: **Given** valid authentication, **When** each of the 14 E-Links attribute names that are still unsupported (the four canonical types not yet served: `jurisdictions`, `ticket_categories`, `ticket_category_types` and `tickets`; their four singular forms; and the unserved singular names `base_location`, `contract_type`, `gender`, `judiciary_role`, `location` and `location_type`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-020**: **Given** valid authentication, **When** the caller sends a location-type request with any query parameter (e.g. `?jurisdiction_id=84`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 7 - Maintainer Adds the Type With One Generic Change (Priority: P2)

As a maintainer, I want location types added by declaring the type and supplying its dataset, plus one generic capability that any type can use, so that 002's extension design keeps working without type-specific code.

**Why this priority**: Keeps the remaining types cheap to add and the codebase free of special cases.

**Independent Test**: Review the change, run the start-up validation tests for declared text fields, and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-021**: **Given** the delivered change, **When** it is reviewed, **Then** its only main-code change is the generic text-field capability (FR-019); everything else is the type entry, its dataset, tests, documentation and example requests.
2. **AC-022**: **Given** a type that declares a text field, **When** a record leaves it out, sets it to null, or holds a non-string, empty or untrimmed value, **Then** the service refuses to start, naming the fixture and the field, and the record by its id (or, for a value that fails while the JSON is read, by its position); **and given** a type that doesn't declare the field, a record that carries it also stops the service at start-up. The automated guard against type-specific code passes unchanged: its existing `location` fragment already recognises `location_type` in every spelling.
3. **AC-023**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `location_types` alongside the existing supported values and does not list `location_type`, and `integration_code` is documented as an optional, non-nullable string present only for types that declare it, without that documentation having been edited by hand; `ReferenceDataResponse` is otherwise unchanged.

---

### User Story 8 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for location types so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every location-type request's checks pass.

**Acceptance Scenarios**:

1. **AC-024**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the location-type collection, a single record (e.g. `1`), an ended record (e.g. `14`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `location_type` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values.

---

### Edge Cases

- **EC-001 — Ids from the extract, without gaps**: Ids run from 1 to 16 with no gaps, so every well-formed id in that range is known, and `17`, past the last id, is the nearest unknown id (`404`) (DR-1). 002 FR-023's id-gap clause is read across all shipped default data, where the other types supply gaps, so no waiver is needed (as 005 established).
- **EC-002 — Repeated names**: "Region" occurs three times (ids 1, 6 and 14, in jurisdictions 30, 84 and 86) and "AC" twice (ids 7 and 11, both in jurisdiction 84), over 13 distinct names. Each occurrence is a separate location type with its own id; none is merged or dropped, and the order of records doesn't depend on names.
- **EC-003 — Name content**: Names are plain ASCII, between 2 ("AC") and 17 ("Area or Parent AC") characters, and contain only letters and spaces. Every name is free of leading or trailing whitespace. "Location" (id 5) is a location-type name, not a reference to the Location type.
- **EC-004 — Integration codes and columns not served**: Every row's `integration_code` is 7 to 22 characters of upper-case letters and underscores (`MAGS_AC` to `MAGS_AREA_SUBCOMMITTEE`), and is served exactly as the extract has it. The 16 codes happen to be unique, but uniqueness is not required and not checked, by the service or by tests (Clarifications). The extract's own dates and timestamps and the empty trailing column on every row are not served.
- **EC-005 — The extract's dates are not reproduced**: The extract starts every row on 30/03/2023, with fixed created and updated timestamps and no end dates. All are ignored; 002's position-based rule applies, ending 2 records (ids 7 and 14). Those end dates are synthetic, so a location can be open while its location type is ended: 101 of the 115 locations of type 7 or 14 are open (no base location has either type). References resolve regardless of end dates, and the mock defines no consistency rule between them (Clarifications).
- **EC-006 — Order**: Records are ordered by ascending id, and the date rule counts positions in that order, so a record's position equals its id. Names are not sorted.
- **EC-007 — Same id, different type**: Ids 1 to 16 also exist under `locations` (e.g. `1` "Aberconwy") and `10` exists under `base_locations`, each meaning a different record. Identifiers are scoped to their type.
- **EC-008 — Case and spelling variants**: `Location_Types`, `LOCATION_TYPES`, `location-types`, `locationTypes` and `location_types_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-009 — Inherited edge cases**: Covered by FR-004. For location types, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the LocationType reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `location_types`. The singular name `location_type` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations` and `location_types`. This supersedes 007's FR-002; every other E-Links attribute name, including `location_type`, MUST remain unsupported (`400`).
- **FR-003**: LocationType MUST be added through the generic reference-data capability: a type entry and its dataset, plus the generic capability of FR-019. The change MUST NOT add or modify route, authentication or error-handling logic, and MUST NOT introduce code that names a specific reference-data type or behaves differently for one.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to LocationType without exception. Names MAY repeat within the type; the type declares exactly one reference field, `jurisdiction_id`, which is not nullable, one text field, `integration_code`, and no boolean fields.

**Response content**

- **FR-005**: Each LocationType record MUST contain exactly these fields, in this order: `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date` and `end_date`, the shared fields in 002's formats (002 FR-011). `jurisdiction_id` MUST be a positive integer and `integration_code` a non-empty string, never null, on every record (deviation D-4); `type_id` and `parent_id` MUST NOT appear. The records of the existing types MUST stay exactly as 002 to 007 define them, without `integration_code`.

**Data**

- **FR-006**: The LocationType dataset MUST be deterministic and supplied as a fixed, checked-in fixture. Only the public `id`, `name`, `jurisdiction_id` and `integration_code` values may be taken from the supplied location-type reference extract, each copied unchanged; every other value (dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over (EC-004).
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 16 rows in the extract, which have 13 distinct names. Each name and integration code MUST be carried over exactly as the extract has it: nothing trimmed, merged, case-folded, corrected or filtered (EC-002 to EC-004).
- **FR-009**: Records MUST keep the extract's ids (DR-1) and be ordered by ascending id, numbered from position 1 to 16 in that order. Records whose position is a multiple of 7 (ids 7 and 14) are ended, with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; all other records have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates and timestamps MUST NOT be used (EC-005).
- **FR-010**: LocationType ids MUST be stable across releases of the default dataset and MUST equal the extract's, so that Location (007), BaseLocation (003) and later features can refer to them.

**Generic capability**

- **FR-019**: A type MUST be able to declare text fields, chosen from a fixed set of known field names (currently only `integration_code`), as 004 lets a type declare boolean fields. Each declared text field MUST be present on every record of the type as a JSON string that is non-empty and has no leading or trailing whitespace; a missing, null, non-string, empty or untrimmed value MUST stop the service at start-up with an error naming the fixture and the field, and the record by its id (or, for a null or non-string value, which fails while the JSON is read, by its position). A record of a type that doesn't declare the field MUST NOT carry it, or the service MUST NOT start. Responses carry a text field only for types that declare it, so the responses of every other type MUST stay byte-for-byte the same.

**Referential integrity**

- **FR-011**: References between served types MUST resolve (Constitution Principle IV), verified by automated tests run on every build against the shipped datasets: every Location `type_id` and every BaseLocation `type_id` MUST be the `id` of a served LocationType record, closing 007 DF-5 and 003 DF-1 for `type_id`. The existing check that every reference between served types is tested MUST cover both references. The service itself does not check that references resolve, as in 007.
- **FR-012**: Served data MUST be internally consistent (Constitution Principle IV), verified by an automated test run on every build against the shipped datasets: every Location's and every BaseLocation's `jurisdiction_id` MUST equal the `jurisdiction_id` of the LocationType its `type_id` names.

**Documentation and example requests**

- **FR-013**: The generated API documentation MUST list `location_types` among the supported `attribute_name` values and MUST NOT list `location_type`, derived from the configuration rather than edited by hand. It MUST document `integration_code` as an optional, non-nullable string present only for types that declare it (D-4); the rest of `ReferenceDataResponse` MUST be unchanged.
- **FR-014**: The example request collection MUST cover the location-type routes as AC-024 describes, using environment variables for every environment-specific value.
- **FR-015**: The project's documentation of supported reference-data types MUST list location types, and every statement that `location_types` is not served (including the description of BaseLocation's and Location's `type_id`) MUST be updated. The instructions for adding a type MUST describe the text-field option (FR-019) and remain accurate.

**Quality**

- **FR-016**: The location-type collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-017**: Unit, integration, functional and smoke coverage MUST be extended to the new type and the new capability, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009), with the extract-id order rule that 007 introduced, MUST be checked by an automated test without the extract; the values are compared with the extract once, when the fixture is generated (FR-007).
- **FR-018**: The automated guard against type-specific code MUST keep passing without change: its existing `location` fragment already covers `location_type` and `location_types` in every spelling, with no exemption. Test lists that use `location_types` as an example of an unsupported name MUST drop it.

### Records

The whole default dataset, fixed by FR-006, FR-008 and FR-009 and used by tests and example requests. Position equals id. "Ended" records have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; the others have a null `end_date` and `updated_at` 2024-06-03T10:30:00Z. Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01.

| `id` | `name` | `jurisdiction_id` | `integration_code` | Ended | Note |
|------|--------|-------------------|--------------------|-------|------|
| 1 | Region | 30 | `COURTS_REGION` | | first; "Region" (1 of 3) |
| 2 | Court | 30 | `COURTS_COURT_NAME` | | |
| 3 | Tier | 34 | `TRIBS_TIER` | | |
| 4 | Chamber | 34 | `TRIBS_CHAMBER` | | |
| 5 | Location | 34 | `TRIBS_LOCATION` | | |
| 6 | Region | 84 | `MAGS_REGION` | | "Region" (2 of 3) |
| 7 | AC | 84 | `MAGS_AC` | yes | first ended; "AC" (1 of 2); shortest name |
| 8 | LJA | 84 | `MAGS_AC_LJA` | | |
| 9 | Court Name | 84 | `MAGS_AC_COURT_NAME` | | |
| 10 | Area or Parent AC | 84 | `MAGS_AREA_OR_PARENT_AC` | | longest name |
| 11 | AC | 84 | `MAGS_AREA_AC` | | "AC" (2 of 2) |
| 12 | Subcommittee | 84 | `MAGS_AREA_SUBCOMMITTEE` | | |
| 13 | Committee | 84 | `MAGS_AREA_COMMITTEE` | | |
| 14 | Region | 86 | `CORONERS_REGION` | yes | last ended; "Region" (3 of 3) |
| 15 | Local Authority | 86 | `CORONERS_LOCAL_AUTH` | | |
| 16 | Geographic Area | 86 | `CORONERS_AREA` | | last |

`17`, `999` and `999999` are guaranteed unknown ids.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/location_types` | `200`, 16 records |
| Unserved singular name | `GET /api/v1/reference_data/location_type` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/location_types/1` | `200`, Region, `jurisdiction_id` 30, `integration_code` `COURTS_REGION` |
| Ended record | `GET /api/v1/reference_data/location_types/14` | `200`, Region, `jurisdiction_id` 86, `end_date` 2025-03-31 |
| Unknown id | `GET /api/v1/reference_data/location_types/17` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/location_types/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/location_types?jurisdiction_id=84` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/jurisdictions` | `400`, shared error shape |
| Missing or invalid credentials | any location-type route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002 and extended by 003, 004 and 007. This feature adds a seventh instance, LocationType, with canonical name `location_types`, no served alias, repeated names allowed, the single reference field `jurisdiction_id`, the text field `integration_code`, no nullable reference fields and no boolean fields. A type's declaration gains one optional element: the list of its text fields (FR-019).
- **LocationType record**: One kind of location in E-Links' location hierarchy (such as a region, court, tribunal chamber, local justice area or coroner's area), belonging to one jurisdiction. Attributes: `id` (unique, stable, the E-Links id), `name` (public name, exactly as in the source, not necessarily unique), `jurisdiction_id` (E-Links jurisdiction), `integration_code` (E-Links' code for the type, exactly as in the source; not required to be unique), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. Location and BaseLocation records refer to it through their `type_id`; it refers to `jurisdictions` through a forward reference (DF-7).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 24 acceptance scenarios (AC-001 to AC-024) pass as automated tests or, for AC-021 and AC-024, as review and collection-run checks.
- **SC-002**: The default location-type collection contains exactly 16 records with 13 distinct names, 2 of them ended, matching the Records table.
- **SC-003**: 100% of the 16 served records carry the same `id`, `name`, `jurisdiction_id` and `integration_code` as the source row, character for character.
- **SC-004**: 100% of `type_id` values, in both Location (2,000) and BaseLocation (1,462) records, resolve to a served location type; 0 dangling type references remain.
- **SC-005**: 100% of Location and BaseLocation records have the same `jurisdiction_id` as their location type; 0 mismatches.
- **SC-006**: 0 requests via the unserved singular name `location_type` return reference data; 100% with valid credentials return `400`.
- **SC-007**: 0 of the 14 still-unsupported attribute names return a `200` response.
- **SC-008**: 0 location-type requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-009**: 100% of 002's to 007's acceptance tests pass with their assertions and expected responses unchanged, and every existing golden response is byte-for-byte identical.
- **SC-010**: 0 lines of main code name a specific reference-data type; the only main-code change is the generic text-field capability.
- **SC-011**: The location-type collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-012**: An integrator using the published documentation and example requests retrieves the location-type list and the type of a given location at the first attempt, without reading source code.

## Assumptions

- **Source values are public reference data**: Location-type names, ids, jurisdiction codes and integration codes describe public categories of court and administrative location, not people, so reusing them is consistent with Constitution Principle IV. Dates and timestamps are synthetic.
- **The extract's ids are required, not chosen**: Principle IV's forward-reference exception requires a referenced type to be served with the extract's ids. Renumbering location types would leave every Location and BaseLocation `type_id` dangling, so DR-1 is the only compliant option, as for Location (007 DR-1).
- **Names are reproduced, not curated**: The mock stands in for the real reference data, so repeated names such as "Region" and "AC" are part of what consumers must handle; the jurisdiction tells them apart (EC-002).
- **`integration_code` is served**: It is public reference data (a code per category of location, not about people) and, in the shipped data, the only value besides the id that tells every same-named type apart (both "AC" types are in jurisdiction 84), though codes are not required to be unique (Clarifications). It needs a new kind of declared field (text) and a new contract deviation, D-4 (Clarifications). Only one text field is known so far; the capability is generic so later types can declare their own.
- **Same date rules as 002**: The dates, timestamps and ended-record rule are 002's, counted over ascending-id positions. For 16 records the rule ends ids 7 and 14, which the extract doesn't end. 002's FR-023 is read across all shipped default data (as 005 established), so LocationType's lack of id gaps needs no waiver (EC-001).
- **Jurisdiction consistency is checked, not enforced**: A location's jurisdiction is stored on the location and on its location type. The shipped data agrees everywhere (checked against the local extracts and the shipped fixtures: 0 mismatches in 2,000 locations and 1,462 base locations), and FR-012 keeps it that way through a build-time test. The service doesn't check it at start-up, as for reference resolution (007 Clarifications).
- **Deprecated names are out of scope**: The feature description excludes deprecated versions, so only `location_types` is served, as for the singular names of BaseLocation, ContractType, Gender, JudiciaryRole and Location. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The guard needs no change**: 007 made the type-specific-code guard match `location` in every spelling, which already covers `location_type` and `location_types`.
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged.
- **Fixture source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 007 delivered: the generic routes, central type registry, per-type declarations (reference fields, boolean fields, repeated names) and fixture validation at start-up, the extract-id order rule in the fixture generation-rules test, the reference-resolution test with its completeness check, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
