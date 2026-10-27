# Research: Reference Data API — Judiciary Roles

**Feature**: `006-judiciary-roles-reference-data` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items. The design questions were how to ship the type without code, how to generate 164 names exactly (including the first `’` characters served) and prove it, and which existing tests and artifacts assume `judiciary_roles` is unsupported. Each decision follows 005's equivalent where one exists.

---

## R1. How the type is added: configuration and a fixture

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: judiciary_roles
        fixture: classpath:reference-data/judiciary_roles.json
```

No `aliases` (the singular name is not served, spec FR-001), no `reference-fields`, no `boolean-fields` and no `unique-names` (names are unique, the default).

**Rationale**: The generic mechanism already covers a type with only the shared fields, as 005 showed (spec FR-003). Appending keeps the OpenAPI `enum` order stable: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `appointment_title`.

**Alternatives considered**: *Serving `judiciary_role`* — excluded by the feature description (deprecated names are out of scope).

---

## R2. Generating the fixture, and proving the names are exact

**Decision**: Generate `judiciary_roles.json` once with a throwaway script kept outside the repository (e.g. the session scratchpad; never committed), as 003–005 did, and check in only the output:

1. Read `joh-elinks-api/ReferenceData/REF_JudiciaryRole.csv` as UTF-8 (stripping a byte-order mark if present; there is none today) and take `name` from every row (164 rows). Ignore every other column, including the extract's ids (90000–90163) and its dates and end dates.
2. Sort names in code-point order. Positions 1 to 164.
3. `id = 10 × position` (10 to 1640). Ended iff `position % 7 == 0`: 23 records (70 to 1610). Dates and timestamps per 002.
4. Write the array with fields in 002's order, two-space indented, UTF-8 without BOM, non-ASCII characters written literally (not as `\u` escapes), trailing newline.

**Before checking in**, the script MUST confirm, and the implementation notes record:
- 164 records and 164 distinct names;
- the multiset of names equals the extract's `name` column **exactly, as code points** (SC-003): nothing trimmed, normalised or re-encoded;
- 7 names contain non-ASCII characters (`’` U+2019 or `–` U+2013), and the file has no `\u` escape;
- 23 ended records, ids 70 to 1610;
- every row of the spec's Reference Points table.

This one-off comparison is the only place the extract is used (FR-007). After it, the fixture is the source of truth, and the build checks it without the extract (R3, data-model.md § 4).

**Add a row** to `ReferenceDataFixtureRulesTest`'s `fixtures()` table: `judiciary_roles.json`, 164 records, 164 distinct names, `Map.of()` true counts, and the spec's 13 Reference Points as `point(…)` entries (`END_DATE` for 70, 140, 1400 and 1610; null otherwise). The test's per-position rule and code-point order check already apply, so no rule changes.

**Rationale**: 002's rules unchanged keep one documented pattern across all fixtures. The test already sorts with `compareCodePoints`, which is what places `’` and `–` after ASCII.

**Alternatives considered**:
- *Reproducing the extract's 14 end dates.* Rejected by the spec (FR-009, EC-007).
- *Normalising apostrophes and dashes, or fixing "Principle" and the truncated name.* Rejected by the spec (FR-008, EC-003, EC-005).
- *Committing a copy of the extract for a build-time comparison.* Forbidden (FR-007; Constitution IV): the extract is local reference material.

---

## R3. Enforcing "written literally, not escaped"

**Decision**: Add one generic check to `ReferenceDataFixtureRulesTest`: for every shipped fixture, the raw file text contains no `\u` escape sequence. It applies to all five fixtures, not only this one.

**Rationale**: FR-008 requires literal non-ASCII characters, and FR-015 requires the generation rules to be checked by an automated test. The existing rule test reads fixtures as parsed JSON, which can't tell `’` from `’`, so it can't catch a regenerated file with escapes. The check is generic (no type named), costs one assertion per fixture, and passes for all four existing fixtures today (checked: none contains `\u`; `base_locations.json` already writes its en dash literally, as 003 required).

**Alternatives considered**:
- *A check only for `judiciary_roles.json`.* Rejected: 003 had the same rule for its en dash, and a per-type branch would be type-specific test logic for no gain.
- *No check, relying on the golden file.* Rejected: the golden file is captured from a running service whose responses are re-serialised, so it doesn't show how the fixture is written.

---

## R4. Tests that change because `judiciary_roles` is now supported

| Test or artifact | Change |
|------------------|--------|
| `ReferenceDataOpenApiContractTest` | `enum` becomes `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `appointment_title` on both operations; description `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles. Also supports deprecated values: appointment_title`. `ReferenceDataResponse` assertions unchanged (FR-011). |
| `ReferenceDataFunctionalTest` › `UnsupportedTypeTests` | The list loses `judiciary_roles` and keeps `judiciary_role`: 16 E-Links names (spec AC-016), plus `Judiciary_roles`, `JUDICIARY_ROLES`, `judiciary-roles`, `judiciaryRoles` and `judiciary_roles_` (EC-011). The comments become "The other 16 E-Links attribute names: six canonical types, their six deprecated aliases, and base_location, contract_type, gender and judiciary_role, which 003–006 don't serve" (the existing comment's "deprecated aliases" are the spec's "singular forms"), and the "Also covers" note adds 006 AC-016 and EC-011. |
| `ReferenceDataFunctionalTest` › `AuthenticationTests` | The missing-token case `GET /api/v1/reference_data/judiciary_roles` is there as an *unsupported* path, to show authentication is checked first. It would still return `401`, so it wouldn't fail, but it would stop testing what it was written for. Change it to `/api/v1/reference_data/jurisdictions` (spec AC-015). |
| `ReferenceDataExtensionIntegrationTest` | Expected unchanged (it declares its own test types); confirm during implementation. |

