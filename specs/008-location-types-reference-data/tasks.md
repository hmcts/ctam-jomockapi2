---

description: "Task list for 008-location-types-reference-data"
---

# Tasks: Reference Data API — Location Types

**Input**: Design documents from `/specs/008-location-types-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-location-types.md](./contracts/reference-data-api-location-types.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-017, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. The generic text-field capability is built test-first (its unit tests come before the code they cover).

**Organization**: Tasks are grouped by user story (spec.md US1–US8) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US5), then P2 (US4, US6, US7), then P3 (US8). The generic capability is in Phase 2, not under US7: the LocationType fixture cannot load without it, so it blocks every story. US7 then verifies its scope.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US8)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. The base package is `uk.gov.hmcts.ctam.jo`, abbreviated below as `…/jo/`.
- **Main**: `src/main/java/…/jo/` (generic changes only; no type named anywhere) and `src/main/resources/`.
- **Suites**: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/` and `src/smokeTest/java/…/jo/`.
- **Shared helpers**:
  - `src/testFixtures/java/…/jo/testsupport/` (`ContractPaths`, `ContractMessages`);
  - `src/test/java/…/jo/testsupport/ReferenceDataFixtures.java` (unit-test builders: `plainType`, `referencingType`, `nullableParentType`, `salariedType`, `fullType`, response helpers);
  - `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java` (`get`, `getAccepting`, `send`, `nameOf`, `assertError`, `assertUnauthorized`, `byId`, `golden`, `json`, `CONTRACT_FIELDS`).

Expected values come from [data-model.md § 4](./data-model.md) and the [contract delta](./contracts/reference-data-api-location-types.md). Error messages are the constants in `ContractMessages`. Postman uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

Two text rules apply throughout:
- No main source file may contain "location" (or any other type name) in any spelling. The new enum is `TextField`, its constant `INTEGRATION_CODE`; say "record's type", never "location type", in main-code comments and OpenAPI descriptions.
- New component and parameter positions are **last** (`TypeProperties.textFields`, the `ReferenceDataType` constructor), except in `ReferenceDataResponse`, where `integrationCode` sits after `jurisdictionId` (research R4).

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [X] T001 Run `./gradlew check -PskipOwasp` on the branch before any change. Record the per-suite test counts and the overall JaCoCo line and branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/008-location-types-reference-data/plan.md.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the generic text-field capability test-first, ship the type (configuration and data), and keep the build green. Adding the type immediately changes what existing tests assert (research R9), so that also belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

### Tests first for the generic capability (they fail or don't compile until T007–T013)

- [X] T002 [P] In src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java:
  - pass a new eighth `TypeProperties` argument, `textFields` (`List.of()` where unused), at all eleven `new TypeProperties(…)` calls;
  - add a case where `text-fields: [integration_code]` resolves to `EnumSet.of(TextField.INTEGRATION_CODE)`;
  - add cases where start-up fails with `IllegalStateException` for a null entry (`Invalid reference-data type widgets: empty entry in text-fields`), an unknown name (`unknown text field 'colour'`) and a duplicate (`duplicate text field integration_code`), following the file's existing message assertions;
  - add a case where omitting the list gives an empty set.

  (Research R2; FR-019.)
- [X] T003 [P] In src/test/java/…/jo/entity/ReferenceDataTypeTest.java:
  - pass the new text-field set (or `null`) at the five `new ReferenceDataType(…)` calls;
  - add a test that the text-field set is copied (a later change to the caller's set doesn't reach the type), is unmodifiable, and is empty when null is passed.

  (Research R2.)
