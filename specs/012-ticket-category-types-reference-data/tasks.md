---

description: "Task list for 012-ticket-category-types-reference-data"
---

# Tasks: Reference Data API — Ticket Category Types

**Input**: Design documents from `/specs/012-ticket-category-types-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-ticket-category-types.md](./contracts/reference-data-api-ticket-category-types.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-019, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. There is no main-code change (FR-003, SC-009), so the user-story work *is* test work: the tests prove the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US8) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US5), then P2 (US4, US6, US7), then P3 (US8).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US8)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/resources/` only — **no file under `src/main/java` changes** (AC-022)
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; functional helpers: `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java`

Expected values come from [data-model.md § 2–3](./data-model.md) and the [contract delta](./contracts/reference-data-api-ticket-category-types.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. Postman uses only the environment variables `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [ ] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/012-ticket-category-types-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Ship the type (configuration and data) and keep the build green. Configuring `ticket_category_types` immediately changes what existing tests assert — the OpenAPI enum, the unsupported-name list, the fixture-rules table, and `ReferenceDataReferencesTest`'s completeness check, which demands the `ticket_categories.type_id` and `ticket_category_types.jurisdiction_id` rows — so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

- [ ] T002 Generate src/main/resources/reference-data/ticket_category_types.json with a throwaway script in the session scratchpad (never committed), following data-model.md § 3 rules 1–6 exactly: read the gitignored local `joh-elinks-api/ReferenceData/REF_TicketCategoryType.csv` as UTF-8 with a CSV parser; take `id`, `name`, `jurisdiction_id` and `integration_code`, ignoring every other column; order by ascending `id`; ended iff `position % 7 == 0` (none); every record `end_date null`, `updated_at 2024-06-03T10:30:00Z`, `created_at 2024-01-15T09:00:00Z`, `start_date 2024-01-01`; two-space indent, field order `id, name, jurisdiction_id, integration_code, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, no `\u` escapes, trailing newline. Before keeping the file, the script MUST check (research R2): no BOM; the header is exactly `id,name,jurisdiction_id,integration_code,start_date,end_date,created_at,updated_at`; 5 data rows of 8 columns; ids are the integers 1 to 5; names non-blank and trimmed, 4 distinct; `jurisdiction_id` positive integers; `integration_code` non-blank, trimmed, matching `[A-Z_]+` and distinct; the four written values equal the extract's row for row; no record ended; every `jurisdiction_id` is an id in src/main/resources/reference-data/jurisdictions.json; every `type_id` in src/main/resources/reference-data/ticket_categories.json is a written id and each category's `jurisdiction_id` equals its type's; and the result equals data-model.md § 3's Records table. Neither the extract nor the script is committed (FR-006 to FR-010, FR-013)
- [ ] T003 In src/main/resources/application.yml (research R1): append after the `ticket_categories` entry under `jo.reference-data.types` a `name: ticket_category_types` entry exactly as research R1 shows (the DR-1 comment, `fixture: classpath:reference-data/ticket_category_types.json`, `reference-fields: [jurisdiction_id]` with the comment `# The id of a jurisdiction, checked by ReferenceDataReferencesTest.`, `text-fields: [integration_code]` with `# The extract's integration code (008 D-4).`, and `unique-names: false` with `# "Court Type" is both a Courts and a Magistrates type (012 EC-002).`), with no `aliases`, `nullable-reference-fields` or `boolean-fields` keys; and replace the `ticket_categories` entry's three-line comment above `reference-fields` with "type_id, jurisdiction_id and parent_category_id are ids of a ticket category type, a jurisdiction and another ticket category, checked by ReferenceDataReferencesTest (012)." (wrapped at 120 characters), so `type_id` is no longer called a forward reference (FR-001, FR-002, FR-004, FR-017)
- [ ] T004 [P] Add `public static final String TICKET_CATEGORY_TYPES = "/api/v1/reference_data/ticket_category_types";` after `TICKET_CATEGORIES` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [ ] T005 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: `ATTRIBUTE_NAMES` gains `ticket_category_types` after `ticket_categories` (so both `enum` assertions become `appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, ticket_categories, ticket_category_types, appointment_title`); the description assertion becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, ticket_categories, ticket_category_types. Also supports deprecated values: appointment_title`; leave every `ReferenceDataResponse` assertion unchanged (contract delta § 1–2; AC-024, FR-015)
- [ ] T006 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java (research R6; FR-021): (a) in the unsupported-name `@ValueSource`, remove only `ticket_category_types` (so it holds the 10 unserved singular names; `ticket_category_type` stays), add `Ticket_Category_Types`, `TICKET_CATEGORY_TYPES`, `ticket-category-types` and `ticket_category_types_` to the case variants, update the comment to "The other 10 E-Links attribute names: the deprecated singular forms base_location, contract_type, gender, judiciary_role, jurisdiction, location, location_type, ticket, ticket_category and ticket_category_type, which 003–012 don't serve", and add 012 AC-020 and EC-008 (specs/012-ticket-category-types-reference-data) to the comment listing covered scenarios; (b) in `AuthenticationTests`, change the CSV row `"GET, /api/v1/reference_data/ticket_category_types"` to `"GET, /api/v1/reference_data/ticket_category_type"`, so the case still shows authentication is checked before an unsupported type (AC-016, AC-019)
- [ ] T007 [P] In src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R3; FR-008, FR-009, FR-013, FR-019): add a row to `fixtures()` after `ticket_categories.json`, preceded by the comment `// 012 data-model.md § 3: the extract's ids, without gaps; none ended.`: `new Fixture("ticket_category_types.json", 5, 4, Map.of(), IdRule.SOURCE, List.of(point(1, "Court Type", null).withReferences(30L).withTexts("COURTS_COURT_TYPE"), point(2, "Tribunal Type", null).withReferences(34L).withTexts("TRIBS_TYPE"), point(3, "Tribunal Name", null).withReferences(34L).withTexts("TRIBS_NAME"), point(4, "Court Type", null).withReferences(84L).withTexts("MAGS_COURT_TYPE"), point(5, "Skills Category", null).withReferences(87L).withTexts("SKILLS_CATEGORY")))`; keep `JURISDICTION_IDS` as `{30, 34, 84, 86, 87}` and change its Javadoc's "87 Skills (by ticket category 88 only, 011)" to "87 Skills (by ticket category 88 and ticket category type 5; 011, 012)"; keep `TICKET_CATEGORY_TYPE_IDS` and its `PINNED_TYPE_REFERENCE_VALUES` entry, changing its Javadoc to "The ticket-category-type ids the shipped ticket categories refer to. Kept after 012 served ticket_category_types, as {@link #TICKET_CATEGORY_IDS} was after 011: ReferenceDataReferencesTest checks that the ids resolve, and this pins which ones the categories use."
- [ ] T008 [P] In src/test/java/…/jo/repository/ReferenceDataReferencesTest.java (research R4; FR-011, FR-012): append to `references()`, after the `ticket_categories` rows, the comment `// Closes 011 DF-10.` and the rows `new Reference("ticket_categories", "type_id", "ticket_category_types")` and `new Reference("ticket_category_types", "jurisdiction_id", "jurisdictions")`; append to `agreements()` `new Agreement("ticket_categories", "type_id", "ticket_category_types", "jurisdiction_id")`; leave `TARGET_TYPES` and `TARGET_TYPE_OVERRIDES` unchanged; if the class Javadoc or any comment still describes `ticket_categories.type_id` or `ticket_category_types` as unserved or a forward reference (011 DF-10), reword it so it doesn't
- [ ] T009 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file
- [ ] T010 Create src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java mirroring src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java (same record shape, with `integration_code`) and src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java (no ended record): `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "012's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.TICKET_CATEGORY_TYPES`; constants `SINGULAR = "/api/v1/reference_data/ticket_category_type"`, `OPEN_UPDATED_AT = "2024-06-03T10:30:00Z"`, `FIELDS` (`id, name, jurisdiction_id, integration_code, created_at, updated_at, start_date, end_date`) and `RECORDS`, a `Row(id, name, jurisdictionId, integrationCode)` list of the five records in data-model.md § 3; a private helper `assertRecord(record, row)` (id, name, `jurisdiction_id` via `ReferenceDataHttp.integralField`, `integration_code`, null `end_date`, `updated_at` `OPEN_UPDATED_AT`, `created_at` 2024-01-15T09:00:00Z, `start_date` 2024-01-01); `assertUnauthorized` and `byId` from `ReferenceDataHttp`; story phases add test methods directly to this class
- [ ] T011 Run `./gradlew check -PskipOwasp`: every suite green, including `ReferenceDataFixtureRulesTest`, `ReferenceDataReferencesTest` (the two new rows and the agreement pass, and the completeness check is satisfied), `NoTypeSpecificCodeTest` (unchanged so far), the earlier types' functional tests, and the JaCoCo gate

**Checkpoint**: The type is live; existing tests reflect eleven supported types; every reference touching it is checked; the build is green; no main Java file changed.

---

## Phase 3: User Story 1 - Integrator Retrieves All Ticket Category Types (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 5 ticket category types, deterministically.

**Independent Test**: `GET /api/v1/reference_data/ticket_category_types` with a valid token returns `200` and 5 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–4).

