---

description: "Task list for 003-base-locations-reference-data"
---

# Tasks: Reference Data API — Base Locations

**Input**: Design documents from `/specs/003-base-locations-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-base-locations.md](./contracts/reference-data-api-base-locations.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-016, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage for the new type. Because no production Java changes (AC-017), most user-story work *is* test work: the tests are the deliverable that proves the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US7) so each can be verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/java/…/jo/`, `src/main/resources/` — **no Java changes in this feature**
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`

Expected values (counts, reference points, messages) come from [data-model.md § 3](./data-model.md) and the [contract delta](./contracts/reference-data-api-base-locations.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline on the right base.

- [ ] T001 Decide the base for this branch: if PR #9 (`test-coverage-improvements`, coverage gate) has merged to `main`, rebase `003-base-locations-reference-data` onto `main`; otherwise continue and record in plan.md § Implementation notes that FR-016's gate applies once PR #9 lands (plan.md XIII row)
- [ ] T002 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) in specs/003-base-locations-reference-data/plan.md § Implementation notes, as the baseline that later tasks must not regress

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Ship the type (configuration and data) and keep the build green. Adding the configuration immediately changes the behaviour two existing tests assert (research R5), so those updates belong here, not in a later story.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

- [ ] T003 Generate src/main/resources/reference-data/base_locations.json with a throwaway script kept outside the repository (e.g. in a scratch directory; never committed), following data-model.md § 3 rules 1–10 exactly: distinct `name` values of the gitignored `joh-elinks-api/ReferenceData/REF_BaseLocation.csv` read as UTF-8 (strip any BOM), compared exactly (no trimming of internal spaces, no case folding, no filtering of "Unknown…" names), sorted by code point, `id = 10 × position`, ended iff `position % 7 == 0` (`end_date 2025-03-31`, `updated_at 2025-04-01T08:00:00Z`), otherwise `end_date null` / `updated_at 2024-06-03T10:30:00Z`, all `created_at 2024-01-15T09:00:00Z` / `start_date 2024-01-01`; two-space indent, field order `id, name, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, en dash written literally (not as the escape `\u2013`), trailing newline. Confirm 1,276 records and the four reference points (ids 10, 70, 8880, 12760) before continuing
- [ ] T004 Add the type entry after `appointment_titles` under `jo.reference-data.types` in src/main/resources/application.yml: `name: base_locations`, `aliases: [base_location]`, `fixture: classpath:reference-data/base_locations.json` (research R1)
- [ ] T005 [P] Add `public static final String BASE_LOCATIONS = "/api/v1/reference_data/base_locations";` to src/testFixtures/java/…/jo/testsupport/ContractPaths.java, keeping the class Javadoc's rule that tests use these constants rather than main code's
- [ ] T006 [P] Update the two `attribute_name` assertions in src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java (collection operation and single-record operation): `enum` exactly `appointment_titles, base_locations, appointment_title, base_location`, and description exactly `Can be one of: appointment_titles, base_locations. Also supports deprecated values: appointment_title, base_location` (contract delta § 1; AC-019)
- [ ] T007 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java `UnsupportedTypeTests`, remove `base_locations` and `base_location` from the contract-name list, so it holds the 18 still-unsupported names, and update its comment to say "the other 18 E-Links attribute names: nine canonical types and their nine deprecated aliases" (research R5)
- [ ] T008 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged (its indexed `@TestPropertySource` list replaces the configured list, so `base_locations` is not loaded there); only if it fails because the lists merged, redeclare its type list in full in that file (research R5)
- [ ] T009 Create src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java holding the HTTP helpers both functional classes need — `get`, `send` (exact `URI.create`, optional `Authorization`), `assertError` (shared shape, exact message, `traceId` equals `X-Correlation-Id`) and `json` — moved from src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java, which then uses the helper with no behaviour change (research R6; Principle III). The package name `uk.gov.hmcts.ctam.jo.testsupport` deliberately matches the testFixtures package, so helpers are found in one place by name; this is safe because the test suites don't use Java modules
- [ ] T010 Create src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "003's acceptance scenarios, end to end over real HTTP. One nested class per user story.", using `ReferenceDataHttp` and `ContractPaths.BASE_LOCATIONS`; nested test classes are added by the story phases below
- [ ] T011 Run `./gradlew check -PskipOwasp`: every suite green, including `NoTypeSpecificCodeTest` (unchanged so far) and 002's AppointmentTitle functional tests

