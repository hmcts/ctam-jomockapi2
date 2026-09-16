# Implementation Plan: Healthcheck Endpoint

**Branch**: `001-healthcheck-endpoint` | **Date**: 2026-09-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-healthcheck-endpoint/spec.md`

## Summary

Expose a mock `GET /elinks/api/v5/healthcheck` endpoint that mirrors the E-Links API's
healthcheck operation exactly: no parameters, no authentication, and an HTTP 200 with an
empty response body on success (per the Clarifications in `spec.md`). This is the first
feature in the repository, so the plan also bootstraps the minimal Spring Boot project
skeleton (Principle II) needed to serve it — a thin controller with no business logic,
since the operation has nothing to compute, validate, or map.

## Technical Context

**Language/Version**: Java 25 (LTS)

**Primary Dependencies**: Spring Boot 4.1.1, `spring-boot-starter-web`

**Storage**: N/A — the healthcheck endpoint reads and persists no data

**Testing**: JUnit 5 + `spring-boot-starter-test` (Spring `MockMvc` for controller/API tests)

**Target Platform**: JVM, deployed as a standard Spring Boot application (Linux server / container)

**Project Type**: Single web-service (Gradle, standard Spring Boot layout)

**Performance Goals**: Sub-second response (per SC-003); no artificial delay introduced (FR-006)

**Constraints**: No authentication (FR-003); empty response body (FR-004); deterministic,
state-independent response (FR-005); no parameters accepted or required (FR-002)

**Scale/Scope**: Single endpoint; this plan also stands up the base Spring Boot
application skeleton since no prior feature exists in this repository, plus the
project-wide shared error/observability/quality-gate infrastructure required by
Constitution v1.1.0 (Principles IX, XIII, XIV) so later features inherit it rather than
each retrofitting their own (see `research.md`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Contract-First Development | PASS | Path (`/elinks/api/v5/healthcheck`), method (`GET`), no parameters, 200 status, and empty body all sourced directly from `joh-elinks-api/swagger-ui-elinks-api-v5.pdf`; the one contract gap (no documented response schema) was resolved via an explicit, recorded clarification rather than a silent assumption. |
| II. Java Spring Boot Architecture | PASS | Java 25 + Spring Boot 4.1.1; thin controller only — there is no business logic to place in a service, since the operation performs no computation. |
| III. Reuse Before Duplication | N/A (this feature) | No pagination, filtering, mapping, or reference-data behaviour is involved. Nothing to build here should be duplicated by a future endpoint; conversely this endpoint introduces no shared component other endpoints would need. |
| IV. Synthetic Data Only | PASS | No data is served; there is no risk of production/personal data exposure. |
| V. Deterministic Behaviour | PASS | FR-005 requires an identical response on every call; no randomness or per-request state is involved. |
| VI. Behavioural Fidelity | PASS | No parameters are defined by the contract, so there is nothing to silently ignore; FR-002 confirms this explicitly. |
| VII. Generic Reference-Data Handling | N/A | This feature is not a reference-data endpoint. |
| VIII. Typed API Models | PASS | Response body is empty by contract (FR-004); no DTO is needed, so there is no `Map<String,Object>` shortcut to avoid. |
| IX. Centralised Validation and Error Handling *(shape amended v1.1.0)* | PASS | No input to validate. The one error case this endpoint can produce (405 on an unsupported method) is handled by the shared `GlobalExceptionHandler`, returning the shared `ErrorResponse` shape (message + correlation ID) mandated by the amended principle, not a bespoke body. |
| X. Configuration Over Hard-Coding | PASS | Nothing environment-specific is hard-coded; the endpoint's behaviour is fixed by contract, not by scenario configuration this feature needs to expose. |
| XI. Testability | PASS | Controller/API test (MockMvc) and a contract test asserting path/method/status/empty-body are planned (see `research.md` for the contract-test approach given no machine-readable OpenAPI file exists). |
| XII. Simplicity and Maintainability | PASS | Single thin controller, no premature abstraction; matches the feature's actual scope. |
| XIII. Automated Quality Gates *(added v1.1.0)* | PASS | Gradle build gets the `uk.gov.hmcts.java` plugin (checkstyle + OWASP dependency-check, matching CNP framework tooling), JaCoCo, compiler warnings-as-errors, and a CI workflow as part of this feature's bootstrap (`research.md`), since this is the feature that establishes the build itself. |
| XIV. Observability & Traceability *(added v1.1.0)* | PASS | A shared `CorrelationIdFilter` and `GlobalExceptionHandler`/`ErrorResponse` are introduced here (`research.md`) so every request — including this endpoint's — carries a correlation ID and any error response uses the shared shape; the healthcheck's own `200` body stays empty per FR-004 (a header, not a body). |

No violations requiring justification. Complexity Tracking is not needed.

*Post-Phase 1 re-check*: The design artifacts (`data-model.md`, `contracts/healthcheck.md`,
`quickstart.md`) introduce no entities or DTOs beyond the shared `ErrorResponse`/
`CorrelationIdFilter`/`GlobalExceptionHandler` infrastructure required by Principles IX,
XIII, and XIV; the healthcheck controller itself remains a single thin handler. No new
violations.

*Amendment re-check (constitution v1.0.0 → v1.1.0, 2026-09-15)*: This plan was
originally written against v1.0.0 (Principles I–XII only). Rows for the two added
principles (XIII, XIV) and the amended Principle IX are now included above; see
`research.md` for the corresponding new decisions. No other row changed.

## Project Structure

### Documentation (this feature)

```text
specs/001-healthcheck-endpoint/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── healthcheck.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

