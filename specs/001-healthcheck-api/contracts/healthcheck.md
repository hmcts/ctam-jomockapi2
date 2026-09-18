# Contract: Healthcheck Endpoint

## `GET /api/v1/healthcheck`

**Summary (OpenAPI operation)**: Healthcheck

**Authentication**: None required (public endpoint — see spec.md Clarifications, FR-011).

### Request

- **Path parameters**: none
- **Query parameters**: none
- **Headers**: none required
- **Body**: none required

Any unexpected query parameters or body sent by a caller MUST be ignored, not rejected (spec.md Edge Cases EC-004).

### Responses

| Status | Description | Body |
|---|---|---|
| `200` | Service is healthy | `{"status": "ok"}` |

The body is a `HealthResponse` value (data-model.md) with a single `status` field, per constitution Principle II's health-endpoint response convention. The source E-Links Swagger contract does not itself define this body — it only specifies `200` + the description "Service is healthy" — so this is an addition into a gap the contract leaves undefined, not a deviation from anything the contract pins down (research.md Decision; see plan.md Constitution Check for the Principle I/II reconciliation).

No other status code is part of this contract. If an unexpected internal failure occurs, the application's standard (default Spring Boot) error handling applies — this endpoint MUST NOT be special-cased to force `200` regardless of actual health (spec.md EC-003). This is a temporary state: constitution Principle IX ultimately requires every error response to use a shared `ErrorResponse(String error, Instant timestamp, String traceId)` shape produced by a `GlobalExceptionHandler`, but building that is deferred to a future foundation feature (plan.md Complexity Tracking) — not introduced by this feature.

A request using any HTTP method other than `GET` against `/api/v1/healthcheck` MUST NOT invoke this endpoint's success behaviour (spec.md FR-008); Spring's default method-not-allowed handling for the path applies.

### OpenAPI representation (FR-010)

Generated via `springdoc-openapi` from the controller method and the `HealthResponse` record — equivalent to:

```yaml
/api/v1/healthcheck:
  get:
    summary: Healthcheck
    responses:
      "200":
        description: Service is healthy
        content:
          application/json:
            schema:
              type: object
              properties:
                status:
                  type: string
                  example: ok
```

No request schema is declared, and the response schema is limited to the single `status` field defined above — matching the "don't invent beyond the established convention" constraint in spec.md and research.md.
