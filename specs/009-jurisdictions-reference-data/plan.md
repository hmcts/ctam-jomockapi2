# Implementation Plan: Reference Data API — Jurisdictions

**Branch**: `009-jurisdictions-reference-data` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-jurisdictions-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves Jurisdiction through the generic reference-data routes, as `jurisdictions` only. The singular name `jurisdiction` is not served (spec FR-001; Constitution VII). Its 5 records keep the extract's ids 30, 34, 84, 86 and 87 (DR-1, as 007 and 008 did), so every Location, BaseLocation and LocationType `jurisdiction_id` resolves. Records carry only the shared fields and `name`; names are unique, and none is ended.

There is **no main-code change**, as for Gender (005): one type entry in `application.yml` and one fixture. Three table rows in `ReferenceDataReferencesTest` check that every `jurisdiction_id` resolves, closing 003 DF-1, 007 DF-5 and 008 DF-7; after this, every reference between served types is checked and none is outstanding (spec FR-011, Clarifications).

**What changes**:
- **Data**: `jurisdictions.json`, 5 records generated once from the local JSON extract. Ids and names are copied; dates follow 002's rule by ascending-id position (none ended). The generating script proves the copied values equal the extract and that every `jurisdiction_id` resolves before the file is checked in (research R2; data-model.md § 3–4).
- **Configuration**: one `jurisdictions` entry; the comments on `base_locations`, `locations` and `location_types` no longer call `jurisdiction_id` a forward reference (R1).
- **Tests**:
  - new `JurisdictionsFunctionalTest` with its golden file (R7);
  - `ReferenceDataReferencesTest`: three `jurisdiction_id` rows (R4);
  - fixture-rules test: a `jurisdictions` row, and the jurisdiction-id set described as the referenced subset (R3);
  - `NoTypeSpecificCodeTest`: the plural fragment `jurisdictions`, with spelling and non-match cases (R5);
  - OpenAPI contract assertions, the unsupported-name list and its missing-token example, and a smoke guard (R6).
- **Documentation and examples**: a "Jurisdictions" Postman sub-folder, and the "unsupported" example moved to `tickets`; README (eight types; no forward references left); CLAUDE.md; the contract delta; a pointer from 002's contract; closing notes on 003 DF-1, 007 DF-5 and 008 DF-7 (R8).

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi, Jackson 3, Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: classpath JSON fixtures loaded at start-up. One more: `jurisdictions.json`, 5 records, about 1 KB.

**Testing**: JUnit 5 and AssertJ in the `test`, `integrationTest`, `functionalTest` and `smokeTest` suites; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: Collection p95 within the smoke suite's default 1 s bound, and within 100 ms when run with `-Dperf.strict=true`. At 5 records this is the second-smallest collection.

**Constraints**:
- No main source file changes (FR-003, SC-009), and none names a reference-data type (FR-018).
- The seven existing types' responses are byte-for-byte unchanged (FR-005, AC-017).
- The extract is used once and never committed (FR-007).
- Names are reproduced exactly (FR-008).
- Every `jurisdiction_id` of every served type resolves (FR-011).

