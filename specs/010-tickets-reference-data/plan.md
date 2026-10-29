# Implementation Plan: Reference Data API — Tickets

**Branch**: `010-tickets-reference-data` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-tickets-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves Ticket through the generic reference-data routes, as `tickets` only. The singular name `ticket` is not served (spec FR-001; Constitution VII). Its 159 records keep the extract's ids, 289 to 1709 with gaps (DR-1, as in 007 to 009), so the People feature can later point authorisations' `ticket_id` at them (DF-9). Each record carries the extract's `ticket_category_id`, a forward reference to the unserved `ticket_categories` (DF-8). Names repeat, so the type opts out of unique names; 22 records are ended by 002's date rule.

There is **one generic main-code change**, test-first, as 003 did for its reference fields and 008 for text fields: `ticket_category_id` becomes a fourth reference field that any type can declare (spec FR-019). The validation and mapping machinery already loops over every `ReferenceField`, so the change is the enum value, the record and response fields, one mapper mapping and the response's property order. The other eight types' responses stay byte-for-byte unchanged, because a reference field is written only for types that declare it.

**What changes**:
- **Main code** (generic only; research R2–R4): `ReferenceField` gains `TICKET_CATEGORY_ID`; `ReferenceDataRecord` gains `ticketCategoryId`; `ReferenceDataResponse` gains a `ticket_category_id` component after `jurisdiction_id` (deviation D-5); `ReferenceDataMapper` maps it; `ReferenceDataProperties`' Javadoc lists it.
- **Data**: `tickets.json`, 159 records generated once from the local CSV extract. Ids, names and category ids are copied; dates follow 002's rule by ascending-id position. The generating script proves the copied values equal the extract and that every category id is an id in the local ticket-category extract before the file is checked in (research R6; data-model.md § 4–5).
- **Configuration**: one `tickets` entry declaring `reference-fields: [ticket_category_id]` and `unique-names: false` (R1).
- **Tests**:
  - test-first unit cases for the new field (registry, repository, mapper, serialisation, controller) and updates to the 11 positional `ReferenceDataResponse` calls (R5);
  - new `TicketsFunctionalTest` with its golden file (R10);
  - `ReferenceDataReferencesTest`: `TARGET_TYPES` maps `ticket_category_id` to `ticket_categories` (R8);
  - fixture-rules test: a `tickets` row, and the category-id set pinned (R7);
  - `NoTypeSpecificCodeTest`: the plural fragment `tickets`, with spelling and non-match cases (R9);
  - OpenAPI contract assertions, the unsupported-name list and its missing-token example, and a smoke guard (R9).
- **Documentation and examples**: a "Tickets" Postman sub-folder, and the "unsupported" example moved to `ticket_categories`; README (nine types, four reference fields, the forward reference); CLAUDE.md; the contract delta; a pointer from 002's contract; 002's quickstart row 5 (R11).

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi, Jackson 3, Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: classpath JSON fixtures loaded at start-up. One more: `tickets.json`, 159 records, about 35 KB.

**Testing**: JUnit 5 and AssertJ in the `test`, `integrationTest`, `functionalTest` and `smokeTest` suites; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: Collection p95 within the smoke suite's default 1 s bound, and within 100 ms when run with `-Dperf.strict=true`. At 159 records the collection is mid-sized (between `judiciary_roles`' 164 and `appointment_titles`' 194).

**Constraints**:
- The only main-code change is the generic reference field (FR-003, FR-019); no type-specific code, and no main source names a reference-data type (FR-018).
- The eight existing types' responses are byte-for-byte unchanged (FR-005, AC-018).
- The extract is used once and never committed (FR-007).
- Names are reproduced exactly (FR-008).
- References between served types keep resolving; `ticket_category_id` is a recorded forward reference (FR-011).

