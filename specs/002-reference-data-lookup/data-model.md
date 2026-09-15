# Phase 1 Data Model: Reference Data Lookup by Type and ID

**Feature**: `002-reference-data-lookup`

## Overview

Two conceptual entities from `spec.md` → Key Entities map to concrete shapes: the
**Reference Data Type** (a registry entry, not itself returned by the API) and the
**Reference Data Record** (what the API actually returns — one concrete shape per type,
sharing a common set of fields).

## Reference Data Type (registry, not an API response shape)

One entry per supported type. Drives `attribute_name` resolution (FR-002, FR-003,
FR-007) and which backing dataset + record shape a lookup uses.

| Field | Description |
|---|---|
| Canonical name | Current, plural `attribute_name` value (e.g. `jurisdictions`) |
| Deprecated alias | Legacy, singular `attribute_name` value (e.g. `jurisdiction`) |
| Backing resource | Bundled classpath resource holding this type's records (see `research.md`) |
| Record shape | The Reference Data Record shape this type returns (below) |

**11 entries** (canonical name → deprecated alias):

| Canonical (current) | Deprecated alias |
|---|---|
| `appointment_titles` | `appointment_title` |
| `base_locations` | `base_location` |
| `contract_types` | `contract_type` |
| `genders` | `gender` |
| `judiciary_roles` | `judiciary_role` |
| `jurisdictions` | `jurisdiction` |
| `location_types` | `location_type` |
| `locations` | `location` |
| `ticket_categories` | `ticket_category` |
| `ticket_category_types` | `ticket_category_type` |
| `tickets` | `ticket` |

**Validation rule**: an `attribute_name` not matching any canonical or deprecated value
above resolves to "no type" → FR-007 not-found response.

## Reference Data Record (common shape)

Every type's record carries these fields (source: all 11 extracts share this base
schema):

| Field | Type | Notes |
|---|---|---|
| `id` | integer | Unique within its type (FR-005). The `reference_id` path parameter matches this field. |
| `name` | string | Human-readable label. |
| `start_date` | date (ISO-8601) | Normalized at load time (see `research.md`). |
| `end_date` | date (ISO-8601), nullable | Absent/null when the record has no end date (Edge Cases, `spec.md`). Informational only — never used to filter lookup results (FR-012). |
| `created_at` | timestamp (ISO-8601) | From source extract. |
| `updated_at` | timestamp (ISO-8601) | From source extract. |

**Validation rule**: `id` must be a valid numeric value in the source data; a source row
that lacks one is excluded at load time (see `research.md`) and can never be returned.

## Per-type additional fields

Beyond the common shape above, each type carries the following extra fields (source:
CSV/JSON headers in `joh-elinks-api/ReferenceData/`):

| Type (canonical name) | Additional fields |
|---|---|
| `appointment_titles` | *(none)* |
| `base_locations` | `type_id` (integer), `parent_id` (integer, nullable), `jurisdiction_id` (integer) |
| `contract_types` | `salaried` (boolean, nullable — absent in source means not salaried) |
| `genders` | *(none)* |
| `judiciary_roles` | *(none)* |
| `jurisdictions` | *(none)* |
| `location_types` | `jurisdiction_id` (integer), `integration_code` (string) |
| `locations` | `type_id` (integer), `parent_id` (integer, nullable), `jurisdiction_id` (integer) |
| `ticket_categories` | `type_id` (integer), `jurisdiction_id` (integer), `parent_category_id` (integer, nullable) |
| `ticket_category_types` | `jurisdiction_id` (integer), `integration_code` (string) |
| `tickets` | `ticket_category_id` (integer) |

`parent_id` / `parent_category_id` express an optional self-referencing hierarchy within
the same type (e.g. a location's parent is another location in the same dataset); this
feature does not resolve or follow that reference — it returns the raw ID only, matching
FR-005's "return exactly one record" scope (following relationships is out of scope for
a single-record-by-ID lookup).

## Relationships

- A Reference Data Type owns many Reference Data Records (1:N), scoped strictly by type
  (Edge Cases, `spec.md` — no cross-type ID collisions are possible).
- Several types' records reference another type's records by ID (`jurisdiction_id`,
  `type_id`, `ticket_category_id`, `parent_id`, `parent_category_id`) but this feature
  does not dereference or validate those references — they are opaque IDs from this
  endpoint's point of view.

## State transitions

None. All data is static, read-only, and loaded once per running instance (Principle V);
no request mutates or transitions any record's state.
