# Quickstart: Healthcheck Endpoint

**Feature**: `001-healthcheck-endpoint` | **Contract**: [contracts/healthcheck.md](./contracts/healthcheck.md)

## Prerequisites

- Java 25 (see `research.md`)
- Gradle (bundled wrapper `./gradlew` once the project skeleton exists)

## Run the application

```bash
./gradlew bootRun
```

The application starts on its configured local port (default `8080` unless overridden
in `src/main/resources/application.yml`).

## Validate the endpoint manually

```bash
curl -i http://localhost:8080/elinks/api/v5/healthcheck
```

**Expected outcome** (per `contracts/healthcheck.md`):

- HTTP status: `200 OK`
- Response body: empty
- No `Authorization` header required
- An `X-Correlation-Id` response header is present (Constitution Principle XIV)

Repeat the call several times (including with an extra, undocumented query parameter,
e.g. `?foo=bar`) — every response must be identical: `200 OK` with an empty body.

Time the healthy-path call to confirm it comfortably meets the sub-second target (SC-003):

```bash
curl -o /dev/null -s -w "%{time_total}\n" http://localhost:8080/elinks/api/v5/healthcheck
```

**Expected outcome**: a total time well under `1.0` (seconds).

## Validate an unsupported method

```bash
curl -i -X POST http://localhost:8080/elinks/api/v5/healthcheck
```

**Expected outcome**: HTTP `405 Method Not Allowed`, produced by the application's
centralised error handling (not a special case in the healthcheck controller), with a
JSON body in the shared error shape: `{"message": "...", "timestamp": "...", "traceId":
"..."}` (Constitution Principle IX, amended v1.1.0), where `traceId` matches the
response's `X-Correlation-Id` header.

## Run the quality gates

```bash
./gradlew check
```

**Expected outcome**: compiler warnings-as-errors, checkstyle, OWASP dependency-check,
and JaCoCo coverage reporting all run and pass with no violations (Constitution
Principle XIII, added v1.1.0).

## Run automated tests

```bash
./gradlew test
```

**Expected outcome**: the controller/API test and the contract test described in
`plan.md` (`HealthcheckControllerTest`, `HealthcheckContractTest`) both pass, confirming
the same behaviour verified manually above.
