# Quickstart: Reference Data API — Base Locations

**Feature**: `003-base-locations-reference-data` | **Contract delta**: [contracts/reference-data-api-base-locations.md](./contracts/reference-data-api-base-locations.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and token setup are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated. Expected messages and shapes are in the contracts.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, both collections (FR-015)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes: no main source mentions `appointment` or `base_location` (AC-018).
- `ReferenceDataFixtureRulesTest` passes for both fixtures (FR-009, research R4).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the change is configuration and data only

```bash
git diff --stat main -- src/main/java      # expected: no output (AC-017, SC-007)
git diff --stat main -- src/main/resources
```

**Expected**: no changes under `src/main/java`; under `src/main/resources`, only `application.yml` (one type entry) and the new `reference-data/base_locations.json`.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/base_locations \| jq '.results \| length'` | `1276` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/base_locations \| jq -c '[.results[0].id, .results[-1].id, ([.results[].id] == ([.results[].id] \| sort))]'` | `[10,12760,true]` | AC-001, AC-002 |
| 3 | `curl -s -H "$AUTH" $BASE/base_locations \| jq '[.results[].name] \| unique \| length'` | `1276` (all names distinct) | SC-002 |
| 4 | `curl -s -H "$AUTH" $BASE/base_locations/70` | Aldridge and Brownhills, `end_date` `2025-03-31` | AC-002, AC-005 |
| 5 | `curl -s -H "$AUTH" $BASE/base_locations/8880 \| jq -r .name` | `Royal Courts of Justice – Office of the Judge Advocate General`, with an en dash | AC-003 |
| 6 | `curl -s -H "$AUTH" $BASE/base_locations/8880 \| grep -c 'u2013'` | `0` (sent as a UTF-8 character, not an escape) | FR-010 |
| 7 | `curl -si -H "$AUTH" $BASE/base_location` | `400`, unsupported attribute | AC-009 |
| 8 | `curl -si -H "$AUTH" $BASE/base_location/70` | `400`, unsupported attribute | AC-010 |
| 9 | `curl -si -H "$AUTH" $BASE/base_locations/15` | `404`, record not found | AC-006 |
| 10 | `curl -si -H "$AUTH" $BASE/base_locations/abc` | `400`, malformed id | AC-007 |
| 11 | `curl -s -H "$AUTH" $BASE/base_locations/10 \| jq -r .name; curl -s -H "$AUTH" $BASE/appointment_titles/10 \| jq -r .name` | `Aberconwy`, then `Acting Senior Coroner` | AC-008, EC-005 |
| 12 | `curl -si $BASE/base_locations` | `401`, `WWW-Authenticate: Bearer` | AC-012 |
| 13 | `curl -si -H 'Authorization: Bearer wrong' $BASE/base_locations/70` | `401` | AC-013 |
| 14 | `curl -si -H "$AUTH" $BASE/genders` | `400`, unsupported attribute | AC-015 |
| 15 | `curl -si -H "$AUTH" "$BASE/base_locations?name=Aberconwy"` | `400`, query parameter message | AC-016 |
| 16 | `curl -s -H "$AUTH" $BASE/appointment_titles \| jq '.results \| length'` | `194` (unchanged) | AC-014 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
```

**Expected**: `["appointment_titles","base_locations","appointment_title"]`, with no `base_location` (AC-019). In Swagger UI (`/swagger-ui.html`), the `attribute_name` drop-down offers `base_locations`.

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the five in **Reference Data › Base Locations** (AC-020).
