# Contract delta: Reference Data API (v1) — Ticket Categories

**Feature**: `011-ticket-categories-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by the deltas of 003 to 010 (the latest is [010](../../010-tickets-reference-data/contracts/reference-data-api-tickets.md)). Everything there applies unchanged: routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name`: allowed values (both routes)

| | Before (010) | After (011) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets` | the same, then `ticket_categories` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `ticket_category` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `ticket_categories`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, ticket_categories. Also supports deprecated values: appointment_title` |

Any other value still returns `400` with `Unsupported reference data attribute_name.`. That includes:
- `ticket_category`, which is not served, since deprecated names are out of scope (spec FR-001, Constitution VII);
- `base_location`, `contract_type`, `gender`, `judiciary_role`, `jurisdiction`, `location`, `location_type` and `ticket`;
- the last E-Links canonical name, `ticket_category_types`, and its singular `ticket_category_type`.

---

## 2. `ReferenceDataResponse`: new optional, nullable `parent_category_id`

| Property | Before (010) | After (011) |
|----------|--------------|-------------|
| Properties, in order | `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `ticket_category_id`, `integration_code`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date` | `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `ticket_category_id`, **`parent_category_id`**, `integration_code`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date` |
| `parent_category_id` | — | `integer`, `format: int64`, not required, **nullable**; description `Id of the record's parent category, in E-Links' numbering. Present only for types whose records carry it (deviation D-6 from the E-Links schema). Null for a top-level record of a type that declares it nullable.`; example `48` |

`required` is unchanged. The field appears only for a type that declares it, today only `ticket_categories`, so every other type's responses are byte-for-byte unchanged. `type_id` and `jurisdiction_id` are unchanged; their descriptions cite D-2 and apply to ticket categories too.

---

## 3. `GET /api/v1/reference_data/ticket_categories`

**200**, `application/json`, `ReferenceDataApiResponse`. `results` holds 54 records in ascending `id` order, from 31 to 88. Each record has exactly these fields, in this order: `id`, `name`, `type_id`, `jurisdiction_id`, `parent_category_id`, `created_at`, `updated_at`, `start_date`, `end_date`. `parent_category_id` is `null` (present) for the 8 top-level categories. The 7 records at positions 7, 14, …, 49 are ended.

The first seven records (the real response is compact JSON):

```json
{
  "results": [
    {"id": 31, "name": "Civil", "type_id": 1, "jurisdiction_id": 30, "parent_category_id": null, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 32, "name": "Crime", "type_id": 1, "jurisdiction_id": 30, "parent_category_id": null, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 33, "name": "Family", "type_id": 1, "jurisdiction_id": 30, "parent_category_id": null, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 35, "name": "First tier Tribunal", "type_id": 2, "jurisdiction_id": 34, "parent_category_id": null, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 36, "name": "Social Entitlement Chamber", "type_id": 3, "jurisdiction_id": 34, "parent_category_id": 35, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 37, "name": "Health, Education and Social Care Chamber", "type_id": 3, "jurisdiction_id": 34, "parent_category_id": 35, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 38, "name": "Immigration and Asylum Chamber", "type_id": 3, "jurisdiction_id": 34, "parent_category_id": 35, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2025-04-01T08:00:00Z", "start_date": "2024-01-01", "end_date": "2025-03-31"},
    …
  ]
}
```

---

## 4. `GET /api/v1/reference_data/ticket_categories/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`, equal to the collection element with that id. A top-level record:

```json
{
  "id": 31,
  "name": "Civil",
  "type_id": 1,
  "jurisdiction_id": 30,
  "parent_category_id": null,
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

A child record:

```json
{
  "id": 56,
  "name": "Employment Tribunal (England & Wales)",
  "type_id": 3,
  "jurisdiction_id": 34,
  "parent_category_id": 48,
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

The other responses, all with 002's messages and error shape:
- **404** for a well-formed id with no record (`1`, `30`, `34`, `84`, `89`, `999999`);
- **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`);
- **400** for any query parameter (e.g. `?parent_category_id=48`);
- **401** without valid credentials.

---

## 5. `GET /api/v1/reference_data/ticket_category[/{reference_id}]`

**400** with `Unsupported reference data attribute_name.` for any `reference_id`, checked after authentication (`401` without valid credentials), as for every unsupported name.