**Checkpoint**: The type is live; existing tests reflect two supported types; the build is green.

---

## Phase 3: User Story 1 — Integrator Retrieves All Base Locations (Priority: P1) 🎯 MVP

**Goal**: The collection route serves all 1,276 base locations, deterministically, with names exactly as in the source.

**Independent Test**: `GET /api/v1/reference_data/base_locations` with a valid token returns `200` and 1,276 records in ascending `id` order, equal to the golden file on repeated calls (quickstart § 3 rows 1–3, 5–6).

- [ ] T012 [P] [US1] Create src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R4): parameterised over `reference-data/appointment_titles.json` (194 records; reference points 10 "Acting Senior Coroner", 70 "Area Coroner" ended, 1940 "Vice-President, Employment Tribunal (Scotland)") and `reference-data/base_locations.json` (1,276 records; reference points per data-model.md § 3), loading the classpath fixture with Jackson and asserting: names strictly ascending by `String.compareTo`; `id == 10 × position`; `position % 7 == 0` ⇔ `end_date 2025-03-31` and `updated_at 2025-04-01T08:00:00Z`, otherwise `end_date null` and `updated_at 2024-06-03T10:30:00Z`; every `created_at 2024-01-15T09:00:00Z` and `start_date 2024-01-01`; exact record count; each reference point's `name` and `end_date`. Write the en dash in Java source as the escape `\u2013`
- [ ] T013 [US1] Capture src/functionalTest/resources/golden/base_locations.json: start the service, `GET /api/v1/reference_data/base_locations` with a valid token, and save the exact response body bytes (compact JSON, UTF-8, no trailing newline added) the same way 002 captured `golden/appointment_titles.json`; confirm it holds 1,276 records
- [ ] T014 [US1] Add nested class `CollectionTests` to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac001ReturnsAll1276BaseLocationsInAscendingIdOrder` (status 200, `application/json`, `results` size 1,276, ids strictly ascending, every record has exactly the six contract fields); `ac002ReferencePointsMatchTheDataModel` (ids 10, 70, 12760: `name`, `end_date`, `updated_at` per data-model.md § 3); `ac003TheNonAsciiNameIsServedExactly` (record 8880's decoded `name` equals the Java literal `"Royal Courts of Justice \u2013 Office of the Judge Advocate General"`, i.e. the real en dash U+2013; and the raw response body does not contain the six-character escape text, checked with the Java literal `"\\u2013"`); `ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile` (several calls, each body equal to `golden/base_locations.json`, read as UTF-8)
- [ ] T015 [US1] Run `./gradlew test --tests '*ReferenceDataFixtureRulesTest' functionalTest --tests '*BaseLocationsFunctionalTest'` (each `--tests` filters the task before it) and confirm US1's tests pass

**Checkpoint**: MVP — base locations can be loaded as a lookup.

---

## Phase 4: User Story 2 — Integrator Retrieves a Single Base Location by ID (Priority: P1)

**Goal**: Single-record lookups, `404` for unknown ids and `400` for malformed ids, with ids scoped to the type.

**Independent Test**: `GET …/base_locations/70` returns Aldridge and Brownhills; `…/15` returns `404`; `…/abc` returns `400` (quickstart § 3 rows 4, 9–11).

- [ ] T016 [US2] Add nested class `SingleRecordTests` to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac005ReturnsTheRecordEqualToTheCollectionElement` (for ids 10, 70, 8880, 12760: status 200, a single object without `results`, equal to the matching collection element); `ac006UnknownIdsReturn404` (parameterised `15`, `8885`, `12770`, `999999`: `404` with `RECORD_NOT_FOUND`); `ac007MalformedIdsReturn400` (parameterised `abc`, `12x`, `1.5`, `-1`: `400` with `MALFORMED_REFERENCE_ID`); `ac008IdsAreScopedToTheirType` (`base_locations/10` is "Aberconwy" while `appointment_titles/10` is "Acting Senior Coroner"; `base_locations/1940` is the base location at position 194, not the appointment title with id 1940)
- [ ] T017 [US2] Run the functional suite and confirm `SingleRecordTests` passes