- [X] T004 [P] Update src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java (nine `TypeProperties` calls, one `ReferenceDataType` call) and src/test/java/…/jo/services/ReferenceDataServiceTest.java (one `TypeProperties` call) to pass the new argument. Add fixtures under src/test/resources/reference-data/:
  - `valid-text-fields.json`: two records each with `"integration_code"` (`"WIDGET_A"`, `"WIDGET_B"`);
  - `invalid/text-field-missing.json`: a record without the key;
  - `invalid/text-field-explicit-null.json`: `"integration_code": null`;
  - `invalid/text-field-number.json`: `"integration_code": 7`;
  - `invalid/text-field-boolean.json`: `"integration_code": true`;
  - `invalid/text-field-object.json`: `"integration_code": {}`;
  - `invalid/text-field-empty.json`: `""`;
  - `invalid/text-field-blank.json`: `"   "`;
  - `invalid/text-field-untrimmed.json`: `" WIDGET_A"`.

  Add tests, with `text-fields: [integration_code]` unless stated:
  - the valid fixture loads and keeps both values;
  - missing key fails with `record id <id>: integration_code is required`;
  - empty and blank fail with `integration_code must not be blank`; untrimmed fails with `integration_code must be trimmed`;
  - explicit null, number, boolean and object fail while the JSON is read, before any declaration is checked. Add them to `failsStartupOnUnparseableFixtures`'s `@CsvSource`, which uses the undeclared `widgets` type. Also add one parameterised case that runs the same four fixtures with `text-fields: [integration_code]` declared (spec FR-019's case). Each message must name `integration_code` and the record's position;
  - with **no** `text-fields` declared, `valid-text-fields.json` fails with `record id <id>: integration_code is not a text field of widgets`.

  (Research R3; FR-019, AC-022.)
- [X] T005 [P] In src/test/java/…/jo/testsupport/ReferenceDataFixtures.java and src/test/java/…/jo/mappers/ReferenceDataMapperTest.java:
  - pass the new arguments in `type(…)` (`TypeProperties`) and `resolvedType(…)` (`ReferenceDataType`);
  - add `integrationCode` (null) after `jurisdictionId` at every `new ReferenceDataResponse(…)` call (six in the fixtures, one in the mapper test), and the same in src/test/java/…/jo/domain/ReferenceFieldValueTest.java (one call);
  - add `textType()` (declares `integration_code`) and `textResponse(id, name, code)` helpers.

  Add mapper cases: a record with `integrationCode("WIDGET_A")` under `textType()` maps to `textResponse(…, "WIDGET_A")`; a plain record maps to `integrationCode() == null`.

  (Research R4, R8.)
- [X] T006 After T005 (it uses T005's response helpers, and edits `ReferenceFieldValueTest` after T005's constructor change): In src/test/java/…/jo/controllers/ReferenceDataControllerTest.java, add two cases mirroring the `salaried` ones: a response with `integrationCode` "WIDGET_A" writes `$.integration_code` = "WIDGET_A", and the raw JSON has the key straight after `jurisdiction_id` when that is present (use `fullResponse` plus a code, or a dedicated helper); a plain response has no `$.integration_code`. In src/test/java/…/jo/domain/ReferenceFieldValueTest.java, extend the field-order assertion to `id, name, type_id, parent_id, jurisdiction_id, integration_code, salaried, created_at, …`. (Research R4.)

### Generic capability