Existing golden files for `appointment_titles`, `base_locations`, `contract_types` and `genders` MUST stay byte-for-byte unchanged (AC-015).

---

## R5. Functional tests for the new type

**Decision**: A new `JudiciaryRolesFunctionalTest`, structured like `GendersFunctionalTest` and using `ReferenceDataHttp` (`assertUnauthorized`, `byId`, `nameOf`), covering AC-001 to AC-017, with a golden file `src/functionalTest/resources/golden/judiciary_roles.json` for AC-005, captured with `curl -o` from a running instance (compact body, no trailing newline, about 27 KB, as `ReferenceDataHttp.golden()` compares bytes).

It checks:
- 164 records in ascending `id` order, each with the six-field list (AC-001), and the Reference Points rows (AC-002);
- names served exactly (AC-003): the parsed names of the three non-ASCII reference points (140, 150, 960) and of 970, and, on the raw body, "Magistrates’ Liaison Judge", "Deputy Chairman – Advisory Committee" "Judicial Commissioner Investigatory Powers Commiss", and the three "Vice President … Bench" names (EC-004, EC-006) as substrings;
- ended records (AC-004): on the raw body, exactly 23 `"end_date":"2025-03-31"` and 141 `"end_date":null` — counts, not absence checks, following 005's code-review fix, so a reformatted body fails instead of passing silently; and on the parsed records, the ended ids are exactly the multiples of 70;
- the cross-type id `10` (AC-009, EC-010: "Acting Designated Civil Judge" here, and the other four types' names);
- the singular-name routes (AC-010 to AC-012), credentials (AC-013, AC-014), `?name=Mentor%20Judge` rejected (AC-017), and the inherited path, method, media-type and id edge cases (EC-012), as `GendersFunctionalTest` does.

Test source contains `’` and `–` literally, as `ReferenceDataFixtureRulesTest` already does for the en dash; Java reads sources as UTF-8 by default and the build sets `-Dfile.encoding=UTF-8`.

**Rationale**: One class per type keeps each feature's acceptance tests together. The golden file pins all 164 names as served; R2's one-off comparison ties them to the extract.

---

## R6. Widening the type-specific-code guard

**Decision**: Add `judiciary_role` to `NoTypeSpecificCodeTest`'s fragments, with:
- recognition cases: `judiciary_role`, `judiciary_roles`, `JUDICIARY_ROLE`, `judiciary role`, `Judiciary Role`, `judiciary-role`, `JudiciaryRole`, `judiciaryRole`, `myJudiciaryRole`, `XJUDICIARY_ROLE`, `judiciary_role_id`, `judiciaryRoleNameId`, and a wrapped Javadoc line `judiciary\n     * role`;
- non-matching cases: `judiciary`, `role`, `judicial role`, `JudicialRole`, `judiciary member role`, and `nonjudiciary role` (the fragment straight after a lower-case letter, exercising the boundary rule).

No exemption for `judiciary_roles`, `judiciary_role_id` or `judiciary_role_name_id` (spec FR-016, DF-4).

The matcher joins the fragment's words with any run (including none) of whitespace, underscores, hyphens or Javadoc asterisks, case-insensitively, and doesn't start straight after a lower-case letter unless the fragment starts upper-case there. So `myJudiciaryRole` and `XJUDICIARY_ROLE` match, `nonjudiciary role` doesn't, and `judicial role` doesn't because `judicial` isn't `judiciary`.

**Rationale**: Same treatment as `base_location` and `contract_type` (two-word fragments). No main Java source mentions it today (checked), so the guard passes immediately; the People feature will add its reviewed exception (DF-4).

---

## R7. Performance

**Decision**: Add `JUDICIARY_ROLES` to `ContractPaths` and to the smoke suite's per-collection p95 guard. No other change. The response is about 27 KB, between `appointment_titles` (194 records) and `base_locations` (1,462).

---

## R8. Documentation and example requests

**Decision**:
- **Postman**: a "Reference Data › Judiciary Roles" sub-folder, after "Genders", with nine requests: the collection (164 results; first "Acting Designated Civil Judge", last "Wales Training Course Director (JC)"; 23 ended), `/960` ("Judge in Charge of Jury List (King’s Bench)", checking the `’` survives), `/15` (`404`), `/abc` (`400`), `?name=Mentor%20Judge` (`400`), trailing slash (`404`), missing token (`401`), invalid token (`401`), and `judiciary_role` (`400`, "singular name not served"). Only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`. The folder is inserted as text so the existing collection's formatting is untouched (as in 004 and 005).
- **Postman, existing request**: "GET /api/v1/reference_data/judiciary_roles - unsupported (contract-valid, out of scope)" becomes "GET /api/v1/reference_data/jurisdictions - unsupported (contract-valid, out of scope)", with its URL and path changed; its tests are unchanged.
- **README**: § Reference data lists five types (`judiciary_roles`: 164 records; `judiciary_role` returns `400`). "Adding a reference-data type" is still accurate and needs no change.
- **CLAUDE.md**: § Current repo state gains a 006 line; § Reference data adds judiciary role names to the public values taken from the extracts, and the data-model reference.
- **002 contract**: a one-line pointer to this feature's delta, next to the 003–005 pointers.
- **Quickstarts that request `judiciary_roles` as unsupported**: 002's § 3 row 5 and 005's § 3 row 14 (and its § 5 sentence) change to `jurisdictions`, so each row still passes when run, as 005's code review did for 002's row.

---

## R9. Ids and the People reference

**Decision**: Ids are synthetic (10 × position) and stable (FR-010). DF-4 tells the People feature to pick which of `judiciary_role_id` and `judiciary_role_name_id` refers to these records, use these ids, test that every reference resolves, keep any `name` it carries equal to the referenced record's name, and add its reviewed guard exception. Nothing is built for it now.

---

## Resolved: no outstanding `NEEDS CLARIFICATION` items