- [ ] T012 [US1] In src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java add `ac001ReturnsAll5TicketCategoryTypesInAscendingIdOrder`: `200`, `application/json`, `results` of 5 records with ids 1 to 5, each with exactly `FIELDS` in order and no other key (AC-001, FR-005)
- [ ] T013 [US1] In the same file add `ac002RecordsMatchTheRecordsTable`: each record equals its `RECORDS` row via `assertRecord` (AC-002, SC-002, SC-003)
- [ ] T014 [US1] In the same file add `ac003NoTicketCategoryTypeIsEnded`: every `end_date` is JSON null and every `updated_at` is `OPEN_UPDATED_AT` (AC-003, EC-004)
- [ ] T015 [US1] In the same file add `ac004CourtTypeIsTheOnlyRepeatedName`: 4 distinct names; "Court Type" is exactly records 1 and 4, with `jurisdiction_id` 30 and 84 and `integration_code` `COURTS_COURT_TYPE` and `MAGS_COURT_TYPE`; the five integration codes are distinct (a data fact from EC-003, not a requirement; no FR requires distinct codes) (AC-004, EC-002, EC-003)
- [ ] T016 [US1] Capture the collection response from a running instance (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/ticket_category_types.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/ticket_category_types`). The file must be the compact body exactly as served, with no reformatting and no trailing newline, because `ReferenceDataHttp.golden()` compares bytes. Check it matches the fixture field for field; then add `ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java, as in LocationTypesFunctionalTest (AC-005)

**Checkpoint**: US1 complete — the MVP: integrators can load ticket category types.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Ticket Category Type by ID (Priority: P1)

**Goal**: The single-record route returns one ticket category type and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/ticket_category_types/4` returns `200` with the Magistrates "Court Type" record (quickstart § 3 rows 5–8).

- [ ] T017 [US2] In src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 1 to 5: `200`, a single object (no `results`), equal to the collection element with that id (AC-006)
- [ ] T018 [US2] In the same file add `ac007UnknownIdsReturn404` over `6`, `30`, `31` and `999999` (`404`, record-not-found message) and `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message) (AC-007, AC-008, EC-001)
- [ ] T019 [US2] In the same file add `ac009IdsAreScopedToTheirType`: id 1 returns "Court Type" under `ticket_category_types` and "Region" under `location_types`; id 4 returns "Court Type" under `ticket_category_types` and "Chamber" under `location_types` (AC-009, EC-006)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 3 - Integrator Resolves Every Ticket Category's Type (Priority: P1)

