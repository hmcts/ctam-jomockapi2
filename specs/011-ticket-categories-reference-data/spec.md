# Feature Specification: Reference Data API — Ticket Categories

**Feature Branch**: `011-ticket-categories-reference-data`

**Created**: 2026-10-30

**Status**: Implemented

**Input**: User description: "Add the `ticket_categories` reference-data type to the generic reference-data API (feature 002), through configuration, a checked-in fixture and one generic capability: a fifth reference field, `parent_category_id`. Deprecated versions are not to be considered. Contract (E-Links Swagger v5, "Reference Data"): `GET /api/v5/reference_data/{attribute_name}` and `/{reference_id}`; `ticket_categories` is one of the 11 `attribute_name` values; `reference_id` is a number; responses `200` (`ReferenceDataApiResponse` / `ReferenceDataResponse`) and `401` ("Unauthorized. Invalid or missing token."); `ReferenceDataResponse` requires `id`, `updated_at`, `created_at`, `start_date` and `end_date`. Scope: serve `ticket_categories` on `GET /api/v1/reference_data/ticket_categories` and `/{reference_id}`. Authentication, validation, error shape, ordering and edge cases are as in 002. Record shape: the Swagger fields, plus `name` (deviation D-1), the existing reference fields `type_id` and `jurisdiction_id` (deviation D-2, as for base locations, locations and location types) and the new reference field `parent_category_id` (new deviation D-6: the extract carries it, the Swagger schema does not list it), written after `name` in reference-field order: `id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, then the dates. Type configuration: `reference-fields: [type_id, jurisdiction_id, parent_category_id]`; `nullable-reference-fields: [parent_category_id]` (8 top-level categories have none, served as `"parent_category_id": null`, as 007 does for `parent_id`); no boolean or text fields; `unique-names: false`, since one name repeats. The singular `ticket_category` is not served and returns `400`, like every other unsupported E-Links name. Generic capability (main-code change, test-first, as 010 did for `ticket_category_id`): `parent_category_id` becomes a fifth value of the `ReferenceField` enum, carried by `ReferenceDataRecord` and `ReferenceDataResponse` as a `ReferenceFieldValue`, declared and validated like the others (including nullable), and written only for types that declare it. The other nine types' responses must stay byte-for-byte unchanged (existing golden files), and the OpenAPI `ReferenceDataResponse` schema gains `parent_category_id` as an optional, nullable integer citing D-6. Ids (DR, as in 007's DR-1 and 008 to 010): records keep the extract's ids, 31 to 88 with gaps (34, 84, 86 and 87 are missing), not 002's 10 × position, so that every ticket's `ticket_category_id` and every category's `parent_category_id` resolves. Records are ordered by ascending id, and 002's date rule is applied by position in that order: positions 7, 14, …, 49 are ended (7 records: ids 38, 45, 52, 59, 66, 73, 80), with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; the rest have `end_date` null and `updated_at` 2024-06-03T10:30:00Z; all have `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates (one ended row, 81 "Transport Tribunal", and its own timestamps) are not reproduced. Fixture: generated once from the local extract REF_TicketCategory.csv: a header plus 54 rows of 9 columns (`id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, `start_date`, `end_date`, `created_at`, `updated_at`), quoted strings, ASCII, no BOM, no trailing newline. Ids are unique and ascending; names are non-blank, trimmed and copied exactly (including `&`, `'`, `,`, `-` and parentheses, e.g. "Employment Tribunal (England & Wales)", "Health Medicine's Tribunal"). Take `id`, `name`, `type_id`, `jurisdiction_id` and `parent_category_id` (blank means null); ignore every other column. 54 records, 53 distinct names: "Immigration and Asylum Chamber" appears twice, as 38 (under 35 "First tier Tribunal") and 45 (under 43 "Upper Tier"). By the synthetic rule both are ended (positions 7 and 14). The hierarchy: 8 top-level categories (31 Civil, 32 Crime, 33 Family, 35 First tier Tribunal, 43 Upper Tier, 48 Other Tribunal, 85 Court Type, 88 Languages); the other 46 have a parent among 35 (7 children), 43 (4) and 48 (35), so the depth is at most 1. Every category's `jurisdiction_id` equals its parent's. References: - Closes 010 DF-8: every `tickets.ticket_category_id` (41 distinct values) MUST resolve to a served ticket category, as a row in ReferenceDataReferencesTest; the completeness check already demands it once the type is configured. 13 categories are referenced by no ticket (including the three tribunal parents); that is allowed. - `parent_category_id` is a self-reference: a row `ticket_categories.parent_category_id -> ticket_categories`, run by the resolution and cycle tests (as `locations.parent_id`). - `jurisdiction_id` (30, 34, 84, 87) refers to served jurisdictions: a row `ticket_categories.jurisdiction_id -> jurisdictions`. This makes 87 "Skills" referenced for the first time (by 88 "Languages"), so `ReferenceDataFixtureRulesTest`'s `JURISDICTION_IDS` gains 87, and the statements that Skills is unreferenced (README, 009's EC-003 and JurisdictionsFunctionalTest's `ac011SkillsIsServedButUnreferenced`, which checks only the location data) need updating. - An agreement row: every ticket category's `jurisdiction_id` equals its parent's (0 mismatches in the extract). The agreement test currently requires every `via` value to be present, so it must skip top-level records with a null `parent_category_id`. - `type_id` (1 to 5) refers to `ticket_category_types`, which is not served yet: a forward reference (Constitution Principle IV), recorded as new deferred item DF-10. The feature that serves `ticket_category_types` MUST keep the extract's ids and MUST add the resolution row, plus an agreement row (every category's `jurisdiction_id` equals its type's; 0 mismatches in the extract). The generating script MUST confirm, once, that every `type_id` is an id in the local REF_TicketCategoryType.csv (5 types). - Problem to resolve in clarify: ReferenceDataReferencesTest's `TARGET_TYPES` maps a reference field to one target type, and `type_id` already maps to `location_types`. For ticket categories `type_id` means a ticket category type, not a location type, and the values 1 to 5 happen to be location-type ids too, so the check would wrongly demand, and wrongly pass, a `ticket_categories.type_id -> location_types` row. Decide whether to (a) make the target per type and field in the test (e.g. `(ticket_categories, type_id) -> ticket_category_types`, with `type_id -> location_types` as the default), test code only; (b) declare each reference field's target type in the type's configuration, a main-code change; or (c) another approach. Build checks without the extract: ReferenceDataFixtureRulesTest covers the fixture with the extract-id rule (`SOURCE`), the three declared reference fields (one nullable) and reference points. It should also pin this type's forward-referenced `type_id` values (1 to 5) until DF-10 closes, as 010 pinned `ticket_category_id`; but `PINNED_REFERENCE_VALUES` is keyed by field alone, and `type_id` is shared with the location data, so how to pin it follows from the `TARGET_TYPES` decision above. `TICKET_CATEGORY_IDS` stays, as `JURISDICTION_IDS` did after 009. NoTypeSpecificCodeTest: main code already names `ticket_category_id` and will name `parent_category_id`, so a `ticket_category` fragment would fail the build. Use the plural fragment `ticket_categories`, as 009 and 010 did, with cases showing it matches the plural spellings but not `ticket_category_id`, `ticketCategoryId` or "the record's ticket category" (or decide otherwise in clarify). The existing `tickets` fragment does not match `ticket_categories`. Remove `ticket_categories` from the unsupported-name examples and move them to the last still-unsupported canonical name, `ticket_category_types`: ReferenceDataFunctionalTest's unsupported list and its missing-token path, the Postman "ticket_categories - unsupported" request, and 002's quickstart row 5. `ticket_category` stays in the list. Update README and CLAUDE.md (ten types; five reference fields; the ticket-category paragraph; DF-8 closed; Skills now referenced; the new forward reference). Reference points: 31 "Civil" (first; top-level; type 1; jurisdiction 30; most used by tickets, 30), 35 "First tier Tribunal" (top-level; unreferenced by tickets; parent of 7), 38 and 45 "Immigration and Asylum Chamber" (repeated name; parents 35 and 43; both ended by the synthetic rule), 48 "Other Tribunal" (top-level; parent of 35 categories), 56 "Employment Tribunal (England & Wales)" (child of 48; `&` and parentheses), 81 "Transport Tribunal" (ended in the extract but open here; unreferenced by tickets), 85 "Court Type" (top-level; type 4; jurisdiction 84), 88 "Languages" (last; top-level; type 5; jurisdiction 87, the only reference to Skills). Unknown ids: 1, 30 (just before the first), 34 (a gap), 84 (a gap; a jurisdiction id), 89 (just past the last) and 999999. Deliver unit, integration, functional and smoke coverage; generated OpenAPI documentation (the `attribute_name` enum gains `ticket_categories`; `ReferenceDataResponse` gains `parent_category_id`); and Postman artifacts (collection and environment, with test scripts). Acceptance scenarios mirror the existing reference-data features."

