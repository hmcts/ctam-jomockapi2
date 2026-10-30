# Research: Reference Data API — Ticket Categories

**Feature**: `011-ticket-categories-reference-data` | **Date**: 2026-10-30 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec's one clarification settled where reference targets live: in the build's reference checks only, with a per-type override. The design questions were:
- how the type is declared;
- how a fifth, nullable reference field is added generically, and where it sits;
- how the new field is tested;
- how the fixture is generated and checked;
- how the per-type override, the parent-jurisdiction agreement and the pins work in the existing tests;
- how the guard covers the type, and which existing tests and artifacts change.

Each decision follows 007 (nullable parent, self-reference), 010 (a new reference field plus a type) or 009 (the plural guard fragment) where one exists.

---

## R1. The type entry

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: ticket_categories
        # Keeps the extract's E-Links ids, so every ticket_category_id and parent_category_id resolves (011 DR-1).
        fixture: classpath:reference-data/ticket_categories.json
        # type_id is a forward reference to ticket category types, not served yet (011 DF-10); jurisdiction_id and
        # parent_category_id are ids of jurisdictions and other ticket categories, checked by
        # ReferenceDataReferencesTest.
        reference-fields: [type_id, jurisdiction_id, parent_category_id]
        # Null for the 8 top-level categories (011 FR-005).
        nullable-reference-fields: [parent_category_id]
        # "Immigration and Asylum Chamber" is in two tiers (011 EC-002).
        unique-names: false
```

The `tickets` entry's DF-8 comment changes to say `ticket_category_id` is the id of a ticket category, checked by `ReferenceDataReferencesTest` (011).

**Rationale**: Appending keeps the OpenAPI `enum` order stable, with `ticket_categories` after `tickets`. The order of names in `reference-fields` doesn't matter: the response order follows the enum (R3).

---

## R2. The fifth reference field

**Decision**:
- `ReferenceField` gains `PARENT_CATEGORY_ID("parent_category_id", ReferenceDataRecord::getParentCategoryId)`, after `TICKET_CATEGORY_ID`.
- `ReferenceDataRecord` gains `@JsonProperty("parent_category_id") Long parentCategoryId`, after `ticketCategoryId`, with the Javadoc used for `parentId` ("Null unless the type declares `ReferenceField#PARENT_CATEGORY_ID`, or declares it nullable and the record has no value. The repository decides whether a null or a missing key is allowed.").
- `ReferenceDataProperties`' Javadoc lists the five fields.

Validation needs no change: the registry resolves declared and nullable names through `DeclaredField.byName`, and the repository iterates over `ReferenceField.values()` (R2 of 010).

**Rationale and alternatives**: As 010 R2. Reusing `parent_id` was rejected: the extract and E-Links name the field `parent_category_id` (spec Assumptions), and `parent_id`'s target is `locations`.

---

## R3. Where the field sits

**Decision**: `parent_category_id` follows `ticket_category_id`, keeping all reference fields together in enum order: `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `ticket_category_id`, `parent_category_id`, `integration_code`, `salaried`, then the dates. Ticket-category records, which declare `type_id`, `jurisdiction_id` and `parent_category_id`, are written `id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, then the dates (spec FR-005).

**Rationale**: One ordering rule for fixtures (`ReferenceDataFixtureRulesTest.fieldOrder`) and responses.

---

## R4. The response and OpenAPI schema

**Decision**: `ReferenceDataResponse` gains a `ReferenceFieldValue parentCategoryId` component after `ticketCategoryId`, annotated like `parent_id`:
- `@Schema(description = "Id of the record's parent category, in E-Links' numbering. Present only for types whose records carry it (deviation D-6 from the E-Links schema). Null for a top-level record of a type that declares it nullable.", implementation = Long.class, format = "int64", requiredMode = NOT_REQUIRED, nullable = true, example = "48")`;
- `@JsonProperty("parent_category_id")`, `@JsonInclude(NON_NULL)` (a `ReferenceFieldValue` holding null is written as `null`, as for `parent_id`);
- `@JsonPropertyOrder` gains `"parent_category_id"` after `"ticket_category_id"`;
- the class Javadoc names the field and D-6.

