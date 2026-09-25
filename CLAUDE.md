# CLAUDE.md

Guidance for Claude Code working in this repository.

## What this project is

A mock implementation of the **E-Links API** (Ministry of Justice / judiciary.uk) — a Java/Spring Boot service standing in for the real E-Links API for integration testing and development, serving synthetic data only. See `README.md` for the full overview.

## Source of truth

**`.specify/memory/constitution.md` is the authoritative source** for architecture, tech stack, layering, testing, and quality-gate rules — not this file. Read it before making any non-trivial change. Key pins as of the current version:

- Java 25, Spring Boot 4.1.1, built with **Gradle** (via the wrapper) — never Maven
- Package-by-layer under base package `uk.gov.hmcts.ctam.jo` (`controllers/`, `services/`, `repository/`, `mappers/`, `domain/`, `entity/`, `exceptions/`, `filters/`, `config/`) — not package-by-feature
- Controllers stay thin; business logic lives in services
- Typed DTOs everywhere (no `Map<String, Object>`); Lombok for boilerplate, MapStruct for mapping
- One shared error response shape via a single `GlobalExceptionHandler`
- Every request gets a correlation ID (`X-Correlation-Id`), except health/readiness/info/metrics-style endpoints, which are explicitly exempted
- All significant behaviour MUST have automated tests (unit, controller/API, contract)
- Every endpoint MUST appear in a generated (springdoc) OpenAPI spec, kept in step with the code, with Swagger UI exposed (Principle XVIII)
- Every endpoint MUST have Postman artifacts in `postman/` (collection + environment, with test scripts and no hard-coded environment values), updated in the same feature (Principle XVII)
- Gradle dependency locking is on: after changing dependencies, regenerate `gradle.lockfile` with `./gradlew dependencies --write-locks`

The constitution is currently at **v1.10.0** (last amended 2026-09-22).

If a change would conflict with the constitution, that's a blocker — resolve it by updating the constitution deliberately (via `/speckit-constitution`), not by working around it silently.

## Development workflow

This repo is built feature-by-feature using the [Spec Kit](https://github.com/github/spec-kit) workflow. Each feature lives under `specs/<feature-name>/` with its own `spec.md`, `plan.md`, `tasks.md`, and supporting design docs. Typical flow for a new feature:

```
/speckit-specify   → write/refine the spec
/speckit-clarify    → resolve ambiguities
/speckit-plan       → produce the implementation plan
/speckit-tasks      → generate the task list
/speckit-analyze    → cross-check spec/plan/tasks/constitution before implementing
/speckit-implement  → execute the tasks
```

Always run `/speckit-analyze` before `/speckit-implement` — it catches coverage gaps, terminology drift, and constitution conflicts across the three core artifacts.

## Current repo state

- **`001-healthcheck-api` — done, merged to `main`.** It set up the Gradle/Spring Boot skeleton (`build.gradle`, wrapper, `gradle.lockfile`), `Application`, `HealthcheckController` (`GET /api/v1/healthcheck` → `{"status": "ok"}`), the `HealthResponse` DTO, springdoc OpenAPI/Swagger UI, controller/contract/smoke tests, the Postman collection and environment, and `start.sh`.
- **`002-reference-data-api` — spec, plan and tasks written, not implemented yet** (branch `002-reference-data-api`, PR #7). It will add `GET /api/v1/reference_data/{attribute_name}` and `GET /api/v1/reference_data/{attribute_name}/{reference_id}` (`appointment_titles`, with deprecated alias `appointment_title`). As the first business endpoint, it will also build the shared infrastructure 001 left out: Bearer-token auth, correlation-ID filter, structured and sanitised logging, `ErrorResponse`/`GlobalExceptionHandler`, Lombok/MapStruct, and the Principle XIII quality gates and test suites. See `specs/002-reference-data-api/plan.md`.

Don't assume anything listed for 002 exists in `src/` until its tasks have actually been run.

## Commands

```bash
./start.sh                                   # build and run on http://localhost:8080 (foreground; wraps ./gradlew bootRun)
./gradlew bootRun                            # run the application locally
./gradlew test                               # run the automated test suite
./gradlew build                              # full build (compile + test)
./gradlew dependencies --write-locks         # regenerate gradle.lockfile after dependency changes
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json   # run the Postman checks against a running instance
```

With the app running: Swagger UI at `http://localhost:8080/swagger-ui/index.html`, OpenAPI document at `http://localhost:8080/v3/api-docs`.

## Reference data

`joh-elinks-api/` contains the real E-Links Swagger/OpenAPI reference (`swagger-ui-elinks-api-v5.pdf`) and reference-data CSV/JSON extracts used to seed synthetic data. This directory is reference material, not part of the mock's source code — it's excluded from `.gitignore`-tracked build artifacts but the data files themselves are checked in intentionally.
