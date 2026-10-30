# Feature Specification: Reference Data API — Ticket Category Types

**Feature Branch**: `012-ticket-category-types-reference-data`

**Created**: 2026-10-30

**Status**: Implemented

**Input**: User description: "Add the `ticket_category_types` reference-data type to the generic reference-data API (feature 002), through configuration and a checked-in fixture only, with no main-code change. Deprecated versions are not to be considered. This is the last E-Links canonical name, so after this feature every one of the contract's 11 `attribute_name` values is served. Contract (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `ticket_category_types` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` (`ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` requires `id`, `updated_at`, `created_at`, `start_date` and `end_date`. Scope: serve `ticket_category_types` on `GET /api/v1/reference_data/ticket_category_types` and `/{reference_id}`. Authentication, validation, error shape, ordering and edge cases are as in 002. Record shape: the Swagger fields, plus `name` (deviation D-1), the existing reference field `jurisdiction_id` (deviation D-2) and the existing text field `integration_code` (deviation D-4), the same shape as location types: `id`, `name`, `jurisdiction_id`, `integration_code`, then the dates. Type configuration: `reference-fields: [jurisdiction_id]`; no nullable reference fields; `text-fields: [integration_code]`; no boolean fields; `unique-names: false`, since one name repeats. No new field or capability is needed, so the other ten types' responses must stay byte-for-byte unchanged (existing golden files), and the OpenAPI `ReferenceDataResponse` schema is unchanged; only the `attribute_name` enum and description gain `ticket_category_types`. The singular `ticket_category_type` is not served and returns `400`, like every other unsupported name. Ids (DR, as in 007's DR-1 and 008 to 011): records keep the extract's ids, 1 to 5 with no gaps, so that every ticket category's `type_id` resolves. Records are ordered by ascending id, and 002's date rule is applied by position in that order: there are only 5 positions, so none is ended (as for jurisdictions in 009). All have `created_at` 2024-01-15T09:00:00Z, `updated_at` 2024-06-03T10:30:00Z, `start_date` 2024-01-01 and `end_date` null. The extract's own dates (`start_date` 2023-03-30 and its own timestamps) are not reproduced. Fixture: generated once from the local extract REF_TicketCategoryType.csv: a header plus 5 rows of 8 columns (`id`, `name`, `jurisdiction_id`, `integration_code`, `start_date`, `end_date`, `created_at`, `updated_at`), quoted strings, ASCII, no BOM, no trailing newline. Take `id`, `name`, `jurisdiction_id` and `integration_code`; ignore every other column. Names are non-blank, trimmed and copied exactly. 5 records, 4 distinct names: "Court Type" appears twice, as 1 (jurisdiction 30 Courts, `COURTS_COURT_TYPE`) and 4 (jurisdiction 84 Magistrates, `MAGS_COURT_TYPE`). Every `integration_code` matches the existing format rule `[A-Z_]+` and is distinct: `COURTS_COURT_TYPE`, `TRIBS_TYPE`, `TRIBS_NAME`, `MAGS_COURT_TYPE`, `SKILLS_CATEGORY`. (Ticket category 85 is also named "Court Type"; names are unique per type only, so this doesn't matter.) References: - Closes 011 DF-10: every `ticket_categories.type_id` (1 to 5) MUST resolve to a served ticket category type, as a row `ticket_categories.type_id -> ticket_category_types` in ReferenceDataReferencesTest. `TARGET_TYPE_OVERRIDES` already maps it, so the completeness check demands the row once the type is configured. Every type is referenced: 1 by 3 categories, 2 by 3, 3 by 46, 4 by 1 and 5 by 1. - An agreement row (required by 011 DF-10): every ticket category's `jurisdiction_id` equals its type's, via `type_id` with target `ticket_category_types`, not `location_types` (0 mismatches in the extract). `type_id` isn't nullable for ticket categories, so no record is skipped. - `jurisdiction_id` (30, 34, 84, 87) refers to served jurisdictions: a row `ticket_category_types.jurisdiction_id -> jurisdictions`. `ReferenceDataFixtureRulesTest`'s `JURISDICTION_IDS` already holds all four. Skills (87) is now referenced twice, by ticket category 88 "Languages" and ticket category type 5 "Skills Category", so the statements that it is referenced only by Languages (README; JurisdictionsFunctionalTest's comment above `ac011SkillsIsServedAndUnreferencedByTheLocationData`) need updating. - This type makes no forward reference, so after this feature no served type has an outstanding deferred reference apart from those owned by the People feature (DF-2, DF-3, DF-4, DF-6, DF-9). Build checks without the extract: ReferenceDataFixtureRulesTest covers the fixture with the extract-id rule (`SOURCE`), the one declared reference field, the declared text field and its format, and reference points. `TICKET_CATEGORY_TYPE_IDS` and its `PINNED_TYPE_REFERENCE_VALUES` entry stay, as `TICKET_CATEGORY_IDS` did after 011; `TARGET_TYPE_OVERRIDES` also stays, since `type_id` still means a location type for the location data. NoTypeSpecificCodeTest: main code names no ticket category type today, and the existing `ticket_categories` fragment doesn't match `ticket_category_types`. Add the plural fragment `ticket_category_types`, as 009 to 011 did, with cases showing it matches the plural spellings (snake_case, spaced words, camelCase) but not "ticket category" or `ticket_category_id`, with no exemption. The application.yml comment on ticket_categories (which calls `type_id` "a forward reference to ticket category types, not served yet") becomes a plain statement that the reference is checked by ReferenceDataReferencesTest. Unsupported-name examples: every E-Links canonical name is now served, so no contract-valid but out-of-scope name is left. Move the examples from `ticket_category_types` to the singular `ticket_category_type` (or decide otherwise in clarify): ReferenceDataFunctionalTest's unsupported list and its missing-token path, the Postman "ticket_category_types - unsupported (contract-valid, out of scope)" request (renamed to reflect that it's no longer contract-valid) and 002's quickstart row 5. `ticket_category_type` stays in the list. The "other types don't carry this field" checks (010's ac012 for `ticket_category_id`, and 011's equivalent for `parent_category_id`) gain `ticket_category_types`. Update README and CLAUDE.md (eleven types, every E-Links name served; the ticket-category-type paragraph; DF-10 closed; Skills referenced twice). Reference points: 1 "Court Type" (first; jurisdiction 30; `COURTS_COURT_TYPE`; type of 31 Civil, 32 Crime and 33 Family), 3 "Tribunal Name" (jurisdiction 34; `TRIBS_NAME`; most used, by 46 categories), 4 "Court Type" (repeated name; jurisdiction 84; `MAGS_COURT_TYPE`; type of 85 "Court Type" only), 5 "Skills Category" (last; jurisdiction 87; `SKILLS_CATEGORY`; type of 88 Languages only). Unknown ids: 6 (just past the last), 30 (a jurisdiction id), 31 (a ticket category id), 999999. Deliver unit, integration, functional and smoke coverage; generated OpenAPI documentation (the `attribute_name` enum gains `ticket_category_types`); and Postman artifacts (collection and environment, with test scripts). Acceptance scenarios mirror the existing reference-data features."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003` to `011` added BaseLocation, ContractType, Gender, JudiciaryRole, Location, LocationType, Jurisdiction, Ticket and TicketCategory through configuration and fixtures. Along the way, types gained five declarable reference fields (`type_id`, `parent_id`, `jurisdiction_id`, `ticket_category_id`, `parent_category_id`), nullable references, boolean and text fields and repeated names. 007 to 011 kept the extract's own ids for types that other data refers to (DR-1 in each).

