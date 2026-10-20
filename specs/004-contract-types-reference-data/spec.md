# Feature Specification: Reference Data API — Contract Types

**Feature Branch**: `004-contract-types-reference-data`

**Created**: 2026-10-20

**Status**: Implemented

**Input**: User description: "Add the contract_types reference-data type to the existing generic reference-data API (feature 002), served by both routes — `GET /api/v1/reference_data/contract_types` and `GET /api/v1/reference_data/contract_types/{reference_id}`. Reuse the generic mechanism: a type entry in configuration and a checked-in fixture, with no type-specific code (NoTypeSpecificCodeTest must cover contract types in every spelling). Names stay unique (the default), and the type declares no reference fields. Records use the shared fields (id, name, created_at, updated_at, start_date, end_date) plus the extract's `salaried` flag, served as a JSON boolean (`true` where the extract has 1, `false` where it is blank). Add it through a generic per-type declaration of extra fields, like the existing `reference-fields` declaration, validated at start-up and written in responses only for types that declare it, so existing types' responses are unchanged. Record it as a contract deviation, because the Swagger's ReferenceDataResponse has no such field. Generate the fixture once from the name and salaried columns of the local extract (REF_ContractType.csv: 14 rows, 14 distinct ASCII names) and ignore every other column. Note that the extract's Salaried row has a blank id, and that names contain `%`. Sort names in code-point order (so "SPTW-…" sorts before "Salaried"). Use ids of 10 × position and dates by 002's rule. Say in the spec that the extract's own end-dated rows ("Unknown", "SPTW-75%") are not reproduced and that 002's position-based end dates apply instead. Document known reference points: first, last, an end-dated record, a salaried and a non-salaried record, an unknown id inside the range and one outside it. The future People/appointments feature will carry `contract_type_id`: record as a Deferred Item that its appointments must reference these synthetic ids and test that every reference resolves. Every E-Links attribute name that is still unsupported must still be rejected with `400`. Deliver unit, integration, functional and smoke coverage, generated OpenAPI documentation and Postman artifacts (collection and environment, with test scripts), with acceptance scenarios mirroring the existing reference-data features."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Feature `003-base-locations-reference-data` added a second type through configuration and a fixture alone, and made two generic extensions: a per-type opt-out from unique names, and per-type reference fields.

This feature adds ContractType, the third of the eleven E-Links reference-data types. A contract type says on what basis a judicial office holder holds an appointment: salaried, fee-paid, voluntary, or a salaried part-time working (SPTW) percentage. Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to ContractType unchanged, and is not restated here except where this feature tests it. Two differences are deliberate: the mock doesn't serve ContractType's deprecated alias `contract_type` (Clarifications; Constitution Principle VII), and ContractType records carry a `salaried` flag, through a new generic per-type declaration of boolean fields (FR-005, FR-017; deviation D-3).

## Clarifications

### Session 2026-10-20

