---

description: "Task list for 013-people-api"
---

# Tasks: People API — Search and Single Office Holder

**Input**: Design documents from `/specs/013-people-api/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/people-api.md](./contracts/people-api.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The spec (SC-001, FR-045, FR-053) and the constitution (Principle XI) require unit, integration (including OpenAPI contract), functional and smoke coverage. Shared components are built test-first: the unit tests are written and seen to fail before the main code.

**Organization**: Tasks are grouped by user story (spec.md US1–US9) so each can be verified independently. Story phases run in priority order: P1 (US1, US2, US3, US5, US7), then P2 (US4, US6, US8), then P3 (US9).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US9)
- Every task names exact file paths

## Path Conventions

Single Gradle project at the repository root. Base package `uk.gov.hmcts.ctam.jo`, abbreviated below as `…/jo/`.
- Main: `src/main/java/…/jo/` and `src/main/resources/`
- Suites: `src/test/java/…/jo/` (unit), `src/integrationTest/java/…/jo/`, `src/functionalTest/java/…/jo/`, `src/smokeTest/java/…/jo/`
- Unit-test People fixtures: `src/test/resources/people/` (valid) and `src/test/resources/people/invalid/`
- Shared test helpers: `src/testFixtures/java/…/jo/testsupport/` (`ContractMessages`, `ContractPaths`, `TestTokens`, `ErrorResponseAssertions`, `JsonTrees`)

Rule references: G-n and V-n are the generation and start-up rules in [data-model.md § 4–5](./data-model.md); Rn are decisions in [research.md](./research.md). Error messages are the constants in `ContractMessages` (the contract's error table). Postman uses only `{{baseUrl}}`, `{{bearerToken}}`, `{{invalidBearerToken}}` and the new `{{personId}}` and `{{peopleMidDate}}`.

**Guard rule for every task that creates a file marked † in plan.md § Source Code**: add its path to `PEOPLE_SOURCES` in src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java in the same task, and keep the file to the contract names of spec FR-052, including in comments.

---

## Phase 1: Setup

**Purpose**: Start from a known-green baseline.

- [X] T001 Run `./gradlew check -PskipOwasp` on the branch before any change and record the per-suite test counts and overall JaCoCo line and branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under a new `## Implementation notes` heading at the end of specs/013-people-api/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared components (R5, R6, R8), the guard exception (R1), the configuration (R4), the fixture (R3), loading and validation (R6), and the response models and mapping (R2, R7, R9). Every story needs these.

**⚠️ CRITICAL**: No user-story phase can start until this phase is complete and `./gradlew check -PskipOwasp` is green (T025).

### Shared JSON, pagination and parameter components (tests first)

- [X] T002 Extract the strict-coercion `JsonMapper` that `FixtureReferenceDataRepository`'s constructor builds inline into a new final class src/main/java/…/jo/repository/StrictFixtureJson.java with `public static JsonMapper mapper()`, building exactly the same configuration (every `withCoercionConfig` call, in the same order). Make src/main/java/…/jo/repository/FixtureReferenceDataRepository.java call it. Run `./gradlew test`: every existing `FixtureReferenceDataRepositoryTest` case passes unchanged (R6)
- [X] T003 [P] Write src/test/java/…/jo/services/PaginatorTest.java (R8):
  - a `@MethodSource` table over N ∈ {0, 1, 49, 50, 51, 120} × P ∈ {1, 7, 50, 120, `Long.MAX_VALUE`}, asserting for every page from 1 to `pages + 1`:
    - `results` = N and `pages` = ⌈N ÷ P⌉ (0 when N = 0);
    - `current_page` = the requested page and `results_per_page` = P;
    - `more_pages` ⇔ page < pages;
    - the slice is the expected sub-list, and the union of pages 1…`pages` is the input in order (AC-007, AC-008, AC-009, SC-002);
  - `page = Long.MAX_VALUE` with N = 120 gives an empty slice without throwing;
  - `PageRequest` rejects a `page` or `perPage` below 1 with `IllegalArgumentException`;
  - `PageRequest.DEFAULT_PAGE` is 1 and `DEFAULT_PER_PAGE` is 50.
- [X] T004 [P] Write src/test/java/…/jo/services/QueryParameterParserTest.java (R5):
  - `requiredDate("updated_since", raw)`:
    - `2025-06-01` and `2024-02-29` parse;
    - `null` and `""` throw `InvalidQueryParameterException` with "updated_since is required.";
    - `2025-02-30`, `2023-02-29`, `2025-6-1`, `20250601`, `2025-06-01T00:00:00Z`, `2025-06-01 `, `+2025-06-01`, `yesterday` and `２０２５-06-01` (full-width digits) throw with "updated_since must be a valid date.";
  - `positiveWholeNumber("page", raw, 1)`:
    - `null` gives 1; `1`, `007` (7) and `9223372036854775807` parse;
    - `""`, `0`, `00`, `-1`, `+1`, `1.0`, `1e3`, ` 1`, `abc`, `９` and `9223372036854775808` throw with "page must be a positive whole number.";
  - the same table for `per_page` with default 50;
  - `flag("include_previous_appointments", raw)`: `null` gives false; `true` and `false` parse; `TRUE`, `True`, `1`, `yes` and `""` throw with "include_previous_appointments must be true or false.";
  - no message contains the raw value.
- [X] T005 [P] Write src/test/java/…/jo/services/PersonIdParserTest.java (R5, FR-005, EC-005): a lower-case and an upper-case canonical UUID both return the lower-case form; `abc`, `19759`, `9100120`, a UUID without hyphens, `1-1-1-1-1`, a UUID with braces, one with a trailing space, and the empty string each throw `InvalidPersonIdException` with "id must be a UUID."
- [X] T006 [P] Write src/test/java/…/jo/filters/DeclaredQueryParametersInterceptorTest.java (R5). Build `HandlerMethod`s from a small nested test controller with one method declaring `@RequestParam` names `a` and `b`, and one declaring none. Assert:
  - no query passes;
  - `a=1&b=2` passes;
  - `c=1` throws `UndefinedQueryParameterException`;
  - `a=1&a=2` and `a=1&b=2&a=3` throw `RepeatedQueryParameterException`;
  - `c=1&c=2` throws `RepeatedQueryParameterException` (the repeat check runs first);
  - any parameter on the method with none throws `UndefinedQueryParameterException`;
  - a non-`HandlerMethod` handler passes untouched;
  - a parameter with an empty value (`a=`) counts as present.
