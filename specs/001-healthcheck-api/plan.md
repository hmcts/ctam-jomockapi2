# Implementation Plan: Healthcheck API

**Branch**: `001-healthcheck-api` | **Date**: 2026-09-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-healthcheck-api/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Expose a single, unauthenticated `GET /api/v5/healthcheck` endpoint that returns `HTTP 200 OK` with a `{"status":"ok"}` JSON body (per constitution Principle II's health-endpoint convention) whenever the application can serve HTTP traffic, with no dependency checks, no request inputs, and no side effects. Because this repository has no application code yet (Spec Kit scaffolding only — see `.specify/memory/constitution.md` and `README.md`), this feature also stands up the minimum Spring Boot/Gradle project skeleton needed to run one controller: a `build.gradle` and Gradle wrapper, a Spring Boot application entry point, and the healthcheck controller and its response record, following the constitution's Java Spring Boot / layer-by-layer architecture principle. Full realization of the constitution's repo-wide quality-gate and observability principles (XIII, XIV, and IX's shared error shape) is intentionally deferred — see Constitution Check and Complexity Tracking below.

**Revision note (2026-09-18)**: This plan was updated after the constitution was amended (v1.2.0 → v1.6.0) to name concrete Gradle/package-by-layer/health-response-body/error-handler conventions that didn't exist when this plan was first written. Build tool, package structure, response body, and the deferred `GlobalExceptionHandler`/`ErrorResponse` preview below reflect the amended constitution, not the original planning session.

## Technical Context

**Language/Version**: Java 25 (constitution Principle II mandates this exact version, amended 2026-09-18)

**Build Tool**: Gradle, via the Gradle wrapper (`./gradlew`) — constitution Principle II mandates Gradle explicitly (this feature originally assumed Maven before the constitution named a build tool; corrected)

**Primary Dependencies**: Spring Boot 4.1.1 (`spring-boot-starter-web`, per Principle II), `springdoc-openapi-starter-webmvc-ui` (OpenAPI/Swagger UI generation, satisfies FR-010 without hand-written spec drift; version selected for Spring Boot 4.x compatibility)

**Storage**: N/A — no persistence; the endpoint reports process liveness only (Assumption in spec.md)

**Testing**: JUnit 5 + Spring's `@WebMvcTest`/`MockMvc` for controller behaviour, `@SpringBootTest` for a thin end-to-end smoke test, and a lightweight assertion against the generated `/v3/api-docs` payload as the "contract" test (Principle XI) — see research.md for rationale

