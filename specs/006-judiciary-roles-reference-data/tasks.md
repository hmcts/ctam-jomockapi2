---

description: "Task list for 006-judiciary-roles-reference-data"
---

# Tasks: Reference Data API — Judiciary Roles

**Input**: Design documents from `/specs/006-judiciary-roles-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-judiciary-roles.md](./contracts/reference-data-api-judiciary-roles.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-015, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. There is no production code change at all (FR-003, SC-008), so the user-story work *is* test work: the tests prove the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US7) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US4), then P2 (US3, US5, US6), then P3 (US7).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/resources/` only — **no file under `src/main/java` changes** (AC-018)
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; functional helpers: `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java` (`get`, `getAccepting`, `nameOf`, `assertError`, `assertUnauthorized`, `byId`, `golden`, `json`, `CONTRACT_FIELDS`)

Expected values come from [data-model.md § 3](./data-model.md) and the [contract delta](./contracts/reference-data-api-judiciary-roles.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. Postman uses only the environment variables `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`. Names containing `’` (U+2019) and `–` (U+2013) are written literally in Java, JSON and Postman sources, never as escapes.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [X] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/006-judiciary-roles-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Ship the type (configuration and data) and keep the build green. Adding the configuration immediately changes what existing tests assert (research R4), so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

- [X] T002 Generate src/main/resources/reference-data/judiciary_roles.json with a throwaway script kept outside the repository (the session scratchpad; never committed), following data-model.md § 3 rules 1–8 exactly: from the gitignored `joh-elinks-api/ReferenceData/REF_JudiciaryRole.csv` read as UTF-8 (strip a BOM if present), take `name` of every row (164 rows) unchanged, ignoring the extract's ids and every date column; sort names by code point; `id = 10 × position`; ended iff `position % 7 == 0` (`end_date 2025-03-31`, `updated_at 2025-04-01T08:00:00Z`), otherwise `end_date null` / `updated_at 2024-06-03T10:30:00Z`; all `created_at 2024-01-15T09:00:00Z` / `start_date 2024-01-01`; two-space indent, field order `id, name, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, non-ASCII written literally (e.g. Python `ensure_ascii=False`), trailing newline. Before continuing, have the script confirm, and record in specs/006-judiciary-roles-reference-data/plan.md § Implementation notes: 164 records and 164 distinct names; the sorted list of names in the file equals the sorted `name` column of the extract as code points (SC-003); 7 names contain non-ASCII characters and the file contains no `\u`; 23 ended records, ids 70 to 1610; and every row of data-model.md § 3's Reference points table (research R2)
- [X] T003 Append the type entry after `genders` under `jo.reference-data.types` in src/main/resources/application.yml: `name: judiciary_roles` and `fixture: classpath:reference-data/judiciary_roles.json`, with no `aliases`, `reference-fields`, `boolean-fields` or `unique-names` keys (research R1)
- [X] T004 [P] Add `public static final String JUDICIARY_ROLES = "/api/v1/reference_data/judiciary_roles";` after `GENDERS` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [X] T005 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: the `attribute_name` `enum` list becomes exactly `appointment_titles, base_locations, contract_types, genders, judiciary_roles, appointment_title`; the description becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles. Also supports deprecated values: appointment_title`, wrapped to stay within Checkstyle's 120-character limit; leave every `ReferenceDataResponse` assertion unchanged (contract delta § 1; AC-020, FR-011)
- [X] T006 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java: (a) in `UnsupportedTypeTests`, remove only `judiciary_roles` from the contract-name list, so it holds the 16 still-unsupported names (`judiciary_role` stays), add `Judiciary_roles`, `JUDICIARY_ROLES`, `judiciary-roles`, `judiciaryRoles` and `judiciary_roles_` to the case variants, change the list comment to "The other 16 E-Links attribute names: six canonical types, their six deprecated aliases, and base_location, contract_type, gender and judiciary_role, which 003–006 don't serve", and extend the "Also covers" note with 006 AC-016 and EC-011 (specs/006-judiciary-roles-reference-data); (b) in `AuthenticationTests`, change the CSV row `"GET, /api/v1/reference_data/judiciary_roles"` to `"GET, /api/v1/reference_data/jurisdictions"`, so the case still shows authentication is checked before an unsupported type (research R4; AC-015)
- [X] T007 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file (research R4)
- [X] T008 Add a row to `fixtures()` in src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java: `new Fixture("judiciary_roles.json", 164, 164, Map.of(), List.of(…))` with the 13 reference points of data-model.md § 3 as `point(id, name, endDate)`, using `END_DATE` for 70, 140, 1400 and 1610 and `null` for the others, names copied character for character (research R2; FR-008, FR-009)
- [X] T009 In the same file, add a generic test `fixturesWriteNonAsciiCharactersLiterally` (or equivalent name in the file's style) that, for every shipped fixture the test already discovers, reads the raw file as UTF-8 and asserts it contains no `\u` escape sequence, with the fixture name in the failure message; it names no type (research R3; FR-008, FR-015)
- [X] T010 Create src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java mirroring src/functionalTest/java/…/jo/GendersFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "006's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.JUDICIARY_ROLES`, a `SINGULAR` constant `/api/v1/reference_data/judiciary_role` (Javadoc: "The singular name, which 006 doesn't serve: deprecated names are out of scope."), constants `OPEN_UPDATED_AT` (2024-06-03T10:30:00Z), `ENDED_UPDATED_AT` (2025-04-01T08:00:00Z) and `END_DATE` (2025-03-31), and a private helper `assertRecord(record, name, ended)` checking the name, `end_date` and `updated_at`; story phases add test methods directly to this class
- [X] T011 Run `./gradlew check -PskipOwasp`: every suite green, including `ReferenceDataFixtureRulesTest` (five fixtures and the new literal-character check), `NoTypeSpecificCodeTest` (unchanged so far), the earlier types' functional tests, and the JaCoCo gate

**Checkpoint**: The type is live; existing tests reflect five supported types; the build is green; no main Java file changed.

---

## Phase 3: User Story 1 - Integrator Retrieves All Judiciary Roles (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 164 judiciary roles, exactly as named in the source, deterministically.

**Independent Test**: `GET /api/v1/reference_data/judiciary_roles` with a valid token returns `200` and 164 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–4).

