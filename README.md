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

Reference data extracts used to seed the mock's synthetic dataset live under
`joh-elinks-api/ReferenceData/`.

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

The Gradle/Spring Boot skeleton is in place, and the first endpoint — `GET
/api/v1/healthcheck` — is implemented and covered by automated tests (see
`specs/001-healthcheck-api/`).

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

Or exercise it with the checked-in Postman collection (`postman/`), which covers
both the success and method-not-allowed cases with assertions:

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`, and the
raw OpenAPI document at `http://localhost:8080/v3/api-docs`.
