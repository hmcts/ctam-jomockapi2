# Implementation Plan: Reference Data API — Location Types

**Branch**: `008-location-types-reference-data` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-location-types-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves LocationType through the generic reference-data routes, as `location_types` only. The singular name `location_type` is not served (spec FR-001; Constitution VII). Its 16 records keep the extract's ids 1 to 16 (DR-1, as 007 did for Location), so every Location and BaseLocation `type_id` resolves. Each record carries one reference field, `jurisdiction_id`, and one text field, `integration_code` (D-4); names repeat ("Region" ×3, "AC" ×2).

Serving `integration_code` needs **one generic main-code change**, modelled on 004's boolean fields: a per-type `text-fields` declaration, carried through:
- configuration, registry and type, with a new `TextField` enum (R2);
- start-up validation: present, non-null, non-empty and trimmed for declaring types, absent for every other type (R3);
- the response DTO, where `integration_code` is an optional, non-nullable string after `jurisdiction_id` (R4).

No code names a type, and the other six types' responses stay byte-for-byte unchanged. Table-driven tests check that every Location and BaseLocation `type_id` resolves and that their jurisdictions agree with their type's, which closes 003 DF-1 and 007 DF-5 for `type_id` (spec FR-011, FR-012).

**What changes**:
- **Data**: `location_types.json`, 16 records generated once from the local extract. Ids, names, jurisdiction ids and integration codes are copied; dates follow 002's rule by ascending-id position (ids 7 and 14 ended). The generating script proves the copied columns equal the extract, and that references resolve and agree, before the file is checked in (research R5; data-model.md § 4–5).
- **Main code** (generic): `TextField`; `text-fields` in configuration, registry and type; text-field validation in fixture loading; `integration_code` on the response.
- **Tests**:
  - new `LocationTypesFunctionalTest` with its golden file;
  - `ReferenceDataReferencesTest`: two `type_id` rows and a new jurisdiction-agreement table;
  - fixture-rules test: text fields and a `location_types` row;
  - text-field unit cases in the registry, type, repository and mapper tests;
  - OpenAPI contract assertions, the unsupported-name list, and a smoke guard (R6–R10).
- **Documentation and examples**: a "Location Types" Postman sub-folder; README (seven types, and the new option); CLAUDE.md; the contract delta; a pointer from 002's contract; DF notes in 003's and 007's specs (R11).

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi, Jackson 3 (with Jackson 2 annotations), Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: classpath JSON fixtures loaded at start-up. One more: `location_types.json`, 16 records, about 4 KB.

**Testing**: JUnit 5 and AssertJ in the `test`, `integrationTest`, `functionalTest` and `smokeTest` suites; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: Collection p95 within the smoke suite's default 1 s bound, and within 100 ms when run with `-Dperf.strict=true`. At 16 records this is the second-smallest collection.

**Constraints**:
- No code names a reference-data type (FR-003, FR-018).
- The six existing types' responses are byte-for-byte unchanged (FR-005, FR-019, AC-018).
- The extract is used once and never committed (FR-007).
- Names and the copied codes are reproduced exactly (FR-008).
- References between served types resolve, and jurisdictions agree (FR-011, FR-012).

