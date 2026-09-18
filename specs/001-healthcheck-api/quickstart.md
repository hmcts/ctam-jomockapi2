# Quickstart: Healthcheck API

Validates that `GET /api/v1/healthcheck` works end-to-end once implemented per plan.md / contracts/healthcheck.md.

## Prerequisites

- Java 25
- Gradle (wrapper — `./gradlew`)

## Run the application

```bash
./gradlew bootRun
```

## Validate

```bash
curl -i http://localhost:8080/api/v1/healthcheck
```

**Expected outcome**:

- `HTTP/1.1 200` status line
- JSON body `{"status":"ok"}` (see contracts/healthcheck.md)

## Validate no-input behaviour (spec.md AC-002)

```bash
curl -i "http://localhost:8080/api/v1/healthcheck?unexpected=1" -d '{"ignored":true}'
```

**Expected outcome**: still `200` with `{"status":"ok"}` — extra query parameters/body are ignored, not rejected.

## Validate unsupported-method behaviour (spec.md AC-003, FR-008)

```bash
curl -i -X POST http://localhost:8080/api/v1/healthcheck
```

**Expected outcome**: Spring's default `405 Method Not Allowed` — the healthcheck success behaviour must not be triggered.

## Validate OpenAPI documentation (spec.md AC-006, FR-010)

```bash
curl -s http://localhost:8080/v3/api-docs | grep -A10 '"/api/v1/healthcheck"'
```

**Expected outcome**: the path is documented under a `get` operation summarised "Healthcheck" with a `200` response described "Service is healthy", and a response schema limited to the single `status` string field.

## Automated equivalents

Run the automated test suite (see tasks.md) instead of manual curl for CI:

```bash
./gradlew test
```

This is expected to cover the same three scenarios above via `HealthcheckControllerTest`, `HealthcheckSmokeTest`, and `HealthcheckOpenApiContractTest` (see plan.md Project Structure).
