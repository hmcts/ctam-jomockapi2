---

description: "Task list for 009-jurisdictions-reference-data"
---

# Tasks: Reference Data API — Jurisdictions

**Input**: Design documents from `/specs/009-jurisdictions-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-jurisdictions.md](./contracts/reference-data-api-jurisdictions.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-017, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. There is no main-code change (FR-003, SC-009), so the user-story work *is* test work: the tests prove the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US8) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US5), then P2 (US4, US6, US7), then P3 (US8).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US8)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/resources/` only — **no file under `src/main/java` changes** (AC-020)
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; functional helpers: `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java`

Expected values come from [data-model.md § 3](./data-model.md) and the [contract delta](./contracts/reference-data-api-jurisdictions.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. Postman uses only the environment variables `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [ ] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/009-jurisdictions-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Ship the type (configuration and data) and keep the build green. Configuring `jurisdictions` immediately changes what existing tests assert — the OpenAPI enum, the unsupported-name list, and `ReferenceDataReferencesTest`'s completeness check, which demands the three `jurisdiction_id` rows — so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

- [ ] T002 Generate src/main/resources/reference-data/jurisdictions.json with a throwaway script in the session scratchpad (never committed), following data-model.md § 3 rules 1–6 exactly: read the gitignored local `joh-elinks-api/ReferenceData/REF_Jurisdiction.json` as UTF-8 with a JSON parser; take `id` and `name` from each object of `results`, ignoring every other key; order by ascending `id`; ended iff `position % 7 == 0` (none); every record `end_date null`, `updated_at 2024-06-03T10:30:00Z`, `created_at 2024-01-15T09:00:00Z`, `start_date 2024-01-01`; two-space indent, field order `id, name, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, trailing newline. Before keeping the file, the script MUST check (research R2): the top level is an object whose only key is `results`; 5 entries, each with exactly the keys `id`, `name`, `start_date`, `end_date`, `created_at`, `updated_at`; ids are the integers 30, 34, 84, 86, 87; names non-blank, trimmed and distinct; the written `id` and `name` equal the extract's entry for entry; no record ended; every `jurisdiction_id` in src/main/resources/reference-data/locations.json, base_locations.json and location_types.json is one of the written ids; and the result equals data-model.md § 3's Records table (30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners, 87 Skills). Neither the extract nor the script is committed (FR-006, FR-007, FR-008, FR-009, FR-010)
- [ ] T003 In src/main/resources/application.yml (research R1): append after the `location_types` entry under `jo.reference-data.types` a `name: jurisdictions` entry with the comment `# Keeps the extract's E-Links ids, so every jurisdiction_id resolves (009 DR-1).` and `fixture: classpath:reference-data/jurisdictions.json`, with no `aliases`, declared-field or `unique-names` keys; and reword the comments on the `base_locations`, `locations` and `location_types` entries so `jurisdiction_id` is described as the id of a jurisdiction, checked by ReferenceDataReferencesTest (009), not as a forward reference (no mention of 003 DF-1, 007 DF-5 or 008 DF-7 as open). This makes `jurisdictions` the only new supported name, with 002's behaviour unchanged (FR-001, FR-002, FR-004)
- [ ] T004 [P] Add `public static final String JURISDICTIONS = "/api/v1/reference_data/jurisdictions";` after `LOCATION_TYPES` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [ ] T005 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: `ATTRIBUTE_NAMES` gains `jurisdictions` after `location_types` (so both `enum` assertions become `appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, appointment_title`); the description assertion becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions. Also supports deprecated values: appointment_title`; leave every `ReferenceDataResponse` assertion unchanged (contract delta § 1–2; AC-022, FR-013)
- [ ] T006 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java (research R6): (a) in the unsupported-name `@ValueSource`, remove only `jurisdictions` (so it holds the 13 still-unsupported E-Links names; `jurisdiction` stays), add `Jurisdictions`, `JURISDICTIONS` and `jurisdictions_` to the case variants, update the comment to "The other 13 E-Links attribute names: three canonical types, their three deprecated aliases, and base_location, contract_type, gender, judiciary_role, jurisdiction, location and location_type, which 003–009 don't serve", and add 009 AC-018 and EC-008 (specs/009-jurisdictions-reference-data) to the comment listing covered scenarios; (b) in `AuthenticationTests`, change the CSV row `"GET, /api/v1/reference_data/jurisdictions"` to `"GET, /api/v1/reference_data/tickets"`, so the case still shows authentication is checked before an unsupported type (AC-017)
- [ ] T007 [P] In src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R3; FR-008, FR-009, FR-017): add a row to `fixtures()` after `location_types.json`: `new Fixture("jurisdictions.json", 5, 5, Map.of(), IdRule.SOURCE, List.of(point(30, "Courts", null), point(84, "Magistrates", null), point(86, "Coroners", null), point(87, "Skills", null)))`; keep `JURISDICTION_IDS` as `{30, 34, 84, 86}` and change its Javadoc to "The jurisdictions the shipped location data refers to: 30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners. Skills (87) is served but unreferenced (009 EC-003)."
- [ ] T008 [P] In src/test/java/…/jo/repository/ReferenceDataReferencesTest.java (research R4; FR-011): append to `references()`, after the `type_id` rows, the comment `// Close 003 DF-1, 007 DF-5 and 008 DF-7 for jurisdiction_id.` and the rows `new Reference("locations", "jurisdiction_id", "jurisdictions")`, `new Reference("base_locations", "jurisdiction_id", "jurisdictions")` and `new Reference("location_types", "jurisdiction_id", "jurisdictions")`; leave `agreements()` unchanged (FR-012); rewrite the class Javadoc's forward-reference sentence so it no longer lists 003 DF-1, 007 DF-5 or 008 DF-7 as open, and says that a reference to a type not served yet is a forward reference that the adding feature records as a deferred item (spec Clarifications)
- [ ] T009 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file
- [ ] T010 Create src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java mirroring src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "009's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.JURISDICTIONS`; constants `SINGULAR = "/api/v1/reference_data/jurisdiction"`, `OPEN_UPDATED_AT = "2024-06-03T10:30:00Z"`, `FIELDS` (`id, name, created_at, updated_at, start_date, end_date`) and `RECORDS`, a `Row(id, name)` list of the five records in data-model.md § 3; private helpers `assertRecord(record, row)` (id, name, null `end_date`, `updated_at` `OPEN_UPDATED_AT`, `created_at` 2024-01-15T09:00:00Z, `start_date` 2024-01-01), `assertUnauthorized(response)` and `byId(results)`, as in LocationTypesFunctionalTest; story phases add test methods directly to this class
- [ ] T011 Run `./gradlew check -PskipOwasp`: every suite green, including `ReferenceDataFixtureRulesTest`, `ReferenceDataReferencesTest` (the three new rows pass and the completeness check is satisfied), `NoTypeSpecificCodeTest` (unchanged so far), the earlier types' functional tests, and the JaCoCo gate

**Checkpoint**: The type is live; existing tests reflect eight supported types; every `jurisdiction_id` resolution is checked; the build is green; no main Java file changed.

---

## Phase 3: User Story 1 - Integrator Retrieves All Jurisdictions (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 5 jurisdictions, deterministically.

**Independent Test**: `GET /api/v1/reference_data/jurisdictions` with a valid token returns `200` and 5 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–2).

