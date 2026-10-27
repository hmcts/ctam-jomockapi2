# Research: Reference Data API — Locations

**Feature**: `007-locations-reference-data` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items. The design questions were:
- how a type declares a nullable reference field, and how start-up validation treats it;
- how the fixture writes a missing parent;
- how one type's response writes `"parent_id": null` while every other type keeps leaving undeclared fields out;
- how the build checks a fixture whose ids aren't `10 × position`;
- how it checks that references resolve;
- which existing tests and artifacts change.

Each decision follows 003–006's equivalent where one exists.

---

## R1. The type entry

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: locations
        fixture: classpath:reference-data/locations.json
        reference-fields: [type_id, parent_id, jurisdiction_id]
        nullable-reference-fields: [parent_id]
        unique-names: false
```

No `aliases` (`location` is not served, spec FR-001) and no `boolean-fields`.

**Rationale**: `reference-fields` and `unique-names: false` are 003's declarations, used exactly as BaseLocation uses them. Appending keeps the OpenAPI `enum` order stable: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `appointment_title`.

---

## R2. Declaring a nullable reference field

**Decision**: A new optional list, `nullable-reference-fields`, on each type, defaulting to none:
- **Configuration**: `ReferenceDataProperties.TypeProperties` gains `List<String> nullableReferenceFields`, copied like the other lists.
- **Registry**: `ReferenceDataTypeRegistry.toType` resolves it with the existing `declaredFields(…, ReferenceField.class)`, which already rejects empty entries, unknown names and duplicates. One new check: every listed field MUST also be in the type's `reference-fields`, otherwise start-up fails with `Invalid reference-data type <name>: nullable reference field <field> is not a reference field` (spec FR-011).
- **Entity**: `ReferenceDataType` gains `Set<ReferenceField> nullableReferenceFields`, held as an `EnumSet` like the others.

**Rationale**:
- It reuses 003's pattern (a declared list, resolved against an enum, validated once at start-up), so there is no type-specific code and no new concept beyond "may be null".
- A separate list keeps `reference-fields` unchanged for BaseLocation and for every test that builds a type.
- Requiring the subset makes a typo, such as declaring a field nullable that the type never carries, fail at start-up rather than being silently ignored.

**Alternatives considered**:
- *A per-field map* (`reference-fields: {parent_id: nullable}`). Rejected: it changes the shape of an existing key, so BaseLocation's entry and every test that builds configuration would change for no gain.
- *Making every reference field nullable for every type.* Rejected: base locations' references must stay non-null, and the existing start-up failure for a missing reference is a guard 003 relies on (spec FR-011).

---

## R3. How the fixture writes a missing parent

**Decision**: The fixture writes `"parent_id": null` explicitly, so every Location record has the same nine keys in the same order, matching the response.

The rules the repository enforces, for every type:

| The type… | Key absent | `null` | Positive integer | Other value |
|-----------|-----------|--------|------------------|-------------|
| declares the field nullable | fail: `<field> is required` | accepted | accepted | fail, as today |
| declares it, not nullable | fail: `<field> is required` (as today) | fail: `<field> is required` | accepted | fail, as today |
| doesn't declare it | accepted (as today) | fail: `<field> is not a reference field of <type>` | fail (as today) | fail, as today |

**Implementation shape**:
- `ReferenceDataRecord` drops `@JsonSetter(nulls = Nulls.FAIL)` from the three reference fields, so an explicit null reaches validation instead of failing inside Jackson with an unhelpful message.
- The repository already reads each fixture as a tree for its date check (`requireDateStrings`). It gains a matching tree check for the reference-field keys: whether each key is present and whether it is null, judged by the type's declarations as in the table. Validation is the same; only where it runs changes.
- `boolean-fields` keep `Nulls.FAIL` (004 FR-018 is untouched).

**Rationale**:
- Writing null explicitly keeps the fixture shaped like the response, so `ReferenceDataFixtureRulesTest`'s field-order check (every record has exactly the type's fields, in order) needs no special case.
- Requiring the key for a nullable field catches a generator that drops fields.
- The existing fixture `invalid/reference-field-explicit-null.json` (`"parent_id": null` on a type that declares no reference fields) still fails, now in validation with `record id 5: parent_id is not a reference field of widgets`, so its test moves to the invalid-record table with that exact message (tasks T004). A new fixture covers a declared, non-nullable field set to `null` (`parent_id is required`).

**Alternatives considered**:
- *Leaving the key out for top-level locations.* Rejected: the Java record can't tell an absent key from a null one, so the repository couldn't require the key. The fixture would differ from the response, and the fixture-rules test would need a per-record exception.
- *Keeping `Nulls.FAIL` and switching it off per type.* Rejected: Jackson annotations are static, so per-type behaviour would need a custom deserializer, which is more code than one tree check.

---

## R4. Writing `"parent_id": null` for one type only

**Decision**: A reference field in `ReferenceDataResponse` has three states (absent, null, a value), so its three components change type from `Long` to a small value type in `domain/`:

```java
public record ReferenceFieldValue(@JsonValue Long value) { }
```

- **Serialising**: each component keeps `@JsonInclude(NON_NULL)`. A Java `null` component means the type doesn't declare the field, and it is left out, as today. `new ReferenceFieldValue(8L)` writes `8`, and `new ReferenceFieldValue(null)` writes `null`. BaseLocation's responses therefore stay byte-for-byte the same.
- **Mapping**: `ReferenceDataMapper` takes the type as a MapStruct `@Context`. For each reference field it returns `null` when the type doesn't declare the field, and otherwise `new ReferenceFieldValue(record value)`. `ReferenceDataService` already has the type in hand when it maps (`findAll(type)`, `findById(type, id)`), so it passes it along.
- **OpenAPI schema**: each component is annotated `@Schema(implementation = Long.class, format = "int64", …)`, so the schema stays an `int64` integer and no `ReferenceFieldValue` component appears. `parent_id`'s annotation adds `nullable = true`, and its description mentions null for top-level locations (spec FR-013).

**Rationale**:
- It is the smallest typed change that can express "present but null" for some types and "absent" for others.
- It keeps one response DTO, with no `Map<String, Object>` (constitution: typed DTOs everywhere).
- It keeps the mapping decision in the mapper, where the type is known.

**Alternatives considered**:
- *`Optional<Long>` components.* Jackson writes `Optional.empty()` as null under `NON_NULL`, so this would work. Rejected: optional-typed record components and parameters are a well-known Sonar smell (java:S3553), and the project keeps a Sonar configuration.
- *A per-type serialisation filter or a second DTO for types with nullable fields.* Rejected: more moving parts, and a second DTO would duplicate the schema in the OpenAPI document.
- *Leaving `parent_id` out for top-level locations.* Rejected by the spec (EC-005, FR-005).

**To verify during implementation**:
- A Jackson test confirms the three serialised forms: absent, `null` and a number.
- The OpenAPI contract test confirms the `int64` integer schema, `parent_id` nullable, and no extra component. If springdoc ignores `implementation` on a `@JsonValue` type, a property-level `@Schema(type = "integer", …)` is the fallback.

---

## R5. Generating the fixture, with the extract's ids

**Decision**: Generate `locations.json` once with a throwaway script outside the repository (the session scratchpad; never committed), as 003–006 did:
1. Read `joh-elinks-api/ReferenceData/REF_Location.csv` as UTF-8, stripping a BOM if present (there is none).
2. Take `id`, `name`, `type_id`, `parent_id` and `jurisdiction_id` from every row, unchanged. An empty `parent_id` becomes `null`. Every other column is ignored.
3. Sort by `id`, ascending; positions run from 1 to 2,000 in that order.
4. A record is ended iff `position % 7 == 0` (285 records, ids 9 to 2364). Dates and timestamps follow 002's rule.
5. Write a JSON array in 003's field order (`id, name, type_id, parent_id, jurisdiction_id, created_at, updated_at, start_date, end_date`), two-space indented, UTF-8 without BOM, non-ASCII characters written literally, with a trailing newline. That is about 534 KB.

Before checking the file in, the script MUST confirm, and the implementation notes record:
- 2,000 records with 1,523 distinct names;
- the five copied columns equal the extract's, row for row, as code points (spec SC-003);
- 142 null parents;
- every non-null parent resolves, none is self-referencing, and the maximum depth is 2;
- 285 ended records, ids 9 to 2364;
- one non-ASCII name, at id 2398, and no `\u` escape in the file;
- every row of the spec's Reference Points table;
- every parent in the **checked-in** `base_locations.json` (408 distinct values) is a location id.

The last check was run during planning against a simulated fixture: all 408 resolve.

**Rationale**: As in 003–006, plus the extract's ids (spec DR-1).

---

## R6. Checking the generation rules without the extract

**Decision**: `ReferenceDataFixtureRulesTest` gets a per-fixture id rule:
- **Fixture rows**: the `Fixture` record gains an `IdRule ids` component: `POSITIONAL` (002's rule: names in code-point order, `id = 10 × position`) for the five existing rows, and `SOURCE` for `locations.json`.
- **`SOURCE` rule**: ids are positive and strictly ascending, with no constraint on their values or on name order. Every other rule (dates by position, field order, reference-field shape, the jurisdiction set) applies as for every fixture.
- **Declarations**: `Declaration` gains the nullable reference fields read from `application.yml`. The reference-field check accepts null only for those fields.
- **Reference points**: `ReferencePoint.withReferences` accepts nulls (it builds its list with `Arrays.asList` rather than `List.of`), so a top-level point can expect `null`.
- **New row**: `locations.json`, 2,000 records, 1,523 distinct names, `Map.of()`, and the spec's 12 reference points.
- **Literal characters**: the no-`\u` check (006) covers the new fixture automatically.

**Rationale**:
- The id rule is a property of the dataset, not of the type's configuration (nothing at runtime depends on it), so it belongs in the test's fixture table.
- `SOURCE` is the honest rule: the test can't know the extract's ids without the extract, but it can enforce everything that follows from them.

**Alternatives considered**:
- *A configuration flag such as `id-rule: source`.* Rejected: it would add runtime configuration that no runtime code reads.

---

## R7. Checking that references resolve (spec FR-012; Clarifications)

**Decision**: A new unit test, `src/test/java/…/jo/repository/ReferenceDataReferencesTest.java`, reads the shipped fixtures from `src/main/resources/reference-data/` and holds a table of references between served types:

| Source fixture | Field | Target fixture |
|----------------|-------|----------------|
| `locations.json` | `parent_id` | `locations.json` |
| `base_locations.json` | `parent_id` | `locations.json` |

For each row, every non-null value is an `id` in the target fixture. For self-references (source is target), no record is its own parent, and following parents from every record reaches a record with a null parent within the number of records, so there are no cycles. A second check confirms the table is complete: every pair of a declared reference field and a served target type is listed. The test records the mapping from field to target type itself (`type_id` → `location_types`, `parent_id` → `locations`, `jurisdiction_id` → `jurisdictions`), so when `location_types` or `jurisdictions` is served, the test fails until its row is added. That is how DF-1 and DF-5 stay visible.

The functional suite repeats AC-010 and AC-011 over HTTP (`LocationsFunctionalTest`), so the served data is checked as well as the files.

**Rationale**:
- Tests only, as clarified.
- Table-driven, so later types add rows rather than code.
- The completeness check turns "the feature that serves the target type MUST add a test" (Constitution IV) into a build failure rather than a reminder.

---

## R8. Widening the type-specific-code guard

**Decision**: Add `location` to `NoTypeSpecificCodeTest`'s fragments, with:
- **Recognition cases**: `location`, `locations`, `LOCATION`, `Location`, `LocationHelper`, `myLocation`, `XLOCATION`, `location_id`, `base_location`, `baseLocation`.
- **Non-matching cases**: `allocation`, `relocation`, `dislocation`, `collocation`. Each has the fragment straight after a lower-case letter, which exercises the boundary rule.

The only main-source match today is the Javadoc phrase "fixture location" in `ReferenceDataType`, which becomes "fixture path". The new `ReferenceFieldValue`, configuration and repository code must not use the word either; for example, comments say "top-level record", not "top-level location".

**Rationale**: As for `gender` (005): a single-word fragment, matched in every spelling, with no exemption. The People feature adds its own reviewed exception (spec DF-6).

---

## R9. Tests that change because `locations` is now supported or the response type changed

| Test or artifact | Change |
|------------------|--------|
| `ReferenceDataOpenApiContractTest` | `enum` gains `locations` before `appointment_title`. The description becomes `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations. Also supports deprecated values: appointment_title`. `parent_id` is now asserted nullable, while `type_id` and `jurisdiction_id` stay not nullable and all three stay `int64`. A new assertion checks that no `ReferenceFieldValue` schema component exists. |
| `ReferenceDataFunctionalTest` › `UnsupportedTypeTests` | The list loses `locations` and keeps `location`, giving 15 E-Links names, plus `Locations`, `LOCATIONS`, `locations_` and `Location`. The comment becomes "The other 15 E-Links attribute names: five canonical types, their five deprecated aliases, and base_location, contract_type, gender, judiciary_role and location, which 003–007 don't serve". The "Also covers" note adds 007 AC-019 and EC-010. |
| `ReferenceDataMapperTest`, `testsupport/ReferenceDataFixtures`, `ReferenceDataControllerTest`, `ReferenceDataServiceTest` | The mapper now takes the type, and reference components are `ReferenceFieldValue`. Helpers such as `referencingResponse` wrap their values. New mapper cases cover a declared-nullable field holding null (written as null), an undeclared field (absent), and a declared field with a value. |
| `FixtureReferenceDataRepositoryTest`, `ReferenceDataTypeRegistryTest`, `testsupport/ReferenceDataFixtures` | The 16 `new TypeProperties(…)` calls gain the new argument. New cases: a nullable field accepts null and a positive id; a nullable field with its key missing fails; a nullable field holding 0 or a string fails; nullable declared without being a reference field fails at start-up; empty, unknown and duplicate entries in `nullable-reference-fields` fail. New test fixtures go under `src/test/resources/reference-data/`. |
| `ReferenceDataTypeTest` | Its four constructions gain the nullable set, and it checks the copy is an unmodifiable `EnumSet`. |
| `ReferenceDataExtensionIntegrationTest` | Expected to pass unchanged, since it declares its own types; confirm during implementation. |

Existing golden files MUST stay byte-for-byte unchanged (spec AC-018, SC-008), including `base_locations.json`. This is the end-to-end proof that R4's serialisation change leaves undeclared and non-nullable fields as they were.

---

## R10. Functional, smoke, Postman and documentation

- **`LocationsFunctionalTest`**: structured like `JudiciaryRolesFunctionalTest`, covering AC-001 to AC-020.
  - The golden file `src/functionalTest/resources/golden/locations.json` is captured with `curl -o` from a running instance (compact, no trailing newline, about 414 KB).
  - Raw-body counts are exact: 285 `"end_date":"2025-03-31"`, 1,715 `"end_date":null` and 142 `"parent_id":null`.
  - On the raw body it also checks `"name":"Cheshire  LJA"` with two spaces and the en-dash name, so a whitespace or escape change fails.
  - `ContractPaths.LOCATIONS` is added.
- **Smoke**: `LOCATIONS` is added to the per-collection p95 guard. At about 414 KB it becomes the largest collection; base locations' strict p95 was 4 ms in 006.
- **Postman**: a "Reference Data › Locations" sub-folder after "Judiciary Roles", with ten requests:
  - the collection (`200`; 2,000 results; first `1` "Aberconwy"; last `2433`; 285 ended; 142 null parents; every record has nine fields);
  - `/1` (`parent_id` 1722);
  - `/2141` (`parent_id` null, the key present);
  - `/2` (`404`);
  - `/abc` (`400`);
  - `?parent_id=1722` (`400`);
  - the trailing slash (`404`);
  - missing token (`401`);
  - invalid token (`401`);
  - `location` (`400`, singular name not served).

  It uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`. The folder is inserted as text, as in 004–006. The unsupported example (`jurisdictions`) is unaffected.
- **README**: § Reference data lists six types, with `locations` at 2,000 records and `location` returning `400`, and explains the E-Links ids and null `parent_id`. § Adding a reference-data type documents `nullable-reference-fields` and the explicit-null fixture rule (spec FR-015).
- **CLAUDE.md**: a 007 line in § Current repo state, and § Reference data adds location names and reference codes, with the data-model reference.
- **002 contract**: a one-line pointer to this feature's delta.
- **003 spec**: its DF-1 row gets a one-line note that 007 closed it for `parent_id` (the rest stays open). No other earlier artifact uses `locations` as an example of an unsupported type.

---

## R11. Performance

**Decision**: No change beyond R10's smoke guard. The response is about 414 KB, against about 300 KB for base locations. Records are held in memory and serialised per request, as for every type.

---

## Resolved: no outstanding `NEEDS CLARIFICATION` items