**Scale/Scope**: Eight configured types (194 + 1,462 + 14 + 4 + 164 + 2,000 + 16 + 5 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `jurisdictions` is a contract-valid value. Its singular form is not served, which Principle I allows. The record shape is the contract's `ReferenceDataResponse` plus `name` (D-1, unchanged). DR-1 concerns id values, which the contract doesn't define, so it isn't a contract deviation. |
| II. Java Spring Boot Architecture | **Pass.** No main code changes; the layering is untouched. |
| III. Reuse Before Duplication | **Pass.** Everything the type needs already exists: the extract-id rule, the references table and its completeness check, `ReferenceDataHttp`, and the fixture-rules table. |
| IV. Synthetic Data Only | **Pass.** Only public jurisdiction names and ids come from the extract; dates are synthetic. The three references to `jurisdictions` (Location, BaseLocation, LocationType) resolve, verified by `ReferenceDataReferencesTest`, which closes 003 DF-1, 007 DF-5 and 008 DF-7. No forward reference remains. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in fixed order; the golden file and repeated-call test check it. |
| VII. Generic Reference-Data Handling | **Pass.** Configuration and data only. `jurisdiction` is not served and has no `aliases` entry. The guard gains the plural fragment `jurisdictions` (spec Clarifications), which matches no main source today. |
| VIII. Typed API Models | **Pass.** No model change. |
| IX. Centralised Validation and Error Handling | **Pass.** The fixture is validated by the existing start-up rules; API errors are unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** The type is declared in `application.yml`. |
| XI. Testability | **Pass (planned).** Unit: fixture rules, references, the guard. Integration: the OpenAPI contract. Functional: AC-001 to AC-019 over HTTP. Smoke: latency (research R3–R7). |
| XII. Simplicity and Maintainability | **Pass.** Nothing new is built. |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged; with no main-code change, coverage can't drop. Inherits 002's open Sonar deferral. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** A "Reference Data › Jurisdictions" sub-folder with ten requests and tests, with no hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration. The contract test asserts them, and a contract delta records the change. |

Principles VI, XIV, XV and XVI: no change from 002. No filter, logging, security or class-design rule is affected.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: Jurisdiction has no ended record but has id gaps; the ended-record clause is met across the shipped default data, as 005 established (spec EC-004).
- **Earlier artifacts that become stale**: 008's FR-002 lists the supported names; this spec's FR-002 supersedes it. 003's DF-1, 007's DF-5 and 008's DF-7 are closed, and each spec gets a one-line note (R8). `ReferenceDataFunctionalTest`, the Postman collection and the README use `jurisdictions` as an unserved name or a forward-reference target; all are updated (R6, R7, R8).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/009-jurisdictions-reference-data/
├── plan.md                                          # This file
├── research.md                                      # Phase 0: decisions R1–R8
├── data-model.md                                    # Phase 1: the new type and fixture rules
├── quickstart.md                                    # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-jurisdictions.md          # Phase 1: contract delta against 002–008
├── checklists/
│   └── requirements.md                              # From /speckit-specify
└── tasks.md                                         # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/resources/
├── application.yml                                  # + jurisdictions entry; three comments updated (R1)
└── reference-data/jurisdictions.json                # NEW: 5 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── repository/ReferenceDataFixtureRulesTest.java    # jurisdictions row; JURISDICTION_IDS Javadoc (R3)
├── repository/ReferenceDataReferencesTest.java      # three jurisdiction_id rows; Javadoc (R4)
└── architecture/NoTypeSpecificCodeTest.java         # + "jurisdictions" fragment and its cases (R5)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                               # + JURISDICTIONS

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java            # enum and description gain jurisdictions (R6)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java                 # unsupported list: 13 names + variants; missing-token path (R6)
└── JurisdictionsFunctionalTest.java                 # NEW: 009 AC-001 … AC-019 over real HTTP (R7)
src/functionalTest/resources/golden/
└── jurisdictions.json                               # NEW: captured collection response (AC-004)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                      # + JURISDICTIONS (R6)

postman/ctam-jomockapi.postman_collection.json       # + Reference Data › Jurisdictions; unsupported example → tickets
README.md                                            # eight types; jurisdiction_id resolves; no forward references
CLAUDE.md                                            # § Current repo state: + 009; § Reference data: jurisdiction values
specs/002-reference-data-api/contracts/reference-data-api.md    # one-line pointer to the 009 delta
specs/003-base-locations-reference-data/spec.md                 # DF-1: closed by 009 for jurisdiction_id
specs/007-locations-reference-data/spec.md                      # DF-5: closed by 009 for jurisdiction_id
specs/008-location-types-reference-data/spec.md                 # DF-7: closed by 009
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. No main Java file changes.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST and no new contract deviation. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-jurisdictions.md, quickstart.md).

The design adds a configuration entry, a fixture and tests, with no new dependency, layer, endpoint or main class. Three details strengthen the assessments:
- **Byte-identical responses (V, VII)**: no main code changes and no existing fixture changes, so the existing golden files prove the other types are unaffected.
- **Reference checks (IV)**: `ReferenceDataReferencesTest`'s completeness check demands the three `jurisdiction_id` rows as soon as `jurisdictions` is configured, so they cannot be forgotten; with every target type served, it now demands a row for every reference field of every served type.
- **Guard coverage (VII)**: the plural fragment was checked against today's main sources: 0 matches for `jurisdictions`, 14 for the singular (all the generic field `jurisdiction_id` or its description), so the guard passes without an exemption.

The quickstart's expected values were checked against a collection simulated from the extract by the data-model rules: 5 records; ids `[30,34,84,86,87]`; first `[30,"Courts"]` and last `[87,"Skills"]`; no ended record; 5 distinct names; 0 unresolved `jurisdiction_id` values across `locations.json` (2,000), `base_locations.json` (1,462) and `location_types.json` (16). The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.

## Implementation notes

**Baseline (T001, before any change)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 400, `integrationTest` 33, `functionalTest` 392, `smokeTest` 9. JaCoCo: lines 534/534 (100%), branches 239/240 (99.6%; the missed branch is MapStruct's generated null check on the boxed `id`, as recorded in 003, 007 and 008).

**Fixture (T002)**: generated once from the gitignored `REF_Jurisdiction.json` by a throwaway script in the session scratchpad (never committed). Before the file was kept, the script confirmed:
- no BOM; the top level is an object whose only key is `results`; 5 entries, each with exactly `id`, `name`, `start_date`, `end_date`, `created_at` and `updated_at`;
- ids are the integers 30, 34, 84, 86, 87; names non-blank, trimmed and distinct;
- the written `id` and `name` equal the extract's, entry for entry (SC-003); no record is ended;
- every row of data-model.md § 3's Records table;
- every `jurisdiction_id` in the checked-in `locations.json` (2,000 records), `base_locations.json` (1,462) and `location_types.json` (16) is one of the written ids; each uses exactly {30, 34, 84, 86}, and none uses 87.

UTF-8 without BOM, two-space indent, trailing newline, 923 bytes, no `\u`.

**Extension test (T009)**: `ReferenceDataExtensionIntegrationTest` passed unchanged (6 tests).

**Golden file (T015)**: `golden/jurisdictions.json` was captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline, 723 bytes). Its 5 records equal the fixture's, field for field and in key order. The existing seven golden files are unchanged (`git diff --quiet origin/main -- src/functionalTest/resources/golden/` succeeds; AC-017, SC-008).