- [X] T012 [US1] In src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java add `ac001ReturnsAll164JudiciaryRolesInAscendingIdOrder`: `200`, `application/json`, `results` of 164 records with ids 10, 20, …, 1640 in order, each with exactly `CONTRACT_FIELDS` in order (AC-001, FR-005)
- [X] T013 [US1] In the same file add `ac002RecordsMatchTheReferencePoints`: using `byId`, each of the 13 Reference Points rows has its name, `end_date` and `updated_at` (via `assertRecord`), and every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01 (AC-002)
- [X] T014 [US1] In the same file add `ac003NamesAreServedExactly`: on the raw body, assert the substrings `"name":"Magistrates’ Liaison Judge"`, `"name":"Deputy Chairman – Advisory Committee"`, `"name":"Judge in Charge of Jury List (Queen's Bench)"`, `"name":"Principle Judge - Residential Property"`, `"name":"Judicial Commissioner Investigatory Powers Commiss"`, `"name":"Vice President Kings Bench"`, `"name":"Vice President of King's Bench"` and `"name":"Vice President Queens Bench"` (near-duplicates kept apart, and a Queen's Bench name served); on the parsed records, the seven non-ASCII names of data-model.md § 3 at their ids (140, 150, 450, 580, 960, 990, 1180) (AC-003, EC-002 to EC-006)
- [X] T015 [US1] In the same file add `ac004Exactly23RecordsAreEnded`: on the raw body, count exactly 23 `"end_date":"2025-03-31"` and 141 `"end_date":null` (counts, not absence checks, so a reformatted body fails); on the parsed records, the ended ids are exactly the multiples of 70 from 70 to 1610, each with `updated_at` 2025-04-01T08:00:00Z, and "Regional Judge" and "Vice President Queens Bench" (end-dated in the extract) are open (AC-004, EC-007)
- [X] T016 [US1] Capture the collection response from a running instance (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/judiciary_roles.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/judiciary_roles`). The file must be the compact body exactly as served, with no reformatting and no trailing newline, because `ReferenceDataHttp.golden()` compares bytes. Check it has 164 records matching the fixture's ids and names and contains `’` and `–` as UTF-8 characters; then add `ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java, as in GendersFunctionalTest (AC-005)

**Checkpoint**: US1 complete — the MVP: integrators can load judiciary roles.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Judiciary Role by ID (Priority: P1)

**Goal**: The single-record route returns one judiciary role and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/judiciary_roles/960` returns `200` with "Judge in Charge of Jury List (King’s Bench)" (quickstart § 3 rows 5–13).

- [X] T017 [US2] In src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 10, 70, 150, 960, 970, 1070 and 1640: `200`, a single object (no `results`), equal to the collection element with that id (AC-006)
- [X] T018 [US2] In the same file add `ac007UnknownIdsReturn404` over `15`, `1650` and `999999` (`404`, record-not-found message) and `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message) (AC-007, AC-008, EC-009)
- [X] T019 [US2] In the same file add `ac009IdsAreScopedToTheirType`: id 10 returns "Acting Designated Civil Judge" under `judiciary_roles`, "Acting Senior Coroner" under `appointment_titles`, "Aberconwy" under `base_locations`, "Fee-paid" under `contract_types` and "Female" under `genders` (AC-009, EC-010)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each judiciary role route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 16–17).

