---

description: "Task list for 011-ticket-categories-reference-data"
---

# Tasks: Reference Data API — Ticket Categories

**Input**: Design documents from `/specs/011-ticket-categories-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-ticket-categories.md](./contracts/reference-data-api-ticket-categories.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-017, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. The generic field (FR-019) is built test-first, as 010 built `ticket_category_id`: the unit tests are written and seen to fail before the main-code change.

**Organization**: Tasks are grouped by user story (spec.md US1–US9) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US4, US5), then P2 (US6, US7, US8), then P3 (US9).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US9)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/java/…/jo/` (the generic field only) and `src/main/resources/`
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Unit-test fixtures: `src/test/resources/reference-data/` (valid) and `src/test/resources/reference-data/invalid/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; `src/test/java/…/jo/testsupport/ReferenceDataFixtures.java`; functional helpers `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java`

Expected values come from [data-model.md § 3–4](./data-model.md) and the [contract delta](./contracts/reference-data-api-ticket-categories.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. Postman uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [X] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/011-ticket-categories-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the generic `parent_category_id` field test-first, then ship the type and keep the build green. Configuring `ticket_categories` immediately changes what existing tests assert — the OpenAPI enum, the unsupported-name list, `ReferenceDataReferencesTest`'s completeness check (three new rows, and a target for `ticket_categories.type_id`) and `ReferenceDataFixtureRulesTest`'s jurisdiction pin — so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

### Generic field, tests first (FR-019; research R2–R5)

- [X] T002 [P] In src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java: extend `resolvesTheDeclaredReferenceFields` to declare all five names (in the order `parent_category_id, ticket_category_id, jurisdiction_id, type_id, parent_id`) and expect `TYPE_ID, PARENT_ID, JURISDICTION_ID, TICKET_CATEGORY_ID, PARENT_CATEGORY_ID`; add `resolvesTheParentCategoryReferenceField` (declaring only `parent_category_id` resolves to exactly `ReferenceField.PARENT_CATEGORY_ID`) and `resolvesParentCategoryAsANullableReferenceField` (declared in both `reference-fields` and `nullable-reference-fields`, `getNullableReferenceFields()` is exactly `PARENT_CATEGORY_ID`)
- [X] T003 [P] In src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java (research R5), mirroring the `ticket_category_id` cases 010 added: add `PARENT_CATEGORY = List.of("parent_category_id")`; create src/test/resources/reference-data/valid-parent-category-reference-field.json (records 20 "Bravo" with `parent_category_id` 10 and 10 "Alpha" with `null`) and the invalid fixtures src/test/resources/reference-data/invalid/parent-category-id-missing.json, parent-category-id-not-positive.json (`0`), parent-category-id-explicit-null.json (`null`), parent-category-id-fractional.json (`10.5`), parent-category-id-string.json (`"10"`) and parent-category-id-empty-string.json (`""`), each a single record id 5 otherwise like the ticket-category ones; add `loadsTheParentCategoryReferenceField` (the valid fixture under a type declaring the field nullable loads ids 10 and 20 with `getParentCategoryId()` null and 10, every other reference field null); `failsStartupWhenTheParentCategoryReferenceFieldIsInvalid` as a `@CsvSource` over `parent-category-id-missing | record id 5: parent_category_id is required`, `parent-category-id-not-positive | record id 5: parent_category_id must be greater than 0` and `parent-category-id-explicit-null | record id 5: parent_category_id is required`, under a type declaring it but not nullable; `failsStartupWhenARecordCarriesTheParentCategoryFieldItsTypeDoesNotDeclare` (the valid fixture under a type declaring no reference field fails with `record id 20: parent_category_id is not a reference field of widgets`); add `parent-category-id-fractional, parent_category_id`, `parent-category-id-string, parent_category_id` and `parent-category-id-empty-string, parent_category_id` to `failsStartupOnUnparseableFixtures`; and assert `getParentCategoryId()` is null in `recordsOfATypeWithoutReferenceFieldsCarryNone`
- [X] T004 [P] In src/test/java/…/jo/testsupport/ReferenceDataFixtures.java, add a `null` argument after the `ticketCategoryId` argument of all 9 `new ReferenceDataResponse(…)` calls; add `parentCategoryType()` (a `widgets` type declaring `ReferenceField.PARENT_CATEGORY_ID`, nullable) and `parentCategoryResponse(long id, String name, Long parentCategoryId)` (the field as `new ReferenceFieldValue(parentCategoryId)`, so `null` is a present null), with Javadoc; in src/test/java/…/jo/mappers/ReferenceDataMapperTest.java add the same argument to its 2 calls, extend `mapsEveryReferenceFieldTogether` with `.parentCategoryId(48L)` and `value(48)`, add `mapsTheParentCategoryFieldIncludingAPresentNull` (a record with `.parentCategoryId(48L)` maps to `parentCategoryResponse(10, "Child", 48L)`, and one without maps to `parentCategoryResponse(20, "Top", null)` whose `parentCategoryId()` equals `new ReferenceFieldValue(null)`), and assert `response.parentCategoryId()` is null in `leavesOutTheReferenceFieldsTheTypeDoesNotDeclare`; in src/test/java/…/jo/domain/ReferenceFieldValueTest.java add the argument to `writesAValueAsANumber`'s call and change `keepsTheFieldOrderWithEveryField` to pass `value(48)` after `value(31)` and expect `…"ticket_category_id":31,"parent_category_id":48,"integration_code":"WIDGET_A"…`. These tests don't compile until T006–T007
- [X] T005 After T004: in src/test/java/…/jo/controllers/ReferenceDataControllerTest.java add `parentCategoryFieldIsWrittenOnlyWhenTheRecordCarriesIt` (`parentCategoryResponse(10, "Child", 48L)` is written `{"id":10,"name":"Child","parent_category_id":48,` then the dates, with 7 properties) and `nullParentCategoryIsWrittenAsNull` (`parentCategoryResponse(10, "Top", null)` has `"parent_category_id":null` present); and assert `$.parent_category_id` does not exist in `recordsWithoutReferenceFieldsLeaveThemOut`
- [X] T006 In src/main/java/…/jo/entity/ReferenceField.java add `PARENT_CATEGORY_ID("parent_category_id", ReferenceDataRecord::getParentCategoryId)` after `TICKET_CATEGORY_ID`; in src/main/java/…/jo/entity/ReferenceDataRecord.java add `@JsonProperty("parent_category_id") Long parentCategoryId;` after `ticketCategoryId`, with the Javadoc of research R2; in src/main/java/…/jo/config/ReferenceDataProperties.java extend the Javadoc's list of reference fields with `{@code parent_category_id}`, keeping every line within 120 characters
- [X] T007 In src/main/java/…/jo/domain/ReferenceDataResponse.java add the component `ReferenceFieldValue parentCategoryId` after `ticketCategoryId` exactly as research R4 gives it (description citing D-6 and "top-level record", `implementation = Long.class`, `format = "int64"`, `NOT_REQUIRED`, `nullable = true`, `example = "48"`, `@JsonProperty("parent_category_id")`, `@JsonInclude(NON_NULL)`), add `"parent_category_id"` after `"ticket_category_id"` in `@JsonPropertyOrder`, and name the field and D-6 in the class Javadoc (re-wrapping it within 120 characters); in src/main/java/…/jo/mappers/ReferenceDataMapper.java add `@Mapping(target = "parentCategoryId", expression = "java(referenceField(record, ReferenceField.PARENT_CATEGORY_ID, type))")`
- [X] T008 In src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java (contract delta § 2; AC-025, FR-015): the property-name assertion gains `parent_category_id` after `ticket_category_id`; the reference loop covers it, expecting `deviation D-6` in its description; assert it is nullable and its description contains "top-level record"; `required` is unchanged
- [X] T009 Run `./gradlew check -PskipOwasp`: every test passes, including T002–T005 and T008, with no other test changed, Checkstyle clean, and every existing golden file still byte for byte. Record in specs/011-ticket-categories-reference-data/plan.md § Implementation notes the per-suite counts and that springdoc publishes `parent_category_id` as an optional, nullable `int64`

### The type (FR-001–FR-016, FR-020; research R1, R6–R9)

- [X] T010 Generate src/main/resources/reference-data/ticket_categories.json with a throwaway script in the session scratchpad (never committed), following data-model.md § 4 rules 1–7: read the gitignored `joh-elinks-api/ReferenceData/REF_TicketCategory.csv` as UTF-8 with a CSV parser; take `id`, `name`, `type_id`, `jurisdiction_id` and `parent_category_id` (integers; blank parent → `null`); order by ascending `id`; ended iff `position % 7 == 0`; two-space indent, field order `id, name, type_id, jurisdiction_id, parent_category_id, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, no `\u`, trailing newline. Before keeping the file, the script MUST check every item of research R6: the exact header, 54 rows of 9 columns, no BOM; ids 31 to 88 unique and ascending; names trimmed, 53 distinct; the five written values equal the extract's row for row; the ended ids are exactly 38, 45, 52, 59, 66, 73, 80; the 8 top-level ids; every parent is a top-level id in the file with the same `jurisdiction_id`; every `jurisdiction_id` is an id in src/main/resources/reference-data/jurisdictions.json; every `type_id` is an id in the local `joh-elinks-api/ReferenceData/REF_TicketCategoryType.csv`; every `ticket_category_id` in src/main/resources/reference-data/tickets.json is an id in the new file; and every reference point of data-model.md § 4. Neither the extracts nor the script is committed (FR-006–FR-012)
- [X] T011 In src/main/resources/application.yml append after the `tickets` entry the `ticket_categories` entry exactly as research R1 gives it, and change the `tickets` entry's DF-8 comment to "# ticket_category_id is the id of a ticket category, checked by ReferenceDataReferencesTest (011)." (FR-001, FR-002, FR-004)
- [X] T012 [P] Add `public static final String TICKET_CATEGORIES = "/api/v1/reference_data/ticket_categories";` after `TICKETS` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [X] T013 [P] In src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: `ATTRIBUTE_NAMES` gains `ticket_categories` after `tickets`; the description assertion becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, ticket_categories. Also supports deprecated values: appointment_title` (contract delta § 1; AC-025)
- [X] T014 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java (research R9): (a) remove only `ticket_categories` from the unsupported-name `@ValueSource` (leaving the 11 names of spec AC-021; `ticket_category` stays), add `Ticket_Categories`, `TICKET_CATEGORIES`, `ticket-categories` and `ticket_categories_` to the variants, update the comment to "The other 11 E-Links attribute names: one canonical type, its deprecated alias, and base_location, contract_type, gender, judiciary_role, jurisdiction, location, location_type, ticket and ticket_category, which 003–011 don't serve", and add 011 AC-021 and EC-010 (specs/011-ticket-categories-reference-data) to the covered-scenarios comment; (b) in `AuthenticationTests`, change the row `"GET, /api/v1/reference_data/ticket_categories"` to `"GET, /api/v1/reference_data/ticket_category_types"` (AC-020, AC-021)
- [X] T015 [P] In src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R7; FR-012, FR-013, FR-017): `JURISDICTION_IDS` gains `87L`, with the Javadoc of research R7; add `TICKET_CATEGORY_TYPE_IDS = Set.of(1L, 2L, 3L, 4L, 5L)` and `PINNED_TYPE_REFERENCE_VALUES = Map.of("ticket_categories", Map.of("type_id", TICKET_CATEGORY_TYPE_IDS))`, each with Javadoc citing 011 DF-10 and FR-020; add a helper `pinnedValues(String type, String field)` (the per-type entry if any, else `PINNED_REFERENCE_VALUES.get(field)`), with the type taken from the fixture file name; use it in `followsTheGenerationRules`; change `everyPinnedReferenceValueIsUsed` to collect values under the same key (type and field for an override, field otherwise) and compare with the union of both tables; add to `fixtures()` after `tickets.json` the comment `// 011 data-model.md § 4: the extract's ids, with gaps; one name repeats; 8 top-level categories.` and `new Fixture("ticket_categories.json", 54, 53, Map.of(), IdRule.SOURCE, List.of(…))` with the 10 reference points of data-model.md § 4, each `withReferences(typeId, jurisdictionId, parentCategoryId)` (`null` for a top-level parent; `END_DATE` for 38 and 45)
- [X] T016 [P] In src/test/java/…/jo/repository/ReferenceDataReferencesTest.java (research R8; FR-011, FR-013, FR-020): add `"parent_category_id", "ticket_categories"` to `TARGET_TYPES`; add `TARGET_TYPE_OVERRIDES = Map.of("ticket_categories", Map.of("type_id", "ticket_category_types"))` with Javadoc citing 011 FR-020 and a helper `targetOf(String type, String field)`, used by `everyReferenceBetweenServedTypesIsChecked`; append to `references()` the comment `// Closes 010 DF-8.` and the three rows of research R8; append to `agreements()` `new Agreement("ticket_categories", "parent_category_id", "ticket_categories", "jurisdiction_id")`; in `everyAgreementHolds`, skip a record whose `via` value is present and `null` (a missing key still fails through `integral`), and say so in the `Agreement` Javadoc
- [X] T017 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file
- [X] T018 Create src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java mirroring src/functionalTest/java/…/jo/TicketsFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "011's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.TICKET_CATEGORIES`; constants `SINGULAR = "/api/v1/reference_data/ticket_category"`, `OPEN_UPDATED_AT`, `ENDED_UPDATED_AT`, `END_DATE`, `FIELDS` (spec FR-005's nine), `ENDED_IDS` (7), `TOP_LEVEL_IDS` (8), `UNREFERENCED_IDS` (13, data-model.md § 3), `Row(id, name, typeId, jurisdictionId, Long parentCategoryId, ended)` and `REFERENCE_POINTS` (data-model.md § 4); private helper `assertRecord(record, row)` checking every field, with `parent_category_id` present and `null` for a top-level row; story phases add test methods directly to this class
- [X] T019 In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java rename `ac011SkillsIsServedButUnreferenced` to `ac011SkillsIsServedAndUnreferencedByTheLocationData`, assertions unchanged, with a comment that ticket category 88 refers to it (011); then run `./gradlew check -PskipOwasp`: every suite green, including `ReferenceDataFixtureRulesTest`, `ReferenceDataReferencesTest`, `NoTypeSpecificCodeTest` (unchanged so far), the earlier types' functional tests and the JaCoCo gate

**Checkpoint**: The field and the type are live; existing tests reflect ten supported types and five reference fields; DF-8 is closed; the forward reference to `ticket_category_types` is tracked; the build is green; the nine existing types' responses are unchanged.

---

## Phase 3: User Story 1 - Integrator Retrieves All Ticket Categories (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 54 ticket categories, deterministically.

**Independent Test**: `GET /api/v1/reference_data/ticket_categories` returns `200` and 54 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–4).

