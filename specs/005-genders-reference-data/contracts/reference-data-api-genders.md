# Contract delta: Reference Data API (v1) — Genders

**Feature**: `005-genders-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by [003's delta](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md) and [004's delta](../../004-contract-types-reference-data/contracts/reference-data-api-contract-types.md). Everything there — routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message — applies unchanged. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name` — allowed values (both routes)

| | Before (004) | After (005) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types` | `appointment_titles`, `base_locations`, `contract_types`, `genders` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `gender` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders. Also supports deprecated values: appointment_title` |

Any other value, including `gender` (deliberately not served per spec Clarifications and Constitution VII), `base_location`, `contract_type`, and the remaining seven E-Links canonical names and their seven aliases, still returns `400` with `Unsupported reference data attribute_name.`

No new path, operation, parameter, header, response code or schema property is added. `ReferenceDataResponse` is unchanged.

---

## 2. `GET /api/v1/reference_data/genders`

**200**, `application/json`, `ReferenceDataApiResponse`: `results` holds 4 records in ascending `id` order. Each record has exactly 002's six fields, in this order: `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`. Every `end_date` is null.

Example (compact JSON in the real response):

```json
{
  "results": [
    {"id": 10, "name": "Female", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 20, "name": "Male", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 30, "name": "Other", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 40, "name": "Prefer not to say", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null}
  ]
}
```

---

## 3. `GET /api/v1/reference_data/genders/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`:

```json
{
  "id": 20,
  "name": "Male",
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

**404** for a well-formed id with no record (`15`, `50`, `999999`); **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`); **400** for any query parameter (e.g. `?name=Male`); **401** without valid credentials — all with 002's messages and error shape.

---

## 4. `GET /api/v1/reference_data/gender[/{reference_id}]`

**400**, `Unsupported reference data attribute_name.`, for any `reference_id`, after authentication (`401` without valid credentials), as for every unsupported name.