**AC-010 (T021)**: met by T008. `everyReferenceBetweenServedTypesIsChecked` passes with the three new `jurisdiction_id` rows; with every target type now served, it requires a row for every reference field of every served type, and all seven rows are present. No new check was added (spec Clarifications Q2).

**AC-020 / SC-009 (T029)**: `git diff --stat origin/main -- src/main/java` prints nothing. Under `src/main/resources`, only `application.yml` (the type entry and the three reworded comments; 8 insertions, 6 deletions) and the new `reference-data/jurisdictions.json`.

**AC-022 (T030)**: covered by `ReferenceDataOpenApiContractTest` (T005): both operations' `enum` lists `jurisdictions` after `location_types` and before `appointment_title`, the description names it, `jurisdiction` is absent, and the `ReferenceDataResponse` assertions are unchanged.

**Postman (T032)**: newman ran 81 requests (+10 in Reference Data › Jurisdictions) with 309 assertions (+39), 0 failures. The folder was inserted as text after Location Types, in the collection's own layout (checked by round-tripping the Location Types folder first); the only other change is the "unsupported" example renamed from `jurisdictions` to `tickets` (name, `raw` and `path`, tests unchanged). The diff is 557 insertions and those 3 changed lines.

**Final (T038)**: `./gradlew check -PskipOwasp` green. Tests per suite:
- `test` 421 (+21): one fixture-rules row, which adds three cases; `ReferenceDataReferencesTest` (+6: the three `jurisdiction_id` rows, each run by the resolution and cycle tests); `NoTypeSpecificCodeTest` (+12: the `jurisdictions` fragment, six recognised spellings and five ignored field spellings).
- `integrationTest` 33 (the contract test's `enum` and description changed).
- `functionalTest` 444 (+52): 50 in `JurisdictionsFunctionalTest`, and 002's unsupported-name list lost `jurisdictions` and gained three case variants (+2).
- `smokeTest` 10 (+1: the jurisdictions collection).

JaCoCo: lines 534/534 (100%), branches 239/240 (99.6%), unchanged, as no main code changed.

**Strict latency (T038, `./gradlew smokeTest -Dperf.strict=true`)**: collection p95: `appointment_titles` 2 ms, `base_locations` 4 ms, `contract_types` 1 ms, `genders` 1 ms, `judiciary_roles` 1 ms, `locations` 3 ms, `location_types` 1 ms, `jurisdictions` 1 ms (bound 100 ms).

**Quickstart (T039)**: §§ 1–5 walked through against `./gradlew bootRun`. Every expected result matched: § 2's scope checks, § 3 rows 1–16 (including 0 unresolved `jurisdiction_id` values and 0 references to 87 in each of `locations`, `base_locations` and `location_types`), the § 4 `enum` (`jurisdictions` listed, `jurisdiction` not) and § 5's newman run.

**Differences from the planned design**:
- As in 006–008, `JurisdictionsFunctionalTest` was written in one pass (T010 and the story tasks), and the golden file was captured before the first gate run, so T011's gate already ran every story's functional test. T028's guard cases and T033's smoke path were also added before T011, since they touch files the gate runs.
- `assertUnauthorized` and `byId` come from `ReferenceDataHttp`, where 007 moved them, so the new class has only `assertRecord` as a private helper.
- T035 also adds jurisdiction names to CLAUDE.md's list of public names taken from the extracts, alongside the ids.

**Code-review follow-ups (2026-10-29)**: one `/code-review` round found no correctness bugs. One finding was fixed and the rest kept.
- **Fixed, every record pinned**: the `jurisdictions.json` fixture-rules row lists all five records as reference points, adding 34 Tribunals, so the unit suite catches a changed name without the functional suite. `test` 421, unchanged in count (reference points are checked inside the row's existing cases).
- **Kept**:
  - The guard's plural fragment `jurisdictions` doesn't catch the singular in main code; this is the decision recorded in spec Clarifications Q1.
  - `JURISDICTION_IDS` and the base-locations Postman check stay pinned to {30, 34, 84, 86}: they pin the values the location data uses, while resolution is `ReferenceDataReferencesTest`'s job (research R3).
  - `ac009` and `ac011` repeat the resolution check over HTTP because US3 requires it to be observable to integrators; the extra fetches cost a few milliseconds.
  - The OpenAPI test's wrapped description string follows the existing style.