**Goal**: Every ticket category's `type_id` names a served type in the same jurisdiction, and every type's `jurisdiction_id` a served jurisdiction, observable over HTTP.

**Independent Test**: Load the ticket-category-type, ticket-category and jurisdiction collections and check the references and the agreement (quickstart § 3 rows 9–12). The build-time check is T008.

- [ ] T020 [US3] In src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java add `ac010EveryTicketCategoryTypeIdResolves`: load the type ids over HTTP, then assert every one of the 54 ticket categories has a `type_id` (read with `ReferenceDataHttp.integralField`) that is one of them, naming the category on failure; and that the counts per type are 1→3, 2→3, 3→46, 4→1, 5→1 (AC-010, SC-004)
- [ ] T021 [US3] In the same file add `ac011EveryTicketCategoryIsInItsTypesJurisdiction`: for each of the 54 ticket categories, its `jurisdiction_id` equals the `jurisdiction_id` of the type its `type_id` names, naming the category on failure, and assert 54 were compared (AC-011, FR-012)
- [ ] T022 [US3] In the same file add `ac012EveryTypeJurisdictionResolves`: every ticket category type's `jurisdiction_id` is the id of a served jurisdiction, and type 5 refers to 87 "Skills" (AC-012, AC-013)
- [ ] T023 [US3] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java change the comment above `ac011SkillsIsServedAndUnreferencedByTheLocationData` to "Ticket category 88 (011) and ticket category type 5 (012) refer to Skills; the location data doesn't.", leaving its assertions unchanged (FR-014)
- [ ] T024 [US3] Confirm AC-013's completeness half is met by T008 (`everyReferenceBetweenServedTypesIsChecked` passes and now demands both new rows, with no outstanding forward reference between served types), and note the mapping in specs/012-ticket-category-types-reference-data/plan.md § Implementation notes (AC-013)

