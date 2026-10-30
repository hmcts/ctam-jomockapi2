# Implementation Plan: Reference Data API — Ticket Categories

**Branch**: `011-ticket-categories-reference-data` | **Date**: 2026-10-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-ticket-categories-reference-data/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

This feature serves TicketCategory through the generic reference-data routes, as `ticket_categories` only. The singular name `ticket_category` is not served (spec FR-001; Constitution VII). Its 54 records keep the extract's ids, 31 to 88 with gaps (DR-1), so every ticket's `ticket_category_id` resolves, closing 010 DF-8. Each record carries the extract's `type_id` (a forward reference to the unserved `ticket_category_types`, DF-10), `jurisdiction_id` and `parent_category_id` (null for the 8 top-level categories). One name repeats; 7 records are ended by 002's date rule.

There is **one generic main-code change**, test-first, as 010 did: `parent_category_id` becomes a fifth reference field that any type can declare (spec FR-019). As in 010, the change is the enum value, the record and response fields, one mapper mapping and the response's property order; this time the schema marks the field nullable, as `parent_id`'s is.

The clarified reference-target question (spec FR-020, Clarifications) is solved **in test code only**: the reference checks gain a per-type override, so `ticket_categories.type_id` targets `ticket_category_types` while every other `type_id` keeps targeting `location_types`. The same override pins this type's `type_id` values separately.

**What changes**:
- **Main code** (generic only; research R2–R4): `ReferenceField` gains `PARENT_CATEGORY_ID`; `ReferenceDataRecord` gains `parentCategoryId`; `ReferenceDataResponse` gains a nullable `parent_category_id` component after `ticket_category_id` (deviation D-6); `ReferenceDataMapper` maps it; `ReferenceDataProperties`' Javadoc lists it.
- **Data**: `ticket_categories.json`, 54 records generated once from the local CSV extract. Ids, names and the three reference values are copied; dates follow 002's rule by ascending-id position. The generating script proves the copied values equal the extract, that every parent resolves with the same jurisdiction, that every `type_id` is a ticket-category-type id, and that every ticket's category resolves (research R6; data-model.md § 4–5).
- **Configuration**: one `ticket_categories` entry declaring the three reference fields, `parent_category_id` nullable, and `unique-names: false` (R1).
- **Tests**:
  - test-first unit cases for the new field (registry, repository, mapper, serialisation, controller) and updates to the 13 positional `ReferenceDataResponse` calls (R5);
  - `ReferenceDataReferencesTest`: per-type target override, three new reference rows (one self-referencing), a parent-jurisdiction agreement row, and agreement rows that skip a present `null` via (R8);
  - fixture-rules test: a `ticket_categories` row, per-type pinned values for its `type_id`, and `JURISDICTION_IDS` gains 87 (R7);
  - new `TicketCategoriesFunctionalTest` with its golden file; 009's `ac011` renamed to say Skills is unreferenced by the location data (R10);
  - `NoTypeSpecificCodeTest`: the plural fragment `ticket_categories` (R9);
  - OpenAPI contract assertions, the unsupported-name list and its missing-token example, and a smoke guard (R9).
- **Documentation and examples**: a "Ticket Categories" Postman sub-folder, and the "unsupported" example moved to `ticket_category_types`; README (ten types, five reference fields, DF-8 closed, Skills referenced, the new forward reference); CLAUDE.md; the contract delta; pointers from 002's contract; 002's quickstart row 5; a closing note on 010 DF-8 (R11).

## Technical Context

**Language/Version**: Java 25 (unchanged)

**Primary Dependencies**: Spring Boot 4.1.1, springdoc-openapi, Jackson 3, Lombok, MapStruct (all unchanged; `gradle.lockfile` untouched)

**Storage**: classpath JSON fixtures loaded at start-up. One more: `ticket_categories.json`, 54 records, about 14 KB.

**Testing**: JUnit 5 and AssertJ in the `test`, `integrationTest`, `functionalTest` and `smokeTest` suites; JaCoCo gate (90% lines, 80% branches); Checkstyle; newman for Postman

**Target Platform**: Linux container / local JVM (unchanged)