- Q: Should the mock serve the deprecated alias `contract_type`? → A: No. Only the canonical name `contract_types` is served; `contract_type` is rejected with `400` like any unsupported attribute name, as `base_location` is. Constitution Principles I and VII make serving a deprecated alias optional per type and require the spec to state the choice, so this is not a contract deviation. AppointmentTitle's alias `appointment_title` is unaffected.

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `contract_types` among the 11 canonical `attribute_name` values and `contract_type` among the deprecated values. It defines one generic `ReferenceDataResponse` shape for every type, with no type-specific fields. Its People section shows that each appointment (`AppointmentsResponse`) carries `contract_type` and `contract_type_id`, both required. The reference extract shows that the real contract-type data also carries a `salaried` flag.

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `contract_types` | Same |
| Deprecated alias | `contract_type` | Not served: rejected with `400` like any unsupported name (Clarifications; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (deviation D-1 from 002, extended to this type) |
| Type-specific fields | None documented in the Swagger; the extract shows `salaried` | `salaried` (boolean) on every ContractType record, after `name` (deviation D-3) |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | ContractType records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and appointments carry the contract-type name (`AppointmentsResponse.contract_type`), so a list of contract types without names has no practical use. This feature applies the existing, agreed deviation to the new type. | Permanent, scope-justified (unchanged from 002) |
| D-3 | ContractType records include `salaried`, which the Swagger `ReferenceDataResponse` schema does not list | The reference extract shows the real data carries it, and it is how a consumer tells salaried contracts (including every SPTW percentage) from fee-paid and voluntary ones without hard-coding names. The field is optional in the generated schema and appears only on records of a type that declares it (FR-017), so AppointmentTitle and BaseLocation responses are unchanged. | Permanent, scope-justified |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-2 | Appointments will refer to contract types through `contract_type_id` and `contract_type` (Swagger `AppointmentsResponse`) | The People/appointments feature doesn't exist yet. ContractType is served first, so this is a backward reference: the target type already exists, and Constitution Principle IV requires such references to resolve once both types are served. | The People/appointments feature MUST set every appointment's `contract_type_id` to the `id` of a served ContractType record (the synthetic ids of this feature, not the extract's ids) and its `contract_type` to that record's `name`, and MUST add a test that every appointment's contract-type reference resolves; that feature is incomplete until the test passes. |

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of 002's and 003's IDs.*

### User Story 1 - Integrator Retrieves All Contract Types (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of contract types so that my system can load and resolve contract-type reference data exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving contract types can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/contract_types` and confirm a `200 OK` JSON response whose `results` array holds all 14 contract-type records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/contract_types`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every ContractType record in the configured dataset (14 by default), each in the reference-data record shape plus `salaried`, in ascending `id` order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** the records at the reference points equal the Reference Points table (values for `id`, `name`, `salaried`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection or a single record is retrieved, **Then** every ContractType record carries `salaried` after `name` as a JSON boolean, never null or missing: `true` on the 11 records "Salaried" and "SPTW-10%" to "SPTW-90%", and `false` on the 3 records "Fee-paid", "Unknown" and "Voluntary".
4. **AC-004**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.
5. **AC-005**: **Given** the default dataset, **When** the collection is retrieved, **Then** names containing `%` and `-` (e.g. "SPTW-75%", "Fee-paid") are returned exactly as in the source, with no escaping or rewriting beyond standard JSON encoding.

---

### User Story 2 - Integrator Retrieves a Single Contract Type by ID (Priority: P1)

As an integrator, I want to fetch one contract type by its reference identifier so that my system can resolve an identifier it holds (for example an appointment's `contract_type_id`) to its full reference record.

**Why this priority**: Part of the same contract as the collection route, and it exercises the identifier validation paths.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/contract_types/{id}` for a reference-point id and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/contract_types/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, `salaried` included.
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no contract type (`15`, which falls in an id gap; `150`, just past the last id; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `10`, which is also an AppointmentTitle and a BaseLocation id), **When** it is requested under `contract_types`, **Then** the result depends only on the ContractType dataset: identifiers are scoped to their type and never resolve across types.

---

### User Story 3 - The Deprecated Alias Is Not Served (Priority: P2)

As an integrator, I want the deprecated singular name `contract_type` rejected clearly, so that my system moves to the canonical name `contract_types` instead of relying on an alias the mock doesn't serve.

**Why this priority**: The alias is deprecated in the E-Links contract itself, and this feature chooses not to serve it (Clarifications). The choice must be observable and tested, so a request via the alias never returns data.

**Independent Test**: Call `contract_type` and `contract_type/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-010**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/contract_type`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-011**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/contract_type/{reference_id}` for any `reference_id` (one that exists under `contract_types` such as `70`, an unknown one such as `15`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-012**: **Given** a request to a `contract_type` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name, as for any unsupported name.

---

### User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want contract-type requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each contract-type route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-013**: **Given** a request with no credentials, **When** it is sent to any contract-type route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-014**: **Given** a request with present but invalid credentials, **When** it is sent to any contract-type route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using appointment titles or base locations, I want adding contract types to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: Adding a type through configuration must not change any other type's behaviour; this is the regression boundary of the feature.

**Independent Test**: Re-run 002's and 003's acceptance tests unchanged, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-015**: **Given** the delivered feature, **When** 002's AppointmentTitle and 003's BaseLocation acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature; in particular no AppointmentTitle or BaseLocation record carries `salaried` (shared test helpers may be moved without changing what is asserted).
2. **AC-016**: **Given** valid authentication, **When** each of the 18 E-Links attribute names that are still unsupported (the eight canonical types not yet served, e.g. `genders`; their eight deprecated aliases, e.g. `gender`; and the unserved aliases `base_location` and `contract_type`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-017**: **Given** valid authentication, **When** the caller sends a contract-type request with any query parameter (e.g. `?salaried=true` or `?name=Fee-paid`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 6 - Maintainer Adds the Type Without Type-Specific Code (Priority: P2)

As a maintainer, I want contract types added by declaring the type and supplying its dataset, with the `salaried` flag carried by a generic per-type declaration, so that no code is specific to the type and later types can declare boolean fields the same way.

**Why this priority**: Keeps 002's extension design intact (its AC-019 and SC-007) and keeps the cost of each later type low.

**Independent Test**: Review the change, run the automated guard that forbids type-specific code, and start the service with deliberately invalid fixtures and declarations.

**Acceptance Scenarios**:

1. **AC-018**: **Given** the delivered change, **When** it is reviewed, **Then** it adds no route, handler, authentication or error-handling logic, and its only change to main code is the type-agnostic boolean-field declaration and its validation (FR-017, FR-018); every other addition is the type entry, its dataset, tests, documentation and example requests.
2. **AC-019**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes, and it now also recognises the contract-type name in every spelling (snake_case, spaced words, camelCase, upper case), so no main source file names a specific reference-data type.
3. **AC-020**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `contract_types` as a supported value alongside the existing ones, does not list `contract_type`, and `ReferenceDataResponse` lists `salaried` as an optional boolean property, without that documentation having been edited by hand.
4. **AC-021**: **Given** a type that declares the boolean field `salaried`, **When** the service starts with a fixture in which a record lacks it, has it as `null`, as a number (`0`/`1`) or as a string (`"true"`), or **given** a type that doesn't declare it, **when** one of its records carries it, or **given** a type entry that declares an unknown, repeated or empty boolean-field name, **Then** start-up fails with a clear error naming the type, as for every other invalid fixture or declaration.

---

### User Story 7 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for contract types so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every contract-type request's checks pass.

**Acceptance Scenarios**:

1. **AC-022**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the contract-type collection, a salaried and a non-salaried single record by reference-point id, an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved alias `contract_type` returning `400`, each with checks for status, response structure and key field values (`salaried` included), and no hard-coded environment values.

---

### Edge Cases

- **EC-001 — Blank id in the extract**: The extract's "Salaried" row has a blank `id`, and the extract's ids are irregular (1–3, 5–9, 11–14, 100). Neither matters: the extract's ids are never used, and every record's `id` is assigned by position (FR-009). The "Salaried" row is served like any other.
- **EC-002 — Punctuation in names**: Ten names contain `%` and eleven contain `-` (e.g. "SPTW-75%", "Fee-paid"). They are kept verbatim and returned as ordinary JSON strings. Names never appear in a request path, and query parameters are rejected (AC-017), so `%` never needs URL encoding.
- **EC-003 — Placeholder-like name**: "Unknown" is a real value of the E-Links data and is served like any other name, at position 13 (id 130), with `salaried` `false`.
- **EC-004 — The extract's own end dates are not reproduced**: In the extract, "Unknown" and "SPTW-75%" are the ended contract types (both with start and end date 2023-03-29). The mock doesn't copy them: 002's position-based rule applies instead (FR-009), so the ended records are "SPTW-60%" (position 7) and "Voluntary" (position 14), and "Unknown" and "SPTW-75%" are open. Consumers must not rely on which contract types are ended in the mock.
- **EC-005 — Identifier gaps**: Ids are multiples of 10, so a well-formed id between two of them (e.g. `15`) or beyond the last one (e.g. `150`) returns `404`.
- **EC-006 — Same id, different type**: `GET /api/v1/reference_data/contract_types/10` returns "Fee-paid", while id `10` under `appointment_titles` and `base_locations` returns "Acting Senior Coroner" and "Aberconwy"; identifiers are scoped to their type.
- **EC-007 — Case and spelling variants**: `Contract_Types`, `CONTRACT_TYPES` and `contract-types` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-008 — Code-point order**: Upper-case letters sort before lower-case ones, so the ten "SPTW-…" names (positions 2–11) sort before "Salaried" (position 12), and "SPTW-70%" sorts before "SPTW-75%". The order is fixed by the generation rule, not by any business meaning of the names.
- **EC-009 — Inherited edge cases**: Covered by FR-004. For contract types, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the ContractType reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `contract_types`. The deprecated alias `contract_type` MUST NOT be served (Clarifications; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations` and `contract_types`. This supersedes 003's FR-002; every other E-Links attribute name, including `contract_type`, MUST remain unsupported (`400`).
- **FR-003**: ContractType MUST be added through the generic reference-data capability alone: a type entry and its dataset. The change MUST NOT add or modify route, authentication or error-handling logic; its only changes to the record shape and validation are FR-017's and FR-018's, which are type-agnostic. It MUST NOT introduce code that names a specific reference-data type.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to ContractType without exception. Names MUST be unique within the type (the default; ContractType does not opt out), and the type declares no reference fields.

**Response content**

- **FR-005**: Each ContractType record MUST contain exactly these fields, in this order: `id`, `name`, `salaried`, `created_at`, `updated_at`, `start_date` and `end_date`. The shared fields are in 002's formats (002 FR-011); `salaried` is a JSON boolean, never null (deviation D-3). AppointmentTitle and BaseLocation records MUST stay exactly as 002 and 003 define them, without `salaried`.

**Data**

- **FR-006**: The ContractType dataset MUST be synthetic, deterministic and supplied as a fixed, checked-in fixture. Only the public `name` values and the `salaried` flag may be taken from the supplied ContractType reference extract; every other value (`id`, dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over.
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 14 rows in the extract, which have 14 distinct names. Names MUST be free of leading or trailing whitespace and carried over verbatim. `salaried` MUST be `true` where the extract's `salaried` column is `1` and `false` where it is blank; any other value in that column MUST stop generation rather than be guessed.
- **FR-009**: Records MUST be generated following 002's fixture generation rules: names sorted in code-point order and numbered from position 1 to 14; `id` = 10 × position (10 to 140); records whose position is a multiple of 7 (2 records) have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; all other records (12) have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own ids, dates and timestamps MUST NOT be used (EC-001, EC-004).
- **FR-010**: ContractType ids MUST be stable across releases of the default dataset, so that later features (the People/appointments feature, DF-2) and consumer test suites can refer to them.

**Documentation and example requests**

- **FR-011**: The generated API documentation MUST list `contract_types` among the supported `attribute_name` values and MUST NOT list `contract_type`, and MUST list `salaried` as an optional boolean property of `ReferenceDataResponse`, all derived from the configuration and the record model rather than edited by hand.
- **FR-012**: The example request collection MUST cover the contract-type routes as AC-022 describes, using environment variables for every environment-specific value.
- **FR-013**: The project's documentation of supported reference-data types MUST list contract types, and the instructions for adding a type MUST describe the boolean-field declaration alongside `unique-names` and `reference-fields`.

**Quality**

- **FR-014**: The contract-type collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-015**: Unit, integration, functional and smoke coverage MUST be extended to the new type, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass.
- **FR-016**: The automated guard against type-specific code MUST enforce FR-003's ban for this type: it MUST recognise the contract-type name in every spelling, as it does for the existing types.

**Generic boolean fields**

- **FR-017**: A type's entry MUST be able to declare which boolean fields its records carry, from a fixed set of known boolean fields; this feature defines one, `salaried`. A type declares none by default. Responses MUST write a boolean field only for records that carry it. The mechanism MUST be type-agnostic (no type named in code); ContractType declares `salaried`, and AppointmentTitle and BaseLocation declare nothing. The generated API documentation MUST list each known boolean field as an optional boolean property of `ReferenceDataResponse`.
- **FR-018**: At start-up, every record of a type MUST carry each boolean field the type declares as a JSON boolean (`true` or `false`; not `null`, a number or a string) and MUST NOT carry a known boolean field the type doesn't declare. An unknown, repeated or empty boolean-field name in a type entry MUST stop start-up with the same kind of error as every other invalid declaration (003 FR-019). The checks MUST be type-agnostic.

### Reference Points

These records of the default dataset are fixed by FR-008 and FR-009 and are used by tests and example requests:

| Position | `id` | `name` | `salaried` | `end_date` | `updated_at` |
|----------|------|--------|------------|------------|--------------|
| 1 (first, not salaried) | 10 | Fee-paid | false | null | 2024-06-03T10:30:00Z |
| 2 (first salaried) | 20 | SPTW-10% | true | null | 2024-06-03T10:30:00Z |
| 7 (first ended) | 70 | SPTW-60% | true | 2025-03-31 | 2025-04-01T08:00:00Z |
| 12 | 120 | Salaried | true | null | 2024-06-03T10:30:00Z |
| 13 | 130 | Unknown | false | null | 2024-06-03T10:30:00Z |
| 14 (last, ended) | 140 | Voluntary | false | 2025-03-31 | 2025-04-01T08:00:00Z |

Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. `15` is a guaranteed unknown id inside the range, and `150` and `999999` are outside it.

The full default order is: 1 Fee-paid, 2 SPTW-10%, 3 SPTW-20%, 4 SPTW-30%, 5 SPTW-40%, 6 SPTW-50%, 7 SPTW-60%, 8 SPTW-70%, 9 SPTW-75%, 10 SPTW-80%, 11 SPTW-90%, 12 Salaried, 13 Unknown, 14 Voluntary.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/contract_types` | `200`, 14 records |
| Unserved alias | `GET /api/v1/reference_data/contract_type` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/contract_types/120` | `200`, Salaried, `salaried` `true` |
| Unknown id (gap) | `GET /api/v1/reference_data/contract_types/15` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/contract_types/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/contract_types?salaried=true` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/genders` | `400`, shared error shape |
| Missing or invalid credentials | any contract-type route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002. This feature adds a third instance, ContractType, with canonical name `contract_types` and no served alias, and lets a type declare boolean fields.
- **ContractType record**: One basis on which a judicial office holder holds an appointment. Attributes: `id` (unique, stable integer), `name` (unique public name, deviation D-1), `salaried` (whether the contract is salaried, including salaried part-time working; deviation D-3), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. Its identifiers are independent of other types' identifiers. Appointments will refer to it by `id` and `name` (DF-2).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 22 acceptance scenarios (AC-001 to AC-022) pass as automated tests or, for AC-018 and AC-022, as review and collection-run checks.
- **SC-002**: The default contract-type collection contains exactly 14 records with 14 distinct names, 11 with `salaried` `true` and 3 with `false`, and the six reference points match the Reference Points table.
- **SC-003**: 0 requests via the unserved alias `contract_type` return reference data; 100% with valid credentials return `400`.
- **SC-004**: 0 of the 18 still-unsupported attribute names return a `200` response.
- **SC-005**: 0 contract-type requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-006**: 100% of 002's and 003's acceptance tests pass with their assertions and expected responses unchanged; 0 AppointmentTitle or BaseLocation records carry `salaried`.
- **SC-007**: Adding the type changes 0 lines of route, authentication or error-handling logic; the only main Java code change is FR-017's and FR-018's generic boolean fields, naming no type, and the only main resource changes are the type entry and its fixture.
- **SC-008**: The contract-type collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-009**: An integrator using the published documentation and example requests retrieves the contract-type list and a single contract type, and tells salaried from non-salaried contract types, at the first attempt, without reading source code.

## Assumptions

- **Source values are public**: Contract-type names ("Salaried", "Fee-paid", "Voluntary", "Unknown" and the SPTW percentages) are public employment categories, and `salaried` is a yes/no flag about the category. Neither is personal data, so reusing them is consistent with Constitution Principle IV. Every other value is synthetic.
- **Same generation rules as 002**: The dates, timestamps and ended-record rule are 002's, applied unchanged, so all default datasets follow one documented pattern. The two ended records give the dataset both null and non-null end dates, and the multiples of 10 give it id gaps, as 002's FR-023 requires of a dataset. Reproducing the extract's real end dates was considered and rejected for consistency (EC-004).
- **Synthetic ids, not the extract's**: As for the existing types, ids are 10 × position. The People/appointments feature must therefore use these ids for `contract_type_id` (DF-2), not the real E-Links ids.
- **`salaried` as a boolean**: The extract encodes the flag as `1` or blank. A JSON boolean is the clearest typed form for consumers. The real API's encoding of the flag isn't documented in the Swagger, so this is part of deviation D-3.
- **Boolean fields as a new generic declaration**: `salaried` is not an id of another type, so it doesn't fit 003's reference fields. A separate, closed set of declarable boolean fields keeps typed models (Principle VIII) and keeps the change type-agnostic (Principle VII).
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged. With 14 records the collection is the smallest served.
- **Fixture location and source**: The extract used for generation is the local, gitignored `joh-elinks-api/ReferenceData/REF_ContractType.csv`. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 and 003 delivered: the generic routes, central type registry with alias resolution, per-type declarations and fixture validation at start-up, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
