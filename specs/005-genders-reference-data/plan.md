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

Unchanged from 002's plan: no new language, dependency, storage or platform. `gradle.lockfile` is untouched. This feature changes only the following.

**Data**: one more classpath JSON fixture, `genders.json` (4 records, about 0.7 KB of JSON, the smallest collection served), loaded at start-up by the existing repository.

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
| IV. Synthetic Data Only | **Pass.** Only four generic category labels come from the extract; ids, timestamps and dates are synthetic (data-model.md § 3). No real person's gender is involved. DF-3 records the backward reference the People feature must make resolve, with its closing test. |
| VII. Generic Reference-Data Handling | **Pass, and demonstrated in its simplest form.** The type is added through configuration alone. The spec states the alias is not served (`gender` → `400`); the entry has no `aliases`. The guard test is widened to `gender` with no exemptions (spec Clarifications; research R5). |
| XI. Testability | **Pass (planned).** Unit (fixture rules, guard), integration (OpenAPI contract), functional (AC-001 to AC-016 over HTTP) and smoke (latency for the new collection) coverage (research R3–R6). |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** "Reference Data › Genders" sub-folder with nine requests and tests; the request that used `genders` as an unsupported example moves to `judiciary_roles` (research R7). No hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration; the contract test asserts them (research R3). A contract delta records the change. |

Principles II, III, V, VI, VIII–X and XII–XVI: no change from 002; this feature adds no code. XIII inherits 002's open Sonar deferral unchanged.

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
└── tasks.md                                       # Phase 2: task list (/speckit-tasks)
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

Every principle stays **Pass**. Open item: the inherited Sonar deferral.

## Implementation notes

**Baseline (T001, before any change)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 279, `integrationTest` 33, `functionalTest` 182, `smokeTest` 5. JaCoCo: lines 497/497 (100%), branches 221/222 (99.5%; the missed branch is MapStruct's generated null check on the boxed `id`, as recorded in 003).

**Final (T033)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 295 (+16: one fixture-rule row, which adds two cases, plus the `gender` fragment, nine spelling cases and four non-matching cases in `NoTypeSpecificCodeTest`), `integrationTest` 33 (the contract test's `enum` and description now include `genders`), `functionalTest` 225 (+43: 41 in `GendersFunctionalTest`, and 002's unsupported-name list lost `genders` and gained three case variants), `smokeTest` 6 (+1: the genders collection). JaCoCo: lines 497/497 (100%), branches 221/222 (99.5%), unchanged, as no main code changed.

**Strict latency (T033, `./gradlew smokeTest -Dperf.strict=true`)**: collection p95 over 20 calls: `appointment_titles` 2 ms, `base_locations` 4 ms, `contract_types` 1 ms, `genders` 1 ms (bound 100 ms).

**AC-017 / SC-007 (T025)**: `git diff --stat main -- src/main/java` prints nothing. `git diff --stat main -- src/main/resources` lists only `application.yml` (+2: the type entry), plus the new `reference-data/genders.json`. The widened `NoTypeSpecificCodeTest` passes: no main source mentions `gender` in any spelling.

**AC coverage (T026)**: AC-019 (OpenAPI lists `genders`, not `gender`; `ReferenceDataResponse` unchanged) is asserted by `ReferenceDataOpenApiContractTest` (T005). AC-015 is covered by `ReferenceDataFunctionalTest` › `UnsupportedTypeTests` (T006).

**Fixture (T002)**: generated once from the gitignored `REF_Gender.csv` by a throwaway script outside the repository; equals data-model.md § 3 (10 Female, 20 Male, 30 Other, 40 Prefer not to say, all open), UTF-8 without BOM, trailing newline.

**Extension test (T007)**: `ReferenceDataExtensionIntegrationTest` passed unchanged; it needed no redeclared type list.

**Golden file (T014, T021)**: `golden/genders.json` was captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline) and equals the fixture's records. `git diff main -- src/functionalTest/resources/golden/` shows the existing three golden files unchanged.

**Postman (T028)**: newman ran 42 requests (9 new in Reference Data › Genders) with 155 assertions (+34), 0 failures. The folder was inserted as text so the existing collection's formatting is untouched; the unsupported example now requests `judiciary_roles`.

**Quickstart (T034)**: §§ 1–5 walked through; every expected result matched (§ 3 rows 1–15 and the § 4 `enum` `["appointment_titles","base_locations","contract_types","genders","appointment_title"]`). An older `bootRun` was already listening on 8080, so §§ 3–5 ran against this build on port 8081 (`--server.port=8081`, newman with `--env-var baseUrl=http://localhost:8081`).

**Differences from the planned design**:
- The two lengthened `enum` assertions in `ReferenceDataOpenApiContractTest` were wrapped to stay within Checkstyle's 120-character limit.
- `GendersFunctionalTest` was written in one pass (T009 and the story tasks), as every story adds to the same file; T010's gate ran before it was added, and T033's after.

**Code-review follow-ups (2026-10-22)**: one `/code-review` round found no correctness bugs and six findings; three were fixed and three kept.
- 002's quickstart § 3 row 5 now requests `judiciary_roles` instead of carrying a note under the table, so the row passes when run.
- `assertUnauthorized`, `byId` and `nameOf` moved into `testsupport/ReferenceDataHttp` (`nameOf` as an instance method), replacing the private copies in `GendersFunctionalTest`, `ContractTypesFunctionalTest`, `BaseLocationsFunctionalTest` and `ReferenceDataFunctionalTest`. No assertion changed, so AC-014 still holds.
- `ac003NoGenderIsEndedAndNamesAreServedExactly` now checks only the raw body, since `ac002` already checks the parsed records: exactly four `"end_date":null` and "Prefer not to say" verbatim. A second review pass found that an earlier version, which checked for the absence of `"end_date":"`, would pass silently if the JSON were ever reformatted; counting the null values fails instead.
- **Kept**: the FR-023 reading (decided in PR review; spec Clarifications); the guard matching all-caps words such as `TRANSGENDER`, which follows from matching `XGENDER` and fails safe; and the earlier types' id-scoping tests, which AC-014 keeps unchanged (`ac008` covers all four types).
- `./gradlew check -PskipOwasp` green after the fixes, with the same counts: `test` 295, `integrationTest` 33, `functionalTest` 225, `smokeTest` 6; JaCoCo lines 497/497, branches 221/222.
