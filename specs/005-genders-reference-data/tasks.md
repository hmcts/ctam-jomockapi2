---

description: "Task list for 005-genders-reference-data"
---

# Tasks: Reference Data API — Genders

**Input**: Design documents from `/specs/005-genders-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-genders.md](./contracts/reference-data-api-genders.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-015, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. There is no production code change at all (FR-003, SC-007), so the user-story work *is* test work: the tests prove the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US7) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US4), then P2 (US3, US5, US6), then P3 (US7).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/resources/` only — **no file under `src/main/java` changes** (AC-017)
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; functional helpers: `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java`

Expected values come from [data-model.md § 3](./data-model.md) and the [contract delta](./contracts/reference-data-api-genders.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. Postman uses only the environment variables `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [ ] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/005-genders-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Ship the type (configuration and data) and keep the build green. Adding the configuration immediately changes what existing tests assert (research R3), so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

- [ ] T002 Generate src/main/resources/reference-data/genders.json with a throwaway script kept outside the repository (never committed), following data-model.md § 3 rules 1–8 exactly: from the gitignored local gender extract (`joh-elinks-api/ReferenceData/`) read as UTF-8, take `name` of every row (4 rows), ignoring the extract's ids and dates; sort names by code point; `id = 10 × position`; ended iff `position % 7 == 0` (none); every record `end_date null`, `updated_at 2024-06-03T10:30:00Z`, `created_at 2024-01-15T09:00:00Z`, `start_date 2024-01-01`; two-space indent, field order `id, name, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, trailing newline. Before continuing, confirm it equals data-model.md § 3's full dataset table: ids 10 Female, 20 Male, 30 Other, 40 Prefer not to say, all open
- [ ] T003 Append the type entry after `contract_types` under `jo.reference-data.types` in src/main/resources/application.yml: `name: genders` and `fixture: classpath:reference-data/genders.json`, with no `aliases`, `reference-fields`, `boolean-fields` or `unique-names` keys (research R1)
- [ ] T004 [P] Add `public static final String GENDERS = "/api/v1/reference_data/genders";` to src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [ ] T005 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: both `attribute_name` `enum` assertions become exactly `appointment_titles, base_locations, contract_types, genders, appointment_title`; the description becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders. Also supports deprecated values: appointment_title`; leave every `ReferenceDataResponse` assertion unchanged (contract delta § 1; AC-019, FR-011)
- [ ] T006 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java: (a) in `UnsupportedTypeTests`, remove only `genders` from the contract-name list, so it holds the 17 still-unsupported names (`gender` stays), add `Genders`, `GENDERS` and `genders_` to the case variants, and update the comment to "the other 17 E-Links attribute names: seven canonical types, their seven deprecated aliases, and base_location, contract_type and gender, which 003–005 don't serve" plus a pointer to 005 AC-015 and EC-007; (b) in `AuthenticationTests`, change the CSV row `"GET, /api/v1/reference_data/genders"` to `"GET, /api/v1/reference_data/judiciary_roles"`, so the case still shows authentication is checked before an unsupported type (research R3)
- [ ] T007 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file (research R3)
- [ ] T008 Add a row to `fixtures()` in src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java: `new Fixture("genders.json", 4, 4, Map.of(), List.of(point(10, "Female", null), point(20, "Male", null), point(30, "Other", null), point(40, "Prefer not to say", null)))` (research R2; FR-008, FR-009)
- [ ] T009 Create src/functionalTest/java/…/jo/GendersFunctionalTest.java mirroring src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "005's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.GENDERS`, an `ALIAS` constant `/api/v1/reference_data/gender`, a `GENDER_FIELDS` list of 002's six fields, and private helpers `assertRecord(record, name)` (name, null `end_date`, `updated_at` 2024-06-03T10:30:00Z), `assertUnauthorized(response)`, `byId(results)` and `nameOf(path)`; story phases add test methods directly to this class
- [ ] T010 Run `./gradlew check -PskipOwasp`: every suite green, including `ReferenceDataFixtureRulesTest`, `NoTypeSpecificCodeTest` (unchanged so far), the earlier types' functional tests, and the JaCoCo gate

**Checkpoint**: The type is live; existing tests reflect four supported types; the build is green; no main Java file changed.

---

## Phase 3: User Story 1 - Integrator Retrieves All Genders (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 4 genders, deterministically.

**Independent Test**: `GET /api/v1/reference_data/genders` with a valid token returns `200` and 4 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–3).