This is the first feature in the repository, so this plan also establishes the base
Gradle/Spring Boot project layout. Single project (standard Spring Boot Gradle
layout), matching the CNP framework's own `service-api-marketplace` conventions:

```text
build.gradle
settings.gradle
gradlew, gradlew.bat, gradle/wrapper/
config/owasp/suppressions.xml         # OWASP dependency-check accepted-finding records (Principle XIII)

.github/workflows/ci.yml              # ./gradlew check on every PR and push to main (Principle XIII)

src/
├── main/
│   ├── java/uk/gov/moj/elinks/mock/
│   │   ├── ElinksMockApplication.java
│   │   ├── healthcheck/
│   │   │   └── HealthcheckController.java
│   │   ├── tracing/
│   │   │   └── CorrelationIdFilter.java     # shared: correlation ID in MDC + response header (Principle XIV)
│   │   ├── logging/
│   │   │   └── LogSanitizer.java            # shared: strips CR/LF from values before logging (Principle XIV)
│   │   └── error/
│   │       ├── ErrorResponse.java           # shared error DTO: message, timestamp, traceId (Principle IX)
│   │       └── GlobalExceptionHandler.java  # shared: maps framework failures to ErrorResponse (Principle IX)
│   └── resources/
│       └── application.yml
└── test/
    └── java/uk/gov/moj/elinks/mock/
        ├── healthcheck/
        │   ├── HealthcheckControllerTest.java   # controller/API test
        │   └── HealthcheckContractTest.java      # contract test vs. source spec
        ├── tracing/
        │   └── CorrelationIdFilterTest.java
        └── error/
            └── GlobalExceptionHandlerTest.java
```

**Structure Decision**: Single Gradle-based Spring Boot application at the repository
root (`src/main/java`, `src/test/java`), per Principle II. The `healthcheck` sub-package
under the base package (`uk.gov.moj.elinks.mock`, see `research.md`) holds this
feature's controller and tests; later features will add their own sub-packages
(and, where behaviour is genuinely shared, common packages such as `pagination`,
`referencedata`, or `errorhandling`) alongside it rather than inside it, per Principle
III. The new `tracing`, `logging`, and `error` packages are shared, project-wide
infrastructure (Principles IX, XIII, XIV) introduced here because this feature
bootstraps the skeleton; later features extend these rather than creating their own
equivalents.

## Complexity Tracking

*No entries — Constitution Check reported no violations.*
