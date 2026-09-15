# Contract: Reference Data Lookup by Type and ID

**Source of truth**: `joh-elinks-api/swagger-ui-elinks-api-v5.pdf` (E-Links API 5.0,
OAS 3.0), "Reference Data" group, page 3–4 — no machine-readable OpenAPI file is checked
into the repository (see `research.md` for the contract-test implication of this).

## Operation

| Field | Value |
|---|---|
| Server base path | `/elinks` |
| Operation path | `/api/v5/reference_data/{attribute_name}/{reference_id}` |
| Effective mock path | `/elinks/api/v5/reference_data/{attribute_name}/{reference_id}` |
| Method | `GET` |
| Summary (source spec) | Get reference data |
| Authentication | Required (lock indicator shown on this operation in the source spec) |

## Request

### Path parameters

| Name | Type (source spec) | Required | Description |
|---|---|---|---|
| `attribute_name` | string | Yes | One of the 11 canonical values, or its deprecated alias — see `data-model.md` for the full list. |
| `reference_id` | number | Yes | The `id` of the record to retrieve within the resolved type's dataset. |

- Any `attribute_name` not matching a canonical or deprecated value is unsupported (FR-007).
- Any `reference_id` that is not a well-formed numeric value is malformed (FR-008),
  distinct from an unmatched-but-numeric ID (FR-006).

## Response

### 200 OK

- **Description (source spec)**: "The found reference data"
- **Media type**: `application/json`
- **Body**: a single JSON object for the resolved type, containing the common fields
  (`id`, `name`, `start_date`, `end_date`, `created_at`, `updated_at`) plus that type's
  additional fields — see `data-model.md` for the full per-type field list. The source
  spec's example (`id`, `updated_at`, `created_at`, `start_date`, `end_date` only) is
  illustrative, not the full schema (Assumptions, `spec.md`).
- Returned regardless of whether the record's `start_date`/`end_date` window covers
  today (FR-012, Clarifications in `spec.md`).
- **Determinism**: identical for a given type/ID on every call against a running
  instance (FR-010).

### 400 Bad Request *(undocumented in source spec; see Assumptions in `spec.md`)*

- Returned when `reference_id` is not a well-formed numeric value (FR-008).
- **Body** *(Constitution v1.1.0, Principle IX amendment)*: the shared `ErrorResponse`
  shape — `{"message": string, "timestamp": ISO-8601 string, "traceId": string}`.

### 401 Unauthorized

- **Description (source spec)**: "Unauthorized. Invalid or missing token."
- Returned when the request does not present a valid simulated credential (FR-009). No
  record data is included in the body.
- **Body** *(Constitution v1.1.0, Principle IX amendment)*: the shared `ErrorResponse`
  shape, written directly by `SimulatedAuthInterceptor` (`research.md`).

### 404 Not Found *(undocumented in source spec; see Assumptions in `spec.md`)*

- Returned when `attribute_name` does not match any canonical or deprecated supported
  value (FR-007).
- Returned when `attribute_name` resolves to a supported type but `reference_id` does
  not match any record's `id` within that type's dataset (FR-006).
- **Body** *(Constitution v1.1.0, Principle IX amendment)*: the shared `ErrorResponse`
  shape.

### All responses *(Constitution v1.1.0, Principle XIV)*

- Every response — `200`, `400`, `401`, `404` alike — carries an `X-Correlation-Id`
  response header, via the shared `CorrelationIdFilter` established by
  `001-healthcheck-endpoint`. On an error response, the body's `traceId` matches this
  header's value.

### Unsupported HTTP methods

- Any method other than `GET` against this path MUST be rejected in a way that clearly
  indicates the method is not supported (Edge Cases, `spec.md`), consistent with the
  application's centralised error handling (Constitution Principle IX).

## Out of scope for this contract

- The list-style operation `GET /api/v5/reference_data/{attribute_name}` (returning all
  records for a type) is a separate operation in the source spec and is not covered by
  this feature.
- Following/resolving cross-type ID references embedded in a record (e.g. a location's
  `jurisdiction_id`) is out of scope — those are returned as opaque IDs only.