This feature adds TicketCategoryType, the eleventh and last of the E-Links reference-data types: the 5 kinds of ticket category, such as "Court Type" or "Tribunal Name". After it, every canonical `attribute_name` value in the E-Links contract is served. That shapes the feature:

- **No main-code change** (FR-003). A ticket category type carries a name, a jurisdiction and an integration code, the same shape as a location type (008) without its `type_id`. Every one of those fields can already be declared, so the type needs only a type entry and a fixture, as Jurisdiction did (009).
- **It closes 011's forward reference** (FR-011; DF-10). Every ticket category's `type_id` must resolve to a served ticket category type, so the ids are the extract's own (DR-1), and every category must belong to the same jurisdiction as its type (FR-012).
- **It opens none** (FR-011). The type's only reference, `jurisdiction_id`, points at the served jurisdictions, so after this feature no served type has an outstanding forward reference; the deferred items left all belong to the People feature.
- **No contract-valid name is left unsupported** (FR-021). Examples that used the last unserved canonical name as an "unsupported type" move to an unserved singular name.

Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to TicketCategoryType unchanged and is not restated here except where this feature tests it. The mock doesn't serve the deprecated alias `ticket_category_type` (FR-001; Constitution Principle VII).

## Clarifications

### Session 2026-10-30

- Q: Now that every E-Links canonical name is served, which attribute name should the "unsupported type" examples (the missing-token check, the example request collection's "unsupported" request and 002's quickstart row) use? → A: The unserved singular `ticket_category_type`; the example request is renamed so that it no longer calls the name contract-valid (FR-021).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `ticket_category_types` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. The local extract shows that real ticket-category-type data carries an id, a name, a jurisdiction id, an integration code and the shared dates. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `ticket_category_types` | Same |
| Deprecated alias | Out of scope | Not served: `ticket_category_type` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (D-1), `jurisdiction_id` (D-2) and `integration_code` (D-4) |
| Ids | E-Links' own ticket-category-type ids | Same as the extract (1 to 5); see DR-1 |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | TicketCategoryType records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of category types without names has no practical use. | Permanent, scope-justified (unchanged from 002) |
| D-2 (from 003) | TicketCategoryType records include `jurisdiction_id`, which the Swagger schema does not list; they carry no other reference field | The extract shows the real data places each category type in a jurisdiction, which is also what tells apart the two types named "Court Type". The field is optional in the generated schema and appears only on records of a type that declares it. | Permanent, scope-justified (unchanged from 003) |
| D-4 (from 008) | TicketCategoryType records include `integration_code`, which the Swagger schema does not list | The extract carries a code for each category type, as for location types; consumers use it to match categories to their own systems. The field is optional in the generated schema and appears only on records of a type that declares it. | Permanent, scope-justified (unchanged from 008) |

