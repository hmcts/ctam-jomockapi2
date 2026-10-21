# Research: Reference Data API — Genders

**Feature**: `005-genders-reference-data` | **Date**: 2026-10-21 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items. The design questions were how to ship the type without code, what the four-record fixture looks like under 002's rules, and which existing tests and artifacts assume `genders` is unsupported. Each decision follows 003's and 004's equivalent where one exists.

---

## R1. How the type is added: configuration and a fixture

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: genders
        fixture: classpath:reference-data/genders.json
```

No `aliases` (the alias is not served, spec Clarifications), no `reference-fields`, no `boolean-fields` and no `unique-names` (names are unique, the default).

**Rationale**: The generic mechanism already covers a type with only the shared fields; nothing else is needed (spec FR-003). Appending keeps the OpenAPI `enum` order stable: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `appointment_title`.

**Alternatives considered**: *Serving the alias* — rejected in spec Clarifications.

---

## R2. Generating the fixture, and the missing ended record

**Decision**: Generate `genders.json` once with a throwaway script kept outside the repository, as 003 R2 and 004 R4 did, and check in only the output:

1. Read the local gender extract as UTF-8 and take `name` from every row (4 rows). Ignore every other column, including the extract's ids (1, 3, 4, 5) and dates.
2. Sort names in code-point order: Female, Male, Other, Prefer not to say. Positions 1 to 4.
3. `id = 10 × position` (10 to 40). Ended iff `position % 7 == 0`: none. Dates and timestamps per 002.
4. Write the array with fields in 002's order (`id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`), two-space indented, trailing newline.

Add a row to `ReferenceDataFixtureRulesTest`'s `fixtures()` table: `genders.json`, 4 records, 4 distinct names, `Map.of()` true counts, and all four records as reference points (`point(…)` with null end dates). The test's per-position rule already expects an open record at positions 1 to 4, so no rule changes.

**On 002 FR-023**: no test asserts "at least one ended record" per fixture (checked: the fixture-rule test checks each position against the rule; no suite counts ended records), so the decision that Gender has no ended record (spec Clarifications) needs no test change and no exception in code.

**Rationale**: 002's rules unchanged keep one documented pattern across all fixtures; the spec chose this over a special rule for small types.

**Alternatives considered**:
- *Ending the last record to satisfy FR-023.* Rejected in spec Clarifications: it would misrepresent a gender category as withdrawn.
- *Copying the extract's ids (1, 3, 4, 5).* The spec fixes synthetic ids (FR-009), as for every other type.

---

## R3. Tests that change because `genders` is now supported

| Test or artifact | Change |
|------------------|--------|
| `ReferenceDataOpenApiContractTest` | `enum` becomes `appointment_titles`, `base_locations`, `contract_types`, `genders`, `appointment_title` on both operations; description `Can be one of: appointment_titles, base_locations, contract_types, genders. Also supports deprecated values: appointment_title`. `ReferenceDataResponse` assertions unchanged (FR-011). |
| `ReferenceDataFunctionalTest` › unsupported names | The list loses `genders` and keeps `gender`: 17 E-Links names (spec AC-015), plus `Genders`, `GENDERS` and `genders_` (EC-007). |
| `ReferenceDataFunctionalTest` › `AuthenticationTests` | The missing-token case `GET /api/v1/reference_data/genders` was there as an *unsupported* path, to show authentication is checked first. It still returns `401` once `genders` is served, so it wouldn't fail, but it would no longer test what it was written for. Change it to `/api/v1/reference_data/judiciary_roles`. |
| `ReferenceDataExtensionIntegrationTest` | Expected unchanged (it declares its own test types); confirm during implementation. |

Existing golden files for `appointment_titles`, `base_locations` and `contract_types` MUST stay byte-for-byte unchanged (AC-014).

---

## R4. Functional tests for the new type

**Decision**: A new `GendersFunctionalTest`, structured like `ContractTypesFunctionalTest` and using `ReferenceDataHttp`, covering AC-001 to AC-016, with a golden file `src/functionalTest/resources/golden/genders.json` for AC-004, captured with `curl -o` from a running instance (compact body, no trailing newline, as `ReferenceDataHttp.golden()` compares bytes). It checks the six-field list per record, the four records, every `end_date` null (EC-004), "Prefer not to say" verbatim, the cross-type id `10` (AC-008), the alias routes (AC-009 to AC-011), `?name=Male` rejected (AC-016), and the inherited path, method and id edge cases.

**Rationale**: One class per type keeps each feature's acceptance tests together.

---

## R5. Widening the type-specific-code guard

**Decision**: Add `gender` to `NoTypeSpecificCodeTest`'s fragments, with recognition cases (`gender`, `genders`, `GENDER`, `Gender`, `GenderHelper`, `myGender`, `XGENDER`, `genderId`, `gender_id`) and non-matching cases (`engender`, `agender`, `transgender`, `agenda`). No exemption for `gender` or `gender_id` (spec Clarifications).

The guard's matcher finds the fragment anywhere except straight after a lower-case letter, unless the fragment starts with an upper-case letter there (a camelCase hump). So `engender` (lower-case `n` before `gender`) does not match, while `myGender` and `XGENDER` do. `agender` and `transgender` likewise have `gender` straight after a lower-case letter, so they show the boundary rule; `agenda` shows that a near-spelling doesn't match.

**Rationale**: Same treatment as `appointment` (also a single word). No main Java source mentions `gender` today (checked), so the guard passes immediately; the People feature will add its reviewed exception (DF-3).

---

## R6. Performance

**Decision**: Add `GENDERS` to the smoke suite's per-collection p95 guard. No other change.

---

## R7. Documentation and example requests

**Decision**:
- **Postman**: a "Reference Data › Genders" sub-folder, after "Contract Types", with nine requests: the collection (4 results, all `end_date` null, first Female, last "Prefer not to say"), `/20` (Male), `/15` (`404`), `/abc` (`400`), `?name=Male` (`400`), trailing slash (`404`), missing token (`401`), invalid token (`401`), and `gender` (`400`, "deprecated alias not served"). Only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`. The folder is inserted as text so the existing collection's formatting is untouched (as in 004).
- **Postman, existing request**: "GET /api/v1/reference_data/genders - unsupported (contract-valid, out of scope)" becomes "GET /api/v1/reference_data/judiciary_roles - unsupported (contract-valid, out of scope)", with its URL changed; its tests are unchanged.
- **README**: § Reference data lists four types (`genders`: 4 records; `gender` returns `400`). "Adding a reference-data type" is still accurate and needs no change.
- **CLAUDE.md**: § Current repo state gains a 005 line; § Reference data adds gender names to the public values taken from the extracts.
- **002 contract**: a one-line pointer to this feature's delta. **002 quickstart**: a note on § 3 row 5 that `genders` is served since 005, so a still-unsupported name such as `judiciary_roles` shows the same `400`.

---

## R8. Ids and the People reference

**Decision**: Ids are synthetic (10 × position) and stable (FR-010). DF-3 tells the People feature to use them for `gender_id`, with a test that every reference resolves, and to add its reviewed guard exception. Nothing is built for it now.

---

## Resolved: no outstanding `NEEDS CLARIFICATION` items
