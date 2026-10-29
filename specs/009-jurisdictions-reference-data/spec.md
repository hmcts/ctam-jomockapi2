# Feature Specification: Reference Data API — Jurisdictions

**Feature Branch**: `009-jurisdictions-reference-data`

**Created**: 2026-10-29

**Status**: Draft

**Input**: User description: "Add the `jurisdictions` reference-data type to the generic reference-data API (feature 002), through configuration and a checked-in fixture only, with no main-code change. Deprecated versions are not to be considered. Contract (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `jurisdictions` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` (`ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` requires `id`, `updated_at`, `created_at`, `start_date` and `end_date`. Scope: serve `jurisdictions` on `GET /api/v1/reference_data/jurisdictions` and `/{reference_id}`. Authentication, validation, error shape, ordering and edge cases are as in 002. Record shape: the Swagger fields, plus `name` (deviation D-1), and nothing else: no reference, boolean or text fields, so no new deviation. Type configuration: no `reference-fields`, `boolean-fields`, `nullable-reference-fields` or `text-fields`; `unique-names` stays at its default (unique), since all 5 names are distinct. The singular `jurisdiction` is not served and returns `400`, like every other unsupported E-Links name. Ids (DR, as in 007's DR-1 and 008): records keep the extract's ids, 30, 34, 84, 86 and 87 (with gaps), not 002's 10 × position, so that every existing `jurisdiction_id` resolves (Constitution Principle IV). This is a contract-neutral deviation from 002's id rule, limited to this type. Records are ordered by ascending id, and 002's date rule is applied by position in that order. With 5 records no position is a multiple of 7, so none is ended: all have `created_at` 2024-01-15T09:00:00Z, `updated_at` 2024-06-03T10:30:00Z, `start_date` 2024-01-01 and `end_date` null. The extract's own dates (start 2023-03-30, created 2023-05-19T11:18:41Z, updated 2023-12-21T00:02:00Z, no end dates) are not reproduced. Fixture: generated once from the local extract REF_Jurisdiction.json. Unlike the earlier extracts it is JSON, not CSV: a top-level object whose `results` array holds 5 objects, each with `id`, `name`, `start_date`, `end_date`, `created_at` and `updated_at`. It is ASCII and has unique ids and no blank names. Take `id` and `name` and ignore every other key. Names are copied exactly. Records, as id name: 30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners, 87 Skills. References (this type is the target of every remaining forward reference): - MUST test, as rows in ReferenceDataReferencesTest, that every `locations.jurisdiction_id`, `base_locations.jurisdiction_id` and `location_types.jurisdiction_id` (values 30, 34, 84, 86) resolves to a served jurisdiction. This closes 003 DF-1 and 007 DF-5 for `jurisdiction_id`, and 008 DF-7. The test's completeness check already requires all three rows once the type is configured. After this feature no served type has a forward reference. - 87 "Skills" is referenced by no shipped record. That is allowed: references must resolve, not every record must be referenced. - The existing jurisdiction-agreement table (008) is unchanged. Build checks without the extract: ReferenceDataFixtureRulesTest covers the fixture with the extract-id rule (`SOURCE`) and no declared fields. NoTypeSpecificCodeTest: main code already names the generic reference field `jurisdiction_id` (ReferenceField, ReferenceDataRecord, ReferenceDataResponse, ReferenceDataProperties). A plain `jurisdiction` fragment would fail the build. Decide in clarify whether to (a) add the fragment with a reviewed exemption limited to the `jurisdiction_id` field's spellings (rewording any other main-code mention), or (b) add no fragment and record why. Remove `jurisdictions` from the unsupported-name examples (ReferenceDataFunctionalTest's unsupported list and its missing-token path, which should use another unsupported name such as `tickets`; the Postman "jurisdictions - unsupported" request). `jurisdiction` stays in the list. Update the README and CLAUDE.md text saying `jurisdictions` isn't served and that `jurisdiction_id` is a forward reference. Reference points: 30 "Courts" (first); 84 "Magistrates" (most referenced); 86 "Coroners"; 87 "Skills" (last, unreferenced). Unknown ids: 31 (a gap), 88 (just past the last), 1 (an id served by other types but not by this one) and 999. Deliver unit, integration, functional and smoke coverage; generated OpenAPI documentation (the `attribute_name` enum gains `jurisdictions`); and Postman artifacts (collection and environment, with test scripts). Acceptance scenarios mirror the existing reference-data features."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003` to `008` added BaseLocation, ContractType, Gender, JudiciaryRole, Location and LocationType through configuration and fixtures. Along the way, types gained declared reference, boolean and text fields and repeated names. 007 and 008 also kept the extract's own ids for types that other types refer to (007 DR-1, 008 DR-1).

This feature adds Jurisdiction, the eighth of the eleven E-Links reference-data types: the 5 top-level areas of judicial work (courts, tribunals, magistrates, coroners and skills). Every location, base location and location type already carries a `jurisdiction_id`, so Jurisdiction is the **target** of the last forward references in the served data. That shapes the feature:

- **Ids are the extract's own** (FR-009; DR-1). Constitution Principle IV, 003 DF-1, 007 DF-5 and 008 DF-7 require Jurisdiction to be served with the E-Links ids, so that every `jurisdiction_id` resolves. Every other value stays synthetic.
- **Every reference between served types resolves** (FR-011). Every Location, BaseLocation and LocationType `jurisdiction_id` resolves to a served jurisdiction, closing 003 DF-1, 007 DF-5 and 008 DF-7. After this feature no served type has an outstanding forward reference.
- **No main-code change** (FR-003). Records carry only the shared fields and `name`, and extract ids are already supported, so the type needs only a type entry and a fixture, as Gender did (005).

Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to Jurisdiction unchanged and is not restated here except where this feature tests it. The mock doesn't serve the deprecated alias `jurisdiction` (FR-001; Constitution Principle VII).

## Clarifications

### Session 2026-10-29

- Q: Main code already names the generic reference field `jurisdiction_id` (and describes it as "the record's jurisdiction"), so a `jurisdiction` fragment in the type-specific-code guard would fail. How should the guard cover this type? → A: With the plural fragment `jurisdictions` in every spelling and no exemption. It matches nothing in main code today, so no main-code change is needed; the singular is left uncovered because the generic field name contains it (FR-018).
- Q: Should the build gain a permanent check that no served type refers to an unserved type, or is "no forward references remain" a statement about the data after this feature? → A: A statement, not a new check. AC-010 is met by the existing completeness check, which now requires a resolution test for every reference field of every served type; later features may still add forward references recorded as deferred items (Constitution Principle IV) (FR-011).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `jurisdictions` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. The local extract shows that real jurisdiction data carries an id, a name and the shared dates, and nothing else. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `jurisdictions` | Same |
| Deprecated alias | Out of scope | Not served: `jurisdiction` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (D-1) |
| Ids | E-Links' own jurisdiction ids | Same as the extract (30, 34, 84, 86, 87); see DR-1 |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | Jurisdiction records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of jurisdictions without names has no practical use. | Permanent, scope-justified (unchanged from 002) |

No other deviation applies: Jurisdiction records carry no reference, boolean or text field.

### Data-Rule Deviation

| # | Deviation | Reason |
|---|-----------|--------|
| DR-1 | Jurisdiction ids are the extract's own ids, not 002's synthetic `10 × position`, and records are numbered for the date rule by position in ascending-id order, not code-point name order | Constitution Principle IV, 003 DF-1, 007 DF-5 and 008 DF-7: Location, BaseLocation and LocationType `jurisdiction_id` values are E-Links jurisdiction ids, so they only resolve if Jurisdiction keeps those ids. This is not a contract deviation (the contract defines no id values); it applies to this type only, as 007's and 008's DR-1 do to Location and LocationType, and every other generated value follows 002's rules. |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-1 (from 003), closed | BaseLocation's `jurisdiction_id` → `jurisdictions` (`parent_id` closed by 007, `type_id` by 008) | This feature serves `jurisdictions`. | Closed by this feature's test (FR-011, AC-009). |
| DF-5 (from 007), closed | Location's `jurisdiction_id` → `jurisdictions` (`type_id` closed by 008) | This feature serves `jurisdictions`. | Closed by this feature's test (FR-011, AC-009). |
| DF-7 (from 008), closed | LocationType's `jurisdiction_id` → `jurisdictions` | This feature serves `jurisdictions`. | Closed by this feature's test (FR-011, AC-009). |

This feature opens no deferred item. DF-2, DF-3, DF-4 and DF-6 belong to the People feature and are unaffected.

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Jurisdictions (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of jurisdictions so that my system can load them exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving jurisdictions can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/jurisdictions` and confirm a `200 OK` JSON response whose `results` array holds all 5 jurisdiction records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/jurisdictions`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every Jurisdiction record in the configured dataset (5 by default), in ascending `id` order, each with exactly the fields `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`, in that order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** every record equals the Records table (values for `id`, `name`, `end_date` and `updated_at`).
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** no record is ended: every record has a null `end_date` and `updated_at` 2024-06-03T10:30:00Z (EC-004).
4. **AC-004**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Jurisdiction by ID (Priority: P1)

As an integrator, I want to fetch one jurisdiction by its reference identifier so that my system can resolve an id it holds — such as a location's `jurisdiction_id` — to its full record.

**Why this priority**: Part of the same contract as the collection route, and the way a consumer looks up the jurisdiction of a location or location type it holds.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/jurisdictions/{id}` for an id in the Records table and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-005**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/jurisdictions/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including the unreferenced record `87` ("Skills").
2. **AC-006**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no jurisdiction (`31`, in a gap; `88`, just past the last id; `1`, below the first id; `999`; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-007**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-008**: **Given** an `id` that exists in another type's dataset (e.g. `84`, which is the location "Central Devon Sub Committee" under `locations`, or `30`, which is a record under every 10 × position type, such as "Other" under `genders`), **When** it is requested under `jurisdictions`, **Then** the result depends only on the Jurisdiction dataset (`84` is "Magistrates", `30` is "Courts"): identifiers are scoped to their type and never resolve across types (EC-006).

---

### User Story 3 - Integrator Resolves Every Jurisdiction Reference (Priority: P1)

As an integrator, I want every `jurisdiction_id` a location, base location or location type carries to name a jurisdiction the mock serves, so that my system can label every location and location type with its jurisdiction without hitting a dangling reference.

**Why this priority**: Resolving these references is the reason Jurisdiction is served with the extract's ids, and Constitution Principle IV requires references between served types to resolve. It closes the last open forward references: 003 DF-1, 007 DF-5 and 008 DF-7.

**Independent Test**: Load the Jurisdiction, Location, BaseLocation and LocationType collections and check that every `jurisdiction_id` names a served jurisdiction.

**Acceptance Scenarios**:

1. **AC-009**: **Given** the default datasets, **When** the Jurisdiction, Location, BaseLocation and LocationType collections are retrieved, **Then** every Location `jurisdiction_id` (2,000 records), every BaseLocation `jurisdiction_id` (1,462 records) and every LocationType `jurisdiction_id` (16 records), each one of 30, 34, 84 and 86, is the `id` of a served jurisdiction, closing 003 DF-1, 007 DF-5 and 008 DF-7.
2. **AC-010**: **Given** the delivered feature, **When** the check that every reference between served types is tested runs, **Then** it passes and requires a resolution test for every reference field of every served type, since every target is now served; so after this feature no served type has an outstanding forward reference. No new check is added: later features may still introduce forward references recorded as deferred items (Clarifications).
3. **AC-011**: **Given** the default datasets, **When** they are retrieved, **Then** jurisdiction `87` ("Skills") is referenced by no Location, BaseLocation or LocationType record and is still served: references must resolve, but a record need not be referenced (EC-003).

---

### User Story 4 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `jurisdiction` rejected clearly, so that my system uses the canonical name `jurisdictions` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested.

**Independent Test**: Call `jurisdiction` and `jurisdiction/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-012**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/jurisdiction`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-013**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/jurisdiction/{reference_id}` for any `reference_id` (one that exists under `jurisdictions` such as `30`, an unknown one such as `31`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-014**: **Given** a request to a `jurisdiction` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name.

---

### User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want jurisdiction requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each jurisdiction route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-015**: **Given** a request with no credentials, **When** it is sent to any jurisdiction route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-016**: **Given** a request with present but invalid credentials, **When** it is sent to any jurisdiction route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using the seven served types, I want adding jurisdictions to leave those types and every still-unbuilt type behaving exactly as before.

**Why this priority**: Every feature must hold the regression boundary.

**Independent Test**: Re-run 002's to 008's acceptance tests, compare the existing golden responses byte for byte, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-017**: **Given** the delivered feature, **When** 002's to 008's acceptance scenarios are run, **Then** they all pass, with the same assertions and the same responses as before this feature. Shared test lists of unsupported names may lose `jurisdictions`, and examples that used `jurisdictions` as an unsupported type, such as the missing-token check and the example request collection's "unsupported" request, move to another unsupported name; nothing else asserted changes.
2. **AC-018**: **Given** valid authentication, **When** each of the 13 E-Links attribute names that are still unsupported (the three canonical types not yet served: `ticket_categories`, `ticket_category_types` and `tickets`; their three singular forms; and the unserved singular names `base_location`, `contract_type`, `gender`, `judiciary_role`, `location`, `location_type` and `jurisdiction`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-019**: **Given** valid authentication, **When** the caller sends a jurisdiction request with any query parameter (e.g. `?name=Courts`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 7 - Maintainer Adds the Type Without Code (Priority: P2)

As a maintainer, I want jurisdictions added by declaring the type and supplying its dataset alone, so that 002's extension design keeps working without type-specific code.

**Why this priority**: Keeps the remaining types cheap to add and the codebase free of special cases.

**Independent Test**: Review the change and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-020**: **Given** the delivered change, **When** it is reviewed, **Then** it changes no main source code; every addition is the type entry, its dataset, tests, documentation and example requests.
2. **AC-021**: **Given** the delivered change, **When** the automated guard against type-specific code runs, **Then** it passes, and it now also recognises the plural type name `jurisdictions` in every spelling, while the generic field `jurisdiction_id` in its spellings is not matched (FR-018).
3. **AC-022**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `jurisdictions` alongside the existing supported values and does not list `jurisdiction`, and `ReferenceDataResponse` is unchanged, without that documentation having been edited by hand.

---

### User Story 8 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for jurisdictions so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every jurisdiction request's checks pass.

**Acceptance Scenarios**:

1. **AC-023**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the jurisdiction collection, a single record (e.g. `84`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `jurisdiction` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values. The request that used `jurisdictions` as an example of an unsupported type uses another unsupported name.

---

### Edge Cases

- **EC-001 — Ids from the extract, with gaps**: Ids are 30, 34, 84, 86 and 87 (DR-1). Every well-formed id outside that set is unknown (`404`), including `1` (below the first), `31` (in a gap) and `88` (just past the last).
- **EC-002 — Name content**: Names are distinct plain-ASCII single words of 6 ("Skills") to 11 ("Magistrates") characters, free of leading or trailing whitespace, so the default unique-names rule holds.
- **EC-003 — An unreferenced jurisdiction**: "Skills" (87) is in the extract but no shipped location, base location or location type refers to it. It is served like every other record; nothing requires a record to be referenced.
- **EC-004 — No ended record**: With 5 records, no position is a multiple of 7, so 002's rule ends none. 002 FR-023 is read across all shipped default data, whose other types include ended records and id gaps, so no waiver is needed (as 005 established).
- **EC-005 — The extract's dates are not reproduced**: The extract starts every record on 2023-03-30, with fixed created and updated timestamps and no end dates. All are ignored; 002's position-based rule applies.
- **EC-006 — Same id, different type**: `30` exists under every type numbered 10 × position (e.g. "Other" under `genders`), and `84` and `86` exist under `locations`. Identifiers are scoped to their type.
- **EC-007 — Order**: Records are ordered by ascending id (Courts, Tribunals, Magistrates, Coroners, Skills), not by name (which would put Coroners first), and the date rule counts positions in that order. Names are not sorted.
- **EC-008 — Case and spelling variants**: `Jurisdictions`, `JURISDICTIONS` and `jurisdictions_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-009 — Inherited edge cases**: Covered by FR-004. For jurisdictions, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the Jurisdiction reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `jurisdictions`. The singular name `jurisdiction` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types` and `jurisdictions`. This supersedes 008's FR-002; every other E-Links attribute name, including `jurisdiction`, MUST remain unsupported (`400`).
- **FR-003**: Jurisdiction MUST be added through the generic reference-data capability alone: a type entry and its dataset. The change MUST NOT modify any main source code, and so adds no route, authentication, error-handling or type-specific logic.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to Jurisdiction without exception. Names MUST be unique within the type; the type declares no reference, boolean, nullable-reference or text fields.

**Response content**

- **FR-005**: Each Jurisdiction record MUST contain exactly these fields, in this order: `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`, in 002's formats (002 FR-011). No reference, boolean or text field may appear. The records of the existing types MUST stay exactly as 002 to 008 define them.

**Data**

- **FR-006**: The Jurisdiction dataset MUST be deterministic and supplied as a fixed, checked-in fixture. Only the public `id` and `name` values may be taken from the supplied jurisdiction reference extract, each copied unchanged; every other value (dates and timestamps) MUST be synthetic, and no other key of the extract may be carried over (EC-005).
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 5 entries in the extract, with 5 distinct names. Each name MUST be carried over exactly as the extract has it: nothing trimmed, case-folded, corrected or filtered (EC-002).
- **FR-009**: Records MUST keep the extract's ids (DR-1) and be ordered by ascending id, numbered from position 1 to 5 in that order. Records whose position is a multiple of 7 are ended (none, for this type); every record has no end date, `updated_at` 2024-06-03T10:30:00Z, `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates and timestamps MUST NOT be used (EC-004, EC-005).
- **FR-010**: Jurisdiction ids MUST be stable across releases of the default dataset and MUST equal the extract's, so that Location (007), BaseLocation (003), LocationType (008) and later features can refer to them.

**Referential integrity**

- **FR-011**: References between served types MUST resolve (Constitution Principle IV), verified by automated tests run on every build against the shipped datasets: every Location, BaseLocation and LocationType `jurisdiction_id` MUST be the `id` of a served Jurisdiction record, closing 003 DF-1, 007 DF-5 and 008 DF-7. The existing check that every reference between served types is tested MUST cover all three references; no check forbidding future forward references is added (Clarifications). The service itself does not check that references resolve, as in 007 and 008.
- **FR-012**: The existing check that every Location's and BaseLocation's `jurisdiction_id` equals its LocationType's (008 FR-012) MUST keep passing unchanged.

**Documentation and example requests**

- **FR-013**: The generated API documentation MUST list `jurisdictions` among the supported `attribute_name` values and MUST NOT list `jurisdiction`, derived from the configuration rather than edited by hand. `ReferenceDataResponse` MUST be unchanged.
- **FR-014**: The example request collection MUST cover the jurisdiction routes as AC-023 describes, using environment variables for every environment-specific value.
- **FR-015**: The project's documentation of supported reference-data types MUST list jurisdictions, and every statement that `jurisdictions` is not served or that `jurisdiction_id` is a forward reference (including the descriptions of Location's, BaseLocation's and LocationType's `jurisdiction_id`) MUST be updated.