- [X] T020 [US1] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac001ReturnsAll54TicketCategoriesInAscendingIdOrder`: `200`, `application/json`, 54 records with strictly ascending ids from 31 to 88, each with exactly `FIELDS` in order (AC-001, FR-005)
- [X] T021 [US1] In the same file add `ac002ReferencePointsMatchTheTable`: each `REFERENCE_POINTS` row equals its record via `assertRecord`, and every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01 (AC-002, SC-002)
- [X] T022 [US1] In the same file add `ac003Exactly7RecordsAreEnded`: on the raw body, exactly 7 `"end_date":"2025-03-31"` and 47 `"end_date":null`; the ended ids are exactly `ENDED_IDS` with `ENDED_UPDATED_AT`, and every open record has `OPEN_UPDATED_AT` (AC-003)
- [X] T023 [US1] In the same file add `ac004TheRepeatedNameStaysSeparate`: 53 distinct names; "Immigration and Asylum Chamber" is exactly ids 38 and 45, with `parent_category_id` 35 and 43 (AC-004, EC-002)
- [X] T024 [US1] Stop any instance already listening on :8080 (it would serve the pre-011 configuration), then capture the collection response (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/ticket_categories.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/ticket_categories`): compact, no trailing newline. Check it matches the fixture field for field; then add `ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java (AC-005)

**Checkpoint**: US1 complete — the MVP: integrators can load ticket categories.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Ticket Category by ID (Priority: P1)

**Goal**: The single-record route returns one category and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/ticket_categories/56` returns `200` with its parent 48 (quickstart § 3 rows 5–9).