- [X] T007 In src/main/java/…/jo/entity/ReferenceDataRecord.java, add after `salaried`: `@JsonProperty("integration_code") @JsonSetter(nulls = Nulls.FAIL) String integrationCode;` with the Javadoc "Null unless the type declares {@link TextField#INTEGRATION_CODE}. An explicit {@code null} fails, as for every text field." In the same change (the enum's accessor needs that getter), create src/main/java/…/jo/entity/TextField.java, modelled on `BooleanField`: `public enum TextField implements DeclaredField<String>` with `INTEGRATION_CODE("integration_code", ReferenceDataRecord::getIntegrationCode)`, and the Javadoc "An optional text field. A type declares the fields its records carry ({@code text-fields}); records of every other type never carry them." In src/main/java/…/jo/entity/DeclaredField.java, extend the Javadoc's list to `{@code reference-fields}, {@code boolean-fields}, {@code text-fields}`. (Research R2, R3.)
- [X] T008 In src/main/java/…/jo/config/ReferenceDataProperties.java, add `List<String> textFields` as the last `TypeProperties` component, after `nullableReferenceFields`, copied with `copyOf`. Extend the class Javadoc with one sentence: `text-fields` lists the text fields (e.g. `integration_code`) the type's records carry; it defaults to none. (Research R2.)
- [X] T009 In src/main/java/…/jo/entity/ReferenceDataType.java, add `Set<TextField> textFields` as the last field and constructor parameter, copied with the existing `copyOf(…, TextField.class)`, null meaning none; mention text fields in the class and constructor Javadoc. T003 must pass. (Research R2.)
- [X] T010 In src/main/java/…/jo/services/ReferenceDataTypeRegistry.java › `toType`, call `requireNoEmptyEntry(name, "text-fields", properties.textFields())`, resolve with `declaredFields(name, "text field", properties.textFields(), TextField.class)` and pass the set to `ReferenceDataType`. Add "unknown or repeated text field" to the class Javadoc's list and `{@code text-fields}` to `declaredFields`' Javadoc. T002 must pass. (Research R2.)
- T011 *(Merged into T007: `TextField`'s accessor needs the record's getter, so both land together.)*
- [X] T012 In src/main/java/…/jo/repository/FixtureReferenceDataRepository.java, add `validateTextFields(fixture, type, record)`, called in `load` right after `validateBooleanFields`:
  - first `validateDeclaredFields(fixture, type, record, "text field", TextField.values(), type.getTextFields())`;
  - then, for each declared text field, `value.isBlank()` → `invalid(fixture, id, "<field> must not be blank")`, and `!value.equals(value.strip())` → `invalid(fixture, id, "<field> must be trimmed")`.

  Update `validateDeclaredFields`' Javadoc to say it serves boolean and text fields. T004 must pass, along with every existing repository test. (Research R3; data-model.md § 2.)
- [X] T013 In src/main/java/…/jo/domain/ReferenceDataResponse.java, add the `integrationCode` component after `jurisdictionId`, exactly as research R4 gives it (`@Schema` description "E-Links' code for the record's type, e.g. MAGS_AC_LJA. Present only for types whose records carry it (deviation D-4 from the E-Links schema).", `NOT_REQUIRED`, example `MAGS_AC_LJA`; `@JsonProperty("integration_code")`; `@JsonInclude(NON_NULL)`). Insert `"integration_code"` after `"jurisdiction_id"` in `@JsonPropertyOrder`, and mention D-4 in the class Javadoc. Leave src/main/java/…/jo/mappers/ReferenceDataMapper.java unchanged: MapStruct maps the field by name, and `unmappedTargetPolicy = ERROR` fails the compile if it doesn't.

  In the same change, update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java › `referenceDataResponseMatchesTheContractSchema`: the property order gains `integration_code` after `jurisdiction_id`; `integration_code`'s types are exactly `string`, it is not nullable, not in `required`, and its description contains `deviation D-4`.

  T005 and T006 must pass. (Research R4; FR-013.)
- [X] T014 Run `./gradlew test integrationTest functionalTest`. Every existing test must pass with only the T002–T006 changes and T013's contract-test change, and every existing golden file must still match byte for byte. That is the proof that the six existing types' responses are unchanged.

  No exception: any failure stops the phase.

### The type, its data and the tests that change

- [X] T015 Generate src/main/resources/reference-data/location_types.json with a throwaway script kept outside the repository (the session scratchpad; never committed), following data-model.md § 4 rules 1–7 exactly:
  - read the gitignored `joh-elinks-api/ReferenceData/REF_LocationType.csv` with a CSV reader, as UTF-8;
  - copy `id`, `name`, `jurisdiction_id` and `integration_code` unchanged;
  - sort by `id`; ended iff `position % 7 == 0`; 002's dates;
  - write the fields in data-model.md § 3's order, two-space indented, UTF-8 without BOM, with a trailing newline.

  Before continuing, have the script confirm, and record in specs/008-location-types-reference-data/plan.md § Implementation notes:
  - 16 rows of 9 columns; ids 1 to 16; 13 distinct names;
  - the four copied columns equal the extract row for row (SC-003);
  - every code matches `^[A-Z_]+$`, 7 to 22 characters;
  - ended ids exactly {7, 14};
  - every row of data-model.md § 4's Records table;
  - every `type_id` in the checked-in src/main/resources/reference-data/locations.json and base_locations.json is an id in the new file, and each record's `jurisdiction_id` equals its type's.

  (Research R5.)
- [X] T016 Append the type entry after `locations` under `jo.reference-data.types` in src/main/resources/application.yml, with the comments of research R1: `name: location_types`, `fixture: classpath:reference-data/location_types.json`, `reference-fields: [jurisdiction_id]`, `text-fields: [integration_code]`, `unique-names: false`. No `aliases`, `boolean-fields` or `nullable-reference-fields`. Update the `base_locations` comment to "parent_id and type_id are ids of locations and location types (checked by ReferenceDataReferencesTest, 007, 008); jurisdiction_id is an id of a record in a type not served yet (constitution Principle IV forward reference, 003 DF-1)", and the `locations` comment to "parent_id is the id of another location and type_id of a location type (008); jurisdiction_id is a forward reference (007 DF-5)". (Research R1.)
- [X] T017 [P] Add `public static final String LOCATION_TYPES = "/api/v1/reference_data/location_types";` after `LOCATIONS` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java.
- [X] T018 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java (contract delta § 1; AC-023, FR-013):
  - **enum**: `ATTRIBUTE_NAMES` becomes exactly `appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, appointment_title`;
  - **description**: becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types. Also supports deprecated values: appointment_title`, wrapped within 120 characters.
- [X] T019 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java › `UnsupportedTypeTests`:
  - remove only `location_types` from the contract-name list, so it holds the 14 still-unsupported names (`location_type` stays);
  - add `Location_Types`, `LOCATION_TYPES`, `location-types`, `locationTypes` and `location_types_` to the case variants;
  - change the list comment to "The other 14 E-Links attribute names: four canonical types, their four deprecated aliases, and base_location, contract_type, gender, judiciary_role, location and location_type, which 003–008 don't serve";
  - extend the "Also covers" note with 008 AC-019 and EC-008 (specs/008-location-types-reference-data).

  (Research R9.)
- [X] T020 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged. Only if it fails because of the new `TypeProperties` component or the merged type list, fix it in that file (research R9).
- [X] T021 In src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R6; FR-017):
  - **Declarations**: add `List<String> textFields` to `Declaration`, read with `inResponseOrder(TextField.values(), type.textFields())`;
  - **Field order**: `fieldOrder` puts text fields after reference fields and before boolean fields;
  - **Shape**: each declared text field is a JSON string, non-empty and equal to its trimmed form;
  - **Text format**: add a constant `TEXT_FORMATS = Map.of("integration_code", Pattern.compile("[A-Z_]{7,22}"))`, next to `JURISDICTION_IDS`. Every declared text field that has an entry must match it in full (spec EC-004). The failure message names the fixture, the record's position and the value;
  - **Reference points**: add `List<String> texts` to `ReferencePoint` (empty by default), a `withTexts(String…)` builder, and compare it like `references` and `booleans`;
  - **New row**: `new Fixture("location_types.json", 16, 13, Map.of(), IdRule.SOURCE, List.of(…))` with the points 1 Region (30, `COURTS_REGION`), 6 Region (84, `MAGS_REGION`), 7 AC (ended; 84, `MAGS_AC`), 10 Area or Parent AC (84, `MAGS_AREA_OR_PARENT_AC`), 11 AC (84, `MAGS_AREA_AC`), 14 Region (ended; 86, `CORONERS_REGION`) and 16 Geographic Area (86, `CORONERS_AREA`), each `.withReferences(<jurisdiction>).withTexts(<code>)`;
  - **Class Javadoc**: mention text fields and 008 data-model.md § 4.
- [X] T022 In src/test/java/…/jo/repository/ReferenceDataReferencesTest.java › `references()`, add `new Reference("locations", "type_id", "location_types")` and `new Reference("base_locations", "type_id", "location_types")`, with a comment that they close 007 DF-5 and 003 DF-1 for `type_id`. Configuring `location_types` (T016) makes `everyReferenceBetweenServedTypesIsChecked` demand exactly these rows, and `everyReferenceResolves` then checks them (research R7; FR-011).
- [X] T023 Create src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java mirroring src/functionalTest/java/…/jo/LocationsFunctionalTest.java:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, and the Javadoc "008's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.";
  - `ReferenceDataHttp` and `ContractPaths.LOCATION_TYPES`;
  - a `SINGULAR` constant, `/api/v1/reference_data/location_type`, with the Javadoc "The singular name, which 008 doesn't serve: deprecated names are out of scope.";
  - constants `OPEN_UPDATED_AT`, `ENDED_UPDATED_AT` and `END_DATE`, and `FIELDS`: `id, name, jurisdiction_id, integration_code, created_at, updated_at, start_date, end_date`;
  - a `RECORDS` table holding all 16 rows of data-model.md § 4 (id, name, jurisdiction, code, ended), and a private helper `assertRecord(record, row)`.

  Story phases add test methods directly to this class.
- [X] T024 Run `./gradlew check -PskipOwasp`. Every suite must be green, including:
  - `ReferenceDataFixtureRulesTest` (seven fixtures, two id rules, text fields, the literal-character check);
  - `ReferenceDataReferencesTest`, with T022's two `type_id` rows;
  - `NoTypeSpecificCodeTest` (unchanged);
  - the earlier types' functional tests;
  - the JaCoCo gate.

**Checkpoint**: The capability and the type are live. Existing tests reflect seven supported types, the existing golden files are unchanged, and the build is green.

---

## Phase 3: User Story 1 - Integrator Retrieves All Location Types (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 16 location types, exactly as in the source, deterministically.

**Independent Test**: `GET /api/v1/reference_data/location_types` with a valid token returns `200` and 16 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–6).

- [X] T025 [US1] In src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java, add `ac001ReturnsAll16LocationTypesInAscendingIdOrder`: `200`, `application/json`, and `results` of 16 records with ids 1 to 16 in order. Each record has exactly `FIELDS`, in order, with no `type_id` or `parent_id` (AC-001, FR-005).
- [X] T026 [US1] In the same file, add `ac002RecordsMatchTheRecordsTable`: every record equals its `RECORDS` row (name, `jurisdiction_id`, `integration_code`, `end_date`, `updated_at`), and every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01 (AC-002, SC-003).
- [X] T027 [US1] In the same file, add `ac003RepeatedNamesStaySeparate`: "Region" is exactly ids 1, 6 and 14 with jurisdictions 30, 84 and 86; "AC" is exactly ids 7 and 11; 13 distinct names in total (AC-003, EC-002).
- [X] T028 [US1] In the same file, add `ac004ExactlyIds7And14AreEnded`. On the raw body, exactly 2 `"end_date":"2025-03-31"` and 14 `"end_date":null`; on the parsed records, the ended ids are exactly 7 and 14, each with `updated_at` 2025-04-01T08:00:00Z (AC-004, EC-005, EC-006).
- [X] T029 [US1] Capture the collection response from a running instance: run `./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/location_types.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/location_types`.
  - The file must be the compact body exactly as served, with no reformatting and no trailing newline.
  - Check that its 16 records equal the fixture's, field for field.
  - Then add `ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile`, as in LocationsFunctionalTest.

  (AC-005.)

**Checkpoint**: US1 complete. This is the MVP: integrators can load location types.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Location Type by ID (Priority: P1)

**Goal**: The single-record route returns one location type and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/location_types/14` returns `200` with "Region", `jurisdiction_id` 86, `CORONERS_REGION` and `end_date` 2025-03-31 (quickstart § 3 rows 7–11).

- [X] T030 [US2] In src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java, add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 1, 7, 10, 14 and 16. Each returns `200` with a single object (no `results`) equal to the collection element with that id (AC-006).
- [X] T031 [US2] In the same file, add:
  - `ac007UnknownIdsReturn404` over `17`, `999` and `999999` (`404`, record-not-found message);
  - `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message).

  (AC-007, AC-008, EC-001.)
- [X] T032 [US2] In the same file, add `ac009IdsAreScopedToTheirType`: under `location_types`, id 1 is "Region" and id 10 "Area or Parent AC"; id 1 is "Aberconwy" under `locations`, and id 10 is "Aberconwy" under `base_locations` (AC-009, EC-007).

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 3 - Integrator Resolves Every Location's Type (Priority: P1)

**Goal**: Every `type_id` a location or base location carries names a served location type with the same jurisdiction; 003 DF-1 and 007 DF-5 are closed for `type_id`.

**Independent Test**: `ReferenceDataReferencesTest` passes, and so do the HTTP checks (quickstart § 3 rows 17–18).

- [X] T033 [P] [US3] In src/test/java/…/jo/repository/ReferenceDataReferencesTest.java, after T022's `type_id` rows (research R7; FR-012):
  - **Agreement table**: add `record Agreement(String source, String via, String target, String field)` and a `@MethodSource` of `("locations", "type_id", "location_types", "jurisdiction_id")` and `("base_locations", "type_id", "location_types", "jurisdiction_id")`;
  - **New test** `everyAgreementHolds`: for each row, every source record's `field` equals the `field` of the target record whose id is its `via` value; the failure message names the source fixture, record id, both values and the target id;
  - **Class Javadoc**: add the agreement rule (constitution Principle IV, internal consistency; 008 FR-012).
- [X] T034 [US3] In src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java, add:
  - `ac010EveryTypeIdResolves`: over HTTP, every `type_id` in the `locations` collection (16 distinct, 1–16) and in the `base_locations` collection (2, 5, 8, 9, 11, 12, 13, 16) is a served location-type id;
  - `ac011JurisdictionsAgreeWithTheirType`: every location's and base location's `jurisdiction_id` equals its type's;
  - `ac012JurisdictionCodesAreCopied`: every location type's `jurisdiction_id` is in {30, 34, 84, 86}, never null, with counts 2, 3, 8 and 3.

  (AC-010 to AC-012.)

**Checkpoint**: US1–US3 complete.

---

## Phase 6: User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each location-type route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 14–15).

- [X] T035 [US5] In src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java, add:
  - `ac016MissingTokenIsRejected`, parameterised over the collection, `/1`, `/17` and `/abc`: `assertUnauthorized`;
  - `ac017InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty), each on the collection, `/1` and `/abc`.

  (AC-016, AC-017.)

