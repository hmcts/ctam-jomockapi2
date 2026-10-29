# Research: Reference Data API — Tickets

**Feature**: `010-tickets-reference-data` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items; its one clarification settled the guard fragment. The design questions were:
- how the type is declared;
- how a fourth reference field is added generically, and where it sits in responses and the OpenAPI schema;
- how the new field is tested without disturbing the existing reference-field tests;
- how the fixture is generated and checked, on every build and once against the extract;
- how the forward reference is tracked until `ticket_categories` is served;
- how the type-specific-code guard covers the type;
- which existing tests and artifacts change.

Each decision follows the equivalent decision in 003 (reference fields), 007 (extract ids), 008 (a generic field plus a type) or 009 (the plural guard fragment) where one exists.

---

## R1. The type entry

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: tickets
        # Keeps the extract's E-Links ids, so People authorisations' ticket_id values can resolve (010 DR-1).
        fixture: classpath:reference-data/tickets.json
        # ticket_category_id is a forward reference to ticket categories, not served yet (010 DF-8).
        reference-fields: [ticket_category_id]
        # Different categories share ticket names (010 EC-002).
        unique-names: false
```

No `aliases` (`ticket` is not served, spec FR-001), no nullable, boolean or text fields.

**Rationale**: Appending keeps the OpenAPI `enum` order stable: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `appointment_title`. `application.yml` is configuration, outside the type-specific-code guard, so its comments may name types, as every earlier entry's do.

**Alternatives considered**: placing the entry alphabetically — rejected, as in 009; it would reorder the published `enum`.

---

## R2. The fourth reference field

**Decision**:
- `ReferenceField` gains `TICKET_CATEGORY_ID("ticket_category_id", ReferenceDataRecord::getTicketCategoryId)`, after `JURISDICTION_ID`.
- `ReferenceDataRecord` gains `@JsonProperty("ticket_category_id") Long ticketCategoryId`, after `jurisdictionId`, with the same Javadoc pattern ("Null unless the type declares `ReferenceField#TICKET_CATEGORY_ID`. The repository decides whether a null or a missing key is allowed.").
- `ReferenceDataProperties`' Javadoc lists the four fields.

Nothing else in validation changes: `ReferenceDataTypeRegistry` resolves declared names through `DeclaredField.byName(ReferenceField.class, …)`, and `FixtureReferenceDataRepository.requireReferenceKeys` and `validateReferenceFields` iterate over `ReferenceField.values()` and the type's declared fields. The new field therefore gets every existing rule: required in every record of a declaring type, `null` only if declared nullable, a positive integer, and absent from every record of a type that doesn't declare it (spec FR-019).

**Rationale**: 003 built reference fields as a closed, typed set (so a typo in configuration fails at start-up, and the response is a typed DTO), and 008 confirmed that adding a member to such a set is the intended extension point. Constitution III: no new validation code.

**Alternatives considered**:
- A generic map of reference fields on the record and response — rejected: Principle VIII forbids `Map<String, Object>` DTOs, and it would be a refactor across 003–009.
- Serving the field without declaring it (an untyped pass-through) — rejected: undeclared fields fail start-up by design (002 FR-020).

---

## R3. Where the field sits

**Decision**: In responses, `ticket_category_id` follows `jurisdiction_id`, so the reference fields stay together in enum order, before the text and boolean fields: `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `ticket_category_id`, `integration_code`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date`. Ticket records, which declare only this field, are written `id`, `name`, `ticket_category_id`, then the dates (spec FR-005).

**Rationale**: `ReferenceDataFixtureRulesTest.fieldOrder` already orders declared reference fields by `ReferenceField.values()`, so putting the enum value last among reference fields and the JSON property in the same place keeps one ordering rule for fixtures and responses.

---

## R4. The response and OpenAPI schema

**Decision**: `ReferenceDataResponse` gains a `ReferenceFieldValue ticketCategoryId` component after `jurisdictionId`, annotated like `jurisdiction_id`:
- `@Schema(description = "Id of the record's ticket category, in E-Links' numbering. Present only for types whose records carry it (deviation D-5 from the E-Links schema).", implementation = Long.class, format = "int64", requiredMode = NOT_REQUIRED, example = "31")`, not nullable;
- `@JsonProperty("ticket_category_id")`, `@JsonInclude(NON_NULL)`;
- `@JsonPropertyOrder` gains `"ticket_category_id"` after `"jurisdiction_id"`;
- the class Javadoc names the new field and D-5.

`ReferenceDataMapper` gains `@Mapping(target = "ticketCategoryId", expression = "java(referenceField(record, ReferenceField.TICKET_CATEGORY_ID, type))")`. Because the mapper has `unmappedTargetPolicy = ERROR`, forgetting the mapping fails the build.

**Rationale**: Identical to the three existing reference fields (003, 007), so springdoc publishes it the same way: an optional `integer`/`int64`, not nullable, with no new schema component. `NON_NULL` plus "no value object for an undeclared field" leaves every other type's JSON unchanged byte for byte.

