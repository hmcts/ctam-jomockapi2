# Contract delta: Reference Data API (v1) — Jurisdictions

**Feature**: `009-jurisdictions-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by the deltas of [003](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md), [004](../../004-contract-types-reference-data/contracts/reference-data-api-contract-types.md), [005](../../005-genders-reference-data/contracts/reference-data-api-genders.md), [006](../../006-judiciary-roles-reference-data/contracts/reference-data-api-judiciary-roles.md), [007](../../007-locations-reference-data/contracts/reference-data-api-locations.md) and [008](../../008-location-types-reference-data/contracts/reference-data-api-location-types.md). Everything there applies unchanged: routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name`: allowed values (both routes)

| | Before (008) | After (009) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `jurisdiction` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions. Also supports deprecated values: appointment_title` |

Any other value still returns `400` with `Unsupported reference data attribute_name.`. That includes:
- `jurisdiction`, which is not served, since deprecated names are out of scope (spec FR-001, Constitution VII);
- `base_location`, `contract_type`, `gender`, `judiciary_role`, `location` and `location_type`;
- the remaining three E-Links canonical names (`ticket_categories`, `ticket_category_types`, `tickets`) and their three singular forms.

---

## 2. `ReferenceDataResponse`

Unchanged. Jurisdiction records use only the shared fields and `name`.

---

## 3. `GET /api/v1/reference_data/jurisdictions`

**200**, `application/json`, `ReferenceDataApiResponse`. `results` holds 5 records in ascending `id` order: the E-Links ids 30, 34, 84, 86 and 87. Each record has exactly these fields, in this order: `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`. None is ended.

The whole response (the real response is compact JSON):

```json
{
  "results": [
    {"id": 30, "name": "Courts", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 34, "name": "Tribunals", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 84, "name": "Magistrates", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 86, "name": "Coroners", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 87, "name": "Skills", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null}
  ]
}
```

---

## 4. `GET /api/v1/reference_data/jurisdictions/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`, equal to the collection element with that id:

```json
{
  "id": 84,
  "name": "Magistrates",
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

The other responses, all with 002's messages and error shape:
- **404** for a well-formed id with no record (`1`, `31`, `88`, `999`, `999999`);
- **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`);
- **400** for any query parameter (e.g. `?name=Courts`);
- **401** without valid credentials.

---

## 5. `GET /api/v1/reference_data/jurisdiction[/{reference_id}]`

**400** with `Unsupported reference data attribute_name.` for any `reference_id`, checked after authentication (`401` without valid credentials), as for every unsupported name.