**Checkpoint**: All P1 stories complete.

---

## Phase 7: User Story 4 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `location_type` is rejected like any unsupported name.

**Independent Test**: `location_type` and `location_type/{id}` return `400` with a token and `401` without (quickstart § 3 rows 12–13).

- [X] T036 [US4] In src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java, add:
  - `ac013TheSingularCollectionIsUnsupported`: `400`, unsupported-attribute message, no `results`;
  - `ac014TheSingularSingleRecordRouteIsUnsupported` over `1`, `17` and `abc`: all `400` with the unsupported-attribute message;
  - `ac015AuthenticationIsCheckedBeforeTheSingularName` over the two singular paths, with no token and with `Bearer wrong-token`: `401`.

  (AC-013 to AC-015.)

**Checkpoint**: US4 complete.

---

## Phase 8: User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unbuilt type behave exactly as before.

**Independent Test**: 002's to 007's acceptance tests pass unchanged, every existing golden file matches, and the 14 unsupported names return `400` (quickstart § 2 and § 3 rows 16, 19).

- [X] T037 [US6] Confirm with `git diff origin/main -- src/functionalTest/resources/golden/` that the existing golden files (`appointment_titles.json`, `base_locations.json`, `contract_types.json`, `genders.json`, `judiciary_roles.json`, `locations.json`) are unchanged and only `location_types.json` is new (AC-018, SC-009).
- [X] T038 [US6] In src/functionalTest/java/…/jo/LocationTypesFunctionalTest.java, add the following, as in LocationsFunctionalTest:
  - `ac020QueryParametersReturn400`, parameterised over `?jurisdiction_id=84`, `?page=2`, `?x=` and `/1?name=Region`: `400`, query-parameter message;
  - the inherited checks `ec009UnmatchedPathsReturn404` (trailing slash, `/1/`, `/1/extra`), `ec009OtherMethodsReturn405` (POST, PUT, PATCH and DELETE on the collection and `/1`), `ec009UnacceptableMediaTypesReturn406` (`application/xml` on the collection and `/1`) and `ec009LeadingZerosAndOversizedIds` (`/0001` → id 1; `/99999999999999999999` → `404`);
  - `ec004OtherTypesDoNotCarryIntegrationCode`: no record in the `locations`, `base_locations` or `contract_types` collection has an `integration_code` key (FR-005, FR-019).

  AC-019 is covered by T019. (AC-020, EC-009.)