- [X] T020 [US4] In src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java add `ac013MissingTokenIsRejected`, parameterised over the collection, `/20`, `/15` and `/abc`: `assertUnauthorized`, no `results` (AC-013)
- [X] T021 [US4] In the same file add `ac014InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty): `401` with the same message as a missing token, on the collection, `/20` and `/abc` (AC-014)

**Checkpoint**: All P1 stories complete.

---

## Phase 6: User Story 3 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `judiciary_role` is rejected like any unsupported name.

**Independent Test**: `judiciary_role` and `judiciary_role/{id}` return `400` with a token and `401` without (quickstart § 3 rows 14–15).

- [X] T022 [US3] In src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java add `ac010TheSingularCollectionIsUnsupported` (`400`, unsupported-attribute message, no `results`), `ac011TheSingularSingleRecordRouteIsUnsupported` over `20`, `15` and `abc` (all `400` with the unsupported-attribute message: the type is checked before the id) and `ac012AuthenticationIsCheckedBeforeTheSingularName` over the two singular paths without a token and with `Bearer wrong-token` (`401`) (AC-010, AC-011, AC-012)

**Checkpoint**: US3 complete.

---

## Phase 7: User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unbuilt type behave exactly as before.

**Independent Test**: 002's to 005's acceptance tests pass unchanged; the 16 unsupported names return `400` (quickstart § 3 rows 19–20).

- [X] T023 [US5] Confirm with `git diff main -- src/functionalTest/resources/golden/` that the existing golden files (`appointment_titles.json`, `base_locations.json`, `contract_types.json`, `genders.json`) are unchanged and only `judiciary_roles.json` is new (AC-015, SC-007)
- [X] T024 [US5] In src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java add `ac017QueryParametersReturn400`, parameterised over `?name=Judge`, `?page=2`, `?x=` and `/20?name=Judge` (a plain value, so the test doesn't depend on how the HTTP helper encodes `%20`; Postman and the quickstart use `?name=Mentor%20Judge`): `400`, query-parameter message; and the inherited checks, as in GendersFunctionalTest: `ec012UnmatchedPathsReturn404` (trailing slash, `/20/`, `/20/extra`), `ec012OtherMethodsReturn405` (POST, PUT, PATCH, DELETE on the collection and `/20`), `ec012UnacceptableMediaTypesReturn406` (`application/xml` on the collection and `/20`) and `ec012LeadingZerosAndOversizedIds` (`/020` → id 20; `/99999999999999999999` → `404`). The order of checks and unexpected failures (002 EC-009 and EC-010) are type-agnostic and covered by `ErrorShapeIntegrationTest` and 002's functional tests. AC-016 is covered by T006 (AC-017, EC-012)
- [X] T025 [US5] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T006's list and example changes (AC-015)

**Checkpoint**: US5 complete — no regressions.

---

## Phase 8: User Story 6 - Maintainer Adds the Type With Configuration and Data Only (Priority: P2)

**Goal**: The change is configuration and data only, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; `git diff --stat main -- src/main/java` is empty (quickstart § 2).

- [X] T026 [P] [US6] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java add `"judiciary_role"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, with no exemption (FR-016, DF-4); add a parameterised recognition test for `judiciary_role`, `judiciary_roles`, `JUDICIARY_ROLE`, `judiciary role`, `Judiciary Role`, `judiciary-role`, `JudiciaryRole`, `judiciaryRole`, `myJudiciaryRole`, `XJUDICIARY_ROLE`, `judiciary_role_id`, `judiciaryRoleNameId` and `judiciary\n     * role`, and a non-matching test for `judiciary`, `role`, `judicial role`, `JudicialRole`, `judiciary member role` and `nonjudiciary role`, with a comment that the last has the fragment straight after a lower-case letter and so exercises the boundary rule (research R6; AC-019)
- [X] T027 [US6] Run `git diff --stat main -- src/main/java` (expect no output) and `git diff --stat main -- src/main/resources` (expect application.yml and judiciary_roles.json only), and record the result in specs/006-judiciary-roles-reference-data/plan.md § Implementation notes (AC-018, SC-008)
- [X] T028 [US6] Confirm AC-020 (OpenAPI lists `judiciary_roles`, not `judiciary_role`, schema unchanged) is covered by T005 and AC-016 by T006, and note the mapping in specs/006-judiciary-roles-reference-data/plan.md § Implementation notes

**Checkpoint**: US6 complete.

---

