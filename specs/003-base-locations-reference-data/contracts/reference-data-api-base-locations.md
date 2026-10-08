# Contract delta: Reference Data API (v1) — Base Locations

**Feature**: `003-base-locations-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md). Everything there — routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message — applies unchanged. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name` — allowed values (both routes)

| | Before (002) | After (003) |
|-|--------------|-------------|
| Canonical | `appointment_titles` | `appointment_titles`, `base_locations` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `base_location` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `appointment_title` | `appointment_titles`, `base_locations`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations. Also supports deprecated values: appointment_title` |

Any other value, including `base_location` (BaseLocation's deprecated alias, deliberately not served per spec Clarifications and Constitution VII), the remaining nine E-Links canonical names and their nine aliases, still returns `400` with `Unsupported reference data attribute_name.`

Nothing else in the OpenAPI document changes: no new path, operation, parameter, schema, header or response code.

---

## 2. `GET /api/v1/reference_data/base_locations`

**200**, `application/json`, `ReferenceDataApiResponse`: `results` holds 1,276 records in ascending `id` order. Each record has exactly the six `ReferenceDataResponse` fields.

Example (first and last elements; compact JSON in the real response):

```json
{
  "results": [
    {
      "id": 10,
      "name": "Aberconwy",
      "created_at": "2024-01-15T09:00:00Z",
      "updated_at": "2024-06-03T10:30:00Z",
      "start_date": "2024-01-01",
      "end_date": null
    },
    {
      "id": 12760,
      "name": "Yorkshire & Humberside",
      "created_at": "2024-01-15T09:00:00Z",
      "updated_at": "2024-06-03T10:30:00Z",
      "start_date": "2024-01-01",
      "end_date": null
    }
  ]
}
```

---

## 3. `GET /api/v1/reference_data/base_locations/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`:

```json
{
  "id": 70,
  "name": "Aldridge and Brownhills",
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2025-04-01T08:00:00Z",
  "start_date": "2024-01-01",
  "end_date": "2025-03-31"
}
```

Non-ASCII names are sent as UTF-8 characters, not escapes. `GET …/base_locations/8880` returns `"name": "Royal Courts of Justice – Office of the Judge Advocate General"` with U+2013 between "Justice" and "Office".

**404**: a well-formed id with no base location (e.g. `15`, `8885`, `12770`) returns `Reference data record not found.` Ids are scoped to the type: `appointment_titles/1940` exists, `base_locations/1940` is the base location at position 194, "Central/South Western Staffordshire Sub Committee".

**400**: malformed ids (e.g. `abc`, `1.5`, `-1`) return `reference_id must be a non-negative whole number.`

---

## 4. Examples of unchanged behaviour on the new type

| Request | Result |
|---------|--------|
| `GET /api/v1/reference_data/base_locations?name=Aberconwy` | `400`, `Query parameters are not supported on this endpoint.` |
| `GET /api/v1/reference_data/base_locations/` | `404`, `Resource not found.` |
| `POST /api/v1/reference_data/base_locations` | `405`, `Method not allowed.`, `Allow: GET` |
| Any base-location route without a valid token | `401`, `Unauthorized. Invalid or missing token.`, `WWW-Authenticate: Bearer` |
| `GET /api/v1/reference_data/Base_Locations` | `400`, `Unsupported reference data attribute_name.` |
| `GET /api/v1/reference_data/base_location` or `…/base_location/70` | `400`, `Unsupported reference data attribute_name.` (alias not served) |
| `GET /api/v1/reference_data/base_location` without a valid token | `401`, `Unauthorized. Invalid or missing token.` (authentication first) |
