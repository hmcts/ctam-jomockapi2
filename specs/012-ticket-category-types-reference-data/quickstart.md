# Quickstart: Reference Data API — Ticket Category Types

**Feature**: `012-ticket-category-types-reference-data` | **Contract delta**: [contracts/reference-data-api-ticket-category-types.md](./contracts/reference-data-api-ticket-category-types.md) | **Data model**: [data-model.md](./data-model.md)

This guide checks the feature end to end. Building, running and setting up a token are as in [002's quickstart](../002-reference-data-api/quickstart.md) §1–§2 and are not repeated here. The contracts give the expected messages and shapes.

## 1. Build with every quality gate

```bash
./gradlew check -PskipOwasp                     # local; CI runs plain ./gradlew check
./gradlew smokeTest -Dperf.strict=true          # strict 100 ms p95 target, all eleven collections (FR-018)
```

**Expected**: `BUILD SUCCESSFUL`. In particular:
- `NoTypeSpecificCodeTest` passes with the new `ticket_category_type` fragment, and its new cases show it matches the singular and plural spellings but not `ticket_category_id`, `ticket_categories` or "ticket category" (FR-020).
- `ReferenceDataFixtureRulesTest` passes for all eleven fixtures, with every ticket-category-type record checked as a reference point and its `integration_code` format checked (FR-013, FR-019).
- `ReferenceDataReferencesTest` passes: the two new rows resolve, every ticket category agrees with its type's jurisdiction, and the completeness check demands both rows (AC-010 to AC-013).
- The existing golden files match byte for byte (AC-019).

## 2. Confirm there is no main-code change

```bash
git diff --stat origin/main -- src/main/java
git diff --stat origin/main -- src/main/resources
git diff --quiet origin/main -- src/functionalTest/resources/golden/ && echo "existing golden files unchanged"
git grep -n -i -E 'ticket.?category.?types' -- src/main/java      # expected: no output
```

**Expected**:
- **Under `src/main/java`**: no output (AC-022, SC-009).
- **Under `src/main/resources`**: `application.yml` (the type entry and the `ticket_categories` comment) and the new `reference-data/ticket_category_types.json`.
- **Golden files**: the third command prints its message; only `golden/ticket_category_types.json` is new.
- **Guard**: no main source names the type.

## 3. Manual checks with curl

Start the service (`./gradlew bootRun`), then:

```bash
BASE=http://localhost:8080/api/v1/reference_data
AUTH='Authorization: Bearer local-dev-token'
```

| # | Command | Expected | Spec |
|---|---------|----------|------|
| 1 | `curl -s -H "$AUTH" $BASE/ticket_category_types \| jq '.results \| length'` | `5` | AC-001, SC-002 |
| 2 | `curl -s -H "$AUTH" $BASE/ticket_category_types \| jq -c '[.results[] \| [.id, .name, .jurisdiction_id, .integration_code]]'` | `[[1,"Court Type",30,"COURTS_COURT_TYPE"],[2,"Tribunal Type",34,"TRIBS_TYPE"],[3,"Tribunal Name",34,"TRIBS_NAME"],[4,"Court Type",84,"MAGS_COURT_TYPE"],[5,"Skills Category",87,"SKILLS_CATEGORY"]]` | AC-002, AC-004 |
| 3 | `curl -s -H "$AUTH" $BASE/ticket_category_types \| jq -c '[.results[] \| select(.end_date != null) \| .id]'` | `[]` | AC-003 |
| 4 | `curl -s -H "$AUTH" $BASE/ticket_category_types/1 \| jq -c 'keys_unsorted'` | `["id","name","jurisdiction_id","integration_code","created_at","updated_at","start_date","end_date"]` | FR-005 |
| 5 | `curl -s -H "$AUTH" $BASE/ticket_category_types/4` | the record in contract § 4 | AC-006 |
| 6 | `for id in 6 30 31 999999; do curl -s -o /dev/null -w '%{http_code} ' -H "$AUTH" $BASE/ticket_category_types/$id; done` | `404 404 404 404` | AC-007, EC-001 |
| 7 | `curl -si -H "$AUTH" $BASE/ticket_category_types/abc` | `400`, malformed id | AC-008 |
| 8 | `curl -s -H "$AUTH" $BASE/ticket_category_types/1 \| jq -r .name; curl -s -H "$AUTH" $BASE/location_types/1 \| jq -r .name` | `Court Type`, then `Region` | AC-009, EC-006 |
| 9 | `T=$(curl -s -H "$AUTH" $BASE/ticket_category_types \| jq -c '[.results[] \| {key: (.id \| tostring), value: .jurisdiction_id}] \| from_entries'); curl -s -H "$AUTH" $BASE/ticket_categories \| jq --argjson t "$T" '[.results[] \| select($t[.type_id \| tostring] != .jurisdiction_id)] \| length'` | `0`: every category's type resolves, in the same jurisdiction | AC-010, AC-011, SC-004 |
| 10 | `curl -s -H "$AUTH" $BASE/ticket_categories \| jq -c '[.results[].type_id] \| group_by(.) \| map([.[0], length])'` | `[[1,3],[2,3],[3,46],[4,1],[5,1]]` | AC-010 |
| 11 | `J=$(curl -s -H "$AUTH" $BASE/jurisdictions \| jq -c '[.results[].id]'); curl -s -H "$AUTH" $BASE/ticket_category_types \| jq --argjson j "$J" '[.results[] \| select(.jurisdiction_id as $x \| $j \| index($x) \| not)] \| length'` | `0`: every type's jurisdiction resolves | AC-012 |
| 12 | `for t in ticket_categories ticket_category_types; do curl -s -H "$AUTH" $BASE/$t \| jq -cj '[.results[] \| select(.jurisdiction_id == 87) \| .id]'; echo -n ' '; done` | `[88] [5]` | AC-013, FR-014 |
| 13 | `for f in ticket_category_id parent_category_id; do for t in appointment_titles base_locations contract_types genders judiciary_roles locations location_types jurisdictions tickets ticket_categories ticket_category_types; do curl -s -H "$AUTH" $BASE/$t \| jq -j --arg f $f '[.results[] \| select(has($f))] \| length'; echo -n ' '; done; echo; done` | the first line is `0` everywhere except `tickets`; the second is `0` everywhere except `ticket_categories` | AC-019 |
| 14 | `curl -si -H "$AUTH" $BASE/ticket_category_type` | `400`, unsupported attribute | AC-014, FR-021 |
| 15 | `curl -si -H "$AUTH" $BASE/ticket_category_type/1` | `400`, unsupported attribute | AC-015 |
| 16 | `curl -si $BASE/ticket_category_types` | `401`, `WWW-Authenticate: Bearer` | AC-017 |
| 17 | `curl -si -H 'Authorization: Bearer wrong' $BASE/ticket_category_types/1` | `401` | AC-018 |
| 18 | `curl -si -H "$AUTH" "$BASE/ticket_category_types?jurisdiction_id=30"` | `400`, query parameter message | AC-021 |
| 19 | `for n in base_location contract_type gender judiciary_role jurisdiction location location_type ticket ticket_category ticket_category_type; do curl -s -o /dev/null -w '%{http_code} ' -H "$AUTH" $BASE/$n; done` | `400` ten times | AC-020, SC-006 |

## 4. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs \
  | jq -c '.paths["/api/v1/reference_data/{attribute_name}"].get.parameters[] | select(.name=="attribute_name") | .schema.enum'
```

**Expected**: `["appointment_titles","base_locations","contract_types","genders","judiciary_roles","locations","location_types","jurisdictions","tickets","ticket_categories","ticket_category_types","appointment_title"]`, with no `ticket_category_type`; `ReferenceDataResponse` unchanged (AC-024).

## 5. Postman

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

**Expected**: every request passes, including the ten in **Reference Data › Ticket Category Types** and the renamed "ticket_category_type - unsupported (unserved singular)" request (AC-025).
