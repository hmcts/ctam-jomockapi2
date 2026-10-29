# Feature Specification: Reference Data API — Locations

**Feature Branch**: `007-locations-reference-data`

**Created**: 2026-10-27

**Status**: Implemented

**Input**: User description: "Add the locations reference-data type to the existing generic reference-data API (feature 002). Contract reference (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `locations` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` ("The found reference data": `ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` has `id`, `updated_at`, `created_at`, `start_date` and `end_date`, all required; the People schema's appointments carry `location` (required) alongside `base_location` and `base_location_id`. Scope: serve `locations` on `GET /api/v1/reference_data/locations` and `/{reference_id}`, reusing the generic mechanism (a type entry and a checked-in fixture, no type-specific code); names may repeat (`unique-names: false`, as for base_locations); the type declares the reference fields `type_id`, `parent_id` and `jurisdiction_id`, in that order after `name`, as base_locations does (existing deviation D-2); no boolean fields; authentication, validation, error shape, ordering and edge cases as 002. Record shape: the Swagger fields plus `name` (existing deviation D-1) and the three reference fields, nothing else. Ids: unlike 002–006, records MUST keep the extract's own ids (2,000 records, ids 1 to 2433, with gaps), not 10 × position. Constitution Principle IV and 003's DF-1 require `locations` to be served with the extract's ids so that every base location's `parent_id` (copied unchanged from the extract in 003) resolves; locations' own `parent_id` values also use these ids. Record this as a contract-neutral deviation from 002's id rule, limited to this type. Every other value stays synthetic: records are ordered by ascending id, and 002's date rule is applied by position in that order (records at positions that are multiples of 7 are ended: 285 records, first id 9 "Ashby-de-la-Zouch and Market Bosworth", last id 2364; `end_date` 2025-03-31, `updated_at` 2025-04-01T08:00:00Z; the other 1,715 have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01). Say in the spec that the extract's own 1,123 end dates and its dates and timestamps are not reproduced. Fixture: generated once from the local location reference extract (REF_Location.csv: 2,000 rows, unique ids, no blank names, no BOM), taking `id`, `name`, `type_id`, `parent_id` and `jurisdiction_id` and ignoring every other column. Names are copied exactly, read as UTF-8, with nothing trimmed, merged, corrected or normalised: there are 1,523 distinct names and 237 names repeat ("National" 40 times, "London" 8 times, "Unknown" 7 times), each repeat being a separate location with its own id and references; "Cheshire  LJA" (id 1176, two spaces) and "Cheshire LJA" (id 1323) are different locations and both kept as they are; one name contains the en dash `–` (2398 "Royal Courts of Justice – Office of the Judge Advocate General", 62 characters, the longest), written literally in the fixture, not escaped; names also contain `&`, `'`, `(`, `)`, `,`, `-`, `.` and `/`. References: `type_id` (16 distinct values, 1 to 16) refers to `location_types` and `jurisdiction_id` (30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners) to `jurisdictions`, neither served yet, so both are forward references copied unchanged and recorded as a Deferred Item (Constitution IV). `parent_id` refers to another location: 1,858 records have one, and every one resolves to a record in the same extract (no record is its own parent; the hierarchy is at most two levels deep, e.g. 1 "Aberconwy" → 1722 "Old Gwynedd" → 2141 "Gwynedd"). The other 142 records are top-level locations with no parent (type_ids 1, 3, 6 and 14), and MUST be served with `"parent_id": null`, not with the field left out. Today start-up validation requires every declared reference field to be a positive id, so this needs one generic change: a type can declare which of its reference fields may be null (for example `nullable-reference-fields: [parent_id]`), still with no type-specific code; base_locations stays non-null and its responses stay byte-for-byte unchanged. Required tests: every `parent_id` in `locations` resolves to a served location, and, closing 003's DF-1 for `parent_id`, every base location's `parent_id` (408 distinct values) resolves to a served location. Note in the spec that every base location in the extract is also a location with the same E-Links id, but the mock's base_locations ids are synthetic (10 × position) and stay unchanged; the two types are not linked by id. Build checks without the extract: the fixture-rules test must cover this fixture with its own id and ordering rule (ascending extract ids, not code-point name order) while keeping 002's rules for the other types, plus the reference-field and literal-character checks. NoTypeSpecificCodeTest must cover `location` in every spelling (which also covers `base_location`); the one existing main-code mention, the Javadoc phrase "fixture location" in ReferenceDataType, must be reworded. Document known reference points: first (1 "Aberconwy", type 8, parent 1722, jurisdiction 84), an ended record (9 "Ashby-de-la-Zouch and Market Bosworth"), its parent chain (1722 "Old Gwynedd", type 7, parent 2141; 2141 "Gwynedd", type 6, no parent), the two "London" top-level locations (1466, type 6, jurisdiction 84, ended; 1482, type 1, jurisdiction 30), the double-space name (1176 "Cheshire  LJA") next to 1323 "Cheshire LJA", the en-dash name (2398, type 2, parent 1482, jurisdiction 30), the last record (2433 "Kent and Medway", type 16, parent 1925, jurisdiction 86), an unknown id inside the range (2) and one outside it (2434). The singular name `location` is not served and returns `400`, like every still-unsupported E-Links name. Deferred: `location_types` and `jurisdictions` must make every location `type_id` and `jurisdiction_id` resolve, with a test; the People feature decides how an appointment's `location` relates to these records. Deliver unit, integration, functional and smoke coverage, generated OpenAPI documentation (the `attribute_name` enum gains `locations`; the schema documents `parent_id` as nullable) and Postman artifacts (collection and environment, with test scripts), with acceptance scenarios mirroring the existing reference-data features. Deprecated versions are not to be considered."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003` to `006` added BaseLocation, ContractType, Gender and JudiciaryRole through configuration and fixtures. 003 added per-type reference fields (`type_id`, `parent_id`, `jurisdiction_id`) and repeated names (`unique-names: false`), and copied BaseLocation's references unchanged from the extract as forward references to types not yet served (003 DF-1).

This feature adds Location, the sixth of the eleven E-Links reference-data types: the hierarchy of court, tribunal, magistrates' and coroners' locations, regions and areas that base locations sit in. It is the first type that is the **target** of an existing reference: every BaseLocation `parent_id` names a location. That shapes three decisions that differ from 004–006:

- **Ids are the extract's own** (FR-009). Constitution Principle IV and 003 DF-1 require Location to be served with the E-Links ids, so that base locations' `parent_id` values, and locations' own `parent_id` values, resolve. Every other value stays synthetic.
- **Some references are absent.** 142 top-level locations have no parent. They are served with `parent_id` null. Today every declared reference field must hold a positive id, so this feature makes one **generic** change: a type can declare that a reference field may be null (FR-011). No type-specific code is added.
- **References within served types must resolve** (FR-012). Every location `parent_id` resolves to a location, and this feature closes 003 DF-1 for BaseLocation's `parent_id`.

Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to Location unchanged and is not restated here except where this feature tests it. The mock doesn't serve the deprecated alias `location` (FR-001; Constitution Principle VII).

## Clarifications

### Session 2026-10-27

- Q: Should the service itself refuse to start when a `parent_id` points at a location it doesn't serve, or is it enough that automated tests check every reference resolves? → A: Tests only. A build-time test checks that every Location and BaseLocation `parent_id` resolves in the shipped fixtures; start-up validation changes only to accept nullable reference fields (FR-011, FR-012).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `locations` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. Its People section shows that each appointment carries `location` (required) alongside `base_location` and `base_location_id`. The local extract shows that real location data carries `type_id`, `parent_id` and `jurisdiction_id`, with `parent_id` empty for top-level locations. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `locations` | Same |
| Deprecated alias | Out of scope | Not served: `location` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (D-1) and `type_id`, `parent_id`, `jurisdiction_id` (D-2), with `parent_id` null for top-level locations |
| Ids | E-Links' own location ids | Same as the extract (1 to 2433, with gaps); see DR-1 |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | Location records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of locations without names has no practical use. | Permanent, scope-justified (unchanged from 002) |
| D-2 (from 003) | Location records include `type_id`, `parent_id` and `jurisdiction_id`, which the Swagger schema does not list; on Location records `parent_id` may be null | The extract shows the real location data carries them, with no parent for top-level locations, and CTAM reads them. The fields appear only on records of types that declare them, so the other types' responses are unchanged. | Permanent, scope-justified (extended to this type) |

### Data-Rule Deviation

| # | Deviation | Reason |
|---|-----------|--------|
| DR-1 | Location ids are the extract's own ids, not 002's synthetic `10 × position`, and records are numbered for the date rule by position in ascending-id order, not code-point name order | Constitution Principle IV and 003 DF-1: BaseLocation `parent_id` values are E-Links location ids, and so are Location's own `parent_id` values, so they only resolve if Location keeps those ids. This is not a contract deviation (the contract defines no id values); it applies to this type only, and every other generated value follows 002's rules. |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-1 (from 003), partly closed | BaseLocation's `parent_id` → `locations`; `type_id` → `location_types`; `jurisdiction_id` → `jurisdictions` | This feature serves `locations`, so it closes DF-1 for `parent_id` (FR-012, AC-011). `location_types` and `jurisdictions` are not served yet. | `parent_id`: closed by this feature's test. `type_id` and `jurisdiction_id`: unchanged; the features serving `location_types` and `jurisdictions` close them. |
| DF-5 | Location's `type_id` (16 distinct values, 1 to 16) refers to `location_types`, and its `jurisdiction_id` (30, 34, 84, 86) to `jurisdictions`: forward references (Constitution Principle IV) | Neither type is served yet. In the local extracts every value exists in the matching type's extract, so the references will resolve once those types are served with the extract's ids. | The features that serve `location_types` and `jurisdictions` MUST serve them with the extract's ids and MUST add a test that every Location reference to that type resolves to a served record; each is incomplete until its test passes. `type_id` closed by 008 and `jurisdiction_id` closed by 009 ([009 FR-011](../009-jurisdictions-reference-data/spec.md); `ReferenceDataReferencesTest`), so DF-5 is fully closed. |
| DF-6 | Appointments carry `location` (Swagger People schema) | The People feature doesn't exist yet. | The People feature MUST decide how an appointment's `location` relates to these records and, if it refers to them, test that every reference resolves. Since this feature makes the type-specific-code guard forbid `location` in every spelling (FR-018), that feature MUST add a deliberate, reviewed exception for its own `location`, `base_location` and `base_location_id` fields. |

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Locations (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of locations so that my system can load the location hierarchy exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving locations can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/locations` and confirm a `200 OK` JSON response whose `results` array holds all 2,000 location records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/locations`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every Location record in the configured dataset (2,000 by default), in ascending `id` order, each with exactly the fields `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `created_at`, `updated_at`, `start_date` and `end_date`, in that order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** the records at the Reference Points table's ids equal that table (values for `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** every name is returned with exactly the characters of the source: repeated names stay separate records (EC-002), "Cheshire  LJA" keeps its two spaces (EC-003), and the en dash in "Royal Courts of Justice – Office of the Judge Advocate General" is unchanged (EC-004).
4. **AC-004**: **Given** the default dataset, **When** the collection is retrieved, **Then** exactly 285 records are ended — those at positions 7, 14, …, 1995 in ascending-id order, from id 9 to id 2364 — each with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z, and every other record has a null `end_date` and `updated_at` 2024-06-03T10:30:00Z (EC-007).
5. **AC-005**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Location by ID (Priority: P1)

