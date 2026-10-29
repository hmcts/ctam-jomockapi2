# Research: Reference Data API — Location Types

**Feature**: `008-location-types-reference-data` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items. The design questions were:
- how a type declares a text field, and how start-up validation and the response treat it;
- where `integration_code` sits in the response and in the OpenAPI schema;
- how the build checks a fixture that keeps the extract's ids and carries a text field;
- how the build checks that `type_id` references resolve and that jurisdictions agree;
- which existing tests and artifacts change.

Each decision follows the equivalent decision in 004 (boolean fields) or 007 (extract ids, reference checks) where one exists.

---

## R1. The type entry

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: location_types
        # Keeps the extract's E-Links ids, so locations' and base locations' type_id values resolve (008 DR-1).
        fixture: classpath:reference-data/location_types.json
        # A forward reference to jurisdictions (008 DF-7).
        reference-fields: [jurisdiction_id]
        # The extract's integration code (008 D-4).
        text-fields: [integration_code]
        # "Region" and "AC" repeat (008 EC-002).
        unique-names: false
```

No `aliases` (`location_type` is not served, spec FR-001), no `boolean-fields` and no `nullable-reference-fields`. The comments on `base_locations` and `locations` change to say that `type_id` is now checked.

**Rationale**: Every declaration except `text-fields` is used exactly as earlier types use it. A single declared reference field is already supported: the registry, repository, mapper and fixture-rules test all work per field. Appending keeps the OpenAPI `enum` order stable: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `appointment_title`.

**Alternatives considered**: `unique-names: true` — rejected, the start-up check would refuse the fixture ("Region" ×3, "AC" ×2).

---

## R2. Declaring a text field

**Decision**: A third kind of declared field, modelled exactly on 004's boolean fields:
- **Entity**: a new enum `entity/TextField implements DeclaredField<String>`, with one constant, `INTEGRATION_CODE("integration_code", ReferenceDataRecord::getIntegrationCode)`. `DeclaredField`'s Javadoc lists `text-fields` next to the other two lists.
- **Configuration**: `ReferenceDataProperties.TypeProperties` gains `List<String> textFields`, appended as the last component and copied like the other lists (null → empty).
- **Registry**: `ReferenceDataTypeRegistry.toType` checks the list with `requireNoEmptyEntry(name, "text-fields", …)` and resolves it with the existing `declaredFields(name, "text field", …, TextField.class)`. That already rejects empty entries, unknown names and duplicates, with messages naming the type and the field. The class Javadoc adds "text field" to its list of invalid declarations.
- **Type**: `ReferenceDataType` gains `Set<TextField> textFields`, appended to the constructor and held as an `EnumSet` through the existing `copyOf`.

**Rationale**: 004 set the pattern for "a field some types carry": an enum of known fields, a configuration list, resolution through `declaredFields`, and presence rules in the repository. Following it adds no new mechanism, keeps `NoTypeSpecificCodeTest` happy (the field name is not a type name), and lets the fixture-rules test read the declaration the way it reads the other two. Appending the new component and parameter last keeps every existing call site's argument order, as 007 did.

**Alternatives considered**:
- **A free-form map of extra string fields per record** (`Map<String, String>`): rejected by Constitution VIII (typed models, no `Map` payloads), and the OpenAPI schema couldn't name the field.
- **A dedicated `integrationCode` property with no declaration**: every type's response would need a rule for when to write it, which is type-specific behaviour (Constitution VII).
- **Reusing `ReferenceField` with a string value**: reference fields are ids pointing at another type, checked by `ReferenceDataReferencesTest`; mixing in free text would blur that rule.

---

## R3. Validating a text field at start-up

**Decision**:
- **Record**: `ReferenceDataRecord` gains `@JsonProperty("integration_code") @JsonSetter(nulls = Nulls.FAIL) String integrationCode`, so an explicit `null` fails while reading, as for `salaried`. The strict mapper's existing `Textual` coercion settings already reject a number or boolean where a string is expected, and Jackson rejects an object or array.
- **Repository**: a new `validateTextFields(fixture, type, record)` in `FixtureReferenceDataRepository`, called after `validateBooleanFields`. It first applies the shared `validateDeclaredFields(…, "text field", TextField.values(), type.getTextFields())`, which gives "required in each record of a type that declares it, absent from every other type's records" with the existing messages (`<field> is required`, `<field> is not a text field of <type>`). It then checks every present value: empty or blank → `<field> must not be blank`; leading or trailing whitespace → `<field> must be trimmed`. These match the existing `name` rules and messages.

**Rationale**: Presence is a parsed-record check, as for boolean fields: a text field is never nullable, so a missing key and an explicit null both mean "absent" (the latter already failed while reading). 007's JSON-tree check (`requireReferenceKeys`) exists only to tell a missing key from a null one, which text fields don't need. The blank and trimmed rules mirror `name`'s, so every text value the mock serves follows one rule (spec FR-019).

**Alternatives considered**:
- **Check presence on the JSON tree, like reference fields**: more code for no extra case.
- **Allow untrimmed or blank values**: spec FR-019 forbids them; the extract has none.
- **Check uniqueness**: not required (spec Clarifications, Q3).

---

## R4. The response and OpenAPI schema

**Decision**: `ReferenceDataResponse` gains, after `jurisdictionId` and before `salaried`:

```java
@Schema(description = "E-Links' code for the record's type, e.g. MAGS_AC_LJA. Present only for types whose records "
                      + "carry it (deviation D-4 from the E-Links schema).",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "MAGS_AC_LJA")
