# Research: Reference Data API — Ticket Category Types

**Feature**: `012-ticket-category-types-reference-data` | **Date**: 2026-10-30 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec has no open question: its one clarification chose `ticket_category_type` as the unsupported-name example (FR-021). The design questions were:
- how the type is declared;
- how the fixture is generated and checked;
- how the existing reference, agreement and pin tables take the type;
- how the guard covers it, and which existing tests and artifacts change.

There is no main-code change. Every capability the type needs already exists: a declared non-nullable `jurisdiction_id` and a text field `integration_code` (008), repeated names (003), extract ids (007), and the per-type target override for `type_id` (011). Each decision follows 008 (the same record shape), 009 (a config-only type that closes forward references) or 011 (the override and pins) where one exists.

---

## R1. The type entry

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`, in `location_types`' style:

```yaml
      - name: ticket_category_types
        # Keeps the extract's E-Links ids, so every ticket category's type_id resolves (012 DR-1).
        fixture: classpath:reference-data/ticket_category_types.json
        # The id of a jurisdiction, checked by ReferenceDataReferencesTest.
        reference-fields: [jurisdiction_id]
        # The extract's integration code (008 D-4).
        text-fields: [integration_code]
        # "Court Type" is both a Courts and a Magistrates type (012 EC-002).
        unique-names: false
```

The `ticket_categories` entry's comment changes from "type_id is a forward reference to ticket category types, not served yet (011 DF-10); jurisdiction_id and parent_category_id are ids of …" to "type_id, jurisdiction_id and parent_category_id are ids of a ticket category type, a jurisdiction and another ticket category, checked by ReferenceDataReferencesTest (012)." (spec FR-017).

**Rationale**: Appending keeps the OpenAPI `enum` order stable, with `ticket_category_types` after `ticket_categories` and before the alias `appointment_title`. No `nullable-reference-fields` or `boolean-fields` key, as for `location_types`.

**Alternatives considered**: none; the shape is `location_types`' without `type_id`.

---

## R2. Generating the fixture

**Decision**: A throwaway script in the session scratchpad (never committed) reads the local, gitignored `joh-elinks-api/ReferenceData/REF_TicketCategoryType.csv` with a CSV parser and writes `src/main/resources/reference-data/ticket_category_types.json` by data-model.md § 3's rules. Before the file is kept, the script checks:
- the header is exactly `id,name,jurisdiction_id,integration_code,start_date,end_date,created_at,updated_at`; 5 data rows of 8 columns; no BOM;
- ids are the integers 1 to 5, ascending; names non-blank and trimmed (4 distinct); `jurisdiction_id` positive integers; `integration_code` non-blank, trimmed, matching `[A-Z_]+` and distinct;
- the four copied values equal the extract's, row for row (SC-003); no record is ended;
- every `jurisdiction_id` is an id in the checked-in `jurisdictions.json`;
- every `type_id` in the checked-in `ticket_categories.json` is an id in the new file, and each category's `jurisdiction_id` equals its type's (closes DF-10);
- every row of data-model.md § 3's Records table.

**Rationale**: As in 003 to 011; the extract is used once, and the build never depends on it (FR-007).

---

## R3. Fixture rules on every build

**Decision**: `ReferenceDataFixtureRulesTest`:
- **New row**, after `ticket_categories.json`: `new Fixture("ticket_category_types.json", 5, 4, Map.of(), IdRule.SOURCE, …)` with every record as a reference point, each `point(id, name, null).withReferences(jurisdictionId).withTexts(integrationCode)`, as `location_types.json`'s row does. The declaration comes from `application.yml`, as for every row.
- **`JURISDICTION_IDS`**: values unchanged ({30, 34, 84, 86, 87}); the Javadoc's "87 Skills (by ticket category 88 only, 011)" becomes "87 Skills (by ticket category 88 and ticket category type 5; 011, 012)". The new type uses {30, 34, 84, 87}, so `everyPinnedReferenceValueIsUsed` still finds every pinned value used.
- **`TICKET_CATEGORY_TYPE_IDS`** and its `PINNED_TYPE_REFERENCE_VALUES` entry stay (spec FR-019). The Javadoc changes from "pinned until ticket_category_types is served (011 DF-10)" to "Kept after 012 served ticket_category_types, as `TICKET_CATEGORY_IDS` was after 011: ReferenceDataReferencesTest checks that the ids resolve, and this pins which ones the categories use."
- The `integration_code` format (`TEXT_FORMATS`, `[A-Z_]+`) already applies to every type that declares the field.

**Rationale**: Every record is a reference point, as for `jurisdictions.json` after 009's review, so the unit suite catches any changed value without the functional suite.

**Alternatives considered**: dropping `TICKET_CATEGORY_TYPE_IDS` now its target is served. Rejected for consistency with 009 and 011, which kept their pins (and as the description asks).

---

## R4. References and agreement on every build

**Decision**: `ReferenceDataReferencesTest`:
- **New rows** in `references()`:
  ```java
  // Closes 011 DF-10.
  new Reference("ticket_categories", "type_id", "ticket_category_types"),
  new Reference("ticket_category_types", "jurisdiction_id", "jurisdictions")
  ```
- **Agreement**: `agreements()` gains `new Agreement("ticket_categories", "type_id", "ticket_category_types", "jurisdiction_id")`. `type_id` isn't nullable for ticket categories, so the null-via skip never applies and all 54 records are compared (`compared > 0` holds).
- **`TARGET_TYPES` and `TARGET_TYPE_OVERRIDES`** are unchanged. `everyReferenceBetweenServedTypesIsChecked` now demands both rows, since `targetOf("ticket_categories", "type_id")` is `ticket_category_types`, now served, and `jurisdiction_id`'s default target is `jurisdictions`. The override's Javadoc stays accurate.

**Rationale**: As 008's and 011's agreement rows. The override (011 Clarifications) is what makes the new `type_id` row point at `ticket_category_types` rather than `location_types` (spec EC-009).

---

## R5. The guard

**Decision**: `NoTypeSpecificCodeTest`'s fragments gain `ticket_category_types`, and the comment on the plural fragments names it. New cases:
- `theGuardRecognisesEveryTicketCategoryTypesSpelling` over `ticket_category_types`, `TicketCategoryTypes`, `TICKET_CATEGORY_TYPES`, `ticket category types`, `myTicketCategoryTypes` and `XTICKET_CATEGORY_TYPES`;
- `theGuardIgnoresOtherTicketCategoryNames` over `ticket_category_id`, `ticketCategoryId`, `ticket_categories`, `TicketCategories` and `the record's ticket category`, each not matched by the new fragment.

