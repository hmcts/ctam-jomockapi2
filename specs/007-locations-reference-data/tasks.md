---

description: "Task list for 007-locations-reference-data"
---

# Tasks: Reference Data API — Locations

**Input**: Design documents from `/specs/007-locations-reference-data/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reference-data-api-locations.md](./contracts/reference-data-api-locations.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (FR-017, SC-001) and the constitution (Principle XI) require unit, integration, functional and smoke coverage. The generic nullable-reference-field capability is built test-first (its unit tests come before the code they cover).

**Organization**: Tasks are grouped by user story (spec.md US1–US8) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US5), then P2 (US4, US6, US7), then P3 (US8). The generic capability is in Phase 2, not under US7: the Location fixture cannot load without it, so it blocks every story. US7 then verifies its scope and the guard.

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
  - `src/test/java/…/jo/testsupport/ReferenceDataFixtures.java` (unit-test builders);
  - `src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java` (`get`, `getAccepting`, `send`, `nameOf`, `assertError`, `assertUnauthorized`, `byId`, `golden`, `json`, `CONTRACT_FIELDS`).

Expected values come from [data-model.md § 4](./data-model.md) and the [contract delta](./contracts/reference-data-api-locations.md). Error messages are the constants in `ContractMessages`. Postman uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`.

Two text rules apply throughout:
- The name `Cheshire  LJA` has **two** spaces, and the en dash `–` is written literally everywhere, never as an escape.
- No main source file may contain the word "location" in any spelling (research R8). In main code, say "top-level record", not "top-level location".

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [ ] T001 Run `./gradlew check -PskipOwasp` on the branch before any change. Record the per-suite test counts and the overall JaCoCo line and branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/007-locations-reference-data/plan.md.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the generic nullable-reference-field capability test-first, ship the type (configuration and data), and keep the build green. Adding the type immediately changes what existing tests assert (research R9), so that also belongs here.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green.

### Tests first for the generic capability (they fail or don't compile until T009–T015)

- [ ] T002 [P] In src/test/java/…/jo/services/ReferenceDataTypeRegistryTest.java:
  - pass a new seventh `TypeProperties` argument, `nullableReferenceFields` (`List.of()` where unused), at all nine `new TypeProperties(…)` calls;
  - add cases where `nullable-reference-fields: [parent_id]` with `reference-fields` containing `parent_id` resolves to `EnumSet.of(PARENT_ID)`;
  - add cases where start-up fails with `IllegalStateException` for: a nullable field missing from `reference-fields` (message exactly `Invalid reference-data type widgets: nullable reference field parent_id is not a reference field`), a null entry (`empty entry in nullable-reference-fields`), an unknown name (`unknown nullable reference field 'colour'`) and a duplicate (`duplicate nullable reference field parent_id`), following the file's existing message assertions;
  - add a case where omitting the list gives an empty set.

  (Research R2; FR-011.)
- [ ] T003 [P] In src/test/java/…/jo/entity/ReferenceDataTypeTest.java:
  - pass the new nullable set at the four `new ReferenceDataType(…)` calls;
  - add a test that the nullable set is copied (a later change to the caller's set doesn't reach the type), is unmodifiable, and is empty when null is passed.

  (Research R2.)
- [ ] T004 [P] Update src/test/java/…/jo/repository/FixtureReferenceDataRepositoryTest.java, and `type(…)` in src/test/java/…/jo/testsupport/ReferenceDataFixtures.java, to pass the new `TypeProperties` argument at every call (seven in the test, one in the helper). Add new fixtures under src/test/resources/reference-data/:
  - `valid-nullable-reference-fields.json`: two records with all three reference fields, one with `"parent_id": null`, one with a positive `parent_id`;
  - `invalid/nullable-reference-field-missing.json`: a record without the `parent_id` key;
  - `invalid/nullable-reference-field-not-positive.json`: `"parent_id": 0`;
  - `invalid/nullable-reference-field-string.json`: `"parent_id": "1722"`;
  - `invalid/non-nullable-reference-field-explicit-null.json`: all three reference fields, with `"parent_id": null`, loaded with a type declaring all three and none nullable.

  Add tests:
  - with `reference-fields: [type_id, parent_id, jurisdiction_id]` and `nullable-reference-fields: [parent_id]`, the valid fixture loads, and the null parent is read as null;
  - the missing key fails with `record id <id>: parent_id is required`;
  - the zero fails with `parent_id must be greater than 0`;
  - the string fails as an unparseable fixture naming `parent_id`;
  - the declared, non-nullable explicit null fails with `record id <id>: parent_id is required`;
  - the existing `reference-field-explicit-null` case (`"parent_id": null` on a type declaring no reference fields) now fails in validation, not parsing. Move it from `failsStartupOnUnparseableFixtures`'s `@CsvSource` to `failsStartupOnAnInvalidRecord`'s table, with its exact message `record id 5: parent_id is not a reference field of widgets`.

  (Research R3; FR-011, AC-022.)