- [X] T025 [US2] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over 31, 38, 45, 56, 81, 85 and 88: `200`, a single object, equal to the collection element with that id and matching its `REFERENCE_POINTS` row, with `parent_category_id` present (null for 31, 85 and 88) (AC-006)
- [X] T026 [US2] In the same file add `ac007UnknownIdsReturn404` over `1`, `30`, `34`, `84`, `89` and `999999` and `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (AC-007, AC-008, EC-001)
- [X] T027 [US2] In the same file add `ac009IdsAreScopedToTheirType`: 31 is "Civil" under `ticket_categories` and "Bexley L317" under `locations`; 40 is "Tax Chamber" under `ticket_categories` and "Prefer not to say" under `genders` (AC-009, EC-006)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 3 - Integrator Resolves Every Ticket's Category (Priority: P1)

**Goal**: Every ticket's `ticket_category_id` names a served category (closes 010 DF-8).

**Independent Test**: quickstart § 3 row 10. The build-time check is T016.

- [X] T028 [US3] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac010EveryTicketsCategoryResolves`: load both collections over HTTP; all 159 tickets carry a `ticket_category_id` that is a served category id, naming the ticket on failure (AC-010, SC-004)
- [X] T029 [US3] In the same file add `ac011UnreferencedCategoriesAreServed`: each of `UNREFERENCED_IDS` returns `200`, and no ticket refers to any of them (AC-011, EC-003)

