---

description: "Task list for 003-base-locations-reference-data"
---

# Tasks: Reference Data API — Base Locations

**Input**: Design documents from `/specs/003-base-locations-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-base-locations.md](./contracts/reference-data-api-base-locations.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-016, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage for the new type. The only production change is one type-agnostic deletion (FR-017, T003), so most user-story work *is* test work: the tests are the deliverable that proves the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US7) so each can be verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/java/…/jo/`, `src/main/resources/` — **one Java change only: T003's deletion in `FixtureReferenceDataRepository.java`**
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`

Expected values (counts, reference points, messages) come from [data-model.md § 3](./data-model.md) and the [contract delta](./contracts/reference-data-api-base-locations.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline on the right base.

- [x] T001 Confirm the branch base includes PR #9 (coverage gate): done, 459d909 is an ancestor of the branch
- [ ] T002 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) in specs/003-base-locations-reference-data/plan.md § Implementation notes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Allow repeated names, ship the type (configuration and data), and keep the build green. The fixture can't load until T003 is in, and adding the configuration immediately changes the behaviour two existing tests assert (research R5), so all of this belongs here, not in a later story.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

- [ ] T003 In src/main/java/…/jo/repository/FixtureReferenceDataRepository.java `load`, delete the duplicate-name check: the `Set<String> names = new HashSet<>();` line and the `if (!names.add(record.getName())) { throw invalid(fixture, record.getId(), "duplicate name"); }` block, then the now-unused `java.util.HashSet` and `java.util.Set` imports (Checkstyle fails on unused imports). Change nothing else: the duplicate-id check and every field rule stay. Name no type in the code or its comments (FR-017, research R10)
- [ ] T004 [P] In src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java, remove the `name-duplicate | record id 6: duplicate name` row from `failsStartupOnAnInvalidRecord`; `git mv` src/test/resources/reference-data/invalid/name-duplicate.json to src/test/resources/reference-data/valid-repeated-names.json (content unchanged: ids 5 and 6, both named "A"); add `loadsRecordsThatShareAName`, which loads it as type `widgets` and asserts `findAll` returns ids `5, 6` in that order, both named "A", and `findById` finds each (FR-017, research R10)
- [ ] T005 Generate src/main/resources/reference-data/base_locations.json with a throwaway script kept outside the repository (e.g. in a scratch directory; never committed), following data-model.md § 3 rules 1–10 exactly: the `name` value of **every** row of the gitignored `joh-elinks-api/ReferenceData/REF_BaseLocation.csv`, read as UTF-8 (strip any BOM), with repeats kept and nothing trimmed, case-folded or filtered (including the "Unknown…" names); sorted by code point; `id = 10 × position`; ended iff `position % 7 == 0` (`end_date 2025-03-31`, `updated_at 2025-04-01T08:00:00Z`), otherwise `end_date null` / `updated_at 2024-06-03T10:30:00Z`; all `created_at 2024-01-15T09:00:00Z` / `start_date 2024-01-01`; two-space indent, field order `id, name, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, en dash written literally (not as the escape `–`), trailing newline. Before continuing, confirm 1,462 records, 1,276 distinct names, 208 ended records, and the four reference points (ids 10, 70, 10290, 14620)
- [ ] T006 Add the type entry after `appointment_titles` under `jo.reference-data.types` in src/main/resources/application.yml: `name: base_locations` and `fixture: classpath:reference-data/base_locations.json`, with no `aliases` key, because `base_location` is not served (spec Clarifications; research R1)
- [ ] T007 [P] Add `public static final String BASE_LOCATIONS = "/api/v1/reference_data/base_locations";` to src/testFixtures/java/…/jo/testsupport/ContractPaths.java, keeping the class Javadoc's rule that tests use these constants rather than main code's
- [ ] T008 [P] Update the two `attribute_name` assertions in src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java (collection operation and single-record operation): `enum` exactly `appointment_titles, base_locations, appointment_title`, and description exactly `Can be one of: appointment_titles, base_locations. Also supports deprecated values: appointment_title` (contract delta § 1; AC-019, FR-012)
- [ ] T009 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java `UnsupportedTypeTests`, remove only `base_locations` from the contract-name list, so it holds the 19 still-unsupported names (`base_location` stays: it is not served), and update its comment to say "the other 19 E-Links attribute names: nine canonical types, their nine deprecated aliases, and base_location, which 003 doesn't serve" (research R5)
- [ ] T010 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged (its indexed `@TestPropertySource` list replaces the configured list, so `base_locations` is not loaded there); only if it fails because the lists merged, redeclare its type list in full in that file (research R5)
- [ ] T011 Create src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java holding the HTTP helpers both functional classes need — `get`, `send` (exact `URI.create`, optional `Authorization`), `assertError` (shared shape, exact message, `traceId` equals `X-Correlation-Id`) and `json` — moved from src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java, which then uses the helper with no behaviour change (research R6; Principle III). The package name `uk.gov.hmcts.ctam.jo.testsupport` deliberately matches the testFixtures package, so helpers are found in one place by name; this is safe because the test suites don't use Java modules
- [ ] T012 Create src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "003's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.BASE_LOCATIONS`; no `@Nested` classes: the story phases below add test methods directly to this class
- [ ] T013 Run `./gradlew check -PskipOwasp`: every suite green, including `FixtureReferenceDataRepositoryTest`, `NoTypeSpecificCodeTest` (unchanged so far), 002's AppointmentTitle functional tests and the JaCoCo gate