- [ ] T005 [P] Create src/test/java/…/jo/domain/ReferenceFieldValueTest.java. Serialise `ReferenceDataResponse` values with a default `tools.jackson.databind.json.JsonMapper`, and assert the exact JSON:
  - all three reference components null gives no `type_id`, `parent_id` or `jurisdiction_id` key;
  - `new ReferenceFieldValue(8L)` gives `"type_id":8`;
  - `new ReferenceFieldValue(null)` for `parent_id` gives `"parent_id":null`, with the key present;
  - the field order is unchanged (`id, name, type_id, parent_id, jurisdiction_id, salaried, created_at, …`).

  (Research R4.)
- [ ] T006 [P] In src/test/java/…/jo/mappers/ReferenceDataMapperTest.java and src/test/java/…/jo/testsupport/ReferenceDataFixtures.java:
  - change `referencingResponse` and `fullResponse` to wrap their ids in `ReferenceFieldValue`;
  - add a `nullableParentResponse` helper (or equivalent) that builds a response with a null `parent_id` value;
  - make every mapper call pass a `ReferenceDataType`: one declaring no fields for plain records, one declaring all three reference fields for referencing records, one declaring `salaried` for boolean records.

  Add mapper cases:
  - a type declaring `parent_id` nullable, with a record whose parent is null, maps to `parentId = new ReferenceFieldValue(null)`;
  - a type not declaring a field maps it to `null`, so it is absent;
  - a declared field with a value maps to `new ReferenceFieldValue(value)`;
  - `toResponses` keeps list order with the type passed.

  (Research R4, R9.)
- [ ] T007 [P] Update src/test/java/…/jo/services/ReferenceDataServiceTest.java and src/test/java/…/jo/controllers/ReferenceDataControllerTest.java for the mapper's new signature and the wrapped reference values. No assertion changes meaning (research R9).

### Generic capability

- [ ] T008 [P] Create src/main/java/…/jo/domain/ReferenceFieldValue.java: `public record ReferenceFieldValue(@JsonValue Long value)`, with Javadoc: "A reference field's value in a response: present with an id, or present and null for a top-level record of a type that declares the field nullable. A response leaves out a reference field its type doesn't declare by holding no value object at all." (Research R4.)
- [ ] T009 In src/main/java/…/jo/config/ReferenceDataProperties.java, add `List<String> nullableReferenceFields` as the last `TypeProperties` component, after `uniqueNames`, copied with `copyOf` like the other lists. Extend the class Javadoc with one sentence: `nullable-reference-fields` lists the declared reference fields that may be null; it defaults to none. (Research R2.)
- [ ] T010 In src/main/java/…/jo/entity/ReferenceDataType.java:
  - add `Set<ReferenceField> nullableReferenceFields`, copied as an unmodifiable `EnumSet` like `referenceFields`, with null meaning none;
  - add it as the last constructor parameter;
  - in the class Javadoc, reword "fixture location" to "fixture path" and mention the nullable reference fields.

  (Research R2, R8.)
- [ ] T011 In src/main/java/…/jo/services/ReferenceDataTypeRegistry.java:
  - in `toType`, call `requireNoEmptyEntry(name, "nullable-reference-fields", …)`;
  - resolve the list with `declaredFields(name, "nullable reference field", …, ReferenceField.class)`;
  - fail with `Invalid reference-data type <name>: nullable reference field <field> is not a reference field` for any entry missing from the resolved reference fields;
  - pass the set to `ReferenceDataType`;
  - add "nullable reference field not declared as a reference field" to the class Javadoc's list of invalid declarations.

  T002 must pass. (Research R2.)
