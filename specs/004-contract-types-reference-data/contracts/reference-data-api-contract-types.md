# Contract delta: Reference Data API (v1) — Contract Types

**Feature**: `004-contract-types-reference-data` | **Spec**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

**Base contract**: [002 contracts/reference-data-api.md](../../002-reference-data-api/contracts/reference-data-api.md), as changed by [003's delta](../../003-base-locations-reference-data/contracts/reference-data-api-base-locations.md). Everything there — routes, authentication, correlation, the order of checks, response shapes, headers, status codes and every error message — applies unchanged. This document lists only what changes. The springdoc-generated `/v3/api-docs` MUST describe exactly the combined contract (Principle XVIII); `ReferenceDataOpenApiContractTest` asserts it.

---

## 1. `attribute_name` — allowed values (both routes)

| | Before (003) | After (004) |
|-|--------------|-------------|
| Canonical | `appointment_titles`, `base_locations` | `appointment_titles`, `base_locations`, `contract_types` |
| Deprecated alias | `appointment_title` | `appointment_title` (unchanged; `contract_type` is not served) |
| OpenAPI `enum` (in order) | `appointment_titles`, `base_locations`, `appointment_title` | `appointment_titles`, `base_locations`, `contract_types`, `appointment_title` |
| OpenAPI `description` | `Can be one of: appointment_titles, base_locations. Also supports deprecated values: appointment_title` | `Can be one of: appointment_titles, base_locations, contract_types. Also supports deprecated values: appointment_title` |

Any other value, including `contract_type` (deliberately not served per spec Clarifications and Constitution VII), `base_location`, and the remaining eight E-Links canonical names and their eight aliases, still returns `400` with `Unsupported reference data attribute_name.`

No new path, operation, parameter, header or response code is added.

---

## 1a. `ReferenceDataResponse` schema — boolean field

The schema gains one optional property, after `jurisdiction_id` (spec FR-005, FR-017; deviation D-3):

| Property | Type | Required | Nullable | Description |
|----------|------|----------|----------|-------------|
| `salaried` | boolean | no | no | Whether the contract type is salaried, including salaried part-time working. Present only for types whose records carry it (deviation D-3 from the E-Links schema). |

`required` stays `id`, `name`, `created_at`, `updated_at`, `start_date`, `end_date`. The property is written only when the record carries it: every ContractType record carries it, and AppointmentTitle and BaseLocation records don't, so their responses are unchanged.

---

## 2. `GET /api/v1/reference_data/contract_types`

**200**, `application/json`, `ReferenceDataApiResponse`: `results` holds 14 records in ascending `id` order. Each record has exactly seven fields, in this order: `id`, `name`, `salaried`, `created_at`, `updated_at`, `start_date`, `end_date`.

Example (first and last elements; compact JSON in the real response):

```json
{
  "results": [
    {
      "id": 10,
      "name": "Fee-paid",
      "salaried": false,
      "created_at": "2024-01-15T09:00:00Z",
      "updated_at": "2024-06-03T10:30:00Z",
      "start_date": "2024-01-01",
      "end_date": null
    },
    {
      "id": 140,
      "name": "Voluntary",
      "salaried": false,
      "created_at": "2024-01-15T09:00:00Z",
      "updated_at": "2025-04-01T08:00:00Z",
      "start_date": "2024-01-01",
      "end_date": "2025-03-31"
    }
  ]
}
```

---

## 3. `GET /api/v1/reference_data/contract_types/{reference_id}`

**200**, `application/json`, a single `ReferenceDataResponse`:

```json
{
  "id": 120,
  "name": "Salaried",
  "salaried": true,
  "created_at": "2024-01-15T09:00:00Z",
  "updated_at": "2024-06-03T10:30:00Z",
  "start_date": "2024-01-01",
  "end_date": null
}
```

**404** for a well-formed id with no record (`15`, `150`, `999999`); **400** for a malformed id (`abc`, `12x`, `1.5`, `-1`); **400** for any query parameter (e.g. `?salaried=true`); **401** without valid credentials — all with 002's messages and error shape.

---

## 4. `GET /api/v1/reference_data/contract_type[/{reference_id}]`

**400**, `Unsupported reference data attribute_name.`, for any `reference_id`, after authentication (`401` without valid credentials), as for every unsupported name.