**Checkpoint**: US3 complete — 011 DF-10 is closed.

---

## Phase 6: User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each ticket-category-type route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 16–17).

- [ ] T025 [US5] In src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java add `ac017MissingTokenIsRejected`, parameterised over the collection, `/4`, `/6` and `/abc`: `401`, shared shape, `WWW-Authenticate: Bearer`, no `results` (AC-017)
- [ ] T026 [US5] In the same file add `ac018InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty): `401` with the same message as a missing token, on the collection, `/4` and `/abc` (AC-018)

**Checkpoint**: All P1 stories complete.

---

## Phase 7: User Story 4 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `ticket_category_type` is rejected like any unsupported name.

**Independent Test**: `ticket_category_type` and `ticket_category_type/{id}` return `400` with a token and `401` without (quickstart § 3 rows 14–15).

- [ ] T027 [US4] In src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java add `ac014TheSingularCollectionIsUnsupported` (`400`, unsupported-attribute message, no `results`), `ac015TheSingularSingleRecordRouteIsUnsupported` over `1`, `6` and `abc` (all `400` with the unsupported-attribute message: the type is checked before the id) and `ac016AuthenticationIsCheckedBeforeTheSingularName` over the two singular paths without a token and with `Bearer wrong-token` (`401`) (AC-014, AC-015, AC-016)

**Checkpoint**: US4 complete.

---

## Phase 8: User Story 6 - Existing Behaviour Is Unchanged and Other Names Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unserved name behave exactly as before.

**Independent Test**: 002's to 011's acceptance tests pass; the 10 unsupported names return `400` (quickstart § 3 rows 13 and 19).

- [ ] T028 [US6] Confirm with `git diff --quiet origin/main -- src/functionalTest/resources/golden/` that the ten existing golden files are unchanged and only `ticket_category_types.json` is new (AC-019, SC-008)
- [ ] T029 [P] [US6] Add `TICKET_CATEGORY_TYPES` (with its static import) to the `@ValueSource` of `ac012OtherTypesDoNotCarryTheTicketCategoryField` in src/functionalTest/java/…/jo/TicketsFunctionalTest.java and of `fr014OtherTypesDoNotCarryTheParentCategoryField` in src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java, so each field's "other types don't carry it" check covers every other served type; and in TicketCategoriesFunctionalTest.java change the Javadoc of `ac014TypeIdsAreCopied` from "The values only; that they refer to the unserved ticket category types is ReferenceDataReferencesTest's concern (AC-014, DF-10)." to "The values only; that they resolve to served ticket category types is ReferenceDataReferencesTest's concern (AC-014; DF-10, closed by 012)." (AC-019, FR-017)
- [ ] T030 [US6] In src/functionalTest/java/…/jo/TicketCategoryTypesFunctionalTest.java add `ac021QueryParametersReturn400`, parameterised over `?jurisdiction_id=30`, `?page=2`, `?x=` and `/4?name=Court%20Type`: `400`, query-parameter message; and the inherited checks `ec010UnmatchedPathsReturn404` (trailing slash, `/4/`, `/4/extra`), `ec010OtherMethodsReturn405` (POST, PUT, PATCH, DELETE on the collection and `/4`), `ec010UnacceptableMediaTypesReturn406` and `ec010LeadingZerosAndOversizedIds` (`/004` → id 4; `/99999999999999999999` → `404`), as in LocationTypesFunctionalTest. AC-020 and EC-008 are covered by T006 (AC-021, EC-010)
- [ ] T031 [US6] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T006's list and example changes and T029's added path (AC-019)

**Checkpoint**: US6 complete — no regressions.

---

## Phase 9: User Story 7 - Maintainer Adds the Type Without Code (Priority: P2)

**Goal**: The change is configuration and data only, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; `git diff --stat origin/main -- src/main/java` is empty (quickstart § 1–2).

- [ ] T032 [P] [US7] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R5; FR-020, AC-023): add `"ticket_category_types"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, with no exemption, and extend the comment on the plural fragments to name it; add `theGuardRecognisesEveryTicketCategoryTypesSpelling` over `ticket_category_types`, `TicketCategoryTypes`, `TICKET_CATEGORY_TYPES`, `ticket category types`, `myTicketCategoryTypes` and `XTICKET_CATEGORY_TYPES`, asserting `spellingsOf("ticket_category_types")` matches; and `theGuardIgnoresOtherTicketCategoryNames` over `ticket_category_id`, `ticketCategoryId`, `ticket_categories`, `TicketCategories` and `the record's ticket category`, asserting it doesn't match
- [ ] T033 [US7] Run `git diff --stat origin/main -- src/main/java` (expect no output) and `git diff --stat origin/main -- src/main/resources` (expect application.yml and ticket_category_types.json only), and record the result in specs/012-ticket-category-types-reference-data/plan.md § Implementation notes (AC-022, SC-009)
- [ ] T034 [US7] Confirm AC-024 (OpenAPI lists `ticket_category_types`, not `ticket_category_type`; `ReferenceDataResponse` unchanged) is covered by T005, and note the mapping in specs/012-ticket-category-types-reference-data/plan.md § Implementation notes