**Alternatives considered**: marking it nullable in the schema — rejected: no type declares it nullable, and `type_id` and `jurisdiction_id` set the precedent (nullable only where a type needs it, as `parent_id` does).

---

## R5. Unit tests for the new field (test-first)

**Decision**: Written before the main-code change, failing until it lands:
- **`ReferenceDataTypeRegistryTest`**: a type declaring `ticket_category_id` resolves to `ReferenceField.TICKET_CATEGORY_ID`; the existing "unknown or repeated field" cases are unchanged.
- **`FixtureReferenceDataRepositoryTest`**:
  - the existing constant `ALL_REFERENCE_FIELDS` becomes `FIRST_REFERENCE_FIELDS`, with a comment that it holds the three fields 003 added, so its eight existing fixtures stay unchanged;
  - new fixture `valid-ticket-category-reference-field.json` (two records with `ticket_category_id`), loaded by a type declaring only that field;
  - new invalid fixtures `ticket-category-id-missing.json` and `ticket-category-id-not-positive.json`, added to a `@CsvSource` with the messages `record id 5: ticket_category_id is required` and `record id 5: ticket_category_id must be greater than 0`;
  - an undeclared case: the new valid fixture under a type declaring only `type_id` fails with `ticket_category_id is not a reference field of widgets`;
  - an explicit `null` for the declared, non-nullable field fails with `ticket_category_id is required`;
  - a type declaring the field nullable loads an explicit `null` (new fixture `valid-nullable-ticket-category-reference-field.json`), so FR-019's nullable rule is proved for this field, not only through `parent_id`.
- **`ReferenceDataMapperTest`**: the field is mapped for a declaring type and left out (no value object) otherwise.
- **`ReferenceFieldValueTest`**: `keepsTheFieldOrderWithEveryField` covers all four reference fields in order.
- **`ReferenceDataControllerTest`**: a record carrying `ticket_category_id` is written with it straight after `name`.
- **`ReferenceDataFixtures`**: the 8 positional `ReferenceDataResponse` calls gain a `null` argument after `jurisdictionId`, and a helper for a ticket-category response is added. `ReferenceFieldValueTest` (2 calls) and `ReferenceDataMapperTest` (1 call) are updated the same way: 11 calls in total.

**Rationale**: Mirrors 008's T002–T006, which added the text field test-first. Renaming the constant rather than extending it keeps the existing fixtures and messages exactly as they are, so the diff shows only additions for the new field.

**Alternatives considered**: extending `ALL_REFERENCE_FIELDS` to four fields — rejected: all eight fixtures used with it (valid, missing, not positive, explicit null and the nullable variants) would need a `ticket_category_id` added, for no extra coverage.

---

## R6. Generating the fixture

**Decision**: A throwaway script in the session scratchpad (never committed) reads the local, gitignored `joh-elinks-api/ReferenceData/REF_Ticket.csv` with a CSV parser, takes `id`, `name` and `ticket_category_id` from each row, and writes `src/main/resources/reference-data/tickets.json` by data-model.md § 4's rules. Before the file is kept, the script checks:
- the header is exactly `id,name,ticket_category_id,start_date,end_date,created_at,updated_at`; 159 data rows of 7 columns; no BOM;
- ids are unique, ascending integers from 289 to 1709; names are non-blank and trimmed (152 distinct); every `ticket_category_id` is a positive integer;
- the three copied values equal the extract's, row for row (SC-003);
- the ended ids are exactly spec AC-003's 22;
- the 41 distinct category ids are exactly `TICKET_CATEGORY_IDS` (R7), and each is an `id` in the local `REF_TicketCategory.csv` (54 categories) (FR-011, SC-004);
- every row of the spec's Reference Points table and the 7 repeated-name pairs of EC-002.

The plan's implementation notes record the result.

**Rationale**: As in 003–009: the extract is used once, and the build never depends on it (FR-007). Checking the category ids against the ticket-category extract now is the strongest check possible while `ticket_categories` isn't served.

---

## R7. Fixture rules on every build

**Decision**: `ReferenceDataFixtureRulesTest`:
- **New row**: `new Fixture("tickets.json", 159, 152, Map.of(), IdRule.SOURCE, …)`, with the declaration taken from `application.yml` (`reference-fields: [ticket_category_id]`, `unique-names: false`) and reference points 289, 290, 294, 295, 365, 367, 409, 410 and 1709 from the spec's table, each `withReferences(categoryId)`.
- **`TICKET_CATEGORY_IDS`**: a new constant holding the 41 values (31, 32, 33, 36, 37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 49, 50, 52, 54, 55, 56, 57, 58, 59, 62, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 80, 82, 83, 85, 88), with Javadoc saying they are the ticket-category ids the shipped tickets refer to, pinned until `ticket_categories` is served (DF-8). As `JURISDICTION_IDS` is for `jurisdiction_id`, every `ticket_category_id` in a fixture that declares the field must be one of them; in addition, the values `tickets.json` uses must be exactly this set, so a dropped category is caught too.
- The `SOURCE` id rule, the date rule by position, the field order and the literal-character check apply unchanged.
- After code review, both pinned sets sit in one map, `PINNED_REFERENCE_VALUES`, checked generically for every declared reference field (plan.md § Implementation notes).