**Checkpoint**: US3 complete — 010 DF-8 is closed.

---

## Phase 6: User Story 4 - Integrator Navigates the Category Hierarchy and Its References (Priority: P1)

**Goal**: Parents, jurisdictions and types are as in E-Links; parents and jurisdictions resolve and agree.

**Independent Test**: quickstart § 3 rows 11–14. The build-time checks are T015–T016.

- [X] T030 [US4] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac012TheHierarchyHasTwoLevels`: exactly `TOP_LEVEL_IDS` have `parent_category_id` present and `null`; every other record's parent is one of 35, 43 and 48, which have 7, 4 and 35 children; no record is its own parent and every parent is top-level (AC-012)
- [X] T031 [US4] In the same file add `ac013JurisdictionsResolveAndAgreeWithTheParent`: every `jurisdiction_id` is a served jurisdiction id (loaded over HTTP), the counts per jurisdiction are 30: 3, 34: 49, 84: 1, 87: 1, every child's `jurisdiction_id` equals its parent's, and 88 is the only record with 87 (AC-013); and add `fr014OtherTypesDoNotCarryTheParentCategoryField`, parameterised over `APPOINTMENT_TITLES`, `BASE_LOCATIONS`, `CONTRACT_TYPES`, `GENDERS`, `JUDICIARY_ROLES`, `LOCATIONS`, `LOCATION_TYPES`, `JURISDICTIONS` and `TICKETS`: the body does not contain `parent_category_id` and `results` is not empty (FR-014, EC-004); and in src/functionalTest/java/…/jo/TicketsFunctionalTest.java add `TICKET_CATEGORIES` (with its static import) to the `@ValueSource` of `ac012OtherTypesDoNotCarryTheTicketCategoryField`, so 010's check covers every other served type (FR-014's counterpart for `ticket_category_id`)
- [X] T032 [US4] In the same file add `ac014TypeIdsAreCopied`: every record has a non-null integral `type_id`, with counts 1: 3, 2: 3, 3: 46, 4: 1, 5: 1; and note in specs/011-ticket-categories-reference-data/plan.md § Implementation notes that AC-014's build-time half (the completeness check treating `ticket_categories.type_id` as a reference to the unserved `ticket_category_types`, and location data's `type_id` as before) is met by T016 (AC-014, FR-020, DF-10)

**Checkpoint**: US4 complete.

---

## Phase 7: User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: quickstart § 3 rows 18–19.

- [X] T033 [US5] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac015MissingTokenIsRejected` over the collection, `/31`, `/34` and `/abc`, and `ac016InvalidTokenIsRejectedWithTheSameMessage` over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` on the collection, `/31` and `/abc` (AC-015, AC-016)

**Checkpoint**: All P1 stories complete.

---

## Phase 8: User Story 6 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `ticket_category` is rejected like any unsupported name.

**Independent Test**: quickstart § 3 rows 16–17.

- [X] T034 [US6] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac017TheSingularCollectionIsUnsupported`, `ac018TheSingularSingleRecordRouteIsUnsupported` over `31`, `34` and `abc`, and `ac019AuthenticationIsCheckedBeforeTheSingularName` over both singular paths without a token and with `Bearer wrong-token` (AC-017, AC-018, AC-019)

