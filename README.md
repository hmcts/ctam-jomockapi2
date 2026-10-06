# ctam-jomockapi2

A mock implementation of the **E-Links API** (Ministry of Justice / judiciary.uk) — a
Java/Spring Boot service that stands in for the real E-Links API for integration testing
and development, serving synthetic data only.

## Purpose

The real E-Links API's Swagger/OpenAPI specification is the source of truth for this mock.
Every endpoint the mock exposes preserves the real API's paths, methods, parameters,
response shapes, status codes, filtering, and pagination behaviour, so consumers can
develop and test against it exactly as they would against the real service — without
touching production data.

Reference extracts from the real service are local-only reference material: they are
gitignored, never committed, and the build doesn't depend on them. The checked-in fixtures
are synthetic.

## Governance

This project is built following the principles in
[`.specify/memory/constitution.md`](.specify/memory/constitution.md) — contract-first
development, a component-centric Spring Boot architecture, synthetic data only,
deterministic behaviour, and centralised, reusable handling of pagination, filtering,
validation, and reference data. Any change to the mock's observable HTTP behaviour must
trace back to the source specification.

## Development process

This repository is developed feature-by-feature using the
[Spec Kit](https://github.com/github/spec-kit) workflow. Each feature lives under
`specs/<feature-name>/` with its specification, implementation plan, and task
breakdown.

## Status

Implemented and covered by automated tests:

- `GET /api/v1/healthcheck` (see `specs/001-healthcheck-api/`)
- the Reference Data API for appointment titles (see [Reference data](#reference-data) and
  `specs/002-reference-data-api/`)

### Quality gates

```bash
./gradlew check                 # compile (-Werror), Checkstyle, the four test suites, JaCoCo + coverage gate, OWASP check
./gradlew check -PskipOwasp     # quicker local run without the NVD download (CI never skips it)
./gradlew dependencyUpdates     # dependency freshness report
```

The coverage report, merged from all four suites, is at `build/reports/jacoco/test/html/index.html`.
`check` fails if line coverage drops below 90% or branch coverage below 80%, and CI shows the figures
on each run's summary page. Run one suite at a time with
`./gradlew test`, `integrationTest`, `functionalTest` or `smokeTest`. The OWASP check reads an NVD
API key from `NVD_API_KEY`; without one it runs slowly, and in CI it fails rather than run keyless.

## Running locally

Prerequisites: none beyond the Gradle wrapper committed in this repo — it will
provision the required Java 25 toolchain automatically on first run.

```bash
./start.sh
```

This builds and runs the application in the foreground on `http://localhost:8080`
(stop it with `Ctrl+C`). It's a plain process — no IDE or Claude session required —
so it can be started independently and exercised by any HTTP client.

Once it's running, verify it directly:

```bash
curl -i http://localhost:8080/api/v1/healthcheck
```

Or exercise it with the checked-in Postman collection (`postman/`), which has a request
with assertions for each success and error case:

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`, and the
raw OpenAPI document at `http://localhost:8080/v3/api-docs`.

## Reference data

Two routes serve every configured reference-data type through the same code:

| Route | Returns |
|-------|---------|
| `GET /api/v1/reference_data/{attribute_name}` | `{"results": [...]}`: every record, in ascending `id` order |
| `GET /api/v1/reference_data/{attribute_name}/{reference_id}` | One record, as a single object |

The only type configured is `appointment_titles` (194 synthetic records), with the deprecated alias
`appointment_title`, which behaves identically. Any other attribute name returns `400`, an unknown id
returns `404`, and any query parameter returns `400`. Every error uses one shape:
`{"error": ..., "timestamp": ..., "traceId": ...}`.

```bash
curl -i http://localhost:8080/api/v1/reference_data/appointment_titles/70 \
  -H 'Authorization: Bearer local-dev-token'
```

### Authentication

Every route under `/api/` needs an `Authorization: Bearer <token>` header, except the healthcheck.
A missing or wrong token gets `401` with `WWW-Authenticate: Bearer`.

- The default token, `local-dev-token`, is **not a secret**. It exists for local development and
  Postman.
- Set `JO_SECURITY_BEARER_TOKENS` to a comma-separated list to replace it, e.g.
  `JO_SECURITY_BEARER_TOKENS=token-a,token-b ./start.sh`.
- Authentication can't be switched off: the application refuses to start if the token list is
  empty or contains a blank entry.

An optional `X-Correlation-Id` request header (`^[A-Za-z0-9._-]{1,64}$`) is echoed on the response
and in `traceId`; otherwise a UUID is generated.

### Adding a reference-data type

No Java changes are needed. Add two things:

1. **A fixture**: `src/main/resources/reference-data/<attribute_name>.json`, a JSON array of records
   with the fields `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`. The rules
   (unique positive ids, unique trimmed names, whole-second UTC timestamps, dates in order) are in
   [data-model.md §2](specs/002-reference-data-api/data-model.md). The application refuses to start
   if a fixture breaks one.
2. **A configuration entry** under `jo.reference-data.types` in `application.yml`:

   ```yaml
   jo:
     reference-data:
       types:
         - name: <attribute_name>            # lower-case letters and underscores
           aliases: [<deprecated_name>]      # optional
           fixture: classpath:reference-data/<attribute_name>.json
   ```

The new type is then served by both routes, with the same authentication, validation and errors,
and appears in the OpenAPI `attribute_name` enum. `ReferenceDataExtensionIntegrationTest` shows
this working for a type declared only in test properties. The design is in
[`specs/002-reference-data-api/`](specs/002-reference-data-api/).
