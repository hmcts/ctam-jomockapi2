# Feature Specification: Reference Data API — Base Locations

**Feature Branch**: `003-base-locations-reference-data`

**Created**: 2026-10-08

**Status**: Draft

**Input**: User description: "Add the base_locations reference-data type to the existing generic reference-data API (feature 002), served by both routes — `GET /api/v1/reference_data/base_locations` and `GET /api/v1/reference_data/base_locations/{reference_id}` — with the deprecated alias `base_location`, exactly as `appointment_titles` / `appointment_title` work today. Reuse the generic mechanism (a type entry in configuration and a checked-in fixture, no type-specific code). Records use the same fields as appointment titles; the extract's `type_id`, `parent_id` and `jurisdiction_id` columns are out of scope and deferred. Generate the fixture from the public name column of the local extract only: de-duplicate (1,462 rows, 1,276 distinct names), sort in code-point order, ids of 10 × position, dates as in 002; say how the one non-ASCII name is handled. Document known reference points. The latency guards must still pass for a collection ~6.5× larger. Deliver unit, integration, functional and smoke coverage, generated API documentation and Postman artifacts, with acceptance scenarios mirroring 002."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served a single type through it, AppointmentTitle (`appointment_titles`, alias `appointment_title`). Its User Story 6 (AC-019, SC-007) promised that a later feature could add another type by supplying a type definition and its data, without new route, validation, authentication or error-handling logic.

This feature is the first to use that promise. It adds BaseLocation, the second of the eleven E-Links reference-data types. Everything 002 specified for AppointmentTitle — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to BaseLocation unchanged, and is not restated here except where this feature tests it. One difference is deliberate: the mock doesn't serve BaseLocation's deprecated alias `base_location` (Clarifications; Constitution Principle VII, v1.11.0).

## Clarifications

### Session 2026-10-08

