# Implementation Plan: Reference Data API — Ticket Category Types

**Branch**: `012-ticket-category-types-reference-data` | **Date**: 2026-10-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/012-ticket-category-types-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves TicketCategoryType through the generic reference-data routes, as `ticket_category_types` only. The singular name `ticket_category_type` is not served (spec FR-001; Constitution VII). It is the last E-Links canonical name, so afterwards all eleven are served. Its 5 records keep the extract's ids 1 to 5 (DR-1, as 007 to 011 did), so every ticket category's `type_id` resolves. Records carry `jurisdiction_id` and `integration_code` after `name`, like location types without `type_id`; "Court Type" repeats, and none is ended.

There is **no main-code change**, as for Jurisdiction (009): one type entry in `application.yml` and one fixture. Two rows in `ReferenceDataReferencesTest` check that every ticket category's `type_id` and every type's `jurisdiction_id` resolve, and an agreement row checks that every category is in its type's jurisdiction. This closes 011 DF-10; afterwards no reference between served types is outstanding (spec FR-011, FR-012).

**What changes**:
- **Data**: `ticket_category_types.json`, 5 records generated once from the local CSV extract. Ids, names, jurisdictions and integration codes are copied; dates follow 002's rule by ascending-id position (none ended). The generating script proves the copied values equal the extract, every `jurisdiction_id` resolves, and every ticket category's `type_id` resolves in the same jurisdiction, before the file is checked in (research R2; data-model.md § 3).
- **Configuration**: one `ticket_category_types` entry; the `ticket_categories` comment no longer calls `type_id` a forward reference (R1).
- **Tests**:
  - new `TicketCategoryTypesFunctionalTest` with its golden file (R7);
  - `ReferenceDataReferencesTest`: two resolution rows and one agreement row (R4);
  - fixture-rules test: a `ticket_category_types` row, and Javadoc on the jurisdiction and type pins (R3);
  - `NoTypeSpecificCodeTest`: the fragment `ticket_category_type` (singular, which also matches the plural), with spelling and non-match cases (R5);
  - OpenAPI contract assertions, the unsupported-name list and its missing-token example (now `ticket_category_type`), the "other types don't carry the field" lists of 010 and 011, a Skills comment, and a smoke guard (R6).
- **Documentation and examples**: a "Ticket Category Types" Postman sub-folder, and the "unsupported" example moved to `ticket_category_type`; README (eleven types, every E-Links name served); CLAUDE.md; the contract delta; a pointer from 002's contract and 002's quickstart row 5; a closing note on 011 DF-10 (R8).

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi, Jackson 3, Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: classpath JSON fixtures loaded at start-up. One more: `ticket_category_types.json`, 5 records, about 1 KB.

**Testing**: JUnit 5 and AssertJ in the `test`, `integrationTest`, `functionalTest` and `smokeTest` suites; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: Collection p95 within the smoke suite's default 1 s bound, and within 100 ms when run with `-Dperf.strict=true`. At 5 records it ties with jurisdictions as the second-smallest collection.

**Constraints**:
- No main source file changes (FR-003, SC-009), and none names a reference-data type (FR-020).
- The ten existing types' responses are byte-for-byte unchanged (FR-005, AC-019).
- The extract is used once and never committed (FR-007).
- Names and integration codes are reproduced exactly (FR-008).
- Every reference between served types resolves, and every ticket category agrees with its type's jurisdiction (FR-011, FR-012).

**Scale/Scope**: Eleven configured types (194 + 1,462 + 14 + 4 + 164 + 2,000 + 16 + 5 + 159 + 54 + 5 records), two routes, no new endpoints.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `ticket_category_types` is a contract-valid value. Its singular form is not served, which Principle I allows. The record shape is the contract's `ReferenceDataResponse` plus `name` (D-1), `jurisdiction_id` (D-2) and `integration_code` (D-4), all existing deviations. DR-1 concerns id values, which the contract doesn't define, so it isn't a contract deviation. |
| II. Java Spring Boot Architecture | **Pass.** No main code changes; the layering is untouched. |
| III. Reuse Before Duplication | **Pass.** Everything the type needs already exists: declared reference and text fields, repeated names, the extract-id rule, the per-type target override, the references and agreement tables, `ReferenceDataHttp`, and the fixture-rules table. |
| IV. Synthetic Data Only | **Pass.** Only public names, ids, jurisdiction ids and integration codes come from the extract; dates are synthetic. Both references touching the type (TicketCategory `type_id` → TicketCategoryType; TicketCategoryType `jurisdiction_id` → Jurisdiction) resolve, verified by `ReferenceDataReferencesTest`, which closes 011 DF-10. No forward reference remains between served types. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in fixed order; the golden file and repeated-call test check it. |
| VII. Generic Reference-Data Handling | **Pass.** Configuration and data only. `ticket_category_type` is not served and has no `aliases` entry. The guard gains the fragment `ticket_category_type`, which matches no main source today. |
| VIII. Typed API Models | **Pass.** No model change. |
| IX. Centralised Validation and Error Handling | **Pass.** The fixture is validated by the existing start-up rules; API errors are unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** The type is declared in `application.yml`. |
| XI. Testability | **Pass (planned).** Unit: fixture rules, references, agreement, the guard. Integration: the OpenAPI contract. Functional: AC-001 to AC-021 over HTTP. Smoke: latency (research R3–R7). |
| XII. Simplicity and Maintainability | **Pass.** Nothing new is built. |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged; with no main-code change, coverage can't drop. Inherits 002's open Sonar deferral. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** A "Reference Data › Ticket Category Types" sub-folder with ten requests and tests, with no hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration. The contract test asserts them, and a contract delta records the change. |

