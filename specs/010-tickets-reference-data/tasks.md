---

description: "Task list for 010-tickets-reference-data"
---

# Tasks: Reference Data API — Tickets

**Input**: Design documents from `/specs/010-tickets-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-tickets.md](./contracts/reference-data-api-tickets.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-017, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. The generic field (FR-019) is built test-first, as 008 built text fields: the unit tests are written and seen to fail before the main-code change.

**Organization**: Tasks are grouped by user story (spec.md US1–US8) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US5), then P2 (US4, US6, US7), then P3 (US8).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US8)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/java/…/jo/` (the generic field only) and `src/main/resources/`
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Unit-test fixtures: `src/test/resources/reference-data/` (valid) and `src/test/resources/reference-data/invalid/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; `src/test/java/…/jo/testsupport/ReferenceDataFixtures.java`; functional helpers `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java`

Expected values come from [data-model.md § 4](./data-model.md) and the [contract delta](./contracts/reference-data-api-tickets.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. Postman uses only the environment variables `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [ ] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/010-tickets-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the generic `ticket_category_id` field test-first, then ship the type (configuration and data) and keep the build green. Configuring `tickets` immediately changes what existing tests assert — the OpenAPI enum, the unsupported-name list, and `ReferenceDataReferencesTest`'s completeness check, which demands a target type for `ticket_category_id` — so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

### Generic field, tests first (FR-019; research R2–R5)

