# Research: Reference Data API — Jurisdictions

**Feature**: `009-jurisdictions-reference-data` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

The spec left no `NEEDS CLARIFICATION` items; its two clarifications settled the guard fragment and the status of "no forward references remain". The design questions were:
- how the type is declared, and how the fixture is generated from a JSON extract;
- how the build checks a fixture that keeps the extract's ids;
- how the build checks that every `jurisdiction_id` resolves;
- how the type-specific-code guard covers the type;
- which existing tests and artifacts change.

Each decision follows the equivalent decision in 005 (configuration only), 007 (extract ids) or 008 (reference rows) where one exists.

---

## R1. The type entry

**Decision**: One entry at the end of `jo.reference-data.types` in `application.yml`:

```yaml
      - name: jurisdictions
        # Keeps the extract's E-Links ids, so every jurisdiction_id resolves (009 DR-1).
        fixture: classpath:reference-data/jurisdictions.json
```

No `aliases` (`jurisdiction` is not served, spec FR-001), no declared fields, and `unique-names` left at its default (`true`): the 5 names are distinct. The comments on `base_locations`, `locations` and `location_types` change so that `jurisdiction_id` is described as an id of a jurisdiction checked by `ReferenceDataReferencesTest` (009), not as a forward reference.

**Rationale**: As for Gender (005), the type needs nothing the generic capability lacks. Appending keeps the OpenAPI `enum` order stable: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `appointment_title`.

**Alternatives considered**: placing the entry alphabetically, before `locations` — rejected; it would reorder the published `enum` for no benefit, and every earlier type was appended.

---

## R2. Generating the fixture

**Decision**: A throwaway script in the session scratchpad (never committed) reads the local, gitignored `joh-elinks-api/ReferenceData/REF_Jurisdiction.json` with a JSON parser, takes `id` and `name` from each object of `results`, and writes `src/main/resources/reference-data/jurisdictions.json` by data-model.md § 3's rules. Before the file is kept, the script checks:
- the top level is an object whose only key is `results`; 5 entries, each with exactly the keys `id`, `name`, `start_date`, `end_date`, `created_at` and `updated_at`;
- ids are integers 30, 34, 84, 86 and 87, unique; names are non-blank, trimmed and distinct;
- the two copied values equal the extract's entry for entry (SC-003);
- no record is ended;
- every `jurisdiction_id` in the checked-in `locations.json`, `base_locations.json` and `location_types.json` is an id (FR-011);
- every row of the spec's Records table.

The plan's implementation notes record the result.

**Rationale**: As in 003–008: the extract is used once, and the build never depends on it (FR-007). The extract being JSON rather than CSV changes only the reader; the extract's own formatting (tabs, trailing newline) is irrelevant because the fixture is written fresh.

---

## R3. Fixture rules on every build

**Decision**: `ReferenceDataFixtureRulesTest`:
- **New row**: `new Fixture("jurisdictions.json", 5, 5, Map.of(), IdRule.SOURCE, …)` with reference points 30 "Courts", 84 "Magistrates", 86 "Coroners" and 87 "Skills", each with a null end date.
- **`JURISDICTION_IDS`** stays `{30, 34, 84, 86}`: it pins the values that the location data uses, which is narrower than the served set (87 is unreferenced). Its Javadoc changes from "The E-Links jurisdictions" to "The jurisdictions the shipped location data refers to (Skills, 87, is served but unreferenced)".
- The `SOURCE` id rule, the date rule by position, the field order and the literal-character check apply unchanged.

**Rationale**: 007 built the `SOURCE` rule for exactly this case; gaps are allowed under it, as for Location. Keeping `JURISDICTION_IDS` means a stray new jurisdiction value in a location fixture still fails, even though it would resolve.

**Alternatives considered**: deriving `JURISDICTION_IDS` from `jurisdictions.json` — rejected; resolution is `ReferenceDataReferencesTest`'s job, and the derived set would accept 87, weakening the check.

---

## R4. References on every build

**Decision**: `ReferenceDataReferencesTest` gains three rows:

```java
// Close 003 DF-1, 007 DF-5 and 008 DF-7 for jurisdiction_id.
new Reference("locations", "jurisdiction_id", "jurisdictions"),
new Reference("base_locations", "jurisdiction_id", "jurisdictions"),
new Reference("location_types", "jurisdiction_id", "jurisdictions")
```