`ReferenceDataMapper` gains `@Mapping(target = "parentCategoryId", expression = "java(referenceField(record, ReferenceField.PARENT_CATEGORY_ID, type))")`.

**Rationale**: Identical to `parent_id` (007), which a shipped type declares nullable; springdoc publishes it as an optional, nullable `int64`.

---

## R5. Unit tests for the new field (test-first)

**Decision**: Written before the main-code change, failing until it lands, mirroring 010's T002–T005 and its review follow-ups:
- **`ReferenceDataTypeRegistryTest`**: `resolvesTheDeclaredReferenceFields` declares all five names and expects enum order; a new case resolves `parent_category_id` alone; `resolvesTheDeclaredNullableReferenceFields` gains a case where `parent_category_id` is declared nullable.
- **`FixtureReferenceDataRepositoryTest`**, with new fixtures under `src/test/resources/reference-data/`:
  - `valid-parent-category-reference-field.json` (two records, one with `null`), loaded by a type declaring the field nullable;
  - `parent-category-id-missing`, `-not-positive` and `-explicit-null` (the last under a type that declares it but not nullable) in a `@CsvSource`, with 010's messages;
  - `parent-category-id-fractional`, `-string` and `-empty-string` added to `failsStartupOnUnparseableFixtures`;
  - an undeclared case: the valid fixture under a type declaring no reference field fails with `parent_category_id is not a reference field of widgets`;
  - `recordsOfATypeWithoutReferenceFieldsCarryNone` asserts `getParentCategoryId()` is null.
- **`ReferenceDataMapperTest`**: `mapsEveryReferenceFieldTogether` carries all five; a new case maps a present null for a nullable `parent_category_id`.
- **`ReferenceFieldValueTest`**: `keepsTheFieldOrderWithEveryField` covers all five.
- **`ReferenceDataControllerTest`**: a record carrying `parent_category_id` writes it after `ticket_category_id`'s place, and a null value is written as `"parent_category_id":null`.
- **`ReferenceDataFixtures`**: the 9 positional calls gain a `null` argument after `ticketCategoryId`; `parentCategoryType()` (declares `PARENT_CATEGORY_ID`, nullable) and `parentCategoryResponse(id, name, Long parentCategoryId)` are added. `ReferenceFieldValueTest` (2 calls) and `ReferenceDataMapperTest` (2 calls) are updated the same way: 13 calls in total.

**Rationale**: As 010. The three-field helpers and `everyReferenceType()` stay as 010 left them; `everyReferenceType()` picks up the fifth field automatically.

---

## R6. Generating the fixture

**Decision**: A throwaway script in the session scratchpad (never committed) reads the local, gitignored `joh-elinks-api/ReferenceData/REF_TicketCategory.csv` with a CSV parser and writes `src/main/resources/reference-data/ticket_categories.json` by data-model.md § 4's rules. Before the file is kept, the script checks:
- the header is exactly `id,name,type_id,jurisdiction_id,parent_category_id,start_date,end_date,created_at,updated_at`; 54 data rows of 9 columns; no BOM;
- ids unique, ascending integers from 31 to 88; names non-blank and trimmed (53 distinct); `type_id` and `jurisdiction_id` positive integers; `parent_category_id` a positive integer or blank;
- the five copied values equal the extract's, row for row (SC-003);
- the ended ids are exactly spec AC-003's 7;
- every parent is a top-level category in the file, with the same `jurisdiction_id`; the 8 top-level ids match the spec;
- every `jurisdiction_id` is a served jurisdiction id (from the checked-in `jurisdictions.json`);
- every `type_id` is an `id` in the local `REF_TicketCategoryType.csv` (5 types) (FR-012, SC-004);
- every `ticket_category_id` in the checked-in `tickets.json` is an id in the new file (closes DF-8);
- every reference point in data-model.md § 4.

**Rationale**: As in 003–010; the extract is used once, and the build never depends on it (FR-007).

---

## R7. Fixture rules on every build

