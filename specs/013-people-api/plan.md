# Implementation Plan: People API — Search and Single Office Holder

**Branch**: `013-people-api` | **Date**: 2026-11-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/013-people-api/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves the two E-Links People routes that return current office holders:

- `GET /api/v1/people`: a search by `updated_since`, paginated;
- `GET /api/v1/people/{id}`: one office holder.

Both are behind the existing bearer-token authentication, correlation IDs and shared error shape. It is the first feature outside reference data, and it builds the shared pagination and query-parameter components that the leavers and deleted features (DF-11) will reuse.

**Data**: 120 synthetic office holders in a checked-in fixture (research R3; data-model.md § 4). The fixture holds only ids for reference data. Every name an office holder carries (gender, appointment title, contract type, base location and its location type, parent location and jurisdiction, judiciary role, ticket, and the ticket's jurisdiction) is derived at start-up from the reference data already served (R2). That type mapping lives in configuration, never in Java (R4, FR-052). Because the names are read from the records they name, they always agree with them. That closes 004 DF-2, 005 DF-3, 006 DF-4, 007 DF-6 and 010 DF-9, and an independent test checks it (FR-045).

**Behaviour**:
- `updated_since` is a strict `YYYY-MM-DD` date, inclusive. Results are ordered by (`last_updated`, `id`).
- `page`/`per_page` default to 1/50 and accept any positive 64-bit whole number.
- `include_previous_appointments` filters `appointments` and the authorisations under them against a configured reference date (default 2026-01-01, D-8), never the clock.
- Undefined or repeated parameters return `400`, values are validated in a fixed order with fixed messages, and a malformed `id` returns `400` while an unknown one returns `404` (R5).
- An invalid configured fixture stops start-up with a message naming the first broken rule (FR-046; R6).

**Guard**: `NoTypeSpecificCodeTest` gains the reviewed People exception you chose during planning. A fixed list of People sources may use only the contract's names; every other type name still fails (R1; spec FR-052, updated).

**Spec refinements made while planning** (recorded in spec Clarifications or the requirements themselves):
- FR-052 and AC-032: the guard scope you chose.
- FR-013 and EC-003: `page`/`per_page` are bounded at 2^63−1, so no request can overflow.
- EC-013: an empty People fixture is valid. The existing `ReferenceDataExtensionIntegrationTest` needs this, because it replaces the reference types.

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi 3.1.1, Jackson 3, Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: Classpath JSON fixtures loaded at start-up. New: `people/office_holders.json`, 120 office holders, about 250 KB.

**Testing**: JUnit 5 and AssertJ in `test`, `integrationTest`, `functionalTest` and `smokeTest`; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: SC-009. The single route, a default search, and the whole dataset on one page with the flag on each meet p95 < 1 s by default and < 100 ms with `-Dperf.strict=true` (R11).

**Constraints**:
- Reference-data responses, the healthcheck and every existing error are byte-for-byte unchanged (FR-050). The reference-data OpenAPI output is unchanged too.
- No reference-data type name appears in Java. People sources use only the contract names in FR-052.
- Nothing depends on the wall clock (Principle V; D-8).
- No real personal data: `.example` email addresses and the Ofcom drama phone range only (FR-033).
- `per_page` has no upper bound below 2^63−1, so the pagination arithmetic must not overflow (R8).

**Scale/Scope**: 2 new routes, 7 response schemas, 120 office holders, about 25 new main classes, 11 reference-data types unchanged.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** Paths (under the project's `/api/v1` base, as for reference data), parameters, field names, nesting and required flags follow the Swagger. The undocumented error statuses (D-7) and the configured reference date (D-8) are recorded as contract deviations in the spec. The inferred types are not deviations: the Swagger states none. |
| II. Java Spring Boot Architecture | **Pass.** Package by layer: `PeopleController` (thin: binds strings, returns DTOs); `PeopleService`, `Paginator`, `QueryParameterParser`, `PersonIdParser`, `AppointmentStatus` and `AuthorisationSummaries` in `services/`; `FixturePeopleRepository` and `PeopleDatasetValidator` in `repository/` (the validator sits beside the repository that loads the fixture, as reference-data validation does); records in `entity/`; DTOs in `domain/`; `PeopleMapper` in `mappers/`; the new exceptions in `exceptions/`, handled by the single `GlobalExceptionHandler`; the interceptor in `filters/`; `PeopleProperties` in `config/`. |
| III. Reuse Before Duplication | **Pass.** One pagination component (R8), one query-parameter parser and one declared-parameters interceptor, all reusable unchanged by leavers and deleted (DF-11). The strict fixture `JsonMapper` moves into a shared factory used by both repositories (R6). The OpenAPI shared-error customiser is extended, not copied (R9). Names are resolved through the existing `ReferenceDataRepository`. |
| IV. Synthetic Data Only | **Pass.** Entirely synthetic People data with no extract (FR-030), reserved email and phone ranges (FR-033). Every reference to served reference data resolves: the validator checks this at start-up (V-9) and `PeopleReferencesTest` independently (FR-045), so DF-2, DF-3, DF-4, DF-6 and DF-9 are closed. There are no forward references. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture with a fixed order (FR-012, FR-025), and a configured reference date instead of the clock. Golden files, a repeated-call test and a test with the clock moved years ahead (SC-003) check it. |
| VI. Behavioural Fidelity | **Pass.** `updated_since` filters genuinely, `page`/`per_page` page genuinely, and the flag changes the content observably. Undefined parameters are rejected, never ignored (FR-015). Pagination goes through one component (FR-018). |
| VII. Generic Reference-Data Handling | **Pass.** The reference-data mechanism is untouched apart from the shared `JsonMapper` factory, which changes no behaviour. People reads reference data through the generic repository, with the type names in configuration. The guard keeps forbidding type names, with the reviewed People exception (R1). |
| VIII. Typed API Models | **Pass.** Seven contract DTOs as records, entities separate from DTOs, MapStruct `PeopleMapper`, Lombok on the entities. No `Map<String, Object>`; the reference types are typed properties, not a map (R4). |
| IX. Centralised Validation and Error Handling | **Pass.** The new exceptions extend `ApiRequestException` with fixed messages and are mapped in `GlobalExceptionHandler` (4xx logged as warnings). Parameter validation lives in one parser, and every error uses the shared shape. |
| X. Configuration Over Hard-Coding | **Pass.** The fixture location, reference date and reference types are configured under `jo.people`, and an empty fixture is supported (EC-013). |
| XI. Testability | **Pass (planned).** Unit, controller, integration (OpenAPI contract, start-up, clock), functional (AC-001 to AC-030, golden files) and smoke tests (R10). |
| XII. Simplicity and Maintainability | **Pass.** Derived names avoid a second consistency mechanism (R2); no caching until measurement asks for it (R11); no new dependency. |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged, and the new code is covered to the existing gate. Inherits 002's open Sonar deferral. |
| XIV. Observability & Traceability | **Pass.** The existing correlation-ID filter already covers `/api/v1/people`. Rejections log a sanitised diagnostic, never caller input in a body. |
| XV. Security by Design | **Pass.** Both routes are behind the existing bearer filter (prefix `/api/`), and authentication is checked first (FR-003, AC-030). Nothing is relaxed. The richest data in the mock is synthetic and uses reserved contact ranges. |
| XVI. Object-Oriented Design Discipline | **Pass.** Small single-purpose components composed by `PeopleService`. The controller talks only to the service. |
| XVII. Postman Artifacts | **Pass (planned).** A "People" folder with about 25 requests and tests; the environment gains `personId` and `peopleMidDate` (R12). |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** Generated from annotations; the shared parts come from the extended customiser. `PeopleOpenApiContractTest` checks the document against actual responses (FR-053). |

**Non-principle checks**:
- **CLAUDE.md "Source of truth" rule**: no constitution conflict, so no amendment is needed. The guard-scope change is a spec decision you approved (spec Clarifications), not a constitution change.
- **Earlier artifacts that become stale**:
  - the deferred-item tables of 004, 005, 006, 007 and 010: closing notes;
  - the README's open deferred items;
  - the comments in `NoTypeSpecificCodeTest` that say "the People feature adds its own reviewed exception": replaced by the exception itself;
  - `ReferenceDataExtensionIntegrationTest`: gains an empty People fixture.

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

**Post-design re-check (after Phase 1)**: Still passes. The design adds no dependency or layer and no hard-coded type name. `StrictFixtureJson` is the only change to reference-data code, and it is behaviour-preserving, which the existing repository tests prove unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/013-people-api/
├── plan.md                 # This file
├── research.md             # Phase 0: decisions R1–R12
├── data-model.md           # Phase 1: DTOs, fixture format, derivations, generation (G-*) and start-up (V-*) rules
├── quickstart.md           # Phase 1: run and check guide
├── contracts/
│   └── people-api.md       # Phase 1: HTTP contract for both routes
├── checklists/
│   └── requirements.md     # From /speckit-specify
└── tasks.md                # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Files marked † are on the guard's reviewed People list (R1).

```text
src/main/java/uk/gov/hmcts/ctam/jo/
├── config/ApiPaths.java                         # + PEOPLE = "/api/v1/people"
├── config/PeopleProperties.java          †      # NEW: jo.people (fixture, reference-date, reference-types) (R4)
├── config/WebConfig.java                        # + register DeclaredQueryParametersInterceptor for PEOPLE (R5)
├── config/OpenApiConfig.java                    # customiser also covers PEOPLE operations (R9)
├── controllers/PeopleController.java     †      # NEW: two GET operations, "People" tag
├── domain/PeopleApiResponse.java                # NEW
├── domain/PaginationResponse.java               # NEW (generic, R8)
├── domain/CurrentOfficeHolderResponse.java †    # NEW
├── domain/AppointmentsResponse.java      †      # NEW
├── domain/JudiciaryRoleResponse.java     †      # NEW
├── domain/AuthorisationSummaryResponse.java †   # NEW
├── domain/AuthorisationsResponse.java    †      # NEW
├── entity/OfficeHolderRecord.java        †      # NEW (data-model § 2)
├── entity/AppointmentRecord.java         †      # NEW
├── entity/JudiciaryRoleRecord.java       †      # NEW
├── entity/AuthorisationRecord.java       †      # NEW
├── entity/OfficeHolder.java              †      # NEW: a validated record with its resolved names (§ 3)
├── exceptions/InvalidQueryParameterException.java    # NEW (fixed per-parameter messages)
├── exceptions/UndefinedQueryParameterException.java  # NEW "Unsupported query parameter."
├── exceptions/RepeatedQueryParameterException.java   # NEW "Query parameters must not be repeated."
├── exceptions/InvalidPersonIdException.java          # NEW "id must be a UUID."
├── exceptions/PersonNotFoundException.java           # NEW "Person not found."
├── exceptions/GlobalExceptionHandler.java            # + the five exceptions (400 ×4, 404)
├── filters/DeclaredQueryParametersInterceptor.java   # NEW (generic, R5)
├── mappers/PeopleMapper.java             †      # NEW (MapStruct; flag and reference date as context)
├── repository/PeopleRepository.java      †      # NEW interface: findAll() ordered, findById(id)
├── repository/FixturePeopleRepository.java †    # NEW: load, validate, resolve, order (R6)
├── repository/PeopleDatasetValidator.java †     # NEW: V-1 … V-10
├── repository/StrictFixtureJson.java            # NEW: shared strict JsonMapper (moved from FixtureReferenceDataRepository)
├── repository/FixtureReferenceDataRepository.java    # uses StrictFixtureJson; no behaviour change
├── services/PeopleService.java           †      # NEW: parse → filter → paginate → map
├── services/AppointmentStatus.java       †      # NEW: current vs previous against the reference date
├── services/AuthorisationSummaries.java  †      # NEW: FR-044 grouping
├── services/Paginator.java                      # NEW (generic)
├── services/Page.java                           # NEW (generic)
├── services/PageRequest.java                    # NEW (generic, defaults 1 / 50)
├── services/QueryParameterParser.java           # NEW (generic)
└── services/PersonIdParser.java                 # NEW

src/main/resources/
├── application.yml                              # + jo.people block (R4)
└── people/office_holders.json                   # NEW: 120 office holders (data-model § 4)

src/test/java/uk/gov/hmcts/ctam/jo/              # R10: unit and @WebMvcTest
├── architecture/NoTypeSpecificCodeTest.java     # People exception, its own tests (R1)
├── controllers/PeopleControllerTest.java        # NEW
├── filters/DeclaredQueryParametersInterceptorTest.java # NEW
├── mappers/PeopleMapperTest.java                # NEW
├── repository/FixturePeopleRepositoryTest.java  # NEW: every V-rule
├── repository/PeopleFixtureRulesTest.java       # NEW: every G-rule
├── repository/PeopleReferencesTest.java         # NEW: FR-045
├── services/{Paginator,QueryParameterParser,PersonIdParser,AppointmentStatus,AuthorisationSummaries,PeopleService}Test.java # NEW
├── exceptions/GlobalExceptionHandlerTest.java   # + new exceptions
├── config/OpenApiConfigTest.java                # + People paths
└── testsupport/PeopleFixtures.java              # NEW: builders for small test datasets
src/test/resources/people/                       # NEW: invalid and minimal fixtures for V-rule tests

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
├── PeopleOpenApiContractTest.java               # NEW
├── PeopleStartupIntegrationTest.java            # NEW
├── PeopleClockIndependenceIntegrationTest.java  # NEW
├── InternalFailureIntegrationTest.java          # + People route
└── ReferenceDataExtensionIntegrationTest.java   # + jo.people.fixture = empty fixture (EC-013)
src/integrationTest/resources/people/            # NEW: empty.json, invalid fixtures

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── PeopleFunctionalTest.java                    # NEW: AC-001 … AC-030
└── testsupport/PeopleHttp.java                  # NEW
src/functionalTest/resources/golden/
├── people_all_with_previous.json                # NEW
├── people_default_page1.json                    # NEW
└── person_single.json                           # NEW

src/smokeTest/java/uk/gov/hmcts/ctam/jo/PeopleSmokeTest.java # NEW: SC-009
src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
├── ContractMessages.java                        # + People messages
└── ContractPaths.java                           # + PEOPLE

postman/ctam-jomockapi.postman_collection.json   # + "People" folder (R12)
postman/ctam-jomockapi.postman_environment.json  # + personId, peopleMidDate
README.md, CLAUDE.md                             # FR-055
specs/00{4,5,6,7}-*/spec.md, specs/010-*/spec.md # closing notes on DF-2, DF-3, DF-4, DF-6, DF-9
specs/002-reference-data-api/contracts/reference-data-api.md # pointer to contracts/people-api.md
```

**Structure Decision**: The existing single Spring Boot project and package-by-layer layout (Principle II). People code sits beside the reference-data code in the same layer packages, with no feature sub-packages. Generic components (pagination, query parameters, the declared-parameters interceptor, `StrictFixtureJson`) carry no People names, so leavers and deleted can reuse them without joining the guard's reviewed list.

## Implementation order (for `/speckit-tasks`)

1. **Foundation, test-first**: `StrictFixtureJson` extraction (existing tests must stay green); `Paginator`, `PageRequest`, `Page`, `PaginationResponse`; `QueryParameterParser`, `PersonIdParser`; the new exceptions and their handler mappings; `DeclaredQueryParametersInterceptor`.
2. **Guard**: the People exception in `NoTypeSpecificCodeTest` with its own tests, before any People source exists. The list's existence check is satisfied as the files land (tasks order them so the guard stays green at each step).
3. **Data**: `PeopleProperties` and its configuration; entities; generate `office_holders.json`; `PeopleFixtureRulesTest`.
4. **Loading**: `PeopleDatasetValidator` and `FixturePeopleRepository` (V-rules, test-first); `PeopleReferencesTest`; the empty-fixture change in `ReferenceDataExtensionIntegrationTest`.
5. **Serving**: DTOs, `PeopleMapper`, `AppointmentStatus`, `AuthorisationSummaries`, `PeopleService`, `PeopleController`; OpenAPI customiser; unit and controller tests.
6. **Suites**: integration (contract, start-up, clock, 500), functional and golden files, smoke.
7. **Artifacts**: Postman folder and environment, README, CLAUDE.md, closing notes in earlier specs, the pointer in 002's contract.

## Complexity Tracking

No violations to justify. Inherited: 002's Sonar deferral (unchanged).