- [ ] T012 In src/main/java/…/jo/entity/ReferenceDataRecord.java, remove `@JsonSetter(nulls = Nulls.FAIL)` from `typeId`, `parentId` and `jurisdictionId` only; `salaried` keeps it. Reword their Javadoc: the repository decides whether a null or a missing key is allowed, from the type's declarations (data-model.md § 2). (Research R3.)
- [ ] T013 In src/main/java/…/jo/repository/FixtureReferenceDataRepository.java:
  - keep the tree from `mapper.readTree(json)` and pass it to the per-record loop in `load`;
  - after the `id is required` check and before `validate`, run a new reference-key check on that record's tree node, `records.get(position - 1)`, so its messages can use the validated record id, and fixtures with a missing id keep their existing `record at position N: id is required` message.

  For each `ReferenceField`:
  - undeclared, key present (null or not): fail with `record id <id>: <field> is not a reference field of <type>`;
  - declared, key absent: fail with `<field> is required`;
  - declared, value null, field not nullable: fail with `<field> is required`.

  Then make `validateReferenceFields` accept a null value for declared-nullable fields, while still applying `must be greater than 0` to every present value. Keep messages in the existing `invalid(…)` form. T004 must pass, along with every existing repository test. (Research R3; data-model.md § 2.)
- [ ] T014 In src/main/java/…/jo/domain/ReferenceDataResponse.java, change the `typeId`, `parentId` and `jurisdictionId` components to `ReferenceFieldValue`, keeping `@JsonProperty` and `@JsonInclude(NON_NULL)`.
  - Annotate each with `@Schema(implementation = Long.class, format = "int64", requiredMode = NOT_REQUIRED, …)`, keeping the existing descriptions and examples.
  - Add `nullable = true` to `parent_id`, and to its description add: "Null for a top-level record of a type that declares it nullable."
  - Update the class Javadoc: a reference field is left out when its type doesn't declare it, and is written as null when the type declares it nullable and the record has no value.

  In the same change, update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java › `referenceDataResponseMatchesTheContractSchema`:
  - `parent_id` is nullable, while `type_id` and `jurisdiction_id` stay not nullable;
  - all three stay `int64` integers;
  - `components.schemas` has no `ReferenceFieldValue` entry.

  If springdoc publishes a `ReferenceFieldValue` component or a non-integer type, switch to research R4's fallback (a property-level `@Schema(type = "integer", format = "int64", …)`) here.

  T005 must pass. (Research R4; FR-013.)
- [ ] T015 In src/main/java/…/jo/mappers/ReferenceDataMapper.java, add a MapStruct `@Context ReferenceDataType type` parameter to `toResponse` and `toResponses`. Map the three reference components through a default method that returns `null` when the type doesn't declare the `ReferenceField`, and `new ReferenceFieldValue(field.readFrom(record))` when it does. In src/main/java/…/jo/services/ReferenceDataService.java, pass the resolved type to both calls. T006 and T007 must pass. (Research R4.)
- [ ] T016 Run `./gradlew test integrationTest functionalTest`. Every existing test must pass with only the T002–T007 changes, and every existing golden file must still match byte for byte. That is the proof that the five existing types' responses are unchanged.

  No exception: any failure stops the phase.

### The type, its data and the tests that change

- [ ] T017 Generate src/main/resources/reference-data/locations.json with a throwaway script kept outside the repository (the session scratchpad; never committed), following data-model.md § 4 rules 1–7 exactly:
  - read the gitignored `joh-elinks-api/ReferenceData/REF_Location.csv` as UTF-8, stripping a BOM if present;
  - copy `id`, `name`, `type_id`, `parent_id` and `jurisdiction_id` unchanged, with an empty `parent_id` becoming `null`;
  - sort by `id`; ended iff `position % 7 == 0`; 002's dates;
  - write 003's field order with every key present, two-space indented, UTF-8 without BOM, non-ASCII written literally, with a trailing newline.

  Before continuing, have the script confirm, and record in specs/007-locations-reference-data/plan.md § Implementation notes:
  - 2,000 records and 1,523 distinct names;
  - the five copied columns equal the extract row for row, as code points (SC-003);
  - 142 null parents, every non-null parent an id in the file, no self-parent, maximum depth 2;
  - 285 ended records, ids 9 to 2364;
  - exactly one non-ASCII name (2398) and no `\u` in the file;
  - every row of data-model.md § 4's Reference points table;
  - every `parent_id` in the checked-in src/main/resources/reference-data/base_locations.json is an id in the new file.

  (Research R5.)
