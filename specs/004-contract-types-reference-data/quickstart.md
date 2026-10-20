# Quickstart: Reference Data API — Contract Types

**Feature**: `004-contract-types-reference-data` | **Contract delta**: [contracts/reference-data-api-contract-types.md](./contracts/reference-data-api-contract-types.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and token setup are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated. Expected messages and shapes are in the contracts.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all three collections (FR-014)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes: no main source mentions `appointment`, `base_location` or `contract_type` in any spelling (AC-019).
- `ReferenceDataFixtureRulesTest` passes for all three fixtures, including the boolean-field rules (FR-009, research R4).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the change is configuration, data and type-agnostic code

```bash
git diff --stat main -- src/main/java      # expected: the generic boolean fields only (AC-018, SC-007)
git diff --stat main -- src/main/resources
```

**Expected**: under `src/main/java`, only the boolean-field declaration, model, response property and validation (FR-017, FR-018; research R2, R3), with no type named; under `src/main/resources`, only `application.yml` (one type entry) and the new `reference-data/contract_types.json`.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/contract_types \| jq '.results \| length'` | `14` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/contract_types \| jq -c '[.results[].name]'` | `["Fee-paid","SPTW-10%",…,"SPTW-90%","Salaried","Unknown","Voluntary"]` | AC-002, EC-008 |
| 3 | `curl -s -H "$AUTH" $BASE/contract_types \| jq -c '[.results[] \| select(.salaried) ] \| length'` | `11` | AC-003, SC-002 |
| 4 | `curl -s -H "$AUTH" $BASE/contract_types \| jq -c '[.results[] \| select(.salaried == false) \| .name]'` | `["Fee-paid","Unknown","Voluntary"]` | AC-003 |
| 5 | `curl -s -H "$AUTH" $BASE/contract_types/120 \| jq -c 'keys_unsorted'` | `["id","name","salaried","created_at","updated_at","start_date","end_date"]` | AC-006, FR-005 |
| 6 | `curl -s -H "$AUTH" $BASE/contract_types/70 \| jq -c '[.name, .salaried, .end_date]'` | `["SPTW-60%",true,"2025-03-31"]` | AC-002, EC-004 |
| 7 | `curl -s -H "$AUTH" $BASE/contract_types/90 \| jq -r .end_date` | `null` (the extract's end date for SPTW-75% is not reproduced) | EC-004 |
| 8 | `curl -si -H "$AUTH" $BASE/contract_types/15` | `404`, record not found | AC-007 |
| 9 | `curl -si -H "$AUTH" $BASE/contract_types/abc` | `400`, malformed id | AC-008 |
| 10 | `curl -s -H "$AUTH" $BASE/contract_types/10 \| jq -r .name; curl -s -H "$AUTH" $BASE/base_locations/10 \| jq -r .name` | `Fee-paid`, then `Aberconwy` | AC-009, EC-006 |
| 11 | `curl -si -H "$AUTH" $BASE/contract_type` | `400`, unsupported attribute | AC-010 |
| 12 | `curl -si -H "$AUTH" $BASE/contract_type/70` | `400`, unsupported attribute | AC-011 |
| 13 | `curl -si $BASE/contract_types` | `401`, `WWW-Authenticate: Bearer` | AC-013 |
| 14 | `curl -si -H 'Authorization: Bearer wrong' $BASE/contract_types/120` | `401` | AC-014 |
| 15 | `curl -si -H "$AUTH" "$BASE/contract_types?salaried=true"` | `400`, query parameter message | AC-017 |
| 16 | `curl -s -H "$AUTH" $BASE/appointment_titles/70 \| jq 'has("salaried")'; curl -s -H "$AUTH" $BASE/base_locations/70 \| jq 'has("salaried")'` | `false`, `false` | AC-015, SC-006 |
| 17 | `curl -si -H "$AUTH" $BASE/genders` | `400`, unsupported attribute | AC-016 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '[(.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum), .components.schemas.ReferenceDataResponse.properties.salaried.type]'
```

**Expected**: `[["appointment_titles","base_locations","contract_types","appointment_title"],"boolean"]`, with no `contract_type` (AC-020).

## 5. Start-up validation

Covered by unit tests (AC-021); to see it by hand, temporarily change one record's `salaried` in `contract_types.json` to `1` and run `./gradlew bootRun`.

**Expected**: start-up fails with an `Invalid reference-data fixture classpath:reference-data/contract_types.json` error. Revert the edit.

## 6. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the ten in **Reference Data › Contract Types** (AC-022).
