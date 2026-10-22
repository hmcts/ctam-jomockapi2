# Implementation Plan: Reference Data API — Genders

**Branch**: `005-genders-reference-data` | **Date**: 2026-10-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-genders-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves Gender through the generic reference-data routes, as `genders` only: the deprecated alias `gender` is deliberately not served (spec Clarifications; Constitution VII). Gender records have only the shared fields, so the type is one entry in `jo.reference-data.types` and one checked-in fixture of 4 synthetic records, with **no main Java change** (spec FR-003, SC-007). It is the first type to use 002's extension design with nothing added to it.

**What changes**:
- **Data**: `genders.json`, 4 synthetic records generated once from the public names in the local extract, using 002's generation rules unchanged. With 4 records no position is a multiple of 7, so none is ended (research R2; data-model.md § 3).
- **Main code**: none. One configuration entry in `application.yml`.
- **Tests**: a new functional test class and golden file, the fixture-rule table extended, a smoke latency guard, the type guard widened to `gender`, and updates to the tests and Postman request that used `genders` as their example of an unsupported type (research R3–R6).
- **Documentation and examples**: a "Genders" Postman sub-folder, README and CLAUDE.md updates, and a contract delta for this feature (research R7).

## Technical Context

**Language/Version**: Java 25 (Gradle toolchain), per Principle II. Unchanged.

**Build Tool**: Gradle via the wrapper. No dependency changes, so `gradle.lockfile` is untouched.

**Primary Dependencies**: None added. Spring Boot 4.1.1, springdoc-openapi, Jackson 3, Lombok and MapStruct, as before.

**Storage**: No database. One more classpath JSON fixture, loaded at start-up by the existing repository.

**Testing**: JUnit 5, AssertJ, MockMvc and real HTTP in the four existing suites (`test`, `integrationTest`, `functionalTest`, `smokeTest`). Postman via newman.

**Target Platform**: JVM 25, executable Spring Boot JAR; CI on GitHub Actions as today.

**Project Type**: A single web service.

**Performance Goals**: 002's targets unchanged: collection p95 under 100 ms on a developer machine (`-Dperf.strict=true`), under 1 s on CI. The gender collection is about 0.7 KB of JSON, the smallest served.

**Constraints**:
- No main Java change (FR-003, AC-017, SC-007).
- The extract is used once and never committed (FR-007).
- Existing types' responses are unchanged (FR-005, AC-014, SC-006).
- Every 002 behaviour applies to the new type unchanged (FR-004).