**Checkpoint**: Repeated names load; the type is live; existing tests reflect two supported types; the build is green.

---

## Phase 3: User Story 1 — Integrator Retrieves All Base Locations (Priority: P1) 🎯 MVP

**Goal**: The collection route serves all 1,462 base locations, one per source row, deterministically, with names exactly as in the source.

**Independent Test**: `GET /api/v1/reference_data/base_locations` with a valid token returns `200` and 1,462 records (1,276 distinct names) in ascending `id` order, equal to the golden file on repeated calls (quickstart § 3 rows 1–3, 5–6).

- [ ] T014 [P] [US1] Create src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R4): parameterised over `reference-data/appointment_titles.json` (194 records, 194 distinct names; reference points 10 "Acting Senior Coroner", 70 "Area Coroner" ended, 1940 "Vice-President, Employment Tribunal (Scotland)") and `reference-data/base_locations.json` (1,462 records, 1,276 distinct names; reference points per data-model.md § 3), loading the classpath fixture with Jackson and asserting: names non-decreasing by `String.compareTo` (equal neighbours allowed, FR-008); `id == 10 × position`; `position % 7 == 0` ⇔ `end_date 2025-03-31` and `updated_at 2025-04-01T08:00:00Z`, otherwise `end_date null` and `updated_at 2024-06-03T10:30:00Z`; every `created_at 2024-01-15T09:00:00Z` and `start_date 2024-01-01`; exact record count and exact distinct-name count; each reference point's `name` and `end_date`. Write the en dash in Java source as the escape `–`; also assert that the parameter list names every `*.json` file in the `src/main/resources/reference-data` directory, read from the file system the way `NoTypeSpecificCodeTest` reads `src/main/java` (not by classpath search, which could pick up other suites' resources such as the integration suite's `test_widgets.json`), so a new type's fixture can't go unchecked
- [ ] T015 [US1] Capture src/functionalTest/resources/golden/base_locations.json: start the service, `GET /api/v1/reference_data/base_locations` with a valid token, and save the exact response body bytes (compact JSON, UTF-8, no trailing newline added; about 226 KB) the same way 002 captured `golden/appointment_titles.json`; confirm it holds 1,462 records; before committing, confirm its `results` ids and names equal those in src/main/resources/reference-data/base_locations.json
- [ ] T016 [US1] Add the collection test methods to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac001ReturnsAll1462BaseLocationsInAscendingIdOrder` (status 200, `application/json`, `results` size 1,462, ids strictly ascending, every record has exactly the six contract fields); `ac001RepeatedNamesAreSeparateRecords` (EC-001, SC-002: 1,276 distinct names; "National" on exactly 40 records, ids 7200 to 7590; "Bedfordshire LJA" on ids 540 and 550); `ac002ReferencePointsMatchTheDataModel` (ids 10, 70, 14620: `name`, `end_date`, `updated_at` per data-model.md § 3); `ac003TheNonAsciiNameIsServedExactly` (record 10290's decoded `name` equals the Java literal `"Royal Courts of Justice – Office of the Judge Advocate General"`, i.e. the real en dash U+2013; and the raw response body does not contain the six-character escape text, checked with the Java literal `"\\u2013"`); `ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile` (several calls, each body equal to `golden/base_locations.json`, read as UTF-8)
- [ ] T017 [US1] Run `./gradlew test --tests '*ReferenceDataFixtureRulesTest' functionalTest --tests '*BaseLocationsFunctionalTest'` (each `--tests` filters the task before it) and confirm US1's tests pass

**Checkpoint**: MVP — base locations can be loaded as a lookup.

---

## Phase 4: User Story 2 — Integrator Retrieves a Single Base Location by ID (Priority: P1)

**Goal**: Single-record lookups, `404` for unknown ids and `400` for malformed ids, with ids scoped to the type.

**Independent Test**: `GET …/base_locations/70` returns Aldridge and Brownhills; `…/15` returns `404`; `…/abc` returns `400` (quickstart § 3 rows 4, 9–11).

- [ ] T018 [US2] Add the single-record test methods to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac005ReturnsTheRecordEqualToTheCollectionElement` (for ids 10, 70, 540, 550, 10290, 14620: status 200, a single object without `results`, equal to the matching collection element; 540 and 550 are both "Bedfordshire LJA", so lookups by id tell them apart); `ac006UnknownIdsReturn404` (parameterised `15`, `8885`, `14630`, `999999`: `404` with `RECORD_NOT_FOUND`); `ac007MalformedIdsReturn400` (parameterised `abc`, `12x`, `1.5`, `-1`: `400` with `MALFORMED_REFERENCE_ID`); `ac008IdsAreScopedToTheirType` (`base_locations/10` is "Aberconwy" while `appointment_titles/10` is "Acting Senior Coroner"; `base_locations/1940` is "Central Buckinghamshire" (position 194), not the appointment title with id 1940 ("Vice-President, Employment Tribunal (Scotland)"))
- [ ] T019 [US2] Run the functional suite and confirm the `ac005`–`ac008` tests in BaseLocationsFunctionalTest.java pass

