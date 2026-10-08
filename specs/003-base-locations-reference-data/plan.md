# Implementation Plan: Reference Data API — Base Locations

**Branch**: `003-base-locations-reference-data` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-base-locations-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves BaseLocation through the generic reference-data routes 002 built, as `base_locations` with the deprecated alias `base_location`. It is the first real use of 002's extension design: one entry in `jo.reference-data.types` and one checked-in fixture, with **no change under `src/main/java`** (research R1).

**What changes**:
- **Data**: `base_locations.json`, 1,276 synthetic records generated once from the distinct public names in the local extract, using 002's generation rules (research R2, data-model.md § 3).
- **Tests**: a new functional test class for the type, a fixture-rule test for both fixtures, a smoke latency guard per collection, a wider type-specific-code guard, and updates to the three tests whose expectations assume a single supported type (research R4–R8).
- **Documentation and examples**: a "Base Locations" Postman sub-folder, README and CLAUDE.md updates, and a pointer from 002's contract to this feature's contract delta (research R9).

## Technical Context

**Language/Version**: Java 25 (Gradle toolchain), per Principle II. Unchanged.

**Build Tool**: Gradle 9.7.1 via the wrapper. No dependency changes, so `gradle.lockfile` is untouched.

**Primary Dependencies**: None added. Spring Boot 4.1.1, springdoc-openapi 3.1.1, Jackson 3, Lombok and MapStruct, as 002.

**Storage**: No database. One more classpath JSON fixture, loaded into immutable in-memory maps at start-up by the existing repository.

**Testing**: JUnit 5, AssertJ, MockMvc and real HTTP in the four existing suites (`test`, `integrationTest`, `functionalTest`, `smokeTest`). Postman via newman.

**Target Platform**: JVM 25, executable Spring Boot JAR; CI on GitHub Actions as today.

**Project Type**: A single web service.

**Performance Goals**: 002's targets unchanged: collection p95 under 100 ms on a developer machine (`-Dperf.strict=true`), under 1 s on CI. The base-location collection is about 203 KB of JSON against 32 KB today; the estimate is about 17 ms per call (research R7).

**Constraints**:
- No type-specific code (FR-003, AC-017/018).
- The extract is used once and never committed (FR-007).
- Names are carried over exactly, including the en dash (FR-010).
- Every 002 behaviour applies to the new type unchanged (FR-004).