- [X] T039 [US6] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T019's list changes (AC-018).

**Checkpoint**: US6 complete, with no regressions.

---

## Phase 9: User Story 7 - Maintainer Adds the Type With One Generic Change (Priority: P2)

**Goal**: The only main-code change is the generic text-field capability; the guard still enforces "no type named".

**Independent Test**: `NoTypeSpecificCodeTest` passes unchanged, and the main-code diff is limited to the files in quickstart § 2.

- [X] T040 [US7] Run `git diff --stat origin/main -- src/main/java` and `git diff --stat origin/main -- src/main/resources`.
  - **`src/main/java`**: expect only the eight files listed in quickstart § 2 (seven changed and the new `TextField`); `ReferenceDataMapper` and `ReferenceDataService` unchanged.
  - **`src/main/resources`**: expect only `application.yml` and `location_types.json`.
  - **Check**: confirm `git grep -n -i -E 'location|appointment|contract_type|gender|judiciary' -- src/main/java` prints nothing, and `git diff origin/main -- src/test/java/uk/gov/hmcts/ctam/jo/architecture/NoTypeSpecificCodeTest.java` is empty.
  - **Record**: write the result in specs/008-location-types-reference-data/plan.md § Implementation notes (AC-021, AC-022, SC-010).
- [X] T041 [US7] Confirm the coverage mapping and note it in specs/008-location-types-reference-data/plan.md § Implementation notes: AC-022 (text-field start-up behaviour) is covered by T002 and T004, and AC-023 (OpenAPI: `location_types` listed, `location_type` not, `integration_code` an optional non-nullable string) by T013 and T018.