- [X] T007 Create the exceptions in src/main/java/…/jo/exceptions/, each extending `ApiRequestException` with a public `MESSAGE` constant (or fixed messages) and a diagnostic constructor:
  - `InvalidQueryParameterException`: static factories `required(String name)`, `invalidDate(String name)`, `notPositiveWholeNumber(String name)` and `notBoolean(String name)`, giving "<name> is required.", "<name> must be a valid date.", "<name> must be a positive whole number." and "<name> must be true or false.";
  - `UndefinedQueryParameterException`: "Unsupported query parameter.";
  - `RepeatedQueryParameterException`: "Query parameters must not be repeated.";
  - `InvalidPersonIdException`: "id must be a UUID.";
  - `PersonNotFoundException`: "Person not found.".
  
  Add the first four to the `400` handler and `PersonNotFoundException` to a new `404` handler in src/main/java/…/jo/exceptions/GlobalExceptionHandler.java. Extend src/test/java/…/jo/exceptions/GlobalExceptionHandlerTest.java with one case per exception (status, message, warning log, no caller input in the body). Add the eight new messages to src/testFixtures/java/…/jo/testsupport/ContractMessages.java
- [X] T008 Implement src/main/java/…/jo/services/PageRequest.java (record `long page, long perPage`, validating ≥ 1, constants `DEFAULT_PAGE = 1` and `DEFAULT_PER_PAGE = 50`), src/main/java/…/jo/services/Page.java (record `List<T> results, PaginationResponse pagination`), src/main/java/…/jo/domain/PaginationResponse.java (record `long results, long pages, long currentPage, long resultsPerPage, boolean morePages`):
  - `@JsonProperty` names and `@JsonPropertyOrder` as in contracts/people-api.md;
  - `@Schema` `requiredMode = REQUIRED`, `format = "int64"` and descriptions;
  - Javadoc stating it's shared with the leavers and deleted features (DF-11).
  
  Then implement src/main/java/…/jo/services/Paginator.java (`@Component`, `<T> Page<T> paginate(List<T> ordered, PageRequest request)`) with the overflow-free arithmetic of R8. T003 passes