- [ ] T011 [US1] In src/functionalTest/java/…/jo/GendersFunctionalTest.java add `ac001ReturnsAll4GendersInAscendingIdOrder`: `200`, `application/json`, `results` of 4 records with ids 10, 20, 30, 40, each with exactly 002's six fields in order (AC-001, FR-005)
- [ ] T012 [US1] In the same file add `ac002RecordsMatchTheReferencePoints`: the four records are Female, Male, Other and Prefer not to say (ids 10–40), each open with `updated_at` 2024-06-03T10:30:00Z, `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01 (AC-002)
- [ ] T013 [US1] In the same file add `ac003NoGenderIsEndedAndNamesAreServedExactly`: every `end_date` is JSON null (EC-004), and id 40's name is exactly "Prefer not to say" (EC-002) (AC-003)
- [ ] T014 [US1] Capture the collection response from a running instance (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/genders.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/genders`). The file must be the compact body exactly as served, with no reformatting and no trailing newline, because `ReferenceDataHttp.golden()` compares bytes. Check it matches the fixture's ids and names; then add `ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/GendersFunctionalTest.java, as in ContractTypesFunctionalTest (AC-004)

**Checkpoint**: US1 complete — the MVP: integrators can load genders.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Gender by ID (Priority: P1)

**Goal**: The single-record route returns one gender and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/genders/20` returns `200` with the Male record (quickstart § 3 rows 4–8).

- [ ] T015 [US2] In src/functionalTest/java/…/jo/GendersFunctionalTest.java add `ac005ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 10, 20, 30 and 40: `200`, a single object (no `results`), equal to the collection element with that id (AC-005)
- [ ] T016 [US2] In the same file add `ac006UnknownIdsReturn404` over `15`, `50` and `999999` (`404`, record-not-found message) and `ac007MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message) (AC-006, AC-007, EC-005)
- [ ] T017 [US2] In the same file add `ac008IdsAreScopedToTheirType`: id 10 returns "Female" under `genders`, "Acting Senior Coroner" under `appointment_titles`, "Aberconwy" under `base_locations` and "Fee-paid" under `contract_types` (AC-008, EC-006)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each gender route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 11–12).

- [ ] T018 [US4] In src/functionalTest/java/…/jo/GendersFunctionalTest.java add `ac012MissingTokenIsRejected`, parameterised over the collection, `/20`, `/15` and `/abc`: `401`, shared shape, `WWW-Authenticate: Bearer`, no `results` (AC-012)
- [ ] T019 [US4] In the same file add `ac013InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty): `401` with the same message as a missing token, on the collection, `/20` and `/abc` (AC-013)

**Checkpoint**: All P1 stories complete.

---

## Phase 6: User Story 3 - The Deprecated Alias Is Not Served (Priority: P2)

**Goal**: `gender` is rejected like any unsupported name.

**Independent Test**: `gender` and `gender/{id}` return `400` with a token and `401` without (quickstart § 3 rows 9–10).

- [ ] T020 [US3] In src/functionalTest/java/…/jo/GendersFunctionalTest.java add `ac009TheAliasCollectionIsUnsupported` (`400`, unsupported-attribute message, no `results`), `ac010TheAliasSingleRecordRouteIsUnsupported` over `20`, `15` and `abc` (all `400` with the unsupported-attribute message: the type is checked before the id) and `ac011AuthenticationIsCheckedBeforeTheAlias` over the two alias paths without a token and with `Bearer wrong-token` (`401`) (AC-009, AC-010, AC-011)

**Checkpoint**: US3 complete.

---

## Phase 7: User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unbuilt type behave exactly as before.

**Independent Test**: 002's, 003's and 004's acceptance tests pass unchanged; the 17 unsupported names return `400` (quickstart § 3 rows 14–15).

- [ ] T021 [US5] Confirm with `git diff main -- src/functionalTest/resources/golden/` that the existing golden files (`appointment_titles.json`, `base_locations.json`, `contract_types.json`) are unchanged and only `genders.json` is new (AC-014, SC-006)
- [ ] T022 [US5] In src/functionalTest/java/…/jo/GendersFunctionalTest.java add `ac016QueryParametersReturn400`, parameterised over `?name=Male`, `?page=2`, `?x=` and `/20?name=Male`: `400`, query-parameter message; and the inherited checks `ec009UnmatchedPathsReturn404` (trailing slash, `/20/`, `/20/extra`), `ec009OtherMethodsReturn405` (POST, PUT, PATCH, DELETE on the collection and `/20`) and `ec009LeadingZerosAndOversizedIds` (`/020` → id 20; `/99999999999999999999` → `404`), as in ContractTypesFunctionalTest. Unacceptable media types, the order of checks and unexpected failures (002 EC-009 to EC-012) are type-agnostic and covered by `ErrorShapeIntegrationTest` and 002's functional tests. AC-015 is covered by T006 (AC-016, EC-009)
- [ ] T023 [US5] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T006's list and example changes (AC-014)

**Checkpoint**: US5 complete — no regressions.

---

## Phase 8: User Story 6 - Maintainer Adds the Type With Configuration and Data Only (Priority: P2)

**Goal**: The change is configuration and data only, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; `git diff --stat main -- src/main/java` is empty (quickstart § 2).

