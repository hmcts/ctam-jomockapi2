# Quickstart: People API

**Feature**: `013-people-api` | Validation guide; behaviour details are in [contracts/people-api.md](./contracts/people-api.md) and [data-model.md](./data-model.md).

## Prerequisites

- JDK 25 on the path; Node.js for `npx newman`.
- From the repository root, on branch `013-people-api`.

## 1. Quality gates

```bash
./gradlew check -PskipOwasp      # all four suites, Checkstyle, JaCoCo gate (CI runs the OWASP scan too)
./gradlew smokeTest -Dperf.strict=true   # SC-009 at the strict 100 ms p95 target
```

Expected: green, with `PeopleFunctionalTest`, `PeopleOpenApiContractTest`, `PeopleStartupIntegrationTest`, `PeopleFixtureRulesTest`, `PeopleReferencesTest` and `PeopleSmokeTest` among the tests run, and every earlier test and golden file unchanged.

## 2. Run the service

```bash
./start.sh        # http://localhost:8080, token local-dev-token
```

## 3. Manual checks

Set `T='Authorization: Bearer local-dev-token'` and `B=http://localhost:8080/api/v1/people`.

| # | Request | Expected |
|---|---|---|
| 1 | `curl -s -H "$T" "$B?updated_since=2000-01-01" \| jq '.pagination'` | `{"results":120,"pages":3,"current_page":1,"results_per_page":50,"more_pages":true}` |
| 2 | `...&page=3` → `jq '.results \| length, .pagination.more_pages'` | `20`, `false` |
| 3 | `...&page=4` | `"results": []`, `current_page` `4` |
| 4 | `...?updated_since=2099-01-01` | `results` `0`, `pages` `0` |
| 5 | `...?updated_since=2025-06-01` | only `last_updated >= 2025-06-01`, ascending |
| 6 | Take an `id` from #1; `curl -s -H "$T" "$B/<id>"` | the same object as in #1 |
| 7 | `"$B/<id of a person with previous appointments>?include_previous_appointments=true"` vs without | more `appointments` and `authorisations_with_dates`; identical `authorisations_current` and `judiciary_roles` |
| 8 | `"$B/00000000-0000-0000-0000-000000000000"` | `404` "Person not found." |
| 9 | `"$B/abc"` | `400` "id must be a UUID." |
| 10 | `"$B?updated_since=2025-06-01T00:00:00Z"` | `400` "updated_since must be a valid date." |
| 11 | `"$B?updated_since=2000-01-01&page=1&page=2"` | `400` "Query parameters must not be repeated." |
| 12 | `"$B?updated_since=2000-01-01&foo=1"` | `400` "Unsupported query parameter." |
| 13 | `curl -s -i "$B?updated_since=2000-01-01"` (no token) | `401`, `WWW-Authenticate: Bearer` |
| 14 | `curl -s -X POST -H "$T" "$B"` | `405` |
| 15 | `curl -s -H "$T" http://localhost:8080/api/v1/reference_data/genders` | unchanged from 005 |

For #7, a person with previous appointments is any with `appointments[].end_date` before `2026-01-01` in the `include_previous_appointments=true` response.

## 4. Swagger and Postman

- Swagger UI `http://localhost:8080/swagger-ui/index.html`: a "People" tag with `searchPeople` and `getPerson`, every parameter and status of the contract.
- With the service running:

  ```bash
  npx newman run postman/ctam-jomockapi.postman_collection.json \
    -e postman/ctam-jomockapi.postman_environment.json
  ```

  Expected: every request in "People" and in the existing folders passes.

## 5. Configuration checks

- `JO_PEOPLE_REFERENCE_DATE=2030-01-01 ./start.sh`: fewer current appointments in default responses; same people, order and pagination.
- `JO_PEOPLE_FIXTURE=file:/path/to/broken.json ./start.sh` with a duplicated `id`: the service refuses to start with `Invalid People fixture …: duplicate id (office holder <id>)`.
