# Implementation Plan: Reference Data API — Locations

**Branch**: `007-locations-reference-data` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-locations-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves Location through the generic reference-data routes, as `locations` only. The singular name `location` is not served (spec FR-001; Constitution VII). Like BaseLocation, its records carry `type_id`, `parent_id` and `jurisdiction_id`, and names may repeat. Unlike every earlier type:
- **Ids**: it keeps the extract's own ids (DR-1), because BaseLocation's `parent_id` values and its own refer to them (Constitution IV, 003 DF-1).
- **Null parents**: 142 top-level records have no parent, so they are served with `"parent_id": null`.

Serving that null needs **one generic main-code change**, a per-type `nullable-reference-fields` declaration, carried through:
- start-up validation, where null is accepted only for declared-nullable fields;
- the response, where a three-state reference value is written as absent, null or an id;
- the mapper, which now knows the type;
- the OpenAPI schema, where `parent_id` becomes nullable.

No code names a type, and the other five types' responses stay byte-for-byte unchanged. A table-driven test checks that every Location and BaseLocation `parent_id` resolves, which closes 003's DF-1 for `parent_id` (spec FR-012, tests only per Clarifications).

**What changes**:
- **Data**: `locations.json`, 2,000 records generated once from the local extract. Ids, names and the three reference codes are copied, and dates follow 002's rule by ascending-id position (285 ended). The generating script proves the copied columns equal the extract before the file is checked in (research R5; data-model.md § 4–5).
- **Main code** (generic):
  - `nullable-reference-fields` in configuration, registry and type (R2);
  - explicit-null handling in fixture loading (R3);
  - a `ReferenceFieldValue` response type, with the mapper taking the type (R4);
  - one reworded comment (R8).
- **Tests**:
  - new `LocationsFunctionalTest` with its golden file, and new `ReferenceDataReferencesTest`;
  - fixture-rules test with a per-fixture id rule;
  - nullable-field unit cases in the registry, repository and mapper tests;
  - the guard widened to `location`;
  - OpenAPI contract assertions, the unsupported-name list, and a smoke guard (R6–R10).
- **Documentation and examples**: a "Locations" Postman sub-folder; README (six types, and the new option); CLAUDE.md; the contract delta; a pointer from 002's contract; a DF-1 note in 003's spec (R10).

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi, Jackson 3 (with Jackson 2 annotations), Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: classpath JSON fixtures loaded at start-up. One more: `locations.json`, about 534 KB as stored and about 414 KB as a compact response.

**Testing**: JUnit 5 and AssertJ in the `test`, `integrationTest`, `functionalTest` and `smokeTest` suites; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: Collection p95 within the smoke suite's default 1 s bound, and within 100 ms when run with `-Dperf.strict=true`. This becomes the largest collection; base locations' strict p95 was 4 ms in 006.

**Constraints**:
- No code names a reference-data type (FR-003, FR-018).
- The five existing types' responses are byte-for-byte unchanged (FR-005, FR-011, AC-018).
- The extract is used once and never committed (FR-007).
- Names and the copied codes are reproduced exactly (FR-008).
- References between served types resolve (FR-012).

