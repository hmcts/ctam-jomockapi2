# Quickstart: Reference Data API — Genders

**Feature**: `005-genders-reference-data` | **Contract delta**: [contracts/reference-data-api-genders.md](./contracts/reference-data-api-genders.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and token setup are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated. Expected messages and shapes are in the contracts.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all four collections (FR-014)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes: no main source mentions `appointment`, `base_location`, `contract_type` or `gender` in any spelling (AC-018).
- `ReferenceDataFixtureRulesTest` passes for all four fixtures (FR-009, research R2).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the change is configuration and data only

```bash
git diff --stat main -- src/main/java      # expected: no output (AC-017, SC-007)
git diff --stat main -- src/main/resources
```

**Expected**: nothing under `src/main/java`; under `src/main/resources`, only `application.yml` (one type entry) and the new `reference-data/genders.json`.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/genders \| jq '.results \| length'` | `4` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/genders \| jq -c '[.results[] \| [.id, .name]]'` | `[[10,"Female"],[20,"Male"],[30,"Other"],[40,"Prefer not to say"]]` | AC-002, EC-008 |
| 3 | `curl -s -H "$AUTH" $BASE/genders \| jq -c '[.results[].end_date] \| unique'` | `[null]` | AC-003, EC-004 |
| 4 | `curl -s -H "$AUTH" $BASE/genders/20 \| jq -c 'keys_unsorted'` | `["id","name","created_at","updated_at","start_date","end_date"]` | AC-005, FR-005 |
| 5 | `curl -s -H "$AUTH" $BASE/genders/40 \| jq -r .name` | `Prefer not to say` | AC-003, EC-002 |
| 6 | `curl -si -H "$AUTH" $BASE/genders/15` | `404`, record not found | AC-006 |
| 7 | `curl -si -H "$AUTH" $BASE/genders/abc` | `400`, malformed id | AC-007 |
| 8 | `curl -s -H "$AUTH" $BASE/genders/10 \| jq -r .name; curl -s -H "$AUTH" $BASE/contract_types/10 \| jq -r .name` | `Female`, then `Fee-paid` | AC-008, EC-006 |
| 9 | `curl -si -H "$AUTH" $BASE/gender` | `400`, unsupported attribute | AC-009 |
| 10 | `curl -si -H "$AUTH" $BASE/gender/20` | `400`, unsupported attribute | AC-010 |
| 11 | `curl -si $BASE/genders` | `401`, `WWW-Authenticate: Bearer` | AC-012 |
| 12 | `curl -si -H 'Authorization: Bearer wrong' $BASE/genders/20` | `401` | AC-013 |
| 13 | `curl -si -H "$AUTH" "$BASE/genders?name=Male"` | `400`, query parameter message | AC-016 |
| 14 | `curl -si -H "$AUTH" $BASE/jurisdictions` | `400`, unsupported attribute | AC-015 |
| 15 | `curl -s -H "$AUTH" $BASE/contract_types \| jq '.results \| length'` | `14` (unchanged) | AC-014 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
```

**Expected**: `["appointment_titles","base_locations","contract_types","genders","appointment_title"]`, with no `gender` (AC-019).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the nine in **Reference Data › Genders** and the unsupported-type example, now `jurisdictions` (AC-020).
