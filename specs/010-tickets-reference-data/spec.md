# Feature Specification: Reference Data API — Tickets

**Feature Branch**: `010-tickets-reference-data`

**Created**: 2026-10-29

**Status**: Implemented

**Input**: User description: "Add the `tickets` reference-data type to the generic reference-data API (feature 002), through configuration, a checked-in fixture and one generic capability: a fourth reference field, `ticket_category_id`. Deprecated versions are not to be considered. Contract (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `tickets` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` (`ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` requires `id`, `updated_at`, `created_at`, `start_date` and `end_date`. The People API's `AuthorisationsResponse` carries `ticket` and `ticket_id`, and its examples use E-Links ticket ids 294 "Civil Authorisation", 365 "03 - Disability Living Allowance" and 367 "05 - Industrial Injuries". Scope: serve `tickets` on `GET /api/v1/reference_data/tickets` and `/{reference_id}`. Authentication, validation, error shape, ordering and edge cases are as in 002. Record shape: the Swagger fields, plus `name` (deviation D-1) and `ticket_category_id` (new deviation D-5: the extract carries it, the Swagger schema does not list it), written after `name`. Type configuration: `reference-fields: [ticket_category_id]`, not nullable (every row has one); no boolean, text or nullable reference fields; `unique-names: false`, since 7 names repeat. The singular `ticket` is not served and returns `400`, like every other unsupported E-Links name. Generic capability (main-code change, test-first, as 003 did for its reference fields): `ticket_category_id` becomes a fourth value of the `ReferenceField` enum alongside `type_id`, `parent_id` and `jurisdiction_id`, carried by `ReferenceDataRecord` and `ReferenceDataResponse` as a `ReferenceFieldValue`, declared and validated like the others, and written only for types that declare it. The other eight types' responses must stay byte-for-byte unchanged (existing golden files), and the OpenAPI `ReferenceDataResponse` schema gains `ticket_category_id` as an optional integer citing D-5. Ids (DR, as in 007's DR-1, 008 and 009): records keep the extract's ids, 289 to 1709 with gaps (289–412, then 1412–1709), not 002's 10 × position, so that People authorisations' `ticket_id` values (e.g. 294, 365, 367 in the Swagger examples) can resolve later. Records are ordered by ascending id, and 002's date rule is applied by position in that order: positions 7, 14, …, 154 are ended (22 records: ids 295, 303, 310, 317, 324, 331, 338, 345, 352, 359, 367, 375, 382, 394, 402, 409, 1416, 1455, 1493, 1562, 1600, 1607), with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; the rest have `end_date` null and `updated_at` 2024-06-03T10:30:00Z; all have `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates (16 ended rows, various start/created/updated dates) are not reproduced. Fixture: generated once from the local extract REF_Ticket.csv: a header plus 159 rows of 7 columns (`id`, `name`, `ticket_category_id`, `start_date`, `end_date`, `created_at`, `updated_at`), quoted strings, ASCII, no BOM, no trailing newline. Ids are unique and ascending; names are non-blank, trimmed and copied exactly (including `&`, `'`, `.` and parentheses, e.g. "s.52 Deputies", "Civil Authorisation (High Court Judge Chancery Division)"). Take `id`, `name` and `ticket_category_id`; ignore every other column. 159 records, 152 distinct names. The repeated names are each used in two different ticket categories: Claims Management Services (323, 1561), Community Rights to Bid (324, 1493), Consumer Credit (326, 1560), Financial Remedy (409, 410), National Security (346, 1527), Race Panel (351, 1526), Reserve Forces (353, 1491). References: - `ticket_category_id` (41 distinct values, 31 to 88) refers to `ticket_categories`, which is not served yet: a forward reference (Constitution Principle IV), recorded as deferred item DF-8. The feature that serves `ticket_categories` MUST keep the extract's ids and MUST add the `tickets.ticket_category_id -> ticket_categories` row to ReferenceDataReferencesTest. `TARGET_TYPES` gains `ticket_category_id -> ticket_categories`, so the completeness check demands that row as soon as the target is served. The generating script MUST confirm, once, that every `ticket_category_id` is an id in the local REF_TicketCategory.csv (54 categories); all 41 do today. - DF-9: the People feature must point authorisations' `ticket_id` at these records and test that every reference resolves. - No existing type refers to tickets, and the jurisdiction-agreement table is unchanged. Build checks without the extract: ReferenceDataFixtureRulesTest covers the fixture with the extract-id rule (`SOURCE`), the declared reference field and reference points. NoTypeSpecificCodeTest: once main code names the generic field `ticket_category_id` (ReferenceField, ReferenceDataRecord, ReferenceDataResponse), a `ticket` fragment would fail the build, as `jurisdiction` would have for `jurisdiction_id`. Decide in clarify whether to (a) use the plural fragment `tickets`, as 009 did, with cases showing it matches the plural spellings but not `ticket_category_id`, or (b) another approach. Note that the later `ticket_categories` feature will meet the same collision with `ticket_category_id`. Remove `tickets` from the unsupported-name examples and move them to another still-unsupported name, `ticket_categories`: ReferenceDataFunctionalTest's unsupported list and its missing-token path, the Postman "tickets - unsupported" request, and 002's quickstart row 5. `ticket` stays in the list. Update README and CLAUDE.md (nine types; the ticket paragraph; the forward reference; the new reference field). Reference points: 289 "Welsh Language" (first; category 88), 294 "Civil Authorisation" (Swagger example; category 31), 295 "Commercial" (first ended), 365 "03 - Disability Living Allowance" (Swagger example; category 36), 367 "05 - Industrial Injuries" (Swagger example; ended by the synthetic rule), 409 and 410 "Financial Remedy" (repeated name; categories 31 and 33), 1709 "Valuation Tribunal Wales" (last; category 83). Unknown ids: 1 (served by other types), 288 (just before the first), 297 (a gap), 1710 (just past the last) and 999999. Deliver unit, integration, functional and smoke coverage; generated OpenAPI documentation (the `attribute_name` enum gains `tickets`; `ReferenceDataResponse` gains `ticket_category_id`); and Postman artifacts (collection and environment, with test scripts). Acceptance scenarios mirror the existing reference-data features."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003` to `009` added BaseLocation, ContractType, Gender, JudiciaryRole, Location, LocationType and Jurisdiction through configuration and fixtures. Along the way, types gained declared reference fields (`type_id`, `parent_id`, `jurisdiction_id`), boolean and text fields, nullable references and repeated names. 007 to 009 kept the extract's own ids for types that other data refers to (DR-1 in each).

This feature adds Ticket, the ninth of the eleven E-Links reference-data types: the 159 tickets (authorisations to sit in a particular kind of work, such as "Civil Authorisation" or "03 - Disability Living Allowance") that judicial office holders hold. Each ticket belongs to one ticket category. That shapes the feature:

- **One generic change** (FR-003, FR-019). Every ticket carries the id of its ticket category, `ticket_category_id`. The mock's reference fields are a fixed, known set, so serving this one needs a fourth reference field that any type can declare, as 003 introduced the first three. No type-specific code is added.
- **A forward reference** (FR-011; DF-8). `ticket_category_id` refers to `ticket_categories`, which is not served yet. Constitution Principle IV allows this only if the reference is recorded as a deferred item, its values are copied unchanged from the extract, and the feature that serves `ticket_categories` makes every reference resolve.
- **Ids are the extract's own** (FR-009; DR-1). The People API's authorisations carry a `ticket_id`, and its Swagger examples use E-Links ticket ids (294, 365, 367). Keeping those ids lets the People feature refer to these records later (DF-9). Every other value stays synthetic.

Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to Ticket unchanged and is not restated here except where this feature tests it. The mock doesn't serve the deprecated alias `ticket` (FR-001; Constitution Principle VII).

## Clarifications

### Session 2026-10-29

- Q: The new generic field `ticket_category_id` puts the singular `ticket` in main code, so a `ticket` fragment in the type-specific-code guard would fail. How should the guard cover this type? → A: With the plural fragment `tickets` in every spelling and no exemption, as 009 did for `jurisdictions`. It matches nothing in main code, including `ticket_category_id`; the singular is left uncovered because the generic field name contains it (FR-018).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `tickets` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. Its People section shows authorisations carrying `ticket` (a ticket name) and `ticket_id` (an E-Links ticket id). The local extract shows that real ticket data carries an id, a name, a ticket category id and the shared dates. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `tickets` | Same |
| Deprecated alias | Out of scope | Not served: `ticket` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (D-1) and `ticket_category_id` (D-5) |
| Ids | E-Links' own ticket ids | Same as the extract (289 to 1709, with gaps); see DR-1 |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | Ticket records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and the People API refers to tickets by name as well as id. | Permanent, scope-justified (unchanged from 002) |
| D-5 | Ticket records include `ticket_category_id`, which the Swagger `ReferenceDataResponse` schema does not list | The extract shows the real ticket data carries the id of each ticket's category, which also tells apart tickets with the same name (7 names appear twice, each time in a different category). The field is optional in the generated schema and appears only on records of a type that declares it (FR-019), so the other types' responses are unchanged. | Permanent, scope-justified |

### Data-Rule Deviation

| # | Deviation | Reason |
|---|-----------|--------|
| DR-1 | Ticket ids are the extract's own ids, not 002's synthetic `10 × position`, and records are numbered for the date rule by position in ascending-id order, not code-point name order | People authorisations' `ticket_id` values are E-Links ticket ids (the Swagger examples use 294, 365 and 367), so they can only resolve if Ticket keeps those ids (DF-9; Constitution Principle IV). This is not a contract deviation (the contract defines no id values); it applies to this type only, as 007's to 009's DR-1 do to their types, and every other generated value follows 002's rules. |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-8 | Ticket's `ticket_category_id` (41 distinct values, 31 to 88) refers to `ticket_categories`: a forward reference (Constitution Principle IV) | `ticket_categories` is not served yet. In the local extracts every value is an id in the ticket-category extract, so the references will resolve once that type is served with the extract's ids. | The feature that serves `ticket_categories` MUST serve it with the extract's ids and MUST add a test that every Ticket `ticket_category_id` resolves to a served record; it is incomplete until that test passes. |
| DF-9 | People authorisations carry a `ticket_id` (and a `ticket` name) | The People feature is not built yet. | The People feature MUST point each authorisation's `ticket_id` at a served Ticket record, keep its `ticket` equal to that record's name, and test that every reference resolves. |

DF-2 to DF-4 and DF-6 belong to the People feature and are unaffected. 003 DF-1, 007 DF-5 and 008 DF-7 are closed (009).

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Tickets (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of tickets so that my system can load them exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving tickets can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/tickets` and confirm a `200 OK` JSON response whose `results` array holds all 159 ticket records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/tickets`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every Ticket record in the configured dataset (159 by default), in ascending `id` order, each with exactly the fields `id`, `name`, `ticket_category_id`, `created_at`, `updated_at`, `start_date` and `end_date`, in that order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** every reference point in the Reference Points table has the listed `name`, `ticket_category_id`, `end_date` and `updated_at`.
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** exactly the 22 records at positions 7, 14, …, 154 are ended (ids 295, 303, 310, 317, 324, 331, 338, 345, 352, 359, 367, 375, 382, 394, 402, 409, 1416, 1455, 1493, 1562, 1600 and 1607), each with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; the other 137 have a null `end_date` and `updated_at` 2024-06-03T10:30:00Z.
4. **AC-004**: **Given** the default dataset, **When** the collection is retrieved, **Then** it holds 152 distinct names, and each of the 7 repeated names appears exactly twice as two separate records with different ids and different `ticket_category_id` values (EC-002).
5. **AC-005**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Ticket by ID (Priority: P1)

As an integrator, I want to fetch one ticket by its reference identifier so that my system can resolve an id it holds — such as an authorisation's `ticket_id` — to its full record.

**Why this priority**: Part of the same contract as the collection route, and the way a consumer looks up the ticket of an authorisation it holds.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/tickets/{id}` for a reference point and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/tickets/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including the Swagger example ids `294`, `365` and `367`.
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no ticket (`1`, below the first id; `288`, just before the first; `297`, in a gap; `1710`, just past the last; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `294`, which is the location "Llanelli" under `locations`, or `290`, which is "Commissioner" under `appointment_titles`), **When** it is requested under `tickets`, **Then** the result depends only on the Ticket dataset (`294` is "Civil Authorisation", `290` is "Administrative Court"): identifiers are scoped to their type and never resolve across types (EC-006).