- [ ] T012 [US1] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java add `ac001ReturnsAll5JurisdictionsInAscendingIdOrder`: `200`, `application/json`, `results` of 5 records with ids 30, 34, 84, 86, 87, each with exactly `FIELDS` in order and no other key (AC-001, FR-005)
- [ ] T013 [US1] In the same file add `ac002RecordsMatchTheRecordsTable`: each record equals its `RECORDS` row via `assertRecord` (AC-002, SC-002)
- [ ] T014 [US1] In the same file add `ac003NoJurisdictionIsEnded`: every `end_date` is JSON null and every `updated_at` is `OPEN_UPDATED_AT` (AC-003, EC-004)
- [ ] T015 [US1] Capture the collection response from a running instance (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/jurisdictions.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/jurisdictions`). The file must be the compact body exactly as served, with no reformatting and no trailing newline, because `ReferenceDataHttp.golden()` compares bytes. Check it matches the fixture's ids and names; then add `ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java, as in LocationTypesFunctionalTest (AC-004)

**Checkpoint**: US1 complete — the MVP: integrators can load jurisdictions.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Jurisdiction by ID (Priority: P1)

**Goal**: The single-record route returns one jurisdiction and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/jurisdictions/84` returns `200` with the Magistrates record (quickstart § 3 rows 3–8).

- [ ] T016 [US2] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java add `ac005ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 30, 34, 84, 86 and 87: `200`, a single object (no `results`), equal to the collection element with that id (AC-005)
- [ ] T017 [US2] In the same file add `ac006UnknownIdsReturn404` over `1`, `31`, `88`, `999` and `999999` (`404`, record-not-found message) and `ac007MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message) (AC-006, AC-007, EC-001)
- [ ] T018 [US2] In the same file add `ac008IdsAreScopedToTheirType`: id 84 returns "Magistrates" under `jurisdictions` and "Central Devon Sub Committee" under `locations`; id 30 returns "Courts" under `jurisdictions` and "Other" under `genders` (AC-008, EC-006)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 3 - Integrator Resolves Every Jurisdiction Reference (Priority: P1)

**Goal**: Every served `jurisdiction_id` names a served jurisdiction, observable over HTTP.

**Independent Test**: Load the four collections and check every `jurisdiction_id` resolves (quickstart § 3 rows 15–16). The build-time check is T008.

- [ ] T019 [US3] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java add `ac009EveryJurisdictionIdResolves`, parameterised over `ContractPaths.LOCATIONS`, `BASE_LOCATIONS` and `LOCATION_TYPES`: load the jurisdiction ids over HTTP, then assert every record of that collection has a `jurisdiction_id` that is one of them, naming the record on failure; also assert the record counts (2,000, 1,462 and 16) so an empty response can't pass (AC-009, SC-004)
- [ ] T020 [US3] In the same file add `ac011SkillsIsServedButUnreferenced`: `/87` returns `200` "Skills", and no record of the locations, base-locations or location-types collections has `jurisdiction_id` 87 (AC-011, EC-003)
- [ ] T021 [US3] Confirm AC-010 is met by T008 (the completeness check `everyReferenceBetweenServedTypesIsChecked` passes with a row for every reference field of every served type, and no new check was added), and note the mapping in specs/009-jurisdictions-reference-data/plan.md § Implementation notes (AC-010, spec Clarifications)

**Checkpoint**: US3 complete — 003 DF-1, 007 DF-5 and 008 DF-7 are closed.

---

## Phase 6: User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each jurisdiction route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 11–12).

- [ ] T022 [US5] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java add `ac015MissingTokenIsRejected`, parameterised over the collection, `/84`, `/31` and `/abc`: `401`, shared shape, `WWW-Authenticate: Bearer`, no `results` (AC-015)
- [ ] T023 [US5] In the same file add `ac016InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty): `401` with the same message as a missing token, on the collection, `/84` and `/abc` (AC-016)

