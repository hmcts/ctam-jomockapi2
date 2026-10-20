---

description: "Task list for 004-contract-types-reference-data"
---

# Tasks: Reference Data API — Contract Types

**Input**: Design documents from `/specs/004-contract-types-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-contract-types.md](./contracts/reference-data-api-contract-types.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-015, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. The only production change is the type-agnostic boolean-field mechanism (FR-017, FR-018; Phase 2), so most user-story work *is* test work: the tests prove the configured type behaves as specified.

**Organization**: Tasks are grouped by user story (spec.md US1–US7) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US4), then P2 (US3, US5, US6), then P3 (US7).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`; abbreviated below as `…/jo/`.
- Main: `src/main/java/…/jo/`, `src/main/resources/` — the only Java changes are Phase 2's boolean fields (T002–T008)
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/`; functional helpers: `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java`

Expected values (counts, reference points, messages) come from [data-model.md § 3](./data-model.md) and the [contract delta](./contracts/reference-data-api-contract-types.md); error messages are the constants in `src/testFixtures/java/…/jo/testsupport/ContractMessages.java`. **No main source file or comment may name a reference-data type in any spelling** (`NoTypeSpecificCodeTest`): write `salaried`/`boolean field`, never "contract type", in main code.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [X] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line/branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/004-contract-types-reference-data/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add generic boolean fields, ship the type (configuration and data), and keep the build green. The fixture can't load until the record and validation accept `salaried`, and adding the configuration immediately changes what existing tests assert (research R5), so all of this belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

### Generic boolean fields (main code)

- [X] T002 Create src/main/java/…/jo/entity/BooleanField.java, modelled on `ReferenceField.java` in the same package: enum with one constant `SALARIED("salaried", ReferenceDataRecord::getSalaried)`, a `Function<ReferenceDataRecord, Boolean>` accessor, `fieldName()`, `readFrom(ReferenceDataRecord)` returning `Boolean` (null when the record doesn't carry it) and `static Optional<BooleanField> fromFieldName(String)`; Javadoc: "An optional yes/no field. A type declares the fields its records carry ({@code boolean-fields}); records of every other type never carry them." (research R2)
- [X] T003 [P] In src/main/java/…/jo/entity/ReferenceDataRecord.java add, after `jurisdictionId`, `@JsonProperty("salaried") @JsonSetter(nulls = Nulls.FAIL) Boolean salaried;` with Javadoc "Null unless the type declares {@link BooleanField#SALARIED}. An explicit {@code null} fails, as for every boolean field." (research R2, R3)
- [X] T004 In src/main/java/…/jo/entity/ReferenceDataType.java add `Set<BooleanField> booleanFields` after `referenceFields`, as a constructor parameter copied into an unmodifiable `EnumSet` exactly like `referenceFields` (null means none); update the class and constructor Javadoc. Update every caller to pass the new argument (`Set.of()` or `null` where the test doesn't care): src/main/java/…/jo/services/ReferenceDataTypeRegistry.java, src/test/java/…/jo/entity/ReferenceDataTypeTest.java, src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java, src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java and src/test/java/…/jo/testsupport/ReferenceDataFixtures.java (research R2)
- [X] T005 In src/main/java/…/jo/config/ReferenceDataProperties.java add `List<String> booleanFields` to `TypeProperties` after `referenceFields`, normalised with `copyOf` like `referenceFields`; extend the record Javadoc to describe `boolean-fields` (the boolean fields, e.g. `salaried`, the type's records carry; default none). Update every `new TypeProperties(...)` call, including `type(...)` in src/test/java/…/jo/testsupport/ReferenceDataFixtures.java and the calls in src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java (research R2)
- [X] T006 In src/main/java/…/jo/services/ReferenceDataTypeRegistry.java: call `requireNoEmptyEntry(name, "boolean-fields", properties.booleanFields())`; generalise the private `referenceFields(...)` into one helper that parses a list of declared field names into an `EnumSet` given the list's label and a name lookup, and use it for both `reference-fields` (messages unchanged: `unknown reference field '<name>'`, `duplicate reference field <name>`) and `boolean-fields` (`unknown boolean field '<name>'`, `duplicate boolean field <name>`); pass the result to the `ReferenceDataType` constructor; add "unknown or repeated boolean field" to the class Javadoc's list of invalid declarations (research R2, R3)
- [X] T007 In src/main/java/…/jo/repository/FixtureReferenceDataRepository.java: add `.withCoercionConfig(LogicalType.Boolean, config -> config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail).setCoercion(CoercionInputShape.Float, CoercionAction.Fail).setCoercion(CoercionInputShape.String, CoercionAction.Fail).setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))` to the strict mapper and extend its comment ("booleans are JSON booleans"); add `validateBooleanFields(fixture, type, record)`, called right after `validateReferenceFields`, which for each `BooleanField` throws `<field> is not a boolean field of <type>` when the type doesn't declare it and the record carries it, and `<field> is required` when the type declares it and the record lacks it (research R3; data-model.md § 1a)
- [X] T008 [P] In src/main/java/…/jo/domain/ReferenceDataResponse.java add `@JsonProperty("salaried") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean salaried` after `jurisdictionId`, with `@Schema(description = "Whether the record is salaried, including salaried part-time working. Present only for types whose records carry it (deviation D-3 from the E-Links schema).", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "true")`; add `"salaried"` after `"jurisdiction_id"` in `@JsonPropertyOrder`; mention the boolean field and D-3 in the class Javadoc. Do not change src/main/java/…/jo/mappers/ReferenceDataMapper.java: MapStruct maps by name, and its `unmappedTargetPolicy = ERROR` proves the mapping (research R2)

### Unit tests for the mechanism (FR-017, FR-018, AC-021)

- [X] T009 [P] In src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java add cases: `boolean-fields: [salaried]` resolves to `Set.of(BooleanField.SALARIED)`; absent resolves to empty; an unknown name (`Salaried`, `paid`), a repeated name and an empty (null) entry each throw `IllegalStateException` with the messages from T006; reference-field messages unchanged (research R3)
- [X] T010 [P] In src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java and new fixtures under src/test/resources/reference-data/: add valid-boolean-fields.json (two records, `salaried` `true` and `false`) and a `loadsTheBooleanFieldsTheTypeDeclares` test; add a `recordsOfATypeWithoutBooleanFieldsCarryNone` assertion; add a parameterised `failsStartupWhenADeclaredBooleanFieldIsInvalid` over new fixtures in src/test/resources/reference-data/invalid/: boolean-field-missing.json (`salaried is required`), boolean-field-explicit-null.json, boolean-field-number.json (`1`), boolean-field-string.json (`"true"`) and boolean-field-empty-string.json (`""`), each failing start-up with the fixture name in the message; add `failsStartupWhenARecordCarriesABooleanFieldItsTypeDoesNotDeclare` (`salaried is not a boolean field of <type>`) (research R3; AC-021)
- [X] T011 [P] In src/test/java/…/jo/entity/ReferenceDataTypeTest.java add cases that boolean fields are copied (later changes to the caller's set don't reach the type), unmodifiable, and empty when null (research R2)
- [X] T012 [P] In src/test/java/…/jo/mappers/ReferenceDataMapperTest.java add `mapsTheBooleanFields` (`salaried` `true` and `false` mapped) and assert `salaried` stays null when the record has none (research R2)
- [X] T013 [P] In src/test/java/…/jo/controllers/ReferenceDataControllerTest.java add `booleanFieldsAreWrittenAfterTheReferenceFieldsOnlyWhenTheRecordCarriesThem`, asserting the field order `id, name, salaried, created_at, …` for a record with `salaried` and no reference fields, and `id, name, type_id, parent_id, jurisdiction_id, salaried, created_at, …` for a record with both kinds; `false` is written, not omitted and extend `recordsWithoutReferenceFieldsLeaveThemOut` (or add a sibling test) to assert `salaried` is absent when null (FR-005, FR-017)

### Ship the type

- [X] T014 Generate src/main/resources/reference-data/contract_types.json with a throwaway script kept outside the repository (e.g. the session scratchpad; never committed), following data-model.md § 3 rules 1–9 exactly: from the gitignored `joh-elinks-api/ReferenceData/REF_ContractType.csv` read as UTF-8, take `name` and `salaried` of every row (14 rows), ignoring the extract's `id` and dates; `salaried` `1` → `true`, blank → `false`, anything else aborts; sort names by code point; `id = 10 × position`; ended iff `position % 7 == 0` (`end_date 2025-03-31`, `updated_at 2025-04-01T08:00:00Z`), otherwise `end_date null` / `updated_at 2024-06-03T10:30:00Z`; all `created_at 2024-01-15T09:00:00Z` / `start_date 2024-01-01`; two-space indent, field order `id, name, salaried, created_at, updated_at, start_date, end_date`, UTF-8 without BOM, trailing newline. Before continuing, confirm it equals data-model.md § 3's full dataset table: 14 records, 14 distinct names, 11 `salaried` `true`, ended ids 70 and 140
- [X] T015 Append the type entry after `base_locations` under `jo.reference-data.types` in src/main/resources/application.yml: `name: contract_types`, `fixture: classpath:reference-data/contract_types.json`, `boolean-fields: [salaried]` with the comment `# The extract's salaried flag (004 D-3).`; no `aliases`, `reference-fields` or `unique-names` keys (research R1)
- [X] T016 [P] Add `public static final String CONTRACT_TYPES = "/api/v1/reference_data/contract_types";` to src/testFixtures/java/…/jo/testsupport/ContractPaths.java
- [X] T017 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java: both `attribute_name` `enum` assertions become exactly `appointment_titles, base_locations, contract_types, appointment_title`; the description becomes exactly `Can be one of: appointment_titles, base_locations, contract_types. Also supports deprecated values: appointment_title`; `ReferenceDataResponse` properties become exactly `id, name, type_id, parent_id, jurisdiction_id, salaried, created_at, updated_at, start_date, end_date`, with `salaried` of `type` `boolean`, not nullable, not in `required`, and a description mentioning D-3; `required` unchanged (contract delta § 1, § 1a; AC-020, FR-011)
- [X] T018 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java `UnsupportedTypeTests`, remove only `contract_types` from the contract-name list, so it holds the 18 still-unsupported names (`contract_type` and `base_location` stay), add `Contract_Types`, `CONTRACT_TYPES` and `contract-types` to the case variants, and update the comment to "the other 18 E-Links attribute names: eight canonical types, their eight deprecated aliases, and base_location and contract_type, which 003 and 004 don't serve" plus a pointer to 004 AC-016 and EC-007 (research R5)
- [X] T019 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged; only if it fails because the configured list now merges with its own, redeclare its type list in full in that file (research R5)
- [X] T020 Extend src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java: add `booleanFields` to the `Declaration` record and read it from application.yml alongside `reference-fields`; for every fixture, assert each record carries exactly the declared boolean fields as JSON booleans, after the reference fields in field order; add `new Fixture("contract_types.json", 14, 14, …)` with reference points 10 Fee-paid (open, `false`), 20 SPTW-10% (open, `true`), 70 SPTW-60% (ended, `true`), 120 Salaried (open, `true`), 130 Unknown (open, `false`), 140 Voluntary (ended, `false`) — extending `ReferencePoint` with an optional `salaried` value — and assert 11 records are `true` (research R4; FR-008, FR-009)
- [X] T021 Create src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java mirroring src/functionalTest/java/…/jo/BaseLocationsFunctionalTest.java: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, Javadoc "004's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.", using `ReferenceDataHttp` and `ContractPaths.CONTRACT_TYPES`, with private helpers `assertRecord(record, name, salaried, endDate, updatedAt)` and `assertUnauthorized(response)`; story phases add test methods directly to this class
- [X] T022 Run `./gradlew check -PskipOwasp`: every suite green, including `FixtureReferenceDataRepositoryTest`, `ReferenceDataFixtureRulesTest`, `NoTypeSpecificCodeTest` (unchanged so far), 002's and 003's functional tests, and the JaCoCo gate