**Quality**

- **FR-016**: The jurisdiction collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target.
- **FR-017**: Unit, integration, functional and smoke coverage MUST be extended to the new type, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009), with the extract-id order rule that 007 introduced, MUST be checked by an automated test without the extract; the values are compared with the extract once, when the fixture is generated (FR-007).
- **FR-018**: The automated guard against type-specific code MUST recognise the plural type name `jurisdictions` in every spelling (snake_case, spaced words, camelCase, upper case, e.g. `jurisdictions`, `Jurisdictions`, `JURISDICTIONS`, `myJurisdictions`), with no exemption, and MUST keep passing without any main-code change (Clarifications). It does not cover the singular `jurisdiction`, which the generic reference field `jurisdiction_id` and its description legitimately contain. The guard's own tests MUST show that the fragment matches those plural spellings and does not match `jurisdiction_id`, `jurisdictionId` or `JURISDICTION_ID`. Test lists that use `jurisdictions` as an example of an unsupported name MUST drop it.

### Records

The whole default dataset, fixed by FR-006, FR-008 and FR-009 and used by tests and example requests. No record is ended: every record has a null `end_date`, `updated_at` 2024-06-03T10:30:00Z, `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01.

| Position | `id` | `name` | Referenced by (locations / base locations / location types) | Note |
|----------|------|--------|-------------------------------------------------------------|------|
| 1 | 30 | Courts | 380 / 368 / 2 | first |
| 2 | 34 | Tribunals | 221 / 169 / 3 | |
| 3 | 84 | Magistrates | 1,207 / 826 / 8 | most referenced; longest name |
| 4 | 86 | Coroners | 192 / 99 / 3 | |
| 5 | 87 | Skills | 0 / 0 / 0 | last; unreferenced; shortest name |

`1`, `31`, `88`, `999` and `999999` are guaranteed unknown ids.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/jurisdictions` | `200`, 5 records |
| Unserved singular name | `GET /api/v1/reference_data/jurisdiction` | `400`, shared error shape |
| Single record | `GET /api/v1/reference_data/jurisdictions/84` | `200`, Magistrates |
| Unreferenced record | `GET /api/v1/reference_data/jurisdictions/87` | `200`, Skills |
| Unknown id | `GET /api/v1/reference_data/jurisdictions/31` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/jurisdictions/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/jurisdictions?name=Courts` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/tickets` | `400`, shared error shape |
| Missing or invalid credentials | any jurisdiction route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002 and extended by 003, 004, 007 and 008. This feature adds an eighth instance, Jurisdiction, with canonical name `jurisdictions`, no served alias, unique names and no declared reference, boolean or text fields. The declaration format is unchanged.
- **Jurisdiction record**: One top-level area of judicial work in E-Links (courts, tribunals, magistrates, coroners or skills). Attributes: `id` (unique, stable, the E-Links id), `name` (public name, exactly as in the source, unique), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. Location, BaseLocation and LocationType records refer to it through their `jurisdiction_id`; it refers to no other type.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 23 acceptance scenarios (AC-001 to AC-023) pass as automated tests or, for AC-020 and AC-023, as review and collection-run checks.
- **SC-002**: The default jurisdiction collection contains exactly 5 records with 5 distinct names, none ended, matching the Records table.
- **SC-003**: 100% of the 5 served records carry the same `id` and `name` as the source entry, character for character.
- **SC-004**: 100% of `jurisdiction_id` values, in Location (2,000), BaseLocation (1,462) and LocationType (16) records, resolve to a served jurisdiction; 0 dangling references remain between served types.
- **SC-005**: 0 requests via the unserved singular name `jurisdiction` return reference data; 100% with valid credentials return `400`.
- **SC-006**: 0 of the 13 still-unsupported attribute names return a `200` response.
- **SC-007**: 0 jurisdiction requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-008**: 100% of 002's to 008's acceptance tests pass with their assertions and expected responses unchanged, and every existing golden response is byte-for-byte identical.
- **SC-009**: 0 lines of main code change.
- **SC-010**: The jurisdiction collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-011**: An integrator using the published documentation and example requests retrieves the jurisdiction list and the jurisdiction of a given location at the first attempt, without reading source code.