**Project Type**: Single Spring Boot web service (mock API)

**Performance Goals**: Collection p95 within the smoke suite's default 1 s bound, and within 100 ms when run with `-Dperf.strict=true`. At 54 records the collection is small.

**Constraints**:
- The only main-code change is the generic reference field (FR-003, FR-019); no type-specific code, and no main source names a reference-data type (FR-018).
- The reference-target override lives in test code only (FR-020, Clarifications).
- The nine existing types' responses are byte-for-byte unchanged (FR-005, AC-020).
- The extract is used once and never committed (FR-007).
- Names are reproduced exactly (FR-008).
- References between served types resolve, and child categories agree with their parent's jurisdiction (FR-011, FR-013).

**Scale/Scope**: Ten configured types (194 + 1,462 + 14 + 4 + 164 + 2,000 + 16 + 5 + 159 + 54 records), two routes, no new endpoints, five declarable reference fields.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `ticket_categories` is a contract-valid value; its singular form is not served, which Principle I allows. The record shape is the contract's `ReferenceDataResponse` plus `name` (D-1), `type_id` and `jurisdiction_id` (D-2) and `parent_category_id` (D-6, new, recorded in the spec with its reason). DR-1 concerns id values, which the contract doesn't define. |
| II. Java Spring Boot Architecture | **Pass.** The change stays in the existing layers: an entity enum and field, a domain DTO component and a mapper mapping. |
| III. Reuse Before Duplication | **Pass.** The new field reuses the reference-field machinery end to end, including nullable handling; the hierarchy reuses the self-reference and cycle checks built for `locations.parent_id`; the override extends the existing target table rather than adding a second mechanism. |
| IV. Synthetic Data Only | **Pass.** Only public names and ids come from the extract; dates are synthetic. Served references resolve (tickets → categories, closing 010 DF-8; categories → parent categories; categories → jurisdictions), and child categories agree with their parent's jurisdiction. `type_id` is a forward reference meeting every condition of the exception: the spec records it as DF-10 naming `ticket_category_types`; the values are copied unchanged and confirmed once against the type extract; and the completeness check will require the resolution test once that type is configured. |
| V. Deterministic Behaviour | **Pass.** A fixed fixture in fixed order; the golden file and repeated-call test check it. |
| VII. Generic Reference-Data Handling | **Pass.** One type-agnostic capability plus configuration and data. `ticket_category` is not served. The guard gains the plural fragment `ticket_categories`, which matches no main source after the change. |
| VIII. Typed API Models | **Pass.** The new field is a typed `ReferenceFieldValue` component. |
| IX. Centralised Validation and Error Handling | **Pass.** The field is validated by the existing start-up rules for reference fields; API errors are unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** The type and its declared fields are configured in `application.yml`. |
| XI. Testability | **Pass (planned).** Unit: the new field across the registry, repository, mapper, serialisation and controller tests; fixture rules, references, agreements, the guard. Integration: the OpenAPI contract. Functional: AC-001 to AC-022 over HTTP. Smoke: latency. |
| XII. Simplicity and Maintainability | **Pass.** One enum value and its plumbing; the override is a small table in one test. |
| XIII. Automated Quality Gates | **Pass (planned).** `-Werror`, Checkstyle, JaCoCo and OWASP are unchanged. Inherits 002's open Sonar deferral. |
| XVII. API Testability and Postman Artifacts | **Pass (planned).** A "Reference Data › Ticket Categories" sub-folder with eleven requests and tests, with no hard-coded environment values. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass (planned).** The enum and description are generated from configuration, and `parent_category_id` from the annotated DTO. The contract test asserts them, and a contract delta records the change. |

Principles VI, XIV, XV and XVI: no change from 002.

**Non-principle checks**:
- **002 FR-023 (at least one ended record, and id gaps)**: TicketCategory has both.
- **Earlier artifacts that become stale**: 010's FR-002 (superseded); 010 DF-8 (closed, with a note in 010's spec); 009's README sentence and `ac011` test name about Skills being unreferenced (narrowed); `JURISDICTION_IDS`' Javadoc (87 now referenced). 009's EC-003 already limits itself to the location data and stays true. `ReferenceDataFunctionalTest`, the Postman collection and 002's quickstart use `ticket_categories` as an unserved name; all are updated.

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