## Context

Feature `002-reference-data-api` built one generic reference-data capability and served AppointmentTitle (`appointment_titles`) through it. Features `003` to `010` added BaseLocation, ContractType, Gender, JudiciaryRole, Location, LocationType, Jurisdiction and Ticket through configuration and fixtures. Along the way, types gained four declarable reference fields (`type_id`, `parent_id`, `jurisdiction_id`, `ticket_category_id`), nullable references, boolean and text fields and repeated names. 007 to 010 kept the extract's own ids for types that other data refers to (DR-1 in each).

This feature adds TicketCategory, the tenth of the eleven E-Links reference-data types: the 54 categories that tickets are grouped into, such as "Civil", "General Regulatory Chamber" or "Languages". Categories form a two-level hierarchy, and each belongs to a category type and a jurisdiction. That shapes the feature:

- **One generic change** (FR-003, FR-019). Each category may name a parent category, `parent_category_id`, null for the 8 top-level categories. The mock's reference fields are a fixed, known set, so serving it needs a fifth reference field that any type can declare, as 010 added `ticket_category_id`.
- **It closes 010's forward reference** (FR-011; DF-8). Every ticket's `ticket_category_id` must resolve to a served category, so the ids are the extract's own (DR-1).
- **It opens one** (FR-012; DF-10). `type_id` refers to ticket category types, which are not served yet. The build's reference checks currently assume `type_id` always means a location type, so they gain a per-type override for that target, in test code only (FR-020; Clarifications).
- **Jurisdiction "Skills" becomes referenced** (FR-013). Category 88 "Languages" belongs to jurisdiction 87, which 009 served as unreferenced; the statements and checks that say so are updated.

Everything 002 specified for a supported type — routes, authentication, validation, error shape, ordering, determinism and edge cases EC-001 to EC-013 — applies to TicketCategory unchanged and is not restated here except where this feature tests it. The mock doesn't serve the deprecated alias `ticket_category` (FR-001; Constitution Principle VII).

## Clarifications

### Session 2026-10-30

- Q: Where should the target type of a reference field be defined, given that `type_id` now means a ticket category type for ticket categories and a location type for locations? → A: In the build's reference checks only: each field keeps its usual target as the default (`type_id` → `location_types`), with a per-type override (`ticket_categories.type_id` → `ticket_category_types`), applied to both the reference-resolution check and the pinned reference values. No main-code or configuration change (FR-020).

## Behavioural Reference

The supplied E-Links Swagger (`swagger-ui-elinks-api-v5.pdf`, "Reference Data" section) lists `ticket_categories` among the 11 canonical `attribute_name` values and defines one generic `ReferenceDataResponse` shape for every type. The local extract shows that real ticket-category data carries an id, a name, a type id, a jurisdiction id, an optional parent category id and the shared dates. Deprecated attribute names are out of scope for this feature (the feature description excludes them).