**Checkpoint**: US7 complete.

---

## Phase 10: User Story 8 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise ticket category types from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [ ] T035 [US8] In postman/ctam-jomockapi.postman_collection.json, inserting text with a throwaway scratchpad script so existing formatting is untouched (as 008 to 011 did) (research R7; AC-025, FR-016, FR-021): (a) add a "Ticket Category Types" sub-folder under "Reference Data", after "Ticket Categories", with ten requests and test scripts: the collection (`200`, 5 results with exactly the eight fields, ids `[1,2,3,4,5]`, every `end_date` null, first "Court Type" with `COURTS_COURT_TYPE`, last "Skills Category" with jurisdiction 87); `/3` (`200`, Tribunal Name, jurisdiction 34, `TRIBS_NAME`); `/4` (`200`, Court Type, jurisdiction 84, `MAGS_COURT_TYPE`); `/6` (`404`); `/abc` (`400`); `?jurisdiction_id=30` (`400`); trailing slash (`404`, "Resource not found."); missing token (`401`, `WWW-Authenticate: Bearer`); invalid token (`{{invalidBearerToken}}`, `401`); "`ticket_category_type` - singular name not served" (`400`, unsupported, no `results`), named like the other folders' singular requests; (b) rename the request "GET /api/v1/reference_data/ticket_category_types - unsupported (contract-valid, out of scope)" to "GET /api/v1/reference_data/ticket_category_type - unsupported (unserved singular)" and change its `raw` URL and `path` to `ticket_category_type`, leaving its tests unchanged; it remains the collection's general unsupported-name example, kept alongside the folder request (spec Clarifications). Confirm with `git diff --stat` that the change is insertions plus that one request's three edited lines
- [ ] T036 [US8] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/012-ticket-category-types-reference-data/plan.md § Implementation notes (AC-025)

**Checkpoint**: All user stories complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

