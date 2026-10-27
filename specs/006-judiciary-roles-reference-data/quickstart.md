# Quickstart: Reference Data API — Judiciary Roles

**Feature**: `006-judiciary-roles-reference-data` | **Contract delta**: [contracts/reference-data-api-judiciary-roles.md](./contracts/reference-data-api-judiciary-roles.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and token setup are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated. Expected messages and shapes are in the contracts.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all five collections (FR-014)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes: no main source mentions `appointment`, `base_location`, `contract_type`, `gender` or `judiciary_role` in any spelling (AC-019).
- `ReferenceDataFixtureRulesTest` passes for all five fixtures, including the new check that no fixture contains a `\u` escape (FR-008, FR-009, research R2–R3).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the change is configuration and data only

```bash
git diff --stat main -- src/main/java      # expected: no output (AC-018, SC-008)
git diff --stat main -- src/main/resources
grep -c '\\u' src/main/resources/reference-data/judiciary_roles.json   # expected: 0 (FR-008)
```

**Expected**: nothing under `src/main/java`; under `src/main/resources`, only `application.yml` (one type entry) and the new `reference-data/judiciary_roles.json`.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/judiciary_roles \| jq '.results \| length'` | `164` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/judiciary_roles \| jq -c '[.results[0], .results[-1] \| [.id, .name]]'` | `[[10,"Acting Designated Civil Judge"],[1640,"Wales Training Course Director (JC)"]]` | AC-002 |
| 3 | `curl -s -H "$AUTH" $BASE/judiciary_roles \| jq -c '[.results[] \| select(.end_date != null) \| .id] \| [length, first, last]'` | `[23,70,1610]` | AC-004 |
| 4 | `curl -s -H "$AUTH" $BASE/judiciary_roles \| jq -c '[.results[] \| select(.id >= 320 and .id <= 340) \| .name]'` | `["Course Director Long and Complex Trials (JC)","Course Director Youth Training (JC)","Course Director for COP (JC)"]` | EC-008 |
| 5 | `curl -s -H "$AUTH" $BASE/judiciary_roles/960 \| jq -r .name` | `Judge in Charge of Jury List (King’s Bench)` | AC-006, EC-002 |
| 6 | `curl -s -H "$AUTH" $BASE/judiciary_roles/970 \| jq -r .name` | `Judge in Charge of Jury List (Queen's Bench)` | AC-003, EC-003 |
| 7 | `curl -s -H "$AUTH" $BASE/judiciary_roles/150 \| jq -r .name` | `Boundary Commission for Wales – Deputy Chair` | AC-003, EC-002 |
| 8 | `curl -s -H "$AUTH" $BASE/judiciary_roles/1070 \| jq -r .name` | `Judicial Commissioner Investigatory Powers Commiss` | AC-003, EC-005 |
| 9 | `curl -s -H "$AUTH" $BASE/judiciary_roles/20 \| jq -c 'keys_unsorted'` | `["id","name","created_at","updated_at","start_date","end_date"]` | AC-006, FR-005 |
| 10 | `curl -si -H "$AUTH" $BASE/judiciary_roles/15` | `404`, record not found | AC-007 |
| 11 | `curl -si -H "$AUTH" $BASE/judiciary_roles/1650` | `404`, record not found | AC-007, EC-009 |
| 12 | `curl -si -H "$AUTH" $BASE/judiciary_roles/abc` | `400`, malformed id | AC-008 |
| 13 | `curl -s -H "$AUTH" $BASE/judiciary_roles/10 \| jq -r .name; curl -s -H "$AUTH" $BASE/genders/10 \| jq -r .name` | `Acting Designated Civil Judge`, then `Female` | AC-009, EC-010 |
| 14 | `curl -si -H "$AUTH" $BASE/judiciary_role` | `400`, unsupported attribute | AC-010 |
| 15 | `curl -si -H "$AUTH" $BASE/judiciary_role/20` | `400`, unsupported attribute | AC-011 |
| 16 | `curl -si $BASE/judiciary_roles` | `401`, `WWW-Authenticate: Bearer` | AC-013 |
| 17 | `curl -si -H 'Authorization: Bearer wrong' $BASE/judiciary_roles/20` | `401` | AC-014 |
| 18 | `curl -si -H "$AUTH" "$BASE/judiciary_roles?name=Mentor%20Judge"` | `400`, query parameter message | AC-017 |
| 19 | `curl -si -H "$AUTH" $BASE/jurisdictions` | `400`, unsupported attribute | AC-016 |
| 20 | `curl -s -H "$AUTH" $BASE/genders \| jq '.results \| length'` | `4` (unchanged) | AC-015 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
```

**Expected**: `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","appointment_title"]`, with no `judiciary_role` (AC-020).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the nine in **Reference Data › Judiciary Roles** and the unsupported-type example, now `jurisdictions` (AC-021).