As an integrator, I want to fetch one location by its reference identifier so that my system can resolve an id it holds — such as a base location's `parent_id` — to its full record.

**Why this priority**: Part of the same contract as the collection route, and the way a consumer walks the hierarchy one step at a time.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/locations/{id}` for a reference-point id and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/locations/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including a top-level location whose `parent_id` is null (e.g. `2141`, "Gwynedd") and the en-dash name (`2398`).
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no location (`2`, which falls in an id gap; `2434`, just past the last id; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `10`), **When** it is requested under `locations`, **Then** the result depends only on the Location dataset: identifiers are scoped to their type and never resolve across types (EC-009).

---

### User Story 3 - Integrator Walks the Location Hierarchy (Priority: P1)

As an integrator, I want every parent a location or base location names to be a location the mock serves, so that my system can follow `parent_id` from any base location up to its top-level region without hitting a dangling reference.

**Why this priority**: Resolving parents is the reason Location is served with the extract's ids, and Constitution Principle IV requires references between served types to resolve. It also closes 003's DF-1 for `parent_id`.

**Independent Test**: Load both collections and check that every non-null `parent_id` in either names a served location.

**Acceptance Scenarios**:

1. **AC-010**: **Given** the default dataset, **When** the Location collection is retrieved, **Then** every one of the 1,858 non-null `parent_id` values is the `id` of a served location, no location is its own parent, following parents from any location reaches a top-level location in at most two steps, and the 142 top-level locations carry `"parent_id": null` (the field present, its value null).
2. **AC-011**: **Given** the default datasets, **When** the BaseLocation and Location collections are retrieved, **Then** every BaseLocation `parent_id` (408 distinct values over 1,462 records) is the `id` of a served location, closing 003 DF-1 for `parent_id`.
3. **AC-012**: **Given** the default dataset, **When** the Location collection is retrieved, **Then** every `type_id` is one of the 16 values 1 to 16 and every `jurisdiction_id` is one of 30, 34, 84 and 86, both non-null on every record: they are copied unchanged from the extract and await `location_types` and `jurisdictions` (DF-5).

---

### User Story 4 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `location` rejected clearly, so that my system uses the canonical name `locations` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested.

**Independent Test**: Call `location` and `location/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-013**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/location`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-014**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/location/{reference_id}` for any `reference_id` (one that exists under `locations` such as `1`, an unknown one such as `2`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-015**: **Given** a request to a `location` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name.

---

### User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want location requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each location route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-016**: **Given** a request with no credentials, **When** it is sent to any location route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-017**: **Given** a request with present but invalid credentials, **When** it is sent to any location route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using the five served types, I want adding locations to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: This feature changes main code for the first time since 004, so the regression boundary matters more than for 005 and 006.

**Independent Test**: Re-run 002's to 006's acceptance tests unchanged, compare the existing golden responses byte for byte, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-018**: **Given** the delivered feature, **When** 002's to 006's acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature; in particular every BaseLocation response is byte-for-byte unchanged, with `parent_id` still a positive id on every record. Shared test lists of unsupported names may lose `locations`; nothing else asserted changes.
2. **AC-019**: **Given** valid authentication, **When** each of the 15 E-Links attribute names that are still unsupported (the five canonical types not yet served: `jurisdictions`, `location_types`, `ticket_categories`, `ticket_category_types` and `tickets`; their five singular forms; and the unserved singular names `base_location`, `contract_type`, `gender`, `judiciary_role` and `location`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-020**: **Given** valid authentication, **When** the caller sends a location request with any query parameter (e.g. `?parent_id=1722`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 7 - Maintainer Adds the Type With One Generic Change (Priority: P2)

As a maintainer, I want locations added by declaring the type and supplying its dataset, plus one generic capability that any type can use, so that 002's extension design keeps working without type-specific code.

**Why this priority**: Keeps the remaining types cheap to add and keeps the codebase free of special cases, while allowing the first reference field that is sometimes empty.

**Independent Test**: Review the change, run the start-up validation tests for nullable and non-nullable reference fields, and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-021**: **Given** the delivered change, **When** it is reviewed, **Then** its only main-code changes are the generic nullable-reference-field capability (FR-011) and the rewording of a comment that the widened guard would otherwise match (FR-018); everything else is the type entry, its dataset, tests, documentation and example requests.
2. **AC-022**: **Given** a type that declares a reference field without declaring it nullable, **When** a record holds a null or missing value for that field, **Then** the service refuses to start, naming the fixture, the record and the field, exactly as before; **and given** a type that declares the field nullable, a null value is accepted and served as null, while a value that is present must still be a positive id.
3. **AC-023**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes, and it now also recognises the location type name in every spelling (snake_case, spaced words, camelCase, upper case), so no main source file names a specific reference-data type.
4. **AC-024**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `locations` alongside the existing supported values and does not list `location`, and `parent_id` is documented as nullable, without that documentation having been edited by hand.

---

### User Story 8 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for locations so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every location request's checks pass.

**Acceptance Scenarios**:

1. **AC-025**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the location collection, a single record with a parent (e.g. `1`), a top-level record with a null `parent_id` (e.g. `2141`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `location` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values.

---

### Edge Cases

- **EC-001 — Ids from the extract, with gaps**: Ids run from 1 to 2433 over 2,000 records, so many well-formed ids in that range are unknown (e.g. `2`, `4`, `15`) and return `404`, as does `2434`, past the last id (DR-1).
- **EC-002 — Repeated names**: 237 names occur more than once ("National" 40 times, "London" 8 times, "Unknown" 7 times) over 1,523 distinct names. Each occurrence is a separate location with its own id, type, parent and jurisdiction; none is merged or dropped, and the order of records doesn't depend on names.
- **EC-003 — Internal spacing**: "Cheshire  LJA" (`1176`, two spaces) and "Cheshire LJA" (`1323`) are different locations with different parents. Neither is normalised; both are served as the source has them. Every name is free of leading or trailing whitespace.
- **EC-004 — Non-ASCII characters**: One name contains the en dash `–` (U+2013): "Royal Courts of Justice – Office of the Judge Advocate General" (`2398`, 62 characters, the longest). It is written literally in the dataset, not as an escape, and returned as the same character. Names also contain `&`, `'`, `(`, `)`, `,`, `-`, `.` and `/`.
- **EC-005 — Top-level locations**: 142 locations have no parent (types 1, 3, 6 and 14 in E-Links' numbering, e.g. "London" `1482` and "Gwynedd" `2141`). They are served with `"parent_id": null`; the field is never left out of a Location record, so every Location record has the same nine fields.
- **EC-006 — Ended parents**: End dates are synthetic and assigned by position (FR-009), so a location can be ended while its children are open (24 top-level locations are ended, e.g. "London" `1466`). The mock defines no consistency rule between a location's end date and its parent's; references resolve regardless of end dates.
- **EC-007 — The extract's dates are not reproduced**: The extract end-dates 1,123 rows and carries its own start dates and timestamps. All are ignored; 002's position-based rule applies, ending 285 records.
- **EC-008 — Order**: Records are ordered by ascending id, and the date rule counts positions in that order. Names are not sorted, so repeated names are generally not adjacent.
- **EC-009 — Same name or id, different type**: The extract's base locations are all locations with the same E-Links ids, but the mock's BaseLocation ids are synthetic and stay unchanged, so the two types are not linked by id: "Aberconwy" is `1` under `locations` and `10` under `base_locations`; `locations/10` is "Atherstone and Coleshill"; and `base_locations/1` is unknown (`404`). Identifiers are scoped to their type.
- **EC-010 — Case and spelling variants**: `Locations`, `LOCATIONS`, `locations_` and `Location` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-011 — Inherited edge cases**: Covered by FR-004. For locations, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the Location reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `locations`. The singular name `location` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders`, `judiciary_roles` and `locations`. This supersedes 006's FR-002; every other E-Links attribute name, including `location`, MUST remain unsupported (`400`).
- **FR-003**: Location MUST be added through the generic reference-data capability: a type entry and its dataset, plus the generic capability of FR-011. The change MUST NOT introduce code that names a specific reference-data type or behaves differently for one.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to Location without exception. Names MAY repeat within the type (as for BaseLocation), the type declares the reference fields `type_id`, `parent_id` and `jurisdiction_id`, of which only `parent_id` is nullable, and it declares no boolean fields.

**Response content**

- **FR-005**: Each Location record MUST contain exactly these fields, in this order: `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `created_at`, `updated_at`, `start_date` and `end_date`, the shared fields in 002's formats (002 FR-011). `type_id` and `jurisdiction_id` MUST be positive integers on every record; `parent_id` MUST be a positive integer or null, and MUST be present even when null. Names MUST be returned with exactly the characters of the dataset (FR-008). The records of the existing types MUST stay exactly as 002 to 006 define them.

**Data**

- **FR-006**: The Location dataset MUST be deterministic and supplied as a fixed, checked-in fixture. Only the public `id`, `name`, `type_id`, `parent_id` and `jurisdiction_id` values may be taken from the supplied location reference extract, each copied unchanged; every other value (dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over.
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 2,000 rows in the extract, which have 1,523 distinct names. Each name MUST be carried over exactly as the extract has it, read as UTF-8: nothing trimmed, merged, case-folded, corrected or filtered, and no spacing, apostrophe or dash normalised (EC-002 to EC-004). Non-ASCII characters MUST be written literally in the dataset, not as escapes. Every name is free of leading or trailing whitespace.
- **FR-009**: Records MUST keep the extract's ids (DR-1) and be ordered by ascending id, numbered from position 1 to 2,000 in that order. Records whose position is a multiple of 7 (285 records, ids 9 to 2364) are ended, with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; all other records have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates and timestamps, including its 1,123 end dates, MUST NOT be used (EC-007). A missing `parent_id` in the extract becomes null.
- **FR-010**: Location ids MUST be stable across releases of the default dataset and MUST equal the extract's, so that BaseLocation (003), this type's own `parent_id` values and later features (DF-5, DF-6) can refer to them.

**Generic capability**

- **FR-011**: A type MUST be able to declare which of its declared reference fields may be null. For such a field, a null value MUST be accepted at start-up and served as an explicit null, and a present value MUST still be a positive id. For every other declared reference field, start-up validation MUST behave exactly as before: a null or missing value stops the service with an error naming the fixture, the record and the field. Declaring a field nullable that the type does not declare as a reference field MUST stop the service at start-up. Types that declare no nullable fields MUST produce byte-for-byte the same responses as before.

**Referential integrity**

- **FR-012**: References between served types MUST resolve (Constitution Principle IV), verified by automated tests run on every build against the shipped datasets: every non-null Location `parent_id` MUST be the `id` of a served Location record, and every BaseLocation `parent_id` MUST be the `id` of a served Location record (closing 003 DF-1 for `parent_id`). No Location record may be its own parent, and following parents MUST always reach a top-level location. The service itself does not check that references resolve: start-up validation changes only as FR-011 requires, and a reference field does not declare which type it points at.

**Documentation and example requests**

- **FR-013**: The generated API documentation MUST list `locations` among the supported `attribute_name` values and MUST NOT list `location`, derived from the configuration rather than edited by hand, and MUST document `parent_id` as nullable. The rest of `ReferenceDataResponse` MUST be unchanged.
- **FR-014**: The example request collection MUST cover the location routes as AC-025 describes, using environment variables for every environment-specific value.
- **FR-015**: The project's documentation of supported reference-data types MUST list locations and describe the nullable reference-field option, and the instructions for adding a type MUST remain accurate.

**Quality**

- **FR-016**: The location collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-017**: Unit, integration, functional and smoke coverage MUST be extended to the new type and the new capability, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009), with this type's own id and order rule, MUST be checked by an automated test without the extract, while the other types keep 002's rules; the values are compared with the extract once, when the fixture is generated (FR-007).
- **FR-018**: The automated guard against type-specific code MUST recognise `location` in every spelling (singular and plural, snake_case, spaced words, camelCase, upper case), with no exemption. The one existing main-source mention, the phrase "fixture location" in a comment, MUST be reworded so the guard passes.

### Reference Points

These records of the default dataset are fixed by FR-006, FR-008 and FR-009 and are used by tests and example requests. "Ended" records have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; the others have a null `end_date` and `updated_at` 2024-06-03T10:30:00Z.

| Position | `id` | `name` | `type_id` | `parent_id` | `jurisdiction_id` | Ended |
|----------|------|--------|-----------|-------------|-------------------|-------|
| 1 (first) | 1 | Aberconwy | 8 | 1722 | 84 | |
| 7 (first ended) | 9 | Ashby-de-la-Zouch and Market Bosworth | 8 | 1732 | 84 | yes |
| 8 (id-scoping example) | 10 | Atherstone and Coleshill | 8 | 1751 | 84 | |
| 989 (two spaces) | 1176 | `Cheshire  LJA` (two spaces) | 8 | 1697 | 84 | |
| 1102 | 1323 | Cheshire LJA | 8 | 1476 | 84 | |
| 1225 (top-level, ended) | 1466 | London | 6 | null | 84 | yes |
| 1241 (top-level) | 1482 | London | 1 | null | 30 | |
| 1481 (parent of `1`) | 1722 | Old Gwynedd | 7 | 2141 | 84 | |
| 1900 (top-level; parent of `1722`) | 2141 | Gwynedd | 6 | null | 84 | |
| 1995 (last ended) | 2364 | Newcastle and North Tyneside | 16 | 1820 | 86 | yes |
| 1997 (en dash; longest) | 2398 | Royal Courts of Justice – Office of the Judge Advocate General | 2 | 1482 | 30 | |
| 2000 (last) | 2433 | Kent and Medway | 16 | 1925 | 86 | |

Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. `2` is a guaranteed unknown id inside the range, and `2434` and `999999` are outside it.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/locations` | `200`, 2,000 records |
| Unserved singular name | `GET /api/v1/reference_data/location` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/locations/1` | `200`, Aberconwy, `parent_id` 1722 |
| Top-level record | `GET /api/v1/reference_data/locations/2141` | `200`, Gwynedd, `parent_id` null |
| Unknown id (gap) | `GET /api/v1/reference_data/locations/2` | `404`, shared error shape |
| Unknown id (past the end) | `GET /api/v1/reference_data/locations/2434` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/locations/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/locations?parent_id=1722` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/jurisdictions` | `400`, shared error shape |
| Missing or invalid credentials | any location route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002 and extended by 003 and 004. This feature adds a sixth instance, Location, with canonical name `locations`, no served alias, repeated names allowed, the reference fields `type_id`, `parent_id` and `jurisdiction_id`, and `parent_id` declared nullable. A type's declaration gains one optional element: the list of its reference fields that may be null (FR-011).
- **Location record**: One court, tribunal, magistrates' or coroners' location, region or area in E-Links' location hierarchy. Attributes: `id` (unique, stable, the E-Links id), `name` (public name, exactly as in the source, not necessarily unique), `type_id` (E-Links location type), `parent_id` (the parent Location's `id`, or null for a top-level location), `jurisdiction_id` (E-Links jurisdiction), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. BaseLocation records refer to it through their `parent_id`; it refers to itself through `parent_id`, and to `location_types` and `jurisdictions` through forward references (DF-5).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 25 acceptance scenarios (AC-001 to AC-025) pass as automated tests or, for AC-021 and AC-025, as review and collection-run checks.
- **SC-002**: The default location collection contains exactly 2,000 records with 1,523 distinct names, 285 of them ended and 142 with a null `parent_id`, matching the Reference Points table.
- **SC-003**: 100% of the 2,000 served records carry the same `id`, `name`, `type_id`, `parent_id` and `jurisdiction_id` as the source row, character for character.
- **SC-004**: 100% of non-null `parent_id` values, in both Location (1,858) and BaseLocation (1,462) records, resolve to a served location; 0 dangling parent references remain.
- **SC-005**: 0 requests via the unserved singular name `location` return reference data; 100% with valid credentials return `400`.
- **SC-006**: 0 of the 15 still-unsupported attribute names return a `200` response.
- **SC-007**: 0 location requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-008**: 100% of 002's to 006's acceptance tests pass with their assertions and expected responses unchanged, and every existing golden response is byte-for-byte identical.
- **SC-009**: 0 lines of main code name a specific reference-data type; the only main-code changes are the generic nullable-reference-field capability and one reworded comment.
- **SC-010**: The location collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-011**: An integrator using the published documentation and example requests retrieves the location list, a single location and its parent at the first attempt, without reading source code.

## Assumptions

- **Source values are public reference data**: Location names, ids and their type, parent and jurisdiction codes describe public court and administrative locations, not people, so reusing them is consistent with Constitution Principle IV. Dates and timestamps are synthetic.
- **The extract's ids are required, not chosen**: Principle IV's forward-reference exception, as 003 applied it, requires a referenced type to be served with the extract's ids. Renumbering locations would leave every BaseLocation `parent_id` dangling, so DR-1 is the only compliant option.
- **Names are reproduced, not curated**: The mock stands in for the real reference data, so repeated names and irregular spacing are part of what consumers must handle (EC-002, EC-003).
- **Same date rules as 002**: The dates, timestamps and ended-record rule are 002's, counted over ascending-id positions. For 2,000 records the rule ends 285, so this type meets 002's FR-023 on its own (ended records and id gaps).
- **Null, not absent**: A top-level location's `parent_id` is served as null rather than left out, so every Location record has the same fields and consumers can distinguish "no parent" from "type has no parent field". Types that don't declare the field still leave it out, as today.
- **References are checked by tests, not at start-up**: The shipped datasets are fixed and checked in, so a build-time test catches any reference that doesn't resolve before it can be served. Checking at start-up would need each reference field to declare its target type and would have to skip the forward references that may still dangle (DF-1, DF-5), for no gain with fixed data (Clarifications).
- **BaseLocation stays as it is**: Its ids remain synthetic (10 × position) and its references unchanged. That every base location is also a location in the extract is noted (EC-009) but not modelled; linking the two by id would change BaseLocation's stable ids.
- **Deprecated names are out of scope**: The feature description excludes deprecated versions, so only `locations` is served, as for the singular names of BaseLocation, ContractType, Gender and JudiciaryRole. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The guard's fragment is `location`**: The type-specific-code guard matches `location` in every spelling in main code, with no exemptions; this also covers `base_location`. The one existing mention is a comment and is reworded. The People feature will add a reviewed exception for its own location fields (DF-6).
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged. At 2,000 records this becomes the largest served collection (BaseLocation has 1,462), and the smoke suite's guard applies to it as to the others.
- **Fixture source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 006 delivered: the generic routes, central type registry with alias resolution, per-type declarations (reference fields, boolean fields, repeated names) and fixture validation at start-up, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, the fixture generation-rules test with its literal-character check, and the automated guard against type-specific code.