## Phase 9: User Story 7 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise judiciary roles from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [X] T029 [US7] In postman/ctam-jomockapi.postman_collection.json, inserting text so existing formatting is untouched (as 004 and 005 did): (a) add a "Judiciary Roles" sub-folder under "Reference Data", after "Genders", with nine requests and test scripts: the collection (`200`, 164 results with 002's six fields, ids ascending, first "Acting Designated Civil Judge", last "Wales Training Course Director (JC)", exactly 23 non-null `end_date` values); `/960` (`200`, name exactly "Judge in Charge of Jury List (King’s Bench)", with `’` written literally); `/15` (`404`); `/abc` (`400`); `?name=Mentor%20Judge` (`400`); trailing slash (`404`, "Resource not found."); missing token (`401`, `WWW-Authenticate: Bearer`); invalid token (`{{invalidBearerToken}}`, `401`); `judiciary_role` (`400`, "singular name not served", no `results`); (b) rename the request "GET /api/v1/reference_data/judiciary_roles - unsupported (contract-valid, out of scope)" to "GET /api/v1/reference_data/jurisdictions - unsupported (contract-valid, out of scope)" and change its `raw` URL and `path` to `jurisdictions`, leaving its tests unchanged (AC-021, FR-012; research R8)
- [X] T030 [US7] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/006-judiciary-roles-reference-data/plan.md § Implementation notes (AC-021)

**Checkpoint**: All user stories complete.

---

## Phase 10: Polish & Cross-Cutting Concerns

- [X] T031 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `JUDICIARY_ROLES` to the collection paths of the per-collection p95 guard (FR-014, SC-009; research R7)
- [X] T032 [P] Update README.md § Reference data: five configured types, with a `judiciary_roles` row (164 records; no alias served: `judiciary_role` returns `400`); leave § Adding a reference-data type unchanged, since it is still accurate (FR-013; research R8)
- [X] T033 [P] Update CLAUDE.md: § Current repo state gains a `006-judiciary-roles-reference-data` line (judiciary roles via configuration and fixture only, no main-code change; 164 records, 23 ended; names served exactly, including `’` and `–`; singular name not served; the generic no-`\u`-escape fixture check; DF-4 for People `judiciary_roles` references and the guard exception; `NoTypeSpecificCodeTest` widened to `judiciary_role`); § Reference data adds judiciary role names to the public values taken from the extracts and `specs/006-judiciary-roles-reference-data/data-model.md` § 3 to the list of generation rules
- [X] T034 [P] Add a one-line pointer to specs/006-judiciary-roles-reference-data/contracts/reference-data-api-judiciary-roles.md in specs/002-reference-data-api/contracts/reference-data-api.md next to the 003–005 pointers; change specs/002-reference-data-api/quickstart.md § 3 row 5 and specs/005-genders-reference-data/quickstart.md § 3 row 14 from `judiciary_roles` to `jurisdictions`, and 005's § 5 sentence "now `judiciary_roles`" to "now `jurisdictions`", so each row still passes when run (research R8)
- [X] T035 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite test counts, JaCoCo line/branch coverage and each collection's strict p95 in specs/006-judiciary-roles-reference-data/plan.md § Implementation notes (FR-014, FR-015, SC-009)
- [X] T036 Walk through specs/006-judiciary-roles-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun`, mark the spec's Status as Implemented in specs/006-judiciary-roles-reference-data/spec.md, and record that every expected result matched (or the differences) in specs/006-judiciary-roles-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story. T002 before T003 (the type can't load without its fixture); T004–T010 after T003, with T009 after T008 (same file); T011 last.
- **User stories (Phases 3–9)**: all after Phase 2. They add methods to the same functional test class, so run them in order US1 → US2 → US4 → US3 → US5 → US6 → US7, or have one person own that file. T026 (guard) and T029 (Postman) touch other files and can run alongside.
- **Polish (Phase 10)**: after the stories; T031–T034 in parallel, then T035, then T036.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2, US4 (P1)**, **US3, US5, US6 (P2)**, **US7 (P3)**: each needs only Phase 2 functionally; US7's newman run (T030) is most useful once US1–US6 are green.

### Parallel opportunities

- Phase 2: T004, T005 and T006 (different files).
- Stories: T026 (guard) and T029 (Postman) alongside the functional-test tasks.
- Polish: T031–T034.

### Parallel example: Phase 2 test updates

```text
T004 ContractPaths                        — JUDICIARY_ROLES constant
T005 ReferenceDataOpenApiContractTest     — enum and description
T006 ReferenceDataFunctionalTest          — unsupported list and auth example
```

### Parallel example: alongside the functional tests

```text
T026 NoTypeSpecificCodeTest               — judiciary_role fragment and spellings
T029 Postman collection                   — Judiciary Roles folder; unsupported example → jurisdictions
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (build green with the type live).
2. Phase 3 (US1): the collection works, its names are exact, and it is tested. **Stop and validate**: quickstart § 3 rows 1–4.

### Incremental delivery

3. US2 and US4 (remaining P1) → US3, US5, US6 (P2) → US7 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extract is read only by T002's throwaway script; nothing in the build reads it (FR-007).
