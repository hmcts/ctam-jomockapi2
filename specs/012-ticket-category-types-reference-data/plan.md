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
  - `NoTypeSpecificCodeTest`: the plural fragment `ticket_category_types`, with spelling and non-match cases (R5);
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
| VII. Generic Reference-Data Handling | **Pass.** Configuration and data only. `ticket_category_type` is not served and has no `aliases` entry. The guard gains the plural fragment `ticket_category_types`, which matches no main source today. |
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
└── architecture/NoTypeSpecificCodeTest.java              # + "ticket_category_types" fragment and its cases (R5)

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
- **Guard coverage (VII)**: the plural fragment was checked against today's main sources: `git grep -i -E 'ticket.?category.?types?' -- src/main/java` finds 0 matches, so the guard passes without an exemption.

The quickstart's expected values were checked against the local extract and the checked-in fixtures: 5 records; 4 distinct names; 5 distinct integration codes; 0 ticket categories (of 54) whose `jurisdiction_id` differs from their type's; every type `jurisdiction_id` a served jurisdiction; `location_types/1` is "Region" on the running service. The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.
