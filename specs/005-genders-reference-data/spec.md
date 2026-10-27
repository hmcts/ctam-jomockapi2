# Feature Specification: Reference Data API — Genders

**Feature Branch**: `005-genders-reference-data`

**Created**: 2026-10-21

**Status**: Implemented

**Input**: User description: "Add the genders reference-data type to the existing generic reference-data API (feature 002). Contract reference (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `genders` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` ("The found reference data": `ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` has `id`, `updated_at`, `created_at`, `start_date` and `end_date`, all required; `CurrentOfficeHolderResponse` (People) carries `gender_id` (required), plus `gender` and `sex`. Scope: serve `genders` on `GET /api/v1/reference_data/genders` and `/{reference_id}`, reusing the generic mechanism (a type entry and a checked-in fixture, no type-specific code, no main-code change; NoTypeSpecificCodeTest must cover genders in every spelling); names unique; no reference or boolean fields; authentication, validation, error shape, ordering and edge cases as 002. Record shape: the Swagger fields plus `name` (existing deviation), nothing else. Fixture: generated once from the `name` column of the local gender reference extract (4 rows: "Male", "Female", "Other", "Prefer not to say"), ignoring every other column; note that the extract's ids are 1, 3, 4 and 5 and no row is end-dated; code-point order (Female, Male, Other, Prefer not to say); ids 10 × position; dates by 002's rule, so no record is ended — state that 002's ended-record requirement doesn't hold for this type while the id gaps still do; reference points: first, last, the name with spaces, an unknown id inside the range and one outside. Deferred: the People feature's office holders must reference these synthetic gender ids (`gender_id`), with a test that every reference resolves; how `gender` and `sex` relate is left to that feature. Every still-unsupported E-Links name must be rejected with `400`. Deliver unit, integration, functional and smoke coverage, generated OpenAPI documentation and Postman artifacts, with acceptance scenarios mirroring the existing reference-data features. Deprecated versions are not to be considered."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003-base-locations-reference-data` and `004-contract-types-reference-data` added BaseLocation and ContractType through configuration and fixtures, with generic extensions (repeated names, reference fields, boolean fields).

This feature adds Gender, the fourth of the eleven E-Links reference-data types: the gender categories a judicial office holder's record can carry. It is the simplest type so far: its records have only the shared fields, so it needs a type entry and a fixture and **no change to main code**. Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to Gender unchanged and is not restated here except where this feature tests it. Two differences are deliberate: the mock doesn't serve the deprecated alias `gender` (Clarifications; Constitution Principle VII), and, because the type has only four records, none of them is ended (FR-009, EC-004).

## Clarifications

### Session 2026-10-21

- Q: Should the mock serve the deprecated alias `gender`? → A: No. The feature description excludes deprecated versions, so only the canonical name `genders` is served; `gender` is rejected with `400` like any unsupported attribute name, as `base_location` and `contract_type` are. Constitution Principles I and VII make serving a deprecated alias optional per type and require the spec to state the choice, so this is not a contract deviation. AppointmentTitle's alias `appointment_title` is unaffected.
- Q: 002's generation rule ends every seventh record, which gives no ended record for a four-record type, while 002 FR-023 asks a dataset for at least one ended record. Which wins? → A: The generation rule, unchanged, and no waiver is needed. 002 was written when there was one type, so FR-023's "default dataset" is read as the shipped default data across all types: its ended-record clause is met by the other types' ended records, and its id-gap clause by every type's multiples of 10. Gender has only open records. 002 states this reading next to FR-023 (T032). Ending a gender category only to satisfy the rule would misrepresent the data, and no row of the extract is ended.
- Q: Should the type guard forbid the word "gender" in any form in main Java code, even though the future People feature will need fields called `gender` and `gender_id`? → A: Yes. The guard forbids `gender` in every spelling now, as it does for the other types' names; it doesn't exempt `gender`, `gender_id` or the plural only. The People feature adds a deliberate, reviewed exception for its own fields when it exists (DF-3).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `genders` among the 11 canonical `attribute_name` values and `gender` among the deprecated values. It defines one generic `ReferenceDataResponse` shape for every type. Its People section shows that each current office holder (`CurrentOfficeHolderResponse`) carries `gender_id` (required) and also `sex` (required) and `gender`.

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `genders` | Same |
| Deprecated alias | `gender` | Not served: rejected with `400` like any unsupported name (Clarifications; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (deviation D-1 from 002, extended to this type) |
| Type-specific fields | None documented; the extract has none beyond the shared fields | None |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | Gender records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of genders without names has no practical use. This feature applies the existing, agreed deviation to the new type. | Permanent, scope-justified (unchanged from 002) |

No other deviation: Gender records carry no reference or boolean fields.

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-3 | Current office holders will refer to genders through `gender_id` (Swagger `CurrentOfficeHolderResponse`) | The People feature doesn't exist yet. Gender is served first, so this is a backward reference that Constitution Principle IV requires to resolve once both types are served. | The People feature MUST set every office holder's `gender_id` to the `id` of a served Gender record (this feature's synthetic ids, not the extract's) and MUST add a test that every office holder's `gender_id` resolves; that feature is incomplete until the test passes. It MUST also decide how `gender` and `sex` relate to the Gender type, and MUST add a deliberate, reviewed exception to the type-specific-code guard (FR-016) for its own `gender` and `gender_id` fields, since this feature makes the guard forbid `gender` in every spelling. |

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Genders (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of genders so that my system can load and resolve gender reference data exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving genders can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/genders` and confirm a `200 OK` JSON response whose `results` array holds all 4 gender records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/genders`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every Gender record in the configured dataset (4 by default), each with exactly the shared reference-data fields, in ascending `id` order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** the records equal the Reference Points table (values for `id`, `name`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** every record has a null `end_date` (no gender is ended; EC-004), and the name with spaces, "Prefer not to say", is returned exactly as in the source.
4. **AC-004**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Gender by ID (Priority: P1)

As an integrator, I want to fetch one gender by its reference identifier so that my system can resolve an identifier it holds (for example an office holder's `gender_id`) to its full reference record.

**Why this priority**: Part of the same contract as the collection route, and it exercises the identifier validation paths.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/genders/{id}` for a reference-point id and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-005**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/genders/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response.
2. **AC-006**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no gender (`15`, which falls in an id gap; `50`, just past the last id; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-007**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-008**: **Given** an `id` that exists in another type's dataset (e.g. `10`, which is also an AppointmentTitle, BaseLocation and ContractType id), **When** it is requested under `genders`, **Then** the result depends only on the Gender dataset: identifiers are scoped to their type and never resolve across types.

---

### User Story 3 - The Deprecated Alias Is Not Served (Priority: P2)

As an integrator, I want the deprecated singular name `gender` rejected clearly, so that my system moves to the canonical name `genders` instead of relying on an alias the mock doesn't serve.

**Why this priority**: The alias is deprecated in the E-Links contract itself, and this feature chooses not to serve it (Clarifications). The choice must be observable and tested, so a request via the alias never returns data.

**Independent Test**: Call `gender` and `gender/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-009**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/gender`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-010**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/gender/{reference_id}` for any `reference_id` (one that exists under `genders` such as `20`, an unknown one such as `15`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-011**: **Given** a request to a `gender` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name, as for any unsupported name.

---

### User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want gender requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each gender route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-012**: **Given** a request with no credentials, **When** it is sent to any gender route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-013**: **Given** a request with present but invalid credentials, **When** it is sent to any gender route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using appointment titles, base locations or contract types, I want adding genders to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: Adding a type through configuration must not change any other type's behaviour; this is the regression boundary of the feature.

**Independent Test**: Re-run 002's, 003's and 004's acceptance tests unchanged, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-014**: **Given** the delivered feature, **When** 002's, 003's and 004's acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature. Shared test lists of unsupported names may lose `genders`, and examples that used `genders` as an unsupported type, such as the missing-token check and the Postman "unsupported" request, move to another still-unsupported name (`judiciary_roles`). Nothing else asserted changes.
2. **AC-015**: **Given** valid authentication, **When** each of the 17 E-Links attribute names that are still unsupported (the seven canonical types not yet served, e.g. `judiciary_roles`; their seven deprecated aliases, e.g. `judiciary_role`; and the unserved aliases `base_location`, `contract_type` and `gender`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-016**: **Given** valid authentication, **When** the caller sends a gender request with any query parameter (e.g. `?name=Male`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 6 - Maintainer Adds the Type With Configuration and Data Only (Priority: P2)

As a maintainer, I want genders added by declaring the type and supplying its dataset alone, with no change to main code, so that 002's extension design is shown to work unaided for a type with only the shared fields.

**Why this priority**: Confirms the extensibility 002 was designed for (its AC-019 and SC-007) in its simplest form, and keeps the cost of the remaining types low.

**Independent Test**: Review the change and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-017**: **Given** the delivered change, **When** it is reviewed, **Then** it changes no main source code at all; every addition is the type entry, its dataset, tests, documentation and example requests.
2. **AC-018**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes, and it now also recognises the gender type name in every spelling (snake_case, spaced words, camelCase, upper case), so no main source file names a specific reference-data type.
3. **AC-019**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `genders` as a supported value alongside the existing ones, does not list `gender`, and `ReferenceDataResponse` is unchanged, without that documentation having been edited by hand.

---

### User Story 7 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for genders so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every gender request's checks pass.

**Acceptance Scenarios**:

1. **AC-020**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the gender collection, a single record by reference-point id, an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved alias `gender` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values.

---

### Edge Cases

- **EC-001 — The extract's ids are not used**: The extract's ids are 1, 3, 4 and 5 (there is no 2). They are never served: every record's `id` is assigned by position (FR-009).
- **EC-002 — Name with spaces**: "Prefer not to say" contains spaces. It is kept verbatim and returned as an ordinary JSON string; names never appear in a request path, and query parameters are rejected (AC-016).
- **EC-003 — Placeholder-like names**: "Other" and "Prefer not to say" are real values of the E-Links data and are served like any other name, not filtered out.
- **EC-004 — No ended record**: With four records, no position is a multiple of 7, so 002's rule ends none; every record has a null `end_date` and `updated_at` 2024-06-03T10:30:00Z. 002 FR-023 is read across the shipped default data, whose other types include ended records, so no waiver is needed (Clarifications); Gender's own ids still have gaps (EC-005). Consumers' handling of ended records is exercised through the other types.
- **EC-005 — Identifier gaps**: Ids are multiples of 10, so a well-formed id between two of them (e.g. `15`) or beyond the last one (e.g. `50`) returns `404`.
- **EC-006 — Same id, different type**: `GET /api/v1/reference_data/genders/10` returns "Female", while id `10` under `appointment_titles`, `base_locations` and `contract_types` returns "Acting Senior Coroner", "Aberconwy" and "Fee-paid"; identifiers are scoped to their type.
- **EC-007 — Case and spelling variants**: `Genders`, `GENDERS` and `genders_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-008 — Code-point order**: The order is Female, Male, Other, Prefer not to say, fixed by the generation rule, not by any business meaning of the names.
- **EC-009 — Inherited edge cases**: Covered by FR-004. For genders, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the Gender reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `genders`. The deprecated alias `gender` MUST NOT be served (Clarifications; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types` and `genders`. This supersedes 004's FR-002; every other E-Links attribute name, including `gender`, MUST remain unsupported (`400`).
- **FR-003**: Gender MUST be added through the generic reference-data capability alone: a type entry and its dataset. The change MUST NOT modify any main source code, and MUST NOT introduce code that names a specific reference-data type.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to Gender without exception. Names MUST be unique within the type (the default), and the type declares no reference fields and no boolean fields.

**Response content**

- **FR-005**: Each Gender record MUST contain exactly 002's fields, in 002's order: `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`, in 002's formats (002 FR-011). The records of the existing types MUST stay exactly as 002, 003 and 004 define them.

**Data**

- **FR-006**: The Gender dataset MUST be synthetic, deterministic and supplied as a fixed, checked-in fixture. Only the public `name` values may be taken from the supplied gender reference extract; every other value (`id`, dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over.
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 4 rows in the extract, which have 4 distinct names, carried over verbatim and free of leading or trailing whitespace.
- **FR-009**: Records MUST be generated following 002's fixture generation rules: names sorted in code-point order and numbered from position 1 to 4; `id` = 10 × position (10 to 40); records whose position is a multiple of 7 are ended (none, for this type); all records have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own ids, dates and timestamps MUST NOT be used (EC-001).
- **FR-010**: Gender ids MUST be stable across releases of the default dataset, so that the People feature (DF-3) and consumer test suites can refer to them.

**Documentation and example requests**

- **FR-011**: The generated API documentation MUST list `genders` among the supported `attribute_name` values and MUST NOT list `gender`, derived from the configuration rather than edited by hand. The `ReferenceDataResponse` schema MUST be unchanged.
- **FR-012**: The example request collection MUST cover the gender routes as AC-020 describes, using environment variables for every environment-specific value.
- **FR-013**: The project's documentation of supported reference-data types MUST list genders, and the instructions for adding a type MUST remain accurate.

**Quality**

- **FR-014**: The gender collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-015**: Unit, integration, functional and smoke coverage MUST be extended to the new type, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass.
- **FR-016**: The automated guard against type-specific code MUST enforce FR-003's ban for this type: it MUST recognise the word `gender` in every spelling (singular and plural, snake_case, spaced words, camelCase, upper case), as it does for the existing types, with no exemption for field names such as `gender` or `gender_id` (Clarifications).

### Reference Points

These records of the default dataset are fixed by FR-008 and FR-009 and are used by tests and example requests. They are the whole dataset:

| Position | `id` | `name` | `end_date` | `updated_at` |
|----------|------|--------|------------|--------------|
| 1 (first) | 10 | Female | null | 2024-06-03T10:30:00Z |
| 2 | 20 | Male | null | 2024-06-03T10:30:00Z |
| 3 | 30 | Other | null | 2024-06-03T10:30:00Z |
| 4 (last; name with spaces) | 40 | Prefer not to say | null | 2024-06-03T10:30:00Z |

Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. `15` is a guaranteed unknown id inside the range, and `50` and `999999` are outside it.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/genders` | `200`, 4 records |
| Unserved alias | `GET /api/v1/reference_data/gender` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/genders/20` | `200`, Male |
| Unknown id (gap) | `GET /api/v1/reference_data/genders/15` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/genders/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/genders?name=Male` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/jurisdictions` | `400`, shared error shape |
| Missing or invalid credentials | any gender route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002. This feature adds a fourth instance, Gender, with canonical name `genders`, no served alias, and no reference or boolean fields.
- **Gender record**: One gender category that a judicial office holder's record can carry. Attributes: `id` (unique, stable integer), `name` (unique public category name, deviation D-1), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date` (always null in the default dataset). Its identifiers are independent of other types' identifiers. Office holders will refer to it by `gender_id` (DF-3).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 20 acceptance scenarios (AC-001 to AC-020) pass as automated tests or, for AC-017 and AC-020, as review and collection-run checks.
- **SC-002**: The default gender collection contains exactly 4 records with 4 distinct names, all open, matching the Reference Points table.
- **SC-003**: 0 requests via the unserved alias `gender` return reference data; 100% with valid credentials return `400`.
- **SC-004**: 0 of the 17 still-unsupported attribute names return a `200` response.
- **SC-005**: 0 gender requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-006**: 100% of 002's, 003's and 004's acceptance tests pass with their assertions and expected responses unchanged, apart from the unsupported-name examples AC-014 allows to move.
- **SC-007**: Adding the type changes 0 lines of main Java code; the only main resource changes are the type entry and its fixture.
- **SC-008**: The gender collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-009**: An integrator using the published documentation and example requests retrieves the gender list and a single gender at the first attempt, without reading source code.

## Assumptions

- **Source values are public categories**: The four names ("Male", "Female", "Other", "Prefer not to say") are generic category labels, not data about any person, so reusing them is consistent with Constitution Principle IV. Every other value is synthetic. No real office holder's gender is involved.
- **Same generation rules as 002**: The dates, timestamps and ended-record rule are 002's, applied unchanged, so all default datasets follow one documented pattern. For four records the rule ends none; 002's FR-023 is read across all shipped default data, where the other types supply ended records (Clarifications; EC-004), and the multiples of 10 still give Gender id gaps.
- **Synthetic ids, not the extract's**: As for the existing types, ids are 10 × position. The People feature must therefore use these ids for `gender_id` (DF-3), not the real E-Links ids.
- **The guard's fragment is a single word**: The type-specific-code guard matches `gender` in every spelling in main Java code, with no exemptions (Clarifications). No main source mentions it today; the People feature, whose responses carry `gender` and `gender_id`, will add a reviewed exception for those fields (DF-3).
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged. With 4 records the collection is the smallest served.
- **Fixture location and source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002, 003 and 004 delivered: the generic routes, central type registry with alias resolution, per-type declarations and fixture validation at start-up, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