- [ ] T002 [P] In src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java, add `resolvesTheTicketCategoryReferenceField`: a `TypeProperties` declaring `List.of("ticket_category_id")` resolves to `getReferenceFields()` containing exactly `ReferenceField.TICKET_CATEGORY_ID`; and extend `resolvesTheDeclaredReferenceFields` to declare all four names (in the order `ticket_category_id, jurisdiction_id, type_id, parent_id`) and expect `TYPE_ID, PARENT_ID, JURISDICTION_ID, TICKET_CATEGORY_ID` (enum order). Leave the invalid-field cases unchanged
- [ ] T003 [P] In src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java (research R5): rename `ALL_REFERENCE_FIELDS` to `FIRST_REFERENCE_FIELDS` with the comment "The three reference fields 003 added; their fixtures carry no ticket_category_id." and update its uses; add the constant `TICKET_CATEGORY = List.of("ticket_category_id")`; create src/test/resources/reference-data/valid-ticket-category-reference-field.json (records 20 "Bravo" with `ticket_category_id` 33 and 10 "Alpha" with 31, otherwise like valid-reference-fields.json) and the invalid fixtures src/test/resources/reference-data/invalid/ticket-category-id-missing.json and ticket-category-id-not-positive.json (record id 5 without the key, and with `0`), and src/test/resources/reference-data/invalid/ticket-category-id-explicit-null.json (record id 5 with `null`); add `loadsTheTicketCategoryReferenceField` (loads both records with `getTicketCategoryId()` 31 and 33 and the other three reference fields null), `failsStartupWhenTheTicketCategoryReferenceFieldIsInvalid` as a `@CsvSource` over `ticket-category-id-missing | record id 5: ticket_category_id is required`, `ticket-category-id-not-positive | record id 5: ticket_category_id must be greater than 0` and `ticket-category-id-explicit-null | record id 5: ticket_category_id is required`, and `failsStartupWhenARecordCarriesTheTicketCategoryFieldItsTypeDoesNotDeclare` (the new valid fixture under a type declaring only `type_id` fails with `ticket_category_id is not a reference field of widgets`), and `loadsANullTicketCategoryWhenTheTypeDeclaresItNullable` (new fixture src/test/resources/reference-data/valid-nullable-ticket-category-reference-field.json, one record with `"ticket_category_id": null`, under a type declaring it in both `reference-fields` and `nullable-reference-fields`, loads with `getTicketCategoryId()` null); also extend `recordsOfATypeWithoutReferenceFieldsCarryNone` to assert `getTicketCategoryId()` is null
- [ ] T004 [P] In src/test/java/…/jo/testsupport/ReferenceDataFixtures.java, add a `null` argument after the `jurisdictionId` argument of all 8 `new ReferenceDataResponse(…)` calls; add `ticketCategoryType()` (a `widgets` type declaring only `ReferenceField.TICKET_CATEGORY_ID`, otherwise like `referencingType()`) and `ticketCategoryResponse(long id, String name, long ticketCategoryId)` with Javadoc "A current record that carries only the reference field ticket_category_id."; in src/test/java/…/jo/mappers/ReferenceDataMapperTest.java add the same `null` argument to its 1 call, add `mapsTheTicketCategoryField` (a record built with `.ticketCategoryId(31L)` under `ticketCategoryType()` maps to `ticketCategoryResponse(10, "Categorised", 31)`) and assert `response.ticketCategoryId()` is null in `leavesOutTheReferenceFieldsTheTypeDoesNotDeclare`; in src/test/java/…/jo/domain/ReferenceFieldValueTest.java add the `null` argument to `writesAValueAsANumber`'s call and change `keepsTheFieldOrderWithEveryField` to pass `value(31)` after `value(84)` and expect `…"jurisdiction_id":84,"ticket_category_id":31,"integration_code":"WIDGET_A"…`. These tests don't compile until T006–T007
- [ ] T005 After T004: in src/test/java/…/jo/controllers/ReferenceDataControllerTest.java add `ticketCategoryFieldIsWrittenAfterNameOnlyWhenTheRecordCarriesIt`, mirroring `referenceFieldsAreWrittenAfterNameOnlyWhenTheRecordCarriesThem` with `ticketCategoryResponse(10, "Categorised", 31)`: the JSON is `{"id":10,"name":"Categorised","ticket_category_id":31,` followed by the dates, and a plain record carries no `ticket_category_id`
- [ ] T006 In src/main/java/…/jo/entity/ReferenceField.java add `TICKET_CATEGORY_ID("ticket_category_id", ReferenceDataRecord::getTicketCategoryId)` after `JURISDICTION_ID`; in src/main/java/…/jo/entity/ReferenceDataRecord.java add `@JsonProperty("ticket_category_id") Long ticketCategoryId;` after `jurisdictionId`, with the Javadoc "Null unless the type declares {@link ReferenceField#TICKET_CATEGORY_ID}. The repository decides whether a null or a missing key is allowed."; in src/main/java/…/jo/config/ReferenceDataProperties.java extend the Javadoc's list of reference fields to `type_id`, `parent_id`, `jurisdiction_id` and `ticket_category_id` (research R2)
- [ ] T007 In src/main/java/…/jo/domain/ReferenceDataResponse.java add the component `ReferenceFieldValue ticketCategoryId` after `jurisdictionId` exactly as research R4 gives it (`@Schema` description "Id of the record's ticket category, in E-Links' numbering. Present only for types whose records carry it (deviation D-5 from the E-Links schema).", `implementation = Long.class`, `format = "int64"`, `requiredMode = NOT_REQUIRED`, `example = "31"`; `@JsonProperty("ticket_category_id")`; `@JsonInclude(NON_NULL)`), add `"ticket_category_id"` after `"jurisdiction_id"` in `@JsonPropertyOrder`, and name the field and D-5 in the class Javadoc; in src/main/java/…/jo/mappers/ReferenceDataMapper.java add `@Mapping(target = "ticketCategoryId", expression = "java(referenceField(record, ReferenceField.TICKET_CATEGORY_ID, type))")`
- [ ] T008 In src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java (contract delta § 2; AC-023, FR-013): the `ReferenceDataResponse` property-name assertion gains `ticket_category_id` after `jurisdiction_id`; the loop over reference properties covers `ticket_category_id` (optional `integer`, `int64`); assert it is not nullable and its description contains "deviation D-5"; `required` is unchanged
- [ ] T009 Run `./gradlew test integrationTest functionalTest`: every test passes, including T002–T005 and T008, with no other test changed, and every existing golden file still matches byte for byte (the eight existing types are unaffected). Record in specs/010-tickets-reference-data/plan.md § Implementation notes the per-suite counts and that springdoc publishes `ticket_category_id` as an optional, non-nullable `int64`

### The type (FR-001–FR-012; research R1, R6–R9)