No new deviation applies.

### Data-Rule Deviation

| # | Deviation | Reason |
|---|-----------|--------|
| DR-1 | TicketCategoryType ids are the extract's own ids, not 002's synthetic `10 × position`, and records are numbered for the date rule by position in ascending-id order, not code-point name order | 011 DF-10 and Constitution Principle IV: TicketCategory `type_id` values are E-Links ticket-category-type ids, so they only resolve if TicketCategoryType keeps those ids. This is not a contract deviation (the contract defines no id values); it applies to this type only, as 007's to 011's DR-1 do to their types, and every other generated value follows 002's rules. |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-10 (from 011), closed | TicketCategory's `type_id` → `ticket_category_types`, and each category's `jurisdiction_id` agreeing with its type's | This feature serves `ticket_category_types`. | Closed by this feature's tests (FR-011, FR-012; AC-010, AC-011). |

This feature opens no deferred item. DF-2, DF-3, DF-4, DF-6 and DF-9 belong to the People feature and are unaffected.

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Ticket Category Types (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of ticket category types so that my system can load them exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving ticket category types can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/ticket_category_types` and confirm a `200 OK` JSON response whose `results` array holds all 5 records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket_category_types`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every TicketCategoryType record in the configured dataset (5 by default), in ascending `id` order, each with exactly the fields `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date` and `end_date`, in that order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** every record equals the Records table (values for `id`, `name`, `jurisdiction_id`, `integration_code`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** no record is ended: every record has a null `end_date` and `updated_at` 2024-06-03T10:30:00Z (EC-004).
4. **AC-004**: **Given** the default dataset, **When** the collection is retrieved, **Then** it holds 4 distinct names, and "Court Type" appears exactly twice, as records 1 and 4, with different `jurisdiction_id` values (30 and 84) and different `integration_code` values (`COURTS_COURT_TYPE` and `MAGS_COURT_TYPE`) (EC-002).
5. **AC-005**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Ticket Category Type by ID (Priority: P1)

As an integrator, I want to fetch one ticket category type by its reference identifier so that my system can resolve an id it holds — such as a ticket category's `type_id` — to its full record.

**Why this priority**: Part of the same contract as the collection route, and the way a consumer looks up the type of a ticket category it holds.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/ticket_category_types/{id}` for an id in the Records table and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/ticket_category_types/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including both "Court Type" records (`1` and `4`).
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no ticket category type (`6`, just past the last id; `30`, a jurisdiction id; `31`, a ticket category id; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `1`, which is the location type "Region" under `location_types`, or `4`, which is the location type "Chamber"), **When** it is requested under `ticket_category_types`, **Then** the result depends only on the TicketCategoryType dataset (`1` and `4` are both "Court Type"): identifiers are scoped to their type and never resolve across types (EC-006).

---

### User Story 3 - Integrator Resolves Every Ticket Category's Type (Priority: P1)

As an integrator, I want every `type_id` a ticket category carries to name a ticket category type the mock serves, in the same jurisdiction as the category, and every ticket category type's `jurisdiction_id` to name a served jurisdiction, so that my system can label every category with its type without hitting a dangling or contradictory reference.

**Why this priority**: Resolving these references is the reason TicketCategoryType is served with the extract's ids, and Constitution Principle IV requires references between served types to resolve. It closes 011 DF-10, the last forward reference between served types.

**Independent Test**: Load the TicketCategoryType, TicketCategory and Jurisdiction collections and check that every category's `type_id` names a served type with the same `jurisdiction_id`, and that every type's `jurisdiction_id` names a served jurisdiction.

**Acceptance Scenarios**:

1. **AC-010**: **Given** the default datasets, **When** the TicketCategoryType and TicketCategory collections are retrieved, **Then** every TicketCategory `type_id` (54 records, each one of 1 to 5) is the `id` of a served ticket category type, closing 011 DF-10. Every type is referenced: 1 by 3 categories, 2 by 3, 3 by 46, 4 by 1 and 5 by 1.
2. **AC-011**: **Given** the default datasets, **When** they are retrieved, **Then** every ticket category's `jurisdiction_id` equals the `jurisdiction_id` of the ticket category type its `type_id` names (54 categories compared, 0 mismatches), closing the agreement half of 011 DF-10.
3. **AC-012**: **Given** the default datasets, **When** the TicketCategoryType and Jurisdiction collections are retrieved, **Then** every TicketCategoryType `jurisdiction_id` (each one of 30, 34, 84 and 87) is the `id` of a served jurisdiction.
4. **AC-013**: **Given** the delivered feature, **When** the check that every reference between served types is tested runs, **Then** it passes and requires a resolution test for every reference field of every served type, including both new references; so after this feature no served type has an outstanding forward reference. Jurisdiction `87` ("Skills") is now referenced by both ticket category 88 ("Languages") and ticket category type 5 ("Skills Category").

---

### User Story 4 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `ticket_category_type` rejected clearly, so that my system uses the canonical name `ticket_category_types` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested.

**Independent Test**: Call `ticket_category_type` and `ticket_category_type/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-014**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket_category_type`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-015**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket_category_type/{reference_id}` for any `reference_id` (one that exists under `ticket_category_types` such as `1`, an unknown one such as `6`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-016**: **Given** a request to a `ticket_category_type` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name.

---

### User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want ticket-category-type requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each ticket-category-type route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-017**: **Given** a request with no credentials, **When** it is sent to any ticket-category-type route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-018**: **Given** a request with present but invalid credentials, **When** it is sent to any ticket-category-type route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 6 - Existing Behaviour Is Unchanged and Other Names Stay Unsupported (Priority: P2)

As an integrator already using the ten served types, I want adding ticket category types to leave those types and every unserved name behaving exactly as before.

**Why this priority**: Every feature must hold the regression boundary.

**Independent Test**: Re-run 002's to 011's acceptance tests, compare the existing golden responses byte for byte, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-019**: **Given** the delivered feature, **When** 002's to 011's acceptance scenarios are run, **Then** they all pass, with the same responses as before this feature; every existing golden response is byte-for-byte identical. Shared test lists of unsupported names may lose `ticket_category_types`; examples that used `ticket_category_types` as an unsupported type, such as the missing-token check and the example request collection's "unsupported" request, move to the unserved singular `ticket_category_type` (FR-021); the checks that other types don't carry `ticket_category_id` (010) or `parent_category_id` (011) gain `ticket_category_types`; and statements that jurisdiction 87 is referenced only by ticket category 88 are widened to include ticket category type 5.
2. **AC-020**: **Given** valid authentication, **When** each of the 10 E-Links attribute names that are still unsupported (the unserved singular names `base_location`, `contract_type`, `gender`, `judiciary_role`, `jurisdiction`, `location`, `location_type`, `ticket`, `ticket_category` and `ticket_category_type`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-021**: **Given** valid authentication, **When** the caller sends a ticket-category-type request with any query parameter (e.g. `?jurisdiction_id=30`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 7 - Maintainer Adds the Type Without Code (Priority: P2)

As a maintainer, I want ticket category types added by declaring the type and supplying its dataset alone, so that 002's extension design keeps working without type-specific code.

**Why this priority**: Keeps the codebase free of special cases, and shows the generic design covers every E-Links type.

**Independent Test**: Review the change and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-022**: **Given** the delivered change, **When** it is reviewed, **Then** it changes no main source code; every addition is the type entry, its dataset, tests, documentation and example requests.
2. **AC-023**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes, and it now also recognises the type name `ticket_category_type`, singular or plural, in every spelling, while "ticket category", `ticket_categories` and the generic field `ticket_category_id` in their spellings are not matched by the new fragment (FR-020).
3. **AC-024**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `ticket_category_types` alongside the existing supported values, so it lists all 11 canonical E-Links names and the alias `appointment_title`, and does not list `ticket_category_type`; `ReferenceDataResponse` is unchanged, without that documentation having been edited by hand.

---

### User Story 8 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for ticket category types so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every ticket-category-type request's checks pass.

**Acceptance Scenarios**:

1. **AC-025**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the ticket-category-type collection, a single record (e.g. `3`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `ticket_category_type` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values. The request that used `ticket_category_types` as an example of an unsupported type is renamed and uses `ticket_category_type` (FR-021).

---

### Edge Cases

- **EC-001 — Ids from the extract, without gaps**: Ids are 1 to 5 (DR-1). Every well-formed id outside that range is unknown (`404`), including `6` (just past the last), `30` (a jurisdiction id) and `31` (a ticket category id).
- **EC-002 — A repeated name**: "Court Type" names two records, 1 (Courts) and 4 (Magistrates), told apart by `jurisdiction_id` and `integration_code`, so the type allows repeated names. Ticket category 85 is also named "Court Type"; names are compared within a type only, so that is irrelevant here.
- **EC-003 — Name and code content**: Names are plain ASCII, 10 ("Court Type") to 15 ("Skills Category") characters, free of leading or trailing whitespace. Integration codes are distinct, upper-case ASCII letters and underscores, as for location types (008).
- **EC-004 — No ended record**: With 5 records, no position is a multiple of 7, so 002's rule ends none. 002 FR-023 is read across all shipped default data, whose other types include ended records, so no waiver is needed (as 005 and 009 established).
- **EC-005 — The extract's dates are not reproduced**: The extract starts every record on 2023-03-30, with fixed created and updated timestamps and no end dates. All are ignored; 002's position-based rule applies.
- **EC-006 — Same id, different type**: `1` to `5` exist under `location_types` and `locations` too (e.g. `1` is the location type "Region"). Identifiers are scoped to their type.
- **EC-007 — Order**: Records are ordered by ascending id (Court Type, Tribunal Type, Tribunal Name, Court Type, Skills Category), not by name, and the date rule counts positions in that order. Names are not sorted.
- **EC-008 — Case and spelling variants**: `Ticket_Category_Types`, `TICKET_CATEGORY_TYPES`, `ticket-category-types` and `ticket_category_types_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-009 — The same word, two targets**: `type_id` names a ticket category type on ticket categories but a location type on locations and base locations. The build's reference checks already distinguish the two (011 FR-020); this feature relies on that distinction and does not change it, so ticket categories' `type_id` is checked against `ticket_category_types`, never `location_types`.
- **EC-010 — Inherited edge cases**: Covered by FR-004. For ticket category types, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the TicketCategoryType reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `ticket_category_types`. The singular name `ticket_category_type` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `ticket_categories` and `ticket_category_types`: every canonical E-Links name, plus 002's alias. This supersedes 011's FR-002; every other attribute name, including `ticket_category_type`, MUST remain unsupported (`400`).
- **FR-003**: TicketCategoryType MUST be added through the generic reference-data capability alone: a type entry and its dataset. The change MUST NOT modify any main source code, and so adds no route, authentication, error-handling or type-specific logic.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to TicketCategoryType without exception. The type MUST allow repeated names (as 003 introduced), declare `jurisdiction_id` as its only reference field (not nullable) and `integration_code` as its only text field, and declare no boolean field.

**Response content**

- **FR-005**: Each TicketCategoryType record MUST contain exactly these fields, in this order: `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date` and `end_date`, in 002's formats (002 FR-011), with `jurisdiction_id` a positive integer and `integration_code` a string. No other reference, boolean or text field may appear. The records of the existing types MUST stay exactly as 002 to 011 define them.

**Data**

- **FR-006**: The TicketCategoryType dataset MUST be deterministic and supplied as a fixed, checked-in fixture. Only the public `id`, `name`, `jurisdiction_id` and `integration_code` values may be taken from the supplied ticket-category-type reference extract, each copied unchanged; every other value (dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over (EC-005).
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 5 rows in the extract, with 4 distinct names. Each name and integration code MUST be carried over exactly as the extract has it: nothing trimmed, case-folded, corrected or filtered (EC-002, EC-003).
- **FR-009**: Records MUST keep the extract's ids (DR-1) and be ordered by ascending id, numbered from position 1 to 5 in that order. Records whose position is a multiple of 7 are ended (none, for this type); every record has no end date, `updated_at` 2024-06-03T10:30:00Z, `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates and timestamps MUST NOT be used (EC-004, EC-005).
- **FR-010**: TicketCategoryType ids MUST be stable across releases of the default dataset and MUST equal the extract's, so that TicketCategory (011) and later features can refer to them.

**Referential integrity**

- **FR-011**: References between served types MUST resolve (Constitution Principle IV), verified by automated tests run on every build against the shipped datasets: every TicketCategory `type_id` MUST be the `id` of a served TicketCategoryType record, closing 011 DF-10, and every TicketCategoryType `jurisdiction_id` MUST be the `id` of a served Jurisdiction record. The existing check that every reference between served types is tested MUST cover both references, with ticket categories' `type_id` checked against `ticket_category_types` (EC-009). The service itself does not check that references resolve, as in 007 to 011.
- **FR-012**: Every TicketCategory's `jurisdiction_id` MUST equal the `jurisdiction_id` of the TicketCategoryType its `type_id` names, verified by an automated test on every build, alongside the existing agreement checks (008's location and base-location check and 011's parent-category check), which MUST keep passing unchanged. No category is skipped: `type_id` is never null.
- **FR-013**: Integration codes MUST satisfy the existing rules for the `integration_code` field (008): present in every record as a non-empty, trimmed string of upper-case letters and underscores.
- **FR-014**: Jurisdiction `87` ("Skills") is now referenced by ticket category 88 and ticket category type 5. Statements that it is referenced only by ticket category 88 MUST be updated, and the check that the location data does not refer to it (009, narrowed by 011) MUST keep passing unchanged.

**Documentation and example requests**

- **FR-015**: The generated API documentation MUST list `ticket_category_types` among the supported `attribute_name` values and MUST NOT list `ticket_category_type`, derived from the configuration rather than edited by hand. `ReferenceDataResponse` MUST be unchanged.
- **FR-016**: The example request collection MUST cover the ticket-category-type routes as AC-025 describes, using environment variables for every environment-specific value.
- **FR-017**: The project's documentation of supported reference-data types MUST list ticket category types and state that every E-Links canonical name is now served. Every statement that `ticket_category_types` is not served, or that ticket categories' `type_id` is a forward reference (including the type entry's comment for ticket categories and 011's DF-10, marked closed), MUST be updated.

**Quality**

- **FR-018**: The ticket-category-type collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-019**: Unit, integration, functional and smoke coverage MUST be extended to the new type, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009, FR-013), with the extract-id order rule that 007 introduced and the reference points of the Records table, MUST be checked by an automated test without the extract; the values are compared with the extract once, when the fixture is generated (FR-007). The build's pinned set of ticket categories' `type_id` values (1 to 5, from 011) stays, as 010's pinned `ticket_category_id` values did after 011.
- **FR-020**: The automated guard against type-specific code MUST recognise the type name in the singular fragment `ticket_category_type`, which also matches the plural, in every spelling (e.g. `ticket_category_type`, `ticket_category_types`, `TicketCategoryTypeMapper`, `ticketCategoryType`, `TICKET_CATEGORY_TYPES`, `ticket category type`), with no exemption, and MUST keep passing without any main-code change. The guard's own tests MUST show that the fragment matches those spellings and does not match "ticket category", `ticket_categories` or `ticket_category_id`. The existing fragments are unchanged.
- **FR-021**: Since every canonical E-Links name is now served, examples that need a well-formed but unsupported attribute name MUST use the unserved singular `ticket_category_type` instead of `ticket_category_types`: the shared functional test's unsupported-name list and its missing-token check, the example request collection's "unsupported" request (renamed so that it no longer calls the name contract-valid) and 002's quickstart row that requests an unsupported type (Clarifications). `ticket_category_type` stays in the list of unsupported names.

### Records

The whole default dataset, fixed by FR-006, FR-008 and FR-009 and used by tests and example requests. No record is ended: every record has a null `end_date`, `updated_at` 2024-06-03T10:30:00Z, `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01.

| Position | `id` | `name` | `jurisdiction_id` | `integration_code` | Ticket categories of this type | Note |
|----------|------|--------|-------------------|--------------------|-------------------------------|------|
| 1 | 1 | Court Type | 30 | `COURTS_COURT_TYPE` | 3 (31 Civil, 32 Crime, 33 Family) | first; repeated name |
| 2 | 2 | Tribunal Type | 34 | `TRIBS_TYPE` | 3 (35, 43, 48: the three tribunal parents) | |
| 3 | 3 | Tribunal Name | 34 | `TRIBS_NAME` | 46 (every child category) | most used |
| 4 | 4 | Court Type | 84 | `MAGS_COURT_TYPE` | 1 (85 Court Type) | repeated name |
| 5 | 5 | Skills Category | 87 | `SKILLS_CATEGORY` | 1 (88 Languages) | last; longest name; refers to Skills |

`6`, `30`, `31` and `999999` are guaranteed unknown ids.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/ticket_category_types` | `200`, 5 records |
| Unserved singular name | `GET /api/v1/reference_data/ticket_category_type` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/ticket_category_types/3` | `200`, Tribunal Name |
| Repeated name | `GET /api/v1/reference_data/ticket_category_types/4` | `200`, Court Type (jurisdiction 84) |
| Unknown id | `GET /api/v1/reference_data/ticket_category_types/6` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/ticket_category_types/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/ticket_category_types?jurisdiction_id=30` | `400`, shared error shape |
| Another unserved singular name | `GET /api/v1/reference_data/ticket_category` | `400`, shared error shape |
| Missing or invalid credentials | any ticket-category-type route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002 and extended by 003, 004, 007, 008, 010 and 011. This feature adds an eleventh instance, TicketCategoryType, with canonical name `ticket_category_types`, no served alias, repeated names allowed, `jurisdiction_id` as its only reference field and `integration_code` as its only text field. The declaration format is unchanged.
- **TicketCategoryType record**: One kind of ticket category in E-Links, within one jurisdiction (for example the court types of the Courts, or the tribunal names of the Tribunals). Attributes: `id` (unique, stable, the E-Links id), `name` (public name, exactly as in the source, not necessarily unique), `jurisdiction_id` (the jurisdiction it belongs to), `integration_code` (the E-Links code), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. TicketCategory records refer to it through their `type_id`, and each category's jurisdiction equals its type's.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 25 acceptance scenarios (AC-001 to AC-025) pass as automated tests or, for AC-022 and AC-025, as review and collection-run checks.
- **SC-002**: The default ticket-category-type collection contains exactly 5 records with 4 distinct names, none ended, matching the Records table.
- **SC-003**: 100% of the 5 served records carry the same `id`, `name`, `jurisdiction_id` and `integration_code` as the source row, character for character.
- **SC-004**: 100% of TicketCategory `type_id` values (54) and TicketCategoryType `jurisdiction_id` values (5) resolve to served records; 0 dangling references and 0 forward references remain between served types; 0 of the 54 ticket categories disagree with their type's jurisdiction.
- **SC-005**: 0 requests via the unserved singular name `ticket_category_type` return reference data; 100% with valid credentials return `400`.
- **SC-006**: 0 of the 10 still-unsupported attribute names return a `200` response.
- **SC-007**: 0 ticket-category-type requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-008**: 100% of 002's to 011's acceptance tests pass with their expected responses unchanged, and every existing golden response is byte-for-byte identical.
- **SC-009**: 0 lines of main code change.
- **SC-010**: The ticket-category-type collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-011**: An integrator using the published documentation and example requests retrieves the ticket-category-type list and the type of a given ticket category at the first attempt, without reading source code.

## Assumptions

- **Source values are public reference data**: Ticket-category-type names, ids, jurisdictions and integration codes describe public categories of judicial work, not people, so reusing them is consistent with Constitution Principle IV. Dates and timestamps are synthetic.
- **The extract's ids are required, not chosen**: Principle IV's forward-reference exception requires a referenced type to be served with the extract's ids. Renumbering the types would leave every TicketCategory `type_id` dangling, so DR-1 is the only compliant option, as for 007 to 011.
- **The extract is CSV**: A header plus 5 rows of 8 columns (`id`, `name`, `jurisdiction_id`, `integration_code`, `start_date`, `end_date`, `created_at`, `updated_at`), quoted strings, ASCII, no byte-order mark and no trailing newline. Only `id`, `name`, `jurisdiction_id` and `integration_code` are used.
- **Same date rules as 002**: The dates, timestamps and ended-record rule are 002's, counted over ascending-id positions. For 5 records the rule ends none, which matches the extract (which ends none either, though its own dates are not used).
- **The jurisdiction agreement holds in the source**: In the local extracts every ticket category's jurisdiction equals its type's (0 mismatches), so FR-012 needs no data change.
- **Deprecated names are out of scope**: The feature description excludes deprecated versions, so only `ticket_category_types` is served, as for the singular names of the other types since 003. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The unsupported-name example is the singular**: With every canonical name served, the examples use the unserved singular `ticket_category_type` (FR-021; Clarifications) rather than a name invented outside E-Links. It keeps the examples next to the type they replace and is already in the list of unsupported names.
- **The guard's fragment is the singular**: 009 to 011 used the plural only because a generic reference field (`jurisdiction_id`, `ticket_category_id`) contains the singular. No field contains `ticket_category_type`, and main code mentions the type in neither form, so the guard matches the singular, which also covers the plural (FR-020; changed after code review).
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged.
- **Fixture source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 011 delivered: the generic routes, central type registry, per-type declarations and fixture validation at start-up, declarable reference and text fields with the `integration_code` format rule, repeated names, the extract-id order rule in the fixture generation-rules test, the reference-resolution test with its completeness check and per-type target override, the jurisdiction-agreement test, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