**Scale/Scope**: Nine configured types (194 + 1,462 + 14 + 4 + 164 + 2,000 + 16 + 5 + 159 records), two routes, no new endpoints, four declarable reference fields.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `tickets` is a contract-valid value. Its singular form is not served, which Principle I allows. The record shape is the contract's `ReferenceDataResponse` plus `name` (D-1, unchanged) and `ticket_category_id` (D-5, new, recorded in the spec with its reason). DR-1 concerns id values, which the contract doesn't define, so it isn't a contract deviation. |
| II. Java Spring Boot Architecture | **Pass.** The change stays in the existing layers: an entity enum and field, a domain DTO component and a mapper mapping. No controller, service or filter changes. |
| III. Reuse Before Duplication | **Pass.** The new field reuses the reference-field machinery end to end (declaration, start-up validation, `ReferenceFieldValue`, the mapper's `referenceField` helper), as well as the extract-id rule, the references table and its completeness check, `ReferenceDataHttp`, and the fixture-rules table. |
| IV. Synthetic Data Only | **Pass.** Only public ticket names, ids and category ids come from the extract; dates are synthetic. `ticket_category_id` is a forward reference that meets every condition of the exception: the spec records it as DF-8 naming `ticket_categories`; the values are copied unchanged from the extract and confirmed once against the ticket-category extract; and `TARGET_TYPES` makes the feature that serves `ticket_categories` add the resolution test before it can pass. References between served types keep resolving. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in fixed order; the golden file and repeated-call test check it. |
| VII. Generic Reference-Data Handling | **Pass.** One type-agnostic capability (any type may declare `ticket_category_id`) plus configuration and data. `ticket` is not served and has no `aliases` entry. The guard gains the plural fragment `tickets` (spec Clarifications), which matches no main source after the change, including `ticket_category_id`. |
| VIII. Typed API Models | **Pass.** The new field is a typed `ReferenceFieldValue` component of the `ReferenceDataResponse` record. |
| IX. Centralised Validation and Error Handling | **Pass.** The field is validated by the existing start-up rules for reference fields; API errors are unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** The type and its declared field are configured in `application.yml`. |
| XI. Testability | **Pass (planned).** Unit: the new field in the registry, repository, mapper, serialisation and controller tests; fixture rules, references, the guard. Integration: the OpenAPI contract. Functional: AC-001 to AC-020 over HTTP. Smoke: latency (research R5–R10). |
| XII. Simplicity and Maintainability | **Pass.** One enum value and its plumbing; no new abstraction. |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged; the new branches are covered by the unit cases. Inherits 002's open Sonar deferral. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** A "Reference Data › Tickets" sub-folder with ten requests and tests, with no hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration, and `ticket_category_id` from the annotated DTO. The contract test asserts them, and a contract delta records the change. |

Principles VI, XIV, XV and XVI: no change from 002. No filter, logging, security or class-design rule is affected.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: Ticket has both.
- **Earlier artifacts that become stale**: 009's FR-002 lists the supported names; this spec's FR-002 supersedes it. `ReferenceDataResponse`'s documented fields grow (003's, 007's and 008's contract deltas describe the earlier set; this feature's delta extends them). `ReferenceDataFunctionalTest`, the Postman collection, 002's quickstart and the README use `tickets` as an unserved name; all are updated (R9, R10, R11).

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/010-tickets-reference-data/
├── plan.md                                          # This file
├── research.md                                      # Phase 0: decisions R1–R11
├── data-model.md                                    # Phase 1: the new field, the type and fixture rules
├── quickstart.md                                    # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-tickets.md                # Phase 1: contract delta against 002–009
├── checklists/
│   └── requirements.md                              # From /speckit-specify
└── tasks.md                                         # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/uk/gov/hmcts/ctam/jo/
├── entity/ReferenceField.java                       # + TICKET_CATEGORY_ID (R2)
├── entity/ReferenceDataRecord.java                  # + ticketCategoryId (R2)
├── domain/ReferenceDataResponse.java                # + ticket_category_id component and property order (R4)
├── mappers/ReferenceDataMapper.java                 # + ticketCategoryId mapping (R4)
└── config/ReferenceDataProperties.java              # Javadoc: four reference fields (R2)

src/main/resources/
├── application.yml                                  # + tickets entry (R1)
└── reference-data/tickets.json                      # NEW: 159 records (data-model.md § 4)

src/test/java/uk/gov/hmcts/ctam/jo/
├── testsupport/ReferenceDataFixtures.java           # 8 positional ReferenceDataResponse calls; ticket-category helpers (R5)
├── services/ReferenceDataTypeRegistryTest.java      # declares and resolves ticket_category_id (R5)
├── repository/FixtureReferenceDataRepositoryTest.java  # loads, requires and rejects ticket_category_id (R5)
├── mappers/ReferenceDataMapperTest.java             # maps it only for a declaring type (R5)
├── domain/ReferenceFieldValueTest.java              # property order with every reference field (R5)
├── controllers/ReferenceDataControllerTest.java     # written after name, before text fields (R5)
├── repository/ReferenceDataFixtureRulesTest.java    # tickets row; TICKET_CATEGORY_IDS (R7)
├── repository/ReferenceDataReferencesTest.java      # TARGET_TYPES: ticket_category_id -> ticket_categories (R8)
└── architecture/NoTypeSpecificCodeTest.java         # + "tickets" fragment and its cases (R9)

src/test/resources/                                   # test fixtures for the new field's start-up cases (R5)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                               # + TICKETS

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java            # enum, description and ticket_category_id schema (R9)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java                 # unsupported list: 12 names + variants; missing-token path (R9)
└── TicketsFunctionalTest.java                       # NEW: 010 AC-001 … AC-020 over real HTTP (R10)
src/functionalTest/resources/golden/
└── tickets.json                                     # NEW: captured collection response (AC-005)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                      # + TICKETS (R9)

postman/ctam-jomockapi.postman_collection.json       # + Reference Data › Tickets; unsupported example → ticket_categories
README.md                                            # nine types; four reference fields; the forward reference
CLAUDE.md                                            # § Current repo state: + 010; § Reference data: ticket values
specs/002-reference-data-api/contracts/reference-data-api.md    # one-line pointer to the 010 delta
specs/002-reference-data-api/quickstart.md                      # row 5: tickets -> ticket_categories
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. Four main Java files change in code and one in Javadoc only; no main class is added.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. One new contract deviation, D-5 (`ticket_category_id`), recorded in the spec with its reason, as D-3 and D-4 were. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-tickets.md, quickstart.md).