Principles VI, XIV, XV and XVI: no change from 002. No filter, logging, security or class-design rule is affected.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: TicketCategoryType has neither an ended record nor an id gap; both clauses are met across the shipped default data, as 005 and 009 established (spec EC-004).
- **Earlier artifacts that become stale**: 011's FR-002 lists the supported names; this spec's FR-002 supersedes it. 011's DF-10 is closed and gets a one-line note. `ReferenceDataFunctionalTest`, the Postman collection, 002's quickstart, the README, `application.yml`'s `ticket_categories` comment and the fixture-rules pins' Javadoc describe `ticket_category_types` as unserved or a forward-reference target; all are updated (R1, R3, R6, R7, R8).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/012-ticket-category-types-reference-data/
├── plan.md                                               # This file
├── research.md                                           # Phase 0: decisions R1–R8
├── data-model.md                                         # Phase 1: the new type and fixture rules
├── quickstart.md                                         # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-ticket-category-types.md       # Phase 1: contract delta against 002–011
├── checklists/
│   └── requirements.md                                   # From /speckit-specify
└── tasks.md                                              # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/resources/
├── application.yml                                       # + ticket_category_types entry; ticket_categories comment (R1)
└── reference-data/ticket_category_types.json             # NEW: 5 records (data-model.md § 3)

src/test/java/uk/gov/hmcts/ctam/jo/
├── repository/ReferenceDataFixtureRulesTest.java         # new row; JURISDICTION_IDS and TICKET_CATEGORY_TYPE_IDS Javadoc (R3)
├── repository/ReferenceDataReferencesTest.java           # two reference rows, one agreement row (R4)
└── architecture/NoTypeSpecificCodeTest.java              # + "ticket_category_type" fragment and its cases (R5)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                                    # + TICKET_CATEGORY_TYPES

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java                 # enum and description gain ticket_category_types (R6)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java                      # unsupported list: 10 names + variants; missing-token path (R6)
├── TicketsFunctionalTest.java                            # ac012 + TICKET_CATEGORY_TYPES (R6)
├── TicketCategoriesFunctionalTest.java                   # fr014 + TICKET_CATEGORY_TYPES (R6)
├── JurisdictionsFunctionalTest.java                      # comment above ac011 (R6)
└── TicketCategoryTypesFunctionalTest.java                # NEW: 012 AC-001 … AC-021 over real HTTP (R7)
src/functionalTest/resources/golden/
└── ticket_category_types.json                            # NEW: captured collection response (AC-005)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                           # + TICKET_CATEGORY_TYPES (R6)

postman/ctam-jomockapi.postman_collection.json            # + Reference Data › Ticket Category Types; unsupported example → ticket_category_type
README.md                                                 # eleven types; every E-Links name served; DF-10 closed
CLAUDE.md                                                 # § Current repo state: + 012; § Reference data: ticket-category-type values
specs/002-reference-data-api/contracts/reference-data-api.md    # one-line pointer to the 012 delta
specs/002-reference-data-api/quickstart.md                      # row 5: ticket_category_type
specs/011-ticket-categories-reference-data/spec.md              # DF-10: closed by 012
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. No main Java file changes.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST and no new contract deviation. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-ticket-category-types.md, quickstart.md).

The design adds a configuration entry, a fixture and tests, with no new dependency, layer, endpoint or main class. Three details strengthen the assessments:
- **Byte-identical responses (V, VII)**: no main code changes and no existing fixture changes, so the existing golden files prove the other types are unaffected.
- **Reference checks (IV)**: `ReferenceDataReferencesTest`'s completeness check demands both new rows as soon as `ticket_category_types` is configured, because 011's per-type override already names it as `ticket_categories.type_id`'s target; with every target served, it requires a row for every reference field of every served type.
- **Guard coverage (VII)**: the fragment was checked against today's main sources: `git grep -i -E 'ticket.?category.?types?' -- src/main/java` finds 0 matches, so the guard passes without an exemption.