**Checkpoint**: All P1 stories complete.

---

## Phase 7: User Story 4 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `jurisdiction` is rejected like any unsupported name.

**Independent Test**: `jurisdiction` and `jurisdiction/{id}` return `400` with a token and `401` without (quickstart § 3 rows 9–10).

- [ ] T024 [US4] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java add `ac012TheSingularCollectionIsUnsupported` (`400`, unsupported-attribute message, no `results`), `ac013TheSingularSingleRecordRouteIsUnsupported` over `30`, `31` and `abc` (all `400` with the unsupported-attribute message: the type is checked before the id) and `ac014AuthenticationIsCheckedBeforeTheSingularName` over the two singular paths without a token and with `Bearer wrong-token` (`401`) (AC-012, AC-013, AC-014)

**Checkpoint**: US4 complete.

---

## Phase 8: User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unbuilt type behave exactly as before.

**Independent Test**: 002's to 008's acceptance tests pass; the 13 unsupported names return `400` (quickstart § 3 rows 13–14).

- [ ] T025 [US6] Confirm with `git diff --quiet origin/main -- src/functionalTest/resources/golden/` that the seven existing golden files are unchanged and only `jurisdictions.json` is new (AC-017, SC-008)
- [ ] T026 [US6] In src/functionalTest/java/…/jo/JurisdictionsFunctionalTest.java add `ac019QueryParametersReturn400`, parameterised over `?name=Courts`, `?page=2`, `?x=` and `/84?name=Courts`: `400`, query-parameter message; and the inherited checks `ec009UnmatchedPathsReturn404` (trailing slash, `/84/`, `/84/extra`), `ec009OtherMethodsReturn405` (POST, PUT, PATCH, DELETE on the collection and `/84`), `ec009UnacceptableMediaTypesReturn406` and `ec009LeadingZerosAndOversizedIds` (`/084` → id 84; `/99999999999999999999` → `404`), as in LocationTypesFunctionalTest. AC-018 and EC-008 are covered by T006 (AC-019, EC-009)
- [ ] T027 [US6] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T006's list and example changes (AC-017)

**Checkpoint**: US6 complete — no regressions.