- [ ] T018 Append the type entry after `judiciary_roles` under `jo.reference-data.types` in src/main/resources/application.yml: `name: locations`, `fixture: classpath:reference-data/locations.json`, `reference-fields: [type_id, parent_id, jurisdiction_id]`, `nullable-reference-fields: [parent_id]`, `unique-names: false`. No `aliases` and no `boolean-fields` (research R1).
- [ ] T019 [P] Add `public static final String LOCATIONS = "/api/v1/reference_data/locations";` after `JUDICIARY_ROLES` in src/testFixtures/java/…/jo/testsupport/ContractPaths.java.
- [ ] T020 [P] Update src/integrationTest/java/…/jo/ReferenceDataOpenApiContractTest.java (contract delta § 1; AC-024, FR-013):
  - **enum**: `ATTRIBUTE_NAMES` becomes exactly `appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, appointment_title`.
  - **description**: becomes exactly `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations. Also supports deprecated values: appointment_title`, wrapped within 120 characters.
- [ ] T021 [P] In src/functionalTest/java/…/jo/ReferenceDataFunctionalTest.java › `UnsupportedTypeTests`:
  - remove only `locations` from the contract-name list, so it holds the 15 still-unsupported names (`location` stays);
  - add `Locations`, `LOCATIONS`, `locations_` and `Location` to the case variants;
  - change the list comment to "The other 15 E-Links attribute names: five canonical types, their five deprecated aliases, and base_location, contract_type, gender, judiciary_role and location, which 003–007 don't serve";
  - extend the "Also covers" note with 007 AC-019 and EC-010 (specs/007-locations-reference-data).

  (Research R9.)
- [ ] T022 Run src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java and confirm it passes unchanged. Only if it fails because of the new `TypeProperties` component or the merged type list, fix it in that file (research R9).
- [ ] T023 In src/test/java/…/jo/repository/ReferenceDataFixtureRulesTest.java (research R6; FR-017):
  - **Id rule**: add an `IdRule` enum (`POSITIONAL`: names in code-point order and `id = 10 × position`; `SOURCE`: ids positive and strictly ascending, with no name-order rule) as a new `Fixture` component. Set `POSITIONAL` on the five existing rows and apply the rule in `followsTheGenerationRules`.
  - **Declarations**: add the nullable reference fields read from `application.yml` to `Declaration`, and accept a null value only for those fields in the reference-field shape check.
  - **Reference points**: make `ReferencePoint.withReferences` accept nulls (build the list with `Arrays.asList`).
  - **New row**: `new Fixture("locations.json", 2000, 1523, Map.of(), IdRule.SOURCE, List.of(…))` with the 12 reference points of data-model.md § 4 as `point(id, name, endDate).withReferences(typeId, parentId, jurisdictionId)`. Use `null` for the three top-level parents, `END_DATE` for 9, 1466 and 2364, and copy names character for character, including the two spaces in `Cheshire  LJA`.
  - **Class Javadoc**: update it to cover the `SOURCE` rule.
- [ ] T024 Create src/functionalTest/java/…/jo/LocationsFunctionalTest.java mirroring src/functionalTest/java/…/jo/JudiciaryRolesFunctionalTest.java:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, and the Javadoc "007's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.";
  - `ReferenceDataHttp` and `ContractPaths.LOCATIONS`;
  - a `SINGULAR` constant, `/api/v1/reference_data/location`, with the Javadoc "The singular name, which 007 doesn't serve: deprecated names are out of scope.";
  - constants `OPEN_UPDATED_AT`, `ENDED_UPDATED_AT` and `END_DATE`, and `FIELDS`: `id, name, type_id, parent_id, jurisdiction_id, created_at, updated_at, start_date, end_date`;
  - a private helper `assertRecord(record, name, typeId, parentId, jurisdictionId, ended)`, where a null `parentId` asserts the key is present and its value null.

  Story phases add test methods directly to this class.
