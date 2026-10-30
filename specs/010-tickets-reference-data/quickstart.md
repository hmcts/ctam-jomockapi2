# Quickstart: Reference Data API — Tickets

**Feature**: `010-tickets-reference-data` | **Contract delta**: [contracts/reference-data-api-tickets.md](./contracts/reference-data-api-tickets.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and setting up a token are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated here. The contracts give the expected messages and shapes.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all nine collections (FR-016)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- The new unit cases for `ticket_category_id` pass in the registry, repository, mapper, serialisation and controller tests (AC-022, FR-019).
- `NoTypeSpecificCodeTest` passes with the new `tickets` fragment, and its new cases show the fragment matches the plural spellings but not `ticket_category_id` (FR-018).
- `ReferenceDataFixtureRulesTest` passes for all nine fixtures, `tickets.json` under the `SOURCE` id rule with its 41 category ids (FR-017).
- `ReferenceDataReferencesTest` passes: its completeness check maps `ticket_category_id` to `ticket_categories` and requires no row while that type isn't served (AC-011).
- The existing golden files match byte for byte (AC-018).
- The smoke suite reports a p95 guard per collection path.

## 2. Confirm the scope of the main-code change

```bash
git diff --stat origin/main -- src/main/java
git diff --stat origin/main -- src/main/resources
git diff --quiet origin/main -- src/functionalTest/resources/golden/ && echo "existing golden files unchanged"
git grep -n -i -E 'tickets' -- src/main/java                                      # expected: no output
```

**Expected**:
- **Under `src/main/java`**: exactly `ReferenceField`, `ReferenceDataRecord`, `ReferenceDataResponse`, `ReferenceDataMapper` and `ReferenceDataProperties` (Javadoc only) (AC-021, SC-009).
- **Under `src/main/resources`**: `application.yml` (one type entry) and the new `reference-data/tickets.json`.
- **Golden files**: the third command prints its message; only `golden/tickets.json` is new and untracked.
- **Guard**: no main source names the plural type.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/tickets \| jq '.results \| length'` | `159` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/tickets \| jq -c '[.results[0], .results[-1] \| [.id, .name, .ticket_category_id]]'` | `[[289,"Welsh Language",88],[1709,"Valuation Tribunal Wales",83]]` | AC-002 |
| 3 | `curl -s -H "$AUTH" $BASE/tickets \| jq -c '[.results[] \| select(.end_date != null) \| .id]'` | `[295,303,310,317,324,331,338,345,352,359,367,375,382,394,402,409,1416,1455,1493,1562,1600,1607]` | AC-003 |
| 4 | `curl -s -H "$AUTH" $BASE/tickets \| jq '[.results[].name] \| unique \| length'` | `152` | AC-004, SC-002 |
| 5 | `curl -s -H "$AUTH" $BASE/tickets \| jq -c '[.results[] \| select(.name == "Financial Remedy") \| [.id, .ticket_category_id]]'` | `[[409,31],[410,33]]` | AC-004, EC-002 |
| 6 | `curl -s -H "$AUTH" $BASE/tickets/294 \| jq -c 'keys_unsorted'` | `["id","name","ticket_category_id","created_at","updated_at","start_date","end_date"]` | FR-005 |
| 7 | `curl -s -H "$AUTH" $BASE/tickets/367` | the ended record in contract § 4 | AC-003, AC-006 |
| 8 | `for id in 1 288 297 1710 999999; do curl -s -o /dev/null -w '%{http_code} ' -H "$AUTH" $BASE/tickets/$id; done` | `404 404 404 404 404` | AC-007, EC-001 |
| 9 | `curl -si -H "$AUTH" $BASE/tickets/abc` | `400`, malformed id | AC-008 |
| 10 | `curl -s -H "$AUTH" $BASE/tickets/294 \| jq -r .name; curl -s -H "$AUTH" $BASE/locations/294 \| jq -r .name` | `Civil Authorisation`, then `Llanelli` | AC-009, EC-006 |
| 11 | `curl -s -H "$AUTH" $BASE/tickets \| jq -c '[.results[].ticket_category_id] \| unique \| [length, min, max]'` | `[41,31,88]` | AC-010 |
| 12 | `for t in appointment_titles base_locations contract_types genders judiciary_roles locations location_types jurisdictions; do curl -s -H "$AUTH" $BASE/$t \| jq '[.results[] \| select(has("ticket_category_id"))] \| length'; done` | `0` eight times | AC-012, FR-012 |
| 13 | `curl -si -H "$AUTH" $BASE/ticket` | `400`, unsupported attribute | AC-013 |
| 14 | `curl -si -H "$AUTH" $BASE/ticket/294` | `400`, unsupported attribute | AC-014 |
| 15 | `curl -si $BASE/tickets` | `401`, `WWW-Authenticate: Bearer` | AC-016 |
| 16 | `curl -si -H 'Authorization: Bearer wrong' $BASE/tickets/294` | `401` | AC-017 |
| 17 | `curl -si -H "$AUTH" "$BASE/tickets?ticket_category_id=31"` | `400`, query parameter message | AC-020 |
| 18 | `curl -si -H "$AUTH" $BASE/ticket_categories` | `400`, unsupported attribute | AC-019, EC-004 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.components.schemas.ReferenceDataResponse.properties.ticket_category_id'
```

**Expected**:
- `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","locations","location_types","jurisdictions","tickets","appointment_title"]`, with no `ticket` (AC-023).
- An `integer`/`int64` property, not nullable, whose description cites deviation D-5 (AC-023, FR-013).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the ten in **Reference Data › Tickets** and the "ticket_categories - unsupported" request (AC-024).