**Checkpoint**: Boolean fields work and are validated; the type is live; existing tests reflect three supported types; the build is green.

---

## Phase 3: User Story 1 - Integrator Retrieves All Contract Types (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 14 contract types with `salaried`, deterministically.

**Independent Test**: `GET /api/v1/reference_data/contract_types` with a valid token returns `200` and 14 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–6).

- [X] T023 [US1] In src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java add `ac001ReturnsAll14ContractTypesInAscendingIdOrder`: `200`, `application/json`, `results` of 14 records with ids 10 to 140 ascending, each with exactly the seven fields `id, name, salaried, created_at, updated_at, start_date, end_date` in that order (AC-001, FR-005)
- [X] T024 [US1] In the same file add `ac002ReferencePointsMatchTheDataModel`, checking the six reference points (ids 10, 20, 70, 120, 130, 140) for `name`, `salaried`, `end_date` and `updated_at`, and that every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01 (AC-002)
- [X] T025 [US1] In the same file add `ac003SalariedIsABooleanOnEveryRecord`: every record's `salaried` is a JSON boolean (not null, number or string); the `true` names are exactly "SPTW-10%" … "SPTW-90%" (ten) and "Salaried"; the `false` names are exactly "Fee-paid", "Unknown" and "Voluntary" (AC-003)
- [X] T026 [US1] Capture the collection response from a running instance (`./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/contract_types.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/contract_types`). The file must be the compact body exactly as served, with no reformatting and no trailing newline, because `ReferenceDataHttp.golden()` compares bytes. Check it matches the fixture's ids, names and `salaried` values; then add `ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile` to src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java, as in BaseLocationsFunctionalTest (AC-004)
- [X] T027 [US1] In the same file add `ac005NamesWithPercentAndHyphensAreServedExactly`: "SPTW-75%" (id 90) and "Fee-paid" (id 10) are returned exactly, and the raw body contains `"SPTW-75%"` literally (AC-005, EC-002); and `ec004TheExtractsEndDatesAreNotReproduced`: ids 90 (SPTW-75%) and 130 (Unknown) have `end_date` null, ids 70 and 140 have `2025-03-31` (EC-004)