- [ ] T025 Run `./gradlew check -PskipOwasp`. Every suite must be green, including:
  - `ReferenceDataFixtureRulesTest` (six fixtures, two id rules, the literal-character check);
  - `NoTypeSpecificCodeTest` (unchanged so far; it must still pass after T010's rewording);
  - the earlier types' functional tests;
  - the JaCoCo gate.

**Checkpoint**: The capability and the type are live. Existing tests reflect six supported types, the existing golden files are unchanged, and the build is green.

---

## Phase 3: User Story 1 - Integrator Retrieves All Locations (Priority: P1) 🎯 MVP

**Goal**: The collection route returns all 2,000 locations, exactly as in the source, deterministically.

**Independent Test**: `GET /api/v1/reference_data/locations` with a valid token returns `200` and 2,000 records in ascending `id` order, identical on repeated calls (quickstart § 3 rows 1–5, 9–12).

- [ ] T026 [US1] In src/functionalTest/java/…/jo/LocationsFunctionalTest.java, add `ac001ReturnsAll2000LocationsInAscendingIdOrder`: `200`, `application/json`, and `results` of 2,000 records. Ids are strictly ascending, the first is 1 and the last 2433. Each record has exactly `FIELDS`, in order, including `parent_id` when null (AC-001, FR-005).
- [ ] T027 [US1] In the same file, add `ac002RecordsMatchTheReferencePoints`:
  - using `byId`, each of the 12 Reference Points rows has its name, `type_id`, `parent_id`, `jurisdiction_id`, `end_date` and `updated_at` (via `assertRecord`);
  - every record has `created_at` 2024-01-15T09:00:00Z and `start_date` 2024-01-01.

  (AC-002.)
- [ ] T028 [US1] In the same file, add `ac003NamesAreServedExactly`. Fetch and parse once.
  - **Raw body**: contains `"name":"Cheshire  LJA"` (two spaces), `"name":"Cheshire LJA"` and `"name":"Royal Courts of Justice – Office of the Judge Advocate General"`.
  - **Parsed records**: 40 records are named "National", 8 "London" and 7 "Unknown", each with a distinct id; 1176 and 1323 keep their names.

  (AC-003, EC-002 to EC-004.)
- [ ] T029 [US1] In the same file, add `ac004Exactly285RecordsAreEnded`. Fetch and parse once.
  - **Raw-body counts**: exactly 285 `"end_date":"2025-03-31"` and 1,715 `"end_date":null` (counts, not absence checks).
  - **Parsed records**: the ended records are exactly those at positions 7, 14, …, 1995 in the returned order, the first id 9 and the last 2364, each with `updated_at` 2025-04-01T08:00:00Z.

  (AC-004, EC-007, EC-008.)
- [ ] T030 [US1] Capture the collection response from a running instance: run `./gradlew bootRun`, then `curl -s -o src/functionalTest/resources/golden/locations.json -H 'Authorization: Bearer local-dev-token' http://localhost:8080/api/v1/reference_data/locations`.
  - The file must be the compact body exactly as served, with no reformatting and no trailing newline (about 414 KB).
  - Check that its 2,000 records equal the fixture's, field for field.
  - Then add `ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile`, as in JudiciaryRolesFunctionalTest.

  (AC-005.)

**Checkpoint**: US1 complete. This is the MVP: integrators can load locations.

---

## Phase 4: User Story 2 - Integrator Retrieves a Single Location by ID (Priority: P1)

**Goal**: The single-record route returns one location and validates the id as for every type.

**Independent Test**: `GET /api/v1/reference_data/locations/2141` returns `200` with "Gwynedd" and `"parent_id": null` (quickstart § 3 rows 7–8, 13–16).

- [ ] T031 [US2] In src/functionalTest/java/…/jo/LocationsFunctionalTest.java, add `ac006ReturnsTheRecordEqualToTheCollectionElement`, parameterised over ids 1, 9, 1176, 2141, 2398 and 2433. Each returns `200` with a single object (no `results`) equal to the collection element with that id. For 2141, also assert on the raw body that it contains `"parent_id":null` (AC-006).
- [ ] T032 [US2] In the same file, add:
  - `ac007UnknownIdsReturn404` over `2`, `2434` and `999999` (`404`, record-not-found message);
  - `ac008MalformedIdsReturn400` over `abc`, `12x`, `1.5` and `-1` (`400`, malformed-id message).

  (AC-007, AC-008, EC-001.)
- [ ] T033 [US2] In the same file, add `ac009IdsAreScopedToTheirType`:
  - under `locations`, id 1 is "Aberconwy" and id 10 "Atherstone and Coleshill";
  - id 10 is "Aberconwy" under `base_locations`, "Acting Senior Coroner" under `appointment_titles`, "Fee-paid" under `contract_types`, "Female" under `genders` and "Acting Designated Civil Judge" under `judiciary_roles`;
  - `base_locations/1` returns `404`.

  (AC-009, EC-009.)

**Checkpoint**: US1 and US2 complete.

---

## Phase 5: User Story 3 - Integrator Walks the Location Hierarchy (Priority: P1)

**Goal**: Every parent a location or base location names is a served location; 003 DF-1 is closed for `parent_id`.

**Independent Test**: `ReferenceDataReferencesTest` passes, and so do the HTTP checks (quickstart § 3 rows 4–6, 22–23).

- [ ] T034 [P] [US3] Create src/test/java/…/jo/repository/ReferenceDataReferencesTest.java (research R7; FR-012). It reads the shipped fixtures from `src/main/resources/reference-data/` (the file system, not the classpath), with:
  - **References table**: the rows `locations.json`/`parent_id` → `locations.json` and `base_locations.json`/`parent_id` → `locations.json`. For each row, every non-null value is an `id` in the target fixture, and the failure message names the source fixture, record id and value.
  - **Self-reference check**: for the self-referencing row, no record is its own parent, and following parents from every record reaches a null parent within the number of records.
  - **Completeness check**: a fixed map from reference field to target type (`type_id` → `location_types`, `parent_id` → `locations`, `jurisdiction_id` → `jurisdictions`). For every type in `application.yml` that declares a reference field whose target type is also declared there, the table must hold a row. Today `location_types` and `jurisdictions` aren't served, so only the two rows are required. Serving either later makes this test fail until its row is added (DF-1, DF-5).
- [ ] T035 [US3] In src/functionalTest/java/…/jo/LocationsFunctionalTest.java, add:
  - `ac010EveryParentResolvesWithinLocations`: on the parsed collection, 1,858 non-null parents, each a served id; no self-parent; every chain reaches a null parent within two steps; on the raw body, exactly 142 `"parent_id":null`.
  - `ac011EveryBaseLocationParentIsALocation`: every `parent_id` in the base-locations collection is a served location id.
  - `ac012TypeAndJurisdictionCodesAreCopied`: every `type_id` is in 1–16 and every `jurisdiction_id` in {30, 34, 84, 86}, never null.

  (AC-010 to AC-012.)

**Checkpoint**: US1–US3 complete.

---

## Phase 6: User Story 5 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: The new routes open no unauthenticated path.

**Independent Test**: Each location route without a token or with a wrong token returns `401` in the shared shape (quickstart § 3 rows 19–20).

- [ ] T036 [US5] In src/functionalTest/java/…/jo/LocationsFunctionalTest.java, add:
  - `ac016MissingTokenIsRejected`, parameterised over the collection, `/1`, `/2` and `/abc`: `assertUnauthorized`;
  - `ac017InvalidTokenIsRejectedWithTheSameMessage`, parameterised over `Bearer wrong-token`, `Basic dGVzdA==` and `Bearer ` (empty), each on the collection, `/1` and `/abc`.

  (AC-016, AC-017.)

**Checkpoint**: All P1 stories complete.

---

## Phase 7: User Story 4 - The Singular Name Is Not Served (Priority: P2)

**Goal**: `location` is rejected like any unsupported name.

**Independent Test**: `location` and `location/{id}` return `400` with a token and `401` without (quickstart § 3 rows 17–18).

- [ ] T037 [US4] In src/functionalTest/java/…/jo/LocationsFunctionalTest.java, add:
  - `ac013TheSingularCollectionIsUnsupported`: `400`, unsupported-attribute message, no `results`;
  - `ac014TheSingularSingleRecordRouteIsUnsupported` over `1`, `2` and `abc`: all `400` with the unsupported-attribute message;
  - `ac015AuthenticationIsCheckedBeforeTheSingularName` over the two singular paths, with no token and with `Bearer wrong-token`: `401`.

  (AC-013 to AC-015.)

**Checkpoint**: US4 complete.

---

## Phase 8: User Story 6 - Existing Behaviour Is Unchanged and Other Types Stay Unsupported (Priority: P2)

**Goal**: The existing types and every unbuilt type behave exactly as before.

**Independent Test**: 002's to 006's acceptance tests pass unchanged, every existing golden file matches, and the 15 unsupported names return `400` (quickstart § 2 and § 3 rows 21–22).

- [ ] T038 [US6] Confirm with `git diff main -- src/functionalTest/resources/golden/` that the existing golden files (`appointment_titles.json`, `base_locations.json`, `contract_types.json`, `genders.json`, `judiciary_roles.json`) are unchanged and only `locations.json` is new (AC-018, SC-008).
- [ ] T039 [US6] In src/functionalTest/java/…/jo/LocationsFunctionalTest.java, add the following, as in JudiciaryRolesFunctionalTest:
  - `ac020QueryParametersReturn400`, parameterised over `?parent_id=1722`, `?page=2`, `?x=` and `/1?name=Aberconwy`: `400`, query-parameter message;
  - the inherited checks `ec011UnmatchedPathsReturn404` (trailing slash, `/1/`, `/1/extra`), `ec011OtherMethodsReturn405` (POST, PUT, PATCH and DELETE on the collection and `/1`), `ec011UnacceptableMediaTypesReturn406` (`application/xml` on the collection and `/1`) and `ec011LeadingZerosAndOversizedIds` (`/0001` → id 1; `/99999999999999999999` → `404`).

  AC-019 is covered by T021. (AC-020, EC-011.)
- [ ] T040 [US6] Run `./gradlew functionalTest` and confirm the earlier types' functional classes pass with their assertions unchanged, apart from T021's list changes (AC-018).

**Checkpoint**: US6 complete, with no regressions.

---

## Phase 9: User Story 7 - Maintainer Adds the Type With One Generic Change (Priority: P2)

**Goal**: The only main-code change is the generic capability; the guard enforces "no type named".

**Independent Test**: `NoTypeSpecificCodeTest` passes with the new fragment, and the main-code diff is limited to the files in quickstart § 2.

- [ ] T041 [P] [US7] In src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (research R8; AC-023, FR-018):
  - add `"location"` to the fragments of `noMainSourceFileNamesASpecificReferenceDataType`, with no exemption;
  - add a parameterised recognition test for `location`, `locations`, `LOCATION`, `Location`, `LocationHelper`, `myLocation`, `XLOCATION`, `location_id`, `base_location` and `baseLocation`;
  - add a non-matching test for `allocation`, `relocation`, `dislocation` and `collocation`, with a comment that each has the fragment straight after a lower-case letter and so exercises the boundary rule.
- [ ] T042 [US7] Run `git diff --stat main -- src/main/java` and `git diff --stat main -- src/main/resources`.
  - **`src/main/java`**: expect only the nine files listed in quickstart § 2 (eight changed and one new).
  - **`src/main/resources`**: expect only `application.yml` and `locations.json`.
  - **Check**: confirm `git grep -n -i 'location' -- src/main/java` prints nothing.
  - **Record**: write the result in specs/007-locations-reference-data/plan.md § Implementation notes (AC-021, SC-009).
- [ ] T043 [US7] Confirm the coverage mapping and note it in specs/007-locations-reference-data/plan.md § Implementation notes: AC-022 (nullable versus non-nullable start-up behaviour) is covered by T002 and T004, and AC-024 (OpenAPI: `locations` listed, `location` not, `parent_id` nullable) by T014 and T020.

**Checkpoint**: US7 complete.

---

## Phase 10: User Story 8 - Example Requests Exercise the New Type (Priority: P3)

**Goal**: Testers can exercise locations from Postman without reading code.

**Independent Test**: newman runs the collection against a running instance with every check passing (quickstart § 5).

- [ ] T044 [US8] In postman/ctam-jomockapi.postman_collection.json, insert text so the existing formatting is untouched (as in 004–006): add a "Locations" sub-folder under "Reference Data", after "Judiciary Roles", with ten requests and test scripts:
  - **the collection**: `200`; 2,000 results; every record has the nine fields in order; ids ascending; first `1` "Aberconwy"; last `2433` "Kent and Medway"; exactly 285 non-null `end_date` values; exactly 142 records whose `parent_id` is `null` with the key present;
  - **`/1`**: `200`, `parent_id` 1722;
  - **`/2141`**: `200`, "Gwynedd", the `parent_id` key present and `null`;
  - **`/2`**: `404`;
  - **`/abc`**: `400`;
  - **`?parent_id=1722`**: `400`;
  - **trailing slash**: `404`, "Resource not found.";
  - **missing token**: `401`, `WWW-Authenticate: Bearer`;
  - **invalid token**: `{{invalidBearerToken}}`, `401`;
  - **`location`**: `400`, "singular name not served", no `results`.

  (AC-025, FR-014; research R10.)
- [ ] T045 [US8] Start the app (`./gradlew bootRun`) and run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`. Record the request and assertion counts (0 failures) in specs/007-locations-reference-data/plan.md § Implementation notes (AC-025).

**Checkpoint**: All user stories complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

- [ ] T046 [P] In src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java, add `LOCATIONS` to the collection paths of the per-collection p95 guard (FR-016, SC-010; research R10).
- [ ] T047 [P] Update README.md (FR-015; research R10):
  - **§ Reference data**: "Six types are configured", with a `locations` row (2,000 records; no alias served: `location` returns `400`), and a short paragraph explaining that location records keep the E-Links ids and carry `type_id`, `parent_id` and `jurisdiction_id`, with `parent_id` null for the 142 top-level locations and every parent resolving to a served location.
  - **§ Adding a reference-data type**: add `nullable-reference-fields: [parent_id]   # optional: declared reference fields that may be null` to the YAML example, and a sentence that such a field must be present in every record as a positive integer or `null`, and is written as `null` in responses.
- [ ] T048 [P] Update CLAUDE.md:
  - **§ Current repo state**: add a `007-locations-reference-data` line. It covers: locations through configuration, a fixture and one generic capability (`nullable-reference-fields`, `ReferenceFieldValue`, the mapper taking the type); 2,000 records with E-Links ids and 285 ended; `parent_id` null for 142 top-level locations; references checked by `ReferenceDataReferencesTest`, closing 003 DF-1 for `parent_id`; DF-5 and DF-6; the singular name not served; `NoTypeSpecificCodeTest` widened to `location`.
  - **§ Reference data**: add location names, ids and reference codes to the public values taken from the extracts, and `specs/007-locations-reference-data/data-model.md` § 4 to the list.
- [ ] T049 [P] Add a one-line pointer to specs/007-locations-reference-data/contracts/reference-data-api-locations.md in specs/002-reference-data-api/contracts/reference-data-api.md, next to the 003–006 pointers. In specs/003-base-locations-reference-data/spec.md, add one sentence to the DF-1 row: "`parent_id` closed by 007 (`ReferenceDataReferencesTest`); `type_id` and `jurisdiction_id` remain open." (Research R10.)
- [ ] T050 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`. Record in specs/007-locations-reference-data/plan.md § Implementation notes the per-suite test counts, the JaCoCo line and branch coverage (the gate must hold, with the new branches covered), and each collection's strict p95 (FR-016, FR-017, SC-010).
- [ ] T051 Walk through specs/007-locations-reference-data/quickstart.md §§ 1–5 against `./gradlew bootRun`. Mark the spec's Status as Implemented in specs/007-locations-reference-data/spec.md, and record in specs/007-locations-reference-data/plan.md § Implementation notes that every expected result matched (or the differences).

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story. Within it:
  - T002–T007 (tests) come first and can run in parallel.
  - T008 can run alongside them.
  - Then T009 → T010 → T011 (configuration, type, registry), then T012 → T013 (record, repository), then T014 → T015 (response, mapper and service; T014 includes the OpenAPI schema assertions), then T016.
  - Then T017 → T018 (the fixture before the type that loads it).
  - T019–T021 come after T018, in parallel; then T022, T023, T024; T025 is last.
- **User stories (Phases 3–10)**: all after Phase 2. Most add methods to the same functional test class, so run them in order US1 → US2 → US3 → US5 → US4 → US6 → US7 → US8, or have one person own that file. T034 (references test), T041 (guard) and T044 (Postman) touch other files and can run alongside.
- **Polish (Phase 11)**: after the stories. T046–T049 in parallel, then T050, then T051.

### User story dependencies

- **US1 (P1)**: needs only Phase 2. MVP.
- **Other stories**: US2, US3 and US5 (P1), US4, US6 and US7 (P2) and US8 (P3) each need only Phase 2 to work. US8's newman run (T045) is most useful once US1–US7 are green.

### Parallel opportunities

- Phase 2 tests: T002, T003, T004, T005, T006 and T007 (different files), plus T008.
- Phase 2 test updates: T019, T020 and T021.
- Stories: T034 (references test), T041 (guard) and T044 (Postman) alongside the functional-test tasks.
- Polish: T046–T049.

### Parallel example: Phase 2 tests first

```text
T002 ReferenceDataTypeRegistryTest       — nullable declaration cases
T003 ReferenceDataTypeTest                — nullable set copy
T004 FixtureReferenceDataRepositoryTest   — null / missing / undeclared cases + fixtures
T005 ReferenceFieldValueTest              — absent / null / number serialisation
T006 ReferenceDataMapperTest              — three-state mapping with the type
T007 Service and controller tests         — new mapper signature
```

### Parallel example: alongside the functional tests

```text
T034 ReferenceDataReferencesTest          — parent references resolve; completeness
T041 NoTypeSpecificCodeTest               — location fragment and spellings
T044 Postman collection                   — Locations folder
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1, then Phase 2: the capability built test-first, the type live, the build green, existing golden files unchanged.
2. Phase 3 (US1): the collection works, its data is exact, and it is tested. **Stop and validate** against quickstart § 3 rows 1–5 and 9–12.

### Incremental delivery

3. US2, US3 and US5 (the rest of P1), then US4, US6 and US7 (P2), then US8 (P3), validating each checkpoint.
4. Polish: the smoke guard, documentation, the final gates and the quickstart walk-through.

### Notes

- No constitution change is needed for this feature.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- The extract is read only by T017's throwaway script; nothing in the build reads it (FR-007).
- T016 is the key regression gate for the generic change. If any existing golden file changes, stop: R4's serialisation is wrong.