**Scale/Scope**: Seven configured types (194 + 1,462 + 14 + 4 + 164 + 2,000 + 16 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `location_types` is a contract-valid value. Its singular form is not served, which Principle I allows. The record shape is the contract's `ReferenceDataResponse` plus `name` (D-1), `jurisdiction_id` (D-2) and `integration_code` (D-4, new, recorded in the spec's Contract Deviations as permanent and scope-justified). DR-1 concerns id values, which the contract doesn't define, so it isn't a contract deviation. |
| II. Java Spring Boot Architecture | **Pass.** Changes stay in their layers: `config/` (properties), `services/` (registry), `entity/` (`TextField`, type, record), `repository/` (validation), `domain/` (response). Controllers, services' request logic and the mapper's code are untouched. |
| III. Reuse Before Duplication | **Pass.** Text fields reuse `DeclaredField`, `declaredFields`, `validateDeclaredFields` and the `name` rules' messages. The new tests reuse `ReferenceDataHttp`, the fixture-rules table and the references table. |
| IV. Synthetic Data Only | **Pass.** Only public location-type names, ids, jurisdiction codes and integration codes come from the extract; dates are synthetic. The references between served types (Location → LocationType, BaseLocation → LocationType) resolve, verified by `ReferenceDataReferencesTest`, which closes 003 DF-1 and 007 DF-5 for `type_id`. Jurisdictions agree between a location and its type (internal consistency), verified by the same test. The forward reference `jurisdiction_id` → `jurisdictions` is recorded as DF-7, copied unchanged, and the completeness check makes the closing test unavoidable. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in fixed order; the golden file and repeated-call test check it. |
| VII. Generic Reference-Data Handling | **Pass.** The only behavioural code change is a generic option any type can declare. `location_type` is not served and has no `aliases` entry. The guard needs no change: its `location` fragment already covers `location_type`. |
| VIII. Typed API Models | **Pass.** `integration_code` is a typed `String` component, published as an OpenAPI `string`. No `Map` and no untyped JSON (research R2 rejected a map of extra fields). |
| IX. Centralised Validation and Error Handling | **Pass.** Fixture problems still fail at start-up through the repository's single `invalid(…)` path. API errors are unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** The text field is declared in `application.yml`, not coded per type. |
| XI. Testability | **Pass (planned).** Unit tests: registry, type, repository, mapper, serialisation, fixture rules, references. Integration: the OpenAPI contract. Functional: AC-001 to AC-020 over HTTP. Smoke: latency (research R6–R10). |
| XII. Simplicity and Maintainability | **Pass.** The new option copies 004's boolean-field pattern exactly; research R2–R4 record the rejected, larger alternatives. |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged. The new branches (declared versus not; each invalid value) are covered by unit cases. Inherits 002's open Sonar deferral. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** A "Reference Data › Location Types" sub-folder with ten requests and tests, with no hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration, and `integration_code` comes from the DTO annotation. The contract test asserts both, and a contract delta records the change. |

Principles VI, XIV, XV and XVI: no change from 002. No filter, logging, security or class-design rule is affected.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: two ended records; LocationType's ids have no gaps, so the id-gap clause is met across the shipped default data, as 005 established (spec EC-001).
- **The feature description said "no main-code change"**: superseded by the spec's Clarifications (Q1, revised), which chose to serve `integration_code`. FR-003 and AC-021 now allow exactly the generic text-field capability.
- **Earlier artifacts that become partly stale**: 007's FR-002 lists the supported names; this spec's FR-002 supersedes it. 003's DF-1 and 007's DF-5 are closed for `type_id`, and each spec gets a one-line note (R11). `ReferenceDataFunctionalTest` and the README use `location_types` as an unserved name; both are updated (R9, R11). Postman's unsupported example is `jurisdictions`, which is unaffected.

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/008-location-types-reference-data/
├── plan.md                                          # This file
├── research.md                                      # Phase 0: decisions R1–R11
├── data-model.md                                    # Phase 1: the new type, text fields, fixture rules
├── quickstart.md                                    # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-location-types.md         # Phase 1: contract delta against 002–007
├── checklists/
│   └── requirements.md                              # From /speckit-specify
└── tasks.md                                         # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/uk/gov/hmcts/ctam/jo/
├── config/ReferenceDataProperties.java              # + textFields (R2)
├── services/ReferenceDataTypeRegistry.java          # resolve and validate text-fields (R2)
├── entity/TextField.java                            # NEW: INTEGRATION_CODE (R2)
├── entity/DeclaredField.java                        # Javadoc: text-fields (R2)
├── entity/ReferenceDataType.java                    # + textFields (R2)
├── entity/ReferenceDataRecord.java                  # + integrationCode, Nulls.FAIL (R3)
├── repository/FixtureReferenceDataRepository.java   # validateTextFields (R3)
└── domain/ReferenceDataResponse.java                # + integration_code after jurisdiction_id (R4)

