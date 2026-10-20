# Implementation Plan: Reference Data API — Contract Types

**Branch**: `004-contract-types-reference-data` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-contract-types-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves ContractType through the generic reference-data routes 002 built, as `contract_types` only: the deprecated alias `contract_type` is deliberately not served (spec Clarifications; Constitution VII). As in 003, the type is one entry in `jo.reference-data.types` and one checked-in fixture of 14 synthetic records. The one main-code change is generic: a type can declare **boolean fields** (`boolean-fields`), from a closed set modelled by a new `BooleanField` enum that has one member, `salaried` (spec FR-017, FR-018; research R2, R3). It mirrors 003's reference fields: declared per type, validated strictly at start-up, and written in responses only for records that carry it, so appointment-title and base-location responses are byte-for-byte unchanged.

**What changes**:
- **Data**: `contract_types.json`, 14 synthetic records generated once from the public names and the `salaried` flag in the local extract, using 002's generation rules (research R4; data-model.md § 3).
- **Main code**: generic boolean fields — the `BooleanField` enum, a `boolean-fields` declaration per type (`ReferenceDataProperties`, `ReferenceDataTypeRegistry`, `ReferenceDataType`), a `Boolean salaried` property on `ReferenceDataRecord` and `ReferenceDataResponse`, and strict start-up validation in `FixtureReferenceDataRepository` (research R2, R3). No route, authentication or error-handling change.
- **Tests**: a new functional test class and golden file for the type, the fixture-rule test extended to the third fixture and to boolean fields, a smoke latency guard for the new collection, the type guard widened to `contract_type`, unit tests for the new declaration and validation, and updates to the tests whose expectations list the supported names or the response schema (research R5–R8).
- **Documentation and examples**: a "Contract Types" Postman sub-folder, README and CLAUDE.md updates (including "Adding a reference-data type"), and a contract delta for this feature (research R9).

## Technical Context

**Language/Version**: Java 25 (Gradle toolchain), per Principle II. Unchanged.

**Build Tool**: Gradle via the wrapper. No dependency changes, so `gradle.lockfile` is untouched.

**Primary Dependencies**: None added. Spring Boot 4.1.1, springdoc-openapi, Jackson 3, Lombok and MapStruct, as 002 and 003.

**Storage**: No database. One more classpath JSON fixture, loaded into immutable in-memory maps at start-up by the existing repository.

**Testing**: JUnit 5, AssertJ, MockMvc and real HTTP in the four existing suites (`test`, `integrationTest`, `functionalTest`, `smokeTest`). Postman via newman.

**Target Platform**: JVM 25, executable Spring Boot JAR; CI on GitHub Actions as today.

**Project Type**: A single web service.

**Performance Goals**: 002's targets unchanged: collection p95 under 100 ms on a developer machine (`-Dperf.strict=true`), under 1 s on CI. The contract-type collection is about 2.5 KB of JSON, the smallest served (research R7).

**Constraints**:
- No type-specific code (FR-003, FR-016, AC-018/019). The only main-code change, FR-017's and FR-018's boolean fields, names no type.
- The extract is used once and never committed (FR-007).
- Existing types' responses are unchanged (FR-005, AC-015, SC-006).
- Every 002 behaviour applies to the new type unchanged (FR-004).