**Decision**: `ReferenceDataFixtureRulesTest`:
- **New row**: `new Fixture("ticket_categories.json", 54, 53, Map.of(), IdRule.SOURCE, …)` with the declaration from `application.yml` and the reference points of data-model.md § 4, each `withReferences(typeId, jurisdictionId, parentCategoryId)` in enum order (`null` for a top-level parent).
- **`JURISDICTION_IDS`** gains 87, and its Javadoc becomes "The jurisdictions the shipped data refers to: 30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners, 87 Skills (by ticket category 88 only, 011)."
- **Per-type pins**: a new `PINNED_TYPE_REFERENCE_VALUES = Map.of("ticket_categories", Map.of("type_id", TICKET_CATEGORY_TYPE_IDS))`, with `TICKET_CATEGORY_TYPE_IDS = Set.of(1L, 2L, 3L, 4L, 5L)` ("The ticket-category-type ids the shipped categories refer to, pinned until ticket_category_types is served (011 DF-10)."). A helper `pinnedValues(type, field)` returns the per-type entry if any, else `PINNED_REFERENCE_VALUES.get(field)`. `followsTheGenerationRules` uses it, with the type name taken from the fixture file name. `everyPinnedReferenceValueIsUsed` groups the values it collects by the same key (type and field for an override, field otherwise) and compares them with the union of both tables.
- `TICKET_CATEGORY_IDS` stays (spec FR-012; as `JURISDICTION_IDS` did after 009).

**Rationale**: Location data uses `type_id` 1 to 16 without a pin; adding `type_id` to the field-keyed table would wrongly pin the location data to 1 to 5. The per-type table is the pinning counterpart of R8's target override (spec Clarifications).

---

## R8. References and agreement on every build

**Decision**: `ReferenceDataReferencesTest`:
- **Per-type targets**: `TARGET_TYPES` stays the default; a new `TARGET_TYPE_OVERRIDES = Map.of("ticket_categories", Map.of("type_id", "ticket_category_types"))`, with Javadoc citing 011 FR-020; a helper `targetOf(type, field)`. `everyReferenceBetweenServedTypesIsChecked` uses the helper, so it still asserts a target for every declared field and now requires no row for `ticket_categories.type_id` (unserved target). `TARGET_TYPES` gains `"parent_category_id", "ticket_categories"`.
- **New rows** in `references()`:
  ```java
  // Closes 010 DF-8.
  new Reference("tickets", "ticket_category_id", "ticket_categories"),
  new Reference("ticket_categories", "parent_category_id", "ticket_categories"),
  new Reference("ticket_categories", "jurisdiction_id", "jurisdictions")
  ```
  The self-referencing row is run by `selfReferencesHaveNoCycles`.
- **Agreement**: `agreements()` gains `new Agreement("ticket_categories", "parent_category_id", "ticket_categories", "jurisdiction_id")`. `everyAgreementHolds` skips a record whose `via` value is present and `null` (a top-level record has nothing to agree with); a missing `via` key still fails through `integral`. The record Javadoc says so.

**Rationale**: The override is the clarified answer (spec Clarifications Q1). Skipping only a present null keeps 008's guarantee that an absent value can't pass silently.

**Alternatives considered**: an `Agreement` flag for nullable via — rejected; the JSON already distinguishes a present null from a missing key, and start-up validation allows a present null only for a nullable declared field.

---

## R9. The guard and the other tests that change