---

### User Story 3 - Integrator Sees Each Ticket's Category (Priority: P1)

As an integrator, I want every ticket to carry the id of its ticket category, exactly as E-Links does, so that my system can group tickets by category and tell apart tickets that share a name.

**Why this priority**: The category is the only field besides the name that distinguishes tickets, and 7 names are shared by two tickets each. Constitution Principle IV allows the field to refer to a type the mock doesn't serve yet only under the forward-reference rules, which must be observable and tested.

**Independent Test**: Load the Ticket collection and check that every record carries a `ticket_category_id` from the extract, and that the build's reference checks know which type the field refers to.

**Acceptance Scenarios**:

1. **AC-010**: **Given** the default dataset, **When** the collection is retrieved, **Then** every one of the 159 records carries a non-null, positive integer `ticket_category_id`, the records together use exactly 41 distinct values between 31 and 88, and each reference point's value equals the Reference Points table.
2. **AC-011**: **Given** the delivered feature, **When** the check that every reference between served types is tested runs, **Then** it passes and knows that `ticket_category_id` refers to `ticket_categories`; because `ticket_categories` is not served, it requires no resolution test for the field yet, and it will require one as soon as `ticket_categories` is configured (DF-8).
3. **AC-012**: **Given** the default datasets, **When** any other served type's records are retrieved, **Then** none carries `ticket_category_id`, and no served type refers to tickets.