**Scale/Scope**: Three configured types (194 + 1,462 + 14 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `contract_types` is a contract-valid value the E-Links Swagger enumerates. Its deprecated alias `contract_type` is not served, which Principle I allows for deprecated aliases and which is not a contract deviation. Response shape is the contract's `ReferenceDataResponse`, plus `name` (agreed deviation D-1) and `salaried` (new deviation D-3, documented in the spec as permanent and scope-justified, as Governance requires). |
| II. Java Spring Boot Architecture | **Pass.** Package-by-layer kept: `BooleanField` sits in `entity/` beside `ReferenceField`; configuration, registry, repository, DTO and mapper changes stay in their layers. No new dependency. |
| III. Reuse Before Duplication | **Pass.** The type reuses every shared component. The registry's declaration parsing for reference fields is generalised so boolean fields reuse it rather than copying it (research R2). Functional tests reuse `ReferenceDataHttp`. |
| IV. Synthetic Data Only | **Pass.** Only the public contract-type names and the `salaried` flag come from the extract; ids, timestamps and dates are synthetic (data-model.md § 3). No forward reference is added. DF-2 records the backward reference the People/appointments feature must make resolve, with its closing test. |
| V. Deterministic Behaviour | **Pass.** Static fixture, fixed generation rules, ascending `id` order; a golden-file test covers the new collection (research R6). |
| VI. Behavioural Fidelity | **Pass.** No parameters added; query parameters, including `?salaried=true`, are still rejected. |
| VII. Generic Reference-Data Handling | **Pass, and demonstrated again.** The type is added through configuration. The spec states the alias is not served (`contract_type` → `400`); the entry has no `aliases`. The guard test is widened to `contract_type` (research R8). Boolean fields are declared per type and named in no type-specific code. |
| VIII. Typed API Models | **Pass.** One typed, optional `Boolean salaried` property on the record and DTO, modelled by a closed `BooleanField` enum; no generic map or free-form extra fields (research R2). MapStruct maps it by name. |
| IX. Centralised Validation and Error Handling | **Pass.** No new request-time error path. Start-up fixture and configuration validation stays in the registry and repository, with the same error style (research R3). |
| X. Configuration Over Hard-Coding | **Pass.** The type, its fixture and its boolean fields are configuration. |
| XI. Testability | **Pass (planned).** Unit (registry, repository, entity, mapper, controller, fixture rules, guard), integration (OpenAPI contract), functional (AC-001 to AC-017 over HTTP) and smoke (latency for the new collection) coverage (research R5–R8). |
| XII. Simplicity and Maintainability | **Pass.** One small enum and one property, following 003's reference-field pattern exactly; no general "extra fields" mechanism (research R2). |
| XIII. Automated Quality Gates | **Pass, inheriting 002's open Sonar deferral unchanged.** No gate added, removed or relaxed; the JaCoCo gate enforces FR-015. |
| XIV. Observability & Traceability | **Pass.** Correlation IDs and logging are path-generic and need no change. |
| XV. Security by Design | **Pass.** The new routes are under `/api/`, so the existing filter authenticates them before routing; AC-012 to AC-014 test it. |
| XVI. Object-Oriented Design Discipline | **Pass.** `BooleanField` owns its name and accessor, as `ReferenceField` does; no responsibility moves. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** "Reference Data › Contract Types" sub-folder with ten requests and tests; no hard-coded environment values (research R9). |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum, description and the `salaried` property are generated from configuration and the DTO; the contract test asserts them (research R5). A contract delta records the change. |

**Non-principle checks**:
- **003 artifacts that become partly stale**: 003's FR-002 lists the supported names, and README's "Adding a reference-data type" lists the declarable options. FR-002 of this spec supersedes 003's; README is updated (research R9). 003's spec is a record of that feature and is not rewritten.

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral, which this feature neither changes nor closes.

## Project Structure

### Documentation (this feature)

```text
specs/004-contract-types-reference-data/
├── plan.md                                        # This file
├── research.md                                    # Phase 0: decisions R1–R10
├── data-model.md                                  # Phase 1: the new type, boolean fields, fixture rules
├── quickstart.md                                  # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-contract-types.md       # Phase 1: contract delta against 002 + 003
├── checklists/
│   └── requirements.md                            # From /speckit-specify
└── tasks.md                                       # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
src/main/java/uk/gov/hmcts/ctam/jo/
├── entity/BooleanField.java                       # NEW: closed set of boolean fields (SALARIED) (R2)
├── entity/ReferenceDataRecord.java                # + Boolean salaried, nulls fail (R2, R3)
├── entity/ReferenceDataType.java                  # + Set<BooleanField> booleanFields (R2)
├── config/ReferenceDataProperties.java            # + boolean-fields per type (R2)
├── services/ReferenceDataTypeRegistry.java        # resolve boolean-fields; shared declared-field parsing (R2)
├── repository/FixtureReferenceDataRepository.java # strict boolean coercion; declared/undeclared checks (R3)
└── domain/ReferenceDataResponse.java              # + optional Boolean salaried after the reference fields (R2)

src/main/resources/
├── application.yml                                # + contract_types type entry (R1)
└── reference-data/contract_types.json             # NEW: 14 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── architecture/NoTypeSpecificCodeTest.java       # + contract_type fragment and spellings (R8)
├── repository/ReferenceDataFixtureRulesTest.java  # + contract_types.json; boolean-field rules (R4)
├── repository/FixtureReferenceDataRepositoryTest.java  # boolean-field validation cases (R3)
├── services/ReferenceDataTypeRegistryTest.java    # boolean-fields declaration cases (R3)
├── entity/ReferenceDataTypeTest.java              # boolean fields copied and ordered (R2)
├── mappers/ReferenceDataMapperTest.java           # salaried mapped; absent when null (R2)
└── controllers/ReferenceDataControllerTest.java   # salaried serialised only when present (R2)
src/test/resources/reference-data/                 # invalid fixtures for the boolean-field cases (R3)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                             # + CONTRACT_TYPES

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java          # enum, description, salaried property (R5)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java               # unsupported list: 18 names + contract-type case variants (R5)
└── ContractTypesFunctionalTest.java               # NEW: 004 AC-001 … AC-017 over real HTTP (R6)
src/functionalTest/resources/golden/
└── contract_types.json                            # NEW: captured collection response (AC-004)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                    # + CONTRACT_TYPES collection (R7)

postman/ctam-jomockapi.postman_collection.json     # + Reference Data › Contract Types (10 requests)
README.md                                          # § Reference data: three types; § Adding a type: boolean-fields
CLAUDE.md                                          # § Current repo state: + 004 line
specs/002-reference-data-api/contracts/reference-data-api.md  # one-line pointer to the 004 delta
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. All work is configuration, data, tests and documentation, plus the type-agnostic boolean-field mechanism (FR-017, FR-018), which follows 003's reference-field design class for class.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. Contract deviation D-3 (`salaried`) is documented in the spec as permanent and scope-justified. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-contract-types.md, quickstart.md).

The design added no dependency, layer or general-purpose abstraction: one enum with one member, one property on the record and the DTO, one declaration list, and their validation. Four design details strengthen the assessments:
- The closed `BooleanField` enum (R2) keeps the response typed and its OpenAPI schema exact, instead of a free-form map of extra fields — supports VIII and XVIII.
- Strict boolean coercion (R3) means `0`, `1`, `"true"` and `null` stop start-up instead of being silently converted — supports V and IX.
- The fixture-rule test checks `salaried` for every record against the generation rule (R4), so a hand edit fails the unit suite without the extract — supports IV and V.
- The widened guard (R8) turns "no type-specific code" for contract types into a build failure — supports VII.

Every row stays **Pass**. Open item: the inherited Sonar deferral.

## Implementation notes

**Baseline (T001, before any change)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 243, `integrationTest` 33, `functionalTest` 134, `smokeTest` 4. JaCoCo: lines 464/464 (100%), branches 215/216 (99.5%; the missed branch is MapStruct's generated null check on the boxed `id`, as recorded in 003).

**Final (T046)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 280 (+37: registry, repository, entity, mapper, controller, fixture-rule and guard cases), `integrationTest` 33 (the contract test's assertions extended), `functionalTest` 182 (+48: 47 in `ContractTypesFunctionalTest`, and 002's unsupported-name list lost `contract_types` and gained three case variants), `smokeTest` 5 (+1: the contract-types collection). JaCoCo: lines 496/496 (100%), branches 223/224 (99.6%; the one missed branch is still MapStruct's null check on the boxed `id`).

**Strict latency (T046, `./gradlew smokeTest -Dperf.strict=true`)**: collection p95 over 20 calls: `appointment_titles` 2 ms, `base_locations` 2 ms, `contract_types` 1 ms (bound 100 ms).

**AC-018 / SC-007 (T038)**: `git diff --numstat main -- src/main` lists only the boolean-field changes — `ReferenceDataProperties` (+5/−2), `ReferenceDataResponse` (+11/−4), `BooleanField` (new, 41), `ReferenceDataRecord` (+8), `ReferenceDataType` (+15/−8), `FixtureReferenceDataRepository` (+27/−1), `ReferenceDataTypeRegistry` (+21/−8) — plus `application.yml` (+4) and the new `reference-data/contract_types.json`. No route, authentication or error-handling file changed, no main source names a type, and the widened `NoTypeSpecificCodeTest` passes.

**AC coverage (T039)**: AC-020 (OpenAPI) is asserted by `ReferenceDataOpenApiContractTest` (T017); AC-021 (start-up validation) by `ReferenceDataTypeRegistryTest` and `FixtureReferenceDataRepositoryTest` (T009, T010).

**Golden file (T026)**: `golden/contract_types.json` was captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline) and equals the fixture's records. `golden/appointment_titles.json` and `golden/base_locations.json` are unchanged (AC-015).

**Postman (T041)**: newman ran 33 requests (10 new in Reference Data › Contract Types) with 121 assertions (+39), 0 failures. The folder was inserted as text so the existing collection's formatting is untouched.

**Quickstart (T047)**: §§ 1–4 and 6 walked through against `./gradlew bootRun`; every expected result matched. § 5 (start-up validation by hand) was not repeated by hand: the same failures are asserted by the unit tests in T009–T010.

**Differences from the planned design**:
- `ReferenceDataType` copies both field sets with one private generic helper, and the registry parses both declarations with one generic `declaredFields` helper; reference-field error messages are unchanged.
- `ReferenceDataFixtureRulesTest` was first given a separate `elevenContractTypesAreSalaried` test; after code review the expected count moved into the `Fixture` table (see below).
- Test support gained `salariedResponse` and `fullResponse` in `ReferenceDataFixtures`, and the OpenAPI test a `typesOf` helper that accepts `type` as a string or an array.

**Code-review follow-ups (2026-10-20)**: one `/code-review` round found no correctness bugs and six quality findings; five were fixed and one deferred.
- A shared `DeclaredField<T>` interface (`entity/`) now gives `ReferenceField` and `BooleanField` their `fieldName()`/`readFrom()` contract and one `byName` lookup; the enums' copied `fromFieldName` methods are gone, and the registry resolves both declarations through it.
- `FixtureReferenceDataRepository` checks both kinds with one generic `validateDeclaredFields` (required when declared, absent otherwise, plus an optional extra rule: `> 0` for reference fields). Error messages are unchanged.
- `ReferenceDataFixtureRulesTest`: one generic `inResponseOrder` helper; `ReferencePoint` has one factory (`point`) and `withReferences`/`withBooleans`; reference points are always compared with every declared field, so a point that omits a declared value fails instead of being skipped; the expected `true` count per boolean field is part of the `Fixture` table (`contract_types.json`: `salaried` 11), replacing `elevenContractTypesAreSalaried`.
- **Deferred**: replacing `ReferenceDataResponse`'s positional constructor with a builder, so a new optional field touches fewer files. It's a refactor beyond this feature's scope; worth doing in its own change or when the next type adds a field.
- `./gradlew check -PskipOwasp` green after the fixes. Tests per suite: `test` 279 (−1: the separate count test), `integrationTest` 33, `functionalTest` 182, `smokeTest` 5. JaCoCo: lines 492/492 (100%), branches 217/218 (99.5%; the missed branch is MapStruct's null check on the boxed `id`). Totals fell because duplicated code was removed.

**PR review follow-up (2026-10-20)**: `validateBooleanFields` passed `value -> true, null`, leaving the `rule` message a dead parameter that would read "salaried null" if a real boolean check were later added without a message. `validateDeclaredFields` now has a presence-only overload, which boolean fields use, and an overload with a further rule, which reference fields use. That overload rejects a null predicate or message with `Objects.requireNonNull`. Error messages are unchanged. `./gradlew check -PskipOwasp` green: `test` 279, `integrationTest` 33, `functionalTest` 182, `smokeTest` 5; JaCoCo lines 497/497, branches 221/222.
