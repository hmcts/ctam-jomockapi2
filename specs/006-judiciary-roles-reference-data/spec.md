# Feature Specification: Reference Data API — Judiciary Roles

**Feature Branch**: `006-judiciary-roles-reference-data`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Add the judiciary_roles reference-data type to the existing generic reference-data API (feature 002). Contract reference (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `judiciary_roles` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` ("The found reference data": `ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` has `id`, `updated_at`, `created_at`, `start_date` and `end_date`, all required; the People schema carries `judiciary_roles` (required), a list of `JudiciaryRoleResponse` with `name`, `judiciary_role_id`, `judiciary_role_name_id`, `start_date` and `end_date`, all required. Scope: serve `judiciary_roles` on `GET /api/v1/reference_data/judiciary_roles` and `/{reference_id}`, reusing the generic mechanism (a type entry and a checked-in fixture, no type-specific code, no main-code change expected; NoTypeSpecificCodeTest must cover judiciary roles in every spelling); names unique (the default); no reference or boolean fields; authentication, validation, error shape, ordering and edge cases as 002. Record shape: the Swagger fields plus `name` (existing deviation), nothing else. Fixture: generated once from the `name` column of the local judiciary role reference extract (REF_JudiciaryRole.csv: 164 rows, 164 distinct names, no blank names or ids), ignoring every other column; note that the extract's ids are 90000–90163 and are never served. Names are copied exactly, read as UTF-8, with nothing trimmed, corrected or normalised: some names contain non-ASCII characters — the right single quotation mark `’` (e.g. "Judge in Charge of Jury List (King’s Bench)", "Magistrates’ Liaison Judge") and the en dash `–` (e.g. "Boundary Commission for Wales – Deputy Chair") — written literally in the fixture, not escaped; the extract mixes the ASCII apostrophe and `’` ("Queen's Bench" vs "King’s Bench") and the hyphen and en dash, and both are kept as they are; apparent typos and truncations in the extract ("Principle Judge - Land Registration", "Judicial Commissioner Investigatory Powers Commiss") are kept unchanged; names also contain `&`, `(`, `)` and `-`. Sort names in code-point order (so `’` sorts after every ASCII character, and "…(King’s Bench)" sorts before "…(Queen's Bench)"). Use ids of 10 × position (10–1640) and dates by 002's rule (23 ended records, ids 70, 140, …, 1610). Say in the spec that the extract's own 14 end-dated rows (e.g. "Regional Judge", "Vice President Queens Bench") are not reproduced and that 002's position-based end dates apply instead. Document known reference points: first (10 "Acting Designated Civil Judge"), last (1640 "Wales Training Course Director (JC)"), an end-dated record (70 "Acting Senior Costs Judge"), a name with `’` (960 "Judge in Charge of Jury List (King’s Bench)") next to its ASCII-apostrophe neighbour (970 "Judge in Charge of Jury List (Queen's Bench)"), a name with an en dash (150 "Boundary Commission for Wales – Deputy Chair"), a name with `&` (170 "Business & Property Courts Supervising"), an unknown id inside the range (15) and one outside it (1650). Deferred: the People feature's office holders must reference these synthetic judiciary role ids from their `judiciary_roles` entries, with a test that every reference resolves; which of `judiciary_role_id` and `judiciary_role_name_id` points at this reference data is left to that feature. Every still-unsupported E-Links name must be rejected with `400`. Deliver unit, integration, functional and smoke coverage, generated OpenAPI documentation and Postman artifacts (collection and environment, with test scripts), with acceptance scenarios mirroring the existing reference-data features. Deprecated versions are not to be considered."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003-base-locations-reference-data`, `004-contract-types-reference-data` and `005-genders-reference-data` added BaseLocation, ContractType and Gender through configuration and fixtures; 005 showed that a type with only the shared fields needs no change to main code.

This feature adds JudiciaryRole, the fifth of the eleven E-Links reference-data types: the roles a judicial office holder can hold alongside their appointments, such as "Designated Civil Judge", "Resident Judge" or "Mentor Judge". Like Gender, its records have only the shared fields, so it needs a type entry and a fixture and **no change to main code**. Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to JudiciaryRole unchanged and is not restated here except where this feature tests it. What is new is the data: 164 names, the first served names to contain the right single quotation mark `’`, mixed with ASCII apostrophes, en dashes and hyphens, plus a few apparent typos and truncations in the source, all of which are served exactly as the extract has them (FR-008, EC-002 to EC-006). The mock doesn't serve the deprecated alias `judiciary_role` (FR-001; Constitution Principle VII).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `judiciary_roles` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. Its People section shows that each office holder carries `judiciary_roles` (required): a list of `JudiciaryRoleResponse` entries with `name`, `judiciary_role_id`, `judiciary_role_name_id`, `start_date` and `end_date`, all required. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `judiciary_roles` | Same |
| Deprecated alias | Out of scope | Not served: `judiciary_role` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (deviation D-1 from 002, extended to this type) |
| Type-specific fields | None documented; the extract has none beyond the shared fields | None |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | JudiciaryRole records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of roles without names has no practical use. This feature applies the existing, agreed deviation to the new type. | Permanent, scope-justified (unchanged from 002) |

No other deviation: JudiciaryRole records carry no reference or boolean fields.

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-4 | Office holders will refer to judiciary roles from their `judiciary_roles` entries (Swagger `JudiciaryRoleResponse`) | The People feature doesn't exist yet. JudiciaryRole is served first, so this is a backward reference that Constitution Principle IV requires to resolve once both types are served. | The People feature MUST decide which of `judiciary_role_id` and `judiciary_role_name_id` refers to this reference data, MUST set that field in every entry to the `id` of a served JudiciaryRole record (this feature's synthetic ids, not the extract's), and MUST add a test that every such reference resolves; that feature is incomplete until the test passes. If its entries carry `name`, it MUST equal the referenced record's `name`. It MUST also add a deliberate, reviewed exception to the type-specific-code guard (FR-016) for its own `judiciary_roles` field and any field names containing `judiciary_role`, since this feature makes the guard forbid `judiciary_role` in every spelling. |

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Judiciary Roles (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of judiciary roles so that my system can load and resolve judiciary role reference data exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving judiciary roles can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/judiciary_roles` and confirm a `200 OK` JSON response whose `results` array holds all 164 judiciary role records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/judiciary_roles`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every JudiciaryRole record in the configured dataset (164 by default), each with exactly the shared reference-data fields, in ascending `id` order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** the records at the Reference Points table's ids equal that table (values for `id`, `name`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** every name is returned with exactly the characters of the source: names with the right single quotation mark `’` (e.g. "Magistrates’ Liaison Judge"), the en dash `–`, the ASCII apostrophe, `&`, parentheses and hyphens are unchanged, and no apostrophe, dash, spelling or length has been normalised or corrected (EC-002 to EC-005).
4. **AC-004**: **Given** the default dataset, **When** the collection is retrieved, **Then** exactly 23 records are ended — those with ids 70, 140, …, 1610 (every multiple of 70) — each with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z, and every other record has a null `end_date` and `updated_at` 2024-06-03T10:30:00Z (EC-007).
5. **AC-005**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Judiciary Role by ID (Priority: P1)

As an integrator, I want to fetch one judiciary role by its reference identifier so that my system can resolve an identifier it holds (for example one from an office holder's `judiciary_roles` entry) to its full reference record.

**Why this priority**: Part of the same contract as the collection route, and it exercises the identifier validation paths.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/judiciary_roles/{id}` for a reference-point id and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/judiciary_roles/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including names with non-ASCII characters (e.g. `960`, "Judge in Charge of Jury List (King’s Bench)").
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no judiciary role (`15`, which falls in an id gap; `1650`, just past the last id; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `10`, which is also an AppointmentTitle, BaseLocation, ContractType and Gender id), **When** it is requested under `judiciary_roles`, **Then** the result depends only on the JudiciaryRole dataset: identifiers are scoped to their type and never resolve across types.

---

### User Story 3 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `judiciary_role` rejected clearly, so that my system uses the canonical name `judiciary_roles` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested, so a request via the singular name never returns data.

**Independent Test**: Call `judiciary_role` and `judiciary_role/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-010**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/judiciary_role`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-011**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/judiciary_role/{reference_id}` for any `reference_id` (one that exists under `judiciary_roles` such as `20`, an unknown one such as `15`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-012**: **Given** a request to a `judiciary_role` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name, as for any unsupported name.

---

### User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want judiciary role requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each judiciary role route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-013**: **Given** a request with no credentials, **When** it is sent to any judiciary role route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-014**: **Given** a request with present but invalid credentials, **When** it is sent to any judiciary role route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using appointment titles, base locations, contract types or genders, I want adding judiciary roles to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: Adding a type through configuration must not change any other type's behaviour; this is the regression boundary of the feature.

**Independent Test**: Re-run 002's, 003's, 004's and 005's acceptance tests unchanged, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-015**: **Given** the delivered feature, **When** 002's, 003's, 004's and 005's acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature. Shared test lists of unsupported names may lose `judiciary_roles`, and examples that used `judiciary_roles` as an unsupported type, such as the missing-token check and the example "unsupported" request, move to another still-unsupported name (`jurisdictions`). Nothing else asserted changes.
2. **AC-016**: **Given** valid authentication, **When** each of the 16 E-Links attribute names that are still unsupported (the six canonical types not yet served: `jurisdictions`, `location_types`, `locations`, `ticket_categories`, `ticket_category_types` and `tickets`; their six singular forms; and the unserved singular names `base_location`, `contract_type`, `gender` and `judiciary_role`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-017**: **Given** valid authentication, **When** the caller sends a judiciary role request with any query parameter (e.g. `?name=Mentor%20Judge`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 6 - Maintainer Adds the Type With Configuration and Data Only (Priority: P2)

As a maintainer, I want judiciary roles added by declaring the type and supplying its dataset alone, with no change to main code, so that 002's extension design keeps working unaided for a larger dataset with non-ASCII names.

**Why this priority**: Confirms again the extensibility 002 was designed for (its AC-019 and SC-007), now with the first served names containing `’`, and keeps the cost of the remaining types low.

**Independent Test**: Review the change and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-018**: **Given** the delivered change, **When** it is reviewed, **Then** it changes no main source code at all; every addition is the type entry, its dataset, tests, documentation and example requests.
2. **AC-019**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes, and it now also recognises the judiciary role type name in every spelling (snake_case, spaced words, camelCase, upper case), so no main source file names a specific reference-data type.
3. **AC-020**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `judiciary_roles` as a supported value alongside the existing ones, does not list `judiciary_role`, and `ReferenceDataResponse` is unchanged, without that documentation having been edited by hand.

---

### User Story 7 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for judiciary roles so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every judiciary role request's checks pass.

**Acceptance Scenarios**:

1. **AC-021**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the judiciary role collection, a single record by reference-point id (including one whose name contains `’`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `judiciary_role` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values.

---

### Edge Cases

- **EC-001 — The extract's ids are not used**: The extract's ids run from 90000 to 90163. They are never served: every record's `id` is assigned by position (FR-009).
- **EC-002 — Non-ASCII characters**: Seven names contain non-ASCII characters: the right single quotation mark `’` (U+2019; e.g. "Magistrates’ Liaison Judge", "Director of Magistrates’ Training (JC)") or the en dash `–` (U+2013; e.g. "Deputy Chairman – Advisory Committee"). They are kept as those characters, written literally in the dataset rather than as escapes, and returned as the same characters. Names never appear in a request path, and query parameters are rejected (AC-017).
- **EC-003 — Mixed apostrophes and dashes**: The source mixes the ASCII apostrophe and `’` ("Judge in Charge of Jury List (Queen's Bench)" next to "Judge in Charge of Jury List (King’s Bench)") and the hyphen and en dash ("Principle Judge - Land Registration" next to "Boundary Commission for Wales – Deputy Chair"). Neither is normalised. No two names would become equal even if they were, so uniqueness doesn't depend on it.
- **EC-004 — Near-duplicate names**: "Vice President Kings Bench", "Vice President of King's Bench" and "Vice President Queens Bench" are distinct names and distinct records, as are "Business & Property Courts Supervising" and "Business & Property Courts Supervising Judge". None is merged or dropped.
- **EC-005 — Apparent typos and truncation**: "Principle Judge - Land Registration" and "Principle Judge - Residential Property" (for "Principal"), and "Judicial Commissioner Investigatory Powers Commiss", which is cut off, are kept exactly as the source has them. No name is longer than 50 characters, and the truncated one is exactly 50, so the source may cut names off at that length; no name is extended or corrected.
- **EC-006 — Historical names**: Names that refer to the Queen's Bench are served alongside the King's Bench names; the mock reproduces the reference data and doesn't filter it by currency.
- **EC-007 — The extract's end dates are not reproduced**: The extract end-dates 14 rows (e.g. "Regional Judge", "Vice President Queens Bench"). Their end dates are ignored and 002's position-based rule applies instead, so those two are open here, and 23 other positions are ended. Only one of the 14, "Senior Family Liaison Judge" (`1400`), happens to be ended by the rule.
- **EC-008 — Code-point order**: Names are sorted by Unicode code point, not alphabetically. Upper-case letters sort before lower-case ones, so "Course Director Long and Complex Trials (JC)" (`320`) and "Course Director Youth Training (JC)" (`330`) come before "Course Director for COP (JC)" (`340`). `’` and `–` sort after every ASCII character, although in this dataset no two neighbouring names are put in order by them: "Judge in Charge of Jury List (King’s Bench)" (`960`) comes before "Judge in Charge of Jury List (Queen's Bench)" (`970`) because K precedes Q. The order is fixed by the generation rule, not by any business meaning of the names.
- **EC-009 — Identifier gaps**: Ids are multiples of 10, so a well-formed id between two of them (e.g. `15`) or beyond the last one (e.g. `1650`) returns `404`.
- **EC-010 — Same id, different type**: `GET /api/v1/reference_data/judiciary_roles/10` returns "Acting Designated Civil Judge", while id `10` under `appointment_titles`, `base_locations`, `contract_types` and `genders` returns "Acting Senior Coroner", "Aberconwy", "Fee-paid" and "Female"; identifiers are scoped to their type.
- **EC-011 — Case and spelling variants**: `Judiciary_roles`, `JUDICIARY_ROLES`, `judiciary-roles`, `judiciaryRoles` and `judiciary_roles_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-012 — Inherited edge cases**: Covered by FR-004. For judiciary roles, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the JudiciaryRole reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `judiciary_roles`. The singular name `judiciary_role` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders` and `judiciary_roles`. This supersedes 005's FR-002; every other E-Links attribute name, including `judiciary_role`, MUST remain unsupported (`400`).
- **FR-003**: JudiciaryRole MUST be added through the generic reference-data capability alone: a type entry and its dataset. The change MUST NOT modify any main source code, and MUST NOT introduce code that names a specific reference-data type.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to JudiciaryRole without exception. Names MUST be unique within the type (the default), and the type declares no reference fields and no boolean fields.

**Response content**

- **FR-005**: Each JudiciaryRole record MUST contain exactly 002's fields, in 002's order: `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`, in 002's formats (002 FR-011). Names MUST be returned with exactly the characters of the dataset (FR-008). The records of the existing types MUST stay exactly as 002 to 005 define them.

**Data**

- **FR-006**: The JudiciaryRole dataset MUST be synthetic, deterministic and supplied as a fixed, checked-in fixture. Only the public `name` values may be taken from the supplied judiciary role reference extract; every other value (`id`, dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over.
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 164 rows in the extract, which have 164 distinct names. Each name MUST be carried over exactly as the extract has it, read as UTF-8: nothing trimmed, case-folded, corrected, shortened or filtered, and no apostrophe or dash normalised (EC-002 to EC-006). Non-ASCII characters MUST be written literally in the dataset, not as escapes. Every name is free of leading or trailing whitespace.
- **FR-009**: Records MUST be generated following 002's fixture generation rules: names sorted in code-point order and numbered from position 1 to 164; `id` = 10 × position (10 to 1640); records whose position is a multiple of 7 (23 records, ids 70 to 1610) are ended, with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; all other records have no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own ids, dates and timestamps, including its 14 end dates, MUST NOT be used (EC-001, EC-007).
- **FR-010**: JudiciaryRole ids MUST be stable across releases of the default dataset, so that the People feature (DF-4) and consumer test suites can refer to them.

**Documentation and example requests**

- **FR-011**: The generated API documentation MUST list `judiciary_roles` among the supported `attribute_name` values and MUST NOT list `judiciary_role`, derived from the configuration rather than edited by hand. The `ReferenceDataResponse` schema MUST be unchanged.
- **FR-012**: The example request collection MUST cover the judiciary role routes as AC-021 describes, using environment variables for every environment-specific value. Requests that use `judiciary_roles` as an unsupported example MUST move to a still-unsupported name (AC-015).
- **FR-013**: The project's documentation of supported reference-data types MUST list judiciary roles, and the instructions for adding a type MUST remain accurate.

**Quality**

- **FR-014**: The judiciary role collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-015**: Unit, integration, functional and smoke coverage MUST be extended to the new type, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009) MUST be checked by an automated test, as for the existing types, wherever they can be checked without the extract; the names are compared with the extract once, when the fixture is generated (FR-007).
- **FR-016**: The automated guard against type-specific code MUST enforce FR-003's ban for this type: it MUST recognise `judiciary_role` in every spelling (singular and plural, snake_case, spaced words, camelCase, upper case), as it does for the existing types, with no exemption for field names such as `judiciary_roles`, `judiciary_role_id` or `judiciary_role_name_id` (DF-4).

### Reference Points

These records of the default dataset are fixed by FR-008 and FR-009 and are used by tests and example requests:

| Position | `id` | `name` | `end_date` | `updated_at` |
|----------|------|--------|------------|--------------|
| 1 (first) | 10 | Acting Designated Civil Judge | null | 2024-06-03T10:30:00Z |
| 7 (first ended) | 70 | Acting Senior Costs Judge | 2025-03-31 | 2025-04-01T08:00:00Z |
| 14 (ended; en dash) | 140 | Boundary Commission for England – Deputy Chair | 2025-03-31 | 2025-04-01T08:00:00Z |
| 15 (en dash) | 150 | Boundary Commission for Wales – Deputy Chair | null | 2024-06-03T10:30:00Z |
| 17 (`&`; end-dated in the extract) | 170 | Business & Property Courts Supervising | null | 2024-06-03T10:30:00Z |
| 34 (code-point order) | 340 | Course Director for COP (JC) | null | 2024-06-03T10:30:00Z |
| 96 (`’`) | 960 | Judge in Charge of Jury List (King’s Bench) | null | 2024-06-03T10:30:00Z |
| 97 (ASCII apostrophe) | 970 | Judge in Charge of Jury List (Queen's Bench) | null | 2024-06-03T10:30:00Z |
| 107 (truncated in the source) | 1070 | Judicial Commissioner Investigatory Powers Commiss | null | 2024-06-03T10:30:00Z |
| 128 (typo in the source) | 1280 | Principle Judge - Land Registration | null | 2024-06-03T10:30:00Z |
| 140 (ended; also end-dated in the extract) | 1400 | Senior Family Liaison Judge | 2025-03-31 | 2025-04-01T08:00:00Z |
| 161 (last ended) | 1610 | Vice President of the Unified Tribunals | 2025-03-31 | 2025-04-01T08:00:00Z |
| 164 (last) | 1640 | Wales Training Course Director (JC) | null | 2024-06-03T10:30:00Z |

Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. `15` is a guaranteed unknown id inside the range, and `1650` and `999999` are outside it.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/judiciary_roles` | `200`, 164 records |
| Unserved singular name | `GET /api/v1/reference_data/judiciary_role` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/judiciary_roles/960` | `200`, Judge in Charge of Jury List (King’s Bench) |
| Unknown id (gap) | `GET /api/v1/reference_data/judiciary_roles/15` | `404`, shared error shape |
| Unknown id (past the end) | `GET /api/v1/reference_data/judiciary_roles/1650` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/judiciary_roles/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/judiciary_roles?name=Mentor%20Judge` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/jurisdictions` | `400`, shared error shape |
| Missing or invalid credentials | any judiciary role route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002. This feature adds a fifth instance, JudiciaryRole, with canonical name `judiciary_roles`, no served alias, and no reference or boolean fields.
- **JudiciaryRole record**: One role a judicial office holder can hold, such as "Designated Civil Judge" or "Mentor Judge". Attributes: `id` (unique, stable integer), `name` (unique public role name, exactly as in the source, deviation D-1), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. Its identifiers are independent of other types' identifiers. Office holders will refer to it from their `judiciary_roles` entries (DF-4).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 21 acceptance scenarios (AC-001 to AC-021) pass as automated tests or, for AC-018 and AC-021, as review and collection-run checks.
- **SC-002**: The default judiciary role collection contains exactly 164 records with 164 distinct names, 23 of them ended, matching the Reference Points table.
- **SC-003**: 100% of the 164 served names are identical, character for character, to the source names, including the 7 that contain non-ASCII characters.
- **SC-004**: 0 requests via the unserved singular name `judiciary_role` return reference data; 100% with valid credentials return `400`.
- **SC-005**: 0 of the 16 still-unsupported attribute names return a `200` response.
- **SC-006**: 0 judiciary role requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-007**: 100% of 002's, 003's, 004's and 005's acceptance tests pass with their assertions and expected responses unchanged, apart from the unsupported-name examples AC-015 allows to move.
- **SC-008**: Adding the type changes 0 lines of main Java code; the only main resource changes are the type entry and its fixture.
- **SC-009**: The judiciary role collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-010**: An integrator using the published documentation and example requests retrieves the judiciary role list and a single judiciary role at the first attempt, without reading source code.

## Assumptions

- **Source values are public role titles**: The 164 names are the titles of judicial roles (e.g. "Resident Judge", "Lord Lieutenant"), not data about any person, so reusing them is consistent with Constitution Principle IV. Every other value is synthetic. No real office holder's roles are involved.
- **Names are reproduced, not curated**: The mock stands in for the real reference data, so the source's spellings, truncations, apostrophes and dashes are part of what consumers must handle. Correcting them would make the mock differ from the data it imitates (EC-003 to EC-006).
- **Same generation rules as 002**: The dates, timestamps and ended-record rule are 002's, applied unchanged, so all default datasets follow one documented pattern. For 164 records the rule ends 23, so this type meets 002's FR-023 on its own (ended records and id gaps). The extract's end dates describe the real roles' history, which synthetic data needn't reproduce (EC-007).
- **Synthetic ids, not the extract's**: As for the existing types, ids are 10 × position. The People feature must therefore use these ids in its `judiciary_roles` entries (DF-4), not the real E-Links ids.
- **Deprecated names are out of scope**: The feature description excludes deprecated versions, so only `judiciary_roles` is served, as for the singular names of BaseLocation, ContractType and Gender. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The guard's fragment is `judiciary_role`**: The type-specific-code guard matches `judiciary_role` in every spelling in main Java code, with no exemptions, following 005's decision for `gender`. No main source mentions it today. The People feature, whose responses carry `judiciary_roles`, `judiciary_role_id` and `judiciary_role_name_id`, will add a reviewed exception for those fields (DF-4). Words such as "judicial" or "role" on their own are not type names.
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged. With 164 records the collection is well within the size of the largest served type (1,462 base locations).
- **Fixture location and source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 005 delivered: the generic routes, central type registry with alias resolution, per-type declarations and fixture validation at start-up, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, the fixture generation-rules test, and the automated guard against type-specific code.