---

## Phase 9: User Story 7 - Maintainer Adds the Type Without Code (Priority: P2)

**Goal**: The change is configuration and data only, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; `git diff --stat origin/main -- src/main/java` is empty (quickstart § 1–2).

- [ ] T028 [P] [US7] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R5; FR-018, AC-021): add `"jurisdictions"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, with no exemption, and a comment above the list saying this type's fragment is the plural because the generic reference field `jurisdiction_id` (003) contains the singular (009 Clarifications); add `theGuardRecognisesEveryJurisdictionsSpelling` over `jurisdictions`, `Jurisdictions`, `JURISDICTIONS`, `myJurisdictions`, `JurisdictionsHelper` and `XJURISDICTIONS`, asserting `spellingsOf("jurisdictions")` matches; and `theGuardIgnoresTheGenericJurisdictionField` over `jurisdiction_id`, `jurisdictionId`, `JURISDICTION_ID`, `getJurisdictionId` and `Id of the record's jurisdiction`, asserting it doesn't match
- [ ] T029 [US7] Run `git diff --stat origin/main -- src/main/java` (expect no output) and `git diff --stat origin/main -- src/main/resources` (expect application.yml and jurisdictions.json only), and record the result in specs/009-jurisdictions-reference-data/plan.md § Implementation notes (AC-020, SC-009)
- [ ] T030 [US7] Confirm AC-022 (OpenAPI lists `jurisdictions`, not `jurisdiction`; `ReferenceDataResponse` unchanged) is covered by T005, and note the mapping in specs/009-jurisdictions-reference-data/plan.md § Implementation notes

**Checkpoint**: US7 complete.

---

## Phase 10: User Story 8 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise jurisdictions from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [ ] T031 [US8] In postman/ctam-jomockapi.postman_collection.json, inserting text with a throwaway scratchpad script so existing formatting is untouched (as 008 did) (research R7; AC-023, FR-014): (a) add a "Jurisdictions" sub-folder under "Reference Data", after "Location Types", with ten requests and test scripts: the collection (`200`, 5 results with exactly the six fields, ids `[30,34,84,86,87]`, every `end_date` null, first Courts, last Skills); `/84` (`200`, Magistrates); `/87` (`200`, Skills); `/31` (`404`); `/abc` (`400`); `?name=Courts` (`400`); trailing slash (`404`, "Resource not found."); missing token (`401`, `WWW-Authenticate: Bearer`); invalid token (`{{invalidBearerToken}}`, `401`); `jurisdiction` (`400`, unsupported, no `results`); (b) rename the request "GET /api/v1/reference_data/jurisdictions - unsupported (contract-valid, out of scope)" to "GET /api/v1/reference_data/tickets - unsupported (contract-valid, out of scope)" and change its `raw` URL and `path` to `tickets`, leaving its tests unchanged. Confirm with `git diff --stat` that the change is insertions plus that one request's three edited lines
- [ ] T032 [US8] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/009-jurisdictions-reference-data/plan.md § Implementation notes (AC-023)

**Checkpoint**: All user stories complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