## Project Structure

### Documentation (this feature)

```text
specs/011-ticket-categories-reference-data/
├── plan.md                                          # This file
├── research.md                                      # Phase 0: decisions R1–R11
├── data-model.md                                    # Phase 1: the new field, the type and fixture rules
├── quickstart.md                                    # Phase 1: run and check guide
├── contracts/
│   └── reference-data-api-ticket-categories.md      # Phase 1: contract delta against 002–010
├── checklists/
│   └── requirements.md                              # From /speckit-specify
└── tasks.md                                         # Phase 2: task list (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/uk/gov/hmcts/ctam/jo/
├── entity/ReferenceField.java                       # + PARENT_CATEGORY_ID (R2)
├── entity/ReferenceDataRecord.java                  # + parentCategoryId (R2)
├── domain/ReferenceDataResponse.java                # + nullable parent_category_id component and property order (R4)
├── mappers/ReferenceDataMapper.java                 # + parentCategoryId mapping (R4)
└── config/ReferenceDataProperties.java              # Javadoc: five reference fields (R2)

src/main/resources/
├── application.yml                                  # + ticket_categories entry (R1)
└── reference-data/ticket_categories.json            # NEW: 54 records (data-model.md § 4)

src/test/java/uk/gov/hmcts/ctam/jo/
├── testsupport/ReferenceDataFixtures.java           # 9 positional ReferenceDataResponse calls; parent-category helpers (R5)
├── services/ReferenceDataTypeRegistryTest.java      # declares and resolves parent_category_id (R5)
├── repository/FixtureReferenceDataRepositoryTest.java  # loads, nulls, requires and rejects parent_category_id (R5)
├── mappers/ReferenceDataMapperTest.java             # 2 calls; maps it, including a present null (R5)
├── domain/ReferenceFieldValueTest.java              # 2 calls; property order with every reference field (R5)
├── controllers/ReferenceDataControllerTest.java     # written after ticket_category_id; null when declared nullable (R5)
├── repository/ReferenceDataFixtureRulesTest.java    # ticket_categories row; per-type pins; JURISDICTION_IDS + 87 (R7)
├── repository/ReferenceDataReferencesTest.java      # per-type targets; 3 rows; agreement row; null via skipped (R8)
└── architecture/NoTypeSpecificCodeTest.java         # + "ticket_categories" fragment and its cases (R9)

src/test/resources/reference-data/                   # fixtures for the new field's start-up cases (R5)

src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── ContractPaths.java                               # + TICKET_CATEGORIES

src/integrationTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataOpenApiContractTest.java            # enum, description and parent_category_id schema (R9)

src/functionalTest/java/uk/gov/hmcts/ctam/jo/
├── ReferenceDataFunctionalTest.java                 # unsupported list: 11 names + variants; missing-token path (R9)
├── JurisdictionsFunctionalTest.java                 # ac011 renamed: unreferenced by the location data (R10)
└── TicketCategoriesFunctionalTest.java              # NEW: 011 AC-001 … AC-022 over real HTTP (R10)
src/functionalTest/resources/golden/
└── ticket_categories.json                           # NEW: captured collection response (AC-005)

src/smokeTest/java/uk/gov/hmcts/ctam/jo/
└── ReferenceDataSmokeTest.java                      # + TICKET_CATEGORIES (R9)

postman/ctam-jomockapi.postman_collection.json       # + Reference Data › Ticket Categories; unsupported example → ticket_category_types
README.md                                            # ten types; five reference fields; Skills referenced; DF-8 closed; DF-10
CLAUDE.md                                            # § Current repo state: + 011, 010 DF-8 closed; § Reference data
specs/002-reference-data-api/contracts/reference-data-api.md    # two pointers to the 011 delta
specs/002-reference-data-api/quickstart.md                      # row 5: ticket_categories -> ticket_category_types
specs/010-tickets-reference-data/spec.md                        # DF-8: closed by 011
```