**Checkpoint**: US6 complete.

---

## Phase 9: User Story 7 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and the last unbuilt type behave exactly as before.

**Independent Test**: 002's to 010's acceptance tests pass; the 11 unsupported names return `400` (quickstart § 3 rows 20–21).

- [X] T035 [US7] Confirm with `git diff --quiet origin/main -- src/functionalTest/resources/golden/` that the nine existing golden files are unchanged and only `ticket_categories.json` is new (AC-020, SC-008)
- [X] T036 [US7] In src/functionalTest/java/…/jo/TicketCategoriesFunctionalTest.java add `ac022QueryParametersReturn400` over `?parent_category_id=48`, `?page=2`, `?x=` and `/31?name=Civil`, and the inherited `ec011UnmatchedPathsReturn404` (trailing slash, `/31/`, `/31/extra`), `ec011OtherMethodsReturn405`, `ec011UnacceptableMediaTypesReturn406` and `ec011LeadingZerosAndOversizedIds` (`/031` → 31; `/99999999999999999999` → `404`), as in TicketsFunctionalTest. AC-021 and EC-010 are covered by T014 (AC-022, EC-011)
- [X] T037 [US7] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T014's list and example changes, T019's rename and T031's extra `ac012` parameter (AC-020)

**Checkpoint**: US7 complete — no regressions.