- [X] T009 Implement src/main/java/…/jo/services/QueryParameterParser.java (`@Component`; `LocalDate requiredDate(String name, String raw)`, `long positiveWholeNumber(String name, String raw, long defaultValue)`, `boolean flag(String name, String raw)`) using the ASCII regexes of R5, `DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT)` and `Long.parseLong` with overflow mapped to the same 400. Implement src/main/java/…/jo/services/PersonIdParser.java with a public `PATTERN` constant (used again by the controller's OpenAPI annotation). T004 and T005 pass
- [X] T010 Implement src/main/java/…/jo/filters/DeclaredQueryParametersInterceptor.java (`@Component HandlerInterceptor`). For a `HandlerMethod`, it collects the `@RequestParam` names of its parameters and reads `request.getParameterMap()` restricted to names present in `request.getQueryString()`, so form bodies are never considered. It throws `RepeatedQueryParameterException` for any name with more than one value, then `UndefinedQueryParameterException` for any undeclared name; the diagnostic is the sanitised query string. Add `PEOPLE = "/api/v1/people"` to src/main/java/…/jo/config/ApiPaths.java with Javadoc. Register the interceptor in src/main/java/…/jo/config/WebConfig.java for `ApiPaths.PEOPLE` and `ApiPaths.PEOPLE + "/**"`, leaving the reference-data registration untouched. T006 passes

### Guard exception (R1; spec FR-052, AC-032)

- [X] T011 Extend src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java (R1):
  - **Constants**: add `PEOPLE_SOURCES` (a `List<Path>` of file names under src/main/java, initially empty and filled by later tasks) and `PEOPLE_CONTRACT_NAMES` (`appointment`, `appointments`, `appointment_id`, `include_previous_appointments`, `gender`, `gender_id`, `location`, `base_location`, `base_location_id`, `contract_type`, `contract_type_id`, `judiciary_roles`, `judiciary_role_id`, `judiciary_role_name_id`, `ticket`, `ticket_id`, `tickets`, `jurisdiction`, `jurisdiction_id`), with `judiciary_role` after `judiciary_roles` (spec FR-052). Add `NON_CONTRACT_TYPE_NAMES` (`appointment_titles`, `base_locations`, `contract_types`, `genders`, `locations`, `location_types`, `jurisdictions`, `ticket_categories`, `ticket_category_types`).
  - **Fragment scan**: `noMainSourceFileNamesASpecificReferenceDataType` scans each listed file after `maskContractNames(text)`, and every other file unmasked, as today.
  - **Masking**: `maskContractNames` splits identifiers and words on snake, camel and Pascal boundaries, spaces and hyphens, and blanks every run of words equal to a contract name, wherever it appears in an identifier (research R1).
  - **New tests**:
    - `peopleSourcesNameNoNonContractType`: each listed file, unmasked, matches no `NON_CONTRACT_TYPE_NAMES` spelling;
    - `everyListedPeopleSourceExists`;
    - `theMaskRemovesContractNames`: `getAppointments`, `AppointmentsResponse`, `AppointmentRecord`, `baseLocationId`, `BASE_LOCATION_ID`, `includePreviousAppointments`, `judiciaryRoleNameId`, `getTickets`, `ResolvedAppointment`, `AppointmentStatus`, `currentAppointments`, `baseLocationTypeId`, `baseLocationParentId`, `JudiciaryRoleRecord`, `JudiciaryRoleResponse`, "an appointment's end date" and `withGenderId` mask to no fragment match;
    - `theMaskKeepsOtherTypeNames`: `AppointmentTitle`, `appointmentTitles`, `myLocation`, `GenderHelper`, `ticketCategoryType`, `baseLocations` and `locationTypes` still fail, each rejected by the non-contract check before masking;
    - `aFileOutsideTheListGetsNoExemption`: the masking isn't applied to an unlisted sample.
  - **Comments**: replace the "the People feature adds its own reviewed exception" comments with a pointer to 013 FR-052.
  
  Run `./gradlew test --tests '*NoTypeSpecificCodeTest'`: it passes with the empty list

### Configuration, entities and fixture (R3, R4; data-model.md § 2, § 4)

- [X] T012 Create src/main/java/…/jo/config/PeopleProperties.java † (`@ConfigurationProperties("jo.people")`, `@Validated`, Lombok):
  - `@NotBlank String fixture` (a Spring resource location) and `@NotNull LocalDate referenceDate`;
  - a nested `@Valid @NotNull ReferenceTypes referenceTypes` with ten `@NotBlank String` fields: `genderId`, `roleNameId`, `contractTypeId`, `baseLocationId`, `baseLocationTypeId`, `baseLocationParentId`, `jurisdictionId`, `judiciaryRoleNameId`, `ticketId`, `ticketCategoryId`.
  
  Enable it where `ReferenceDataProperties` is enabled. Add the `jo.people` block of R4 to src/main/resources/application.yml (comments citing D-8 and FR-052). The suites' `application-test.yml` files don't override reference data, so they need no change. Add a unit test src/test/java/…/jo/config/PeoplePropertiesTest.java: binding the defaults works; a missing `reference-date` or a blank reference type fails validation
- [X] T013 [P] Create the fixture entities † in src/main/java/…/jo/entity/ (Lombok `@Value @Builder @Jacksonized`, `@JsonIgnoreProperties(ignoreUnknown = false)`, boxed types so a missing key is detectable, `LocalDate` dates), keys exactly as data-model.md § 2:
  - `OfficeHolderRecord.java`
  - `AppointmentRecord.java`
  - `JudiciaryRoleRecord.java`
  - `AuthorisationRecord.java`
  
  Create `OfficeHolder.java` † as the validated, resolved form: the record plus the derived names of data-model.md § 3 per appointment, judiciary role and authorisation (nested `@Value` classes `ResolvedAppointment`, `ResolvedJudiciaryRole` and `ResolvedAuthorisation`), with lists in FR-025 order.
- [X] T014 Generate src/main/resources/people/office_holders.json once with a throwaway script kept outside the repository (scratchpad). It follows rules G-1 to G-20 of data-model.md § 4 and reads ids and `end_date`s only from the checked-in src/main/resources/reference-data/*.json. Use a fixed seed for the UUIDs. Write 2-space-indented UTF-8 JSON with no `\u` escapes, records in ascending (`last_updated`, `id`) order. Record in specs/013-people-api/plan.md § Implementation notes:
  - the seed;
  - the counts behind G-12, G-16, G-17 and G-19 (people with both kinds of appointment, only previous, several, ended and open roles, authorisations with and without an end, distinct months, shared `last_updated` values, and the current appointments ending in 2026–2029 under G-11);
  - the person holding tickets 294, 365 and 367 (G-18).
- [X] T015 Write src/test/java/…/jo/repository/PeopleFixtureRulesTest.java, reading src/main/resources/people/office_holders.json and the reference-data fixtures as JSON trees. Add one test per rule G-1 to G-20 (data-model.md § 4), named after the rule, with failure messages naming the office holder's `id`. G-5's name lists are not reproduced; the test checks the format only. G-14's shared-name case is checked by finding two appointments whose `base_location_id`s differ but whose base-location records have the same `name`. Also check that the file contains no `\u` escape, as `ReferenceDataFixtureRulesTest` does

### Loading and start-up validation (R6; data-model.md § 5; FR-046)

- [X] T016 Create src/test/resources/people/minimal.json (two office holders exercising every optional field present and absent, both flag outcomes, and a shared `last_updated`, with the first person's appointments, judiciary roles and dated authorisations written out of their FR-025 order, referring only to records in the shipped reference-data fixtures) and src/test/resources/people/empty.json (`[]`). Create one invalid fixture per V-rule failure in src/test/resources/people/invalid/, each a copy of `minimal.json` with one change:
  - `not-an-array.json`
  - `missing-key.json` (no `work_phone`)
  - `null-required.json` (`surname: null`)
  - `unknown-key.json`
  - `string-per-id.json` (`"900001"`)
  - `upper-case-id.json`
  - `bad-uuid.json`
  - `per-id-zero.json`
  - `personal-code-letters.json`
  - `duplicate-id.json`
  - `duplicate-per-id.json`
  - `duplicate-personal-code.json`
  - `duplicate-appointment-id.json` (across two people)
  - `duplicate-authorisation-id.json`
  - `duplicate-judiciary-role-id.json`
  - `real-email-domain.json` (`@example.com`)
  - `phone-outside-range.json`
  - `no-appointments.json`
  - `two-current-principals.json`
  - `no-current-principal.json`
  - `start-after-end.json`
  - `last-updated-too-early.json`
  - `unknown-gender.json`
  - `unknown-role-name.json`
  - `unknown-contract-type.json`
  - `unknown-base-location.json`
  - `unknown-judiciary-role.json`
  - `unknown-ticket.json`
  - `foreign-appointment.json` (an authorisation naming another person's appointment)
  - `authorisation-before-appointment.json`
  - `authorisation-open-after-appointment-ends.json`
- [X] T017 Write src/test/java/…/jo/repository/FixturePeopleRepositoryTest.java (test sources are not scanned by the guard). Construct the repository with the real reference-data repository and registry from the shipped configuration (via `ReferenceDataFixtures` or a small builder), and a `PeopleProperties` pointing at each fixture. Cover:
  - `minimal.json` loads in (`last_updated`, `id`) order with every derived name equal to the reference record's name (data-model.md § 3);
  - `minimal.json`'s out-of-order lists load in FR-025 order: appointments by (`start_date`, `appointment_id`), judiciary roles by (`start_date`, `judiciary_role_id`), dated authorisations by (`start_date`, `authorisation_id`);
  - `empty.json` loads with no reference types required (EC-013);
  - a `@CsvSource` over every invalid fixture of T016, asserting `IllegalStateException` with message `Invalid People fixture classpath:people/invalid/<file>: <rule message> (office holder <id>)`, or `(record at position <n>)` where the id is missing or invalid;
  - a missing fixture resource;
  - a reference type naming an unconfigured type;
  - a base-location type not declaring `parent_id` (V-8), using a test registry.
  
  Fix the exact rule messages in the test, and reuse them in T019.
- [X] T018 Create src/main/java/…/jo/repository/PeopleRepository.java † (`List<OfficeHolder> findAll()` in FR-012 order; `Optional<OfficeHolder> findById(String lowerCaseId)`)
- [X] T019 Implement src/main/java/…/jo/repository/PeopleDatasetValidator.java † (V-2 to V-10 over parsed records and their JSON trees, first failure wins, messages as fixed in T017) and src/main/java/…/jo/repository/FixturePeopleRepository.java † (`@Repository`; the constructor loads `PeopleProperties.getFixture()` with `StrictFixtureJson.mapper()` (V-1), calls the validator, resolves names through `ReferenceDataRepository` and `ReferenceDataTypeRegistry` using `PeopleProperties.getReferenceTypes()`, sorts every list (FR-012, FR-025), and keeps an immutable list and an id map). T017 passes
- [X] T020 Set `jo.people.fixture=classpath:people/empty.json` in src/integrationTest/java/…/jo/ReferenceDataExtensionIntegrationTest.java's `@TestPropertySource` (it replaces the reference types; EC-013), with a comment citing 013 EC-013. Create src/integrationTest/resources/people/empty.json (`[]`)

### Response models and mapping (R2, R7, R9; spec § Records)

- [X] T021 [P] Create the DTO records † in src/main/java/…/jo/domain/:
  - `CurrentOfficeHolderResponse.java`
  - `AppointmentsResponse.java`
  - `JudiciaryRoleResponse.java`
  - `AuthorisationSummaryResponse.java`
  - `AuthorisationsResponse.java`
  
  Create `PeopleApiResponse.java` (not †) alongside them. Each record:
  - has exactly the spec's fields, with `@JsonProperty` snake names and `@JsonPropertyOrder` in the Swagger order (contracts/people-api.md);
  - leaves out `gender`, `email_personal`, `circuit`, `bench` and `advisory_committee_area` when unset (`@JsonInclude(NON_NULL)`);
  - always writes nullable required fields;
  - uses `@Schema` descriptions, `requiredMode`, `nullable = true` where the spec says so, `format = "uuid"` on `id`, `format = "date"` on dates and `format = "int64"` on ids.
  
  Add src/test/java/…/jo/domain/PeopleSerialisationTest.java: field order, a present `null` for every nullable required field, optional fields left out, dates as `YYYY-MM-DD`
- [X] T022 [P] Write src/test/java/…/jo/services/AppointmentStatusTest.java (with reference date 2026-01-01: `end_date` `null`, 2026-01-01 and 2026-06-30 are current; 2025-12-31 is previous) and src/test/java/…/jo/services/AuthorisationSummariesTest.java:
  - grouped by jurisdiction in ascending name;
  - tickets distinct and in ascending name;
  - empty input gives an empty list;
  - two jurisdictions with a shared ticket name keep it in both.
  
  Then implement src/main/java/…/jo/services/AppointmentStatus.java † (`@Component`, reference date from `PeopleProperties`, `isCurrent(LocalDate endDate)`) and src/main/java/…/jo/services/AuthorisationSummaries.java † (`List<AuthorisationSummaryResponse> summarise(List<AuthorisationsResponse>)`)
- [X] T023 Write src/test/java/…/jo/mappers/PeopleMapperTest.java using `OfficeHolder`s built from `minimal.json`. Cover:
  - flag `false`: only current appointments, only authorisations under them, `authorisations` summarising those, and `authorisations_current` summarising the current ones;
  - flag `true`: everything, with `authorisations_current` and `judiciary_roles` identical to the flag-`false` result;
  - `gender` present only when `show_gender` is true;
  - derived names copied through.
  
  Then implement src/main/java/…/jo/mappers/PeopleMapper.java † (MapStruct, `unmappedTargetPolicy = ERROR`), with `@Context` carrying the flag and `AppointmentStatus`, and `AuthorisationSummaries` for the two summaries (R7). T023 passes
- [X] T024 Run `./gradlew test`: T002–T023 pass, `NoTypeSpecificCodeTest` passes with the † files listed so far, and Checkstyle is clean

### Checkpoint

- [X] T025 Run `./gradlew check -PskipOwasp`: green, with every existing test, golden file and reference-data OpenAPI assertion unchanged (FR-050). Record the per-suite counts in specs/013-people-api/plan.md § Implementation notes

---

## Phase 3: User Story 1 - Integrator Finds People Updated Since a Date (Priority: P1) 🎯 MVP

**Goal**: `GET /api/v1/people?updated_since=…` returns the matching office holders in (`last_updated`, `id`) order with a pagination block (FR-001, FR-010 to FR-012, FR-017).

**Independent Test**: AC-001 to AC-005. A search before every record returns 120 people in pages of 50; a mid-range date returns exactly the people updated on or after it; a future date returns none; 100 consecutive calls are identical.

### Tests for User Story 1

- [X] T026 [P] [US1] Write src/test/java/…/jo/services/PeopleServiceTest.java search cases, using a stubbed `PeopleRepository` over `minimal.json`-style office holders and the real `Paginator`, `QueryParameterParser`, `PeopleMapper` and `AppointmentStatus`. Cover:
  - inclusive filter (EC-001);
  - order with ties (EC-007);
  - no matches, giving `pages` 0 (AC-003, EC-002);
  - `pagination.results` equals the match count (AC-002);
  - parameter checks in the order `updated_since`, `page`, `per_page`, `include_previous_appointments`: with two bad values the first one's message wins (EC-010).
- [X] T027 [P] [US1] Write src/test/java/…/jo/controllers/PeopleControllerTest.java † search cases (`@WebMvcTest` with `PeopleService` mocked and the interceptor, filters and handler imported as `ReferenceDataControllerTest` does):
  - `200` with `application/json` and exactly `results` and `pagination`;
  - the service receives the raw strings;
  - `POST` gives `405` with `Allow: GET`;
  - `Accept: text/plain` gives `406`;
  - `/api/v1/people/` gives `404` "Resource not found." (AC-028, FR-007).
- [X] T028 [P] [US1] Create src/functionalTest/java/…/jo/testsupport/PeopleHttp.java (GET helpers with and without a token, `OFFICE_HOLDER_FIELDS`, `APPOINTMENT_FIELDS` and the other field sets from the spec's Records section, `searchAll(updatedSince, perPage, flag)` and `idsInOrder(JsonNode)`), and add `PEOPLE` to src/testFixtures/java/…/jo/testsupport/ContractPaths.java

### Implementation for User Story 1

- [X] T029 [US1] Implement `search(String updatedSince, String page, String perPage, String includePrevious)` in src/main/java/…/jo/services/PeopleService.java † (`@Service`): parse in R5 order, filter `findAll()` by `last_updated >= updated_since`, paginate, then map the page's office holders. T026 passes
- [X] T030 [US1] Implement the search operation in src/main/java/…/jo/controllers/PeopleController.java †:
  - `@RestController`, `@RequestMapping(ApiPaths.PEOPLE)`, `@Tag(name = "People")`, `@SecurityRequirement(BEARER_AUTH)`;
  - `@GetMapping(produces = APPLICATION_JSON_VALUE)`, operation id `searchPeople`;
  - four `@RequestParam(name = …, required = false) String` parameters, with `@Parameter` overriding the schema, since every parameter is bound as `String`:
    - `updated_since`: `required = true, schema = @Schema(type = "string", format = "date")`;
    - `page` and `per_page`: `schema = @Schema(type = "integer", format = "int64", minimum = "1", defaultValue = "1")` and `defaultValue = "50"`;
    - the flag: `schema = @Schema(type = "boolean", defaultValue = "false")`;
  - `200` with `PeopleApiResponse`, and `400` with an `ErrorResponse` example ("updated_since is required.").
  
  T027 passes
- [X] T031 [US1] Create src/functionalTest/java/…/jo/PeopleFunctionalTest.java with `ac001` to `ac005` (spec US1) against the default dataset:
  - `ac001`: before every record gives 120 total and 50 on page 1, with exactly two top-level fields;
  - `ac002`: for a mid-range date taken from the dataset, the count and membership equal a client-side filter of `searchAll`;
  - `ac003`: 2099-01-01 gives none, with `pages` 0;
  - `ac004`: 100 consecutive calls to the whole-dataset search with the flag `true` return byte-identical bodies (SC-003), and the order is (`last_updated`, `id`);
  - `ac005`: each office holder has exactly the Records fields, with the JSON types of the spec;
  - `ec001`: a person's own `last_updated` includes them;
  - `ec007`: tied people are ordered by `id`;
  - `fr025`: in the whole-dataset response with the flag `true`, every office holder's `appointments`, `judiciary_roles` and `authorisations_with_dates` are in FR-025 order, and every summary is sorted by `jurisdiction` with its `tickets` sorted.
- [X] T032 [US1] Capture src/functionalTest/resources/golden/people_default_page1.json from `GET /api/v1/people?updated_since=2000-01-01` on a running instance, formatted exactly as the server writes it, as the existing golden files are. Add `defaultSearchMatchesItsGoldenFile` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java, comparing as the reference-data functional tests do
- [X] T033 [US1] Add a "People" folder to postman/ctam-jomockapi.postman_collection.json with "Search — defaults" (`{{baseUrl}}/api/v1/people?updated_since=2000-01-01`, `Authorization: Bearer {{bearerToken}}`), "Search — filtered" (`updated_since={{peopleMidDate}}`) and "Search — no matches" (`updated_since=2099-01-01`). Each has tests for status, content type, the two top-level fields, the pagination fields and types, ascending (`last_updated`, `id`) order and, for "filtered", that every `last_updated >= peopleMidDate`. Add `peopleMidDate` (the mid-range date used in T031) to postman/ctam-jomockapi.postman_environment.json

**Checkpoint**: US1 works: quickstart.md § 3 rows 1, 4 and 5.

---

## Phase 4: User Story 2 - Integrator Pages Through Results (Priority: P1)

**Goal**: `page` and `per_page` walk the matches exactly once each (FR-013, FR-017 to FR-019).

**Independent Test**: AC-006 to AC-009 and SC-002. Walking every page at sizes 1, 7, 50 and 120 gives each person once, in order.

- [X] T034 [US2] Add to src/test/java/…/jo/services/PeopleServiceTest.java: defaults give page 1 with 50 per page (AC-006); `page=007` means page 7 (EC-004); a huge `per_page` puts every match on page 1 (AC-009).
- [X] T035 [US2] Add `ac006` to `ac009` and `sc002` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java:
  - `ac006`: defaults;
  - `ac007`: for P ∈ {1, 7, 50, 120}, every page from 1 to `pages`; sizes P except the last; `more_pages`; the union equals `searchAll` in order;
  - `ac008`: page 4 of the default search is empty with `current_page` 4 and the other fields unchanged;
  - `ac009`: `per_page=500`;
  - `ec003`: `page=9223372036854775807` gives `200` and an empty page; `per_page=9223372036854775807` gives one page;
  - `ec004`: `page=002` equals `page=2`.
- [X] T036 [US2] Add "Search — page 2", "Search — last page" (`page=3`, 20 results, `more_pages` false), "Search — past the last page" (`page=4`) and "Search — whole dataset" (`per_page=120`) to the "People" folder in postman/ctam-jomockapi.postman_collection.json, with tests for the pagination block and result counts

**Checkpoint**: quickstart.md § 3 rows 2 and 3.

---

## Phase 5: User Story 3 - Integrator Retrieves One Office Holder (Priority: P1)

**Goal**: `GET /api/v1/people/{id}` returns one office holder, the same object as in a search (FR-002, FR-005, FR-006, FR-026).

**Independent Test**: AC-010 to AC-013.

- [X] T037 [P] [US3] Add to src/test/java/…/jo/services/PeopleServiceTest.java: `getById` of a known id returns the same object as that person in `search` for each flag value (AC-011); upper case matches (EC-005); an unknown well-formed id throws `PersonNotFoundException`; a malformed id throws `InvalidPersonIdException` before any lookup; a bad flag is reported before a malformed id (EC-010).
- [X] T038 [P] [US3] Add to src/test/java/…/jo/controllers/PeopleControllerTest.java: `GET /api/v1/people/{id}` gives `200` with a single object (no `results`); `404` "Person not found."; `400` "id must be a UUID."; `/api/v1/people/{id}/x` gives `404` "Resource not found."; `updated_since`, `page` or `per_page` on the single route gives `400` "Unsupported query parameter." (AC-027); `POST /api/v1/people/{id}` gives `405` with `Allow: GET`; `Accept: text/plain` on `/api/v1/people/{id}` gives `406` "Not acceptable." (FR-007)
- [X] T039 [US3] Implement `getById(String rawId, String includePrevious)` in src/main/java/…/jo/services/PeopleService.java (parse the flag, then the id with `PersonIdParser`, then the lookup). Add the `getPerson` operation to src/main/java/…/jo/controllers/PeopleController.java:
  - `@GetMapping(value = "/{id}")`;
  - path `id` with `@Parameter(schema = @Schema(format = "uuid", pattern = PersonIdParser.PATTERN))`;
  - the optional flag, with the same `@Schema(type = "boolean", defaultValue = "false")` override;
  - `200` with `CurrentOfficeHolderResponse`, `400` ("id must be a UUID.") and `404` ("Person not found."), with examples.
  
  T037 and T038 pass
- [X] T040 [US3] Add `ac010` to `ac013` and `ec005` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java (AC-013 with `abc`, a `per_id` and a `personal_code` taken from the dataset), and capture src/functionalTest/resources/golden/person_single.json for the first office holder of the default search. Add `singleOfficeHolderMatchesItsGoldenFile`
- [X] T041 [US3] Add "Single office holder" (`{{baseUrl}}/api/v1/people/{{personId}}`), "Single office holder — unknown id" (`00000000-0000-0000-0000-000000000000`) and "Single office holder — malformed id" (`abc`) to the "People" folder, with tests for status, the object's required fields (no `results` or `pagination`), the `id` equal to `{{personId}}`, and the error messages. Add `personId` (the first office holder of the default search) to postman/ctam-jomockapi.postman_environment.json

**Checkpoint**: quickstart.md § 3 rows 6, 8 and 9.

---

## Phase 6: User Story 5 - People Data Agrees With the Reference Data (Priority: P1)

**Goal**: Every reference on an office holder resolves to a served record with the same name (FR-037 to FR-045; closes DF-2, DF-3, DF-4, DF-6 and DF-9).

**Independent Test**: AC-018 to AC-023, against the default dataset.

- [X] T042 [P] [US5] Write src/test/java/…/jo/repository/PeopleReferencesTest.java (FR-045). Load the real People and reference-data repositories from the shipped configuration and build each office holder's response with both flag values through the real mapper. Assert from the reference-data side (`ReferenceDataRepository.findById` of the configured types, not the derivation code):
  - `ac018` gender;
  - `ac019` contract type and appointment title;
  - `ac020`: base location, `court_name`, `court_type` through `type_id`, `location` through `parent_id`, and `type` through `jurisdiction_id`;
  - `ac021` judiciary role;
  - `ac022`: ticket, and the jurisdiction through the ticket's category (FR-042);
  - `ac023`: each summary agrees with the dated authorisations (FR-044);
  - `ec008`: for the appointments of G-14's shared-name case, `base_location`, `court_name`, `court_type`, `location` and `type` each come from that appointment's own `base_location_id` record, and the two appointments' derived values differ wherever their records differ.
  
  Add a test that each of the five closed deferred items has at least one asserting case (DF-2, DF-3, DF-4, DF-6, DF-9), so an empty dataset can't pass vacuously.
- [X] T043 [P] [US5] Add `ac018` to `ac023` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java over HTTP: fetch the whole dataset with `include_previous_appointments=true` and each reference-data collection, then compare ids and names as T042 does
- [X] T044 [US5] Add closing notes to the deferred-item rows, each pointing at [013 FR-037 to FR-045](../013-people-api/spec.md) and `PeopleReferencesTest`, as 011 did for 010 DF-8:
  - specs/004-contract-types-reference-data/spec.md: DF-2 ("Closed by 013 …");
  - specs/005-genders-reference-data/spec.md: DF-3;
  - specs/006-judiciary-roles-reference-data/spec.md: DF-4;
  - specs/007-locations-reference-data/spec.md: DF-6;
  - specs/010-tickets-reference-data/spec.md: DF-9.

**Checkpoint**: SC-004 holds.

---

## Phase 7: User Story 7 - Requests Without Valid Credentials Are Refused (Priority: P1)

**Goal**: Both routes return `401` with no data, and authentication is checked first (FR-003, AC-029, AC-030).

**Independent Test**: Call both routes with no token and with a wrong token.

- [X] T045 [US7] Add `ac029` and `ac030` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java:
  - `ac029`: no `Authorization` header, and `Bearer wrong`, on the search and on the single route each give `401` with the message, `WWW-Authenticate: Bearer`, the correlation header, and no `results`, `id` or `appointments` in the body;
  - `ac030`: an invalid token with a missing `updated_since`, and with a malformed `id`, still gives `401`.
- [X] T046 [US7] Add "Search — no token" and "Single office holder — invalid token" (`Bearer {{invalidBearerToken}}`) to the "People" folder, with tests for `401`, the message and the `WWW-Authenticate` header

**Checkpoint**: SC-005 holds.

---

## Phase 8: User Story 4 - Integrator Asks for Previous Appointments (Priority: P2)

**Goal**: `include_previous_appointments` adds ended appointments and the authorisations under them, and changes nothing else (FR-020 to FR-022).

**Independent Test**: AC-014 to AC-017, on a person with both current and previous appointments.

- [X] T047 [P] [US4] Add to src/test/java/…/jo/services/PeopleServiceTest.java:
  - the flag absent, `false` and `true` for a person with both kinds of appointment (AC-014, AC-015);
  - a person with only previous appointments: every list empty except `judiciary_roles` when the flag is absent (EC-006);
  - the flag never changes which people match, their order or the pagination block (AC-017).
- [X] T048 [P] [US4] Write src/integrationTest/java/…/jo/PeopleClockIndependenceIntegrationTest.java (SC-003, D-8): two `@SpringBootTest` contexts, one with a `@Primary Clock` fixed at 2026-01-02 and one at 2031-07-01. The whole-dataset search with the flag `true` and with it `false` gives identical bodies in both. A third context with `jo.people.reference-date=2030-01-01` gives exactly the G-11 count (recorded in T014) fewer current appointments for the same people, order and pagination
- [X] T049 [US4] Add `ac014` to `ac017` and `ec006` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java. Choose the people from the flag-`true` whole-dataset response: one with an `end_date` before 2026-01-01 and one without, and one with only ended appointments. Also check that every `authorisations_with_dates[].appointment_id` is in the same response's `appointments` for both flag values (AC-016)
- [X] T050 [US4] Capture src/functionalTest/resources/golden/people_all_with_previous.json (`updated_since=2000-01-01&per_page=120&include_previous_appointments=true`) and add `wholeDatasetWithPreviousMatchesItsGoldenFile` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java
- [X] T051 [US4] Add "Search — with previous appointments" (`per_page=120&include_previous_appointments=true`), "Search — flag false" and "Single office holder — with previous appointments" to the "People" folder. The tests check that flag `true` gives at least as many appointments per person as flag `false`, the same `id` order and pagination, and that each `appointment_id` in `authorisations_with_dates` is in `appointments`

**Checkpoint**: quickstart.md § 3 row 7 and § 5 (reference date).

---

## Phase 9: User Story 6 - Invalid Requests Are Rejected (Priority: P2)

**Goal**: Every invalid case in the Error Scenario Summary returns `400` with its fixed message (FR-010, FR-013 to FR-016).

**Independent Test**: AC-024 to AC-028 and SC-006.

- [X] T052 [US6] Add `ac024` to `ac028`, `ec010` and `ec012` to src/functionalTest/java/…/jo/PeopleFunctionalTest.java as `@ParameterizedTest`s over the contract's error table:
  - missing, empty, malformed and date-time `updated_since`, and a non-existent date;
  - `page` and `per_page` of `0`, `-1`, `1.5`, `abc`, empty and `9223372036854775808`;
  - the flag as `yes`, `TRUE` and empty;
  - `foo=1` on both routes;
  - `updated_since`, `page` and `per_page` on the single route;
  - `page=1&page=2`, and `include_previous_appointments=true&include_previous_appointments=false` on the single route;
  - trailing slash and extra segment.
  
  Each asserts the status, the shared error shape via `ErrorResponseAssertions`, the message, `traceId` equal to `X-Correlation-Id`, and that the body never contains the raw value. `ec010` covers precedence pairs: repeated before undefined, `updated_since` before `page`, and the flag before a malformed `id`.
- [X] T053 [US6] Add "Search — missing updated_since", "Search — malformed updated_since" (`yesterday`), "Search — updated_since with a time", "Search — bad page" (`0`), "Search — bad per_page" (`abc`), "Search — bad flag" (`yes`), "Search — unknown parameter" (`foo=1`) and "Search — repeated parameter" (`page=1&page=2`) to the "People" folder, with tests for `400`, the error shape and each fixed message

**Checkpoint**: quickstart.md § 3 rows 10 to 12; SC-006.

---

## Phase 10: User Story 8 - Existing Behaviour Is Unchanged (Priority: P2)

**Goal**: Reference data, the healthcheck and all existing errors are unchanged, and the guard still holds (FR-050 to FR-052, AC-031, AC-032).

**Independent Test**: Every earlier suite and golden file passes untouched; the guard passes with its People exception.

- [X] T054 [US8] Confirm every † file from plan.md § Source Code is in `PEOPLE_SOURCES`, and that no other People-related file needed listing: `PeopleApiResponse` and the generic components must pass the plain scan. Add `listedPeopleSourcesAreExactlyThePlannedOnes` to src/test/java/…/jo/architecture/NoTypeSpecificCodeTest.java, asserting the list's exact contents so additions are reviewed (AC-032). Add `referenceDataSourcesUseNoPeopleCode`: no reference-data main source (`ReferenceDataController`, `ReferenceDataService`, `ReferenceDataTypeRegistry`, `ReferenceIdParser`, `ReferenceDataRepository`, `FixtureReferenceDataRepository`, `ReferenceDataMapper`, and the `entity/` reference-data classes) mentions `People`, `Person`, `OfficeHolder` or `PeopleProperties` (FR-051)
- [X] T055 [US8] Add to src/integrationTest/java/…/jo/InternalFailureIntegrationTest.java a case where a mocked `PeopleRepository.findAll()` throws, so the search returns `500` "Internal server error." with no internal detail (EC-011). Write src/integrationTest/java/…/jo/PeopleStartupIntegrationTest.java with `ApplicationContextRunner` or `SpringApplication` cases (FR-046): a duplicated-`id` fixture fails start-up with the documented message; `jo.people.reference-types.gender-id=widgets` fails; `empty.json` starts and searches return `pages` 0 (EC-013). Add src/integrationTest/resources/people/invalid-duplicate-id.json
- [X] T056 [US8] Run `git diff --stat main -- src/functionalTest/resources/golden/ src/functionalTest/java/uk/gov/hmcts/ctam/jo/ ':!src/functionalTest/java/uk/gov/hmcts/ctam/jo/PeopleFunctionalTest.java' ':!src/functionalTest/java/uk/gov/hmcts/ctam/jo/testsupport/PeopleHttp.java' ':!src/functionalTest/resources/golden/people_*' ':!src/functionalTest/resources/golden/person_single.json'` and confirm that no existing golden file or reference-data functional test changed (AC-031, SC-008). Record the result in specs/013-people-api/plan.md § Implementation notes

**Checkpoint**: SC-008 holds.

---

## Phase 11: User Story 9 - Documentation and Example Requests Cover the New Routes (Priority: P3)

**Goal**: Both routes are fully documented in the generated OpenAPI document and the Postman collection (FR-053 to FR-055, AC-033, AC-034).

**Independent Test**: Fetch `/v3/api-docs` and run newman against a running instance.

- [X] T057 [US9] Extend src/main/java/…/jo/config/OpenApiConfig.java so the customiser's path test covers `ApiPaths.PEOPLE` and `ApiPaths.PEOPLE + "/"` as well as reference data. People operations then get the correlation request header, the response headers and the shared `401`/`406`/`500`. `describeAttributeName` must still only touch parameters named `attribute_name`. Extend src/test/java/…/jo/config/OpenApiConfigTest.java: a People operation gets the shared responses; a reference-data operation's output is unchanged
- [X] T058 [US9] Write src/integrationTest/java/…/jo/PeopleOpenApiContractTest.java (FR-053, AC-033) against `/v3/api-docs`, modelled on `ReferenceDataOpenApiContractTest`. Assert:
  - both paths with operation ids `searchPeople` and `getPerson`, the "People" tag and `bearerAuth`;
  - every parameter with its `in`, `required`, type, format, minimum, default and pattern from contracts/people-api.md;
  - responses `200`, `400`, `401`, `406` and `500` (plus `404` on `getPerson`), each with the `X-Correlation-Id` header, and `WWW-Authenticate` on `401`;
  - the seven schemas with exactly the spec's properties, `required` lists and nullable flags;
  - for the actual whole-dataset response (`per_page=120&include_previous_appointments=true`) and an actual single response, a hand-rolled walk checks every object against its component schema, following `$ref` and `items`:
    - the property names are a subset of the schema's `properties`;
    - every `required` property is present;
    - a `null` appears only where `nullable` is true;
    - each value's JSON type matches the schema's `type`, and `format` `date` matches `^[0-9]{4}-[0-9]{2}-[0-9]{2}$` and `uuid` the canonical lower-case pattern.

    This adds no new dependency (`gradle.lockfile` untouched);
  - the reference-data operations and schemas are unchanged: `ReferenceDataOpenApiContractTest` passes untouched.
- [X] T059 [US9] Run newman against `./gradlew bootRun` (stop anything on :8080 first) with the collection and environment. Every "People" request and every existing request passes, and the collection has no hard-coded host, token, id or date outside the environment (FR-054, AC-034). Fix any request that doesn't
- [X] T060 [P] [US9] Add a "People" section to README.md:
  - both routes and their parameters;
  - the flag and the reference date (D-8), and `jo.people` configuration with `JO_PEOPLE_FIXTURE` and `JO_PEOPLE_REFERENCE_DATE`;
  - the dataset (120 synthetic office holders, reserved email and phone ranges, names derived from reference data);
  - start-up validation;
  - the error table;
  - DF-2, DF-3, DF-4, DF-6 and DF-9 marked closed, and DF-11 (leavers, deleted) open.
- [X] T061 [P] [US9] Add a `013-people-api` entry to "Current repo state" in CLAUDE.md, in the style of the 002 to 012 entries:
  - the routes, the dataset, and the generic pagination, parameter and interceptor components;
  - the guard's People exception (FR-052) and the config-only type mapping;
  - start-up validation;
  - DF-11.
  
  Extend the "Reference data" paragraph to say the People fixture is synthetic, with only reference ids taken from the served fixtures. Add "Extended by 013: see [the People contract](../../013-people-api/contracts/people-api.md)." to specs/002-reference-data-api/contracts/reference-data-api.md

**Checkpoint**: quickstart.md § 4.

---

## Phase 12: Polish & Cross-Cutting Concerns

- [X] T062 Write src/smokeTest/java/…/jo/PeopleSmokeTest.java (SC-009) with the harness of `ReferenceDataSmokeTest` (`WARM_UP_CALLS`, `MEASURED_CALLS`, p95, the default 1 s bound and 100 ms under `-Dperf.strict=true`), over three requests, each asserting `200`: the single office holder, the default search, and `per_page=120&include_previous_appointments=true`
- [X] T063 Run `./gradlew check -PskipOwasp` and `./gradlew smokeTest -Dperf.strict=true`. Record in specs/013-people-api/plan.md § Implementation notes the per-suite counts, JaCoCo line and branch coverage (still ≥ 90% and 80%) and each People request's strict p95. If the strict p95 fails, apply R11's caching fallback in src/main/java/…/jo/services/PeopleService.java and record it
- [ ] T064 Run `./gradlew check` once with OWASP (no `-PskipOwasp`) to confirm the gate is unaffected (no dependency changed; `gradle.lockfile` untouched)
- [X] T065 Walk through specs/013-people-api/quickstart.md §§ 1–5 against `./gradlew bootRun`, set the spec's Status to Implemented in specs/013-people-api/spec.md, update the Notes in specs/013-people-api/checklists/requirements.md (no markers remain), and record in specs/013-people-api/plan.md § Implementation notes that every expected result matched, or list the differences

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks every story.
  - T002 first (it's a refactor and must stay green).
  - T003–T006 (tests) before T007–T010; T007 before T008–T010.
  - T011 (guard) before any † file: T012, T013, T017–T019, T021–T023.
  - T012 → T013 → T014 → T015; T016 → T017 → T018 → T019 → T020.
  - T021 and T022 after T013; T023 after T019, T021 and T022; T024 then T025 last.
- **User stories (Phases 3–11)**: all after Phase 2.
  - US1 creates `PeopleService`, `PeopleController`, `PeopleFunctionalTest`, `PeopleHttp` and the Postman folder. Every later story extends those files, so US1 comes first.
  - The others then run in priority order, US2 → US3 → US5 → US7 → US4 → US6 → US8 → US9, or with one owner for `PeopleFunctionalTest` and the Postman collection.
- **Polish (Phase 12)**: after the stories; T062, then T063, T064, T065.

### User story dependencies

- **US1 (P1)**: Phase 2 only. MVP.
- **US2, US3, US5, US7 (P1)**: US1's service and controller.
- **US4, US6 (P2)**: US1 and US3 (single route).
- **US8 (P2)**: all routes present.
- **US9 (P3)**: US1 and US3; its contract test covers every parameter, so it is best last.

### Parallel opportunities

- Phase 2 tests: T003, T004, T005 and T006 together.
- Phase 2 models: T013 and T021 (different files), then T022.
- US1: T026, T027 and T028.
- US3: T037 and T038. US5: T042 and T043. US4: T047 and T048.
- US9: T060 and T061.

### Parallel example: Phase 2 component tests

```text
T003 PaginatorTest                         — table over N × P × page
T004 QueryParameterParserTest              — date, whole number, flag classes
T005 PersonIdParserTest                    — UUID forms
T006 DeclaredQueryParametersInterceptorTest — undefined and repeated
```

### Parallel example: User Story 1

```text
T026 PeopleServiceTest (search)
T027 PeopleControllerTest (search)
T028 PeopleHttp + ContractPaths.PEOPLE
```

---

## Implementation Strategy

### MVP first (US1)

1. Phase 1, then Phase 2: shared components test-first, the guard, data, loading and models, with the build green at T025.
2. Phase 3 (US1): the search works and is tested. **Stop and validate**: quickstart.md § 3 rows 1, 4 and 5.

### Incremental delivery

3. The remaining P1 stories (US2, US3, US5, US7), then P2 (US4, US6, US8), then P3 (US9), validating at each checkpoint.
4. Polish: smoke guard, full gates with OWASP, quickstart walk-through.

### Notes

- No constitution change is needed. FR-052's guard scope is a reviewed spec decision (spec Clarifications), not an amendment.
- The People fixture generator stays out of the repository (R3); check `git status` before every commit.
- `-PskipOwasp` is for local runs only; CI runs the full `check`.
- Keep Javadoc lines within 120 characters, and keep † files to the FR-052 contract names in comments too.
- Golden files are captured from the running service, never hand-written; review each diff before committing.
