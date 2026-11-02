# Feature Specification: People API — Search and Single Office Holder

**Feature Branch**: `013-people-api`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Add the People API: `GET /api/v1/people` (search by `updated_since`, paginated) and `GET /api/v1/people/{id}` (one office holder), serving synthetic judicial office holders with their appointments, judiciary roles and authorisations. The leavers (`GET /api/v5/leavers`) and deleted (`GET /api/v5/deleted`) endpoints, also under the Swagger's "People" tag, are out of scope and will be separate features; this feature builds the pagination component they will reuse (Constitution Principles III and VI). Contract (E-Links Swagger v5, "People"): `GET /api/v5/people/{id}`: path `id` (string, required); query `include_previous_appointments` (boolean, optional); `200` "The found person" with a `CurrentOfficeHolderResponse` body. `GET /api/v5/people`: query `updated_since` (string, required), `per_page` (number, optional, "Defaults to 50"), `page` (number, optional, "Defaults to 1"), `include_previous_appointments` (boolean, optional); `200` "The found people" with a `PeopleApiResponse` body; `401` "Unauthorized. Invalid or missing token." [...] `PeopleApiResponse`: `results` and `pagination`. `PaginationResponse` requires `results`, `pages`, `current_page`, `results_per_page` and `more_pages`. `CurrentOfficeHolderResponse`, `AppointmentsResponse`, `JudiciaryRoleResponse`, `AuthorisationSummaryResponse` and `AuthorisationsResponse` as listed in the Swagger. Types are inferred from the examples and the leavers schema. Serve both routes under `/api/v1`, behind the existing bearer-token authentication, correlation IDs and shared `ErrorResponse` shape; behaviour the Swagger doesn't state follows 002's conventions and is listed as a contract deviation where it adds an observable response. `updated_since` filters genuinely on `last_updated`; results are ordered deterministically; `include_previous_appointments` must not depend on the wall clock; `per_page` defaults to 50 and `page` to 1. A checked-in, deterministic fixture of synthetic office holders with no real personal data. Close the People deferred items DF-2, DF-3, DF-4, DF-6 and DF-9: every reference to served reference data resolves and is tested. Add a reviewed `NoTypeSpecificCodeTest` exception scoped to the People response models only. Deliver unit, integration (including OpenAPI contract tests), functional and smoke coverage, generated OpenAPI documentation, Postman artifacts, and README and CLAUDE.md updates." (Full description: see the feature's `/speckit-specify` invocation; the elided parts are restated in the sections below.)

## Context

Features `002` to `012` built one generic reference-data capability and served all 11 E-Links reference-data types through it. Several of them recorded deferred items that only the People feature can close, because E-Links office holders refer to reference data:

- **DF-2** (004): appointments' `contract_type_id` and `contract_type` → contract types.
- **DF-3** (005): office holders' `gender_id` (and `gender`) → genders.
- **DF-4** (006): judiciary-role entries' `judiciary_role_id` or `judiciary_role_name_id` → judiciary roles.
- **DF-6** (007): appointments' `location`, `base_location` and `base_location_id` → base locations and locations.
- **DF-9** (010): authorisations' `ticket_id` → tickets.

This feature is the first one outside reference data. It serves the two People routes that return office holders: a search by `updated_since` and a single office holder by id. It also introduces the project's first paginated endpoint, so the pagination behaviour it defines becomes the shared component that the later leavers and deleted features reuse (Constitution Principles III and VI).

That shapes the feature:

- **New data, entirely synthetic** (FR-030 to FR-036). No People extract exists and none may be used: every name, code, email address and phone number is generated, in a checked-in, deterministic fixture.
- **Coherent with what is already served** (FR-037 to FR-045). Every id an office holder carries that names a served reference-data record must resolve to that record, and the name beside it must be that record's name, verified by automated tests.
- **Filters and paging that really work** (FR-010 to FR-022). `updated_since`, `page`, `per_page` and `include_previous_appointments` each change the response in a way tests can observe; none is accepted and ignored.
- **The rest of the mock is unchanged** (FR-050). Reference-data responses, the healthcheck, authentication and the shared error shape behave exactly as before.

Everything already established for business endpoints — bearer-token authentication, correlation IDs, the shared error response shape, exact path matching, `405`/`406` handling and the `500` fallback — applies to the People routes unchanged and is not restated here except where this feature tests it.

## Clarifications

### Session 2026-11-02

