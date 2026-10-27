# Contract delta: Reference Data API (v1) — Judiciary Roles

**Feature**: `006-judiciary-roles-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by [003's delta](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md), [004's delta](../../004-contract-types-reference-data/contracts/reference-data-api-contract-types.md) and [005's delta](../../005-genders-reference-data/contracts/reference-data-api-genders.md). Everything there — routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message — applies unchanged. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name` — allowed values (both routes)

| | Before (005) | After (006) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `judiciary_role` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles. Also supports deprecated values: appointment_title` |

Any other value, including `judiciary_role` (not served: deprecated names are out of scope, spec FR-001, Constitution VII), `base_location`, `contract_type`, `gender`, and the remaining six E-Links canonical names and their six singular forms, still returns `400` with `Unsupported reference data attribute_name.`

No new path, operation, parameter, header, response code or schema property is added. `ReferenceDataResponse` is unchanged.

---

## 2. `GET /api/v1/reference_data/judiciary_roles`

**200**, `application/json`, `ReferenceDataApiResponse`: `results` holds 164 records in ascending `id` order (10 to 1640). Each record has exactly 002's six fields, in this order: `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`. 23 records (ids 70, 140, …, 1610) have `end_date` `2025-03-31`; the other 141 have null. Names are UTF-8 JSON strings containing exactly the source's characters, including `’` (U+2019) and `–` (U+2013).

Example (first records and one ended record; compact JSON in the real response):

```json
{
  "results": [
    {"id": 10, "name": "Acting Designated Civil Judge", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 20, "name": "Acting Designated Family Judge", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    …
    {"id": 70, "name": "Acting Senior Costs Judge", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2025-04-01T08:00:00Z", "start_date": "2024-01-01", "end_date": "2025-03-31"},
    …
    {"id": 1640, "name": "Wales Training Course Director (JC)", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null}
  ]
}
```

---

## 3. `GET /api/v1/reference_data/judiciary_roles/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`:

```json
{
  "id": 960,
  "name": "Judge in Charge of Jury List (King’s Bench)",
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

**404** for a well-formed id with no record (`15`, `1650`, `999999`); **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`); **400** for any query parameter (e.g. `?name=Mentor%20Judge`); **401** without valid credentials — all with 002's messages and error shape.

---

## 4. `GET /api/v1/reference_data/judiciary_role[/{reference_id}]`

**400**, `Unsupported reference data attribute_name.`, for any `reference_id`, after authentication (`401` without valid credentials), as for every unsupported name.