The quickstart's expected values were checked against the local extract and the checked-in fixtures: 5 records; 4 distinct names; 5 distinct integration codes; 0 ticket categories (of 54) whose `jurisdiction_id` differs from their type's; every type `jurisdiction_id` a served jurisdiction; `location_types/1` is "Region" on the running service. The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.

## Implementation notes

**Baseline (T001, before any change)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 486, `integrationTest` 33, `functionalTest` 573, `smokeTest` 12. JaCoCo: lines 538/538 (100%), branches 239/240 (99.6%; the missed branch is MapStruct's generated null check on the boxed `id`, as recorded since 003).

**Fixture (T002)**: generated once from the gitignored `REF_TicketCategoryType.csv` by a throwaway script in the session scratchpad (never committed). Before the file was kept, the script confirmed:
- no BOM; the header is exactly `id,name,jurisdiction_id,integration_code,start_date,end_date,created_at,updated_at`; 5 rows of 8 columns;
- ids are the integers 1 to 5; names non-blank and trimmed (4 distinct); `jurisdiction_id` positive; integration codes trimmed, `[A-Z_]+` and distinct;
- the written `id`, `name`, `jurisdiction_id` and `integration_code` equal the extract's, row for row (SC-003); no record is ended;
- every `jurisdiction_id` is an id in the checked-in `jurisdictions.json`;
- every `type_id` in the checked-in `ticket_categories.json` (54 records) is a written id, and every category's `jurisdiction_id` equals its type's (0 mismatches);
- every row of data-model.md § 3's Records table.

UTF-8 without BOM, two-space indent, trailing newline, 1,281 bytes, no `\u`.

**Extension test (T009)**: `ReferenceDataExtensionIntegrationTest` passed unchanged (6 tests).

**Golden file (T016)**: `golden/ticket_category_types.json` was captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline, 1,021 bytes). Its 5 records equal the fixture's, field for field and in key order. The existing ten golden files are unchanged (`git diff --quiet origin/main -- src/functionalTest/resources/golden/` succeeds; AC-019, SC-008).

**AC-013 (T024)**: the completeness half is met by T008. `everyReferenceBetweenServedTypesIsChecked` passes with the two new rows; 011's per-type override names `ticket_category_types` as `ticket_categories.type_id`'s target, and that type is now served, so the check demands the row. With every target served, it requires a row for every reference field of every served type. The type-jurisdiction agreement compares all 54 categories.

**AC-022 / SC-009 (T033)**: `git diff --stat origin/main -- src/main/java` prints nothing. Under `src/main/resources`, only `application.yml` (the type entry and the reworded `ticket_categories` comment; 11 insertions, 3 deletions) and the new `reference-data/ticket_category_types.json`.

**AC-024 (T034)**: covered by `ReferenceDataOpenApiContractTest` (T005): both operations' `enum` lists `ticket_category_types` after `ticket_categories` and before `appointment_title`, the description names it, `ticket_category_type` is absent, and the `ReferenceDataResponse` assertions are unchanged.

**Postman (T036)**: newman ran 112 requests (+10 in Reference Data › Ticket Category Types) with 430 assertions (+39), 0 failures. The folder was generated from the Jurisdictions folder (checked by round-tripping it first) and inserted as text after Ticket Categories; the only other change is the "unsupported" example renamed from `ticket_category_types` to `ticket_category_type` (name, `raw` and `path`, tests unchanged). The diff is 563 insertions and those 3 changed lines.