- [ ] T010 Generate src/main/resources/reference-data/tickets.json with a throwaway script in the session scratchpad (never committed), following data-model.md § 4 rules 1–7 exactly: read the gitignored local `joh-elinks-api/ReferenceData/REF_Ticket.csv` as UTF-8 with a CSV parser; take `id`, `name` and `ticket_category_id` (as integers for the ids) from each row, ignoring every other column; order by ascending `id`; ended iff `position % 7 == 0` (`end_date` 2025-03-31, `updated_at` 2025-04-01T08:00:00Z), otherwise `end_date null`, `updated_at` 2024-06-03T10:30:00Z; every record `created_at` 2024-01-15T09:00:00Z, `start_date` 2024-01-01; two-space indent, field order `id, name, ticket_category_id, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, no `\u` escapes, trailing newline. Before keeping the file, the script MUST check (research R6): no BOM; the header is exactly `id,name,ticket_category_id,start_date,end_date,created_at,updated_at`; 159 data rows of 7 columns; ids unique ascending integers from 289 to 1709; names non-blank and trimmed, 152 distinct; the three written values equal the extract's row for row; the ended ids are exactly spec AC-003's 22; the distinct `ticket_category_id` values are exactly data-model.md § 4's 41 and each is an `id` in the local `joh-elinks-api/ReferenceData/REF_TicketCategory.csv`; the 7 repeated-name pairs of data-model.md § 3; and every reference point in data-model.md § 4. Neither the extracts nor the script is committed (FR-006, FR-007, FR-008, FR-009, FR-010, FR-011)
- [ ] T011 In src/main/resources/application.yml append after the `jurisdictions` entry under `jo.reference-data.types` the `tickets` entry exactly as research R1 gives it (`name: tickets`; the DR-1 comment; `fixture: classpath:reference-data/tickets.json`; the DF-8 comment; `reference-fields: [ticket_category_id]`; the EC-002 comment; `unique-names: false`), with no `aliases`, nullable, boolean or text keys (FR-001, FR-002, FR-004)
- [ ] T012 [P] Add `public static final String TICKETS = "/api/v1/reference_data/tickets";` after `JURISDICTIONS` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [ ] T013 [P] In src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: `ATTRIBUTE_NAMES` gains `tickets` after `jurisdictions` (so both `enum` assertions become `appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, appointment_title`); the description assertion becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets. Also supports deprecated values: appointment_title` (contract delta § 1; AC-023, FR-013)
- [ ] T014 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java (research R9): (a) in the unsupported-name `@ValueSource`, remove only `tickets` (so it holds the 12 still-unsupported E-Links names; `ticket` stays), add `Tickets`, `TICKETS` and `tickets_` to the case variants, update the comment to "The other 12 E-Links attribute names: two canonical types, their two deprecated aliases, and base_location, contract_type, gender, judiciary_role, jurisdiction, location, location_type and ticket, which 003–010 don't serve", and add 010 AC-019 and EC-008 (specs/010-tickets-reference-data) to the comment listing covered scenarios; (b) in `AuthenticationTests`, change the CSV row `"GET, /api/v1/reference_data/tickets"` to `"GET, /api/v1/reference_data/ticket_categories"` (AC-018, AC-019)
- [ ] T015 [P] In src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R7; FR-008, FR-009, FR-017): add `TICKET_CATEGORY_IDS`, the 41 values of data-model.md § 4, with Javadoc "The ticket-category ids the shipped tickets refer to, pinned until ticket_categories is served (010 DF-8)."; in the per-record loop, when the fixture declares `ticket_category_id`, assert each value is in `TICKET_CATEGORY_IDS`, and after the loop assert the set of values used equals `TICKET_CATEGORY_IDS`; add to `fixtures()` after `jurisdictions.json` the comment `// 010 data-model.md § 4: the extract's ids, with gaps; seven names repeat.` and `new Fixture("tickets.json", 159, 152, Map.of(), IdRule.SOURCE, List.of(point(289, "Welsh Language", null).withReferences(88L), point(290, "Administrative Court", null).withReferences(31L), point(294, "Civil Authorisation", null).withReferences(31L), point(295, "Commercial", END_DATE).withReferences(31L), point(365, "03 - Disability Living Allowance", null).withReferences(36L), point(367, "05 - Industrial Injuries", END_DATE).withReferences(36L), point(409, "Financial Remedy", END_DATE).withReferences(31L), point(410, "Financial Remedy", null).withReferences(33L), point(1709, "Valuation Tribunal Wales", null).withReferences(83L)))`
- [ ] T016 [P] In src/test/java/…/jo/repository/ReferenceDataReferencesTest.java (research R8; FR-011): add `"ticket_category_id", "ticket_categories"` to `TARGET_TYPES`; add no row to `references()` (`ticket_categories` is not served); leave the class Javadoc and `agreements()` unchanged
- [ ] T017 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file
- [ ] T018 Create src/functionalTest/java/…/jo/TicketsFunctionalTest.java mirroring src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "010's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.TICKETS`; constants `SINGULAR = "/api/v1/reference_data/ticket"`, `OPEN_UPDATED_AT`, `ENDED_UPDATED_AT`, `END_DATE`, `FIELDS` (`id, name, ticket_category_id, created_at, updated_at, start_date, end_date`), `ENDED_IDS` (the 22 of spec AC-003), `Row(id, name, ticketCategoryId, ended)` and `REFERENCE_POINTS` (the 9 rows of data-model.md § 4), and `REPEATED` (the 7 name → id-pair entries of data-model.md § 3); private helper `assertRecord(record, row)` (name, `ticket_category_id`, `end_date`, `updated_at`, `created_at`, `start_date`); story phases add test methods directly to this class
- [ ] T019 Run `./gradlew check -PskipOwasp`: every suite green, including `ReferenceDataFixtureRulesTest` (the tickets row and category ids), `ReferenceDataReferencesTest` (the completeness check is satisfied with the new `TARGET_TYPES` entry), `NoTypeSpecificCodeTest` (unchanged so far; `ticket_category_id` matches no existing fragment), the earlier types' functional tests, and the JaCoCo gate