**Checkpoint**: US1 and US2 together cover both routes for the new type.

---

## Phase 5: User Story 4 — Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new type opens no unauthenticated path.

**Independent Test**: every base-location route, with no token and with a wrong token, returns `401` (quickstart § 3 rows 12–13).

- [ ] T020 [US4] Add the authentication test methods to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac012MissingTokenIsRejected` (parameterised over `/base_locations`, `/base_locations/70`, `/base_locations/abc`: no `Authorization` header → `401` with `UNAUTHORIZED`, `WWW-Authenticate: Bearer`, and no `results` or `id` in the body); `ac013InvalidTokenIsRejectedWithTheSameMessage` (the same paths with `Bearer wrong-token`, `Basic dGVzdA==` and an empty bearer token → identical `401` message)
- [ ] T021 [US4] Run the functional suite and confirm the `ac012` and `ac013` tests in BaseLocationsFunctionalTest.java pass

**Checkpoint**: All P1 stories done.

---

## Phase 6: User Story 3 — The Deprecated Alias Is Not Served (Priority: P2)

**Goal**: `base_location` is rejected like any unsupported attribute name and never returns data.

**Independent Test**: `base_location` and `base_location/{id}` return `400` with a valid token and `401` without one (quickstart § 3 rows 7–8).

- [ ] T022 [US3] Add the alias-not-served test methods to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac009TheAliasCollectionIsUnsupported` (`GET /api/v1/reference_data/base_location` with a valid token → `400` with `UNSUPPORTED_ATTRIBUTE_NAME`, and no `results` in the body); `ac010TheAliasSingleRecordRouteIsUnsupported` (parameterised `70`, `15`, `abc` under `/base_location/` → `400` with `UNSUPPORTED_ATTRIBUTE_NAME`, never `200`, `404` or the malformed-id message, and no `id` in the body); `ac011AuthenticationIsCheckedBeforeTheAlias` (`/base_location` and `/base_location/70` with no `Authorization` header and with `Bearer wrong-token` → `401` with `UNAUTHORIZED`)
- [ ] T023 [US3] Run the functional suite and confirm the `ac009`–`ac011` tests in BaseLocationsFunctionalTest.java pass