**Final (T042)**: `./gradlew check -PskipOwasp` green. Tests per suite:
- `test` 506 (+20): the fixture-rules row (+3 cases); `ReferenceDataReferencesTest` (+5: two rows, each run by the resolution and cycle tests, and one agreement row); `NoTypeSpecificCodeTest` (+12, later +13: the fragment, six (later seven) recognised spellings and five ignored names).
- `integrationTest` 33 (the contract test's `enum` and description changed).
- `functionalTest` 627 (+54): 49 in `TicketCategoryTypesFunctionalTest`; 002's unsupported-name list lost `ticket_category_types` and gained four case variants (+3); and the 010 and 011 "other types don't carry the field" checks gained the new type (+2).
- `smokeTest` 13 (+1: the ticket-category-types collection).

JaCoCo: lines 538/538 (100%), branches 239/240 (99.6%), unchanged, as no main code changed.

**Strict latency (T042, `./gradlew smokeTest -Dperf.strict=true`)**: collection p95: `appointment_titles` 2 ms, `base_locations` 5 ms, `contract_types` 1 ms, `genders` 1 ms, `judiciary_roles` 1 ms, `locations` 4 ms, `location_types` 1 ms, `jurisdictions` 0 ms, `tickets` 1 ms, `ticket_categories` 1 ms, `ticket_category_types` 1 ms (bound 100 ms).

**Quickstart (T043)**: §§ 1–5 walked through against `./gradlew bootRun`. Every expected result matched: § 2's scope checks (no `src/main/java` diff; the guard's pattern finds nothing), § 3 rows 1–19 (including 0 categories disagreeing with their type's jurisdiction, `[88] [5]` for Skills, and `400` for all ten unserved singular names), the § 4 `enum` (`ticket_category_types` listed, `ticket_category_type` not) and § 5's newman run.

**Differences from the planned design**:
- As in 006–011, `TicketCategoryTypesFunctionalTest` was written in one pass (T010 and the story tasks), and the golden file was captured before the first gate run, so T011's gate already ran every story's functional test. T023, T029, T032 and T037 were also done before T011, since they touch files the gate runs.
- The guard fragment became the singular `ticket_category_type` after code review (see below).

**Code-review follow-ups (2026-10-30)**: one `/code-review` round found no correctness bugs. Three findings were fixed and four kept.
- **Fixed**:
  - **The guard fragment is the singular `ticket_category_type`** (spec FR-020, AC-023, Assumptions; research R5; T032). 009 to 011 used the plural only because a generic field contains their singular; none contains `ticket_category_type`, and the singular also matches the plural, so it catches names such as `TicketCategoryTypeMapper` the plural would miss. The recognised-spelling cases are now seven (singular and plural); the guard comment, README bullet and CLAUDE.md line say why. `test` 507 (+1).
  - **`LocationTypesFunctionalTest.ec004OtherTypesDoNotCarryIntegrationCode`** also covers `jurisdictions`, `tickets` and `ticket_categories`, so every type that doesn't declare `integration_code` is checked now that two types do (+3).
  - **`JurisdictionsFunctionalTest.ac009EveryJurisdictionIdResolves`** also covers `ticket_categories` (54) and `ticket_category_types` (5), so jurisdictions resolve over HTTP for every type that declares `jurisdiction_id` (+2). `functionalTest` 632.
- **Kept**:
  - The OpenAPI description of `integration_code` ("E-Links' code for the record's type"): a ticket category type record is itself a type, as a location type is, and rewording it would be a main-code change, which FR-003 and SC-009 rule out.
  - The two Postman requests for `ticket_category_type`: the duplication accepted in analyze (A1), since the top-level request stays the general unsupported-name example (spec Clarifications).
  - `TICKET_CATEGORY_TYPE_IDS`: kept as 009 and 011 kept their pins (spec FR-019; research R3); it pins which ids the categories use, while ReferenceDataReferencesTest checks that they resolve.
  - `ac010` to `ac012` repeating the build-time checks over HTTP: US3 requires them to be observable to integrators, as 009 kept after its review.

`./gradlew check -PskipOwasp` green after the fixes: `test` 507, `integrationTest` 33, `functionalTest` 632, `smokeTest` 13.

**Second code-review round (2026-10-30)**: no correctness bugs. Two findings were fixed:
- `TicketCategoryTypesFunctionalTest.ac012EveryTypeJurisdictionResolves` repeated the check the first round added to `JurisdictionsFunctionalTest.ac009`; it is now `ac012SkillsCategoryRefersToSkills`, which keeps only what it adds (type 5 refers to 87 Skills, AC-013), and T022 says so.
- `ac003NoTicketCategoryTypeIsEnded` counts `"end_date":null` against `RECORDS.size() + 1` instead of a literal 6.

Six were kept:
- 011's quickstart still expects `ticket_category_types` to return `400`: earlier features' quickstarts record their own state, as 009 left 005's and 006's; only 002's living runbook is updated (row 5).
- The duplicate Postman request, the reference targets in test code (011 Clarifications), `ac010`/`ac011` over HTTP and the `TICKET_CATEGORY_TYPE_IDS` pin: as recorded after the first round.
- The missing-token example's move to `ticket_category_type`: it checks that authentication comes before the attribute-name check, which still holds; with every canonical name served, no contract-valid unserved name is left to use.

`./gradlew check -PskipOwasp` green with unchanged counts: `test` 507, `integrationTest` 33, `functionalTest` 632, `smokeTest` 13.