**Checkpoint**: The field and the type are live; existing tests reflect nine supported types and four reference fields; the forward reference is tracked; the build is green; the eight existing types' responses are unchanged.

---

## Phase 3: User Story 1 - Integrator Retrieves All Tickets (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 159 tickets, deterministically.

**Independent Test**: `GET /api/v1/reference_data/tickets` with a valid token returns `200` and 159 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–5).

- [ ] T020 [US1] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac001ReturnsAll159TicketsInAscendingIdOrder`: `200`, `application/json`, `results` of 159 records with strictly ascending ids from 289 to 1709, each with exactly `FIELDS` in order and no other key (AC-001, FR-005)
- [ ] T021 [US1] In the same file add `ac002ReferencePointsMatchTheTable`: each `REFERENCE_POINTS` row equals its record via `assertRecord`, and every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01 (AC-002, SC-002)
- [ ] T022 [US1] In the same file add `ac003Exactly22RecordsAreEnded`: on the raw body, exactly 22 `"end_date":"2025-03-31"` and 137 `"end_date":null`; on the parsed records, the ended ids are exactly `ENDED_IDS`, each with `updated_at` `ENDED_UPDATED_AT`, and every open record has `OPEN_UPDATED_AT` (AC-003)
- [ ] T023 [US1] In the same file add `ac004RepeatedNamesStaySeparate`: 152 distinct names; for each `REPEATED` entry, exactly its two ids carry the name and their `ticket_category_id` values differ (AC-004, EC-002)
- [ ] T024 [US1] Stop any instance already listening on :8080 (it would serve the pre-010 configuration), then capture the collection response from a running instance (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/tickets.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/tickets`). The file must be the compact body exactly as served, with no reformatting and no trailing newline, because `ReferenceDataHttp.golden()` compares bytes. Check it matches the fixture's records field for field; then add `ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/TicketsFunctionalTest.java, as in JurisdictionsFunctionalTest (AC-005)

**Checkpoint**: US1 complete — the MVP: integrators can load tickets.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Ticket by ID (Priority: P1)

**Goal**: The single-record route returns one ticket and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/tickets/294` returns `200` with Civil Authorisation (quickstart § 3 rows 6–10).

- [ ] T025 [US2] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 289, 294, 295, 365, 367, 409, 410 and 1709: `200`, a single object (no `results`), equal to the collection element with that id, and matching its `REFERENCE_POINTS` row (AC-006)
- [ ] T026 [US2] In the same file add `ac007UnknownIdsReturn404` over `1`, `288`, `297`, `1710` and `999999` (`404`, record-not-found message) and `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message) (AC-007, AC-008, EC-001)
- [ ] T027 [US2] In the same file add `ac009IdsAreScopedToTheirType`: id 294 returns "Civil Authorisation" under `tickets` and "Llanelli" under `locations`; id 290 returns "Administrative Court" under `tickets` and "Commissioner" under `appointment_titles` (AC-009, EC-006)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 3 - Integrator Sees Each Ticket's Category (Priority: P1)

