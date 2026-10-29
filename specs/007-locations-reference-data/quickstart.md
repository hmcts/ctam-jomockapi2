# Quickstart: Reference Data API — Locations

**Feature**: `007-locations-reference-data` | **Contract delta**: [contracts/reference-data-api-locations.md](./contracts/reference-data-api-locations.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and setting up a token are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated here. The contracts give the expected messages and shapes.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all six collections (FR-016)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes. No main source mentions `appointment`, `base_location`, `contract_type`, `gender`, `judiciary_role` or `location` in any spelling (AC-023).
- `ReferenceDataFixtureRulesTest` passes for all six fixtures: `locations.json` under the `SOURCE` id rule, and the others under 002's positional rule (FR-017).
- `ReferenceDataReferencesTest` passes: every Location and BaseLocation `parent_id` resolves (FR-012).
- `FixtureReferenceDataRepositoryTest` and `ReferenceDataTypeRegistryTest` pass, including the nullable-field cases (AC-022).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the scope of the main-code change

```bash
git diff --stat main -- src/main/java
git diff --stat main -- src/main/resources
grep -c '\\u' src/main/resources/reference-data/locations.json          # expected: 0
git diff --quiet main -- src/functionalTest/resources/golden/ && echo "existing golden files unchanged"
```

**Expected**:
- **Under `src/main/java`**: only the generic nullable-reference-field capability, in these files:
  - `ReferenceDataProperties`, `ReferenceDataTypeRegistry`, `ReferenceDataType` (whose comment is also reworded);
  - `ReferenceDataRecord`, `FixtureReferenceDataRepository`;
  - `ReferenceDataResponse`, the new `ReferenceFieldValue`;
  - `ReferenceDataMapper`, `ReferenceDataService`.

  None of them names a type (AC-021).
- **Under `src/main/resources`**: `application.yml` (one type entry) and the new `reference-data/locations.json`.
- **Golden files**: the last command prints its message. The existing golden files are unchanged; only `golden/locations.json` is new and untracked (AC-018).

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/locations \| jq '.results \| length'` | `2000` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/locations \| jq -c '[.results[0], .results[-1] \| [.id, .name]]'` | `[[1,"Aberconwy"],[2433,"Kent and Medway"]]` | AC-002 |
| 3 | `curl -s -H "$AUTH" $BASE/locations \| jq -c '[.results[] \| select(.end_date != null) \| .id] \| [length, first, last]'` | `[285,9,2364]` | AC-004 |
| 4 | `curl -s -H "$AUTH" $BASE/locations \| jq '[.results[] \| select(.parent_id == null)] \| length'` | `142` | AC-010, EC-005 |
| 5 | `curl -s -H "$AUTH" $BASE/locations \| jq '[.results[] \| select(has("parent_id") \| not)] \| length'` | `0` (the key is always present) | FR-005 |
| 6 | `curl -s -H "$AUTH" $BASE/locations \| jq '(.results \| map(.id)) as $ids \| [.results[] \| .parent_id \| select(. != null) \| select(IN($ids[]) \| not)] \| length'` | `0` | AC-010 |
| 7 | `curl -s -H "$AUTH" $BASE/locations/1 \| jq -c '[.name, .type_id, .parent_id, .jurisdiction_id]'` | `["Aberconwy",8,1722,84]` | AC-006 |
| 8 | `curl -s -H "$AUTH" $BASE/locations/2141` | the record with `"parent_id":null` (contract § 4) | AC-006, EC-005 |
| 9 | `curl -s -H "$AUTH" $BASE/locations/1176 \| jq '.name'` | `"Cheshire  LJA"` (two spaces) | AC-003, EC-003 |
| 10 | `curl -s -H "$AUTH" $BASE/locations/2398 \| jq -r .name` | `Royal Courts of Justice – Office of the Judge Advocate General` | AC-003, EC-004 |
| 11 | `curl -s -H "$AUTH" $BASE/locations \| jq '[.results[] \| select(.name == "National")] \| length'` | `40` | EC-002 |
| 12 | `curl -s -H "$AUTH" $BASE/locations/20 \| jq -c 'keys_unsorted'` | `["id","name","type_id","parent_id","jurisdiction_id","created_at","updated_at","start_date","end_date"]` | FR-005 |
| 13 | `curl -si -H "$AUTH" $BASE/locations/2` | `404`, record not found | AC-007, EC-001 |
| 14 | `curl -si -H "$AUTH" $BASE/locations/2434` | `404`, record not found | AC-007 |
| 15 | `curl -si -H "$AUTH" $BASE/locations/abc` | `400`, malformed id | AC-008 |
| 16 | `curl -s -H "$AUTH" $BASE/locations/10 \| jq -r .name; curl -s -H "$AUTH" $BASE/base_locations/10 \| jq -r .name` | `Atherstone and Coleshill`, then `Aberconwy` | AC-009, EC-009 |
| 17 | `curl -si -H "$AUTH" $BASE/location` | `400`, unsupported attribute | AC-013 |
| 18 | `curl -si -H "$AUTH" $BASE/location/1` | `400`, unsupported attribute | AC-014 |
| 19 | `curl -si $BASE/locations` | `401`, `WWW-Authenticate: Bearer` | AC-016 |
| 20 | `curl -si -H 'Authorization: Bearer wrong' $BASE/locations/1` | `401` | AC-017 |
| 21 | `curl -si -H "$AUTH" "$BASE/locations?parent_id=1722"` | `400`, query parameter message | AC-020 |
| 22 | `curl -s -H "$AUTH" $BASE/base_locations \| jq '[.results[] \| select((has("parent_id") \| not) or .parent_id == null)] \| length'` | `0`: every base location still has a positive `parent_id` | AC-018 |
| 23 | `jq -n --slurpfile l <(curl -s -H "$AUTH" $BASE/locations) --slurpfile b <(curl -s -H "$AUTH" $BASE/base_locations) '($l[0].results \| map(.id)) as $ids \| [$b[0].results[].parent_id \| select(IN($ids[]) \| not)] \| length'` | `0`: every base-location parent is a location | AC-011 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.components.schemas.ReferenceDataResponse.properties.parent_id, (.components.schemas | has("ReferenceFieldValue"))'
```

**Expected**:
- **First command**: `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","locations","appointment_title"]`, with no `location`.
- **Second command**: a `parent_id` schema that is an `int64` integer and nullable (`"nullable": true`, or `"null"` among its types), followed by `false` (AC-024).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the ten in **Reference Data › Locations** (AC-025).
