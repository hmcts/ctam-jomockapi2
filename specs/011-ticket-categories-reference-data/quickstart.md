# Quickstart: Reference Data API — Ticket Categories

**Feature**: `011-ticket-categories-reference-data` | **Contract delta**: [contracts/reference-data-api-ticket-categories.md](./contracts/reference-data-api-ticket-categories.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and setting up a token are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated here. The contracts give the expected messages and shapes.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all ten collections (FR-017)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- The new unit cases for `parent_category_id` pass in the registry, repository, mapper, serialisation and controller tests (AC-024, FR-019).
- `NoTypeSpecificCodeTest` passes with the new `ticket_categories` fragment, and its new cases show it matches the plural spellings but not `ticket_category_id` or `parent_category_id` (FR-018).
- `ReferenceDataFixtureRulesTest` passes for all ten fixtures, with `ticket_categories.json`'s `type_id` pinned to 1–5 and the jurisdiction pin including 87 (FR-012, FR-013).
- `ReferenceDataReferencesTest` passes: the three new rows resolve, the category hierarchy has no cycles, children agree with their parent's jurisdiction, and the completeness check treats `ticket_categories.type_id` as a reference to the unserved `ticket_category_types` (AC-010, AC-012 to AC-014).
- The existing golden files match byte for byte (AC-020).

## 2. Confirm the scope of the main-code change

```bash
git diff --stat origin/main -- src/main/java
git diff --stat origin/main -- src/main/resources
git diff --quiet origin/main -- src/functionalTest/resources/golden/ && echo "existing golden files unchanged"
git grep -n -i -E 'ticket_categories|ticket categories|TicketCategories' -- src/main/java      # expected: no output
```

**Expected**:
- **Under `src/main/java`**: exactly `ReferenceField`, `ReferenceDataRecord`, `ReferenceDataResponse`, `ReferenceDataMapper` and `ReferenceDataProperties` (Javadoc only) (AC-023, SC-009).
- **Under `src/main/resources`**: `application.yml` (the type entry and the `tickets` comment) and the new `reference-data/ticket_categories.json`.
- **Golden files**: the third command prints its message; only `golden/ticket_categories.json` is new.
- **Guard**: no main source names the plural type.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq '.results \| length'` | `54` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[0], .results[-1] \| [.id, .name, .type_id, .jurisdiction_id, .parent_category_id]]'` | `[[31,"Civil",1,30,null],[88,"Languages",5,87,null]]` | AC-002 |
| 3 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[] \| select(.end_date != null) \| .id]'` | `[38,45,52,59,66,73,80]` | AC-003 |
| 4 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[] \| select(.name == "Immigration and Asylum Chamber") \| [.id, .parent_category_id]]'` | `[[38,35],[45,43]]` | AC-004, EC-002 |
| 5 | `curl -s -H "$AUTH" $BASE/ticket_categories/31 \| jq -c 'keys_unsorted'` | `["id","name","type_id","jurisdiction_id","parent_category_id","created_at","updated_at","start_date","end_date"]` | FR-005 |
| 6 | `curl -s -H "$AUTH" $BASE/ticket_categories/56` | the child record in contract § 4 | AC-006 |
| 7 | `for id in 1 30 34 84 89 999999; do curl -s -o /dev/null -w '%{http_code} ' -H "$AUTH" $BASE/ticket_categories/$id; done` | `404 404 404 404 404 404` | AC-007, EC-001 |
| 8 | `curl -si -H "$AUTH" $BASE/ticket_categories/abc` | `400`, malformed id | AC-008 |
| 9 | `curl -s -H "$AUTH" $BASE/ticket_categories/31 \| jq -r .name; curl -s -H "$AUTH" $BASE/locations/31 \| jq -r .name` | `Civil`, then `Bexley L317` | AC-009, EC-006 |
| 10 | `C=$(curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[].id]'); curl -s -H "$AUTH" $BASE/tickets \| jq --argjson c "$C" '[.results[] \| select(.ticket_category_id as $x \| $c \| index($x) \| not)] \| length'` | `0`: every ticket's category resolves | AC-010, SC-004 |
| 11 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[] \| select(.parent_category_id == null) \| .id]'` | `[31,32,33,35,43,48,85,88]` | AC-012 |
| 12 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq '(.results \| map({key: (.id \| tostring), value: .}) \| from_entries) as $m \| [.results[] \| select(.parent_category_id != null) \| select($m[.parent_category_id \| tostring].jurisdiction_id != .jurisdiction_id or $m[.parent_category_id \| tostring].parent_category_id != null)] \| length'` | `0`: every parent is top-level and in the same jurisdiction | AC-012, AC-013 |
| 13 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[].jurisdiction_id] \| unique'` | `[30,34,84,87]` | AC-013 |
| 14 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[].type_id] \| group_by(.) \| map([.[0], length])'` | `[[1,3],[2,3],[3,46],[4,1],[5,1]]` | AC-014 |
| 15 | `for t in appointment_titles base_locations contract_types genders judiciary_roles locations location_types jurisdictions tickets; do curl -s -H "$AUTH" $BASE/$t \| jq -j '[.results[] \| select(has("parent_category_id"))] \| length'; echo -n ' '; done` | `0` nine times | FR-014 (also automated by TicketCategoriesFunctionalTest) |
| 16 | `curl -si -H "$AUTH" $BASE/ticket_category` | `400`, unsupported attribute | AC-017 |
| 17 | `curl -si -H "$AUTH" $BASE/ticket_category/31` | `400`, unsupported attribute | AC-018 |
| 18 | `curl -si $BASE/ticket_categories` | `401`, `WWW-Authenticate: Bearer` | AC-015 |
| 19 | `curl -si -H 'Authorization: Bearer wrong' $BASE/ticket_categories/31` | `401` | AC-016 |
| 20 | `curl -si -H "$AUTH" "$BASE/ticket_categories?parent_category_id=48"` | `400`, query parameter message | AC-022 |
| 21 | `curl -si -H "$AUTH" $BASE/ticket_category_types` | `400`, unsupported attribute | AC-021, EC-008 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.components.schemas.ReferenceDataResponse.properties.parent_category_id'
```

**Expected**:
- `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","locations","location_types","jurisdictions","tickets","ticket_categories","appointment_title"]`, with no `ticket_category` (AC-025).
- An `int64` integer property that is nullable, whose description cites deviation D-6 (AC-025, FR-015).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the eleven in **Reference Data › Ticket Categories** and the "ticket_category_types - unsupported" request (AC-026).