**Rationale**: `JURISDICTION_IDS` did this job for `jurisdiction_id` before 009 served jurisdictions. Without it, a stray category id in the fixture would pass every build until DF-8 is closed.

---

## R8. References on every build

**Decision**: `ReferenceDataReferencesTest.TARGET_TYPES` gains `"ticket_category_id", "ticket_categories"`. `references()` gains no row: `ticket_categories` is not served, so the completeness check requires none yet. The class Javadoc's sentence about forward references already describes this case and is unchanged.

**Rationale**: The completeness check asserts a target for every declared reference field (`target type of ticket_category_id` would fail without the mapping) and requires a row only for served targets. So the mapping is needed now, and the row becomes mandatory as soon as the `ticket_categories` feature configures its type, which is exactly DF-8's closing duty.

---

## R9. The guard and the other tests that change

**Decision**:
- **`NoTypeSpecificCodeTest`**: fragments gain `tickets`, and the comment above the list says the plural is used for this type as for jurisdictions, because the generic field `ticket_category_id` contains the singular (010 Clarifications). New cases `theGuardRecognisesEveryTicketsSpelling` over `tickets`, `Tickets`, `TICKETS`, `myTickets`, `TicketsHelper` and `XTICKETS`, and `theGuardIgnoresTheGenericTicketCategoryField` over `ticket_category_id`, `ticketCategoryId`, `TICKET_CATEGORY_ID`, `getTicketCategoryId` and `Id of the record's ticket category`.
- **`ContractPaths`** (test fixtures): `+ TICKETS`.
- **`ReferenceDataOpenApiContractTest`**:
  - `ATTRIBUTE_NAMES` gains `tickets` after `jurisdictions`, and the description string gains it;
  - the `ReferenceDataResponse` property list gains `ticket_category_id` after `jurisdiction_id`;
  - the loop over reference properties covers it (optional `integer`, `int64`);
  - it is asserted not nullable, and its description cites D-5.
- **`ReferenceDataFunctionalTest`**:
  - the unsupported-name list loses `tickets` (now 12 E-Links names) and gains the case variants `Tickets`, `TICKETS` and `tickets_` (EC-008);
  - its comment says so and adds 010 to its list of covered scenarios;
  - the missing-token test's unsupported path changes from `/tickets` to `/ticket_categories` (AC-018).
- **`ReferenceDataSmokeTest`**: `+ TICKETS` in the collection latency guard.
- **`ReferenceDataExtensionIntegrationTest`**: expected to pass unchanged.

**Rationale**: As in 009. Checked against the planned main sources: after R2–R4 they contain `ticket_category_id`, `TICKET_CATEGORY_ID`, `ticketCategoryId`, `getTicketCategoryId` (Lombok, not in source) and "ticket category", none of which the plural fragment matches.

---

## R10. Functional tests, golden file and Postman

**Decision**:
- **`TicketsFunctionalTest`** (new, `src/functionalTest`): AC-001 to AC-020 over real HTTP with `ReferenceDataHttp`, mirroring `LocationTypesFunctionalTest` and `JurisdictionsFunctionalTest`. Constants: `SINGULAR`, the field list, a `Row(id, name, ticketCategoryId, ended)` list of the reference points, `ENDED_IDS` (the 22), and the 7 repeated-name pairs. AC-012 checks that the other eight collections never carry `ticket_category_id`. `golden/tickets.json` is captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline), and AC-005 compares against it.
- **Postman**: a "Reference Data › Tickets" sub-folder, after "Jurisdictions", with ten requests: collection; single record `294`; ended record `367`; unknown `297`; malformed `abc`; query parameter (`?ticket_category_id=31`); trailing slash; no token; invalid token; and `ticket`. Each request has status, structure and key-value tests and uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}` (AC-024). The existing "tickets - unsupported" request becomes "ticket_categories - unsupported", with the same tests. As in 008 and 009, the folder is inserted as text in the collection's own layout by a throwaway script.

---

## R11. Documentation

**Decision**:
- **README**: nine types and a `tickets` row (159, 152 distinct names; `ticket` returns `400`); a short paragraph on ticket records (extract ids, `ticket_category_id` as a forward reference to the unserved `ticket_categories`, 7 repeated names); the configuration example's `reference-fields` comment ("any of the four"); the guard bullet mentions `tickets` beside `jurisdictions`.
- **CLAUDE.md**: § Current repo state gains 010; § Reference data adds ticket names, ids and category ids to what was taken from the extracts, and the data-model § list gains 010's § 4.
- **002 contract**: one more "Extended by 010" pointer; **002 quickstart** row 5 changes from `$BASE/tickets` to `$BASE/ticket_categories`.
- **Contract delta**: `contracts/reference-data-api-tickets.md`.
- No earlier spec's deferred item changes: this feature opens DF-8 and DF-9 and closes none.
