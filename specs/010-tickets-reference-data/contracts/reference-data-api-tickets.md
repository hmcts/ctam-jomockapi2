# Contract delta: Reference Data API (v1) — Tickets

**Feature**: `010-tickets-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by the deltas of [003](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md), [004](../../004-contract-types-reference-data/contracts/reference-data-api-contract-types.md), [005](../../005-genders-reference-data/contracts/reference-data-api-genders.md), [006](../../006-judiciary-roles-reference-data/contracts/reference-data-api-judiciary-roles.md), [007](../../007-locations-reference-data/contracts/reference-data-api-locations.md), [008](../../008-location-types-reference-data/contracts/reference-data-api-location-types.md) and [009](../../009-jurisdictions-reference-data/contracts/reference-data-api-jurisdictions.md). Everything there applies unchanged: routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name`: allowed values (both routes)

| | Before (009) | After (010) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `ticket` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets. Also supports deprecated values: appointment_title` |

Any other value still returns `400` with `Unsupported reference data attribute_name.`. That includes:
- `ticket`, which is not served, since deprecated names are out of scope (spec FR-001, Constitution VII);
- `base_location`, `contract_type`, `gender`, `judiciary_role`, `jurisdiction`, `location` and `location_type`;
- the remaining two E-Links canonical names (`ticket_categories`, `ticket_category_types`) and their two singular forms.

---

## 2. `ReferenceDataResponse`: new optional `ticket_category_id`

| Property | Before (009) | After (010) |
|----------|--------------|-------------|
| Properties, in order | `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `integration_code`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date` | `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, **`ticket_category_id`**, `integration_code`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date` |
| `ticket_category_id` | — | `integer`, `format: int64`, not required, not nullable; description `Id of the record's ticket category, in E-Links' numbering. Present only for types whose records carry it (deviation D-5 from the E-Links schema).`; example `31` |

`required` is unchanged (`id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`). The field appears in a response only for a type that declares it, today only `tickets`, so every other type's responses are byte-for-byte unchanged.

---

## 3. `GET /api/v1/reference_data/tickets`

**200**, `application/json`, `ReferenceDataApiResponse`. `results` holds 159 records in ascending `id` order, from 289 to 1709. Each record has exactly these fields, in this order: `id`, `name`, `ticket_category_id`, `created_at`, `updated_at`, `start_date`, `end_date`. The 22 records at positions 7, 14, …, 154 are ended (spec AC-003).

The first seven records (the real response is compact JSON):

```json
{
  "results": [
    {"id": 289, "name": "Welsh Language", "ticket_category_id": 88, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 290, "name": "Administrative Court", "ticket_category_id": 31, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 291, "name": "Admiralty - KBD", "ticket_category_id": 31, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 292, "name": "Chancery", "ticket_category_id": 31, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 293, "name": "Chancery Business in the County Court", "ticket_category_id": 31, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 294, "name": "Civil Authorisation", "ticket_category_id": 31, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 295, "name": "Commercial", "ticket_category_id": 31, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2025-04-01T08:00:00Z", "start_date": "2024-01-01", "end_date": "2025-03-31"},
    …
  ]
}
```

---

## 4. `GET /api/v1/reference_data/tickets/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`, equal to the collection element with that id. An open record:

```json
{
  "id": 294,
  "name": "Civil Authorisation",
  "ticket_category_id": 31,
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

An ended record:

```json
{
  "id": 367,
  "name": "05 - Industrial Injuries",
  "ticket_category_id": 36,
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2025-04-01T08:00:00Z",
  "start_date": "2024-01-01",
  "end_date": "2025-03-31"
}
```

The other responses, all with 002's messages and error shape:
- **404** for a well-formed id with no record (`1`, `288`, `297`, `1710`, `999999`);
- **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`);
- **400** for any query parameter (e.g. `?ticket_category_id=31`);
- **401** without valid credentials.

---

## 5. `GET /api/v1/reference_data/ticket[/{reference_id}]`

**400** with `Unsupported reference data attribute_name.` for any `reference_id`, checked after authentication (`401` without valid credentials), as for every unsupported name.
