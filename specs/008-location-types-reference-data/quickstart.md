# Quickstart: Reference Data API — Location Types

**Feature**: `008-location-types-reference-data` | **Contract delta**: [contracts/reference-data-api-location-types.md](./contracts/reference-data-api-location-types.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and setting up a token are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated here. The contracts give the expected messages and shapes.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all seven collections (FR-016)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes unchanged (AC-022, FR-018).
- `ReferenceDataFixtureRulesTest` passes for all seven fixtures: `locations.json` and `location_types.json` under the `SOURCE` id rule, and the others under 002's positional rule (FR-017).
- `ReferenceDataReferencesTest` passes: every Location and BaseLocation `type_id` resolves to a location type, and their jurisdictions agree with their types' (FR-011, FR-012).
- `FixtureReferenceDataRepositoryTest` and `ReferenceDataTypeRegistryTest` pass, including the text-field cases (AC-022).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the scope of the main-code change

```bash
git diff --stat origin/main -- src/main/java
git diff --stat origin/main -- src/main/resources
git grep -n -i -E 'location|appointment|contract_type|gender|judiciary' -- src/main/java    # expected: no output
git diff --quiet origin/main -- src/functionalTest/resources/golden/ && echo "existing golden files unchanged"
```

**Expected**:
- **Under `src/main/java`**: only the generic text-field capability, in these files: `ReferenceDataProperties`, `ReferenceDataTypeRegistry`, `ReferenceDataType`, `DeclaredField` (Javadoc), `ReferenceDataRecord`, `FixtureReferenceDataRepository`, `ReferenceDataResponse`, and the new `TextField`. None names a type (AC-021, SC-010).
- **Under `src/main/resources`**: `application.yml` (one type entry and two updated comments) and the new `reference-data/location_types.json`.
- **Golden files**: the last command prints its message. The existing golden files are unchanged; only `golden/location_types.json` is new and untracked (AC-018, SC-009).

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/location_types \| jq '.results \| length'` | `16` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/location_types \| jq -c '[.results[0], .results[-1] \| [.id, .name, .integration_code]]'` | `[[1,"Region","COURTS_REGION"],[16,"Geographic Area","CORONERS_AREA"]]` | AC-002 |
| 3 | `curl -s -H "$AUTH" $BASE/location_types \| jq -c '[.results[] \| select(.end_date != null) \| .id]'` | `[7,14]` | AC-004 |
| 4 | `curl -s -H "$AUTH" $BASE/location_types \| jq -c '[.results[] \| select(.name == "Region") \| [.id, .jurisdiction_id]]'` | `[[1,30],[6,84],[14,86]]` | AC-003, EC-002 |
| 5 | `curl -s -H "$AUTH" $BASE/location_types \| jq '[.results[].name] \| unique \| length'` | `13` | SC-002 |
| 6 | `curl -s -H "$AUTH" $BASE/location_types/10 \| jq -c 'keys_unsorted'` | `["id","name","jurisdiction_id","integration_code","created_at","updated_at","start_date","end_date"]` | FR-005 |
| 7 | `curl -s -H "$AUTH" $BASE/location_types/14` | the ended record in contract § 4 | AC-006 |
| 8 | `curl -s -H "$AUTH" $BASE/location_types/10 \| jq -c '[.name, .jurisdiction_id, .integration_code]'` | `["Area or Parent AC",84,"MAGS_AREA_OR_PARENT_AC"]` | AC-002 |
| 9 | `curl -si -H "$AUTH" $BASE/location_types/17` | `404`, record not found | AC-007, EC-001 |
| 10 | `curl -si -H "$AUTH" $BASE/location_types/abc` | `400`, malformed id | AC-008 |
| 11 | `curl -s -H "$AUTH" $BASE/location_types/1 \| jq -r .name; curl -s -H "$AUTH" $BASE/locations/1 \| jq -r .name` | `Region`, then `Aberconwy` | AC-009, EC-007 |
| 12 | `curl -si -H "$AUTH" $BASE/location_type` | `400`, unsupported attribute | AC-013 |
| 13 | `curl -si -H "$AUTH" $BASE/location_type/1` | `400`, unsupported attribute | AC-014 |
| 14 | `curl -si $BASE/location_types` | `401`, `WWW-Authenticate: Bearer` | AC-016 |
| 15 | `curl -si -H 'Authorization: Bearer wrong' $BASE/location_types/1` | `401` | AC-017 |
| 16 | `curl -si -H "$AUTH" "$BASE/location_types?jurisdiction_id=84"` | `400`, query parameter message | AC-020 |
| 17 | `jq -n --slurpfile t <(curl -s -H "$AUTH" $BASE/location_types) --slurpfile l <(curl -s -H "$AUTH" $BASE/locations) '($t[0].results \| map({key: (.id \| tostring), value: .jurisdiction_id}) \| from_entries) as $j \| [$l[0].results[] \| select($j[.type_id \| tostring] != .jurisdiction_id)] \| length'` | `0`: every location's type exists and has the same jurisdiction | AC-010, AC-011 |
| 18 | the same as 17 with `$BASE/base_locations` in place of `$BASE/locations` | `0` | AC-010, AC-011 |
| 19 | `curl -s -H "$AUTH" $BASE/locations \| jq '[.results[] \| select(has("integration_code"))] \| length'` | `0`: other types don't carry the new field | FR-005, FR-019 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.components.schemas.ReferenceDataResponse | [.properties.integration_code.type, (.required | index("integration_code"))]'
```

**Expected**:
- **First command**: `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","locations","location_types","appointment_title"]`, with no `location_type`.
- **Second command**: `["string",null]` (or `[["string"],null]` if the document writes types as an array): a string, with no `"null"` among its types, that is not required (AC-023).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the ten in **Reference Data › Location Types** (AC-024).