| Aspect | E-Links reference behaviour | This mock |
|--------|-----------------------------|-----------|
| Canonical name | `ticket_categories` | Same |
| Deprecated alias | Out of scope | Not served: `ticket_category` is rejected with `400` like any unsupported name (FR-001; Constitution Principle VII) |
| Collection 200 body | `{ "results": [ ReferenceDataResponse, ... ] }` | Same |
| Single-record 200 body | One `ReferenceDataResponse` object | Same |
| `ReferenceDataResponse` fields | `id`, `updated_at`, `created_at`, `start_date`, `end_date` | Same, plus `name` (D-1), `type_id` and `jurisdiction_id` (D-2) and `parent_category_id` (D-6) |
| Ids | E-Links' own ticket-category ids | Same as the extract (31 to 88, with gaps); see DR-1 |
| 401 | "Unauthorized. Invalid or missing token." | Same |

### Contract Deviations

| # | Deviation | Reason | Type |
|---|-----------|--------|------|
| D-1 (from 002) | TicketCategory records include `name`, which the Swagger `ReferenceDataResponse` schema does not list | As for the existing types: the real data carries a name, and a list of categories without names has no practical use. | Permanent, scope-justified (unchanged from 002) |
| D-2 (from 003) | TicketCategory records include `type_id` and `jurisdiction_id`, which the Swagger schema does not list; they carry neither `parent_id` nor `ticket_category_id` | The extract shows the real data carries a category type and a jurisdiction for each category. The fields are optional in the generated schema and appear only on records of a type that declares them. | Permanent, scope-justified (unchanged from 003) |
| D-6 | TicketCategory records include `parent_category_id`, which the Swagger `ReferenceDataResponse` schema does not list; it is `null` for a top-level category | The extract shows the real data places categories in a hierarchy (for example "Immigration and Asylum Chamber" under both "First tier Tribunal" and "Upper Tier"), which is also what tells apart the two categories that share a name. The field is optional and nullable in the generated schema and appears only on records of a type that declares it (FR-019), so the other types' responses are unchanged. | Permanent, scope-justified |

### Data-Rule Deviation

| # | Deviation | Reason |
|---|-----------|--------|
| DR-1 | TicketCategory ids are the extract's own ids, not 002's synthetic `10 × position`, and records are numbered for the date rule by position in ascending-id order, not code-point name order | 010 DF-8 and Constitution Principle IV: Ticket `ticket_category_id` values and TicketCategory `parent_category_id` values are E-Links ticket-category ids, so they only resolve if TicketCategory keeps those ids. This is not a contract deviation (the contract defines no id values); it applies to this type only, as 007's to 010's DR-1 do to their types, and every other generated value follows 002's rules. |

### Deferred Items

| # | Item | Why deferred | Path to compliance |
|---|------|--------------|--------------------|
| DF-8 (from 010), closed | Ticket's `ticket_category_id` → `ticket_categories` | This feature serves `ticket_categories`. | Closed by this feature's test (FR-011, AC-010). |
| DF-10 | TicketCategory's `type_id` (1 to 5) refers to `ticket_category_types`: a forward reference (Constitution Principle IV) | `ticket_category_types` is not served yet. In the local extracts every value is an id in the ticket-category-type extract, so the references will resolve once that type is served with the extract's ids. | The feature that serves `ticket_category_types` MUST serve it with the extract's ids and MUST add a test that every TicketCategory `type_id` resolves to a served record, and a check that every category's `jurisdiction_id` equals its type's (0 mismatches in the extracts); it is incomplete until both pass. Closed by 012 ([012 FR-011, FR-012](../012-ticket-category-types-reference-data/spec.md); `ReferenceDataReferencesTest`). |

DF-2 to DF-4, DF-6 and DF-9 belong to the People feature and are unaffected.

## User Scenarios & Testing *(mandatory)*

*Acceptance scenario and edge case IDs below (`AC-`/`EC-`) are stable identifiers for plan.md, tasks.md, contracts/ and quickstart.md. They are numbered independently of the other features' IDs.*

### User Story 1 - Integrator Retrieves All Ticket Categories (Priority: P1)

As a developer or automated test suite integrating against the E-Links mock, I want to retrieve the complete list of ticket categories so that my system can load them exactly as it would against the real E-Links API.

**Why this priority**: The collection route is how consumers load a reference type, usually once, to fill a lookup. Without it nothing involving ticket categories can be exercised. It is this feature's MVP.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/ticket_categories` and confirm a `200 OK` JSON response whose `results` array holds all 54 records in ascending `id` order, identical on repeated calls.

**Acceptance Scenarios**:

1. **AC-001**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket_categories`, **Then** the response is `200 OK`, `application/json`, with a body `{ "results": [ ... ] }` containing every TicketCategory record in the configured dataset (54 by default), in ascending `id` order, each with exactly the fields `id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, `created_at`, `updated_at`, `start_date` and `end_date`, in that order.
2. **AC-002**: **Given** the default dataset, **When** the collection is retrieved, **Then** every reference point in the Reference Points table has the listed `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, `end_date` and `updated_at`.
3. **AC-003**: **Given** the default dataset, **When** the collection is retrieved, **Then** exactly the 7 records at positions 7, 14, …, 49 are ended (ids 38, 45, 52, 59, 66, 73 and 80), each with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; the other 47 have a null `end_date` and `updated_at` 2024-06-03T10:30:00Z.
4. **AC-004**: **Given** the default dataset, **When** the collection is retrieved, **Then** it holds 53 distinct names, and "Immigration and Asylum Chamber" appears exactly twice, as records 38 and 45, with different `parent_category_id` values (35 and 43) (EC-002).
5. **AC-005**: **Given** the same configured dataset, **When** the collection route is called repeatedly, including across a service restart, **Then** every response is equal in content and record order.

---

### User Story 2 - Integrator Retrieves a Single Ticket Category by ID (Priority: P1)

As an integrator, I want to fetch one ticket category by its reference identifier so that my system can resolve an id it holds — such as a ticket's `ticket_category_id` — to its full record.

**Why this priority**: Part of the same contract as the collection route, and the way a consumer looks up the category of a ticket it holds.

**Independent Test**: With valid credentials, call `GET /api/v1/reference_data/ticket_categories/{id}` for a reference point and confirm a `200 OK` single-object response equal to that record in the collection.

**Acceptance Scenarios**:

1. **AC-006**: **Given** valid authentication and an `id` present in the dataset, **When** the caller sends `GET /api/v1/reference_data/ticket_categories/{id}`, **Then** the response is `200 OK`, `application/json`, with a single record object (not wrapped in `results`) equal to the record with that `id` in the collection response, including a top-level category with `"parent_category_id": null` (e.g. `31`) and a child (e.g. `56`).
2. **AC-007**: **Given** valid authentication and a well-formed numeric `reference_id` that matches no ticket category (`1`; `30`, just before the first; `34`, a gap; `84`, a gap that is also a jurisdiction id; `89`, just past the last; or `999999`), **When** the caller requests it, **Then** the response is `404 Not Found` in the shared error response shape.
3. **AC-008**: **Given** valid authentication, **When** the caller sends a malformed `reference_id` (e.g. `abc`, `12x`, `1.5`, `-1`), **Then** the response is `400 Bad Request` in the shared error response shape.
4. **AC-009**: **Given** an `id` that exists in another type's dataset (e.g. `31`, which is the location "Bexley L317" under `locations`, or `40`, which is "Prefer not to say" under `genders`), **When** it is requested under `ticket_categories`, **Then** the result depends only on the TicketCategory dataset (`31` is "Civil", `40` is "Tax Chamber"): identifiers are scoped to their type and never resolve across types (EC-006).

---

### User Story 3 - Integrator Resolves Every Ticket's Category (Priority: P1)

As an integrator, I want every `ticket_category_id` a ticket carries to name a ticket category the mock serves, so that my system can group tickets by category without hitting a dangling reference.

**Why this priority**: Resolving these references is why TicketCategory is served with the extract's ids, and Constitution Principle IV requires references between served types to resolve. It closes 010 DF-8.

**Independent Test**: Load the Ticket and TicketCategory collections and check that every `ticket_category_id` names a served category.

**Acceptance Scenarios**:

1. **AC-010**: **Given** the default datasets, **When** the Ticket and TicketCategory collections are retrieved, **Then** every Ticket `ticket_category_id` (159 records, 41 distinct values) is the `id` of a served ticket category, closing 010 DF-8.
2. **AC-011**: **Given** the default datasets, **When** they are retrieved, **Then** the 13 categories that no ticket refers to (35, 43, 48, 51, 53, 60, 61, 63, 64, 65, 66, 79 and 81) are still served: references must resolve, but a record need not be referenced (EC-003).

---

### User Story 4 - Integrator Navigates the Category Hierarchy and Its References (Priority: P1)

As an integrator, I want each category's parent, jurisdiction and type to be exactly as E-Links has them, and every parent and jurisdiction to resolve, so that my system can build the category tree and label each category with its jurisdiction.

**Why this priority**: The hierarchy is what tells apart the two categories named "Immigration and Asylum Chamber", and Constitution Principle IV requires the served references to resolve and agree.

**Independent Test**: Load the TicketCategory and Jurisdiction collections and check every `parent_category_id` and `jurisdiction_id`, and the `type_id` values.

**Acceptance Scenarios**:

1. **AC-012**: **Given** the default dataset, **When** the collection is retrieved, **Then** exactly the 8 top-level categories (31, 32, 33, 35, 43, 48, 85 and 88) carry `"parent_category_id": null` (present, not left out); each of the other 46 carries the id of a served top-level category (35 for 7 of them, 43 for 4 and 48 for 35); no record is its own parent, and no chain of parents loops.
2. **AC-013**: **Given** the default datasets, **When** they are retrieved, **Then** every TicketCategory `jurisdiction_id` (30 for 3 records, 34 for 49, 84 for 1 and 87 for 1) is the `id` of a served jurisdiction, and every child category's `jurisdiction_id` equals its parent's; jurisdiction 87 "Skills" is now referenced, by category 88 "Languages".
3. **AC-014**: **Given** the default dataset, **When** the collection is retrieved, **Then** every record carries a non-null `type_id` from the extract (1 for 3 records, 2 for 3, 3 for 46, 4 for 1 and 5 for 1); **and given** the delivered feature, **when** the check that every reference between served types is tested runs, **then** it treats TicketCategory's `type_id` as a reference to the unserved `ticket_category_types` (DF-10), not to location types, while BaseLocation's and Location's `type_id` keep referring to location types (FR-020).

---

### User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

As a security reviewer, I want ticket-category requests without valid credentials refused exactly as requests for the existing types are, so that the new type opens no unauthenticated path.

**Why this priority**: A new type must not weaken the authentication that 002 made mandatory for every reference-data route.

**Independent Test**: Call each ticket-category route with no credentials and with invalid credentials, and confirm `401` in the shared error shape.

**Acceptance Scenarios**:

1. **AC-015**: **Given** a request with no credentials, **When** it is sent to any ticket-category route (collection or single-record, well-formed or malformed id), **Then** the response is `401 Unauthorized` in the shared error response shape and no reference data is returned.
2. **AC-016**: **Given** a request with present but invalid credentials, **When** it is sent to any ticket-category route, **Then** the response is `401 Unauthorized` in the shared error response shape, with the same generic message as for missing credentials.

---

### User Story 6 - The Singular Name Is Not Served (Priority: P2)

As an integrator, I want the singular name `ticket_category` rejected clearly, so that my system uses the canonical name `ticket_categories` instead of relying on an alias the mock doesn't serve.

**Why this priority**: Deprecated names are out of scope for this feature, so the mock serves only the canonical name. The choice must be observable and tested.

**Independent Test**: Call `ticket_category` and `ticket_category/{id}` with and without valid credentials, and confirm `400` (or `401` without valid credentials) in the shared error shape.

**Acceptance Scenarios**:

1. **AC-017**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket_category`, **Then** the response is `400 Bad Request` in the shared error response shape, with the unsupported attribute-name message, and no reference data is returned.
2. **AC-018**: **Given** valid authentication, **When** the caller sends `GET /api/v1/reference_data/ticket_category/{reference_id}` for any `reference_id` (one that exists under `ticket_categories` such as `31`, an unknown one such as `34`, or a malformed one such as `abc`), **Then** the response is `400 Bad Request` with the unsupported attribute-name message: the type is checked before the id, as in 002.
3. **AC-019**: **Given** a request to a `ticket_category` route with no credentials or with invalid credentials, **When** it is sent, **Then** the response is `401 Unauthorized` in the shared error response shape: authentication is checked before the attribute name.