**Scale/Scope**: Two configured types (194 + 1,276 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `base_locations` and `base_location` are contract-valid values the E-Links Swagger enumerates. Response shape is the contract's `ReferenceDataResponse`, plus `name` under the existing agreed deviation D-1, which 002 recorded as applying to the shared record shape. No new deviation. |
| II. Java Spring Boot Architecture | **Pass.** No architectural change; no main Java file changes. |
| III. Reuse Before Duplication | **Pass.** The type reuses every shared component. In tests, the functional HTTP helpers move into a shared helper instead of being copied (research R6). |
| IV. Synthetic Data Only | **Pass.** Only public location names come from the extract; ids, timestamps and dates are synthetic (data-model.md § 3). The names were scanned for personal data (spec Assumptions). The extract's foreign-key columns are deliberately not served, because the records they reference don't exist in the mock (DF-1); serving them would break the coherence this principle requires. |
| V. Deterministic Behaviour | **Pass.** Static fixture, fixed generation rules, ascending `id` order. A golden-file test covers the new collection (research R6). |
| VI. Behavioural Fidelity | **Pass.** No parameters added; query parameters still rejected. No pagination is added for the larger collection, because the contract defines none. |
| VII. Generic Reference-Data Handling | **Pass, and demonstrated.** The type is added through configuration only; the alias is resolved by the registry. The guard test is widened so type-specific code for base locations would fail the build (research R8). |
| VIII. Typed API Models | **Pass.** Existing DTOs; no new fields. |
| IX. Centralised Validation and Error Handling | **Pass.** No new error path; every error on the new type comes from the shared handler with 002's messages. |
| X. Configuration Over Hard-Coding | **Pass.** The type and its fixture location are configuration. |
| XI. Testability | **Pass (planned).** Unit (fixture-rule test, guard), integration (OpenAPI contract), functional (AC-001 to AC-016 over HTTP) and smoke (latency per collection) coverage. |
| XII. Simplicity and Maintainability | **Pass.** No caching, pagination or new abstraction (research R7). |
| XIII. Automated Quality Gates | **Pass, inheriting 002's open Sonar deferral unchanged.** No gate is added, removed or relaxed. **Dependency**: the JaCoCo coverage gate FR-016 refers to arrives with PR #9 (`test-coverage-improvements`), which isn't on `main` yet. If PR #9 merges first, rebase this branch onto it; if not, FR-016 is met by JaCoCo reporting alone, and the gate applies once PR #9 lands. |
| XIV. Observability & Traceability | **Pass.** Correlation IDs and logging are path-generic and need no change. |
| XV. Security by Design | **Pass.** The new routes are under `/api/`, so the existing filter authenticates them before routing; AC-012/013 test it. No security control is relaxed. |
| XVI. Object-Oriented Design Discipline | **Pass.** No production code changes. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** "Reference Data › Base Locations" sub-folder with six requests and tests; no hard-coded environment values (research R9). |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration; the contract test is updated to the new values (research R5). A contract delta document records the change. |

**Non-principle checks**:
- **002 artifacts that become partly stale**: 002's spec FR-003/FR-007 and contract "Allowed values" describe a single type. The spec is a record of that feature and is not rewritten; FR-002 of this spec supersedes it. The 002 contract gets a one-line pointer to this feature's delta, because contract tests and readers treat it as current (research R9).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral, which this feature neither changes nor closes.

## Project Structure

### Documentation (this feature)

```text
specs/003-base-locations-reference-data/
├── plan.md                                        # This file
├── research.md                                    # Phase 0: decisions R1–R9
├── data-model.md                                  # Phase 1: the new type, its records, fixture rules
├── quickstart.md                                  # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-base-locations.md       # Phase 1: contract delta against 002
├── checklists/
│   └── requirements.md                            # From /speckit-specify
└── tasks.md                                       # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
src/main/java/                                     # NO CHANGES (AC-017, SC-007)

src/main/resources/
├── application.yml                                # + base_locations type entry (research R1)
└── reference-data/base_locations.json             # NEW: 1,276 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── architecture/NoTypeSpecificCodeTest.java       # forbidden fragments: appointment, base_location (R8)
└── repository/ReferenceDataFixtureRulesTest.java  # NEW: generation rules for every shipped fixture (R4)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                             # + BASE_LOCATIONS

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataOpenApiContractTest.java          # enum and description for two types (R5)
└── ReferenceDataExtensionIntegrationTest.java     # expected unchanged; confirm (R5)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── testsupport/ReferenceDataHttp.java             # NEW: get/send/assertError/json shared by both classes (R6)
├── ReferenceDataFunctionalTest.java               # uses the helper; unsupported list now 18 names (R5)
└── BaseLocationsFunctionalTest.java               # NEW: 003 AC-001 … AC-016 over real HTTP (R6)
src/functionalTest/resources/golden/
└── base_locations.json                            # NEW: captured collection response (AC-004)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                    # p95 guard per collection path (R7)

postman/ctam-jomockapi.postman_collection.json     # + Reference Data › Base Locations (6 requests)
README.md                                          # § Reference data lists both types
CLAUDE.md                                          # § Current repo state: + 003 line
specs/002-reference-data-api/contracts/reference-data-api.md  # one-line pointer to the 003 delta
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. All work is configuration, data, tests and documentation; the only new test package is the functional suite's own `testsupport`, mirroring the shared `testFixtures` package of the same name.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviations. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-base-locations.md, quickstart.md).

The design added no dependency, layer, abstraction or production code. Three design details strengthen the assessments:
- The fixture-rule test (R4) makes the generation rules, and so id stability (FR-011), enforceable without the extract — supports IV, V and VII.
- The widened guard (R8) turns "no type-specific code" for the new type from a review item into a build failure — supports VII.
- A per-collection latency guard (R7) holds the larger response to the same bound instead of relaxing it — supports XI and XIII.

Every row stays **Pass**. Open items: the inherited Sonar deferral, and the PR #9 dependency for the coverage gate (XIII row).
