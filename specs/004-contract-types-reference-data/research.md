# Research: Reference Data API — Contract Types

**Feature**: `004-contract-types-reference-data` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items. The open design questions were how to carry the `salaried` flag without type-specific code, how strictly to read it from the fixture, and which existing tests and documents change. Each decision below follows 003's equivalent where one exists.

---

## R1. How the type is added: configuration and a fixture

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: contract_types
        fixture: classpath:reference-data/contract_types.json
        # The extract's salaried flag (004 D-3).
        boolean-fields: [salaried]
```

No `aliases` (the alias is not served, spec Clarifications), no `reference-fields`, and no `unique-names` (names are unique, the default).

**Rationale**: Exactly 003's route (003 R1). Appending keeps the OpenAPI `enum` order stable: canonical names in configuration order, then aliases, giving `appointment_titles`, `base_locations`, `contract_types`, `appointment_title`.

**Alternatives considered**: *Serving the alias* — rejected in spec Clarifications.

---

## R2. Carrying `salaried`: generic boolean fields

**Decision**: Add **boolean fields**, a generic per-type declaration modelled on 003's reference fields (003 R11):

- `entity/BooleanField` — an enum with one member, `SALARIED("salaried", ReferenceDataRecord::getSalaried)`, offering `fieldName()`, `readFrom(record)` and `fromFieldName(name)`, like `ReferenceField`.
- `ReferenceDataProperties.TypeProperties` gains `List<String> booleanFields` (`boolean-fields`, default empty).
- `ReferenceDataType` gains `Set<BooleanField> booleanFields`, copied into an `EnumSet` like `referenceFields`.
- `ReferenceDataTypeRegistry` resolves the names, rejecting unknown, repeated and empty entries. The existing private `referenceFields(...)` parsing is generalised into one helper used by both declarations, so the rule and its error messages aren't duplicated (Principle III).
- `ReferenceDataRecord` gains `@JsonProperty("salaried") @JsonSetter(nulls = Nulls.FAIL) Boolean salaried`.
- `ReferenceDataResponse` gains `Boolean salaried`, `@JsonInclude(NON_NULL)`, placed after the reference fields and before `created_at` in `@JsonPropertyOrder`. Its `@Schema` is `type boolean`, `NOT_REQUIRED`, described as deviation D-3. MapStruct maps it by name; the mapper is unchanged and its `unmappedTargetPolicy = ERROR` proves the mapping exists.

For ContractType, which declares no reference fields, the order is `id`, `name`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date` (spec FR-005). A future type that declared both kinds would get reference fields first, then boolean fields.

**Rationale**: `salaried` is not an id, so it can't be a reference field (whose values must be positive integers). A closed enum keeps the DTO typed (Principle VIII) and the OpenAPI schema exact (Principle XVIII); it costs one enum constant and one property per new field, which is the same trade 003 made. No code names a type: `salaried` is a field name, as `jurisdiction_id` is.

**Alternatives considered**:
- *A free-form `extra-fields` map (`Map<String, Object>`) on the record and response.* Forbidden by Principle VIII, and the OpenAPI schema couldn't describe it.
- *Making `salaried` a plain optional DTO property with no declaration.* Then nothing would stop a fixture of another type carrying it, or a contract-type record omitting it, and the guarantee that existing responses are unchanged would rest on fixture content alone.
- *A general typed-field mechanism (field name → JSON type) in configuration.* More code and more ways to misconfigure, for one field (Principle XII).

---

## R3. Reading the flag strictly at start-up

**Decision**:
- In `FixtureReferenceDataRepository`, extend the strict mapper's coercion config to `LogicalType.Boolean`: fail on `Integer`, `Float`, `String` and `EmptyString` input. With `@JsonSetter(nulls = FAIL)` on the record property, only JSON `true`/`false` is accepted; `0`, `1`, `"true"`, `""` and `null` stop start-up.
- Add `validateBooleanFields(fixture, type, record)`, called beside `validateReferenceFields`: for each `BooleanField`, a type that declares it requires it on every record (`salaried is required`); a type that doesn't declare it rejects it (`salaried is not a boolean field of <type>`).
- Registry errors follow 003's wording: `unknown boolean field '<name>'`, `duplicate boolean field <name>`, `empty entry in boolean-fields`.

**Rationale**: Jackson's defaults coerce `1` and `"true"` to `Boolean.TRUE`, which would let a mistyped fixture through and break FR-018 and AC-021. 003 R10/R11 set the precedent of failing on the wrong JSON type for integers and strings (003 FR-019).

**Alternatives considered**: *Accepting `0`/`1` to mirror the extract.* The fixture is the mock's own format, and the response must be a boolean; converting at generation time (R4) keeps the fixture honest.

---

## R4. Generating the fixture and guarding its rules

**Decision**: Generate `contract_types.json` once with a throwaway script run locally against the gitignored `joh-elinks-api/ReferenceData/REF_ContractType.csv`, as 003 R2 did, and check in only the output:

