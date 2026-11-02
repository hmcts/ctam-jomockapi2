# Contract: People API (search and single office holder)

**Feature**: `013-people-api` | The authoritative field list is the spec's Records section; this document is the HTTP contract the tests and the OpenAPI document must match.

Both routes: `GET` only, `Authorization: Bearer <token>` required, optional `X-Correlation-Id` request header, `X-Correlation-Id` on every response, `Content-Type: application/json` on every body, errors in the shared `ErrorResponse` shape (`error`, `timestamp`, `traceId`).

## `GET /api/v1/people`

| Query parameter | Required | Schema | Default | Rule |
|---|---|---|---|---|
| `updated_since` | yes | string, `format: date` | — | `YYYY-MM-DD`, a real date; matches `last_updated >= updated_since` |
| `page` | no | integer, `format: int64`, `minimum: 1` | `1` | decimal digits, leading zeros allowed, ≤ 2^63−1 |
| `per_page` | no | integer, `format: int64`, `minimum: 1` | `50` | as `page` |
| `include_previous_appointments` | no | boolean | `false` | exactly `true` or `false` |

No other parameter; none repeated.

**200** — `PeopleApiResponse`:

```json
{
  "results": [ { "...": "CurrentOfficeHolderResponse" } ],
  "pagination": {
    "results": 120,
    "pages": 3,
    "current_page": 1,
    "results_per_page": 50,
    "more_pages": true
  }
}
```

`results` ordered by `last_updated`, then `id`, ascending. Past the last page: `"results": []`, the other pagination fields as for any page.

## `GET /api/v1/people/{id}`

| Parameter | In | Required | Schema |
|---|---|---|---|
| `id` | path | yes | string, `format: uuid`, pattern `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$` |
| `include_previous_appointments` | query | no | boolean, default `false` |

**200** — one `CurrentOfficeHolderResponse`, identical to the same office holder's object in a search with the same flag value.

## `CurrentOfficeHolderResponse` (example, abbreviated)

```json
{
  "id": "0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7f",
  "last_updated": "2025-06-14",
  "per_id": 900017,
  "personal_code": "9100113",
  "title": "District Judge",
  "known_as": "Alex",
  "surname": "Fenwick",
  "fullname": "District Judge Alex Fenwick",
  "initials": "AF",
  "post_nominals": null,
  "email": "alex.fenwick@judiciary.example",
  "sex": "Female",
  "gender": "Female",
  "gender_id": 10,
  "work_phone": "020 7946 0017",
  "disability": false,
  "retirement_date": null,
  "leaving_on": null,
  "appointments": [
    {
      "appointment_id": 700031,
      "role_name": "<appointment title name>",
      "role_name_id": 0,
      "type": "<jurisdiction name>",
      "court_name": "<base location name>",
      "court_type": "<location type name>",
      "circuit": "North Eastern",
      "location": "<parent location name>",
      "base_location": "<base location name>",
      "base_location_id": 0,
      "is_principal": true,
      "start_date": "2019-04-01",
      "end_date": null,
      "contract_type": "Salaried",
      "contract_type_id": 120
    }
  ],
  "judiciary_roles": [
    { "name": "<judiciary role name>", "judiciary_role_id": 600012, "judiciary_role_name_id": 0,
      "start_date": "2020-01-06", "end_date": null }
  ],
  "authorisations": [ { "jurisdiction": "Courts", "tickets": ["Civil Authorisation"] } ],
  "authorisations_current": [ { "jurisdiction": "Courts", "tickets": ["Civil Authorisation"] } ],
  "authorisations_with_dates": [
    { "authorisation_id": 800044, "appointment_id": 700031, "jurisdiction": "Courts", "jurisdiction_id": 30,
      "ticket": "Civil Authorisation", "ticket_id": 294, "start_date": "2019-04-01", "end_date": null }
  ]
}
```

Placeholders in angle brackets (and `0` ids) stand for values from the served reference data (data-model.md § 3); the golden files hold the real ones. Field order is the Swagger's (`@JsonPropertyOrder`). `gender`, `email_personal`, `circuit`, `bench` and `advisory_committee_area` are left out when unset.

## Errors

| Status | When | `error` |
|---|---|---|
| 400 | `updated_since` missing or empty | `updated_since is required.` |
| 400 | `updated_since` malformed (incl. a time part, a non-existent date) | `updated_since must be a valid date.` |
| 400 | bad `page` | `page must be a positive whole number.` |
| 400 | bad `per_page` | `per_page must be a positive whole number.` |
| 400 | bad flag | `include_previous_appointments must be true or false.` |
| 400 | undefined query parameter | `Unsupported query parameter.` |
| 400 | repeated query parameter | `Query parameters must not be repeated.` |
| 400 | malformed `id` | `id must be a UUID.` |
| 401 | missing or invalid token (checked first) | `Unauthorized. Invalid or missing token.` + `WWW-Authenticate: Bearer` |
| 404 | well-formed unknown `id` | `Person not found.` |
| 404 | trailing slash, extra segment | `Resource not found.` |
| 405 | method other than `GET` | `Method not allowed.` + `Allow: GET` |
| 406 | `Accept` excludes `application/json` | `Not acceptable.` |
| 500 | unexpected failure | `Internal server error.` |

Precedence (EC-010): 401 → 404/405 (route, method) → repeated → undefined parameter → `updated_since` → `page` → `per_page` → flag → `id` format → 404 lookup.

## OpenAPI document

Tag "People"; operation ids `searchPeople` and `getPerson`; both carry `bearerAuth`, the `X-Correlation-Id` request header, the response header on every status, and the shared `401`, `406` and `500` responses (added by `OpenApiConfig`); `400` and (single route) `404` on the operations with examples. Schemas `PeopleApiResponse`, `PaginationResponse`, `CurrentOfficeHolderResponse`, `AppointmentsResponse`, `JudiciaryRoleResponse`, `AuthorisationSummaryResponse`, `AuthorisationsResponse` with the spec's required and nullable flags. The reference-data operations and schemas are unchanged.