## Assumptions

- **Source values are public reference data**: Jurisdiction names and ids describe the public areas of judicial work, not people, so reusing them is consistent with Constitution Principle IV. Dates and timestamps are synthetic.
- **The extract's ids are required, not chosen**: Principle IV's forward-reference exception requires a referenced type to be served with the extract's ids. Renumbering jurisdictions would leave every Location, BaseLocation and LocationType `jurisdiction_id` dangling, so DR-1 is the only compliant option, as for Location (007) and LocationType (008).
- **The extract is JSON**: Unlike the earlier CSV extracts, the jurisdiction extract is a JSON object whose `results` array holds 5 entries with `id`, `name` and the shared dates; it is ASCII with no byte-order mark. Only `id` and `name` are used. The format affects only the one-off generation, not the served data.
- **Same date rules as 002**: The dates, timestamps and ended-record rule are 002's, counted over ascending-id positions. For 5 records the rule ends none, which matches the extract (which ends none either, though its own dates are not used).
- **An unreferenced record is fine**: "Skills" (87) is served although no shipped record refers to it. Principle IV requires references to resolve, not every record to be referenced.
- **Deprecated names are out of scope**: The feature description excludes deprecated versions, so only `jurisdictions` is served, as for the singular names of the other types since 003. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The guard's fragment is the plural**: Unlike the earlier types, whose fragments are the singular name, the guard matches `jurisdictions` (Clarifications). The singular `jurisdiction` is part of the generic reference field `jurisdiction_id`, which 003 added and every type may declare, so it is not type-specific code. Main code has no mention of the plural today.
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged.
- **Fixture source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 008 delivered: the generic routes, central type registry, per-type declarations and fixture validation at start-up, the extract-id order rule in the fixture generation-rules test, the reference-resolution test with its completeness check, the jurisdiction-agreement test, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