The design adds one enum value and its plumbing, a configuration entry, a fixture and tests, with no new dependency, layer, endpoint or main class. Four details strengthen the assessments:
- **Byte-identical responses (V, VII)**: `ticket_category_id` is written only when the type declares it (`@JsonInclude(NON_NULL)` with no value object for undeclared fields), and no existing fixture changes, so the existing golden files prove the other types are unaffected.
- **Forward reference (IV)**: `ReferenceDataReferencesTest`'s completeness check already fails if a declared reference field has no entry in `TARGET_TYPES`, so the mapping to `ticket_categories` is required now and the resolution row is required as soon as that type is configured. `TICKET_CATEGORY_IDS` pins the 41 values on every build until then.
- **Generic validation (III, IX)**: `FixtureReferenceDataRepository.requireReferenceKeys` and `validateReferenceFields` iterate over every `ReferenceField`, and the registry resolves declarations by name, so the new field gets the existing missing, null, non-positive and undeclared checks with no new validation code.
- **Guard coverage (VII)**: after the change, main sources contain `ticket_category_id`, `TICKET_CATEGORY_ID`, `ticketCategoryId` and "ticket category"; none matches the plural fragment `tickets`, so the guard passes without an exemption.

The quickstart's expected values were checked against a collection simulated from the extract by the data-model rules: 159 records; ids from 289 to 1709; first `[289,"Welsh Language",88]` and last `[1709,"Valuation Tribunal Wales",83]`; 22 ended ids as in spec AC-003; 152 distinct names; 41 distinct category ids, all of them ids in the ticket-category extract. The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.