1. Read the extract as UTF-8; take `name` and `salaried` from every row (14 rows). Ignore every other column, including the extract's `id` (blank for the "Salaried" row) and its dates.
2. Map `salaried`: `1` → `true`, blank → `false`; any other value aborts the script (spec FR-008).
3. Sort names in code-point order; number positions from 1; `id = 10 × position`; ended iff `position % 7 == 0`; dates and timestamps per 002.
4. Write the array with fields in the order `id`, `name`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date`, two-space indented, trailing newline.

Extend `ReferenceDataFixtureRulesTest`: add `contract_types.json` to `fixtures()` (14 records, 14 distinct names, the six reference points with their `salaried` values), add boolean fields to `Declaration`, and check for every fixture that each record carries exactly the declared boolean fields as JSON booleans. For `contract_types.json` the test also checks the salaried count (11 `true`, 3 `false`).

**Rationale**: Same as 003 R2/R4: the checked-in file is the source of truth and the rule test keeps it honest without the extract.

**Alternatives considered**: *Hard-coding which names are salaried in the rule test.* Rejected: the reference points and the count are enough, and listing every name would duplicate the fixture.

---

## R5. Tests that change because a third type is supported

| Test | Change |
|------|--------|
| `ReferenceDataOpenApiContractTest` | `enum` becomes `appointment_titles`, `base_locations`, `contract_types`, `appointment_title` on both operations; description `Can be one of: appointment_titles, base_locations, contract_types. Also supports deprecated values: appointment_title`; `ReferenceDataResponse` properties gain `salaried` after `jurisdiction_id` (type `boolean`, not required, not nullable, description mentions D-3). |
| `ReferenceDataFunctionalTest` | The unsupported list loses `contract_types` and keeps `contract_type`: 18 E-Links names (spec AC-016), plus `Contract_Types`, `CONTRACT_TYPES` and `contract-types` (EC-007). |
| `ReferenceDataFixtureRulesTest` | Third fixture and boolean-field rules (R4). |
| `ReferenceDataExtensionIntegrationTest` | Expected unchanged (it declares its own test types); confirm during implementation. |

Existing golden files for `appointment_titles` and `base_locations` MUST stay byte-for-byte unchanged (AC-015).

---

## R6. Functional tests for the new type

**Decision**: A new `ContractTypesFunctionalTest`, structured like `BaseLocationsFunctionalTest` and using `ReferenceDataHttp`, covering AC-001 to AC-017, with a captured golden file `src/functionalTest/resources/golden/contract_types.json` for AC-004. It checks the seven-field list per record, `salaried` on every record, the reference points, `%` names verbatim (AC-005), the cross-type id `10` (AC-009), the alias routes (AC-010 to AC-012) and `?salaried=true` rejected (AC-017).

**Rationale**: One class per type keeps each feature's acceptance tests together (003 R6).

---

## R7. Performance

**Decision**: Add `CONTRACT_TYPES` to the smoke suite's per-collection p95 guard. No caching or other change.

**Rationale**: At 14 records (about 2.5 KB) the collection is the smallest served; the guard is there so a regression in shared code shows up on every path (003 R7).

---

## R8. Keeping the type-specific-code guard meaningful

**Decision**: Add `contract_type` to `NoTypeSpecificCodeTest`'s fragments, and spelling cases (`contract_type`, `contract_types`, `CONTRACT_TYPE`, `contract type`, `Contract Type`, `contract-type`, `ContractType`, `contractType`, `myContractType`, `XCONTRACT_TYPE`) to the recognition test, with non-matching cases such as `contract`, `type` and `contractual type`.

**Rationale**: Turns FR-016 into a build failure (003 R8). The fragment doesn't match `salaried` or `BooleanField`, which are field-level, not type-level, names.

---

## R9. Documentation and example requests

**Decision**:
- **Postman**: a "Reference Data › Contract Types" sub-folder with ten requests: the collection (14 records, `salaried` on all), `/120` (Salaried, `true`), `/10` (Fee-paid, `false`), `/15` (`404`), `/abc` (`400`), `?salaried=true` (`400`), trailing slash, missing token (`401`), invalid token (`401`), and `contract_type` (`400`). Every value comes from collection or environment variables.
- **README**: § Reference data lists three types and describes `salaried`; § Adding a reference-data type documents `boolean-fields` beside `reference-fields` and `unique-names` (FR-013).
- **CLAUDE.md**: § Current repo state gains a 004 line.
- **002 contract**: a one-line pointer to this feature's delta, as 003 added.

---

## R10. Ids and the appointments reference

**Decision**: Ids are synthetic (10 × position), as the spec requires, and are stable (FR-010). DF-2 tells the People/appointments feature to use these ids for `contract_type_id` and these names for `contract_type`, with a test that every reference resolves. Nothing is built for it now.

**Rationale**: Principle IV requires references between served types to resolve; recording it as a Deferred Item with a named closing test is the pattern 003 established for DF-1.

---

## Resolved: no outstanding `NEEDS CLARIFICATION` items
