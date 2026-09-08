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

**Project Type**: Single web-service (Maven, standard Spring Boot layout)

**Performance Goals**: Sub-second response (per SC-003); no artificial delay introduced (FR-006)

**Constraints**: No authentication (FR-003); empty response body (FR-004); deterministic,
state-independent response (FR-005); no parameters accepted or required (FR-002)

**Scale/Scope**: Single endpoint; this plan also stands up the base Spring Boot
application skeleton since no prior feature exists in this repository

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
| IX. Centralised Validation and Error Handling | PASS | No input to validate. Global exception handling is still applied at the application level so any unexpected error still returns a consistent, centrally-handled response — this feature does not bypass it. |
| X. Configuration Over Hard-Coding | PASS | Nothing environment-specific is hard-coded; the endpoint's behaviour is fixed by contract, not by scenario configuration this feature needs to expose. |
| XI. Testability | PASS | Controller/API test (MockMvc) and a contract test asserting path/method/status/empty-body are planned (see `research.md` for the contract-test approach given no machine-readable OpenAPI file exists). |
| XII. Simplicity and Maintainability | PASS | Single thin controller, no premature abstraction; matches the feature's actual scope. |

No violations requiring justification. Complexity Tracking is not needed.

*Post-Phase 1 re-check*: The design artifacts (`data-model.md`, `contracts/healthcheck.md`,
`quickstart.md`) introduce no entities, DTOs, or shared components beyond a single thin
controller, so every row above still holds unchanged after design. No new violations.

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
Maven/Spring Boot project layout. Single project (standard Spring Boot Maven layout):

```text
pom.xml

src/
├── main/
│   ├── java/uk/gov/moj/elinks/mock/
│   │   ├── ElinksMockApplication.java
│   │   └── healthcheck/
│   │       └── HealthcheckController.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/uk/gov/moj/elinks/mock/
        └── healthcheck/
            ├── HealthcheckControllerTest.java   # controller/API test
            └── HealthcheckContractTest.java      # contract test vs. source spec
```

**Structure Decision**: Single Maven-based Spring Boot application at the repository
root (`src/main/java`, `src/test/java`), per Principle II. The `healthcheck` sub-package
under the base package (`uk.gov.moj.elinks.mock`, see `research.md`) holds this
feature's controller and tests; later features will add their own sub-packages
(and, where behaviour is genuinely shared, common packages such as `pagination`,
`referencedata`, or `errorhandling`) alongside it rather than inside it, per Principle
III.

## Complexity Tracking

*No entries — Constitution Check reported no violations.*