**Checkpoint**: US7 complete.

---

## Phase 10: User Story 8 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise location types from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [X] T042 [US8] In postman/ctam-jomockapi.postman_collection.json, insert text so the existing formatting is untouched (as in 004–007): add a "Location Types" sub-folder under "Reference Data", after "Locations", with ten requests and test scripts:
  - **the collection**: `200`; 16 results; every record has the eight fields in order; ids 1 to 16 in order; first `1` "Region" `COURTS_REGION`; last `16` "Geographic Area" `CORONERS_AREA`; ended ids exactly 7 and 14; no record has `type_id` or `parent_id`;
  - **`/1`**: `200`, "Region", `jurisdiction_id` 30, `integration_code` `COURTS_REGION`;
  - **`/14`**: `200`, "Region", `jurisdiction_id` 86, `end_date` 2025-03-31;
  - **`/17`**: `404`;
  - **`/abc`**: `400`;
  - **`?jurisdiction_id=84`**: `400`;
  - **trailing slash**: `404`, "Resource not found.";
  - **missing token**: `401`, `WWW-Authenticate: Bearer`;
  - **invalid token**: `{{invalidBearerToken}}`, `401`;
  - **`location_type`**: `400`, "singular name not served", no `results`.

  (AC-024, FR-014; research R10.)
