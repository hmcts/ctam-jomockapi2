# Contract delta: Reference Data API (v1) — Location Types

**Feature**: `008-location-types-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by the deltas of [003](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md), [004](../../004-contract-types-reference-data/contracts/reference-data-api-contract-types.md), [005](../../005-genders-reference-data/contracts/reference-data-api-genders.md), [006](../../006-judiciary-roles-reference-data/contracts/reference-data-api-judiciary-roles.md) and [007](../../007-locations-reference-data/contracts/reference-data-api-locations.md). Everything there applies unchanged: routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name`: allowed values (both routes)

| | Before (007) | After (008) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `location_type` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types. Also supports deprecated values: appointment_title` |

Any other value still returns `400` with `Unsupported reference data attribute_name.`. That includes:
- `location_type`, which is not served, since deprecated names are out of scope (spec FR-001, Constitution VII);
- `base_location`, `contract_type`, `gender`, `judiciary_role` and `location`;
- the remaining four E-Links canonical names (`jurisdictions`, `ticket_categories`, `ticket_category_types`, `tickets`) and their four singular forms.

---

## 2. `ReferenceDataResponse`: new optional `integration_code`

| Property | Before (007) | After (008) |
|----------|--------------|-------------|
| `integration_code` | — | **new**: optional `string`, not nullable; present on every record of a type that declares it (today only `location_types`), absent otherwise; placed after `jurisdiction_id` (deviation D-4) |
| everything else | — | unchanged; no new schema component |

Responses of the six existing types are byte-for-byte unchanged: none declares a text field.

---

## 3. `GET /api/v1/reference_data/location_types`

**200**, `application/json`, `ReferenceDataApiResponse`. `results` holds 16 records in ascending `id` order: the E-Links ids, 1 to 16. Each record has exactly these fields, in this order: `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date`, `end_date`. There is no `type_id` or `parent_id`.

- **End dates**: ids 7 and 14 have `end_date` `2025-03-31` and `updated_at` `2025-04-01T08:00:00Z`. The other 14 have null and `2024-06-03T10:30:00Z`.
- **Names**: can repeat ("Region" at 1, 6 and 14; "AC" at 7 and 11). The jurisdiction and integration code tell them apart.

Example (first records, an ended record and the last record; the real response is compact JSON):

```json
{
  "results": [
    {"id": 1, "name": "Region", "jurisdiction_id": 30, "integration_code": "COURTS_REGION", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 2, "name": "Court", "jurisdiction_id": 30, "integration_code": "COURTS_COURT_NAME", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    …
    {"id": 7, "name": "AC", "jurisdiction_id": 84, "integration_code": "MAGS_AC", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2025-04-01T08:00:00Z", "start_date": "2024-01-01", "end_date": "2025-03-31"},
    …
    {"id": 16, "name": "Geographic Area", "jurisdiction_id": 86, "integration_code": "CORONERS_AREA", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null}
  ]
}
```

---

## 4. `GET /api/v1/reference_data/location_types/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`, equal to the collection element with that id:

```json
{
  "id": 14,
  "name": "Region",
  "jurisdiction_id": 86,
  "integration_code": "CORONERS_REGION",
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2025-04-01T08:00:00Z",
  "start_date": "2024-01-01",
  "end_date": "2025-03-31"
}
```

The other responses, all with 002's messages and error shape:
- **404** for a well-formed id with no record (`17`, `999`, `999999`);
- **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`);
- **400** for any query parameter (e.g. `?jurisdiction_id=84`);
- **401** without valid credentials.

---

## 5. `GET /api/v1/reference_data/location_type[/{reference_id}]`

**400** with `Unsupported reference data attribute_name.` for any `reference_id`, checked after authentication (`401` without valid credentials), as for every unsupported name.