- [ ] T033 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `JURISDICTIONS` (with its static import) to the collection paths of the per-collection p95 guard (FR-016, SC-010)
- [ ] T034 [P] Update README.md (FR-015; research R8). In § Adding a reference-data type, "Tests to update", extend the `NoTypeSpecificCodeTest` bullet to: "add the type's forbidden fragment, unless an existing one already covers its name (as `location` covers `location_types`); if a generic field name contains the singular, use the plural instead (as `jurisdictions` does, because of `jurisdiction_id`)". In § Reference data: eight configured types, with a `jurisdictions` row (5, one per source entry; none: `jurisdiction` returns `400`); a short paragraph on jurisdiction records (the E-Links ids 30, 34, 84, 86, 87; no reference fields; 87 Skills is served but unreferenced); in the base-location paragraph, replace the sentence that `jurisdiction_id` still refers to a type that isn't served yet, and the sentence about the feature that adds it, with "Every `jurisdiction_id` is the id of a served jurisdiction."; in the location paragraph, extend "Every `type_id` is the id of a served location type with the same `jurisdiction_id`." with ", and every `jurisdiction_id` the id of a served jurisdiction"; in the location-type paragraph, add after its first sentence "Every `jurisdiction_id` is the id of a served jurisdiction."; remove `jurisdictions` from any list of unserved types
- [ ] T035 [P] Update CLAUDE.md: § Current repo state gains a `009-jurisdictions-reference-data` line (jurisdictions via configuration and fixture only, no main-code change; 5 records keeping the extract's ids 30, 34, 84, 86, 87, none ended; three `jurisdiction_id` rows in `ReferenceDataReferencesTest` close 003 DF-1, 007 DF-5 and 008 DF-7, so no served type has an outstanding forward reference; the singular `jurisdiction` returns `400`; `NoTypeSpecificCodeTest` widened to the plural `jurisdictions`, since `jurisdiction_id` contains the singular); mark the deferred items closed by 009 in the earlier lines: the 003 line's DF-1 sentence ("…recorded as DF-1, so the features adding those types must test that every reference resolves") gains "(closed by 007, 008 and 009)"; the 007 line's "DF-5: `location_types` and `jurisdictions` must make every location `type_id` and `jurisdiction_id` resolve" becomes "DF-5 (closed by 008 and 009)"; and the 008 line's "DF-7: the `jurisdictions` feature must make every location type `jurisdiction_id` resolve" becomes "DF-7 (closed by 009)"; § Reference data adds jurisdiction names and ids to the public values taken from the extracts and `specs/009-jurisdictions-reference-data/data-model.md` § 3 to the list of generation rules
- [ ] T036 [P] Add an "Extended by 009: see [the jurisdictions contract delta](../../009-jurisdictions-reference-data/contracts/reference-data-api-jurisdictions.md)." pointer after the 008 pointer on the attribute-name line of specs/002-reference-data-api/contracts/reference-data-api.md; and in specs/002-reference-data-api/quickstart.md § 3, change row 5 from `$BASE/jurisdictions` to the still-unsupported `$BASE/tickets`, which shows the same `400` (as 005 did for `genders`). Leave 005's and 006's quickstarts as they are: they record those features' state (research R8)
- [ ] T037 [P] Add one-line closing notes to the deferred-item rows: specs/003-base-locations-reference-data/spec.md DF-1 ("`jurisdiction_id` closed by 009", so DF-1 is fully closed), specs/007-locations-reference-data/spec.md DF-5 ("`jurisdiction_id` closed by 009", fully closed) and specs/008-location-types-reference-data/spec.md DF-7 ("closed by 009"), each pointing at specs/009-jurisdictions-reference-data/spec.md FR-011 (research R8)
- [ ] T038 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite test counts, JaCoCo line/branch coverage and each collection's strict p95 in specs/009-jurisdictions-reference-data/plan.md § Implementation notes (FR-016, FR-017, SC-010)
- [ ] T039 Walk through specs/009-jurisdictions-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun`, mark the spec's Status as Implemented in specs/009-jurisdictions-reference-data/spec.md, and record that every expected result matched (or the differences) in specs/009-jurisdictions-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story. T002 before T003 (the type can't load without its fixture); T004–T010 after T003; T011 last.
- **User stories (Phases 3–10)**: all after Phase 2. They add methods to the same functional test class, so run them in order US1 → US2 → US3 → US5 → US4 → US6 → US7 → US8, or have one person own that file. T028 (guard) and T031 (Postman) touch other files and can run alongside.
- **Polish (Phase 11)**: after the stories; T033–T037 in parallel, then T038, then T039.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2, US3, US5 (P1)**, **US4, US6, US7 (P2)**, **US8 (P3)**: each needs only Phase 2 functionally; US3's build-time half is T008 in Phase 2. US8's newman run (T032) is most useful once US1–US7 are green.

### Parallel opportunities

- Phase 2: T004, T005, T006, T007 and T008 (different files).
- Stories: T028 (guard) and T031 (Postman) alongside the functional-test tasks.
- Polish: T033–T037.

### Parallel example: Phase 2 test updates

```text
T004 ContractPaths                        — JURISDICTIONS constant
T005 ReferenceDataOpenApiContractTest     — enum and description
T006 ReferenceDataFunctionalTest          — unsupported list and auth example
T007 ReferenceDataFixtureRulesTest        — jurisdictions row; JURISDICTION_IDS Javadoc
T008 ReferenceDataReferencesTest          — three jurisdiction_id rows; Javadoc
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (build green with the type live and its references checked).
2. Phase 3 (US1): the collection works and is tested. **Stop and validate**: quickstart § 3 rows 1–2.

### Incremental delivery

3. US2, US3 and US5 (remaining P1) → US4, US6, US7 (P2) → US8 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extract and the generating script stay out of the repository; check `git status` before every commit.
