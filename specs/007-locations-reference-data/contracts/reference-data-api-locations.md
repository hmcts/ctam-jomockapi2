# Contract delta: Reference Data API (v1) — Locations

**Feature**: `007-locations-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by the deltas of [003](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md), [004](../../004-contract-types-reference-data/contracts/reference-data-api-contract-types.md), [005](../../005-genders-reference-data/contracts/reference-data-api-genders.md) and [006](../../006-judiciary-roles-reference-data/contracts/reference-data-api-judiciary-roles.md). Everything there applies unchanged: routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name`: allowed values (both routes)

| | Before (006) | After (007) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `location` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `genders`, `judiciary_roles`, `locations`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types, genders, judiciary_roles, locations. Also supports deprecated values: appointment_title` |

Any other value still returns `400` with `Unsupported reference data attribute_name.`. That includes:
- `location`, which is not served, since deprecated names are out of scope (spec FR-001, Constitution VII);
- `base_location`, `contract_type`, `gender` and `judiciary_role`;
- the remaining five E-Links canonical names and their five singular forms.

---

## 2. `ReferenceDataResponse`: `parent_id` may be null

| Property | Before (006) | After (007) |
|----------|--------------|-------------|
| `parent_id` | optional `integer` (`int64`), not nullable; present only for types that declare it | optional `integer` (`int64`), **nullable**; present on every record of a type that declares it; `null` for a top-level record of a type that declares it nullable (today only `locations`) |
| `type_id`, `jurisdiction_id` | optional `int64`, not nullable | unchanged |
| everything else | — | unchanged; no new property, and no new schema component |

Responses of the five existing types are byte-for-byte unchanged. None declares a nullable field, and BaseLocation's `parent_id` is never null.

---

## 3. `GET /api/v1/reference_data/locations`

**200**, `application/json`, `ReferenceDataApiResponse`. `results` holds 2,000 records in ascending `id` order: the E-Links ids, 1 to 2433, with gaps. Each record has exactly these fields, in this order: `id`, `name`, `type_id`, `parent_id`, `jurisdiction_id`, `created_at`, `updated_at`, `start_date`, `end_date`.

- **Parents**: 142 records have `"parent_id": null`. Every other `parent_id` is the `id` of a record in this collection.
- **End dates**: 285 records (the 7th, 14th, … in id order; ids 9 to 2364) have `end_date` `2025-03-31`. The other 1,715 have null.
- **Names**: UTF-8 JSON strings with exactly the source's characters. Names can repeat, can contain two consecutive spaces ("Cheshire  LJA"), and can contain `–` (U+2013).

Example (first records, one ended record and a top-level record; the real response is compact JSON):

```json
{
  "results": [
    {"id": 1, "name": "Aberconwy", "type_id": 8, "parent_id": 1722, "jurisdiction_id": 84, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    {"id": 3, "name": "Alnwick", "type_id": 8, "parent_id": 1748, "jurisdiction_id": 84, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    …
    {"id": 9, "name": "Ashby-de-la-Zouch and Market Bosworth", "type_id": 8, "parent_id": 1732, "jurisdiction_id": 84, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2025-04-01T08:00:00Z", "start_date": "2024-01-01", "end_date": "2025-03-31"},
    …
    {"id": 2141, "name": "Gwynedd", "type_id": 6, "parent_id": null, "jurisdiction_id": 84, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null},
    …
    {"id": 2433, "name": "Kent and Medway", "type_id": 16, "parent_id": 1925, "jurisdiction_id": 86, "created_at": "2024-01-15T09:00:00Z", "updated_at": "2024-06-03T10:30:00Z", "start_date": "2024-01-01", "end_date": null}
  ]
}
```

---

## 4. `GET /api/v1/reference_data/locations/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`, equal to the collection element with that id:

```json
{
  "id": 2141,
  "name": "Gwynedd",
  "type_id": 6,
  "parent_id": null,
  "jurisdiction_id": 84,
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

The other responses, all with 002's messages and error shape:
- **404** for a well-formed id with no record (`2`, `2434`, `999999`);
- **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`);
- **400** for any query parameter (e.g. `?parent_id=1722`);
- **401** without valid credentials.

---

## 5. `GET /api/v1/reference_data/location[/{reference_id}]`

**400** with `Unsupported reference data attribute_name.` for any `reference_id`, checked after authentication (`401` without valid credentials), as for every unsupported name.