`everyReferenceBetweenServedTypesIsChecked` demands them as soon as `jurisdictions` is configured, since `TARGET_TYPES` already maps `jurisdiction_id` to `jurisdictions`. None is self-referencing, so the cycle test skips them. The agreement table is unchanged (spec FR-012). The class Javadoc stops listing DF-1, DF-5 and DF-7 as open forward references and says that forward references, when a later feature adds one, are left to that feature's deferred item.

**Rationale**: The table and completeness check were built for this (007, 008). No new check forbids future forward references (spec Clarifications).

---

## R5. The type-specific-code guard

**Decision**: `NoTypeSpecificCodeTest`:
- `noMainSourceFileNamesASpecificReferenceDataType`'s fragments gain `jurisdictions`.
- A new case, `theGuardRecognisesEveryJurisdictionsSpelling`, over `jurisdictions`, `Jurisdictions`, `JURISDICTIONS`, `myJurisdictions`, `JurisdictionsHelper` and `XJURISDICTIONS`.
- A new case, `theGuardIgnoresTheGenericJurisdictionField`, over `jurisdiction_id`, `jurisdictionId`, `JURISDICTION_ID`, `getJurisdictionId` and `Id of the record's jurisdiction`, each not matched by the `jurisdictions` fragment.
- A comment above the fragment list explains why this type's fragment is the plural (spec Clarifications; 003's generic field contains the singular).

**Rationale**: Spec FR-018. Checked against today's main sources: the plural matches nothing, the singular matches 14 times, all in the generic field or its description.

**Alternatives considered**: see spec Clarifications (a singular fragment with an exemption; no fragment).

---

## R6. Tests and artifacts that change

**Decision**:
- **`ContractPaths`** (test fixtures): `+ JURISDICTIONS`.
- **`ReferenceDataOpenApiContractTest`**: `ATTRIBUTE_NAMES` gains `jurisdictions` after `location_types`; the description string gains it. `ReferenceDataResponse` assertions are unchanged.
- **`ReferenceDataFunctionalTest`**:
  - the unsupported-name list loses `jurisdictions` (now 13 E-Links names), its comment says so, and it gains the case variants `Jurisdictions`, `JURISDICTIONS` and `jurisdictions_` (EC-008); it adds 009 to its list of covered scenarios;
  - the missing-token test's unsupported path changes from `/jurisdictions` to `/tickets` (AC-017).
- **`ReferenceDataSmokeTest`**: `+ JURISDICTIONS` in the collection latency guard.
- **`ReferenceDataExtensionIntegrationTest`**: expected to pass unchanged.

---

## R7. Functional tests, golden file and Postman

**Decision**:
- **`JurisdictionsFunctionalTest`** (new, `src/functionalTest`): AC-001 to AC-019 over real HTTP with `ReferenceDataHttp`, mirroring `LocationTypesFunctionalTest`'s structure. AC-009 loads the four collections over HTTP and checks resolution; AC-011 checks that 87 is unreferenced and served. `golden/jurisdictions.json` is captured from `./gradlew bootRun` with `curl -o` (compact, no trailing newline), and AC-004 compares against it.
- **Postman**: a "Reference Data › Jurisdictions" sub-folder, after "Location Types", with ten requests: collection; single record `84`; unreferenced record `87`; unknown `31`; malformed `abc`; query parameter; trailing slash; no token; invalid token; and `jurisdiction`. Each request has status, structure and key-value tests and uses only `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}` (AC-023). The existing "jurisdictions - unsupported" request becomes "tickets - unsupported", with the same tests.

---

## R8. Documentation

**Decision**:
- **README**: eight types; the `jurisdictions` row; a short paragraph on jurisdiction records (extract ids, 87 unreferenced); the base-location, location and location-type paragraphs say `jurisdiction_id` now resolves, and the sentence about the feature that adds `jurisdictions` goes.
- **CLAUDE.md**: § Current repo state gains 009; § Reference data adds jurisdiction names and ids to what was taken from the extracts, and the data-model § list gains 009's § 3.
- **002 contract**: one more "Extended by 009" pointer.
- **003 spec (DF-1)**, **007 spec (DF-5)** and **008 spec (DF-7)**: a one-line note that 009 closed them.
- **Contract delta**: `contracts/reference-data-api-jurisdictions.md`.