- [X] T043 [US8] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`. Record the request and assertion counts (0 failures) in specs/008-location-types-reference-data/plan.md § Implementation notes (AC-024).

**Checkpoint**: All user stories complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

- [X] T044 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java, add `LOCATION_TYPES` to the collection paths of the per-collection p95 guard (FR-016, SC-011; research R9).
- [X] T045 [P] Update README.md (FR-015; research R11):
  - **§ Reference data**: "Seven types are configured", with a `location_types` row (16 records; no alias served: `location_type` returns `400`), and a short paragraph: location-type records keep the E-Links ids 1 to 16 and carry `jurisdiction_id` and `integration_code`; names repeat; ids 7 and 14 are ended by the synthetic rule.
  - **Base-location and location paragraphs**: say that every `type_id` is now the id of a served location type, with the same jurisdiction; only `jurisdiction_id` still refers to an unserved type.
  - **§ Adding a reference-data type**: add `text-fields: [integration_code]   # optional` to the YAML example and a paragraph: every record must have each declared text field as a non-empty, trimmed JSON string (not `null`, a number or a boolean), no record may have an undeclared one, and responses write it after the reference fields.
- [X] T046 [P] Update CLAUDE.md:
  - **§ Current repo state**: add a `008-location-types-reference-data` line covering: location types through configuration, a fixture and one generic capability (`text-fields`, modelled by the `TextField` enum, currently only `integration_code`; contract deviation D-4); 16 records with E-Links ids, 2 ended; `ReferenceDataReferencesTest` gains the `type_id` rows (closing 003 DF-1 and 007 DF-5 for `type_id`) and a jurisdiction-agreement table; DF-7; the singular name not served; `NoTypeSpecificCodeTest` unchanged.
  - **§ Reference data**: add location-type names, ids, jurisdiction codes and integration codes to the public values taken from the extracts, and `specs/008-location-types-reference-data/data-model.md` § 4 to the list.