**Goal**: Every ticket carries its E-Links category id; the forward reference is tracked; no other type carries the field.

**Independent Test**: Load the ticket collection and check the category ids (quickstart § 3 rows 11–12). The build-time checks are T015 and T016.

- [ ] T028 [US3] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac010EveryTicketCarriesItsCategory`: every one of the 159 records has a positive integral `ticket_category_id` (never null); the distinct values number 41, from 31 to 88 (AC-010)
- [ ] T029 [US3] Confirm AC-011 is met by T016 (the completeness check `everyReferenceBetweenServedTypesIsChecked` fails without the `TARGET_TYPES` entry and requires no row while `ticket_categories` is unserved), and note the mapping in specs/010-tickets-reference-data/plan.md § Implementation notes (AC-011, DF-8)
- [ ] T030 [US3] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac012OtherTypesDoNotCarryTheTicketCategoryField`, parameterised over `APPOINTMENT_TITLES`, `BASE_LOCATIONS`, `CONTRACT_TYPES`, `GENDERS`, `JUDICIARY_ROLES`, `LOCATIONS`, `LOCATION_TYPES` and `JURISDICTIONS`: the body does not contain `ticket_category_id` and `results` is not empty (AC-012, FR-012, EC-007)

**Checkpoint**: US3 complete — the category reference is served and tracked (DF-8 open).

---

## Phase 6: User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each ticket route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 15–16).

- [ ] T031 [US5] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac016MissingTokenIsRejected`, parameterised over the collection, `/294`, `/297` and `/abc`: `401`, shared shape, `WWW-Authenticate: Bearer`, no `results` (AC-016)
- [ ] T032 [US5] In the same file add `ac017InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty): `401` with the same message as a missing token, on the collection, `/294` and `/abc` (AC-017)

**Checkpoint**: All P1 stories complete.

---

## Phase 7: User Story 4 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `ticket` is rejected like any unsupported name.

**Independent Test**: `ticket` and `ticket/{id}` return `400` with a token and `401` without (quickstart § 3 rows 13–14).

