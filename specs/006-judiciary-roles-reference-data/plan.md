# Implementation Plan: Reference Data API — Judiciary Roles

**Branch**: `006-judiciary-roles-reference-data` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-judiciary-roles-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves JudiciaryRole through the generic reference-data routes, as `judiciary_roles` only: the singular name `judiciary_role` is not served, since deprecated names are out of scope (spec FR-001; Constitution VII). Like Gender, its records have only the shared fields, so the type is one entry in `jo.reference-data.types` and one checked-in fixture of 164 synthetic records, with **no main Java change** (spec FR-003, SC-008). What is new is the data: the first served names containing `’`, mixed with ASCII apostrophes, en dashes and hyphens, and a few apparent typos and truncations, all served exactly as the extract has them.

**What changes**:
- **Data**: `judiciary_roles.json`, 164 synthetic records generated once from the public names in the local extract, using 002's generation rules unchanged (23 ended). The generating script proves the names equal the extract's as code points before the file is checked in (research R2; data-model.md § 3–4).
- **Main code**: none. One configuration entry in `application.yml`.
- **Tests**: a new functional test class and golden file, the fixture-rule table extended plus one generic "no `\u` escapes" check for every fixture, a smoke latency guard, the type guard widened to `judiciary_role`, and updates to the tests and Postman request that used `judiciary_roles` as their example of an unsupported type (research R3–R7).
- **Documentation and examples**: a "Judiciary Roles" Postman sub-folder, README and CLAUDE.md updates, a contract delta, and two earlier quickstart rows moved to `jurisdictions` (research R8).

## Technical Context

Unchanged from 002's plan: no new language, dependency, storage or platform. `gradle.lockfile` is untouched. This feature changes only the following.

**Data**: one more classpath JSON fixture, `judiciary_roles.json` (164 records, about 34 KB as stored, about 27 KB as a compact response), loaded at start-up by the existing repository. Names are UTF-8 with literal non-ASCII characters; the repository, start-up validation and Jackson already handle these (003's en dash).

**Constraints**:
- No main Java change (FR-003, AC-018, SC-008).
- The extract is used once and never committed (FR-007).
- Names are reproduced exactly: no normalisation or correction (FR-008).
- Existing types' responses are unchanged (FR-005, AC-015, SC-007).
- Every 002 behaviour applies to the new type unchanged (FR-004).

**Scale/Scope**: Five configured types (194 + 1,462 + 14 + 4 + 164 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `judiciary_roles` is a contract-valid value the E-Links Swagger enumerates. Its singular form is not served, which Principle I allows and which is not a contract deviation. Response shape is the contract's `ReferenceDataResponse` plus `name` under the agreed deviation D-1. No new deviation. |
| IV. Synthetic Data Only | **Pass.** Only public role titles come from the extract; ids, timestamps and dates are synthetic, and the extract's ids and end dates are not used (data-model.md § 3). No real office holder's roles are involved. DF-4 records the backward reference the People feature must make resolve, with its closing test. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in a fixed order; the golden file and the repeated-call test (AC-005) check it. |
| VII. Generic Reference-Data Handling | **Pass.** The type is added through configuration alone. The spec states the singular name is not served (`judiciary_role` → `400`); the entry has no `aliases`. The guard test is widened to `judiciary_role` with no exemptions (research R6). |
| XI. Testability | **Pass (planned).** Unit (fixture rules, literal-character check, guard), integration (OpenAPI contract), functional (AC-001 to AC-017 over HTTP) and smoke (latency for the new collection) coverage (research R3–R7). |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** "Reference Data › Judiciary Roles" sub-folder with nine requests and tests; the request that used `judiciary_roles` as an unsupported example moves to `jurisdictions` (research R8). No hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration; the contract test asserts them (research R4). A contract delta records the change. |

Principles II, III, VI, VIII–X and XII–XVI: no change from 002; this feature adds no main code. XIII inherits 002's open Sonar deferral unchanged.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: met by this type on its own: 23 ended records and multiples of 10.
- **Earlier artifacts that become partly stale**: 005's FR-002 lists the supported names; this spec's FR-002 supersedes it. 002's quickstart § 3 row 5 and 005's quickstart § 3 row 14 (and § 5) request `judiciary_roles` as their unsupported example; they change to `jurisdictions` so each still passes when run, as 005's code review did for 002's row (research R8).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/006-judiciary-roles-reference-data/
├── plan.md                                        # This file
├── research.md                                    # Phase 0: decisions R1–R9
├── data-model.md                                  # Phase 1: the new type, fixture rules and verification
├── quickstart.md                                  # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-judiciary-roles.md      # Phase 1: contract delta against 002–005
├── checklists/
│   └── requirements.md                            # From /speckit-specify
└── tasks.md                                       # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/resources/
├── application.yml                                # + judiciary_roles type entry (R1)
└── reference-data/judiciary_roles.json            # NEW: 164 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── architecture/NoTypeSpecificCodeTest.java       # + judiciary_role fragment and spellings (R6)
└── repository/ReferenceDataFixtureRulesTest.java  # + judiciary_roles.json row (R2); + no-\u-escape check for every fixture (R3)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                             # + JUDICIARY_ROLES

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java          # enum and description (R4)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java               # unsupported list: 16 names + judiciary_roles variants; auth example → jurisdictions (R4)
└── JudiciaryRolesFunctionalTest.java              # NEW: 006 AC-001 … AC-017 over real HTTP (R5)
src/functionalTest/resources/golden/
└── judiciary_roles.json                           # NEW: captured collection response (AC-005)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                    # + JUDICIARY_ROLES collection (R7)

postman/ctam-jomockapi.postman_collection.json     # + Reference Data › Judiciary Roles (9 requests); unsupported example → jurisdictions
README.md                                          # § Reference data: five types
CLAUDE.md                                          # § Current repo state: + 006 line; § Reference data: judiciary role names
specs/002-reference-data-api/contracts/reference-data-api.md  # one-line pointer to the 006 delta
specs/002-reference-data-api/quickstart.md         # § 3 row 5 → jurisdictions
specs/005-genders-reference-data/quickstart.md     # § 3 row 14 and § 5 → jurisdictions
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. All work is configuration, data, tests and documentation; no main Java file changes.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-judiciary-roles.md, quickstart.md).

The design adds no main code, dependency, layer or abstraction. Four details strengthen the assessments:
- The fixture-rule table gains the judiciary role fixture with 13 reference points, so its generation rules and ids (FR-010) stay enforceable without the extract — supports IV and V.
- The new generic "no `\u` escapes" check makes FR-008's "written literally" a build failure for every fixture, not a convention — supports V and XI, and applies 003's en-dash rule retroactively at no cost (every existing fixture already passes).
- The widened guard turns "no type-specific code" for judiciary roles into a build failure, with no exemptions (DF-4) — supports VII.
- The two existing checks that used `judiciary_roles` as their unsupported example move to `jurisdictions`, so they keep testing what they were written to test rather than silently changing meaning — supports XI and XVII.

The quickstart's expected values were checked against a response simulated from the extract by the data-model rules (164 records, first/last, 23 ended from 70 to 1610, the EC-008 ordering, the reference-point names, 141 null and 23 dated `end_date` values, about 27 KB compact). The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.