src/main/resources/
├── application.yml                                  # + location_types entry; base_locations/locations comments (R1)
└── reference-data/location_types.json               # NEW: 16 records (data-model.md § 4)

src/test/java/uk/gov/hmcts/ctam/jo/
├── repository/ReferenceDataFixtureRulesTest.java    # text fields; location_types row (R6)
├── repository/ReferenceDataReferencesTest.java      # type_id rows; jurisdiction agreement (R7)
├── repository/FixtureReferenceDataRepositoryTest.java   # text-field cases; new TypeProperties argument (R8)
├── services/ReferenceDataTypeRegistryTest.java      # text-field declaration cases (R8)
├── entity/ReferenceDataTypeTest.java                # text-field set copy (R8)
├── mappers/ReferenceDataMapperTest.java             # integrationCode mapping (R8)
├── controllers/ReferenceDataControllerTest.java     # serialisation of integration_code (R8)
└── testsupport/ReferenceDataFixtures.java           # new argument; textType builder (R8)
src/test/resources/reference-data/                   # NEW valid/invalid text-field fixtures (R8)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                               # + LOCATION_TYPES

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java            # enum, description, property order, integration_code (R9)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java                 # unsupported list: 14 names + location_types variants (R9)
└── LocationTypesFunctionalTest.java                 # NEW: 008 AC-001 … AC-020 over real HTTP (R10)
src/functionalTest/resources/golden/
└── location_types.json                              # NEW: captured collection response (AC-005)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                      # + LOCATION_TYPES (R9)

postman/ctam-jomockapi.postman_collection.json       # + Reference Data › Location Types (10 requests)
README.md                                            # seven types; type_id resolves; § Adding a type: text-fields
CLAUDE.md                                            # § Current repo state: + 008; § Reference data: location-type values
specs/002-reference-data-api/contracts/reference-data-api.md    # one-line pointer to the 008 delta
specs/003-base-locations-reference-data/spec.md                 # DF-1: note that 008 closed it for type_id
specs/007-locations-reference-data/spec.md                      # DF-5: note that 008 closed it for type_id
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. The one new main class, `TextField`, belongs in `entity/` next to `ReferenceField` and `BooleanField`.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. Contract deviation D-4 (`integration_code`) is documented in the spec as permanent and scope-justified, and was agreed in the spec's Clarifications. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-location-types.md, quickstart.md).

The design adds one enum, one configuration list, one record field, one response field and one validation method, with no new dependency, layer or endpoint, and no change to the mapper's code. Three details strengthen the assessments:
- **Byte-identical responses (V, VII)**: start-up validation guarantees only declaring types carry a text field (R3), so `NON_NULL` leaves it out of every other type's response, and the unchanged golden files prove it end to end.
- **Reference and agreement checks (IV)**: `ReferenceDataReferencesTest`'s completeness check demands the two `type_id` rows as soon as `location_types` is configured, and its new agreement table makes "a location's jurisdiction is its type's jurisdiction" a build failure if it ever stops holding.
- **One pattern for declared fields (III, XII)**: text fields reuse 004's boolean-field path end to end, so the three kinds of declared field differ only where their values differ (ids, booleans, strings).

The quickstart's expected values were checked against a collection simulated from the extract by the data-model rules: 16 records; first `[1,"Region","COURTS_REGION"]` and last `[16,"Geographic Area","CORONERS_AREA"]`; ended `[7,14]`; "Region" at `[[1,30],[6,84],[14,86]]`; 13 distinct names; 0 unresolved or disagreeing `type_id` values in both the checked-in `locations.json` and `base_locations.json`. The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.