---

### User Story 4 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `ticket` rejected clearly, so that my system uses the canonical name `tickets` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested.

**Independent Test**: Call `ticket` and `ticket/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-013**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-014**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket/{reference_id}` for any `reference_id` (one that exists under `tickets` such as `294`, an unknown one such as `297`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-015**: **Given** a request to a `ticket` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name.

---

### User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want ticket requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each ticket route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-016**: **Given** a request with no credentials, **When** it is sent to any ticket route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-017**: **Given** a request with present but invalid credentials, **When** it is sent to any ticket route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using the eight served types, I want adding tickets and the new reference field to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: Every feature must hold the regression boundary, and this one changes shared main code.

**Independent Test**: Re-run 002's to 009's acceptance tests, compare the existing golden responses byte for byte, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-018**: **Given** the delivered feature, **When** 002's to 009's acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature; every existing golden response is byte-for-byte identical. Shared test lists of unsupported names may lose `tickets`, and examples that used `tickets` as an unsupported type, such as the missing-token check and the example request collection's "unsupported" request, move to `ticket_categories`; nothing else asserted changes.
2. **AC-019**: **Given** valid authentication, **When** each of the 12 E-Links attribute names that are still unsupported (the two canonical types not yet served, `ticket_categories` and `ticket_category_types`; their two singular forms; and the unserved singular names `base_location`, `contract_type`, `gender`, `judiciary_role`, `jurisdiction`, `location`, `location_type` and `ticket`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-020**: **Given** valid authentication, **When** the caller sends a ticket request with any query parameter (e.g. `?ticket_category_id=31`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 7 - Maintainer Adds the Type With One Generic Change (Priority: P2)

As a maintainer, I want tickets added by declaring the type and supplying its dataset, plus one generic reference field that any type can declare, so that 002's extension design keeps working without type-specific code.

**Why this priority**: Keeps the remaining types cheap to add and the codebase free of special cases.

**Independent Test**: Review the change, run the start-up validation tests for the new reference field, and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-021**: **Given** the delivered change, **When** it is reviewed, **Then** its only main-code change is the generic `ticket_category_id` reference field (FR-019); everything else is the type entry, its dataset, tests, documentation and example requests.
2. **AC-022**: **Given** a type that declares `ticket_category_id`, **When** a record leaves it out or holds anything other than a positive integer, **Then** the service refuses to start, naming the fixture, the field and the record, exactly as for the existing reference fields; **and given** a type that doesn't declare it, a record that carries it also stops the service at start-up. The automated guard against type-specific code passes, and it now also recognises the plural type name `tickets` in every spelling, while the generic field `ticket_category_id` in its spellings is not matched (FR-018).
3. **AC-023**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `tickets` alongside the existing supported values and does not list `ticket`, and `ticket_category_id` is documented as an optional integer present only for types that declare it, without that documentation having been edited by hand; `ReferenceDataResponse` is otherwise unchanged.

---

### User Story 8 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for tickets so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every ticket request's checks pass.

**Acceptance Scenarios**:

1. **AC-024**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the ticket collection, a single record (e.g. `294`), an ended record (e.g. `367`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `ticket` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values. The request that used `tickets` as an example of an unsupported type uses `ticket_categories`.

---

### Edge Cases

- **EC-001 — Ids from the extract, with gaps**: Ids run from 289 to 1709 in two blocks (289–412 and 1412–1709) with gaps inside each (DR-1). Every well-formed id outside the dataset is unknown (`404`), including `1`, `288` (just before the first), `297` (in a gap) and `1710` (just past the last).
- **EC-002 — Repeated names**: 7 names appear twice, each time in a different ticket category: Claims Management Services (323, 1561), Community Rights to Bid (324, 1493), Consumer Credit (326, 1560), Financial Remedy (409, 410), National Security (346, 1527), Race Panel (351, 1526) and Reserve Forces (353, 1491). They are separate records, as for BaseLocation, Location and LocationType; the type opts out of the unique-name rule. By the synthetic date rule both Community Rights to Bid records are ended, and one of the two Financial Remedy records (409).
- **EC-003 — Name content**: Names are plain ASCII, 4 ("Food") to 56 characters long, free of leading or trailing whitespace, and include punctuation (`&`, `'`, `.`, `,`, `-` and parentheses, e.g. "s.52 Deputies") and numbered prefixes (9 names, e.g. "03 - Disability Living Allowance"). They are carried over exactly; order is by id, never by name.
- **EC-004 — A forward reference**: Every `ticket_category_id` refers to a type that isn't served yet. The service does not check references at start-up, and a caller who requests a category under `ticket_categories` gets `400` (unsupported type) until that type is served (DF-8).
- **EC-005 — The extract's dates are not reproduced**: The extract ends 16 records and carries a variety of start, created and updated timestamps. All are ignored; 002's position-based rule applies, so the synthetic ended set (22 records) differs from the extract's. In particular, 367 ("05 - Industrial Injuries", a Swagger example) is ended in the mock, while 323, 326, 346 and 351 are open although the extract ends them.
- **EC-006 — Same id, different type**: Ticket ids overlap ids of other types (e.g. `294` is a location, `290` an appointment title, a base location and a judiciary role). Identifiers are scoped to their type.
- **EC-007 — Declared field on other types**: No other type declares `ticket_category_id`, so no other type's records carry it and their responses are unchanged byte for byte.
- **EC-008 — Case and spelling variants**: `Tickets`, `TICKETS` and `tickets_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-009 — Inherited edge cases**: Covered by FR-004. For tickets, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the Ticket reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `tickets`. The singular name `ticket` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions` and `tickets`. This supersedes 009's FR-002; every other E-Links attribute name, including `ticket`, MUST remain unsupported (`400`).
- **FR-003**: Ticket MUST be added through the generic reference-data capability: a type entry and its dataset, plus the generic capability of FR-019. The change MUST NOT add or modify route, authentication or error-handling logic, and MUST add no type-specific logic.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to Ticket without exception. The type MUST allow repeated names (EC-002) and declares exactly one reference field, `ticket_category_id`, which is not nullable, and no boolean or text fields.

**Response content**

- **FR-005**: Each Ticket record MUST contain exactly these fields, in this order: `id`, `name`, `ticket_category_id`, `created_at`, `updated_at`, `start_date` and `end_date`, the shared fields in 002's formats (002 FR-011) and `ticket_category_id` as a JSON integer. The records of the existing types MUST stay exactly as 002 to 009 define them.

**Data**

- **FR-006**: The Ticket dataset MUST be deterministic and supplied as a fixed, checked-in fixture. Only the public `id`, `name` and `ticket_category_id` values may be taken from the supplied ticket reference extract, each copied unchanged; every other value (dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over (EC-005).
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 159 rows in the extract, which have 152 distinct names. Each name MUST be carried over exactly as the extract has it: nothing trimmed, case-folded, corrected or filtered (EC-003).
- **FR-009**: Records MUST keep the extract's ids (DR-1) and be ordered by ascending id, numbered from position 1 to 159 in that order. Records whose position is a multiple of 7 (the 22 ids in AC-003) are ended, with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; every other record has no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates and timestamps MUST NOT be used (EC-005).
- **FR-010**: Ticket ids MUST be stable across releases of the default dataset and MUST equal the extract's, so that the People feature can refer to them (DF-9).

**Referential integrity**

- **FR-011**: Every Ticket `ticket_category_id` is a forward reference to `ticket_categories`, recorded as DF-8 (Constitution Principle IV). Before the fixture is checked in, its values MUST be confirmed, once, to be ids in the local ticket-category extract (all 41 are). The build's check that every reference between served types is tested MUST know that `ticket_category_id` refers to `ticket_categories`, so that it requires a resolution test as soon as that type is served. References between served types MUST keep resolving, and the existing jurisdiction-agreement check (008 FR-012) MUST keep passing unchanged.
- **FR-012**: No served type other than Ticket may carry `ticket_category_id` (EC-007).

**Documentation and example requests**

- **FR-013**: The generated API documentation MUST list `tickets` among the supported `attribute_name` values and MUST NOT list `ticket`, derived from the configuration rather than edited by hand. It MUST document `ticket_category_id` as an optional integer property of `ReferenceDataResponse`, present only for types that declare it (D-5); the rest of `ReferenceDataResponse` MUST be unchanged.
- **FR-014**: The example request collection MUST cover the ticket routes as AC-024 describes, using environment variables for every environment-specific value.
- **FR-015**: The project's documentation of supported reference-data types MUST list tickets and the new reference field, describe the forward reference to `ticket_categories`, and update every statement that `tickets` is not served.

**Quality**

- **FR-016**: The ticket collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-017**: Unit, integration, functional and smoke coverage MUST be extended to the new type and the new capability, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009), with the extract-id order rule that 007 introduced and the declared reference field, MUST be checked by an automated test without the extract; the values are compared with the extract once, when the fixture is generated (FR-007).
- **FR-018**: The automated guard against type-specific code MUST recognise the plural type name `tickets` in every spelling (snake_case, spaced words, camelCase, upper case, e.g. `tickets`, `Tickets`, `TICKETS`, `myTickets`, `TicketsHelper`), with no exemption, and MUST keep passing once main code names the generic field `ticket_category_id` (FR-019; Clarifications). It does not cover the singular `ticket`, which the generic reference field `ticket_category_id` legitimately contains. The guard's own tests MUST show that the fragment matches those plural spellings and does not match `ticket_category_id`, `ticketCategoryId` or `TICKET_CATEGORY_ID`. Test lists that use `tickets` as an example of an unsupported name MUST drop it.
- **FR-019**: A type MUST be able to declare `ticket_category_id` as a reference field, alongside `type_id`, `parent_id` and `jurisdiction_id`, with the same rules as those fields (003 FR-018, 007's nullable reference fields): at start-up, every record of a declaring type MUST carry it as a positive integer (or `null`, only if the type also declares it nullable), a record of a type that doesn't declare it MUST NOT carry it, and a violation MUST stop the service naming the fixture, the field and the record. Responses write it after `name`, with the other reference fields, only for declaring types, so the responses of every other type MUST stay byte-for-byte the same.

### Reference Points

The default dataset holds 159 records (fixed by FR-006, FR-008 and FR-009). These records are used by tests and example requests. Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01; ended records have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z, open ones a null `end_date` and `updated_at` 2024-06-03T10:30:00Z.

| Position | `id` | `name` | `ticket_category_id` | Ended | Note |
|----------|------|--------|----------------------|-------|------|
| 1 | 289 | Welsh Language | 88 | no | first |
| 2 | 290 | Administrative Court | 31 | no | same id as an appointment title (EC-006) |
| 6 | 294 | Civil Authorisation | 31 | no | Swagger example |
| 7 | 295 | Commercial | 31 | yes | first ended |
| 75 | 365 | 03 - Disability Living Allowance | 36 | no | Swagger example |
| 77 | 367 | 05 - Industrial Injuries | 36 | yes | Swagger example; ended by the synthetic rule (EC-005) |
| 112 | 409 | Financial Remedy | 31 | yes | repeated name |
| 113 | 410 | Financial Remedy | 33 | no | repeated name |
| 159 | 1709 | Valuation Tribunal Wales | 83 | no | last |

`1`, `288`, `297`, `1710` and `999999` are guaranteed unknown ids.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/tickets` | `200`, 159 records |
| Unserved singular name | `GET /api/v1/reference_data/ticket` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/tickets/294` | `200`, Civil Authorisation |
| Ended record | `GET /api/v1/reference_data/tickets/367` | `200`, ended |
| Unknown id | `GET /api/v1/reference_data/tickets/297` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/tickets/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/tickets?ticket_category_id=31` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/ticket_categories` | `400`, shared error shape |
| Missing or invalid credentials | any ticket route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002 and extended by 003, 004, 007 and 008. This feature adds a ninth instance, Ticket, with canonical name `tickets`, no served alias, repeated names allowed and one declared reference field, `ticket_category_id`. The set of declarable reference fields gains `ticket_category_id`; the declaration format is otherwise unchanged.
- **Ticket record**: One E-Links ticket: an authorisation to sit in a particular kind of judicial work. Attributes: `id` (unique, stable, the E-Links id), `name` (public name, exactly as in the source, not necessarily unique), `ticket_category_id` (the E-Links id of its ticket category; a forward reference until `ticket_categories` is served), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. People authorisations will refer to it through `ticket_id` (DF-9).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 24 acceptance scenarios (AC-001 to AC-024) pass as automated tests or, for AC-021 and AC-024, as review and collection-run checks.
- **SC-002**: The default ticket collection contains exactly 159 records with 152 distinct names, exactly 22 of them ended, matching the Reference Points table.
- **SC-003**: 100% of the 159 served records carry the same `id`, `name` and `ticket_category_id` as the source row, character for character.
- **SC-004**: 100% of the 159 `ticket_category_id` values were confirmed, when the fixture was generated, to be ids in the ticket-category extract; 0 references between served types are left dangling.
- **SC-005**: 0 requests via the unserved singular name `ticket` return reference data; 100% with valid credentials return `400`.
- **SC-006**: 0 of the 12 still-unsupported attribute names return a `200` response.
- **SC-007**: 0 ticket requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-008**: 100% of 002's to 009's acceptance tests pass with their assertions and expected responses unchanged, and every existing golden response is byte-for-byte identical.
- **SC-009**: The only main-code change is the generic reference field (FR-019); 0 lines of type-specific code are added.
- **SC-010**: The ticket collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-011**: An integrator using the published documentation and example requests retrieves the ticket list and the ticket for a given authorisation id at the first attempt, without reading source code.

## Assumptions

- **Source values are public reference data**: Ticket names, ids and category ids describe public kinds of judicial work, not people, so reusing them is consistent with Constitution Principle IV. Dates and timestamps are synthetic.
- **The extract's ids are kept for the People feature**: No served type refers to tickets today, but the People API's authorisations do (Swagger examples 294, 365, 367). Keeping the extract's ids now avoids renumbering later, as for Location (007), LocationType (008) and Jurisdiction (009).
- **The category reference is served now, not deferred**: As for BaseLocation's references (003) and LocationType's `jurisdiction_id` (008), consumers see the real response shape now, and the forward reference is recorded (DF-8) and closed by the feature that serves `ticket_categories`.
- **A new deviation number**: `ticket_category_id` gets its own deviation, D-5, as `salaried` (D-3) and `integration_code` (D-4) did, rather than extending 003's D-2.
- **The extract is CSV**: A header plus 159 rows of 7 quoted columns, ASCII, with no byte-order mark and no trailing newline. Only `id`, `name` and `ticket_category_id` are used. The format affects only the one-off generation, not the served data.
- **Same date rules as 002**: The dates, timestamps and ended-record rule are 002's, counted over ascending-id positions. The extract's own end dates are not reproduced (EC-005), as for JudiciaryRole (006) and Location (007).
- **Deprecated names are out of scope**: The feature description excludes deprecated versions, so only `tickets` is served, as for the singular names of the other types since 003. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The next unsupported example is `ticket_categories`**: It stays unsupported until its own feature, which will move the example again (to `ticket_category_types`).
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **The guard's fragment is the plural**: As for Jurisdiction (009), the guard matches `tickets` rather than the singular (Clarifications), because the generic reference field `ticket_category_id`, which any type may declare, contains `ticket`. The later `ticket_categories` feature meets the same collision and can use its plural the same way.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged.
- **Fixture source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 009 delivered: the generic routes, central type registry, per-type declarations and fixture validation at start-up, declared and nullable reference fields, repeated names, the extract-id order rule in the fixture generation-rules test, the reference-resolution test with its completeness check, the jurisdiction-agreement test, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