- [X] T047 [P] Add a one-line pointer to specs/008-location-types-reference-data/contracts/reference-data-api-location-types.md in specs/002-reference-data-api/contracts/reference-data-api.md, after the 007 pointer. In specs/003-base-locations-reference-data/spec.md, extend the DF-1 note: "`type_id` closed by 008 (`ReferenceDataReferencesTest`); `jurisdiction_id` remains open." In specs/007-locations-reference-data/spec.md, add to the DF-5 row: "`type_id` closed by 008; `jurisdiction_id` remains open." (Research R11.)
- [X] T048 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`. Record in specs/008-location-types-reference-data/plan.md § Implementation notes the per-suite test counts, the JaCoCo line and branch coverage (the gate must hold, with the new branches covered), and each collection's strict p95 (FR-016, FR-017, SC-011).
- [X] T049 Walk through specs/008-location-types-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun`. Mark the spec's Status as Implemented in specs/008-location-types-reference-data/spec.md, and record in specs/008-location-types-reference-data/plan.md § Implementation notes that every expected result matched (or the differences).

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story. Within it:
  - T002–T005 (tests) come first and can run in parallel; T006 follows T005.
  - Then T007 (record field and `TextField`) → T008 → T009 → T010 (configuration, type, registry), then T012 (repository), then T013 (response and OpenAPI schema assertions), then T014.
  - Then T015 → T016 (the fixture before the type that loads it).
  - T017–T019 come after T016, in parallel; then T020, T021, T022, T023; T024 is last.
- **User stories (Phases 3–10)**: all after Phase 2. Most add methods to the same functional test class, so run them in order US1 → US2 → US3 → US5 → US4 → US6 → US7 → US8, or have one person own that file. T033 (references test) and T042 (Postman) touch other files and can run alongside.
- **Polish (Phase 11)**: after the stories. T044–T047 in parallel, then T048, then T049.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **Other stories**: US2, US3 and US5 (P1), US4, US6 and US7 (P2) and US8 (P3) each need only Phase 2 to work. US8's newman run (T043) is most useful once US1–US7 are green.

### Parallel opportunities

- Phase 2 tests: T002, T003, T004 and T005 (different files); T006 after T005.
- Phase 2 test updates: T017, T018 and T019.
- Stories: T033 (references test) and T042 (Postman) alongside the functional-test tasks.
- Polish: T044–T047.

### Parallel example: Phase 2 tests first

```text
T002 ReferenceDataTypeRegistryTest        — text-fields declaration cases
T003 ReferenceDataTypeTest                 — text-field set copy
T004 FixtureReferenceDataRepositoryTest    — missing / null / wrong type / blank / untrimmed / undeclared + fixtures
T005 ReferenceDataFixtures + mapper test   — new arguments, integrationCode mapping
T006 Controller + serialisation tests      — after T005; integration_code written or left out, field order
```

### Parallel example: alongside the functional tests

```text
T033 ReferenceDataReferencesTest           — jurisdiction agreement
T042 Postman collection                    — Location Types folder
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1, then Phase 2: the capability built test-first, the type live, the build green, existing golden files unchanged.
2. Phase 3 (US1): the collection works, its data is exact, and it is tested. **Stop and validate** against quickstart § 3 rows 1–6.

### Incremental delivery

3. US2, US3 and US5 (the rest of P1), then US4, US6 and US7 (P2), then US8 (P3), validating each checkpoint.
4. Polish: the smoke guard, documentation, the final gates and the quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extract is read only by T015's throwaway script; nothing in the build reads it (FR-007).
- T014 is the key regression gate for the generic change. If any existing golden file changes, stop: R4's serialisation is wrong.
- The local `main` branch may lag `origin/main`; compare against `origin/main` (fetch first).