**Rationale**: As 009 to 011 (spec FR-020). `git grep -i -E 'ticket.?category.?types?'` over `src/main/java` finds nothing today, so the guard passes with no exemption.

---

## R6. Other tests that change

**Decision**:
- **`ContractPaths`**: `+ TICKET_CATEGORY_TYPES`.
- **`ReferenceDataOpenApiContractTest`**: `ATTRIBUTE_NAMES` gains `ticket_category_types` after `ticket_categories`; the description gains it; the `ReferenceDataResponse` assertions are unchanged.
- **`ReferenceDataFunctionalTest`**:
  - the unsupported list loses `ticket_category_types` (now 10 names, all singular) and gains `Ticket_Category_Types`, `TICKET_CATEGORY_TYPES`, `ticket-category-types` and `ticket_category_types_` (EC-008); its comment says the 10 names are the unserved singular names and cites 012 AC-020 and EC-008;
  - the missing-token path changes from `/ticket_category_types` to `/ticket_category_type` (FR-021).
- **`TicketsFunctionalTest.ac012`** and **`TicketCategoriesFunctionalTest.fr014`**: the "other types don't carry the field" lists gain `TICKET_CATEGORY_TYPES` (AC-019).
- **`JurisdictionsFunctionalTest`**: the comment above `ac011SkillsIsServedAndUnreferencedByTheLocationData` becomes "Ticket category 88 (011) and ticket category type 5 (012) refer to Skills; the location data doesn't."; assertions unchanged (FR-014).
- **`ReferenceDataSmokeTest`**: `+ TICKET_CATEGORY_TYPES`.
- **`ReferenceDataExtensionIntegrationTest`**: expected to pass unchanged.

---

## R7. Functional tests, golden file and Postman

**Decision**:
- **`TicketCategoryTypesFunctionalTest`** (new): AC-001 to AC-021 over real HTTP, mirroring `JurisdictionsFunctionalTest` (record shape) and `TicketCategoriesFunctionalTest` (references). Constants: `SINGULAR`, the field list, and a `Row(id, name, jurisdictionId, integrationCode)` list of all five records. AC-010 loads `ticket_categories` and checks every `type_id` resolves and the per-type counts; AC-011 checks every category's `jurisdiction_id` equals its type's; AC-012 checks every type's `jurisdiction_id` is a served jurisdiction and that 5 refers to 87. `golden/ticket_category_types.json` is captured from `./gradlew bootRun`.
- **Postman**: a "Reference Data › Ticket Category Types" sub-folder after "Ticket Categories", with ten requests: collection; single `3`; repeated name `4`; unknown `6`; malformed `abc`; query parameter (`?jurisdiction_id=30`); trailing slash; no token; invalid token; `ticket_category_type`. The request "ticket_category_types - unsupported (contract-valid, out of scope)" becomes "ticket_category_type - unsupported (unserved singular)", with `raw` and `path` changed and the same tests. Inserted as text by a throwaway script, as in 008 to 011.

**Rationale**: Ten requests, as 009's folder: no ended record to show. The repeated-name request replaces the ended one.

---

## R8. Documentation

**Decision**:
- **README**: "eleven types", every E-Links canonical name served; a `ticket_category_types` row (5, 4 distinct names; `ticket_category_type` returns `400`); the jurisdictions paragraph's last sentence becomes "Skills (87) is referenced by the ticket category Languages and the ticket category type Skills Category."; the ticket-category paragraph's forward-reference sentence becomes "Every `type_id` is the id of a served ticket category type in the same jurisdiction."; a ticket-category-type paragraph (extract ids 1 to 5, `jurisdiction_id` and `integration_code` as for location types, "Court Type" twice, none ended); the guard bullet's example adds `ticket_category_types`; line 40's "ten types, from appointment titles to ticket categories" becomes "all eleven E-Links types".
- **CLAUDE.md**: § Current repo state gains 012, and the 011 line's DF-10 becomes "DF-10 (closed by 012)"; § Reference data adds ticket-category-type names, ids, `jurisdiction_id` and `integration_code` values, and 012's data-model § 3.
- **002 contract**: an "Extended by 012" pointer on the attribute-name line (not the `ReferenceDataResponse` line, which is unchanged); **002 quickstart** row 5 changes from `$BASE/ticket_category_types` to `$BASE/ticket_category_type`.
- **011 spec**: DF-10 row gains "Closed by 012 ([012 FR-011, FR-012](../012-ticket-category-types-reference-data/spec.md); `ReferenceDataReferencesTest`)."
- **Contract delta**: `contracts/reference-data-api-ticket-category-types.md`.