---

### User Story 7 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

As an integrator already using the nine served types, I want adding ticket categories and the new reference field to leave those types and the last unbuilt type behaving exactly as before.

**Why this priority**: Every feature must hold the regression boundary, and this one changes shared main code.

**Independent Test**: Re-run 002's to 010's acceptance tests, compare the existing golden responses byte for byte, and request each of the remaining unsupported attribute names.

**Acceptance Scenarios**:

1. **AC-020**: **Given** the delivered feature, **When** 002's to 010's acceptance scenarios are run, **Then** they all pass, with the same responses as before this feature; every existing golden response is byte-for-byte identical. Shared test lists of unsupported names may lose `ticket_categories`; examples that used `ticket_categories` as an unsupported type, such as the missing-token check and the example request collection's "unsupported" request, move to `ticket_category_types`; and 009's statement that jurisdiction 87 is referenced by no shipped record is narrowed to the location data (FR-013). Nothing else asserted changes.
2. **AC-021**: **Given** valid authentication, **When** each of the 11 E-Links attribute names that are still unsupported (the last canonical type not yet served, `ticket_category_types`; its singular form `ticket_category_type`; and the unserved singular names `base_location`, `contract_type`, `gender`, `judiciary_role`, `jurisdiction`, `location`, `location_type`, `ticket` and `ticket_category`) is requested on either route, **Then** every one is rejected with `400 Bad Request` in the shared error response shape; none returns `200`, including with an empty `results` array.
3. **AC-022**: **Given** valid authentication, **When** the caller sends a ticket-category request with any query parameter (e.g. `?parent_category_id=48`), **Then** it is rejected with `400 Bad Request` in the shared error response shape, as for the existing types: the mock offers no filtering.

---

### User Story 8 - Maintainer Adds the Type With One Generic Change (Priority: P2)

As a maintainer, I want ticket categories added by declaring the type and supplying its dataset, plus one generic reference field that any type can declare, so that 002's extension design keeps working without type-specific code.

**Why this priority**: Keeps the last type cheap to add and the codebase free of special cases.

**Independent Test**: Review the change, run the start-up validation tests for the new reference field, and run the automated guard that forbids type-specific code.

**Acceptance Scenarios**:

1. **AC-023**: **Given** the delivered change, **When** it is reviewed, **Then** its only main-code change is the generic `parent_category_id` reference field (FR-019); everything else is the type entry, its dataset, tests, documentation and example requests.
2. **AC-024**: **Given** a type that declares `parent_category_id`, **When** a record leaves it out or holds anything other than a positive integer (or `null`, for a type that declares it nullable), **Then** the service refuses to start, naming the fixture, the field and the record, exactly as for the existing reference fields; **and given** a type that doesn't declare it, a record that carries it also stops the service at start-up. The automated guard against type-specific code passes, and it now also recognises the plural type name `ticket_categories` in every spelling, while the generic fields `ticket_category_id` and `parent_category_id` in their spellings are not matched (FR-018).
3. **AC-025**: **Given** the running service, **When** its published API documentation is inspected, **Then** the `attribute_name` parameter lists `ticket_categories` alongside the existing supported values and does not list `ticket_category`, and `parent_category_id` is documented as an optional, nullable integer present only for types that declare it, without that documentation having been edited by hand; `ReferenceDataResponse` is otherwise unchanged.

---

### User Story 9 - Example Requests Exercise the New Type (Priority: P3)

As a tester or integrating team, I want ready-made example requests for ticket categories so that I can exercise the new type without reading source code.

**Why this priority**: Required by the constitution (Principle XVII) for every new endpoint behaviour, but it adds no runtime behaviour.

**Independent Test**: Run the example request collection against a running instance and confirm every ticket-category request's checks pass.

**Acceptance Scenarios**:

1. **AC-026**: **Given** a running instance and the example request collection with its environment, **When** the collection is run, **Then** it includes passing requests for the ticket-category collection, a top-level record (e.g. `31`), a child record (e.g. `56`), an ended record (e.g. `38`), an unknown `reference_id` returning `404`, a malformed `reference_id` returning `400`, a query parameter and a trailing slash, each rejected as for the existing types, missing and invalid credentials returning `401`, and the unserved singular name `ticket_category` returning `400`, each with checks for status, response structure and key field values, and no hard-coded environment values. The request that used `ticket_categories` as an example of an unsupported type uses `ticket_category_types`.

---

### Edge Cases

