# Research: Reference Data API — Base Locations

**Feature**: `003-base-locations-reference-data` | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Every decision here builds on [002's research](../002-reference-data-api/research.md). Only what this feature adds or changes is recorded.

---

## R1. How the type is added: configuration and a fixture

**Decision**: Add one entry to `jo.reference-data.types` in `src/main/resources/application.yml`, after `appointment_titles`:

```yaml
      - name: base_locations
        fixture: classpath:reference-data/base_locations.json
```

with no `aliases` (the spec doesn't serve `base_location`; `aliases` is optional, as `empty_things` in `ReferenceDataExtensionIntegrationTest` shows), and the fixture `src/main/resources/reference-data/base_locations.json`. No file under `src/main/java` changes.

**Rationale**: This is the extension path 002 designed (002 R5) and tested with test-only types (`ReferenceDataExtensionIntegrationTest`). The registry already validates names, rejects clashes between names and aliases, and fills the OpenAPI enum and description from configuration. The repository already loads and validates any declared fixture at start-up. Using the path exactly as designed is what AC-017 and SC-007 measure. The one exception, removing the duplicate-name check, is a generic change to the repository that every type gets (R10), not a per-type extension.

**Alternatives considered**:
- *A type-specific provider or controller.* Forbidden by Principle VII and FR-003.
- *Declaring the type in a separate profile or file.* Adds a configuration mechanism nobody else uses; the one list in `application.yml` is where README tells maintainers to look.

---

## R2. Generating the fixture

**Decision**: Generate `base_locations.json` once, with a throwaway script run locally against the gitignored extract, and check in only the output. The rules (data-model.md § Fixture generation rules) are 002's, applied to the `name` column of every row:

1. Read `joh-elinks-api/ReferenceData/REF_BaseLocation.csv` as UTF-8 (stripping a byte-order mark if present).
2. Take the `name` value of every row, unmodified: 1,462 values, 1,276 of them distinct (spec Clarifications, FR-008). Ignore every other column.
3. Sort in code-point order (Java `String.compareTo`, which matches code-point order for these names: the only non-ASCII character, U+2013, is in the Basic Multilingual Plane). Equal names end up adjacent; any stable or unstable sort gives the same output, because equal names produce records that differ only by position.
4. Number positions from 1; `id = 10 × position`; ended iff `position % 7 == 0`.
5. Write a JSON array, two-space indented, one record per object, fields in the order `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`, UTF-8 without a byte-order mark, characters written literally (no `\u2013` escapes), with a trailing newline — the same layout as `appointment_titles.json`.

The script is not committed: the extract isn't either, and a committed script would invite builds to depend on it (FR-007). The procedure is recorded in data-model.md so the fixture can be regenerated and compared.

**Rationale**: Matches 002 exactly (002 R6). The checked-in file becomes the source of truth, and a test (R4) proves it still follows the rules, so the script isn't needed to keep it honest.

**Alternatives considered**:
- *Generating the fixture at build time from the extract.* Breaks FR-007 and 002's rule that the build never depends on reference material.
- *Generating records at start-up from a seed.* 002 R6 rejected generators in favour of a static, reviewable fixture; nothing about base locations changes that.
- *Writing the en dash as the escape `\u2013`.* Valid JSON, but the fixture would no longer show the name as served, and review diffs would be harder to read. Literal UTF-8 is what FR-010 asks for.

---

## R3. Reading and serving the non-ASCII name

**Decision**: No code change. Verified by reading the existing code path:

- `FixtureReferenceDataRepository` reads the fixture through Jackson from an `InputStream`. Jackson detects UTF-8 for JSON input (RFC 8259 requires it), so the en dash is decoded correctly whatever the platform's default charset is.
- Responses are written by Jackson as UTF-8 bytes. Success responses are `application/json` with no `charset` parameter, as 002 settled (002 plan, "Implementation notes"): JSON is UTF-8 by definition.

A functional test asserts the decoded name of id 10290 character-for-character, including U+2013, so a regression in either direction (lost character, mojibake, or an escape) fails the build. The test source writes the character as the Java escape `\u2013` so the assertion doesn't depend on the source file's own encoding.

**Rationale**: FR-010 and AC-003 need proof, not a code change.

**Alternatives considered**: *Adding `;charset=UTF-8` to responses.* Changes every response's `Content-Type`, contradicts 002's decision and the contract, and isn't needed.

---

## R4. Guarding the fixture's rules without the extract

**Decision**: Add a unit test, `ReferenceDataFixtureRulesTest` (test suite, `repository/` package), that loads each shipped fixture in `src/main/resources/reference-data/` and checks the generation rules that can be checked from the fixture alone:

- names non-decreasing in code-point order (repeated names allowed, FR-008);
- `id == 10 × position`;
- `end_date`/`updated_at` follow the `position % 7` rule; `created_at` and `start_date` are the fixed values;
- for `base_locations`: exactly 1,462 records, exactly 1,276 distinct names, and the four reference points of spec § Reference Points.

The test is parameterised by fixture, so `appointment_titles.json` is checked by the same rules (194 records, its 002 reference points).

**Rationale**: Start-up validation (002) already rejects broken records (duplicate ids, untrimmed names, dates out of order), but it can't tell a correct fixture from one hand-edited out of pattern. This test protects FR-009 and FR-011 (stable ids) for the life of the fixture, without the extract (FR-007).

**Alternatives considered**: *Comparing against the extract in a test.* The extract isn't in the repository and the build mustn't need it.

---

## R5. Tests that change because a second type is now supported

**Decision**: Supporting `base_locations` changes these existing expectations; each is updated, not deleted:

| Test | Today | After |
|------|-------|-------|
| `ReferenceDataFunctionalTest.UnsupportedTypeTests` (002 AC-007/AC-014) | 20 contract names rejected, including `base_locations` and `base_location` | The 19 still-unsupported names (003 AC-015): `base_locations` moves to the new functional tests; `base_location` stays, because it is still unsupported (003 AC-009/AC-010). Case variants gain `Base_Locations`, `BASE_LOCATIONS` and `base-locations` (003 EC-006). |
| `ReferenceDataOpenApiContractTest` | `enum` = `appointment_titles, appointment_title`; description lists one type | `enum` = `appointment_titles, base_locations, appointment_title`; description `Can be one of: appointment_titles, base_locations. Also supports deprecated values: appointment_title` (AC-019). The order is the registry's: canonical names in configuration order, then aliases. |
| `ReferenceDataExtensionIntegrationTest` | Redeclares `types[0]` and adds test types at `[1]`, `[2]` | No change expected: an indexed list in `@TestPropertySource` replaces the whole list from `application.yml`, so `base_locations` is not loaded there. Confirm by running it; if Spring merges instead, redeclare the list in full. |

`ReferenceDataFunctionalTest`'s AppointmentTitle tests (002 AC-001 to AC-020) otherwise run unchanged, which is 003 AC-014.

**Rationale**: FR-002 supersedes 002's single-type scope; the tests must say so explicitly rather than silently dropping a case.

---

## R6. Functional tests for the new type

**Decision**: Add `BaseLocationsFunctionalTest` in the functional suite, over real HTTP like 002's, as one flat class without `@Nested` classes: each test method's name starts with the scenario it covers (`ac001…`, `ec008…`), which groups the tests by user story without nesting. 002's `ReferenceDataFunctionalTest` keeps its nested classes; this feature doesn't restructure it. Move the HTTP helpers that both functional classes need (`get`, `send`, `assertError`, `json`) out of `ReferenceDataFunctionalTest` into a small helper in the functional suite's own `testsupport` package, so neither class copies the other (Principle III applies to tests too). Add `BASE_LOCATIONS` to `ContractPaths`.

Determinism (AC-004) is checked against a captured golden response, `src/functionalTest/resources/golden/base_locations.json` (≈ 226 KB), as 002 did for appointment titles.

**Rationale**: A separate class keeps each type's acceptance scenarios traceable to its own spec, while the shared helper keeps the HTTP mechanics in one place.

**Alternatives considered**:
- *Parameterising every existing functional test over both types.* Reference points, counts and names differ per type, so most tests would need per-type tables; the churn and loss of traceability to 002's AC IDs outweigh the saving.
- *Asserting the full collection without a golden file.* 1,462 records can't be asserted by hand; the golden file plus R4's rule test cover the content.

---

## R7. Performance with a larger collection

**Decision**: Keep 002's design (no caching, no pagination) and 002's bounds. Parameterise `ReferenceDataSmokeTest.collectionP95LatencyIsWithinBound` over both collection paths, so each type has its own p95 guard: 1 s by default, 100 ms with `-Dperf.strict=true` (FR-015, SC-008).

**Measurements behind the decision**: the base-location response is about 226 KB of compact JSON, against about 32 KB for appointment titles (7.1×). Serving it is one in-memory list copy and one Jackson serialisation. 002 measured 20 appointment-title calls at 54 ms in total (≈ 2.7 ms each) under `-Dperf.strict=true`; even a linear 7.1× scale-up (≈ 19 ms) leaves the 100 ms target with a wide margin.

**Rationale**: Nothing in the numbers justifies caching serialised responses or adding pagination the contract doesn't define (Principle VI, XII).

**Risk**: if the strict run unexpectedly fails on a developer machine, cache the mapped `ReferenceDataApiResponse` per type in the service — still type-agnostic — rather than relaxing the bound. Recorded here so the response is decided in advance.

---

## R8. Keeping the type-specific-code guard meaningful

**Decision**: Extend `NoTypeSpecificCodeTest` from one forbidden fragment (`appointment`) to a list: `appointment` and `base_location` (which also matches `base_locations`), matched case-insensitively in code, comments and Javadoc, as today. Later type features add their own fragment.

**Rationale**: Today the guard proves only that AppointmentTitle isn't special-cased. Without this change, type-specific code for base locations would pass the guard, and AC-018 would be untested.

**Alternatives considered**:
- *Forbidding every E-Links attribute name, including plain words such as `location`, `ticket` and `gender`.* Produces false positives: `ReferenceDataType`'s Javadoc already says "fixture location". The underscore forms are distinctive; plain English words are not.
- *Reading the forbidden names from `application.yml`.* Ties a unit test to runtime configuration, and removing a type from configuration would silently weaken the guard for any code that still special-cased it.

---

## R9. Documentation and example requests

**Decision**:
- **Postman**: add a "Base Locations" sub-folder under the existing "Reference Data" folder, with five requests: collection, single record `70`, unknown id `15` (`404`), malformed id `abc` (`400`) and the unserved alias `base_location` (`400`, unsupported attribute name). Each has tests for status, structure and key values (count 1,462; 1,276 distinct names; first, ended and last reference points; the error messages). The existing "unsupported (contract-valid)" request keeps `genders`, which is still unsupported. No new environment variables are needed.
- **OpenAPI**: nothing to edit by hand; the enum and description are generated from the registry (002 R10). The contract test in R5 asserts the new values.
- **README**: § Reference data lists both types and counts; the "Adding a reference-data type" fixture rules drop "unique names" (R10), and the steps are otherwise unchanged (FR-014).
- **CLAUDE.md**: § Current repo state gains a `003-base-locations-reference-data` line.
- **002's contract**: its `attribute_name` "Allowed values" rows list only AppointmentTitle. Add one sentence pointing to this feature's contract delta instead of rewriting 002's document.

**Rationale**: Principle XVII and XVIII deliverables, kept to the smallest edits that keep every document true.

---

## R10. Allowing repeated names

**Decision**: Delete the duplicate-name check in `FixtureReferenceDataRepository.load` (the `names` set and its `if`), so a fixture may hold several records with the same `name`. Keep every other rule, including unique ids. In `FixtureReferenceDataRepositoryTest`, drop the `name-duplicate` row from `failsStartupOnAnInvalidRecord`, move its fixture from `src/test/resources/reference-data/invalid/name-duplicate.json` to `src/test/resources/reference-data/valid-repeated-names.json`, and add a test that it loads, with both records found by id and returned by `findAll` in id order. Add one sentence to 002's data-model § 2 (`name` row) pointing to 003 FR-017, and change README's fixture rules from "unique trimmed names" to "trimmed names".

**Rationale**: The spec serves one record per source row (Clarifications, FR-008), and 120 names in the extract belong to more than one location, so the current rule would refuse to start. Nothing else in the code relies on unique names: lookups are by id, ordering is by id, and the registry's uniqueness rules are about type names and aliases, not record names. Deleting the check is type-agnostic, so it satisfies FR-003, FR-017 and the guard (AC-018); it leaves the `-Werror` build and Checkstyle clean as long as the now-unused `HashSet`/`Set` imports go too.

**Alternatives considered**:
- *A per-type flag such as `allow-duplicate-names`.* Adds configuration and a branch for a rule nobody needs: no consumer of the contract relies on names being unique, and the real data has repeats.
- *Keeping the check and making names unique (for example by appending the parent).* Rewrites real names, which FR-008 and FR-010 forbid.
- *One record per distinct name (1,276).* Rejected in Clarifications: it loses 186 real locations and makes the ids unusable when parent references arrive (DF-1).

---

## Resolved: no outstanding `NEEDS CLARIFICATION` items

The spec's Clarifications (2026-10-08) settled names (exact; one record per source row, 1,462 records and 1,276 distinct names), placeholder names (kept), the en dash (kept) and the alias (not served). Technical Context has no unknowns; every decision above uses existing dependencies.