- [ ] T037 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `TICKET_CATEGORY_TYPES` (with its static import) to the collection paths of the per-collection p95 guard (FR-018, SC-010)
- [ ] T038 [P] Update README.md (FR-017; research R8): line 40's "the Reference Data API for ten types, from appointment titles to ticket categories" becomes "the Reference Data API for all eleven E-Links types, from appointment titles to ticket category types"; "Ten types are configured:" becomes "Eleven types are configured, every E-Links canonical name:" with a `ticket_category_types` row (5, one per source row (4 distinct names); none: `ticket_category_type` returns `400`); the jurisdictions paragraph's last sentence becomes "Skills (87) is referenced by the ticket category Languages and the ticket category type Skills Category."; in the ticket-category paragraph, replace "`type_id` is the id of a ticket category type; those aren't served yet, so it is a forward reference that the feature serving them must make resolve (constitution, Principle IV)." with "Every `type_id` is the id of a served ticket category type in the same jurisdiction."; add a ticket-category-type paragraph after it (the E-Links ids 1 to 5; `jurisdiction_id` and then `integration_code` after `name`, as for location types; every `jurisdiction_id` the id of a served jurisdiction; "Court Type" twice, for Courts and Magistrates; none ended); in § Adding a reference-data type, add `ticket_category_types` to the guard bullet's examples of plural fragments; remove `ticket_category_types` from any list or example of unserved types
- [ ] T039 [P] Update CLAUDE.md: § Current repo state gains a `012-ticket-category-types-reference-data` line (ticket_category_types via configuration and fixture only, no main-code change; 5 records keeping the extract's ids 1 to 5, none ended, carrying `jurisdiction_id` and `integration_code` like location types; "Court Type" repeats, so `unique-names: false`; two `ReferenceDataReferencesTest` rows and an agreement row close 011 DF-10, so every E-Links canonical name is served and no served type has an outstanding forward reference; Skills (87) is referenced by ticket category 88 and ticket category type 5; the unsupported-name examples move to `ticket_category_type`; the singular returns `400`; `NoTypeSpecificCodeTest` widened to the plural `ticket_category_types`, with no exemption); in the 011 line, "`type_id` is a forward reference to the unserved `ticket_category_types` (DF-10)" gains "(closed by 012)"; § Reference data adds ticket-category-type names, ids, `jurisdiction_id` and `integration_code` values to the public values taken from the extracts, and `specs/012-ticket-category-types-reference-data/data-model.md` § 3 to the list of generation rules
- [ ] T040 [P] Add an "Extended by 012: see [the ticket-category-types contract delta](../../012-ticket-category-types-reference-data/contracts/reference-data-api-ticket-category-types.md)." pointer after the 011 pointer on the attribute-name line of specs/002-reference-data-api/contracts/reference-data-api.md (not on the `ReferenceDataResponse` line, which is unchanged); and in specs/002-reference-data-api/quickstart.md § 3, change row 5 from `$BASE/ticket_category_types` to `$BASE/ticket_category_type`, which shows the same `400` (FR-021; research R8)
- [ ] T041 [P] Add a closing note to the DF-10 row of specs/011-ticket-categories-reference-data/spec.md: "Closed by 012 ([012 FR-011, FR-012](../012-ticket-category-types-reference-data/spec.md); `ReferenceDataReferencesTest`)." (research R8)
- [ ] T042 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite test counts, JaCoCo line/branch coverage and each collection's strict p95 in specs/012-ticket-category-types-reference-data/plan.md § Implementation notes (FR-018, FR-019, SC-010)
- [ ] T043 Walk through specs/012-ticket-category-types-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun`, mark the spec's Status as Implemented in specs/012-ticket-category-types-reference-data/spec.md, and record that every expected result matched (or the differences) in specs/012-ticket-category-types-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story. T002 before T003 (the type can't load without its fixture); T004–T010 after T003; T011 last.
- **User stories (Phases 3–10)**: all after Phase 2. They add methods to the same functional test class, so run them in order US1 → US2 → US3 → US5 → US4 → US6 → US7 → US8, or have one person own that file. T023 and T029 (other functional classes), T032 (guard) and T035 (Postman) touch other files and can run alongside.
- **Polish (Phase 11)**: after the stories; T037–T041 in parallel, then T042, then T043.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2, US3, US5 (P1)**, **US4, US6, US7 (P2)**, **US8 (P3)**: each needs only Phase 2 functionally; US3's build-time half is T008 in Phase 2. US8's newman run (T036) is most useful once US1–US7 are green.

### Parallel opportunities

- Phase 2: T004, T005, T006, T007 and T008 (different files).
- Stories: T023, T029, T032 and T035 alongside the `TicketCategoryTypesFunctionalTest` tasks.
- Polish: T037–T041.

### Parallel example: Phase 2 test updates

```text
T004 ContractPaths                        — TICKET_CATEGORY_TYPES constant
T005 ReferenceDataOpenApiContractTest     — enum and description
T006 ReferenceDataFunctionalTest          — unsupported list and auth example
T007 ReferenceDataFixtureRulesTest        — ticket_category_types row; pin Javadoc
T008 ReferenceDataReferencesTest          — two reference rows; one agreement row
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (build green with the type live and its references checked).
2. Phase 3 (US1): the collection works and is tested. **Stop and validate**: quickstart § 3 rows 1–4.

### Incremental delivery

3. US2, US3 and US5 (remaining P1) → US4, US6, US7 (P2) → US8 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extract and the generating script stay out of the repository; check `git status` before every commit.