- Q: What format should the search route accept for `updated_since`? → A: A date only (`YYYY-MM-DD`); any time part returns `400`.
- Q: When `include_previous_appointments` is absent or `false`, which other parts of an office holder hide entries for previous appointments? → A: `authorisations_with_dates` drops authorisations under previous appointments and `authorisations` summarises only those that remain; `authorisations_current` and `judiciary_roles` are unaffected by the flag.
- Q: What do a dated authorisation's `jurisdiction_id` and `jurisdiction` refer to? → A: The served jurisdiction of the ticket's category (e.g. 30 "Courts" for ticket 294, 34 "Tribunals" for 365 and 367).
- Q: What should the service do at start-up if the configured People fixture is invalid? → A: Validate structure, uniqueness and every reference at start-up and refuse to start, naming the first problem; the build test also checks the default dataset.
- Q: Which People requests should the smoke suite's latency targets cover? → A: Both routes with 002's targets unchanged: the single route, a default search, and the whole dataset on one page (`per_page=120`) with `include_previous_appointments=true`.
- Q (raised in `/speckit-plan`): The type-name guard blocks words such as `appointment`, `location`, `gender` and `tickets`, which the People controller, service, fixture loader and start-up validator need, not only the response models. How is the guard's exception scoped? → A: It covers a reviewed list of People main-source files; in those files only an allowlisted set of contract names is permitted, and every other type name, including the plural type names that aren't contract fields, still fails (FR-052).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "People" section) defines the following. It gives no field types and its examples are truncated, so the types below are inferred from the examples and from the leavers schema; every inferred type is listed in the Records section and MUST match the generated OpenAPI document.

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Search route | `GET /api/v5/people` | `GET /api/v1/people` (the project's base path, as for reference data) |
| Single route | `GET /api/v5/people/{id}` | `GET /api/v1/people/{id}` |
| Search parameters | `updated_since` (required string), `per_page` (number, defaults to 50), `page` (number, defaults to 1), `include_previous_appointments` (boolean) | Same; validation rules in FR-010 to FR-016 |
| Single parameters | Path `id` (required string); `include_previous_appointments` (boolean) | Same; `id` is the office holder's UUID |
| Search 200 body | `PeopleApiResponse`: `results` (array of `CurrentOfficeHolderResponse`) and `pagination` | Same |
| Single 200 body | One `CurrentOfficeHolderResponse` | Same |
| `pagination` | `results`, `pages`, `current_page`, `results_per_page`, `more_pages` (the Deleted example's `"more pages"` is a typo; the schema wins) | Same, all required |
| 401 | "Unauthorized. Invalid or missing token." | Same |
| Validation errors, unknown person, 5xx | **Not documented** | The project's shared error contract (D-7, Error Scenario Summary) |
| Leavers, deleted | `GET /api/v5/leavers`, `GET /api/v5/deleted` | Out of scope (DF-11) |

### Contract Deviations

Per the constitution's Governance section, intentional deviations from the E-Links contract are recorded here:

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-7 | The People routes return `400`, `404`, `405`, `406` and `500` in the shared `ErrorResponse` shape for cases the Swagger doesn't document (Error Scenario Summary) | The Swagger documents only `200` and `401`. A mock that accepted invalid parameters or answered an unknown person with `200` would hide integration bugs (Principle VI); the shared shape is required by Principle IX. Same reasoning as 002's undocumented status codes. | Permanent, scope-justified; to be revisited if real E-Links error behaviour becomes known |
| D-8 | `include_previous_appointments` decides "current" against a configured reference date, not the real current date | Principle V: the same request must return the same response every day. The configured date stands in for "today" (FR-020). | Permanent, scope-justified |

No other deviation applies. Field names, nesting and required flags follow the Swagger exactly; the inferred types (Records) are not deviations, since the Swagger states none.

### Deferred Items

| # | Item | Why deferred | What the later feature MUST do |
|---|------|--------------|--------------------------------|
| DF-11 | Leavers (`GET /api/v5/leavers`) and deleted (`GET /api/v5/deleted`) | Separate endpoints under the same Swagger tag; out of scope by the feature description | Reuse this feature's pagination component (FR-017 to FR-019) rather than building another, and draw leavers and deleted people from a dataset coherent with this one: a leaver's `id`, `per_id` and `personal_code` MUST NOT clash with a current office holder's unless they are meant to be the same person. |

This feature closes 004 DF-2, 005 DF-3, 006 DF-4, 007 DF-6 and 010 DF-9 (FR-037 to FR-043). No served reference-data type is left with an outstanding deferred item.

## User Scenarios & Testing *(mandatory)*

The actors are the **integrator** (a team testing a client of the E-Links People API against this mock) and the **maintainer** (who owns this repository).

### User Story 1 - Integrator Finds People Updated Since a Date (Priority: P1)

An integrator syncing office holders asks for everyone updated since a date and gets back only those people, in a stable order, with their appointments, judiciary roles and authorisations.

**Why this priority**: Incremental sync by `updated_since` is the People API's main use; without it the endpoint has no value.

**Independent Test**: Request the search with an `updated_since` earlier than every record, and with one in the middle of the range, and check the counts, the order and that every returned person was updated on or after the date.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/people?updated_since=<a date before every record>`, **Then** the response is `200 OK`, `application/json`, with exactly two top-level fields, `results` and `pagination`; `results` holds the first 50 office holders and `pagination.results` equals the dataset size.
2. **AC-002**: **Given** valid authentication, **When** `updated_since` is a date inside the dataset's range, **Then** only office holders whose `last_updated` satisfies the filter (FR-011) are counted and returned, and `pagination.results` equals that count.
3. **AC-003**: **Given** valid authentication, **When** `updated_since` is later than every `last_updated`, **Then** the response is `200 OK` with `"results": []`, `pagination.results` `0`, `pagination.pages` `0` and `more_pages` `false`.
4. **AC-004**: **Given** any search, **When** it is sent twice, including across a service restart, **Then** both responses are identical, and results are ordered by ascending `last_updated`, then ascending `id` (FR-012).
5. **AC-005**: **Given** any office holder in a search response, **Then** it carries every required `CurrentOfficeHolderResponse` field with the types in the Records section, and no field outside the Swagger schema.

---

### User Story 2 - Integrator Pages Through Results (Priority: P1)

An integrator walks every page of a search, using the pagination block to know how many pages there are and when to stop.

**Why this priority**: The search is paginated by contract; a client that can't page can't sync more than the first 50 people.

**Independent Test**: Walk every page at several page sizes and check that each office holder appears exactly once and that the pagination block is right on every page.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication, **When** `page` and `per_page` are omitted, **Then** `pagination.current_page` is `1`, `pagination.results_per_page` is `50`, and `results` holds the first 50 matching office holders.
2. **AC-007**: **Given** a search matching N office holders and a `per_page` of P, **When** the caller requests every page from 1 to `pagination.pages`, **Then** `pages` is ⌈N ÷ P⌉, every page but the last holds P people and the last holds the rest, `more_pages` is `true` on every page but the last, and the union of all pages is the N office holders, each exactly once, in the order of FR-012.
3. **AC-008**: **Given** valid authentication, **When** `page` is past the last page, **Then** the response is `200 OK` with `"results": []`, `current_page` equal to the requested page, `more_pages` `false`, and `results`, `pages` and `results_per_page` as for any other page of that search.
4. **AC-009**: **Given** valid authentication, **When** `per_page` is larger than the number of matching people, **Then** every match is on page 1, `pages` is `1` and `more_pages` is `false`.

---

### User Story 3 - Integrator Retrieves One Office Holder (Priority: P1)

An integrator who already holds an office holder's id fetches that one person.

**Why this priority**: Clients look people up by id after a sync, or to refresh one record.

**Independent Test**: Fetch a known office holder by id and compare the body with the same person in a search response.

**Acceptance Scenarios**:

1. **AC-010**: **Given** valid authentication and the `id` of an office holder in the dataset, **When** the caller sends `GET /api/v1/people/{id}`, **Then** the response is `200 OK`, `application/json`, with a single `CurrentOfficeHolderResponse` object, not wrapped in `results` and with no `pagination`.
2. **AC-011**: **Given** the same office holder and the same `include_previous_appointments` value, **When** it is fetched by id and found by a search, **Then** the two objects are identical.
3. **AC-012**: **Given** valid authentication, **When** `id` is a well-formed UUID that matches no office holder, **Then** the response is `404 Not Found` in the shared error shape.
4. **AC-013**: **Given** valid authentication, **When** `id` is not a UUID (e.g. `abc`, a `per_id` such as `19759`, or a `personal_code`), **Then** the response is `400 Bad Request` in the shared error shape.

---

### User Story 4 - Integrator Asks for Previous Appointments (Priority: P2)

By default an integrator sees only an office holder's current appointments; asking for previous appointments adds the ended ones.

**Why this priority**: The flag changes what a client stores about each person; it must behave predictably, but it's secondary to finding people at all.

**Independent Test**: Fetch an office holder who has both current and ended appointments with the flag absent, `false` and `true`, and compare the appointment lists.

**Acceptance Scenarios**:

1. **AC-014**: **Given** an office holder with at least one current and one previous appointment (FR-020), **When** they are fetched with `include_previous_appointments` absent or `false`, **Then** `appointments` holds only the current appointments, and `authorisations_with_dates` and `authorisations` hold only the authorisations under them.
2. **AC-015**: **Given** the same office holder, **When** they are fetched with `include_previous_appointments=true`, **Then** `appointments` holds the current and the previous appointments, `authorisations_with_dates` and `authorisations` cover every authorisation, and `authorisations_current` and `judiciary_roles` are the same as without the flag.
3. **AC-016**: **Given** any office holder and any flag value, **Then** the response obeys FR-021 (`authorisations_with_dates` and `authorisations` follow the flag; `authorisations_current` and `judiciary_roles` don't), and every `authorisations_with_dates[].appointment_id` names an appointment in the same response's `appointments`.
4. **AC-017**: **Given** the flag on a search, **Then** it changes only the content of each office holder, never which office holders match, their order, or the pagination block.

---

### User Story 5 - People Data Agrees With the Reference Data (Priority: P1)

An integrator resolves the ids on an office holder against the reference-data endpoints this mock already serves, and every one resolves to a record with the same name.

**Why this priority**: Constitution Principle IV requires coherent synthetic data, and five deferred items (DF-2, DF-3, DF-4, DF-6, DF-9) make this feature responsible for it.

**Independent Test**: For every office holder in the default dataset, look up each reference id in the matching reference-data collection and compare names.

**Acceptance Scenarios**:

1. **AC-018**: **Given** the default dataset, **Then** every `gender_id` is a served gender's `id`, and `gender`, where present, and `sex` are that gender's `name` (DF-3).
2. **AC-019**: **Given** the default dataset, **Then** every appointment's `contract_type_id` is a served contract type's `id` and `contract_type` is its `name` (DF-2), and every `role_name_id` is a served appointment title's `id` and `role_name` its `name`.
3. **AC-020**: **Given** the default dataset, **Then** every appointment's `base_location_id` is a served base location's `id`, `base_location` and `court_name` are its `name`, `court_type` is the `name` of its location type, `location` is the `name` of the location its `parent_id` names, and `type` is the `name` of its jurisdiction (DF-6).
4. **AC-021**: **Given** the default dataset, **Then** every judiciary-role entry's `judiciary_role_name_id` is a served judiciary role's `id` and `name` is its `name` (DF-4).
5. **AC-022**: **Given** the default dataset, **Then** every authorisation's `ticket_id` is a served ticket's `id` and `ticket` its `name` (DF-9), and its `jurisdiction_id` and `jurisdiction` are the `id` and `name` of the served jurisdiction of that ticket's category (FR-042).
6. **AC-023**: **Given** the default dataset, **Then**, for each flag value, each office holder's `authorisations` summary agrees with the `authorisations_with_dates` in the same response, and `authorisations_current` agrees with their current dated authorisations (FR-044).

---

### User Story 6 - Invalid Requests Are Rejected (Priority: P2)

An integrator who sends a malformed or incomplete request gets a clear `400`, never a silently wrong answer.

**Why this priority**: Principle VI forbids accepting and ignoring parameters; rejecting bad input is what exposes client bugs.

**Independent Test**: Send each invalid case in the Error Scenario Summary and check the status and fixed message.

**Acceptance Scenarios**:

1. **AC-024**: **Given** valid authentication, **When** a search omits `updated_since` or gives it an empty or malformed value (FR-010), **Then** the response is `400 Bad Request` with the fixed message for that case.
2. **AC-025**: **Given** valid authentication, **When** `page` or `per_page` is zero, negative, fractional, non-numeric or empty, **Then** the response is `400 Bad Request` naming that parameter.
3. **AC-026**: **Given** valid authentication, **When** `include_previous_appointments` is anything other than `true` or `false`, **Then** the response is `400 Bad Request`.
4. **AC-027**: **Given** valid authentication, **When** a request carries a query parameter its route doesn't define (e.g. `foo=1` on either route, or `updated_since`, `page` or `per_page` on the single route), or repeats one, **Then** the response is `400 Bad Request` and no data is returned.
5. **AC-028**: **Given** valid authentication, **When** the path has a trailing slash or an extra segment (`/api/v1/people/`, `/api/v1/people/{id}/x`), **Then** the response is `404 Not Found` with "Resource not found.", as for every route.

---

### User Story 7 - Requests Without Valid Credentials Are Refused (Priority: P1)

An integrator without a valid bearer token gets `401` and no personal data, synthetic or not.

**Why this priority**: The routes carry the mock's richest data; they must be protected exactly like reference data.

**Independent Test**: Call both routes with no token and with a wrong token.

**Acceptance Scenarios**:

1. **AC-029**: **Given** no `Authorization` header, or a token outside the configured set, **When** either route is called, **Then** the response is `401 Unauthorized` with "Unauthorized. Invalid or missing token.", the `WWW-Authenticate: Bearer` header and no office-holder data.
2. **AC-030**: **Given** an invalid token and an otherwise invalid request (missing `updated_since`, malformed `id`), **Then** the response is still `401`: authentication is checked first (FR-003).

---

### User Story 8 - Existing Behaviour Is Unchanged (Priority: P2)

Everything the mock served before this feature behaves exactly as before.

**Why this priority**: Consumers already depend on the reference-data endpoints and the healthcheck.

**Independent Test**: Run every earlier feature's acceptance scenarios and golden-response tests.

**Acceptance Scenarios**:

1. **AC-031**: **Given** the delivered feature, **When** 001's to 012's acceptance scenarios are run, **Then** they all pass and every existing golden response is byte-for-byte identical.
2. **AC-032**: **Given** the delivered feature, **Then** the type-specific-code guard still forbids reference-data type names everywhere in main code except the reviewed People source files, where only the contract names of FR-052 are allowed.

---

### User Story 9 - Documentation and Example Requests Cover the New Routes (Priority: P3)

An integrator explores the People routes through Swagger UI or the Postman collection without reading source code.

**Why this priority**: Required by Principles XVII and XVIII, but it follows from the routes working.

**Independent Test**: Open the generated OpenAPI document and run the Postman collection against a running instance.

**Acceptance Scenarios**:

1. **AC-033**: **Given** a running instance, **When** the OpenAPI document is fetched, **Then** both People routes appear with every parameter, response status, schema, required flag and type of this spec, and contract tests confirm the document matches actual responses.
2. **AC-034**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** a "People" folder exercises a search with defaults, a later page, a past-the-last page, a filtered search, both flag values, a single office holder, an unknown and a malformed `id`, each validation error class, and missing and invalid credentials, each with checks for status, structure and key values, and no hard-coded environment values.

### Edge Cases

- **EC-001 — `updated_since` on a record's own date**: An office holder whose `last_updated` equals `updated_since` is included (FR-011).
- **EC-002 — No matches**: A search that matches nobody returns `200` with `"results": []`, `results` `0`, `pages` `0`, `current_page` as requested and `more_pages` `false`.
- **EC-003 — Very large `page` or `per_page`**: Any positive whole number up to the largest 64-bit integer is accepted, including values far beyond the dataset; a `page` past the last returns an empty page (AC-008), and a huge `per_page` puts every match on page 1 (AC-009). Neither returns `400` or `500`.
- **EC-004 — Leading zeros**: `page=007` means page 7, as 002 treats `reference_id`.
- **EC-005 — Case of the UUID**: `id` matches regardless of letter case; responses always write it in lower case.
- **EC-006 — An office holder with only previous appointments**: With the flag absent, their `appointments`, `authorisations_with_dates`, `authorisations` and `authorisations_current` are `[]`, while their `judiciary_roles` are still served (FR-021); they are still returned by searches that match them.
- **EC-007 — Ties on `last_updated`**: Office holders with the same `last_updated` are ordered by `id`, so page boundaries are stable.
- **EC-008 — A repeated base-location name**: Base locations may share a name (003). The appointment's `base_location_id` decides which record it names; `base_location` is that record's name.
- **EC-009 — Optional fields**: `gender`, `email_personal`, `circuit`, `bench` and `advisory_committee_area` are left out when the person or appointment has no value; required fields are always written, as `null` only where the Records section says they are nullable.
- **EC-010 — Order of checks**: authentication (`401`); route and method (`404`/`405`); unknown or repeated query parameters (`400`); parameter values (`400`); `id` format (`400`); lookup (`404`). The first failure decides the response.
- **EC-011 — Unexpected failure**: An invalid fixture never reaches a request, because the service refuses to start (FR-046); any other unexpected fault returns `500` in the shared error shape, exposing no internal detail.
- **EC-012 — `updated_since` with a time**: A date-time such as `2025-06-01T00:00:00Z` is rejected with `400` "updated_since must be a valid date."; only `YYYY-MM-DD` is accepted (FR-010).
- **EC-013 — Empty People fixture**: A configured fixture holding `[]` is valid. Every search returns `200` with no results and `pages` `0`, and every well-formed `id` returns `404`. With no records there are no references to resolve, so start-up validation (FR-046) doesn't require the reference-data types to be served.

## Requirements *(mandatory)*

### Functional Requirements

**Routing, authentication and errors**

- **FR-001**: System MUST expose `GET /api/v1/people`, returning the office holders that match `updated_since`, one page at a time.
- **FR-002**: System MUST expose `GET /api/v1/people/{id}`, returning the one office holder whose `id` equals the path value.
- **FR-003**: Both routes MUST require the existing bearer-token authentication, evaluated before any other check (EC-010), and MUST be read-only.
- **FR-004**: Both routes MUST carry the existing correlation-ID behaviour (`X-Correlation-Id` echoed on every response and equal to `traceId` on errors), and every error MUST use the shared `ErrorResponse` shape with the fixed messages of the Error Scenario Summary, never echoing caller input.
- **FR-005**: `id` MUST be a UUID in its canonical 8-4-4-4-12 hexadecimal form, matched without regard to letter case; anything else MUST return `400`.
- **FR-006**: A well-formed `id` that matches no office holder MUST return `404`.
- **FR-007**: Paths MUST match exactly; a trailing slash or extra segment MUST return `404` "Resource not found.". Methods other than `GET` MUST return `405`, and an `Accept` header that excludes `application/json` MUST return `406`, as for every route.

**Query parameters**

- **FR-010**: On the search route, `updated_since` MUST be present and non-empty, and MUST be a real calendar date written exactly as `YYYY-MM-DD`, matching the date-only `last_updated`; a missing or empty value, a date-time or any other time part (e.g. `2025-06-01T00:00:00Z`), any other layout, or a non-existent date (e.g. `2025-02-30`) MUST return `400`.
- **FR-011**: The search MUST return exactly the office holders whose `last_updated` is on or after `updated_since`.
- **FR-012**: Search results MUST be ordered by ascending `last_updated`, then ascending `id`, before paging.
- **FR-013**: `page` MUST default to `1` and `per_page` to `50`. When present, each MUST be a positive whole number written in decimal digits (leading zeros allowed), up to 9,223,372,036,854,775,807 (the largest 64-bit integer); zero, a sign, a fraction, letters, whitespace, an empty value or a larger number MUST return `400`. There is no other upper limit (EC-003).
- **FR-014**: `include_previous_appointments` is accepted on both routes and MUST be exactly `true` or `false` when present; absent means `false`. Any other value MUST return `400`.
- **FR-015**: A query parameter that the route doesn't define MUST return `400`. On the single route only `include_previous_appointments` is defined; on the search route `updated_since`, `page`, `per_page` and `include_previous_appointments` are.
- **FR-016**: A query parameter given more than once MUST return `400`, whatever its values.

**Pagination**

- **FR-017**: A search response MUST be `{ "results": [...], "pagination": {...} }`, with `pagination` holding exactly `results` (the number of matching office holders across all pages), `pages` (⌈`results` ÷ `results_per_page`⌉, `0` when nothing matches), `current_page` (the requested page), `results_per_page` (the effective `per_page`) and `more_pages` (`true` exactly when `current_page` < `pages`).
- **FR-018**: Pagination MUST be implemented as one reusable component, independent of the People data, so the leavers and deleted features (DF-11) can use it unchanged (Principles III and VI).
- **FR-019**: A `page` past the last page MUST return `200` with `"results": []` and the pagination block of FR-017.

**Previous appointments**

- **FR-020**: An appointment is *current* when its `end_date` is `null` or on or after the configured reference date, and *previous* otherwise. The reference date MUST come from configuration (default `2026-01-01`), never from the system clock (Principle V; D-8).
- **FR-021**: With `include_previous_appointments` absent or `false`, `appointments` MUST hold only current appointments, `authorisations_with_dates` MUST hold only the dated authorisations whose `appointment_id` names a current appointment, and `authorisations` MUST summarise only those (FR-044); with `true`, all three MUST be complete. `authorisations_current` and `judiciary_roles` MUST be the same whatever the flag's value (judiciary roles are not tied to appointments). Since an authorisation's dates lie within its appointment's (FR-043), an authorisation under a previous appointment is never current, so `authorisations_current` never names a ticket that is missing from the response.
- **FR-022**: The flag MUST NOT change which office holders a search matches, their order or the pagination block (AC-017).

**Response content**

- **FR-023**: Each office holder MUST carry exactly the `CurrentOfficeHolderResponse` fields, each appointment exactly the `AppointmentsResponse` fields, each judiciary-role entry exactly the `JudiciaryRoleResponse` fields, each summary exactly the `AuthorisationSummaryResponse` fields, and each dated authorisation exactly the `AuthorisationsResponse` fields, with the types, required flags and nullability of the Records section. No other field may appear.
- **FR-024**: `authorisations` and `authorisations_current` MUST be lists of `AuthorisationSummaryResponse`; `authorisations_with_dates` MUST be a list of `AuthorisationsResponse`.
- **FR-025**: Lists inside an office holder MUST be in a fixed order: appointments by ascending `start_date`, then `appointment_id`; judiciary roles by ascending `start_date`, then `judiciary_role_id`; dated authorisations by ascending `start_date`, then `authorisation_id`; summaries by `jurisdiction` name, with each summary's `tickets` by name.
- **FR-026**: The single route MUST return the same object for an office holder as the search route, for the same flag value (AC-011).

**Synthetic data**

- **FR-030**: The People dataset MUST be synthetic, deterministic and supplied by a checked-in fixture whose location is externally configurable (Principles IV, V and X). No People extract exists; no real person's name, personal code, email address or phone number may be used.
- **FR-031**: The default dataset MUST contain 120 office holders, so that the default page size gives three pages, the last one partial.
- **FR-032**: `id` values MUST be unique lower-case UUIDs; `per_id` values unique positive integers; `personal_code` values unique strings of digits; `appointment_id`, `authorisation_id` and `judiciary_role_id` values unique across the whole dataset. All MUST be stable across releases of the default dataset.
- **FR-033**: Email addresses MUST use the reserved `.example` top-level domain (RFC 2606), and phone numbers the Ofcom range reserved for drama (`020 7946 0000` to `020 7946 0999`), so no generated contact detail can reach a real person.
- **FR-034**: Every office holder MUST have at least one appointment, exactly one appointment with `is_principal` `true` among their current appointments when they have any, and `last_updated` on or after the latest `start_date` and `end_date` in their record.
- **FR-035**: The default dataset MUST include: office holders with several appointments; office holders with both current and previous appointments; at least one with only previous appointments (EC-006); ended and open judiciary roles; dated authorisations with and without an `end_date`; each optional field both present and absent; at least two office holders sharing a `last_updated` (EC-007); and `last_updated` values spread over at least 12 distinct months.
- **FR-036**: At least one office holder MUST hold authorisations for tickets 294 "Civil Authorisation", 365 "03 - Disability Living Allowance" and 367 "05 - Industrial Injuries", mirroring the Swagger examples.

**Coherence with reference data**

- **FR-037**: Every `gender_id` MUST be a served gender's `id`; `sex` and, where present, `gender` MUST be that gender's `name` (closes DF-3).
- **FR-038**: Every appointment's `contract_type_id` MUST be a served contract type's `id` and `contract_type` its `name` (closes DF-2).
- **FR-039**: Every appointment's `role_name_id` MUST be a served appointment title's `id` and `role_name` its `name`.
- **FR-040**: Every appointment's `base_location_id` MUST be a served base location's `id`; `base_location` and `court_name` MUST be its `name`, `court_type` the `name` of its `type_id` location type, `location` the `name` of its `parent_id` location, and `type` the `name` of its `jurisdiction_id` jurisdiction (closes DF-6). Only base locations with a non-null `parent_id` may be used.
- **FR-041**: Every judiciary-role entry's `judiciary_role_name_id` MUST be a served judiciary role's `id` and `name` its `name`; `judiciary_role_id` identifies the entry itself (closes DF-4).
- **FR-042**: Every dated authorisation's `ticket_id` MUST be a served ticket's `id` and `ticket` its `name` (closes DF-9); its `jurisdiction_id` MUST be the `jurisdiction_id` of the ticket's `ticket_category_id` ticket category, a served jurisdiction's `id`, and `jurisdiction` MUST be that jurisdiction's `name` (e.g. 30 "Courts" for ticket 294, 34 "Tribunals" for tickets 365 and 367). The Swagger examples' 24 "Civil" and 27 "Tribunals" are illustrative and not reproduced. Summaries (FR-044) are therefore grouped by served jurisdiction name.
- **FR-043**: Every `authorisations_with_dates[].appointment_id` MUST be one of the same office holder's `appointment_id`s, and each authorisation's dates MUST lie within its appointment's.
- **FR-044**: `authorisations` MUST summarise every dated authorisation in the response's `authorisations_with_dates` (which depends on the flag, FR-021), grouped by `jurisdiction`, listing each ticket name once; `authorisations_current` MUST do the same for the office holder's current dated authorisations only (`end_date` `null` or on or after the reference date of FR-020), whatever the flag's value.
- **FR-045**: An automated test MUST check FR-037 to FR-044 against the default dataset and the served reference data, failing the build if any reference doesn't resolve or any name disagrees.
- **FR-046**: At start-up the service MUST validate the configured People fixture, whatever its location, against FR-023's required fields, types and nullability and the rules of FR-032, FR-034 and FR-037 to FR-044, checked against the served reference data, and MUST refuse to start if any check fails. The failure message MUST name the first failing rule and the offending record's `id` (or position when the `id` is missing or invalid). The coverage rules of FR-035 and FR-036 describe the default dataset and are checked only by the build test (FR-045), not at start-up.

**Unchanged behaviour and guard**

- **FR-050**: Reference-data responses, the healthcheck and all existing error behaviour MUST be byte-for-byte unchanged (AC-031).
- **FR-051**: The People routes MUST NOT appear under, or change, the reference-data routes, and the reference-data code MUST stay free of People-specific logic.
- **FR-052**: The type-specific-code guard MUST keep forbidding reference-data type names in main code, with one reviewed exception: a fixed, reviewed list of People main-source files (the People controller, service, repository, start-up validator, mapper, entities, response models and properties) MAY use the contract's names `appointment`, `appointments`, `appointment_id`, `include_previous_appointments`, `gender`, `gender_id`, `location`, `base_location`, `base_location_id`, `contract_type`, `contract_type_id`, `judiciary_roles`, `judiciary_role`, `judiciary_role_id`, `judiciary_role_name_id`, `ticket`, `ticket_id`, `tickets`, `jurisdiction` and `jurisdiction_id`, in code and comments, including as word runs inside longer identifiers (e.g. `ResolvedAppointment`, `baseLocationTypeId`). In those files every other match still fails, and so does any spelling of a type name that isn't one of those contract names (e.g. `appointment_titles`, `base_locations`, `genders`, `contract_types`, `locations`); that check runs before contract names are allowed. The singular `judiciary_role` is allowed because the Swagger names the schema `JudiciaryRoleResponse`. Every other main source is checked exactly as before. Which reference-data type each People field refers to MUST come from configuration, never from main code. The exception MUST be covered by its own guard tests, and the guard MUST fail if a listed file doesn't exist.

**Documentation and examples**

- **FR-053**: Both routes MUST appear in the generated OpenAPI document, with every parameter, status code, schema, type, format, required flag and nullable flag of this spec, and contract tests MUST check the document against actual responses (Principle XVIII).
- **FR-054**: The example request collection MUST gain a "People" folder as AC-034 describes, using environment variables for every environment-specific value (Principle XVII).
- **FR-055**: README and CLAUDE.md MUST describe the People routes, their parameters, the dataset and its rules, and record DF-2, DF-3, DF-4, DF-6 and DF-9 as closed.

### Records

Types are inferred (Behavioural Reference); "req." is the Swagger's required flag. Required fields are always written; optional fields are left out when unset.

**`CurrentOfficeHolderResponse`**

| Field | Type | Req. | Nullable | Notes |
|-------|------|------|----------|-------|
| `id` | string (uuid) | yes | no | Lower case |
| `last_updated` | string (date) | yes | no | Filtered by `updated_since` |
| `per_id` | integer | yes | no | |
| `personal_code` | string | yes | no | Digits |
| `title` | string | yes | no | e.g. "District Judge" |
| `known_as` | string | yes | no | |
| `surname` | string | yes | no | |
| `fullname` | string | yes | no | `title`, `known_as` and `surname` |
| `initials` | string | yes | no | |
| `post_nominals` | string | yes | yes | |
| `email` | string | yes | no | `.example` domain |
| `sex` | string | yes | no | Gender name (FR-037) |
| `gender` | string | no | no | Gender name when present |
| `gender_id` | integer | yes | no | Gender id |
| `work_phone` | string | yes | yes | Drama range |
| `disability` | boolean | yes | no | |
| `email_personal` | string | no | no | `.example` domain |
| `retirement_date` | string (date) | yes | yes | |
| `leaving_on` | string (date) | yes | yes | `null` for every current office holder in the default dataset |
| `appointments` | array of `AppointmentsResponse` | yes | no | |
| `judiciary_roles` | array of `JudiciaryRoleResponse` | yes | no | |
| `authorisations` | array of `AuthorisationSummaryResponse` | yes | no | |
| `authorisations_current` | array of `AuthorisationSummaryResponse` | yes | no | |
| `authorisations_with_dates` | array of `AuthorisationsResponse` | yes | no | |

**`AppointmentsResponse`**: `appointment_id` (integer), `role_name` (string), `role_name_id` (integer), `type` (string), `court_name` (string), `court_type` (string), `circuit` (string, optional), `bench` (string, optional), `advisory_committee_area` (string, optional), `location` (string), `base_location` (string), `base_location_id` (integer), `is_principal` (boolean), `start_date` (date, optional in the Swagger, always written), `end_date` (date, optional in the Swagger, always written, `null` when open), `contract_type` (string), `contract_type_id` (integer).

**`JudiciaryRoleResponse`**: `name` (string), `judiciary_role_id` (integer), `judiciary_role_name_id` (integer), `start_date` (date), `end_date` (date, required, `null` when open).

**`AuthorisationSummaryResponse`**: `jurisdiction` (string), `tickets` (array of string).

**`AuthorisationsResponse`**: `authorisation_id` (integer), `appointment_id` (integer), `jurisdiction` (string, the jurisdiction's name), `jurisdiction_id` (integer, the served jurisdiction of the ticket's category, FR-042), `ticket` (string), `ticket_id` (integer), `start_date` (date, always written), `end_date` (date, always written, `null` when open).

**`PaginationResponse`**: `results`, `pages`, `current_page`, `results_per_page` (integers), `more_pages` (boolean), all required.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Search with defaults | `GET /api/v1/people?updated_since=2000-01-01` | `200`, first 50 people, pagination |
| Later page | `...&page=3` | `200`, the last 20 people, `more_pages` `false` |
| Past the last page | `...&page=4` | `200`, `"results": []` |
| Single office holder | `GET /api/v1/people/{id}` | `200`, one object |
| Missing `updated_since` | `GET /api/v1/people` | `400`, "updated_since is required." |
| Malformed `updated_since` | `?updated_since=yesterday` | `400`, "updated_since must be a valid date." |
| `updated_since` with a time | `?updated_since=2025-06-01T00:00:00Z` | `400`, "updated_since must be a valid date." |
| Bad `page` | `?updated_since=2000-01-01&page=0` | `400`, "page must be a positive whole number." |
| Bad `per_page` | `?updated_since=2000-01-01&per_page=abc` | `400`, "per_page must be a positive whole number." |
| Bad flag | `?updated_since=2000-01-01&include_previous_appointments=yes` | `400`, "include_previous_appointments must be true or false." |
| Unknown query parameter | `?updated_since=2000-01-01&foo=1` | `400`, "Unsupported query parameter." |
| Repeated query parameter | `?updated_since=2000-01-01&page=1&page=2` | `400`, "Query parameters must not be repeated." |
| Malformed `id` | `GET /api/v1/people/abc` | `400`, "id must be a UUID." |
| Unknown `id` | `GET /api/v1/people/00000000-0000-0000-0000-000000000000` | `404`, "Person not found." |
| Trailing slash | `GET /api/v1/people/` | `404`, "Resource not found." |
| Missing or invalid token | either route | `401`, "Unauthorized. Invalid or missing token." |
| Unsupported method | `POST /api/v1/people` | `405`, "Method not allowed." |
| Unacceptable media type | `Accept: text/plain` | `406`, "Not acceptable." |
| Unexpected failure | unexpected runtime fault | `500`, "Internal server error." |

### Key Entities

- **Office holder**: One synthetic judicial office holder, identified by a UUID `id`, with `per_id` and `personal_code` as secondary identifiers, personal details, a `last_updated` date, and lists of appointments, judiciary roles and authorisations. Refers to a gender.
- **Appointment**: One post an office holder holds or held, at a base location, under a contract type and an appointment title. Current or previous relative to the reference date.
- **Judiciary role**: A role an office holder holds or held, naming a served judiciary role.
- **Authorisation**: A dated ticket an office holder holds under one of their appointments, naming a served ticket and a jurisdiction; summarised by jurisdiction in `authorisations` and `authorisations_current`.
- **Page**: One slice of a search's ordered matches, described by the pagination block; the shape is shared with the later leavers and deleted features.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 34 acceptance scenarios (AC-001 to AC-034) pass as automated tests.
- **SC-002**: Walking every page of a search at page sizes 1, 7, 50 and 120 returns each matching office holder exactly once, in the same order, in 100% of runs.
- **SC-003**: 100 consecutive identical requests, including across a service restart and on different calendar days, return identical responses.
- **SC-004**: 0 references from the People dataset to reference data fail to resolve, and 0 names disagree with the record they refer to.
- **SC-005**: 0 requests without valid credentials receive office-holder data; 100% receive `401`.
- **SC-006**: 0 invalid parameter values in the Error Scenario Summary are accepted; 100% return `400` with the documented message.
- **SC-007**: An integrator using the published API documentation and the example requests can sync every office holder and fetch one by id on their first attempt, without reading source code.
- **SC-008**: Every earlier feature's golden response is byte-for-byte unchanged.
- **SC-009**: Each of these requests meets 002's smoke-suite latency targets unchanged (p95 under 1 second by default, under 100 ms in strict mode), with every call returning `200`: a single office holder by `id`; a search with defaults; and a search returning the whole default dataset on one page (`per_page=120`) with `include_previous_appointments=true`.

## Assumptions

- **Base path**: The mock serves the contract's `/api/v5` routes under its own `/api/v1`, as it does for reference data.
- **Inferred types**: The Swagger states no types. `id` is a UUID string and `personal_code` a string, as in the leavers example; `per_id` and every `*_id` other than `id` are integers; dates are `YYYY-MM-DD`, and `last_updated` is a date, as in the leavers example, so `updated_since` takes a date too (Clarifications); `disability` and `is_principal` are booleans.
- **Error statuses**: Statuses the Swagger omits follow the project's shared error contract (D-7), as in 002. Distinguishing a malformed `id` (`400`) from an unknown one (`404`) follows 002's `reference_id` rule.
- **`updated_since` is inclusive**: "updated since this date" is read as on or after it, so a record updated on the given date is returned.
- **Search order**: The Swagger states none; ascending `last_updated`, then `id`, gives a sync client the oldest changes first and stable page boundaries.
- **`results_per_page`** is the effective page size requested (or the default), not the number of records on the current page; the Swagger's examples are illustrative and not internally consistent (e.g. 200 results at 19 per page shown as 10 pages).
- **Reference date**: `2026-01-01` by default, set in configuration; it sits inside the dataset's date range so that both current and previous appointments exist.
- **`sex` and `gender`**: The Swagger carries both; with only one gender reference type served, both carry the gender record's name, and `gender` is left out for some office holders to cover the optional field.
- **Judiciary roles**: `judiciary_role_name_id` points at the served judiciary roles, mirroring `role_name_id` on appointments; `judiciary_role_id` is the entry's own id.
- **Appointment details**: `court_name`, `court_type`, `location` and `type` are derived from the appointment's base location (FR-040) rather than invented, so they always agree with the reference data. `circuit`, `bench` and `advisory_committee_area` are synthetic values from small fixed lists.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged to both People routes, including a search returning the whole dataset on one page (SC-009).
- **Dataset size**: 120 office holders is enough to exercise paging at the default size without slowing the build; the fixture can be swapped by configuration (Principle X).
- **No extract**: Unlike the reference-data features, nothing is taken from a local extract except the ids and names of reference data that is already served.