**Structure Decision**: The existing single Gradle/Spring Boot project, unchanged in layout. Four main Java files change in code and one in Javadoc only; no main class is added.

## Complexity Tracking

> Deviations from constitution MUSTs, each documented per Governance.

No new deviation from a constitution MUST. One new contract deviation, D-6 (`parent_category_id`), recorded in the spec with its reason. The Sonar CI deferral recorded in [002's plan](../002-reference-data-api/plan.md#complexity-tracking) remains open and unchanged by this feature.

## Post-Design Constitution Re-Check

Re-run after Phase 1 (research.md, data-model.md, contracts/reference-data-api-ticket-categories.md, quickstart.md).

The design adds one enum value and its plumbing, a configuration entry, a fixture and tests, with no new dependency, layer, endpoint or main class. Four details strengthen the assessments:
- **Byte-identical responses (V, VII)**: `parent_category_id` is written only when the type declares it, and no existing fixture changes, so the existing golden files prove the other types are unaffected.
- **References and agreement (IV)**: the completeness check, with the per-type target override, requires exactly the three new resolution rows (`tickets.ticket_category_id`, `ticket_categories.parent_category_id`, `ticket_categories.jurisdiction_id`) and none for `ticket_categories.type_id`, whose target is unserved. The self-referencing row runs the existing cycle test. The parent-jurisdiction agreement skips the 8 top-level records because their `parent_category_id` is a present `null`, while a missing key still fails.
- **The override is contained (VII, XII)**: it is a two-level table in `ReferenceDataReferencesTest` and the matching pin table in `ReferenceDataFixtureRulesTest`; no main code or configuration knows about it.
- **Guard coverage (VII)**: after the change, main sources contain `parent_category_id`, `PARENT_CATEGORY_ID`, `parentCategoryId`, "parent category", `ticket_category_id` and "ticket category"; none matches the plural fragment `ticket_categories`, and none matches the existing fragments.

The quickstart's expected values were checked against a collection simulated from the extract by the data-model rules: 54 records; ids from 31 to 88; first `[31,"Civil",1,30,null]` and last `[88,"Languages",5,87,null]`; 7 ended ids; 53 distinct names; 8 top-level records; 0 child categories whose jurisdiction differs from their parent's; every ticket's category id among the served ids; every `type_id` an id in the type extract. The real fixture is generated during implementation and checked again then.

Every principle stays **Pass**. Open item: the inherited Sonar deferral.

## Implementation notes

**Baseline (T001, before any change)**: `./gradlew check -PskipOwasp` green. Tests per suite: `test` 450, `integrationTest` 33, `functionalTest` 505, `smokeTest` 11. JaCoCo: lines 536/536 (100%), branches 239/240 (99.6%; the missed branch is MapStruct's generated null check on the boxed `id`).

**Generic field (T002–T009)**: built test-first. With only the T002–T005 test changes, `compileTestJava` failed on the missing `PARENT_CATEGORY_ID`, `getParentCategoryId`, `parentCategoryId` and the 13-argument `ReferenceDataResponse` constructor, as intended. After T006–T008, `./gradlew check -PskipOwasp` passed (`test` 463, +13: registry 2, repository 8, mapper 1, controller 2; `integrationTest` 33; `functionalTest` 505; `smokeTest` 11), Checkstyle clean, and every existing golden file matched byte for byte. springdoc publishes `parent_category_id` as an optional, nullable `int64`, as asserted by the contract test. The 13 positional `ReferenceDataResponse` calls were updated by a small parser inserting the argument after the sixth, then re-wrapped where a line passed 120 characters; both edited class Javadocs were re-wrapped whole, with every `{@code …}` tag kept on one line.

**Fixture (T010)**: generated once from the gitignored `REF_TicketCategory.csv` by a throwaway script in the session scratchpad (never committed). Before the file was kept, the script confirmed every item of research R6:
- no BOM; the exact 9-column header; 54 data rows; ids unique and ascending, 31 to 88; names trimmed, 53 distinct;
- the five copied values equal the extract's, row for row (SC-003); the ended ids are exactly 38, 45, 52, 59, 66, 73 and 80;
- the 8 top-level ids; every parent is a top-level id in the file with the same `jurisdiction_id`;
- every `jurisdiction_id` is an id in `jurisdictions.json`; the `type_id` values are exactly 1 to 5, each an id in the local `REF_TicketCategoryType.csv` (5 types) (SC-004);
- every `ticket_category_id` in `tickets.json` (159) is an id in the new file;
- every reference point of data-model.md § 4.

UTF-8 without BOM, two-space indent, trailing newline, 15,062 bytes, no `\u`.

**Extension test (T017)**: `ReferenceDataExtensionIntegrationTest` passed unchanged (6 tests).

**Golden file (T024)**: the instance from 010 was stopped first. `golden/ticket_categories.json` was captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline, 11,832 bytes); its 54 records equal the fixture's, field for field and in key order. The nine existing golden files are unchanged (AC-020, SC-008).

**AC-014 (T032)**: the build-time half is met by T016: `everyReferenceBetweenServedTypesIsChecked` resolves `ticket_categories.type_id` through `TARGET_TYPE_OVERRIDES` to the unserved `ticket_category_types`, so it requires no row, while `locations.type_id` and `base_locations.type_id` still require theirs; `ReferenceDataFixtureRulesTest` pins the type ids to 1–5 through `PINNED_TYPE_REFERENCE_VALUES`, separately from the location data.

**AC-023 / SC-009 (T039)**: `git diff --stat origin/main -- src/main/java` lists exactly `ReferenceField`, `ReferenceDataRecord`, `ReferenceDataResponse`, `ReferenceDataMapper` and `ReferenceDataProperties` (Javadoc only; 36 insertions, 17 deletions). Under `src/main/resources`, only `application.yml` (the type entry and the `tickets` comment) and the new `reference-data/ticket_categories.json`. `git grep -n -i -E 'ticket_categories|ticket categories|TicketCategories' -- src/main/java` prints nothing. AC-024 is covered by T002–T005 (registry, repository, mapper, controller) and AC-025 by T008 and T013.

**Postman (T041)**: newman ran 102 requests (+11 in Reference Data › Ticket Categories) with 391 assertions (+43), 0 failures. The folder was inserted as text after Tickets, in the collection's own layout (checked by round-tripping the Tickets folder first); the only other change is the "unsupported" example renamed from `ticket_categories` to `ticket_category_types`. The diff is 620 insertions and those 3 changed lines.

**Final (T046)**: `./gradlew check -PskipOwasp` green. Tests per suite:
- `test` 486 (+36): the new field's unit cases (+13); one fixture-rules row, which adds three cases; `ReferenceDataReferencesTest` (+7: three reference rows, each run by the resolution and cycle tests, and one agreement row); and `NoTypeSpecificCodeTest` (+13: the `ticket_categories` fragment, six recognised spellings and six ignored field spellings).
- `integrationTest` 33 (the contract test's `enum`, description and `parent_category_id` schema changed).
- `functionalTest` 573 (+68): 64 in `TicketCategoriesFunctionalTest`; 002's unsupported-name list lost `ticket_categories` and gained four case variants (+3); and 010's `ac012` gained `TICKET_CATEGORIES` (+1).
- `smokeTest` 12 (+1: the ticket-categories collection).

JaCoCo: lines 538/538 (100%), branches 239/240 (99.6%). The two new lines are covered; the one missed branch is still MapStruct's generated null check on the boxed `id`.

**Strict latency (T046, `./gradlew smokeTest -Dperf.strict=true`)**: collection p95: `appointment_titles` 3 ms, `base_locations` 8 ms, `contract_types` 1 ms, `genders` 1 ms, `judiciary_roles` 1 ms, `locations` 3 ms, `location_types` 1 ms, `jurisdictions` 1 ms, `tickets` 1 ms, `ticket_categories` 1 ms (bound 100 ms).

**Quickstart (T047)**: §§ 1–5 walked through against `./gradlew bootRun`. Every expected result matched: § 2's scope checks, § 3 rows 1–21 (including 0 unresolved ticket categories, 0 parents that are not top-level or in another jurisdiction, and 0 records carrying `parent_category_id` in each of the other nine collections), § 4's `enum` (`ticket_categories` listed, `ticket_category` not) and `parent_category_id` schema (`["integer","null"]`), and § 5's newman run.

**Differences from the planned design**:
- As in 006–010, `TicketCategoriesFunctionalTest` was written in one pass (T018 and the story tasks), and the golden file was captured before the first full gate, so T019's gate already ran every story's functional test. T038's guard cases and T042's smoke path were also added before T019.
- The pin key for a per-type override is `type.field` (`ticket_categories.type_id`), and `everyPinnedReferenceValueIsUsed` compares the used values with the union of both pin tables under those keys.
- `JurisdictionsFunctionalTest.ac011`'s comment sits above `@Test`, matching the class's style.

**Code-review follow-ups (2026-10-30)**: one `/code-review` round found no correctness bugs in the served data. Six findings were fixed and one kept.
- **Fixed, stale Javadoc**: `TICKET_CATEGORY_IDS` no longer says it is pinned "until ticket_categories is served"; it says it is kept, as `JURISDICTION_IDS` was after 009, because `ReferenceDataReferencesTest` checks resolution while the pin fixes which ids the tickets use.
- **Fixed, the agreement skip is gated**: `everyAgreementHolds` skips a present `null` `via` only when the source type declares that field nullable (read from `application.yml` through a shared `configuredTypes()`, also used by the completeness check); a null in a non-nullable `via`, such as a location's `type_id`, fails here again, as 008 intended.
- **Fixed, the type comes from configuration**: `ReferenceDataFixtureRulesTest.Declaration` carries the type's name from `application.yml`, replacing the file-name-based `typeOf`, so a fixture whose file name differs from its type can't silently miss its per-type pin.
- **Fixed, one pin lookup**: `pinOf(type, field)` returns a `Pin(key, values)` (the type's own pin, keyed `type.field`, else the field's), replacing `pinnedValues` and `pinKey`; `everyPinnedReferenceValueIsUsed` uses it for both the values it collects and the expected union.
- **Fixed, no null-pointer failures**: `TicketCategoriesFunctionalTest` looks up a parent through `parentOf`, which fails naming the category if the parent isn't served.
- **Fixed, one shared helper**: `ReferenceDataHttp.integralField(context, record, field)` replaces `TicketCategoriesFunctionalTest.integral` and `JurisdictionsFunctionalTest.jurisdictionIdOf`.
- **Kept**: the per-type target override (`ReferenceDataReferencesTest`) and the per-type pin (`ReferenceDataFixtureRulesTest`) stay in their own tests. They do different jobs, and declaring targets in configuration is the option spec Clarifications Q1 declined.

`./gradlew check -PskipOwasp` green after the fixes, with unchanged counts: `test` 486, `integrationTest` 33, `functionalTest` 573, `smokeTest` 12. JaCoCo: lines 538/538, branches 239/240.

**Second code-review round (2026-10-30)**: no correctness bugs. Four findings were fixed and two kept.
- **Fixed**: `TicketCategoriesFunctionalTest.ac004` reads `parent_category_id` through `integralField`, so a missing or null parent fails naming the category instead of reading as 0.
- **Fixed**: `everyAgreementHolds` asserts that it compared at least one record, so a fixture whose `via` values were all null couldn't pass by skipping everything.
- **Fixed**: `ReferenceDataReferencesTest.configuredTypes()` reads `application.yml` once and caches it, instead of once per agreement row.
- **Fixed**: the README's "Tests to update for a new type" list gains a `ReferenceDataReferencesTest` bullet: the reference and agreement rows, and when a type needs a `TARGET_TYPE_OVERRIDES` entry and a matching per-type pin.
- **Kept**: the per-type rule stays in two tables, one per test (spec Clarifications Q1), and pins stay after their target is served (as `JURISDICTION_IDS` and `TICKET_CATEGORY_IDS` did); a per-type pin taking over a type's values from a field pin is what an override means, and no such overlap exists.

`./gradlew check -PskipOwasp` green after the fixes, with unchanged counts: `test` 486, `integrationTest` 33, `functionalTest` 573, `smokeTest` 12.