**Target Platform**: JVM 25, packaged as an executable Spring Boot JAR (no containerisation/deployment target defined yet, per constitution's Sync Impact Report deferral)

**Project Type**: Single web-service project (no frontend)

**Performance Goals**: Responds in well under 100ms under normal load (spec.md SC-002); no I/O on the request path makes this trivial to meet

**Constraints**: No request parameters/body accepted or required; no downstream dependency calls; no authentication; response must not leak internal detail (spec.md FR-003, FR-006, FR-007, FR-011)

**Scale/Scope**: Single endpoint, stateless, safe under arbitrarily frequent polling (spec.md SC-003) — no scale dimension beyond that

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** `GET /api/v5/healthcheck` → `200`, "Service is healthy" is taken verbatim from the documented contract (spec.md §Input). The `{"status":"ok"}` response body is *added* into a gap the Swagger contract leaves undefined (it specifies no body at all) — per Principle II's now-explicit health-endpoint convention — not a deviation from anything the contract pins down. This is not the kind of "intentional deviation from the API contract" Governance requires separate agreement for, since nothing documented is being contradicted; recorded here for traceability regardless (research.md Decision). |
| II. Java Spring Boot Architecture | **Pass.** Implemented as Java 25 + Spring Boot 4.1.1 + Gradle (per the amended principle's exact pins); package-by-layer structure (`controllers/`, `domain/`) under base package `uk.gov.hmcts.elinks`; controller stays thin (no business logic exists to extract); health-endpoint response convention followed (`HealthResponse` record, `{"status":"ok"}`). |
| III. Reuse Before Duplication | **N/A.** No pagination, filtering, mapping, or data generation involved. |
| IV. Synthetic Data Only | **N/A.** No data is served. |
| V. Deterministic Behaviour | **Pass.** Response is constant (`200`, `{"status":"ok"}`) whenever the process is up; no randomness. |
| VI. Behavioural Fidelity | **N/A.** This principle governs *contract-defined* parameters (e.g. `updated_since`) being honoured rather than silently ignored. The healthcheck contract defines none, so an unexpected/undefined query parameter being ignored (spec.md Edge Cases) is not the scenario this principle addresses — there is no defined parameter to honour or ignore. |
| VII. Generic Reference-Data Handling | **N/A.** Not a reference-data endpoint. |
| VIII. Typed API Models | **Pass.** The response is a typed `HealthResponse` record (single `status` field), not a `Map`; per the amended principle, no Lombok is needed on a record (no boilerplate to reduce) and no MapStruct mapper is needed (no domain/entity source to translate from — research.md Decision). |
| IX. Centralised Validation and Error Handling | **Partial — deferred, documented below.** No `GlobalExceptionHandler`/`ErrorResponse` is added by this feature; Spring Boot's default error handling (not yet the constitution's full shared error shape) covers the unexpected-failure edge case (spec.md EC-003) for now. |
| X. Configuration Over Hard-Coding | **Pass (N/A in practice).** Nothing scenario-specific is hard-coded; there is no scenario to configure. |
| XI. Testability | **Pass.** Unit-level controller test, a `@SpringBootTest` smoke test, and a generated-OpenAPI contract check are all in scope (research.md). |
| XII. Simplicity and Maintainability | **Pass.** Only the two layer packages this feature actually needs (`controllers/`, `domain/`) are created — `services/`, `repository/`, `mappers/`, `entity/`, `exceptions/`, `filters/`, `config/` stay absent until a feature needs them. No service/repository/mapper is introduced for a single stateless literal response (spec.md IC-003 equivalent intent). |
| XIII. Automated Quality Gates | **Deferred, documented below.** CI pipeline, static analysis, dependency vulnerability scanning, and coverage reporting are repo-wide concerns with no existing setup to extend; out of scope for a single endpoint feature. |
| XIV. Observability & Traceability | **Deferred, documented below.** Correlation-ID propagation and shared structured logging are repo-wide, cross-cutting concerns; this endpoint does not perform any logging that would need them, so nothing is done here that other endpoints couldn't reuse later. |

**Non-principle constraint check**: Technology & Architecture Constraints also requires a checked-in `.editorconfig` (code style/formatting), which isn't tied to a numbered Core Principle above and so has no row of its own. **Pass** — created by tasks.md T005, unlike the IX/XIII/XIV deferrals below, since it's a single static file with no dependency/build/CI cost, not repo-wide tooling. (Added 2026-09-18 in response to `/speckit-analyze` finding K1, which this requirement had slipped through in every prior amendment round for exactly this reason — no principle number to hang a table row off of.)

**Result**: No blocking violations. Three principles (IX, XIII, XIV) are intentionally not (fully) implemented by this feature — see Complexity Tracking for the documented rationale required by Governance.

## Project Structure

### Documentation (this feature)

```text
specs/001-healthcheck-api/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── healthcheck.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
build.gradle
.editorconfig                                          # code style (Technology & Architecture Constraints)
gradlew
gradlew.bat
gradle/wrapper/...

src/
├── main/
│   ├── java/uk/gov/hmcts/elinks/
│   │   ├── Application.java                        # Spring Boot entry point
│   │   ├── controllers/
│   │   │   └── HealthcheckController.java           # GET /api/v5/healthcheck
│   │   ├── domain/
│   │   │   └── HealthResponse.java                  # record HealthResponse(String status)
│   │   └── exceptions/  [NOT CREATED — deferred, see Complexity Tracking]
│   │       ├── GlobalExceptionHandler.java  (future: @RestControllerAdvice, Principle IX)
│   │       └── ErrorResponse.java           (future: record ErrorResponse(String error, Instant timestamp, String traceId))
│   └── resources/
│       └── application.yml                          # server port, app name only
└── test/
    └── java/uk/gov/hmcts/elinks/
        └── controllers/
            ├── HealthcheckControllerTest.java       # @WebMvcTest — 200 with {"status":"ok"}, non-GET rejected
            ├── HealthcheckSmokeTest.java             # @SpringBootTest — real HTTP round trip
            └── HealthcheckOpenApiContractTest.java   # asserts /v3/api-docs documents the endpoint per FR-010
```

**Structure Decision**: Single Gradle/Spring Boot project — there is no frontend and no second deployable, so a multi-module or web+backend split would be pure over-engineering (Principle XII). The codebase is organised **by layer** under base package `uk.gov.hmcts.elinks` (constitution Principle II), not by feature — `controllers/` and `domain/` are the only layer packages this feature actually creates. `exceptions/` is shown above with its two future files named (`GlobalExceptionHandler.java`, `ErrorResponse.java`) precisely because Principle IX/XIV already fixed those names, but the package itself is **not created by this feature** — it's a preview of what a future foundation feature will add, not a task in tasks.md. `services/`, `repository/`, `mappers/`, `entity/`, `filters/`, and `config/` have no names fixed yet either and are simply absent, not previewed. This supersedes this feature's original package-by-feature (`healthcheck/`) structure under `uk.gov.hmcts.reform.ctam.jomockapi`, decided before the constitution named this convention.

## Complexity Tracking

> Documenting *deferred* constitution compliance, per Governance's requirement that any intentional deviation be explicit and agreed rather than silent.

| Deferred Principle | Why Deferred Here | Why Not Implemented Now |
|---|---|---|
| IX. Centralised Validation and Error Handling (shared error response shape — record `ErrorResponse(String error, Instant timestamp, String traceId)`, applied even to framework-level 404/405 responses, produced by a single `GlobalExceptionHandler` `@RestControllerAdvice` in `exceptions/`) | This requires the same correlation-ID/structured-logging foundation as Principle XIV below (`traceId` has nothing to read until a tracing filter populates it), plus the repo-wide `exceptions/` handler; neither exists yet, and this endpoint has no error branch of its own to build one around. No `exceptions/` package is created by this feature — it stays absent rather than being created empty, per Principle XII. | Building a one-off shared error shape as a side effect of "add a healthcheck endpoint" would be a de facto repo-wide decision made without being framed as one. Spring Boot's default error handling (including its default `405` for the non-`GET` case) minimally satisfies spec.md FR-009/EC-003 ("standard error-handling behaviour applies, not forced 200") for this endpoint in the meantime. **Recommendation**: build `GlobalExceptionHandler`/`ErrorResponse` as part of the same foundation feature recommended for XIII/XIV below, once a correlation-ID filter exists to populate `traceId`. |
| XIII. Automated Quality Gates (CI pipeline, static analysis, dependency scanning, coverage reporting) | These are repo-wide build/CI concerns, not something a single endpoint's controller code can express on its own; the repo currently has no build file or CI config to extend at all. | Building full CI/quality tooling as a side effect of "add a healthcheck endpoint" would silently balloon this feature's scope (explicitly out of scope per spec.md §13 "unrelated infrastructure redesign") and produce tooling nobody has reviewed as its own decision. **Recommendation**: raise a dedicated "project build & CI foundation" feature to establish this once, which every subsequent endpoint (including this one) then inherits. |
| XIV. Observability & Traceability (correlation-ID propagation, shared structured logging, log-injection sanitisation) | Same reasoning — this is cross-cutting infrastructure meant to apply to *every* request/response and log line app-wide, not something meaningful to build for one endpoint that performs no logging and has no error branch worth tracing. | Implementing a one-off correlation-ID filter just for this endpoint would either (a) not actually get reused by later endpoints, duplicating the effort, or (b) be a de facto repo-wide decision made without being framed as one. **Recommendation**: fold into the same foundation feature as XIII, or its own "observability foundation" feature, before or alongside the next endpoint that actually produces meaningful logs/errors. |

All three deferrals are scoped to *cross-cutting infrastructure*, not to this endpoint's own correctness — the healthcheck endpoint itself fully satisfies every functional and non-functional requirement in spec.md without them.

## Post-Design Constitution Re-Check

Re-evaluated after Phase 1 (research.md, data-model.md, contracts/healthcheck.md, quickstart.md): no new dependency or layer was introduced beyond what the initial Constitution Check already assessed. `springdoc-openapi` is documentation tooling, not a business-logic component, so it doesn't change the Principle II/III/XII assessment. The `HealthResponse` record and no-service-layer decision (research.md) keep Principles VIII and XII passing exactly as before. No new violations; the same three deferrals (IX, XIII, XIV) stand, unchanged.

**Re-evaluated again after constitution v1.2.0 → v1.6.0** (2026-09-18, this revision): Gradle, package-by-layer, the `{"status":"ok"}` response body, and the named `GlobalExceptionHandler`/`ErrorResponse` classes are now reflected throughout this plan, research.md, data-model.md, contracts/healthcheck.md, and quickstart.md (the latter two only where the deferred error-handler naming is a relevant forward-reference, not a build task). No principle assessment changed from Pass to Fail or vice versa — the updates were to *how* Principles II, VIII, and IX are satisfied/deferred, not *whether* they are. The IX/XIII/XIV deferrals remain unchanged and still fully justified.
