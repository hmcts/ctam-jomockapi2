# Contract: Healthcheck

**Source of truth**: `joh-elinks-api/swagger-ui-elinks-api-v5.pdf` (E-Links API 5.0,
OAS 3.0), "Utils" group, page 1–2 — no machine-readable OpenAPI file is checked into the
repository (see `research.md` for the contract-test implication of this).

## Operation

| Field | Value |
|---|---|
| Server base path | `/elinks` |
| Operation path | `/api/v5/healthcheck` |
| Effective mock path | `/elinks/api/v5/healthcheck` |
| Method | `GET` |
| Summary (source spec) | Healthcheck |
| Authentication | None required (no lock indicator on this operation in the source spec) |

## Request

- **Parameters**: none (path, query, header, or body).
- Any unexpected query parameters or body content on the request MUST be ignored — they
  MUST NOT cause an error or change the response (Edge Cases, `spec.md`).
- **Correlation ID** *(added: Constitution v1.1.0, Principle XIV)*: an inbound
  `X-Correlation-Id` header is honoured if present; the caller does not need to supply
  one.

## Response

### 200 OK

- **Description (source spec)**: "Service is healthy"
- **Body**: empty (no JSON or text payload) — per the Clarifications in `spec.md`, since
  the source spec documents no response media type or schema for this operation.
- **Headers** *(added: Constitution v1.1.0, Principle XIV)*: `X-Correlation-Id` is
  always present, echoing the inbound value or a freshly generated one. This is a
  header, not a body — it does not conflict with the empty-body requirement above.
- **Determinism**: identical on every call against a running instance (FR-005); no
  dependency on dataset, request history, or other mutable state.

### Unsupported HTTP methods

- Any method other than `GET` against this path MUST be rejected in a way that clearly
  indicates the method is not supported (Edge Cases, `spec.md`), consistent with the
  application's centralised error handling (Constitution Principle IX) rather than a
  bespoke per-endpoint check.
- **Status**: `405 Method Not Allowed`.
- **Body** *(added: Constitution v1.1.0, Principle IX amendment)*: the shared
  `ErrorResponse` shape — `{"message": string, "timestamp": ISO-8601 string, "traceId":
  string}` — where `traceId` matches the request's `X-Correlation-Id`.

## Out of scope for this contract

- No downstream dependency, dataset, or other mock endpoint's health is checked or
  reflected by this operation — it reports only this service's own availability.