**Scale/Scope**: Six configured types (194 + 1,462 + 14 + 4 + 164 + 2,000 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `locations` is a contract-valid value. Its singular form is not served, which Principle I allows. The record shape is the contract's `ReferenceDataResponse` plus `name` (D-1) and the reference fields (D-2, extended to a nullable `parent_id`). DR-1 concerns id values, which the contract doesn't define, so it isn't a contract deviation. |
| II. Java Spring Boot Architecture | **Pass.** Changes stay in their layers: `config/` (properties), `services/` (registry), `entity/` (type, record), `repository/` (validation), `domain/` (`ReferenceFieldValue`, response), `mappers/` (mapper). Controllers are untouched. |
| III. Reuse Before Duplication | **Pass.** The nullable list reuses `declaredFields` and the existing declared-field validation. The new tests reuse `ReferenceDataHttp` and the fixture-rules table. |
| IV. Synthetic Data Only | **Pass.** Only public location names, ids and reference codes come from the extract, and dates are synthetic. The references between served types (Location → Location, BaseLocation → Location) resolve, verified by `ReferenceDataReferencesTest`, which closes 003 DF-1 for `parent_id`. The forward references (`type_id`, `jurisdiction_id`) are recorded as DF-5, copied unchanged, and the completeness check in R7 makes the closing tests unavoidable. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in fixed order; the golden file and repeated-call test check it. |
| VII. Generic Reference-Data Handling | **Pass.** The only behavioural code change is a generic option any type can declare. `location` is not served and has no `aliases` entry. The guard is widened to `location`, with no exemptions (R8). |
| VIII. Typed API Models | **Pass.** `ReferenceFieldValue` is a typed record, so there is no `Map` and no untyped JSON. The OpenAPI schema stays an `int64` integer (R4). |
| IX. Centralised Validation and Error Handling | **Pass.** Fixture problems still fail at start-up through the repository's single `invalid(…)` path. API errors are unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** Nullability is declared in `application.yml`, not coded per type. |
| XI. Testability | **Pass (planned).** Unit tests: registry, repository, mapper, serialisation, fixture rules, references, guard. Integration: the OpenAPI contract. Functional: AC-001 to AC-020 over HTTP. Smoke: latency (research R6–R10). |
| XII. Simplicity and Maintainability | **Pass.** The one generic addition is the smallest that expresses "may be null" (research R2–R4 record the rejected, larger alternatives). |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged. The new branches (nullable versus not, declared versus not) are covered by unit cases. Inherits 002's open Sonar deferral. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** A "Reference Data › Locations" sub-folder with ten requests and tests, with no hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration, and `parent_id` nullability comes from the DTO annotation. The contract test asserts both, and a contract delta records the change. |

Principles VI, XIV, XV and XVI: no change from 002. No filter, logging, security or class-design rule is affected.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: met by this type alone: 285 ended, and ids with gaps.
- **Earlier artifacts that become partly stale**: 006's FR-002 lists the supported names; this spec's FR-002 supersedes it. 003's DF-1 is closed for `parent_id`, and its spec gets a one-line note (R10). No earlier quickstart, test or Postman request uses `locations` as its unsupported example; the only one, `jurisdictions`, is unaffected.

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/007-locations-reference-data/
├── plan.md                                     # This file
├── research.md                                 # Phase 0: decisions R1–R11
├── data-model.md                               # Phase 1: the new type, nullable reference fields, fixture rules
├── quickstart.md                               # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-locations.md         # Phase 1: contract delta against 002–006
├── checklists/
│   └── requirements.md                         # From /speckit-specify
└── tasks.md                                    # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/uk/gov/hmcts/ctam/jo/
├── config/ReferenceDataProperties.java         # + nullableReferenceFields (R2)
├── services/ReferenceDataTypeRegistry.java     # resolve and validate nullable-reference-fields (R2)
├── services/ReferenceDataService.java          # pass the type to the mapper (R4)
├── entity/ReferenceDataType.java               # + nullableReferenceFields; "fixture location" → "fixture path" (R2, R8)
├── entity/ReferenceDataRecord.java             # reference fields accept an explicit null for validation (R3)
├── repository/FixtureReferenceDataRepository.java  # key-presence and null rules per declaration (R3)
├── domain/ReferenceFieldValue.java             # NEW: @JsonValue Long (R4)
├── domain/ReferenceDataResponse.java           # reference components → ReferenceFieldValue; parent_id nullable in schema (R4)
└── mappers/ReferenceDataMapper.java            # @Context ReferenceDataType (R4)

src/main/resources/
├── application.yml                             # + locations type entry (R1)
└── reference-data/locations.json               # NEW: 2,000 records (data-model.md § 4)

src/test/java/uk/gov/hmcts/ctam/jo/
├── architecture/NoTypeSpecificCodeTest.java    # + location fragment and spellings (R8)
├── repository/ReferenceDataFixtureRulesTest.java   # IdRule; nullable declarations; locations row (R6)
├── repository/ReferenceDataReferencesTest.java # NEW: references resolve; completeness (R7)
├── repository/FixtureReferenceDataRepositoryTest.java  # nullable cases; new TypeProperties argument (R9)
├── services/ReferenceDataTypeRegistryTest.java # nullable declaration cases (R9)
├── services/ReferenceDataServiceTest.java      # mapper call with the type (R9)
├── entity/ReferenceDataTypeTest.java           # nullable set copy (R9)
├── mappers/ReferenceDataMapperTest.java        # three-state mapping (R9)
├── domain/ReferenceFieldValueTest.java         # NEW: serialises as absent / null / number (R4)
├── controllers/ReferenceDataControllerTest.java    # response helpers (R9)
└── testsupport/ReferenceDataFixtures.java      # helpers for ReferenceFieldValue and TypeProperties (R9)
src/test/resources/reference-data/              # NEW valid/invalid nullable fixtures (R9)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                          # + LOCATIONS

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java       # enum, description, parent_id nullable, no extra schema (R9)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java            # unsupported list: 15 names + locations variants (R9)
└── LocationsFunctionalTest.java                # NEW: 007 AC-001 … AC-020 over real HTTP (R10)
src/functionalTest/resources/golden/
└── locations.json                              # NEW: captured collection response (AC-005)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                 # + LOCATIONS (R10)

postman/ctam-jomockapi.postman_collection.json  # + Reference Data › Locations (10 requests)
README.md                                       # § Reference data: six types; § Adding a type: nullable-reference-fields
CLAUDE.md                                       # § Current repo state: + 007; § Reference data: location values
specs/002-reference-data-api/contracts/reference-data-api.md   # one-line pointer to the 007 delta
specs/003-base-locations-reference-data/spec.md                # DF-1: note that 007 closed it for parent_id
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. The one new main class, `ReferenceFieldValue`, belongs in `domain/` with the response DTO it serves.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-locations.md, quickstart.md).

The design adds one generic option, one small typed value class and one new mapper parameter, with no new dependency, layer or endpoint. Four details strengthen the assessments:
- **Byte-identical responses (V, VII)**: R4's three-state value keeps the existing types' responses byte-identical (absent stays absent), and the unchanged golden files prove it end to end.
- **Reference checks (IV)**: `ReferenceDataReferencesTest`'s completeness check makes "the feature that serves the target type MUST add a test" a build failure for `location_types` and `jurisdictions`, so DF-1 and DF-5 can't be forgotten.
- **Two id rules (V, XI)**: the per-fixture `IdRule` keeps 002's rule enforced for five fixtures while stating DR-1's rule precisely for the sixth, rather than weakening the check for all.
- **Generic explicit-null rule (IX, XII)**: the explicit-null rule (R3) keeps the existing failure for undeclared and non-nullable fields, with a clearer message, so no protection 003 relied on is lost.

The quickstart's expected values were checked against a response simulated from the extract by the data-model rules: 2,000 records; first and last; 285 ended, from 9 to 2364; 142 null parents with the key always present; 0 unresolved parents; 40 "National"; about 414 KB compact. The simulation also confirmed that every `parent_id` in the checked-in `base_locations.json` (408 distinct) is a location id. The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.
