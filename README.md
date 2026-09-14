# ctam-jomockapi2

A mock implementation of the **E-Links API** (Ministry of Justice / judiciary.uk) — a
Java/Spring Boot service that stands in for the real E-Links API for integration testing
and development, serving synthetic data only.

## Purpose

The real E-Links API's Swagger/OpenAPI specification
(`joh-elinks-api/swagger-ui-elinks-api-v5.pdf`) is the source of truth for this mock.
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
breakdown:

- `specs/001-healthcheck-endpoint/` — the mock's `GET /elinks/api/v5/healthcheck`
  endpoint (in progress; see that directory's `spec.md` for details).

## Status

The project is in the specification/planning stage — endpoint specs, plans, and tasks
are being defined ahead of implementation. No application code exists yet.