- **EC-001 — Ids from the extract, with gaps**: Ids run from 31 to 88; 34, 84, 86 and 87 are missing (DR-1). Every well-formed id outside the dataset is unknown (`404`), including `1`, `30` (just before the first), `34` and `84` (gaps) and `89` (just past the last).
- **EC-002 — A repeated name**: "Immigration and Asylum Chamber" is held by 38 (under 35 "First tier Tribunal") and 45 (under 43 "Upper Tier"). They are separate records, told apart by their parent; the type opts out of the unique-name rule. By the synthetic date rule both are ended (positions 7 and 14).
- **EC-003 — Unreferenced categories**: 13 categories, including the three tribunal parents (35, 43, 48), are referenced by no ticket. They are served like every other record.
- **EC-004 — Top-level categories**: The 8 top-level categories carry `"parent_category_id": null`; the field is never left out, as for Location's `parent_id` (007).
- **EC-005 — The extract's dates are not reproduced**: The extract ends one category (81 "Transport Tribunal") and carries its own timestamps. All are ignored; 002's position-based rule applies, so 81 is open in the mock, and the 7 synthetic ended records, all children, include both "Immigration and Asylum Chamber" records. Their parents and tickets that refer to them stay as they are; nothing requires a referrer to be open when its target is ended (as 008 established).
- **EC-006 — Same id, different type**: Ticket-category ids overlap ids of other types (e.g. `31` is a location, `40` a gender, appointment title, judiciary role, contract type and base location; `84` and `86` are missing here but are jurisdiction ids). Identifiers are scoped to their type.
- **EC-007 — The same field name, a different target**: TicketCategory's `type_id` names a ticket category type; BaseLocation's and Location's `type_id` name a location type. The values 1 to 5 are valid location-type ids too, so only the source type, not the values, tells the targets apart (FR-020).
- **EC-008 — A forward reference**: Every `type_id` refers to a type that isn't served yet. The service does not check references at start-up, and a caller who requests a type under `ticket_category_types` gets `400` (unsupported type) until that type is served (DF-10).
- **EC-009 — Name content**: Names are plain ASCII, 5 ("Civil") to 48 characters long, free of leading or trailing whitespace, and include punctuation (`&`, `'`, `,`, `-` and parentheses). They are carried over exactly; order is by id, never by name.
- **EC-010 — Case and spelling variants**: `Ticket_Categories`, `TICKET_CATEGORIES`, `ticket-categories` and `ticket_categories_` are unsupported attribute names (`400`); matching is exact and case-sensitive, as in 002.
- **EC-011 — Inherited edge cases**: Covered by FR-004. For ticket categories, 002's EC-002 to EC-013 (trailing slashes, extra path segments, leading zeros, oversized ids, unsupported methods, unacceptable media types, evaluation order, unexpected failures and null end dates) apply unchanged.

## Requirements *(mandatory)*

### Functional Requirements

**Scope**

- **FR-001**: The system MUST serve the TicketCategory reference-data type through the existing collection and single-record reference-data routes, under the canonical attribute name `ticket_categories`. The singular name `ticket_category` MUST NOT be served (deprecated names are out of scope; Constitution Principle VII).
- **FR-002**: The supported attribute names MUST become exactly `appointment_titles`, `appointment_title`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets` and `ticket_categories`. This supersedes 010's FR-002; every other E-Links attribute name, including `ticket_category`, MUST remain unsupported (`400`).
- **FR-003**: TicketCategory MUST be added through the generic reference-data capability: a type entry and its dataset, plus the generic capability of FR-019. The change MUST NOT add or modify route, authentication or error-handling logic, and MUST add no type-specific logic.
- **FR-004**: All behaviour 002 defines for a supported type — authentication first, `reference_id` validation, `404` for unknown ids, `400` for query parameters, the shared error shape, ascending `id` order, read-only access and determinism — MUST apply to TicketCategory without exception. The type MUST allow repeated names (EC-002) and declares exactly three reference fields, `type_id`, `jurisdiction_id` and `parent_category_id`, of which only `parent_category_id` is nullable, and no boolean or text fields.

**Response content**

- **FR-005**: Each TicketCategory record MUST contain exactly these fields, in this order: `id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, `created_at`, `updated_at`, `start_date` and `end_date`, the shared fields in 002's formats (002 FR-011), `type_id` and `jurisdiction_id` as JSON integers and `parent_category_id` as a JSON integer or `null`. The records of the existing types MUST stay exactly as 002 to 010 define them.

**Data**

- **FR-006**: The TicketCategory dataset MUST be deterministic and supplied as a fixed, checked-in fixture. Only the public `id`, `name`, `type_id`, `jurisdiction_id` and `parent_category_id` values may be taken from the supplied ticket-category reference extract, each copied unchanged (a blank parent becomes `null`); every other value (dates and timestamps) MUST be synthetic, and no other column of the extract may be carried over (EC-005).
- **FR-007**: The extract MUST be used once, to generate the fixture. It MUST NOT be committed, and building, testing or running the service MUST NOT depend on it.
- **FR-008**: The default dataset MUST contain exactly one record for each of the 54 rows in the extract, which have 53 distinct names. Each name MUST be carried over exactly as the extract has it: nothing trimmed, case-folded, corrected or filtered (EC-009).
- **FR-009**: Records MUST keep the extract's ids (DR-1) and be ordered by ascending id, numbered from position 1 to 54 in that order. Records whose position is a multiple of 7 (the 7 ids in AC-003) are ended, with `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z; every other record has no end date and `updated_at` 2024-06-03T10:30:00Z; every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01. The extract's own dates and timestamps MUST NOT be used (EC-005).
- **FR-010**: TicketCategory ids MUST be stable across releases of the default dataset and MUST equal the extract's, so that Ticket (010), this type's own `parent_category_id` values and later features can refer to them.

**Referential integrity**

- **FR-011**: References between served types MUST resolve (Constitution Principle IV), verified by automated tests run on every build against the shipped datasets: every Ticket `ticket_category_id` and every TicketCategory `parent_category_id` MUST be the `id` of a served TicketCategory record (closing 010 DF-8), the parent hierarchy MUST have no cycles, and every TicketCategory `jurisdiction_id` MUST be the `id` of a served Jurisdiction. The service itself does not check that references resolve, as in 007 to 010.
- **FR-012**: Every TicketCategory `type_id` is a forward reference to `ticket_category_types`, recorded as DF-10 (Constitution Principle IV). Before the fixture is checked in, its values MUST be confirmed, once, to be ids in the local ticket-category-type extract (all 5 are). The build's checks MUST pin this type's `type_id` values until DF-10 closes, as 010 pinned `ticket_category_id`, and MUST require a resolution test as soon as `ticket_category_types` is served.
- **FR-013**: Served data MUST stay internally consistent (Constitution Principle IV), verified by automated tests on every build: every child TicketCategory's `jurisdiction_id` MUST equal its parent's (top-level categories have no parent to agree with), and the existing jurisdiction agreements (008) MUST keep passing. Jurisdiction 87 "Skills" is now referenced; the set of jurisdiction ids the shipped data refers to MUST include it, and every statement that 87 is referenced by no shipped record MUST be narrowed to the location data or updated.
- **FR-014**: No served type other than TicketCategory may carry `parent_category_id`.