**Checkpoint**: US1 and US2 together cover both routes for the new type.

---

## Phase 5: User Story 4 — Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new type opens no unauthenticated path.

**Independent Test**: every base-location route, canonical and alias, with no token and with a wrong token, returns `401` (quickstart § 3 rows 12–13).

- [ ] T018 [US4] Add nested class `AuthenticationTests` to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac012MissingTokenIsRejected` (parameterised over `/base_locations`, `/base_location`, `/base_locations/70`, `/base_location/70`, `/base_locations/abc`: no `Authorization` header → `401` with `UNAUTHORIZED`, `WWW-Authenticate: Bearer`, and no `results` or `id` in the body); `ac013InvalidTokenIsRejectedWithTheSameMessage` (the same paths with `Bearer wrong-token`, `Basic dGVzdA==` and an empty bearer token → identical `401` message)
- [ ] T019 [US4] Run the functional suite and confirm `AuthenticationTests` passes

**Checkpoint**: All P1 stories done.

---

## Phase 6: User Story 3 — Legacy Consumer Uses the Deprecated Alias (Priority: P2)

**Goal**: `base_location` behaves exactly like `base_locations`, transparently.

**Independent Test**: canonical and alias responses are identical for collection, existing, unknown and malformed ids (quickstart § 3 rows 7–8).

- [ ] T020 [US3] Add nested class `AliasTests` to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac009Ac010TheAliasBehavesExactlyLikeTheCanonicalName` (parameterised suffixes `""`, `/10`, `/70`, `/8880`, `/15`, `/abc`, `?page=2`: equal status and content type; equal bodies on 2xx; equal bodies without `timestamp`/`traceId` on errors — follow 002's `withoutVolatileFields` approach, sharing it through `ReferenceDataHttp` if both classes need it); `ac011TheAliasIsTransparent` (suffixes `""`, `/70`, `/15`: identical response header names, and successful alias bodies do not contain the text `base_location"` or `base_locations"`)
- [ ] T021 [US3] Run the functional suite and confirm `AliasTests` passes

---

## Phase 7: User Story 5 — Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: AppointmentTitle is unaffected, the other 18 names still return `400`, and inherited rules apply to the new type.

**Independent Test**: 002's AppointmentTitle functional tests pass unchanged; every still-unsupported name returns `400` (quickstart § 3 rows 14–16).

- [ ] T022 [P] [US5] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java `UnsupportedTypeTests`, add the 003 EC-006 variants `Base_Locations`, `BASE_LOCATIONS` and `base-locations` to the unknown-names list, keep the test name `ac007Ac014UnsupportedNamesAreRejectedOnBothRoutes` (its IDs are 002's), and add a comment above it: "Also covers 003 AC-015 and EC-006 (specs/003-base-locations-reference-data)."
- [ ] T023 [US5] Add nested class `InheritedBehaviourTests` to src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java with: `ac016QueryParametersReturn400` (`?name=Aberconwy`, `?page=2`, `?x=` on collection and `/70` → `400` with `QUERY_PARAMETERS_NOT_SUPPORTED`); `ec008UnmatchedPathsReturn404` (`/base_locations/`, `/base_locations/70/`, `/base_locations/70/extra` → `404` with `RESOURCE_NOT_FOUND`); `ec008OtherMethodsReturn405` (`POST`, `PUT`, `PATCH`, `DELETE` on `/base_locations` and `/base_locations/70` → `405` with `METHOD_NOT_ALLOWED`); `ec008LeadingZerosAndOversizedIds` (`/base_locations/070` → `200`, record 70; `/base_locations/99999999999999999999` → `404` with `RECORD_NOT_FOUND`)
- [ ] T024 [US5] Run `./gradlew functionalTest` and confirm AC-014: every 002 test in ReferenceDataFunctionalTest.java passes with its assertions and expected values unchanged (the only permitted edits are the helper move in T009, which changes no behaviour, and the name-list changes in T007/T022), and the AppointmentTitle golden file is unchanged (`git diff --exit-code src/functionalTest/resources/golden/appointment_titles.json`)

---

## Phase 8: User Story 6 — Maintainer Adds the Type Without Code Changes (Priority: P2)

**Goal**: Prove 002's extension design: no type-specific code, generated documentation.

**Independent Test**: no diff under `src/main/java`; the widened guard passes; `/v3/api-docs` lists the new names (quickstart § 2, § 4).

- [ ] T025 [P] [US6] Widen src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R8): replace the single `appointment` check with a parameterised test over the forbidden fragments `appointment` and `base_location` (case-insensitive, code and comments alike), named e.g. `noMainSourceFileNamesASpecificReferenceDataType`, with a comment that each type feature adds its own fragment; keep `onlyTheRegistryResolvesAliases` unchanged
- [ ] T026 [US6] Verify AC-017/SC-007: `git diff --stat main -- src/main/java` prints nothing, and `git diff --stat main -- src/main/resources` shows only application.yml and reference-data/base_locations.json; record the result in specs/003-base-locations-reference-data/plan.md § Implementation notes
- [ ] T027 [US6] Run `./gradlew test integrationTest` and confirm AC-018 (the widened guard) and AC-019 (T006's OpenAPI assertions against the generated `/v3/api-docs`) pass

---

## Phase 9: User Story 7 — Example Requests Exercise the New Type (Priority: P3)

**Goal**: Ready-made Postman requests for base locations.

**Independent Test**: newman runs the collection against a running instance with every test passing (quickstart § 5).

- [ ] T028 [US7] Add a "Base Locations" sub-folder inside the "Reference Data" folder of postman/ctam-jomockapi.postman_collection.json, following the existing requests' structure (`{{baseUrl}}`, `Authorization: Bearer {{bearerToken}}`, no hard-coded environment values), with six requests and test scripts (research R9): (1) `GET {{baseUrl}}/api/v1/reference_data/base_locations` — 200, JSON, `results` length 1276, first id 10 "Aberconwy", last id 12760 "Yorkshire & Humberside", ids ascending; (2) the same via `base_location` — 200 and length 1276; (3) `GET …/base_locations/70` — 200, single object, name "Aldridge and Brownhills", `end_date` "2025-03-31", exactly the six fields; (4) `GET …/base_location/70` — same assertions; (5) `GET …/base_locations/15` — 404, shared error shape, message "Reference data record not found.", `traceId` present; (6) `GET …/base_locations/abc` — 400, message "reference_id must be a non-negative whole number.". Leave the existing `genders` request as is (still unsupported). postman/ctam-jomockapi.postman_environment.json needs no change
- [ ] T029 [US7] Start the service and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; confirm every request passes, and record the request/assertion counts in specs/003-base-locations-reference-data/plan.md § Implementation notes

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Performance guard, documentation, and full verification.

- [ ] T030 [P] Parameterise `collectionP95LatencyIsWithinBound` in src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java over `ContractPaths.APPOINTMENT_TITLES` and `ContractPaths.BASE_LOCATIONS` (test name shows the path), keeping 5 warm-up and 20 measured calls, the 200 check on every call, and the bounds (1 s default, 100 ms with `-Dperf.strict=true`); update the class Javadoc to say each configured collection is guarded (research R7; FR-015)
- [ ] T031 [P] Update README.md § Reference data: list both configured types with their aliases and record counts (`appointment_titles`/`appointment_title`, 194; `base_locations`/`base_location`, 1,276), keep the "Adding a reference-data type" steps unchanged (FR-014), and mention that names are carried over exactly, including non-ASCII characters
- [ ] T032 [P] Update CLAUDE.md: add a `003-base-locations-reference-data` entry to § Current repo state (type added through configuration only; 1,276 records; fixture rules test; widened `NoTypeSpecificCodeTest`), and change § Reference data so it says public names (titles and base-location names), not only title names, were taken from the extracts
- [ ] T033 [P] In specs/002-reference-data-api/contracts/reference-data-api.md, add one sentence under the `attribute_name` allowed-values rows: "Extended by 003: see [the base-locations contract delta](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md)." Do not rewrite 002's tables
- [ ] T034 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; confirm all suites pass, both collection p95s are under 100 ms (record them), and JaCoCo coverage has not fallen (and the coverage gate passes if PR #9 is on the base)
- [ ] T035 Walk through specs/003-base-locations-reference-data/quickstart.md §§ 1–5 against a running instance and confirm every expected result
- [ ] T036 Add § Implementation notes to specs/003-base-locations-reference-data/plan.md: baseline and final test counts per suite, the strict p95 figures, the AC-017 diff result, newman counts, any difference from the planned design, and items still open before merge (the inherited Sonar deferral; the PR #9 dependency if unresolved)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup. **Blocks every story.** T003 → T004 (the config points at the fixture); T005–T007 can run alongside T003/T004 but only pass once T004 is in; T009 → T010; T011 last.
- **US1 (Phase 3)**: after Phase 2. T013 needs the service running with T003/T004; T014 needs T013.
- **US2, US4, US3 (Phases 4–6)**: after Phase 2; independent of US1 and of each other, but all edit BaseLocationsFunctionalTest.java, so run them one after another (each adds its own nested class).
- **US5 (Phase 7)**: after Phase 2. T022 edits ReferenceDataFunctionalTest.java (independent of the new class); T023 edits BaseLocationsFunctionalTest.java (serialise with Phases 3–6).
- **US6 (Phase 8)**: after Phase 2; T025 is independent of every other story.
- **US7 (Phase 9)**: after Phase 2; needs a running service only.
- **Polish (Phase 10)**: T030–T033 can run any time after Phase 2; T034–T036 after all stories.

### User Story Dependencies

All stories depend only on Phase 2. None depends on another story's output; the only coupling is the shared test file BaseLocationsFunctionalTest.java (US1–US5), which makes those edits sequential, not dependent.

### Within Each Story

Tests are the story's implementation here (no production code changes): write the nested test class, then run it. A failing test means the configuration, fixture or an assumption in the design is wrong — fix the cause, never the expectation, unless the spec says otherwise.

---

## Parallel Examples

### Phase 2 (after T004)

```text
T005  ContractPaths.BASE_LOCATIONS                 (src/testFixtures/…)
T006  OpenAPI contract test expectations           (src/integrationTest/…)
T007  Unsupported-names list → 18                  (src/functionalTest/…/ReferenceDataFunctionalTest.java)
```

T009 also touches ReferenceDataFunctionalTest.java, so run it after T007.

### After Phase 2

```text
T012  ReferenceDataFixtureRulesTest                (src/test/…/repository/)          [US1]
T025  NoTypeSpecificCodeTest widened               (src/test/…/architecture/)        [US6]
T028  Postman Base Locations sub-folder            (postman/)                        [US7]
T030  Smoke latency per collection                 (src/smokeTest/…)                 [Polish]
T031  README / T032 CLAUDE.md / T033 002 contract   (docs)                            [Polish]
```

Meanwhile one person works through BaseLocationsFunctionalTest.java: T014 → T016 → T018 → T020 → T023.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T002) and Phase 2 (T003–T011): the type is served and the build is green.
2. Phase 3 (T012–T015): the collection is proven complete, ordered, exact and deterministic.
3. **Stop and validate**: quickstart § 3 rows 1–3 and 5–6. This alone gives integrators a usable base-location lookup.

### Incremental Delivery

1. + US2 (single record) and US4 (authentication): all P1 behaviour proven.
2. + US3 (alias) and US5 (regression boundary): contract fidelity and no side effects.
3. + US6 (extension proof) and US7 (Postman): maintainers and integrators served.
4. Polish: latency guard, docs, full verification, implementation notes.

---

## Notes

- **No production Java changes.** If any task seems to need one, stop: it means the generic design has a gap, which is a design change to raise (and a constitution-relevant one, Principle VII), not something to patch here.
- The extract is gitignored and must never be staged. Before every commit, check `git status` shows nothing under `joh-elinks-api/`, and that no generation script was added.
- Use `ContractMessages` and `ContractPaths` constants in tests, never main code's own constants.
- Commit after each phase or logical group; keep `./gradlew check -PskipOwasp` green at every commit.