- [ ] T024 [P] [US6] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java add `"gender"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, with no exemption (spec Clarifications); add a parameterised recognition test for `gender`, `genders`, `GENDER`, `Gender`, `GenderHelper`, `myGender`, `XGENDER`, `genderId` and `gender_id`, and a non-matching test for `engender`, `agender`, `transgender` and `agenda` (research R5; FR-016, AC-018)
- [ ] T025 [US6] Run `git diff --stat main -- src/main/java` (expect no output) and `git diff --stat main -- src/main/resources` (expect application.yml and genders.json only), and record the result in specs/005-genders-reference-data/plan.md § Implementation notes (AC-017, SC-007)
- [ ] T026 [US6] Confirm AC-019 (OpenAPI lists `genders`, not `gender`, schema unchanged) is covered by T005, and note the mapping in specs/005-genders-reference-data/plan.md § Implementation notes

**Checkpoint**: US6 complete.

---

## Phase 9: User Story 7 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise genders from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [ ] T027 [US7] In postman/ctam-jomockapi.postman_collection.json, inserting text so existing formatting is untouched (as 004 did): (a) add a "Genders" sub-folder under "Reference Data", after "Contract Types", with nine requests and test scripts: the collection (`200`, 4 results with 002's six fields, every `end_date` null, first Female, last "Prefer not to say", ids ascending); `/20` (`200`, Male); `/15` (`404`); `/abc` (`400`); `?name=Male` (`400`); trailing slash (`404`, "Resource not found."); missing token (`401`, `WWW-Authenticate: Bearer`); invalid token (`{{invalidBearerToken}}`, `401`); `gender` (`400`, "deprecated alias not served", no `results`); (b) rename the request "GET /api/v1/reference_data/genders - unsupported (contract-valid, out of scope)" to "GET /api/v1/reference_data/judiciary_roles - unsupported (contract-valid, out of scope)" and change its `raw` URL and `path` to `judiciary_roles`, leaving its tests unchanged (AC-020, FR-012; research R7)
- [ ] T028 [US7] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/005-genders-reference-data/plan.md § Implementation notes (AC-020)

**Checkpoint**: All user stories complete.

---

## Phase 10: Polish & Cross-Cutting Concerns

- [ ] T029 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `GENDERS` to the collection paths of the per-collection p95 guard (FR-014, SC-008; research R6)
- [ ] T030 [P] Update README.md § Reference data: four configured types, with a `genders` row (4 records; no alias served: `gender` returns `400`); leave § Adding a reference-data type unchanged, since it is still accurate (FR-013; research R7)
- [ ] T031 [P] Update CLAUDE.md: § Current repo state gains a `005-genders-reference-data` line (genders via configuration and fixture only, no main-code change; 4 records, none ended; alias not served; DF-3 for People `gender_id` and the guard exception; `NoTypeSpecificCodeTest` widened to `gender`); § Reference data adds gender names to the public values taken from the extracts
- [ ] T032 [P] Add a one-line pointer to specs/005-genders-reference-data/contracts/reference-data-api-genders.md in specs/002-reference-data-api/contracts/reference-data-api.md next to the 003 and 004 pointers, and a one-line note under specs/002-reference-data-api/quickstart.md § 3's table that `genders` is served since 005, so a still-unsupported name such as `judiciary_roles` shows the same `400` (research R7)
- [ ] T033 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite test counts, JaCoCo line/branch coverage and each collection's strict p95 in specs/005-genders-reference-data/plan.md § Implementation notes (FR-014, FR-015, SC-008)
- [ ] T034 Walk through specs/005-genders-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun`, mark the spec's Status as Implemented in specs/005-genders-reference-data/spec.md, and record that every expected result matched (or the differences) in specs/005-genders-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story. T002 before T003 (the type can't load without its fixture); T004–T009 after T003; T010 last.
- **User stories (Phases 3–9)**: all after Phase 2. They add methods to the same functional test class, so run them in order US1 → US2 → US4 → US3 → US5 → US6 → US7, or have one person own that file. T024 (guard) and T027 (Postman) touch other files and can run alongside.
- **Polish (Phase 10)**: after the stories; T029–T032 in parallel, then T033, then T034.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2, US4 (P1)**, **US3, US5, US6 (P2)**, **US7 (P3)**: each needs only Phase 2 functionally; US7's newman run (T028) is most useful once US1–US6 are green.

### Parallel opportunities

- Phase 2: T004, T005 and T006 (different files).
- Stories: T024 (guard) and T027 (Postman) alongside the functional-test tasks.
- Polish: T029–T032.

### Parallel example: Phase 2 test updates

```text
T004 ContractPaths                        — GENDERS constant
T005 ReferenceDataOpenApiContractTest     — enum and description
T006 ReferenceDataFunctionalTest          — unsupported list and auth example
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (build green with the type live).
2. Phase 3 (US1): the collection works and is tested. **Stop and validate**: quickstart § 3 rows 1–3.

### Incremental delivery

3. US2 and US4 (remaining P1) → US3, US5, US6 (P2) → US7 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