- Q: Should base-location names that differ only by extra internal spaces or capitalisation be kept as separate records, or merged into one? → A: Kept as separate records, verbatim. `Cheshire  LJA` / `Cheshire LJA` and `City of Westminster Sub Committee` / `City of Westminster Sub committee` each remain two records, so the default dataset has 1,276 records.
- Q: Should the seven placeholder-style names in the extract ("Unknown" and "Unknown Sub Committee S2/S3/S4/S5/S9/S94") be served as base locations, or left out of the dataset? → A: Kept, like any other name. They are real values of the E-Links data, at positions 1,141–1,147 (ids 11410–11470).
- Q: Should the one name containing an en dash ("Royal Courts of Justice – Office of the Judge Advocate General") keep the en dash exactly, or should it be replaced with a plain hyphen? → A: Keep the en dash (U+2013) exactly; it is served as UTF-8 and is the only non-ASCII name (position 888, id 8880).
- Q: Should the mock serve the deprecated alias `base_location`? → A: No. Only the canonical name `base_locations` is served; `base_location` is rejected with `400` like any unsupported attribute name. Constitution v1.11.0 (Principles I and VII) makes serving a deprecated alias optional per type and requires the spec to state the choice, so this is not a contract deviation. AppointmentTitle's alias `appointment_title` is unaffected.

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `base_locations` as a canonical `attribute_name` value and `base_location` as its deprecated alias. It defines one generic `ReferenceDataResponse` shape for every type, with no type-specific fields.

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `base_locations` | Same |
| Deprecated alias | `base_location` | Not served: rejected with `400` like any unsupported name (Clarifications; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (deviation D-1 from 002, extended to this type) |
| Type-specific fields | None documented | None (see Deferred Items) |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | BaseLocation records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for AppointmentTitle: the reference extract shows the real data carries a name, and a list of base locations without names has no practical use. This feature applies the existing, agreed deviation to the new type; it introduces no new deviation. | Permanent, scope-justified (unchanged from 002) |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-1 | The extract's `type_id`, `parent_id` and `jurisdiction_id` columns are not served | They refer to location types, other base locations and jurisdictions. The mock serves no location types or jurisdictions yet, so these values would point at records that don't exist, breaking data coherence (Constitution Principle IV). The Swagger schema also documents no such fields. | A later feature that adds `location_types` and `jurisdictions` decides whether BaseLocation records gain these references, and if so generates them so every reference resolves to a served record. |

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of 002's IDs.*

### User Story 1 - Integrator Retrieves All Base Locations (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of base locations so that my system can load and resolve base-location reference data exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving base locations can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/base_locations` and confirm a `200 OK` JSON response whose `results` array holds all 1,276 base-location records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/base_locations`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every BaseLocation record in the configured dataset (1,276 by default), each in the reference-data record shape, in ascending `id` order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** the first, an ended and the last record equal the reference points in the Reference Points table (values for `id`, `name`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** the record whose name contains a non-ASCII character is returned with that character exactly as in the source name (see FR-010), in valid UTF-8 JSON.
4. **AC-004**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Base Location by ID (Priority: P1)

As an integrator, I want to fetch one base location by its reference identifier so that my system can resolve an identifier it holds to its full reference record.

**Why this priority**: Part of the same contract as the collection route, and it exercises the identifier validation paths.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/base_locations/{id}` for a reference-point id and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-005**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/base_locations/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response.
2. **AC-006**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no base location (e.g. `15`, which falls in an id gap, or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-007**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-008**: **Given** an `id` that exists in the AppointmentTitle dataset but not in the BaseLocation dataset (or the reverse), **When** it is requested under `base_locations`, **Then** the result depends only on the BaseLocation dataset: identifiers are scoped to their type and never resolve across types.

---

### User Story 3 - The Deprecated Alias Is Not Served (Priority: P2)

As an integrator, I want the deprecated singular name `base_location` rejected clearly, so that my system moves to the canonical name `base_locations` instead of relying on an alias the mock doesn't serve.

**Why this priority**: The alias is deprecated in the E-Links contract itself, and this feature chooses not to serve it (Clarifications). The choice must be observable and tested, so a request via the alias never returns data.

**Independent Test**: Call `base_location` and `base_location/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-009**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/base_location`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-010**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/base_location/{reference_id}` for any `reference_id` (one that exists under `base_locations` such as `70`, an unknown one such as `15`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-011**: **Given** a request to a `base_location` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name, as for any unsupported name.

---

### User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want base-location requests without valid credentials refused exactly as appointment-title requests are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each base-location route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-012**: **Given** a request with no credentials, **When** it is sent to any base-location route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-013**: **Given** a request with present but invalid credentials, **When** it is sent to any base-location route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using appointment titles, I want adding base locations to leave appointment titles and every still-unbuilt type behaving exactly as before.

**Why this priority**: Adding a type through configuration must not change any other type's behaviour; this is the regression boundary of the feature.

**Independent Test**: Re-run 002's appointment-title acceptance tests unchanged, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-014**: **Given** the delivered feature, **When** 002's AppointmentTitle acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature (shared test helpers may be moved without changing what is asserted).
2. **AC-015**: **Given** valid authentication, **When** each of the 19 E-Links attribute names that are still unsupported (the nine canonical types other than `appointment_titles` and `base_locations`, their nine deprecated aliases, e.g. `genders`, `gender`, and `base_location`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-016**: **Given** valid authentication, **When** the caller sends a base-location request with any query parameter (e.g. `?name=Aberconwy`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for appointment titles.

---

### User Story 6 - Maintainer Adds the Type Without Code Changes (Priority: P2)

As a maintainer, I want base locations added purely by declaring the type and supplying its dataset, so that 002's extension design is proven on a real type and the remaining nine types can follow the same path.

**Why this priority**: Confirms the extensibility 002 was designed for (its AC-019 and SC-007) and keeps the cost of each later type low.

**Independent Test**: Review the change and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-017**: **Given** the delivered change, **When** it is reviewed, **Then** it adds no route, handler, validation, authentication, error-handling or mapping logic; the only additions are the type entry, its dataset, tests, documentation and example requests.
2. **AC-018**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes: no main source file names a specific reference-data type.
3. **AC-019**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `base_locations` as a supported value alongside the appointment-title values, does not list `base_location`, and still lists `appointment_title` as the only deprecated value, without that documentation having been edited by hand.

---

### User Story 7 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for base locations so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every base-location request's checks pass.

**Acceptance Scenarios**:

1. **AC-020**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the base-location collection, a single record by a reference-point id, an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, and the unserved alias `base_location` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values.

---

### Edge Cases

- **EC-001 — Duplicate names in the extract**: The extract holds 1,462 rows but only 1,276 distinct names. Each distinct name yields exactly one record; repeated rows add nothing, so no two records share a name. Names are compared exactly: two pairs that differ only by internal spacing or capitalisation (`Cheshire  LJA` / `Cheshire LJA`, `City of Westminster Sub Committee` / `City of Westminster Sub committee`) are distinct names and each yields its own record, unmodified.
- **EC-002 — Non-ASCII name**: One distinct name contains an en dash (U+2013): "Royal Courts of Justice – Office of the Judge Advocate General". It is kept exactly as in the extract, not replaced with a hyphen or otherwise normalised (FR-010; confirmed in Clarifications), and is ordered by code point like every other name, which places it at position 888.
- **EC-003 — Placeholder-like names**: The extract includes seven placeholder-style names: "Unknown" and "Unknown Sub Committee S2", "S3", "S4", "S5", "S9" and "S94". They are real values of the E-Links data and are served like any other name, not filtered out, at positions 1,141 to 1,147 (ids 11410 to 11470).
- **EC-004 — Identifier gaps**: Ids are multiples of 10, so a well-formed id between two of them (e.g. `15`) or beyond the last one (e.g. `12770`) returns `404`.
- **EC-005 — Same id, different type**: `GET /api/v1/reference_data/base_locations/10` and `GET /api/v1/reference_data/appointment_titles/10` return different records (Aberconwy and Acting Senior Coroner); identifiers are scoped to their type.
- **EC-006 — Case and spelling variants**: `Base_Locations`, `BASE_LOCATIONS` and `base-locations` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-007 — Large collection response**: The default collection holds about 6.5 times as many records as the appointment-title collection. It is still returned in a single response, with no pagination, as the contract defines none.
- **EC-008 — Inherited edge cases**: Covered by FR-004. For base locations, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the BaseLocation reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `base_locations` only. The deprecated alias `base_location` MUST NOT be served (Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title` and `base_locations`. This supersedes 002's FR-003 and FR-007, which limited support to AppointmentTitle; every other E-Links attribute name, including `base_location`, MUST remain unsupported (`400`).
- **FR-003**: BaseLocation MUST be added through the generic reference-data capability alone: a type entry and its dataset. The change MUST NOT add or modify route, validation, authentication, error-handling or mapping logic, and MUST NOT introduce code that names a specific reference-data type.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to BaseLocation without exception.

**Response content**

- **FR-005**: Each BaseLocation record MUST contain exactly the fields of an AppointmentTitle record: `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`, in the same formats (002 FR-011). Records MUST NOT contain any other field; in particular `type_id`, `parent_id` and `jurisdiction_id` MUST NOT appear (DF-1).

**Data**

- **FR-006**: The BaseLocation dataset MUST be synthetic, deterministic and supplied as a fixed, checked-in fixture. Only the public `name` values may be taken from the supplied BaseLocation reference extract; every other value MUST be synthetic, and no other column of the extract may be carried over.
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 1,276 distinct names in the extract, no more and no fewer. Names MUST be unique and free of leading or trailing whitespace. Uniqueness is exact (character-for-character, case-sensitive); names differing only by internal spacing or capitalisation are distinct and MUST NOT be merged or rewritten.
- **FR-009**: Records MUST be generated following 002's fixture generation rules: names sorted in code-point order and numbered from position 1 to 1,276; `id` = 10 × position (10 to 12,760); records whose position is a multiple of 7 (182 records) have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; all other records (1,094) have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01.
- **FR-010**: Names MUST be carried over character-for-character, with no transliteration or normalisation of non-ASCII characters. The fixture and every response MUST encode them as UTF-8.
- **FR-011**: BaseLocation ids MUST be stable across releases of the default dataset, so that later features (for example, People API appointments) and consumer test suites can refer to them.

**Documentation and example requests**

- **FR-012**: The generated API documentation MUST list `base_locations` among the supported `attribute_name` values and MUST NOT list `base_location`, derived from the type entry rather than edited by hand.
- **FR-013**: The example request collection MUST cover the base-location routes as AC-020 describes, using environment variables for every environment-specific value.
- **FR-014**: The project's documentation of supported reference-data types MUST list base locations, and the instructions for adding a type MUST remain accurate.

**Quality**

- **FR-015**: The base-location collection route MUST meet the same latency guards as the appointment-title collection route, under both the default bound and the strict target.
- **FR-016**: Unit, integration, functional and smoke coverage MUST be extended to the new type, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass.

### Reference Points

These records of the default dataset are fixed by FR-009 and are used by tests and example requests:

| Position | `id` | `name` | `end_date` | `updated_at` |
|----------|------|--------|------------|--------------|
| 1 (first) | 10 | Aberconwy | null | 2024-06-03T10:30:00Z |
| 7 (first ended) | 70 | Aldridge and Brownhills | 2025-03-31 | 2025-04-01T08:00:00Z |
| 888 (non-ASCII) | 8880 | Royal Courts of Justice – Office of the Judge Advocate General | null | 2024-06-03T10:30:00Z |
| 1,276 (last) | 12760 | Yorkshire & Humberside | null | 2024-06-03T10:30:00Z |

Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/base_locations` | `200`, 1,276 records |
| Unserved alias | `GET /api/v1/reference_data/base_location` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/base_locations/70` | `200`, Aldridge and Brownhills |
| Unknown id (gap) | `GET /api/v1/reference_data/base_locations/15` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/base_locations/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/base_locations?name=Aberconwy` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/genders` | `400`, shared error shape |
| Missing or invalid credentials | any base-location route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002. This feature adds a second instance, BaseLocation, with canonical name `base_locations` and no served alias.
- **BaseLocation record**: One judicial base location (a court, tribunal venue, bench or committee area). Attributes: `id` (unique, stable integer), `name` (unique public location name, deviation D-1), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. Its identifiers are independent of AppointmentTitle identifiers. Relationships to location types, parent locations and jurisdictions are deferred (DF-1).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 20 acceptance scenarios (AC-001 to AC-020) pass as automated tests or, for AC-017 and AC-020, as review and collection-run checks.
- **SC-002**: The default base-location collection contains exactly 1,276 records with 1,276 distinct names, and the four reference points match the Reference Points table.
- **SC-003**: 0 requests via the unserved alias `base_location` return reference data; 100% with valid credentials return `400`.
- **SC-004**: 0 of the 19 still-unsupported attribute names return a `200` response.
- **SC-005**: 0 base-location requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-006**: 100% of 002's AppointmentTitle acceptance tests pass with their assertions and expected responses unchanged.
- **SC-007**: Adding the type changes 0 lines of route, validation, authentication, error-handling or mapping logic.
- **SC-008**: The base-location collection meets the same latency bounds as the appointment-title collection in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-009**: An integrator using the published documentation and example requests retrieves the base-location list and a single base location at the first attempt, without reading source code.

## Assumptions

- **Source names are public**: Base-location names are public names of courts, tribunal venues, benches and committee areas, not personal data. A scan of the 1,276 distinct names for honorifics, judicial titles and possessive forms found no personal names; the only possessives are place names (King's Lynn, Mayor's and City of London Court). Reusing them, as 002 reused public title names, is consistent with Principle IV; every other value is synthetic.
- **Same generation rules as 002**: The dates, timestamps and ended-record rule are 002's, applied unchanged, so both default datasets follow one documented pattern. The 182 ended records give the dataset both null and non-null end dates, and the multiples of 10 give it id gaps, as 002's FR-023 requires of a dataset.
- **Code-point order**: Sorting uses the same code-point order as 002. All names but one are ASCII; the en dash (U+2013) sorts after every ASCII character, which fixes the non-ASCII name at position 888.
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response, as for appointment titles.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) from 002's smoke suite apply unchanged. Meeting them with the larger collection is a requirement on this feature, not a reason to relax the bounds.
- **No new contract deviation**: Deviation D-1 (`name`) already covers every reference-data type served through the shared record shape; this feature extends it to BaseLocation and records no new deviation.
- **Fixture location and source**: The extract used for generation is the local, gitignored `joh-elinks-api/ReferenceData/REF_BaseLocation.csv`. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 delivered: the generic routes, central type registry with alias resolution, fixture validation at start-up, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