**Checkpoint**: US1 complete — the MVP: integrators can load contract types.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Contract Type by ID (Priority: P1)

**Goal**: The single-record route returns one contract type and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/contract_types/120` returns `200` with the Salaried record (quickstart § 3 rows 5, 8–10).

- [X] T028 [US2] In src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 10, 20, 70, 120, 130 and 140: `200`, a single object (no `results`), equal to the collection element with that id, `salaried` included (AC-006)
- [X] T029 [US2] In the same file add `ac007UnknownIdsReturn404` over `15`, `150` and `999999` (`404`, record-not-found message) and `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message) (AC-007, AC-008, EC-005)
- [X] T030 [US2] In the same file add `ac009IdsAreScopedToTheirType`: id 10 returns "Fee-paid" under `contract_types`, "Acting Senior Coroner" under `appointment_titles` and "Aberconwy" under `base_locations` (AC-009, EC-006)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 4 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each contract-type route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 13–14).

- [X] T031 [US4] In src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java add `ac013MissingTokenIsRejected`, parameterised over the collection, `/120`, `/15` and `/abc`: `401`, shared shape, `WWW-Authenticate: Bearer`, no `results` (AC-013)
- [X] T032 [US4] In the same file add `ac014InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong`, `Basic abc` and `Bearer` (empty): `401` with the same message as a missing token (AC-014)

