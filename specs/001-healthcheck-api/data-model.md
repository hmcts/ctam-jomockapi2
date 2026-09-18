# Data Model: Healthcheck API

## HealthResponse (value object, not a persisted entity)

The healthcheck endpoint's response body, per constitution Principle II's health-endpoint
convention and research.md's Decision.

| Field | Type | Value | Notes |
|---|---|---|---|
| `status` | `String` | `"ok"` (constant) | Not derived from any dependency check (spec.md FR-006) — always `"ok"` whenever the process can handle the request at all. |

Represented as a Java `record HealthResponse(String status)`. No relationships, no
identity/uniqueness rules, no lifecycle/state transitions, and no persistence — this is a
literal, stateless value returned on every successful call, not domain data (spec.md
§Key Entities, §Assumptions). No repository, entity, or mapper is introduced: there is no
underlying domain/entity representation to map from (research.md Decision).