---

## Phase 7: User Story 5 — Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: AppointmentTitle is unaffected, the other 19 names (including `base_location`) still return `400`, and inherited rules apply to the new type.

**Independent Test**: 002's AppointmentTitle functional tests pass unchanged; every still-unsupported name returns `400` (quickstart § 3 rows 14–16).

- [ ] T024 [P] [US5] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java `UnsupportedTypeTests`, add the 003 EC-006 variants `Base_Locations`, `BASE_LOCATIONS` and `base-locations` to the unknown-names list, keep the test name `ac007Ac014UnsupportedNamesAreRejectedOnBothRoutes` (its IDs are 002's), and add a comment above it: "Also covers 003 AC-009/AC-010 (via base_location), AC-015 and EC-006 (specs/003-base-locations-reference-data)."
- [ ] T025 [US5] Add the inherited-behaviour test methods to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac016QueryParametersReturn400` (`?name=Aberconwy`, `?page=2`, `?x=` on collection and `/70` → `400` with `QUERY_PARAMETERS_NOT_SUPPORTED`); `ec008UnmatchedPathsReturn404` (`/base_locations/`, `/base_locations/70/`, `/base_locations/70/extra` → `404` with `RESOURCE_NOT_FOUND`); `ec008OtherMethodsReturn405` (`POST`, `PUT`, `PATCH`, `DELETE` on `/base_locations` and `/base_locations/70` → `405` with `METHOD_NOT_ALLOWED`); `ec008LeadingZerosAndOversizedIds` (`/base_locations/070` → `200`, record 70; `/base_locations/99999999999999999999` → `404` with `RECORD_NOT_FOUND`)
- [ ] T026 [US5] Run `./gradlew functionalTest` and confirm AC-014: every 002 test in ReferenceDataFunctionalTest.java passes with its assertions and expected values unchanged (the only permitted edits are the helper move in T011, which changes no behaviour, and the name-list changes in T009/T024), and the AppointmentTitle golden file is unchanged (`git diff --exit-code src/functionalTest/resources/golden/appointment_titles.json`)

---

## Phase 8: User Story 6 — Maintainer Adds the Type Without Type-Specific Code (Priority: P2)

**Goal**: Prove 002's extension design: no type-specific code, one generic deletion, generated documentation.

**Independent Test**: the only diff under `src/main/java` is T003's deletion; the widened guard passes; `/v3/api-docs` lists the new names (quickstart § 2, § 4).

- [ ] T027 [P] [US6] Widen src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R8): replace the single `appointment` check with a parameterised test over the forbidden fragments `appointment` and `base_location` (case-insensitive, code and comments alike), named e.g. `noMainSourceFileNamesASpecificReferenceDataType`, with a comment that each type feature adds its own fragment; keep `onlyTheRegistryResolvesAliases` unchanged
- [ ] T028 [US6] Verify AC-017/SC-007: `git diff --numstat main -- src/main/java` lists only `src/main/java/uk/gov/hmcts/ctam/jo/repository/FixtureReferenceDataRepository.java`, with `0` added lines (deletions only), and `git diff --stat main -- src/main/resources` shows only application.yml and reference-data/base_locations.json; record the result in specs/003-base-locations-reference-data/plan.md § Implementation notes
- [ ] T029 [US6] Run `./gradlew test integrationTest` and confirm AC-018 (the widened guard) and AC-019 (T008's OpenAPI assertions against the generated `/v3/api-docs`) pass

---

## Phase 9: User Story 7 — Example Requests Exercise the New Type (Priority: P3)

**Goal**: Ready-made Postman requests for base locations.

**Independent Test**: newman runs the collection against a running instance with every test passing (quickstart § 5).

- [ ] T030 [US7] Add a "Base Locations" sub-folder inside the "Reference Data" folder of postman/ctam-jomockapi.postman_collection.json, following the existing requests' structure (`{{baseUrl}}`, `Authorization: Bearer {{bearerToken}}`, no hard-coded environment values), with five requests and test scripts (research R9): (1) `GET {{baseUrl}}/api/v1/reference_data/base_locations` — 200, JSON, `results` length 1462, 1276 distinct names, first id 10 "Aberconwy", last id 14620 "Yorkshire & Humberside", ids ascending, every record has exactly the six fields; (2) `GET …/base_locations/70` — 200, single object, name "Aldridge and Brownhills", `end_date` "2025-03-31", exactly the six fields; (3) `GET …/base_locations/15` — 404, shared error shape, message "Reference data record not found.", `traceId` present; (4) `GET …/base_locations/abc` — 400, shared error shape, message "reference_id must be a non-negative whole number."; (5) `GET …/base_location` — 400, shared error shape, message "Unsupported reference data attribute_name.", no `results`, `traceId` present (the alias is not served). Leave the existing `genders` request as is (still unsupported). postman/ctam-jomockapi.postman_environment.json needs no change
- [ ] T031 [US7] Start the service and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; confirm every request passes, and record the request/assertion counts in specs/003-base-locations-reference-data/plan.md § Implementation notes

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Performance guard, documentation, and full verification.

- [ ] T032 [P] Parameterise `collectionP95LatencyIsWithinBound` in src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java over `ContractPaths.APPOINTMENT_TITLES` and `ContractPaths.BASE_LOCATIONS` (test name shows the path), keeping 5 warm-up and 20 measured calls, the 200 check on every call, and the bounds (1 s default, 100 ms with `-Dperf.strict=true`); update the class Javadoc to say each configured collection is guarded (research R7; FR-015)
- [ ] T033 [P] Update README.md § Reference data: list both configured types with their record counts and served aliases (`appointment_titles`, alias `appointment_title`, 194; `base_locations`, no alias, 1,462 records with 1,276 distinct names), say that a type's feature decides which deprecated aliases it serves (Constitution VII), and in "Adding a reference-data type" change the fixture rules from "unique trimmed names" to "trimmed names (names may repeat; ids may not)" (FR-017, research R10). Keep the two steps (fixture and configuration entry, no Java changes) and add a short "Tests to update" list beneath them (FR-014): add the type's forbidden fragment to `NoTypeSpecificCodeTest`; add the fixture with its record count, distinct-name count and reference points to `ReferenceDataFixtureRulesTest`; update the `attribute_name` enum and description in `ReferenceDataOpenApiContractTest`; remove the names the type serves from the unsupported list in `ReferenceDataFunctionalTest`; add a functional test class, a smoke path and a Postman sub-folder for the type. Mention that names are carried over exactly, including non-ASCII characters
- [ ] T034 [P] Update CLAUDE.md: add a `003-base-locations-reference-data` entry to § Current repo state (type added through configuration plus one generic change: fixture validation no longer requires unique names; 1,462 records, one per source row, 1,276 distinct names; no deprecated alias served, so `base_location` returns `400` (constitution v1.11.0, Principle VII); fixture rules test; widened `NoTypeSpecificCodeTest`), and change § Reference data so it says public names (titles and base-location names), not only title names, were taken from the extracts
- [ ] T035 [P] Add one-line pointers to 002's documents without rewriting their tables: in specs/002-reference-data-api/contracts/reference-data-api.md, under the `attribute_name` allowed-values rows, "Extended by 003: see [the base-locations contract delta](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md)."; in specs/002-reference-data-api/data-model.md, under the § 2 field table, "003 FR-017 drops the rule that `name` is unique within the type: see [003's data model](../003-base-locations-reference-data/data-model.md)."
- [ ] T036 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; confirm all suites pass, both collection p95s are under 100 ms (record them), and the JaCoCo coverage gate passes
- [ ] T037 Walk through specs/003-base-locations-reference-data/quickstart.md §§ 1–5 against a running instance and confirm every expected result
- [ ] T038 Add § Implementation notes to specs/003-base-locations-reference-data/plan.md: baseline and final test counts per suite, the strict p95 figures, the AC-017 diff result, newman counts, any difference from the planned design, and items still open before merge (the inherited Sonar deferral)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup. **Blocks every story.** T003 → T006 (the fixture has repeated names and won't load before T003); T004 runs alongside T003 and passes once T003 is in; T005 → T006 (the config points at the fixture); T007–T009 can run alongside T003–T006 but only pass once T006 is in; T011 → T012; T013 last.
- **US1 (Phase 3)**: after Phase 2. T015 needs the service running with T003, T005 and T006; T016 needs T015.
- **US2, US4, US3 (Phases 4–6)**: after Phase 2; independent of US1 and of each other, but all edit BaseLocationsFunctionalTest.java, so run them one after another (each adds its own test methods).
- **US5 (Phase 7)**: after Phase 2. T024 edits ReferenceDataFunctionalTest.java (independent of the new class); T025 edits BaseLocationsFunctionalTest.java (serialise with Phases 3–6).
- **US6 (Phase 8)**: after Phase 2; T027 is independent of every other story.
- **US7 (Phase 9)**: after Phase 2; needs a running service only.
- **Polish (Phase 10)**: T032–T035 can run any time after Phase 2; T036–T038 after all stories.

### User Story Dependencies

All stories depend only on Phase 2. None depends on another story's output; the only coupling is the shared test file BaseLocationsFunctionalTest.java (US1–US5), which makes those edits sequential, not dependent.

### Within Each Story

Tests are the story's implementation here (the only production change is T003, in Phase 2): write the story's test methods, then run them. A failing test means the configuration, fixture or an assumption in the design is wrong — fix the cause, never the expectation, unless the spec says otherwise.

---

## Parallel Examples

### Phase 2

```text
T003  Delete the duplicate-name check              (src/main/java/…/repository/)
T004  Repository test: repeated names load         (src/test/…/repository/, src/test/resources/…)
T005  Generate base_locations.json                 (src/main/resources/reference-data/)
```

Then, after T006:

```text
T007  ContractPaths.BASE_LOCATIONS                 (src/testFixtures/…)
T008  OpenAPI contract test expectations           (src/integrationTest/…)
T009  Unsupported-names list → 19                  (src/functionalTest/…/ReferenceDataFunctionalTest.java)
```

T011 also touches ReferenceDataFunctionalTest.java, so run it after T009.

### After Phase 2

```text
T014  ReferenceDataFixtureRulesTest                (src/test/…/repository/)          [US1]
T027  NoTypeSpecificCodeTest widened               (src/test/…/architecture/)        [US6]
T030  Postman Base Locations sub-folder            (postman/)                        [US7]
T032  Smoke latency per collection                 (src/smokeTest/…)                 [Polish]
T033  README / T034 CLAUDE.md / T035 002 pointers   (docs)                            [Polish]
```

Meanwhile one person works through BaseLocationsFunctionalTest.java: T016 → T018 → T020 → T022 → T025.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T002) and Phase 2 (T003–T013): repeated names load, the type is served and the build is green.
2. Phase 3 (T014–T017): the collection is proven complete, ordered, exact and deterministic.
3. **Stop and validate**: quickstart § 3 rows 1–3 and 5–6. This alone gives integrators a usable base-location lookup.

### Incremental Delivery

1. + US2 (single record) and US4 (authentication): all P1 behaviour proven.
2. + US3 (alias not served) and US5 (regression boundary): the alias choice is enforced and nothing else changes.
3. + US6 (extension proof) and US7 (Postman): maintainers and integrators served.
4. Polish: latency guard, docs, full verification, implementation notes.

---

## Notes

- **One production Java change only (T003), and it is a deletion.** If any other task seems to need production code, stop: it means the generic design has a gap, which is a design change to raise (and a constitution-relevant one, Principle VII), not something to patch here.
- The extract is gitignored and must never be staged. Before every commit, check `git status` shows nothing under `joh-elinks-api/`, and that no generation script was added.
- Use `ContractMessages` and `ContractPaths` constants in tests, never main code's own constants.
- Commit after each phase or logical group; keep `./gradlew check -PskipOwasp` green at every commit.