- [ ] T033 [US4] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac013TheSingularCollectionIsUnsupported` (`400`, unsupported-attribute message, no `results`), `ac014TheSingularSingleRecordRouteIsUnsupported` over `294`, `297` and `abc` (all `400` with the unsupported-attribute message: the type is checked before the id) and `ac015AuthenticationIsCheckedBeforeTheSingularName` over the two singular paths without a token and with `Bearer wrong-token` (`401`) (AC-013, AC-014, AC-015)

**Checkpoint**: US4 complete.

---

## Phase 8: User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unbuilt type behave exactly as before.

**Independent Test**: 002's to 009's acceptance tests pass; the 12 unsupported names return `400` (quickstart § 3 rows 17–18).

- [ ] T034 [US6] Confirm with `git diff --quiet origin/main -- src/functionalTest/resources/golden/` that the eight existing golden files are unchanged and only `tickets.json` is new (AC-018, SC-008)
- [ ] T035 [US6] In src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `ac020QueryParametersReturn400`, parameterised over `?ticket_category_id=31`, `?page=2`, `?x=` and `/294?name=Commercial`: `400`, query-parameter message; and the inherited checks `ec009UnmatchedPathsReturn404` (trailing slash, `/294/`, `/294/extra`), `ec009OtherMethodsReturn405` (POST, PUT, PATCH, DELETE on the collection and `/294`), `ec009UnacceptableMediaTypesReturn406` and `ec009LeadingZerosAndOversizedIds` (`/0294` → id 294; `/99999999999999999999` → `404`), as in JurisdictionsFunctionalTest. AC-019 and EC-008 are covered by T014 (AC-020, EC-009)
- [ ] T036 [US6] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T014's list and example changes (AC-018)

**Checkpoint**: US6 complete — no regressions.

---

## Phase 9: User Story 7 - Maintainer Adds the Type With One Generic Change (Priority: P2)

**Goal**: The only main-code change is the generic field, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; `git diff --stat origin/main -- src/main/java` lists only the planned files (quickstart § 1–2).

- [ ] T037 [P] [US7] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R9; FR-018, AC-022): add `"tickets"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, with no exemption, and extend the comment above the list to say that tickets' fragment is also the plural, because the generic reference field `ticket_category_id` (010) contains the singular (010 Clarifications); add `theGuardRecognisesEveryTicketsSpelling` over `tickets`, `Tickets`, `TICKETS`, `myTickets`, `TicketsHelper` and `XTICKETS`, asserting `spellingsOf("tickets")` matches; and `theGuardIgnoresTheGenericTicketCategoryField` over `ticket_category_id`, `ticketCategoryId`, `TICKET_CATEGORY_ID`, `getTicketCategoryId` and `Id of the record's ticket category`, asserting it doesn't match
- [ ] T038 [US7] Run `git diff --stat origin/main -- src/main/java` (expect exactly `ReferenceField`, `ReferenceDataRecord`, `ReferenceDataResponse`, `ReferenceDataMapper` and `ReferenceDataProperties`), `git diff --stat origin/main -- src/main/resources` (expect application.yml and tickets.json only) and `git grep -n -i tickets -- src/main/java` (expect no output), and record the result in specs/010-tickets-reference-data/plan.md § Implementation notes (AC-021, SC-009)
- [ ] T039 [US7] Confirm AC-022 (start-up validation of `ticket_category_id`) is covered by T002 and T003, and AC-023 (OpenAPI lists `tickets`, not `ticket`; `ticket_category_id` an optional, non-nullable integer citing D-5) by T008 and T013, and note the mapping in specs/010-tickets-reference-data/plan.md § Implementation notes

**Checkpoint**: US7 complete.

---

## Phase 10: User Story 8 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise tickets from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [ ] T040 [US8] In postman/ctam-jomockapi.postman_collection.json, inserting text with a throwaway scratchpad script so existing formatting is untouched (as 008 and 009 did) (research R10; AC-024, FR-014): (a) add a "Tickets" sub-folder under "Reference Data", after "Jurisdictions", with ten requests and test scripts: the collection (`200`, 159 results with exactly the seven fields, first `[289, "Welsh Language", 88]`, last `[1709, "Valuation Tribunal Wales", 83]`, 22 ended); `/294` (`200`, Civil Authorisation, category 31, not ended); `/367` (`200`, 05 - Industrial Injuries, category 36, `end_date` 2025-03-31); `/297` (`404`); `/abc` (`400`); `?ticket_category_id=31` (`400`); trailing slash (`404`, "Resource not found."); missing token (`401`, `WWW-Authenticate: Bearer`); invalid token (`{{invalidBearerToken}}`, `401`); `ticket` (`400`, unsupported, no `results`); (b) rename the request "GET /api/v1/reference_data/tickets - unsupported (contract-valid, out of scope)" to "GET /api/v1/reference_data/ticket_categories - unsupported (contract-valid, out of scope)" and change its `raw` URL and `path` to `ticket_categories`, leaving its tests unchanged. Confirm with `git diff --stat` that the change is insertions plus that one request's three edited lines
- [ ] T041 [US8] Start the app (`./gradlew bootRun`; stop any instance already on :8080 first) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/010-tickets-reference-data/plan.md § Implementation notes (AC-024)

**Checkpoint**: All user stories complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

- [ ] T042 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `TICKETS` (with its static import) to the collection paths of the per-collection p95 guard (FR-016, SC-010)
- [ ] T043 [P] Update README.md (FR-015; research R11): the Status bullet becomes nine types, "from appointment titles to tickets", up to `specs/010-tickets-reference-data/`; § Reference data: "Nine types are configured", a `tickets` row (`159, one per source row (152 distinct names)`; `none: ticket returns 400`); a paragraph after the jurisdictions paragraph: "Ticket records also keep the E-Links ids (289 to 1709, with gaps), so that People authorisations' `ticket_id` values can refer to them later. Each carries `ticket_category_id`, after `name`: the E-Links id of the ticket's category. Ticket categories aren't served yet, so this is a forward reference: the feature that serves them must make every value resolve (constitution, Principle IV). Seven names repeat, each in two categories."; in the configuration example, the `reference-fields` line becomes `[type_id, parent_id, jurisdiction_id, ticket_category_id]` with the comment "optional: any of the four"; in "Tests to update", the `NoTypeSpecificCodeTest` bullet's example becomes "(as `jurisdictions` and `tickets` do, because of `jurisdiction_id` and `ticket_category_id`)"
- [ ] T044 [P] Update CLAUDE.md: § Current repo state gains a `010-tickets-reference-data` line (tickets via configuration, a fixture and one generic capability: a fourth reference field `ticket_category_id`, deviation D-5, declared like the other three and written only for declaring types, so the other eight types' responses are byte-for-byte unchanged; 159 records keeping the extract's ids 289 to 1709, 22 ended, 7 names repeated in different categories; `ticket_category_id` is a forward reference to `ticket_categories`, DF-8, tracked by `ReferenceDataReferencesTest`'s `TARGET_TYPES` and pinned by `ReferenceDataFixtureRulesTest`'s `TICKET_CATEGORY_IDS`; DF-9: the People feature must point authorisations' `ticket_id` at these records; the singular `ticket` returns `400`; `NoTypeSpecificCodeTest` widened to the plural `tickets`, since `ticket_category_id` contains the singular); § Reference data adds ticket names, ids and category ids to the public values taken from the extracts and `specs/010-tickets-reference-data/data-model.md` § 4 to the list of generation rules
- [ ] T045 [P] Add an "Extended by 010: see [the tickets contract delta](../../010-tickets-reference-data/contracts/reference-data-api-tickets.md)." pointer after the 009 pointer on the attribute-name line (line 44) of specs/002-reference-data-api/contracts/reference-data-api.md, and at the end of the `ReferenceDataResponse` line (line 153), after 008's pointer, as "Extended by 010: optional `ticket_category_id`; see [the tickets contract delta § 2](../../010-tickets-reference-data/contracts/reference-data-api-tickets.md)."; and in specs/002-reference-data-api/quickstart.md § 3, change row 5 from `$BASE/tickets` to the still-unsupported `$BASE/ticket_categories` (research R11)
- [ ] T046 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite test counts, JaCoCo line/branch coverage and each collection's strict p95 in specs/010-tickets-reference-data/plan.md § Implementation notes (FR-016, FR-017, SC-010)
- [ ] T047 Walk through specs/010-tickets-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun` (stop any instance already on :8080 first), mark the spec's Status as Implemented in specs/010-tickets-reference-data/spec.md, and record that every expected result matched (or the differences) in specs/010-tickets-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story.
  - T002–T005 (tests first) before T006–T007 (main code); T004 before T005.
  - T008 after T007 (it asserts the generated schema); T009 after T002–T008.
  - T010 before T011 (the type can't load without its fixture); T012–T018 after T011; T019 last.
- **User stories (Phases 3–10)**: all after Phase 2. They add methods to the same functional test class, so run them in order US1 → US2 → US3 → US5 → US4 → US6 → US7 → US8, or have one person own that file. T037 (guard) and T040 (Postman) touch other files and can run alongside.
- **Polish (Phase 11)**: after the stories; T042–T045 in parallel, then T046, then T047.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2, US3, US5 (P1)**, **US4, US6, US7 (P2)**, **US8 (P3)**: each needs only Phase 2 functionally; US3's build-time half is T015–T016, and US7's start-up and schema half is T002–T008. US8's newman run (T041) is most useful once US1–US7 are green.

### Parallel opportunities

- Phase 2, tests first: T002, T003 and T004 (different files).
- Phase 2, the type: T012, T013, T014, T015 and T016 (different files).
- Stories: T037 (guard) and T040 (Postman) alongside the functional-test tasks.
- Polish: T042–T045.

### Parallel example: Phase 2 test updates for the type

```text
T012 ContractPaths                        — TICKETS constant
T013 ReferenceDataOpenApiContractTest     — enum and description
T014 ReferenceDataFunctionalTest          — unsupported list and auth example
T015 ReferenceDataFixtureRulesTest        — tickets row; TICKET_CATEGORY_IDS
T016 ReferenceDataReferencesTest          — TARGET_TYPES entry
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (the generic field test-first, then the type live, with the build green).
2. Phase 3 (US1): the collection works and is tested. **Stop and validate**: quickstart § 3 rows 1–5.

### Incremental delivery

3. US2, US3 and US5 (remaining P1) → US4, US6, US7 (P2) → US8 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extracts and the generating script stay out of the repository; check `git status` before every commit.