**Checkpoint**: All P1 stories complete.

---

## Phase 6: User Story 3 - The Deprecated Alias Is Not Served (Priority: P2)

**Goal**: `contract_type` is rejected like any unsupported name.

**Independent Test**: `contract_type` and `contract_type/{id}` return `400` with a token and `401` without (quickstart § 3 rows 11–12).

- [X] T033 [US3] In src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java add `ac010TheAliasCollectionIsUnsupported` (`400`, unsupported-attribute message), `ac011TheAliasSingleRecordRouteIsUnsupported` over `70`, `15` and `abc` (all `400` with the unsupported-attribute message: the type is checked before the id) and `ac012AuthenticationIsCheckedBeforeTheAlias` over the two alias paths without a token (`401`) (AC-010, AC-011, AC-012)

**Checkpoint**: US3 complete.

---

## Phase 7: User Story 5 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: Appointment titles, base locations and every unbuilt type behave exactly as before.

**Independent Test**: 002's and 003's acceptance tests pass unchanged; the 18 unsupported names return `400` (quickstart § 3 rows 16–17).

- [X] T034 [US5] Confirm with `git diff main -- src/functionalTest/resources/golden/appointment_titles.json src/functionalTest/resources/golden/base_locations.json` that both golden files are unchanged, and add to src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java `ac015OtherTypesNeverCarrySalaried`: no record in the `appointment_titles` or `base_locations` collection has a `salaried` property (AC-015, SC-006)
- [X] T035 [US5] In src/functionalTest/java/…/jo/ContractTypesFunctionalTest.java add `ac017QueryParametersReturn400`, parameterised over `?salaried=true`, `?name=Fee-paid` and `/120?salaried=true`: `400`, query-parameter message; and the inherited checks from 002 EC-002–EC-013 as in BaseLocationsFunctionalTest (`ec009UnmatchedPathsReturn404` for a trailing slash and extra segment, `ec009OtherMethodsReturn405`, `ec009LeadingZerosAndOversizedIds`) (AC-017, EC-009). Unacceptable media types, the order of checks and unexpected failures (002 EC-009 to EC-012) are type-agnostic and already covered by `ErrorShapeIntegrationTest` and 002's functional tests, so they aren't repeated per type. AC-016 is covered by T018
- [X] T036 [US5] Run `./gradlew functionalTest` and confirm 002's and 003's functional classes pass with their assertions unchanged (AC-015)