**Documentation and example requests**

- **FR-015**: The generated API documentation MUST list `ticket_categories` among the supported `attribute_name` values and MUST NOT list `ticket_category`, derived from the configuration rather than edited by hand. It MUST document `parent_category_id` as an optional, nullable integer property of `ReferenceDataResponse`, present only for types that declare it (D-6); the rest of `ReferenceDataResponse` MUST be unchanged.
- **FR-016**: The example request collection MUST cover the ticket-category routes as AC-026 describes, using environment variables for every environment-specific value. The project's documentation of supported reference-data types MUST list ticket categories and the new reference field, record that DF-8 is closed and DF-10 is open, and update every statement that `ticket_categories` is not served or that jurisdiction 87 is unreferenced.

**Quality**

- **FR-017**: The ticket-category collection route MUST meet the same latency guards as the existing collection routes, under both the default bound and the strict target. Unit, integration, functional and smoke coverage MUST be extended to the new type and the new capability, and the project's JaCoCo coverage gate (90% lines, 80% branches) MUST pass. The fixture's generation rules (FR-008, FR-009), with the extract-id order rule and the declared reference fields, MUST be checked by an automated test without the extract.
- **FR-018**: The automated guard against type-specific code MUST recognise the plural type name `ticket_categories` in every spelling (e.g. `ticket_categories`, `TicketCategories`, `TICKET_CATEGORIES`, `ticket categories`), with no exemption, and MUST keep passing while main code names the generic fields `ticket_category_id` and `parent_category_id`. It does not cover the singular `ticket_category`, which the generic field `ticket_category_id` legitimately contains (as 009 and 010 decided for their types). The guard's own tests MUST show that the fragment matches those plural spellings and does not match `ticket_category_id`, `ticketCategoryId`, `TICKET_CATEGORY_ID` or `Id of the record's ticket category`. Test lists that use `ticket_categories` as an example of an unsupported name MUST drop it.
- **FR-019**: A type MUST be able to declare `parent_category_id` as a reference field, alongside `type_id`, `parent_id`, `jurisdiction_id` and `ticket_category_id`, with the same rules as those fields (003 FR-018, 007's nullable reference fields, 010 FR-019): at start-up, every record of a declaring type MUST carry it as a positive integer, or as `null` only if the type also declares it nullable; a record of a type that doesn't declare it MUST NOT carry it; and a violation MUST stop the service naming the fixture, the field and the record. Responses write it with the other reference fields, only for declaring types, so the responses of every other type MUST stay byte-for-byte the same.
- **FR-020**: The build's reference checks MUST determine each reference's target type from the source type as well as the field, so that TicketCategory's `type_id` refers to `ticket_category_types` while BaseLocation's and Location's `type_id` keep referring to `location_types` (EC-007). The target is defined in the build's reference checks only (Clarifications): each field keeps its usual target as the default, and a per-type override names `ticket_category_types` for TicketCategory's `type_id`. The same override applies to the pinned reference values, so this type's `type_id` values (1 to 5) are pinned separately from the location data's. No main code or configuration changes for this.

### Reference Points

The default dataset holds 54 records (fixed by FR-006, FR-008 and FR-009). These records are used by tests and example requests. Every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01; ended records have `end_date` 2025-03-31 and `updated_at` 2025-04-01T08:00:00Z, open ones a null `end_date` and `updated_at` 2024-06-03T10:30:00Z.

| Position | `id` | `name` | `type_id` | `jurisdiction_id` | `parent_category_id` | Ended | Note |
|----------|------|--------|-----------|-------------------|----------------------|-------|------|
| 1 | 31 | Civil | 1 | 30 | null | no | first; top-level; most used by tickets (30) |
| 4 | 35 | First tier Tribunal | 2 | 34 | null | no | top-level; unreferenced by tickets; parent of 7 |
| 7 | 38 | Immigration and Asylum Chamber | 3 | 34 | 35 | yes | repeated name; first ended |
| 13 | 44 | Administrative Appeals Chamber | 3 | 34 | 43 | no | child of Upper Tier |
| 14 | 45 | Immigration and Asylum Chamber | 3 | 34 | 43 | yes | repeated name |
| 17 | 48 | Other Tribunal | 2 | 34 | null | no | top-level; parent of 35 |
| 25 | 56 | Employment Tribunal (England & Wales) | 3 | 34 | 48 | no | `&` and parentheses |
| 50 | 81 | Transport Tribunal | 3 | 34 | 48 | no | ended in the extract, open here; unreferenced by tickets |
| 53 | 85 | Court Type | 4 | 84 | null | no | top-level; the only Magistrates category |
| 54 | 88 | Languages | 5 | 87 | null | no | last; top-level; the only reference to Skills |

`1`, `30`, `34`, `84`, `89` and `999999` are guaranteed unknown ids.

### Error Scenario Summary

| Scenario | Example request | Expected result |
|----------|-----------------|-----------------|
| Collection (canonical) | `GET /api/v1/reference_data/ticket_categories` | `200`, 54 records |
| Unserved singular name | `GET /api/v1/reference_data/ticket_category` | `400`, shared error shape |
| Top-level record | `GET /api/v1/reference_data/ticket_categories/31` | `200`, Civil, `parent_category_id` null |
| Child record | `GET /api/v1/reference_data/ticket_categories/56` | `200`, parent 48 |
| Ended record | `GET /api/v1/reference_data/ticket_categories/38` | `200`, ended |
| Unknown id | `GET /api/v1/reference_data/ticket_categories/34` | `404`, shared error shape |
| Malformed id | `GET /api/v1/reference_data/ticket_categories/abc` | `400`, shared error shape |
| Query parameter | `GET /api/v1/reference_data/ticket_categories?parent_category_id=48` | `400`, shared error shape |
| Still unsupported type | `GET /api/v1/reference_data/ticket_category_types` | `400`, shared error shape |
| Missing or invalid credentials | any ticket-category route | `401`, shared error shape |

### Key Entities

- **Reference-data type**: As defined in 002 and extended by 003, 004, 007, 008 and 010. This feature adds a tenth instance, TicketCategory, with canonical name `ticket_categories`, no served alias, repeated names allowed and three declared reference fields, one nullable. The set of declarable reference fields gains `parent_category_id`.
- **TicketCategory record**: One E-Links ticket category: a group of tickets, such as a court jurisdiction ("Civil"), a tribunal tier or chamber, or "Languages". Attributes: `id` (unique, stable, the E-Links id), `name` (public name, exactly as in the source, not necessarily unique), `type_id` (the E-Links id of its category type; a forward reference until `ticket_category_types` is served), `jurisdiction_id` (a served jurisdiction), `parent_category_id` (a served top-level category, or `null` for a top-level one), `created_at`, `updated_at` (UTC timestamps), `start_date` and optional `end_date`. Tickets refer to it through `ticket_category_id`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 26 acceptance scenarios (AC-001 to AC-026) pass as automated tests or, for AC-023 and AC-026, as review and collection-run checks.
- **SC-002**: The default ticket-category collection contains exactly 54 records with 53 distinct names, exactly 7 of them ended and 8 top-level, matching the Reference Points table.
- **SC-003**: 100% of the 54 served records carry the same `id`, `name`, `type_id`, `jurisdiction_id` and `parent_category_id` as the source row, character for character.
- **SC-004**: 100% of Ticket `ticket_category_id` values (159), TicketCategory `parent_category_id` values (46) and TicketCategory `jurisdiction_id` values (54) resolve to served records; 0 dangling references remain between served types; 100% of the 5 `type_id` values were confirmed, when the fixture was generated, to be ids in the ticket-category-type extract.
- **SC-005**: 0 requests via the unserved singular name `ticket_category` return reference data; 100% with valid credentials return `400`.
- **SC-006**: 0 of the 11 still-unsupported attribute names return a `200` response.
- **SC-007**: 0 ticket-category requests without valid credentials receive any reference data; 100% receive `401`.
- **SC-008**: 100% of 002's to 010's acceptance tests pass with their expected responses unchanged, and every existing golden response is byte-for-byte identical.
- **SC-009**: The only main-code change is the generic reference field (FR-019); 0 lines of type-specific code are added.
- **SC-010**: The ticket-category collection meets the same latency bounds as the existing collections in the smoke suite, at the default bound on every build and at the strict target when that check is run.
- **SC-011**: An integrator using the published documentation and example requests retrieves the ticket-category list and the category (and parent category) of a given ticket at the first attempt, without reading source code.

## Assumptions

- **Source values are public reference data**: Ticket-category names, ids, type ids, jurisdiction ids and parent ids describe public groupings of judicial work, not people, so reusing them is consistent with Constitution Principle IV. Dates and timestamps are synthetic.
- **The extract's ids are required, not chosen**: 010 DF-8 and Principle IV require TicketCategory to be served with the extract's ids so that every ticket's `ticket_category_id` resolves, as for Location (007), LocationType (008) and Jurisdiction (009).
- **`parent_category_id` is its own field**: The extract and E-Links name it `parent_category_id`, not `parent_id`, so the mock serves it under that name rather than reusing Location's field.
- **A new deviation number**: `parent_category_id` gets its own deviation, D-6, as `ticket_category_id` got D-5; `type_id` and `jurisdiction_id` reuse 003's D-2.
- **The type reference is served now, not deferred**: As for BaseLocation's references (003) and Ticket's `ticket_category_id` (010), consumers see the real response shape now, and the forward reference is recorded (DF-10) and closed by the feature that serves `ticket_category_types`.
- **The extract is CSV**: A header plus 54 rows of 9 quoted columns, ASCII, with no byte-order mark and no trailing newline. Only `id`, `name`, `type_id`, `jurisdiction_id` and `parent_category_id` are used. The format affects only the one-off generation, not the served data.
- **Same date rules as 002**: The dates, timestamps and ended-record rule are 002's, counted over ascending-id positions. The extract's own end date is not reproduced (EC-005).
- **The guard's fragment is the plural**: As for Jurisdiction (009) and Ticket (010), the guard matches `ticket_categories` rather than the singular, because the generic reference field `ticket_category_id` contains `ticket_category`. The feature description proposes this and leaves it open to clarify; it is the default here.
- **Deprecated names are out of scope**: Only `ticket_categories` is served, as for the singular names of the other types since 003. Constitution Principles I and VII make this an explicit per-type choice, not a contract deviation.
- **The next unsupported example is `ticket_category_types`**: It stays unsupported until its own feature, which will need another unsupported example (a singular name such as `ticket_category_type`).
- **No pagination**: The contract defines none for reference data, so the whole collection is returned in one response.
- **Latency targets are 002's**: The default p95 bound (1 second) and strict target (100 ms) apply unchanged.
- **Fixture source**: The extract used for generation is a local, gitignored reference file. Because the fixture is checked in and then becomes the source of truth, later changes to the extract do not change the served data.
- **Dependencies**: Relies on everything 002 to 010 delivered: the generic routes, central type registry, per-type declarations and fixture validation at start-up, declared and nullable reference fields, repeated names, the extract-id order rule, the reference-resolution test with its completeness and cycle checks, the agreement test, the pinned reference values, bearer-token authentication, correlation IDs, shared error shape, generated API documentation, example request collection, and the automated guard against type-specific code.