**Decision**:
- **`NoTypeSpecificCodeTest`**: fragments gain `ticket_categories`; the comment says jurisdictions', tickets' and ticket categories' fragments are plural because the generic fields `jurisdiction_id` and `ticket_category_id` contain the singular. New cases `theGuardRecognisesEveryTicketCategoriesSpelling` over `ticket_categories`, `TicketCategories`, `TICKET_CATEGORIES`, `ticket categories`, `myTicketCategories` and `XTICKET_CATEGORIES`; and `theGuardIgnoresTheGenericTicketCategoryFields` over `ticket_category_id`, `ticketCategoryId`, `TICKET_CATEGORY_ID`, `parent_category_id`, `parentCategoryId` and `Id of the record's ticket category`.
- **`ContractPaths`**: `+ TICKET_CATEGORIES`.
- **`ReferenceDataOpenApiContractTest`**: `ATTRIBUTE_NAMES` gains `ticket_categories` after `tickets`, and the description gains it; the property list gains `parent_category_id` after `ticket_category_id`; the reference loop covers it (`deviation D-6`); it is asserted nullable, with "top-level record" in its description.
- **`ReferenceDataFunctionalTest`**: the unsupported list loses `ticket_categories` (now 11 names) and gains `Ticket_Categories`, `TICKET_CATEGORIES`, `ticket-categories` and `ticket_categories_` (EC-010); the missing-token path changes from `/ticket_categories` to `/ticket_category_types`.
- **`ReferenceDataSmokeTest`**: `+ TICKET_CATEGORIES`.
- **`ReferenceDataExtensionIntegrationTest`**: expected to pass unchanged.

---

## R10. Functional tests, golden file and Postman

**Decision**:
- **`TicketCategoriesFunctionalTest`** (new): AC-001 to AC-022 over real HTTP, mirroring `TicketsFunctionalTest`. Constants: `SINGULAR`, the field list, `ENDED_IDS` (7), `TOP_LEVEL_IDS` (8), `UNREFERENCED_IDS` (13) and a `Row(id, name, typeId, jurisdictionId, Long parentCategoryId, ended)` list of the reference points. AC-010 loads `tickets` and checks every `ticket_category_id` resolves; AC-012 checks parents, top-level nulls (present, not missing) and depth; AC-013 checks jurisdictions resolve and agree with the parent's, and that 88 refers to 87; AC-014 checks the `type_id` counts; FR-014 checks that the other nine collections never carry `parent_category_id`, as 010's `ac012` did for `ticket_category_id`; and `TicketsFunctionalTest.ac012` gains `TICKET_CATEGORIES`, so each new field's "other types don't carry it" check covers every other served type. `golden/ticket_categories.json` is captured from `./gradlew bootRun`.
- **`JurisdictionsFunctionalTest`**: `ac011SkillsIsServedButUnreferenced` becomes `ac011SkillsIsServedAndUnreferencedByTheLocationData`, assertions unchanged; a comment notes that ticket category 88 refers to it (011).
- **Postman**: a "Reference Data › Ticket Categories" sub-folder after "Tickets", with eleven requests: collection; top-level `31`; child `56`; ended `38`; unknown `34`; malformed `abc`; query parameter (`?parent_category_id=48`); trailing slash; no token; invalid token; `ticket_category`. The "ticket_categories - unsupported" request becomes "ticket_category_types - unsupported", with the same tests. Inserted as text by a throwaway script, as in 008 to 010.

---

## R11. Documentation

**Decision**:
- **README**: ten types and a `ticket_categories` row (54, 53 distinct names; `ticket_category` returns `400`); the jurisdictions paragraph's last sentence becomes "Skills (87) is referenced only by the ticket category Languages."; the ticket paragraph's forward-reference sentences become "Every `ticket_category_id` is the id of a served ticket category."; a ticket-category paragraph (extract ids, `type_id` as a forward reference to the unserved `ticket_category_types`, `jurisdiction_id`, nullable `parent_category_id`, two-level hierarchy, one repeated name); the configuration example lists five reference fields ("any of the five"); the guard bullet's example adds `ticket_categories`.
- **CLAUDE.md**: § Current repo state gains 011, and the 010 line's DF-8 becomes "DF-8 (closed by 011)"; § Reference data adds ticket-category names and reference values and 011's data-model § 4.
- **002 contract**: "Extended by 011" pointers on the attribute-name line and the `ReferenceDataResponse` line; **002 quickstart** row 5 changes from `$BASE/ticket_categories` to `$BASE/ticket_category_types`.
- **010 spec**: DF-8 row gains "Closed by 011 ([011 FR-011](../011-ticket-categories-reference-data/spec.md); `ReferenceDataReferencesTest`)."
- **Contract delta**: `contracts/reference-data-api-ticket-categories.md`.