**Checkpoint**: US5 complete — no regressions.

---

## Phase 8: User Story 6 - Maintainer Adds the Type Without Type-Specific Code (Priority: P2)

**Goal**: The change is configuration, data and type-agnostic code, enforced by the guard.

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment; the diff of `src/main` names no type (quickstart § 2).

- [X] T037 [P] [US6] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java add `"contract_type"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, and add a parameterised recognition test for `contract_type`, `contract_types`, `CONTRACT_TYPE`, `contract type`, `Contract Type`, `contract-type`, `ContractType`, `contractType`, `myContractType` and `XCONTRACT_TYPE`, and a non-matching test for `contract`, `type`, `contractual type` and `salaried`; update the comment listing the fragments (research R8; FR-016, AC-019)
- [X] T038 [US6] Run `git diff --stat main -- src/main` and record in specs/004-contract-types-reference-data/plan.md § Implementation notes that the only Java changes are T002–T008 (boolean fields) and the resources changes are application.yml and contract_types.json, with no type named (AC-018, SC-007)
- [X] T039 [US6] Confirm AC-020 (OpenAPI) and AC-021 (start-up validation) are covered by T017 and T009–T010 respectively, and note the mapping in specs/004-contract-types-reference-data/plan.md § Implementation notes

**Checkpoint**: US6 complete.

---

## Phase 9: User Story 7 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise contract types from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 6).

- [X] T040 [US7] Add a "Contract Types" sub-folder under "Reference Data" in postman/ctam-jomockapi.postman_collection.json, after "Base Locations", with ten requests and test scripts, using only the environment variables `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`: the collection (`200`, 14 results, every record has boolean `salaried`, first is Fee-paid); `/120` (`200`, Salaried, `salaried` `true`); `/10` (`200`, Fee-paid, `salaried` `false`); `/15` (`404`); `/abc` (`400`); `?salaried=true` (`400`); trailing slash (as the Base Locations folder asserts); missing token (`401`); invalid token (`401`); `contract_type` (`400`, "deprecated alias not served"). Update postman/ctam-jomockapi.postman_environment.json only if a new variable is needed (AC-022, FR-012; research R9)
- [X] T041 [US7] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; record request and assertion counts (0 failures) in specs/004-contract-types-reference-data/plan.md § Implementation notes (AC-022)

**Checkpoint**: All user stories complete.

---

## Phase 10: Polish & Cross-Cutting Concerns

- [X] T042 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java add `CONTRACT_TYPES` to the collection paths of the per-collection p95 guard (FR-014, SC-008; research R7)
- [X] T043 [P] Update README.md § Reference data: three configured types, with a `contract_types` row (14 records; no alias served: `contract_type` returns `400`) and a paragraph on `salaried` (a boolean on contract-type records only); update § Adding a reference-data type to document `boolean-fields: [salaried]` in the YAML example and the fixture field list, alongside `reference-fields` and `unique-names`, and add one sentence there: `reference-fields` and `boolean-fields` choose from fixed sets (`ReferenceField`, `BooleanField`); a type that needs a field outside them needs a small, type-agnostic code change (a new enum constant and a record and response property), as 003 and 004 made (FR-013; research R9)
- [X] T044 [P] Update CLAUDE.md § Current repo state with a `004-contract-types-reference-data` line (contract types via configuration and fixture; generic `boolean-fields` with `salaried`; alias not served; DF-2 for appointments) and widen the `NoTypeSpecificCodeTest` sentence if needed
- [X] T045 [P] Add a one-line pointer to specs/004-contract-types-reference-data/contracts/reference-data-api-contract-types.md in specs/002-reference-data-api/contracts/reference-data-api.md, next to the existing 003 pointer (research R9)
- [X] T046 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`; record per-suite test counts, JaCoCo line/branch coverage and each collection's strict p95 in specs/004-contract-types-reference-data/plan.md § Implementation notes (FR-014, FR-015, SC-008)
- [X] T047 Walk through specs/004-contract-types-reference-data/quickstart.md §§ 1–6 against `./gradlew bootRun` and record that every expected result matched (or the differences) in specs/004-contract-types-reference-data/plan.md § Implementation notes

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story.
  - T002, T003 first (T003's accessor is used by T002's enum; both compile together); then T004 → T005 → T006 (constructor and properties changes ripple into the registry); T007 and T008 after T003.
  - T009–T013 after T002–T008. T014 any time; T015 after T007 and T014 (the fixture only loads once `salaried` is accepted). T016–T021 after T015. T022 last.
