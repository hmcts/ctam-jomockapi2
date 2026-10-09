# Implementation Plan: Reference Data API — Base Locations

**Branch**: `003-base-locations-reference-data` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-base-locations-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves BaseLocation through the generic reference-data routes 002 built, as `base_locations` only: the deprecated alias `base_location` is deliberately not served (spec Clarifications; Constitution VII, v1.11.0). It is the first real use of 002's extension design: one entry in `jo.reference-data.types` and one checked-in fixture. The only main-code change is generic: fixture validation stops rejecting repeated names, because the extract has 120 names shared by different real locations and the spec serves one record per source row (spec FR-017; research R10).

**What changes**:
- **Data**: `base_locations.json`, 1,462 synthetic records (one per source row, 1,276 distinct names) generated once from the public names in the local extract, using 002's generation rules (research R2, data-model.md § 3).
- **Main code**: remove the duplicate-name check from `FixtureReferenceDataRepository.load` (one `if` and its `Set`), type-agnostic (research R10).
- **Tests**: a new functional test class for the type, a fixture-rule test for both fixtures, a smoke latency guard per collection, a wider type-specific-code guard, updates to the three tests whose expectations assume a single supported type, and the repository unit test that asserts the duplicate-name check (research R4–R8, R10).
- **Documentation and examples**: a "Base Locations" Postman sub-folder, README and CLAUDE.md updates (including README's fixture rules, which say names are unique), and pointers from 002's contract and data model to this feature's changes (research R9, R10).

## Technical Context

**Language/Version**: Java 25 (Gradle toolchain), per Principle II. Unchanged.

**Build Tool**: Gradle 9.7.1 via the wrapper. No dependency changes, so `gradle.lockfile` is untouched.

**Primary Dependencies**: None added. Spring Boot 4.1.1, springdoc-openapi 3.1.1, Jackson 3, Lombok and MapStruct, as 002.

**Storage**: No database. One more classpath JSON fixture, loaded into immutable in-memory maps at start-up by the existing repository.

**Testing**: JUnit 5, AssertJ, MockMvc and real HTTP in the four existing suites (`test`, `integrationTest`, `functionalTest`, `smokeTest`). Postman via newman.

**Target Platform**: JVM 25, executable Spring Boot JAR; CI on GitHub Actions as today.

**Project Type**: A single web service.

**Performance Goals**: 002's targets unchanged: collection p95 under 100 ms on a developer machine (`-Dperf.strict=true`), under 1 s on CI. The base-location collection is about 226 KB of JSON against 32 KB today; the estimate is about 19 ms per call (research R7).

**Constraints**:
- No type-specific code (FR-003, AC-017/018). The one main-code change, FR-017, names no type.
- The extract is used once and never committed (FR-007).
- Names are carried over exactly, including the en dash (FR-010).
- Every 002 behaviour applies to the new type unchanged (FR-004).

**Scale/Scope**: Two configured types (194 + 1,462 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `base_locations` is a contract-valid value the E-Links Swagger enumerates. Its deprecated alias `base_location` is not served, which Principle I (v1.11.0) allows for deprecated aliases and which is not a contract deviation. Response shape is the contract's `ReferenceDataResponse`, plus `name` under the existing agreed deviation D-1, which 002 recorded as applying to the shared record shape. No new deviation. |
| II. Java Spring Boot Architecture | **Pass.** No architectural change. One existing repository method loses a check; no layer, class or dependency is added. |
| III. Reuse Before Duplication | **Pass.** The type reuses every shared component. In tests, the functional HTTP helpers move into a shared helper instead of being copied (research R6). |
| IV. Synthetic Data Only | **Pass.** Only public location names come from the extract (one record per source row, so a name shared by several real locations appears on several records); ids, timestamps and dates are synthetic (data-model.md § 3). The names were scanned for personal data (spec Assumptions). The extract's foreign-key columns are deliberately not served, because the records they reference don't exist in the mock (DF-1); serving them would break the coherence this principle requires. |
| V. Deterministic Behaviour | **Pass.** Static fixture, fixed generation rules, ascending `id` order. Rows sharing a name are indistinguishable apart from their position, so their relative order can't change the output (data-model.md § 3). A golden-file test covers the new collection (research R6). |
| VI. Behavioural Fidelity | **Pass.** No parameters added; query parameters still rejected. No pagination is added for the larger collection, because the contract defines none. |
| VII. Generic Reference-Data Handling | **Pass, and demonstrated.** The type is added through configuration only. The spec states that no alias is served (`base_location` → `400`), as VII requires; the entry has no `aliases`, and `appointment_title` is still resolved centrally by the registry. The guard test is widened so type-specific code for base locations would fail the build (research R8). The validation change (R10) applies to every type alike. |
| VIII. Typed API Models | **Pass.** Existing DTOs; no new fields. |
| IX. Centralised Validation and Error Handling | **Pass.** No new error path; every error on the new type comes from the shared handler with 002's messages. Start-up fixture validation stays in one place and loses only the duplicate-name rule (R10). |
| X. Configuration Over Hard-Coding | **Pass.** The type and its fixture location are configuration. |
| XI. Testability | **Pass (planned).** Unit (fixture-rule test, guard, repository test accepting repeated names), integration (OpenAPI contract), functional (AC-001 to AC-016 over HTTP) and smoke (latency per collection) coverage. |
| XII. Simplicity and Maintainability | **Pass.** No caching, pagination or new abstraction (research R7). |
| XIII. Automated Quality Gates | **Pass, inheriting 002's open Sonar deferral unchanged.** No gate is added, removed or relaxed. The JaCoCo coverage gate (PR #9) is already on this branch's base, so FR-016 is enforced by `check`. |
| XIV. Observability & Traceability | **Pass.** Correlation IDs and logging are path-generic and need no change. |
| XV. Security by Design | **Pass.** The new routes are under `/api/`, so the existing filter authenticates them before routing; AC-012/013 test it. No security control is relaxed. |
| XVI. Object-Oriented Design Discipline | **Pass.** The only production change removes code; no new responsibility or collaborator. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** "Reference Data › Base Locations" sub-folder with five requests and tests; no hard-coded environment values (research R9). |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration; the contract test is updated to the new values (research R5). A contract delta document records the change. |

**Non-principle checks**:
- **002 artifacts that become partly stale**: 002's spec FR-003/FR-007 and contract "Allowed values" describe a single type, and 002's data-model § 2 says `name` is unique within the type. The spec is a record of that feature and is not rewritten; FR-002 and FR-017 of this spec supersede it. The 002 contract and data model each get a one-line pointer to this feature, because tests, README and readers treat them as current (research R9, R10).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral, which this feature neither changes nor closes.

## Project Structure

### Documentation (this feature)

```text
specs/003-base-locations-reference-data/
├── plan.md                                        # This file
├── research.md                                    # Phase 0: decisions R1–R10
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
src/main/java/uk/gov/hmcts/ctam/jo/repository/
└── FixtureReferenceDataRepository.java            # remove the duplicate-name check only (FR-017, R10)

src/main/resources/
├── application.yml                                # + base_locations type entry (research R1)
└── reference-data/base_locations.json             # NEW: 1,462 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── architecture/NoTypeSpecificCodeTest.java       # forbidden fragments: appointment, base_location (R8)
├── repository/ReferenceDataFixtureRulesTest.java  # NEW: generation rules for every shipped fixture (R4)
└── repository/FixtureReferenceDataRepositoryTest.java  # name-duplicate case: now loads instead of failing (R10)
src/test/resources/reference-data/valid-repeated-names.json  # renamed from invalid/name-duplicate.json (R10)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                             # + BASE_LOCATIONS

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataOpenApiContractTest.java          # enum and description for two types (R5)
└── ReferenceDataExtensionIntegrationTest.java     # expected unchanged; confirm (R5)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── testsupport/ReferenceDataHttp.java             # NEW: get/send/assertError/json shared by both classes (R6)
├── ReferenceDataFunctionalTest.java               # uses the helper; unsupported list now 19 names, base_location stays (R5)
└── BaseLocationsFunctionalTest.java               # NEW: 003 AC-001 … AC-016 over real HTTP (R6)
src/functionalTest/resources/golden/
└── base_locations.json                            # NEW: captured collection response (AC-004)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                    # p95 guard per collection path (R7)

postman/ctam-jomockapi.postman_collection.json     # + Reference Data › Base Locations (5 requests)
README.md                                          # § Reference data lists both types; fixture rules drop "unique names"
CLAUDE.md                                          # § Current repo state: + 003 line
specs/002-reference-data-api/contracts/reference-data-api.md  # one-line pointer to the 003 delta
specs/002-reference-data-api/data-model.md         # one-line pointer: name uniqueness superseded by 003 FR-017
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. All work is configuration, data, tests and documentation, plus one type-agnostic deletion in the repository; the only new test package is the functional suite's own `testsupport`, mirroring the shared `testFixtures` package of the same name.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviations. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-base-locations.md, quickstart.md).

The design added no dependency, layer or abstraction, and its only production change deletes a validation rule (R10). Four design details strengthen the assessments:
- The fixture-rule test (R4) makes the generation rules, and so id stability (FR-011), enforceable without the extract — supports IV, V and VII.
- The widened guard (R8) turns "no type-specific code" for the new type from a review item into a build failure — supports VII.
- A per-collection latency guard (R7) holds the larger response to the same bound instead of relaxing it — supports XI and XIII.
- The repository test keeps a fixture with a repeated name and asserts it loads, so FR-017 can't be reverted silently — supports XI.

Every row stays **Pass**. Open item: the inherited Sonar deferral.