---

## Phase 10: User Story 8 - Maintainer Adds the Type With One Generic Change (Priority: P2)

**Goal**: The only main-code change is the generic field, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; the main-code diff lists only the planned files (quickstart § 1–2).

- [X] T038 [P] [US8] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R9; FR-018, AC-024): add `"ticket_categories"` to the fragments, with no exemption, and extend the comment above the list to include ticket categories' plural fragment; add `theGuardRecognisesEveryTicketCategoriesSpelling` over `ticket_categories`, `TicketCategories`, `TICKET_CATEGORIES`, `ticket categories`, `myTicketCategories` and `XTICKET_CATEGORIES` (matches), and `theGuardIgnoresTheGenericTicketCategoryFields` over `ticket_category_id`, `ticketCategoryId`, `TICKET_CATEGORY_ID`, `parent_category_id`, `parentCategoryId` and `Id of the record's ticket category` (no match)
- [X] T039 [US8] Run `git diff --stat origin/main -- src/main/java` (expect exactly `ReferenceField`, `ReferenceDataRecord`, `ReferenceDataResponse`, `ReferenceDataMapper` and `ReferenceDataProperties`), `git diff --stat origin/main -- src/main/resources` (expect application.yml and ticket_categories.json only) and `git grep -n -i -E 'ticket_categories|ticket categories|TicketCategories' -- src/main/java` (expect no output), and record the result and AC-024/AC-025's coverage (T002–T008, T013) in specs/011-ticket-categories-reference-data/plan.md § Implementation notes (AC-023, SC-009)

**Checkpoint**: US8 complete.

---

## Phase 11: User Story 9 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise ticket categories from Postman.

**Independent Test**: newman passes (quickstart § 5).

- [X] T040 [US9] In postman/ctam-jomockapi.postman_collection.json, inserting text with a throwaway scratchpad script after round-tripping the "Tickets" folder (as 008 to 010 did) (research R10; AC-026, FR-016): (a) add a "Ticket Categories" sub-folder after "Tickets", with eleven requests and test scripts: the collection (`200`, 54 results with exactly the nine fields, first `[31, "Civil", 1, 30, null]`, last `[88, "Languages", 5, 87, null]`, 7 ended, 8 with a null parent); `/31` (top-level, parent `null`); `/56` (child, parent 48); `/38` (ended, parent 35); `/34` (`404`); `/abc` (`400`); `?parent_category_id=48` (`400`); trailing slash (`404`); missing token (`401`, `WWW-Authenticate: Bearer`); invalid token (`401`); `ticket_category` (`400`, no `results`); (b) rename "GET /api/v1/reference_data/ticket_categories - unsupported (contract-valid, out of scope)" to "GET /api/v1/reference_data/ticket_category_types - unsupported (contract-valid, out of scope)" and change its `raw` URL and `path`, tests unchanged. Confirm with `git diff --stat` that the change is insertions plus those three lines
- [X] T041 [US9] Start the app (`./gradlew bootRun`; stop any instance already on :8080 first) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/011-ticket-categories-reference-data/plan.md § Implementation notes (AC-026)

**Checkpoint**: All user stories complete.

---

## Phase 12: Polish & Cross-Cutting Concerns

- [X] T042 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `TICKET_CATEGORIES` (with its static import) to the collection paths of the per-collection p95 guard (FR-017, SC-010)
- [X] T043 [P] Update README.md (FR-016; research R11): the Status bullet becomes ten types, "from appointment titles to ticket categories", up to `specs/011-ticket-categories-reference-data/`; "Ten types are configured"; a `ticket_categories` row (`54, one per source row (53 distinct names)`; `none: ticket_category returns 400`); in the jurisdictions paragraph, replace "Skills (87) is served although no shipped record refers to it." with "Skills (87) is referenced only by the ticket category Languages."; in the tickets paragraph, replace the two forward-reference sentences ("Ticket categories aren't served yet, … (constitution, Principle IV).") with "Every `ticket_category_id` is the id of a served ticket category."; add after it a paragraph: "Ticket-category records keep the E-Links ids too (31 to 88, with gaps). Each carries `type_id`, `jurisdiction_id` and `parent_category_id`, after `name`. Categories form two levels: the 8 top-level categories have `"parent_category_id": null`, and every other category's parent is top-level, in the same jurisdiction. `type_id` is the id of a ticket category type; those aren't served yet, so it is a forward reference that the feature serving them must make resolve (constitution, Principle IV). "Immigration and Asylum Chamber" is in both tiers."; the configuration example's `reference-fields` line lists the five fields with "optional: any of the five"; the guard bullet's example becomes "(as `jurisdictions`, `tickets` and `ticket_categories` do, because of `jurisdiction_id` and `ticket_category_id`)"
- [X] T044 [P] Update CLAUDE.md: § Current repo state gains a `011-ticket-categories-reference-data` line (ticket categories via configuration, a fixture and one generic capability: a fifth reference field, `parent_category_id`, nullable, contract deviation D-6; 54 records keeping the extract's ids 31 to 88, 7 ended, a two-level hierarchy; three new `ReferenceDataReferencesTest` rows close 010 DF-8 and check parents (with the cycle test) and jurisdictions, and an agreement row checks each child's jurisdiction equals its parent's, skipping top-level categories; `type_id` is a forward reference to `ticket_category_types` (DF-10), handled by a per-type target override in `ReferenceDataReferencesTest` and a per-type pin in `ReferenceDataFixtureRulesTest`, test code only; jurisdiction 87 is now referenced; the singular `ticket_category` returns `400`; `NoTypeSpecificCodeTest` widened to the plural `ticket_categories`); the 010 line's "`ticket_category_id` is a forward reference to the unserved `ticket_categories` (DF-8)" becomes "… (DF-8, closed by 011)"; § Reference data adds ticket-category names, ids, `type_id`, `jurisdiction_id` and `parent_category_id` values to the public values and `specs/011-ticket-categories-reference-data/data-model.md` § 4 to the generation rules
- [X] T045 [P] In specs/002-reference-data-api/contracts/reference-data-api.md add "Extended by 011: see [the ticket categories contract delta](../../011-ticket-categories-reference-data/contracts/reference-data-api-ticket-categories.md)." after the 010 pointer on the attribute-name line (line 44), and "Extended by 011: optional, nullable `parent_category_id`; see [the ticket categories contract delta § 2](../../011-ticket-categories-reference-data/contracts/reference-data-api-ticket-categories.md)." at the end of the `ReferenceDataResponse` line (line 153); in specs/002-reference-data-api/quickstart.md § 3 change row 5 from `$BASE/ticket_categories` to `$BASE/ticket_category_types`; in specs/010-tickets-reference-data/spec.md add to the DF-8 row "Closed by 011 ([011 FR-011](../011-ticket-categories-reference-data/spec.md); `ReferenceDataReferencesTest`)."
- [X] T046 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite counts, JaCoCo coverage and each collection's strict p95 in specs/011-ticket-categories-reference-data/plan.md § Implementation notes (FR-017, SC-010)
- [X] T047 Walk through specs/011-ticket-categories-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun` (stop any instance already on :8080 first), mark the spec's Status as Implemented in specs/011-ticket-categories-reference-data/spec.md, and record that every expected result matched (or the differences) in specs/011-ticket-categories-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story.
  - T002–T005 (tests first) before T006–T007 (main code); T004 before T005.
  - T008 after T007; T009 after T002–T008.
  - T010 before T011; T012–T018 after T011; T019 last.