- **User stories (Phases 3–9)**: all after Phase 2. They touch the same functional test class, so run them in order US1 → US2 → US4 → US3 → US5 → US6 → US7, or have one person own that file. T037 (guard) and T040 (Postman) touch other files and can run alongside.
- **Polish (Phase 10)**: after the stories; T042–T045 in parallel, then T046, then T047.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **US2, US4 (P1)**, **US3, US5, US6 (P2)**, **US7 (P3)**: each needs only Phase 2 functionally; US7's Postman run (T041) is most useful once US1–US6 are green.

### Parallel opportunities

- Phase 2: T002, T003 and T008 (different files); T009–T013 (different test files); T016, T017 and T018.
- Stories: T037 (guard) and T040 (Postman) alongside the functional-test tasks.
- Polish: T042–T045.

### Parallel example: Phase 2 unit tests

```text
T009 ReferenceDataTypeRegistryTest   — boolean-fields declaration
T010 FixtureReferenceDataRepositoryTest + fixtures — boolean-field validation
T011 ReferenceDataTypeTest           — set copying
T012 ReferenceDataMapperTest         — salaried mapping
T013 ReferenceDataControllerTest     — salaried serialisation
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1 → Phase 2 (build green with the type live).
2. Phase 3 (US1): the collection works and is tested. **Stop and validate**: quickstart § 3 rows 1–6.

### Incremental delivery

3. US2 and US4 (remaining P1) → US3, US5, US6 (P2) → US7 (P3), validating each checkpoint.
4. Polish: smoke guard, docs, final gates and quickstart walk-through.

### Notes

- Commit after each phase or logical group, with the constitution untouched (no amendment is needed for this feature).
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
