# Contract delta: Reference Data API (v1) — Ticket Category Types

**Feature**: `012-ticket-category-types-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by the deltas of 003 to 011 (the latest is [011](../../011-ticket-categories-reference-data/contracts/reference-data-api-ticket-categories.md)). Everything there applies unchanged: routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name`: allowed values (both routes)

| | Before (011) | After (012) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `ticket_categories` | the same, then `ticket_category_types`: all 11 E-Links canonical names |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `ticket_category_type` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `ticket_categories`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `location_types`, `jurisdictions`, `tickets`, `ticket_categories`, `ticket_category_types`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, ticket_categories. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations, location_types, jurisdictions, tickets, ticket_categories, ticket_category_types. Also supports deprecated values: appointment_title` |

Any other value still returns `400` with `Unsupported reference data attribute_name.`. No canonical E-Links name is left unsupported; the unsupported ones are:
- `ticket_category_type`, which is not served, since deprecated names are out of scope (spec FR-001, Constitution VII);
- `base_location`, `contract_type`, `gender`, `judiciary_role`, `jurisdiction`, `location`, `location_type`, `ticket` and `ticket_category`;
- any name outside E-Links, and case or spelling variants such as `TICKET_CATEGORY_TYPES` or `ticket-category-types`.

---

## 2. `ReferenceDataResponse`: unchanged

No property is added, and `required` is unchanged. Ticket-category-type records use the existing optional `jurisdiction_id` (D-2) and `integration_code` (D-4); their descriptions already apply to any type that declares them.

---

## 3. `GET /api/v1/reference_data/ticket_category_types`

**200**, `application/json`, `ReferenceDataApiResponse`. `results` holds 5 records in ascending `id` order, from 1 to 5. Each record has exactly these fields, in this order: `id`, `name`, `jurisdiction_id`, `integration_code`, `created_at`, `updated_at`, `start_date`, `end_date`. None is ended.

The whole collection (the real response is compact JSON):

```json
{
  "results": [
    {"id": 1, "name": "Court Type", "jurisdiction_id": 30, "integration_code": "COURTS_COURT_TYPE", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 2, "name": "Tribunal Type", "jurisdiction_id": 34, "integration_code": "TRIBS_TYPE", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 3, "name": "Tribunal Name", "jurisdiction_id": 34, "integration_code": "TRIBS_NAME", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 4, "name": "Court Type", "jurisdiction_id": 84, "integration_code": "MAGS_COURT_TYPE", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 5, "name": "Skills Category", "jurisdiction_id": 87, "integration_code": "SKILLS_CATEGORY", "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null}
  ]
}
```

---

## 4. `GET /api/v1/reference_data/ticket_category_types/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`, equal to the collection element with that id:

```json
{
  "id": 4,
  "name": "Court Type",
  "jurisdiction_id": 84,
  "integration_code": "MAGS_COURT_TYPE",
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

The other responses, all with 002's messages and error shape:
- **404** for a well-formed id with no record (`6`, `30`, `31`, `999999`);
- **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`);
- **400** for any query parameter (e.g. `?jurisdiction_id=30`);
- **401** without valid credentials.

---

## 5. `GET /api/v1/reference_data/ticket_category_type[/{reference_id}]`

**400** with `Unsupported reference data attribute_name.` for any `reference_id`, checked after authentication (`401` without valid credentials), as for every unsupported name.
