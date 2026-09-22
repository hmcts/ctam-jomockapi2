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

**Greenfield — no application code or build file exists yet.** The first feature (`specs/001-healthcheck-api/`) stands up the minimum Gradle/Spring Boot skeleton alongside its own controller. Do not assume `build.gradle`, `src/`, or the Gradle wrapper exist until that feature's Setup tasks (T001–T005) have actually been run.

## Commands (once the Gradle skeleton exists)

```bash
./gradlew bootRun   # run the application locally
./gradlew test      # run the automated test suite
./gradlew build     # full build (compile + test)
```

## Reference data

`joh-elinks-api/` contains the real E-Links Swagger/OpenAPI reference (`swagger-ui-elinks-api-v5.pdf`) and reference-data CSV/JSON extracts used to seed synthetic data. This directory is reference material, not part of the mock's source code — it's excluded from `.gitignore`-tracked build artifacts but the data files themselves are checked in intentionally.