- **User stories (Phases 3–11)**: all after Phase 2. They add methods to the same functional test class, so run them in order US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 → US9, or have one person own that file. T038 (guard) and T040 (Postman) touch other files and can run alongside.
- **Polish (Phase 12)**: after the stories; T042–T045 in parallel, then T046, then T047.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2–US5 (P1)**, **US6–US8 (P2)**, **US9 (P3)**: each needs only Phase 2 functionally; US3's and US4's build-time halves are T015–T016, and US8's start-up and schema half is T002–T008.

### Parallel opportunities

- Phase 2, tests first: T002, T003 and T004.
- Phase 2, the type: T012, T013, T014, T015 and T016.
- Stories: T038 and T040 alongside the functional-test tasks.
- Polish: T042–T045.

### Parallel example: Phase 2 test updates for the type

```text
T012 ContractPaths                        — TICKET_CATEGORIES constant
T013 ReferenceDataOpenApiContractTest     — enum and description
T014 ReferenceDataFunctionalTest          — unsupported list and auth example
T015 ReferenceDataFixtureRulesTest        — row; per-type pins; JURISDICTION_IDS + 87
T016 ReferenceDataReferencesTest          — target override; 3 rows; agreement; null via
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (the generic field test-first, then the type live, with the build green).
2. Phase 3 (US1): the collection works and is tested. **Stop and validate**: quickstart § 3 rows 1–4.

### Incremental delivery

3. US2–US5 (remaining P1) → US6–US8 (P2) → US9 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extracts and the generating script stay out of the repository; check `git status` before every commit.
- After editing Javadoc, check that no line exceeds 120 characters (010's first gate failed Checkstyle on two joined lines).