@JsonProperty("integration_code")
@JsonInclude(JsonInclude.Include.NON_NULL)
String integrationCode,
```

`@JsonPropertyOrder` becomes `id, name, type_id, parent_id, jurisdiction_id, integration_code, salaried, created_at, updated_at, start_date, end_date`. `ReferenceDataMapper` needs no change: MapStruct maps `integrationCode` by name, and `unmappedTargetPolicy = ERROR` proves the mapping exists at compile time.

**Rationale**: This is how 004 added `salaried`: a plain typed component, left out when null. Since start-up validation guarantees a declaring type's records all hold a value and other types' records hold none (R3), "left out when null" is the same as "present only for declaring types". So the other six types' responses are byte-for-byte unchanged, which the existing golden files prove. springdoc publishes a `String` component as `"type": "string"`, not nullable and not required, which is spec FR-013's "optional, non-nullable string". Placing it after the reference fields matches the spec's record order (FR-005); no type declares both a text field and a boolean field, so its position relative to `salaried` is only a convention.

**Alternatives considered**:
- **A `ReferenceFieldValue`-style wrapper**: only needed when null must be written, which never happens here.
- **Pass the type to the mapper and decide there**: unnecessary given R3's guarantee; 007's three-state rule exists only because `parent_id` can be null.

---

## R5. Generating the fixture

**Decision**: A throwaway script in the session scratchpad (never committed) reads the local, gitignored `joh-elinks-api/ReferenceData/REF_LocationType.csv` with a CSV reader, takes `id`, `name`, `jurisdiction_id` and `integration_code`, and writes `src/main/resources/reference-data/location_types.json` by data-model.md § 4's rules. Before the file is kept, the script checks:
- 16 rows, 9 columns per row (the trailing comma), ids 1 to 16, 13 distinct names;
- the four copied columns equal the extract's row for row (SC-003);
- integration codes match `^[A-Z_]+$`, 7 to 22 characters (EC-004);
- ended ids exactly {7, 14};
- every Location and BaseLocation `type_id` in the checked-in fixtures is an id, and each such record's `jurisdiction_id` equals its type's (FR-011, FR-012);
- every row of the spec's Records table.

The plan's implementation notes record the result.

**Rationale**: As in 003–007: the extract is used once, and the build never depends on it (FR-007).

---

## R6. Fixture rules on every build

**Decision**: `ReferenceDataFixtureRulesTest`:
- **New row**: `new Fixture("location_types.json", 16, 13, Map.of(), IdRule.SOURCE, …)` with reference points 1, 6, 7, 10, 11, 14 and 16 (spec Records table), each with `.withReferences(<jurisdiction>)` and a new `.withTexts(<code>)`.
- **Text fields**: `Declaration` gains `List<String> textFields` (read from `TypeProperties.textFields()` in `TextField` order). `fieldOrder` puts text fields after reference fields and before boolean fields. For every record, each declared text field must be a JSON string, non-empty and trimmed. `ReferencePoint` gains `List<String> texts`, compared like `references` and `booleans`, so a point that leaves out a declared text field fails.
- The `SOURCE` id rule, the date rule by position, the `JURISDICTION_IDS` check and the literal-character check apply unchanged.
- **Text format** (new): a `TEXT_FORMATS` constant, next to `JURISDICTION_IDS`, maps `integration_code` to `[A-Z_]{7,22}`. Every declared text field that has an entry must match it in full, so spec EC-004's code format is checked on every build, not only when the fixture is generated.

**Rationale**: 007 built the `SOURCE` rule for exactly this case. The text-field additions follow the boolean-field ones.

---

## R7. References and jurisdiction agreement on every build

**Decision**: `ReferenceDataReferencesTest`:
- **New reference rows**: `new Reference("locations", "type_id", "location_types")` and `new Reference("base_locations", "type_id", "location_types")`. `everyReferenceBetweenServedTypesIsChecked` then demands them as soon as `location_types` is configured, so they cannot be forgotten; `location_types.jurisdiction_id` → `jurisdictions` stays a forward reference (DF-7) until `jurisdictions` is served. Neither new row is self-referencing, so the cycle test skips them.
- **New agreement check** (FR-012): a second table, `Agreement(source, via, target, field)`, read as "every `source` record's `field` equals the `field` of the `target` record its `via` names". Rows: `("locations", "type_id", "location_types", "jurisdiction_id")` and `("base_locations", "type_id", "location_types", "jurisdiction_id")`. A parameterised test `everyAgreementHolds` fails naming the record, both values and the type.
- The class Javadoc adds the agreement rule.

**Rationale**: The fixtures are fixed, so a build-time check catches any disagreement before it is served (spec Assumptions; 007 Clarifications). A table keeps the test generic and lets the `jurisdictions` feature add its own rows.

**Alternatives considered**: a start-up check — rejected for the same reasons 007 gave for references; it would also need type-specific knowledge of which fields must agree.

---

## R8. Unit tests for the new capability

**Decision**:
- **`ReferenceDataTypeRegistryTest`**: `text-fields` resolves to `TextField.INTEGRATION_CODE`; unknown name, duplicate and empty entry each fail with the type and field named; absent means none.
- **`ReferenceDataTypeTest`**: the text-field set is copied and unmodifiable; null means none.
- **`FixtureReferenceDataRepositoryTest`**, with new fixtures under `src/test/resources/reference-data/`: a valid text field loads; for a declaring type, a missing key, explicit `null`, number, boolean, object, empty string, blank string and untrimmed string each stop start-up with the expected message; for a non-declaring type, a record carrying the field stops start-up.
- **`ReferenceDataMapperTest`**: `integrationCode` is copied, and stays null for a record without one.
- **Serialisation** (`ReferenceDataControllerTest` or the mapper test's JSON check): a response with `integrationCode` writes `"integration_code"` after `jurisdiction_id`; one without leaves the key out.
- **`ReferenceDataFixtures`**: the `TypeProperties` and `ReferenceDataType` helpers gain the new argument (empty), and a `textType` builder is added for the mapper's and repository's new cases.

**Rationale**: AC-022 and FR-017; every new branch (declared versus not, valid versus each invalid kind) gets a case, so the JaCoCo gate stays as it is.

---

## R9. Tests and artifacts that change

**Decision**:
- **`ContractPaths`** (test fixtures): `+ LOCATION_TYPES`.
- **`ReferenceDataOpenApiContractTest`**: `ATTRIBUTE_NAMES` gains `location_types` after `locations`; the description string gains it; the property order gains `integration_code` after `jurisdiction_id`; new assertions: `integration_code` is `"string"`, not nullable, not required, its description contains `deviation D-4`.
- **`ReferenceDataFunctionalTest`**: the unsupported-name list loses `location_types` (now 14 E-Links names), its comment says so, and it gains the case variants `Location_Types`, `LOCATION_TYPES`, `location-types`, `locationTypes` and `location_types_` (EC-008). It also adds 008 to its list of covered scenarios.
- **`NoTypeSpecificCodeTest`**: unchanged (spec FR-018). Its `location` fragment already matches `location_type`.
- **`ReferenceDataSmokeTest`**: `+ LOCATION_TYPES` in the collection latency guard.
- **`ReferenceDataExtensionIntegrationTest`**: expected to pass unchanged, as in 007.

---

## R10. Functional tests, golden file and Postman

**Decision**:
- **`LocationTypesFunctionalTest`** (new, `src/functionalTest`): AC-001 to AC-020 over real HTTP with `ReferenceDataHttp`, mirroring `LocationsFunctionalTest`'s structure; AC-010 and AC-011 load the three collections over HTTP and check resolution and agreement. `golden/location_types.json` is captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline), and AC-005 compares against it.
- **Postman**: a "Reference Data › Location Types" sub-folder, after "Locations", with ten requests: collection; single record `1`; ended record `14`; unknown `17`; malformed `abc`; query parameter; trailing slash; no token; invalid token; `location_type`. Each request has status, structure and key-value tests (including `integration_code`) and uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}` (AC-024).

---

## R11. Documentation

**Decision**:
- **README**: seven types; the `location_types` row; a paragraph on location-type records (`jurisdiction_id`, `integration_code`, extract ids); the base-location and location paragraphs say `type_id` now resolves; § Adding a type documents `text-fields` and the new field order.
- **CLAUDE.md**: § Current repo state gains 008; § Reference data adds location-type names, ids, jurisdiction codes and integration codes to what was taken from the extracts.
- **002 contract**: one more "Extended by 008" pointer.
- **003 spec (DF-1)** and **007 spec (DF-5)**: a one-line note that 008 closed them for `type_id`.
- **Contract delta**: `contracts/reference-data-api-location-types.md`.