**Scale/Scope**: Four configured types (194 + 1,462 + 14 + 4 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `genders` is a contract-valid value the E-Links Swagger enumerates. Its deprecated alias `gender` is not served, which Principle I allows and which is not a contract deviation. Response shape is the contract's `ReferenceDataResponse` plus `name` under the agreed deviation D-1. No new deviation. |
| II. Java Spring Boot Architecture | **Pass.** No code or layer change. |
| III. Reuse Before Duplication | **Pass.** The type reuses every shared component; functional tests reuse `ReferenceDataHttp`. |
| IV. Synthetic Data Only | **Pass.** Only four generic category labels come from the extract; ids, timestamps and dates are synthetic (data-model.md § 3). No real person's gender is involved. DF-3 records the backward reference the People feature must make resolve, with its closing test. |
| V. Deterministic Behaviour | **Pass.** Static fixture, fixed generation rules, ascending `id` order; a golden-file test covers the new collection (research R4). |
| VI. Behavioural Fidelity | **Pass.** No parameters added; query parameters are still rejected. |
| VII. Generic Reference-Data Handling | **Pass, and demonstrated in its simplest form.** The type is added through configuration alone. The spec states the alias is not served (`gender` → `400`); the entry has no `aliases`. The guard test is widened to `gender` with no exemptions (spec Clarifications; research R5). |
| VIII. Typed API Models | **Pass.** No DTO change. |
| IX. Centralised Validation and Error Handling | **Pass.** No new error path; start-up fixture validation applies to the new fixture unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** The type and its fixture are configuration. |
| XI. Testability | **Pass (planned).** Unit (fixture rules, guard), integration (OpenAPI contract), functional (AC-001 to AC-016 over HTTP) and smoke (latency for the new collection) coverage (research R3–R6). |
| XII. Simplicity and Maintainability | **Pass.** Nothing added to main code. |
| XIII. Automated Quality Gates | **Pass, inheriting 002's open Sonar deferral unchanged.** No gate added, removed or relaxed. |
| XIV. Observability & Traceability | **Pass.** Correlation IDs and logging are path-generic. |
| XV. Security by Design | **Pass.** The new routes are under `/api/`, so the existing filter authenticates them before routing; AC-011 to AC-013 test it. |
| XVI. Object-Oriented Design Discipline | **Pass.** No code change. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** "Reference Data › Genders" sub-folder with nine requests and tests; the request that used `genders` as an unsupported example moves to `judiciary_roles` (research R7). No hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration; the contract test asserts them (research R3). A contract delta records the change. |

**Non-principle checks**:
- **002 FR-023 (at least one ended record)**: met. FR-023's "default dataset" is read as the shipped default data across all types (spec Clarifications; EC-004), and the other types include ended records, so Gender, with none, needs no waiver. 002 records this reading next to FR-023 (T032). FR-023 is a 002 dataset requirement, not a constitution MUST, and no existing test asserts it per fixture, so no test changes because of it (research R2).
- **Earlier artifacts that become partly stale**: 004's FR-002 lists the supported names; this spec's FR-002 supersedes it. 002's quickstart uses `genders` as its example of an unsupported type (§ 3 row 5); it is a record of that feature and gets a one-line note pointing at a still-unsupported name (research R7).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/005-genders-reference-data/
├── plan.md                                        # This file
├── research.md                                    # Phase 0: decisions R1–R8
├── data-model.md                                  # Phase 1: the new type and fixture rules
├── quickstart.md                                  # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-genders.md              # Phase 1: contract delta against 002–004
├── checklists/
│   └── requirements.md                            # From /speckit-specify
└── tasks.md                                       # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
src/main/resources/
├── application.yml                                # + genders type entry (R1)
└── reference-data/genders.json                    # NEW: 4 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── architecture/NoTypeSpecificCodeTest.java       # + gender fragment and spellings (R5)
└── repository/ReferenceDataFixtureRulesTest.java  # + genders.json row (R2)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                             # + GENDERS

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java          # enum and description (R3)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java               # unsupported list: 17 names + gender case variants; auth example → judiciary_roles (R3)
└── GendersFunctionalTest.java                     # NEW: 005 AC-001 … AC-016 over real HTTP (R4)
src/functionalTest/resources/golden/
└── genders.json                                   # NEW: captured collection response (AC-004)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                    # + GENDERS collection (R6)

postman/ctam-jomockapi.postman_collection.json     # + Reference Data › Genders (9 requests); unsupported example → judiciary_roles
README.md                                          # § Reference data: four types
CLAUDE.md                                          # § Current repo state: + 005 line; § Reference data: gender names
specs/002-reference-data-api/contracts/reference-data-api.md  # one-line pointer to the 005 delta
specs/002-reference-data-api/quickstart.md         # note on § 3 row 5: genders is now served
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. All work is configuration, data, tests and documentation; no main Java file changes.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-genders.md, quickstart.md).

The design adds no code, dependency, layer or abstraction. Three details strengthen the assessments:
- The fixture-rule table gains the gender fixture, so its generation rules and ids (FR-010) stay enforceable without the extract — supports IV and V.
- The widened guard turns "no type-specific code" for genders into a build failure, with no exemptions (spec Clarifications) — supports VII.
- The two existing checks that used `genders` as their unsupported example move to `judiciary_roles`, so they keep testing what they were written to test rather than silently changing meaning — supports XI and XVII.

Every row stays **Pass**. Open item: the inherited Sonar deferral.
