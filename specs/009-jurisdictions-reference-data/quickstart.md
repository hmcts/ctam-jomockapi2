# Quickstart: Reference Data API — Jurisdictions

**Feature**: `009-jurisdictions-reference-data` | **Contract delta**: [contracts/reference-data-api-jurisdictions.md](./contracts/reference-data-api-jurisdictions.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and setting up a token are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated here. The contracts give the expected messages and shapes.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all eight collections (FR-016)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes with the new `jurisdictions` fragment, and its new cases show the fragment matches the plural spellings but not `jurisdiction_id` (AC-021, FR-018).
- `ReferenceDataFixtureRulesTest` passes for all eight fixtures: `locations.json`, `location_types.json` and `jurisdictions.json` under the `SOURCE` id rule, and the others under 002's positional rule (FR-017).
- `ReferenceDataReferencesTest` passes: every Location, BaseLocation and LocationType `jurisdiction_id` resolves to a jurisdiction, and the completeness check lists every reference field of every served type (FR-011, AC-010).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the scope of the change

```bash
git diff --stat origin/main -- src/main/java                                   # expected: no output
git diff --stat origin/main -- src/main/resources
git diff --quiet origin/main -- src/functionalTest/resources/golden/ && echo "existing golden files unchanged"
```

**Expected**:
- **Under `src/main/java`**: nothing (AC-020, SC-009).
- **Under `src/main/resources`**: `application.yml` (one type entry and three updated comments) and the new `reference-data/jurisdictions.json`.
- **Golden files**: the last command prints its message. The existing golden files are unchanged; only `golden/jurisdictions.json` is new and untracked (AC-017, SC-008).

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/jurisdictions \| jq -c '[.results[] \| [.id, .name]]'` | `[[30,"Courts"],[34,"Tribunals"],[84,"Magistrates"],[86,"Coroners"],[87,"Skills"]]` | AC-001, AC-002, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/jurisdictions \| jq -c '[.results[] \| select(.end_date != null) \| .id]'` | `[]` | AC-003, EC-004 |
| 3 | `curl -s -H "$AUTH" $BASE/jurisdictions/84 \| jq -c 'keys_unsorted'` | `["id","name","created_at","updated_at","start_date","end_date"]` | FR-005 |
| 4 | `curl -s -H "$AUTH" $BASE/jurisdictions/84` | the record in contract § 4 | AC-005 |
| 5 | `curl -s -H "$AUTH" $BASE/jurisdictions/87 \| jq -r .name` | `Skills` | AC-005, AC-011 |
| 6 | `for id in 1 31 88 999; do curl -s -o /dev/null -w '%{http_code} ' -H "$AUTH" $BASE/jurisdictions/$id; done` | `404 404 404 404` | AC-006, EC-001 |
| 7 | `curl -si -H "$AUTH" $BASE/jurisdictions/abc` | `400`, malformed id | AC-007 |
| 8 | `curl -s -H "$AUTH" $BASE/jurisdictions/84 \| jq -r .name; curl -s -H "$AUTH" $BASE/locations/84 \| jq -r .name` | `Magistrates`, then `Central Devon Sub Committee` | AC-008, EC-006 |
| 9 | `curl -si -H "$AUTH" $BASE/jurisdiction` | `400`, unsupported attribute | AC-012 |
| 10 | `curl -si -H "$AUTH" $BASE/jurisdiction/30` | `400`, unsupported attribute | AC-013 |
| 11 | `curl -si $BASE/jurisdictions` | `401`, `WWW-Authenticate: Bearer` | AC-015 |
| 12 | `curl -si -H 'Authorization: Bearer wrong' $BASE/jurisdictions/30` | `401` | AC-016 |
| 13 | `curl -si -H "$AUTH" "$BASE/jurisdictions?name=Courts"` | `400`, query parameter message | AC-019 |
| 14 | `curl -si -H "$AUTH" $BASE/tickets` | `400`, unsupported attribute | AC-018 |
| 15 | `J=$(curl -s -H "$AUTH" $BASE/jurisdictions \| jq -c '[.results[].id]'); for t in locations base_locations location_types; do curl -s -H "$AUTH" $BASE/$t \| jq --argjson j "$J" '[.results[] \| select(.jurisdiction_id as $x \| $j \| index($x) \| not)] \| length'; done` | `0`, `0`, `0`: every `jurisdiction_id` resolves | AC-009, SC-004 |
| 16 | `for t in locations base_locations location_types; do curl -s -H "$AUTH" $BASE/$t \| jq '[.results[] \| select(.jurisdiction_id == 87)] \| length'; done` | `0`, `0`, `0`: Skills is unreferenced | AC-011, EC-003 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
```

**Expected**: `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","locations","location_types","jurisdictions","appointment_title"]`, with no `jurisdiction` (AC-022).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the ten in **Reference Data › Jurisdictions** and the "tickets - unsupported" request (AC-023).
