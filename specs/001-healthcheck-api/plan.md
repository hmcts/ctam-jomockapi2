# Implementation Plan: Healthcheck API

**Branch**: `001-healthcheck-api` | **Date**: 2026-09-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-healthcheck-api/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Expose a single, unauthenticated `GET /api/v1/healthcheck` endpoint that returns `HTTP 200 OK` with a `{"status":"ok"}` JSON body (per constitution Principle II's health-endpoint convention) whenever the application can serve HTTP traffic, with no dependency checks, no request inputs, and no side effects. Because this repository has no application code yet (Spec Kit scaffolding only — see `.specify/memory/constitution.md` and `README.md`), this feature also stands up the minimum Spring Boot/Gradle project skeleton needed to run one controller: a `build.gradle` and Gradle wrapper, a Spring Boot application entry point, and the healthcheck controller and its response record, following the constitution's Java Spring Boot / layer-by-layer architecture principle. Full realization of the constitution's repo-wide quality-gate and observability principles (XIII, XIV, and IX's shared error shape) is intentionally deferred — see Constitution Check and Complexity Tracking below.

**Revision note (2026-09-18)**: This plan was updated after the constitution was amended (v1.2.0 → v1.7.0) to name concrete Gradle/package-by-layer/health-response-body/error-handler conventions that didn't exist when this plan was first written, and to add Principles XV (Security by Design) and XVI (Object-Oriented Design Discipline). Build tool, package structure, response body, the deferred `GlobalExceptionHandler`/`ErrorResponse` preview, and the new XV/XVI assessment rows below all reflect the amended constitution, not the original planning session.

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
| I. Contract-First Development | **Pass.** `GET /api/v1/healthcheck` → `200`, "Service is healthy" is taken verbatim from the documented contract (spec.md §Input). The `{"status":"ok"}` response body is *added* into a gap the Swagger contract leaves undefined (it specifies no body at all) — per Principle II's now-explicit health-endpoint convention — not a deviation from anything the contract pins down. This is not the kind of "intentional deviation from the API contract" Governance requires separate agreement for, since nothing documented is being contradicted; recorded here for traceability regardless (research.md Decision). |
| II. Java Spring Boot Architecture | **Pass.** Implemented as Java 25 + Spring Boot 4.1.1 + Gradle (per the amended principle's exact pins); package-by-layer structure (`controllers/`, `domain/`) under base package `uk.gov.hmcts.ctam.jo`; controller stays thin (no business logic exists to extract); health-endpoint response convention followed (`HealthResponse` record, `{"status":"ok"}`). |
| III. Reuse Before Duplication | **N/A.** No pagination, filtering, mapping, or data generation involved. |
| IV. Synthetic Data Only | **N/A.** No data is served. |
| V. Deterministic Behaviour | **Pass.** Response is constant (`200`, `{"status":"ok"}`) whenever the process is up; no randomness. |
| VI. Behavioural Fidelity | **N/A.** This principle governs *contract-defined* parameters (e.g. `updated_since`) being honoured rather than silently ignored. The healthcheck contract defines none, so an unexpected/undefined query parameter being ignored (spec.md Edge Cases) is not the scenario this principle addresses — there is no defined parameter to honour or ignore. |
| VII. Generic Reference-Data Handling | **N/A.** Not a reference-data endpoint. |
| VIII. Typed API Models | **Pass.** The response is a typed `HealthResponse` record (single `status` field), not a `Map`; per the amended principle, no Lombok is needed on a record (no boilerplate to reduce) and no MapStruct mapper is needed (no domain/entity source to translate from — research.md Decision). |
| IX. Centralised Validation and Error Handling | **Partial — deferred, documented below.** No `GlobalExceptionHandler`/`ErrorResponse` is added by this feature; Spring Boot's default error handling (not yet the constitution's full shared error shape) covers the unexpected-failure edge case (spec.md EC-003) for now. |
| X. Configuration Over Hard-Coding | **Pass (N/A in practice).** Nothing scenario-specific is hard-coded; there is no scenario to configure. |
| XI. Testability | **Pass, with two named exceptions.** Unit-level controller test, a `@SpringBootTest` smoke test, and a generated-OpenAPI contract check are all in scope (research.md). FR-006 and FR-009 are the exceptions: both are negative/absence requirements with no seam to assert against automatically, so they are verified by manual code review (tasks.md T020) rather than an automated test — this is documented in spec.md's FR-006/FR-009 parentheticals and repeated here for Constitution Check traceability (found by `/speckit-analyze`, finding C1). |
| XII. Simplicity and Maintainability | **Pass.** Only the two layer packages this feature actually needs (`controllers/`, `domain/`) are created — `services/`, `repository/`, `mappers/`, `entity/`, `exceptions/`, `filters/`, `config/` stay absent until a feature needs them. No service/repository/mapper is introduced for a single stateless literal response (spec.md IC-003 equivalent intent). |
| XIII. Automated Quality Gates | **Deferred, documented below.** CI pipeline, static analysis, dependency vulnerability scanning, and coverage reporting are repo-wide concerns with no existing setup to extend; out of scope for a single endpoint feature. This also covers the separately runnable unit/integration/functional/smoke suite split — deferred because this feature has only one test class's worth of "unit"-shaped coverage to split, not because `build.gradle` doesn't exist yet (see Complexity Tracking below for the full rationale). |
| XIV. Observability & Traceability | **Pass (health-endpoint exemption), with a narrower cross-cutting deferral noted below.** Principle XIV explicitly permits health-style endpoints to be exempted from correlation-ID and tracing instrumentation, scoped only to that endpoint class — this healthcheck endpoint invokes exactly that exemption, since it carries no business request to trace and performs no logging. This is a permanent, constitution-sanctioned exemption for *this* endpoint, not a temporary gap awaiting a future fix (found by `/speckit-analyze`, finding X1, which the prior wording had conflated with genuine deferrals). Shared structured logging infrastructure (Logback wiring, log-injection sanitisation) remains a genuine repo-wide deferral for other endpoints that do log — see Complexity Tracking below; that deferral does not obligate retrofitting correlation-ID handling onto this endpoint. |
| XV. Security by Design | **Pass.** The one security control this feature relaxes — no authentication on `GET /api/v1/healthcheck` (FR-011) — was made explicit and documented at design time, not retrofitted: it was resolved via `/speckit-clarify` and is recorded in spec.md's Clarifications section, before implementation began. Nothing else in this feature touches a security-relevant decision. |
| XVI. Object-Oriented Design Discipline | **N/A (trivially).** A single thin `@RestController` method returning a literal `record HealthResponse(String status)` has no class hierarchy to check against SOLID, no inheritance to prefer composition over, and no chained calls through a collaborator's internals to violate the Law of Demeter. |

**Non-principle constraint check**: Technology & Architecture Constraints also requires a checked-in `.editorconfig` (code style/formatting), which isn't tied to a numbered Core Principle above and so has no row of its own. **Pass** — created by tasks.md T005, unlike the IX/XIII deferrals below (and XIV's shared structured-logging sub-item), since it's a single static file with no dependency/build/CI cost, not repo-wide tooling. (Added 2026-09-18 in response to `/speckit-analyze` finding K1, which this requirement had slipped through in every prior amendment round for exactly this reason — no principle number to hang a table row off of.) Development Workflow & Quality Gates' transparency bullet ("work in the open") is likewise satisfied by default — this entire feature's spec, plan, research, and tasks live in this repository's `specs/` directory, not a private channel.

**Note on Principle II's Domain Driven Design tension** (added by the constitution's v1.7.0 amendment): this feature's package-by-layer structure (`controllers/`, `domain/`) is the deliberate choice the constitution now explicitly acknowledges sits in tension with HMCTS's general "design around the domain" guidance. Nothing to change here — a single stateless health value has no domain behaviour a DDD module boundary would protect.

**Result**: No blocking violations. Two principles (IX, XIII) are intentionally not (fully) implemented by this feature; Principle XIV is a **Pass** for this endpoint (permanent health-endpoint exemption), with only its shared structured-logging infrastructure sub-item deferred alongside them — see Complexity Tracking for the documented rationale required by Governance.

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
│   ├── java/uk/gov/hmcts/ctam/jo/
│   │   ├── Application.java                        # Spring Boot entry point
│   │   ├── controllers/
│   │   │   └── HealthcheckController.java           # GET /api/v1/healthcheck
│   │   ├── domain/
│   │   │   └── HealthResponse.java                  # record HealthResponse(String status)
│   │   └── exceptions/  [NOT CREATED — deferred, see Complexity Tracking]
│   │       ├── GlobalExceptionHandler.java  (future: @RestControllerAdvice, Principle IX)
│   │       └── ErrorResponse.java           (future: record ErrorResponse(String error, Instant timestamp, String traceId))
│   └── resources/
│       └── application.yml                          # server port, app name only
└── test/
    └── java/uk/gov/hmcts/ctam/jo/
        └── controllers/
            ├── HealthcheckControllerTest.java       # @WebMvcTest — 200 with {"status":"ok"}, non-GET rejected
            ├── HealthcheckSmokeTest.java             # @SpringBootTest — real HTTP round trip
            └── HealthcheckOpenApiContractTest.java   # asserts /v3/api-docs documents the endpoint per FR-010
```

**Structure Decision**: Single Gradle/Spring Boot project — there is no frontend and no second deployable, so a multi-module or web+backend split would be pure over-engineering (Principle XII). The codebase is organised **by layer** under base package `uk.gov.hmcts.ctam.jo` (constitution Principle II), not by feature — `controllers/` and `domain/` are the only layer packages this feature actually creates. `exceptions/` is shown above with its two future files named (`GlobalExceptionHandler.java`, `ErrorResponse.java`) precisely because Principle IX/XIV already fixed those names, but the package itself is **not created by this feature** — it's a preview of what a future foundation feature will add, not a task in tasks.md. `services/`, `repository/`, `mappers/`, `entity/`, `filters/`, and `config/` have no names fixed yet either and are simply absent, not previewed. This supersedes this feature's original package-by-feature (`healthcheck/`) structure under `uk.gov.hmcts.reform.ctam.jomockapi`, decided before the constitution named this convention.

## Complexity Tracking

> Documenting *deferred* constitution compliance, per Governance's requirement that any intentional deviation be explicit and agreed rather than silent.

| Deferred Principle | Why Deferred Here | Why Not Implemented Now |
|---|---|---|
| IX. Centralised Validation and Error Handling (shared error response shape — record `ErrorResponse(String error, Instant timestamp, String traceId)`, applied even to framework-level 404/405 responses, produced by a single `GlobalExceptionHandler` `@RestControllerAdvice` in `exceptions/`) | This requires the same correlation-ID/structured-logging foundation as Principle XIV below (`traceId` has nothing to read until a tracing filter populates it), plus the repo-wide `exceptions/` handler; neither exists yet, and this endpoint has no error branch of its own to build one around. No `exceptions/` package is created by this feature — it stays absent rather than being created empty, per Principle XII. | Building a one-off shared error shape as a side effect of "add a healthcheck endpoint" would be a de facto repo-wide decision made without being framed as one. Spring Boot's default error handling (including its default `405` for the non-`GET` case) minimally satisfies spec.md FR-009/EC-003 ("standard error-handling behaviour applies, not forced 200") for this endpoint in the meantime. **Recommendation**: build `GlobalExceptionHandler`/`ErrorResponse` as part of the same foundation feature recommended for XIII/XIV below, once a correlation-ID filter exists to populate `traceId`. |
| XIII. Automated Quality Gates (CI pipeline, static analysis, dependency scanning, coverage reporting, **and separately runnable unit/integration/functional/smoke test suites**) | These are repo-wide build/CI concerns, not something a single endpoint's controller code can express on its own; the repo currently has no build file or CI config to extend at all. **The unit/integration/functional/smoke suite split is the one exception worth calling out separately: T001 does create `build.gradle` from scratch, so the source-set/task wiring for this split could technically be added now** — it is deferred here because this feature has only one test class's worth of "unit"-shaped coverage and no integration/functional/smoke-shaped tests yet to justify separate suites, not because the build file doesn't exist. | Building full CI/quality tooling as a side effect of "add a healthcheck endpoint" would silently balloon this feature's scope (explicitly out of scope per spec.md §13 "unrelated infrastructure redesign") and produce tooling nobody has reviewed as its own decision. **Recommendation**: raise a dedicated "project build & CI foundation" feature to establish CI/static analysis/dependency scanning/coverage reporting *and* the unit/integration/functional/smoke Gradle source-set split, which every subsequent endpoint (including this one) then inherits. |
| XIV. Observability & Traceability — shared structured logging infrastructure (Logback wiring, log-injection sanitisation) only; correlation-ID/tracing for *this* endpoint is not deferred — it is permanently exempted under Principle XIV's own health-endpoint clause (see Constitution Check row above) | Shared structured logging is cross-cutting infrastructure meant to apply to every request/response and log line app-wide; this endpoint performs no logging at all, so there is nothing here to wire it into yet. | Building a one-off structured-logging setup as a side effect of "add a healthcheck endpoint" would be a de facto repo-wide decision made without being framed as one. **Recommendation**: build the shared Logback/log-injection-sanitisation setup as part of the same foundation feature recommended for XIII, before or alongside the next endpoint that actually produces meaningful log lines. This foundation feature does not need to add correlation-ID handling to the healthcheck endpoint itself — that exemption is permanent, per Principle XIV (found by `/speckit-analyze`, finding X1). |

All of these deferrals — IX and XIII in full, and XIV's shared structured-logging infrastructure sub-item only — are scoped to *cross-cutting infrastructure*, not to this endpoint's own correctness — the healthcheck endpoint itself fully satisfies every functional and non-functional requirement in spec.md without them.

## Post-Design Constitution Re-Check

Re-evaluated after Phase 1 (research.md, data-model.md, contracts/healthcheck.md, quickstart.md): no new dependency or layer was introduced beyond what the initial Constitution Check already assessed. `springdoc-openapi` is documentation tooling, not a business-logic component, so it doesn't change the Principle II/III/XII assessment. The `HealthResponse` record and no-service-layer decision (research.md) keep Principles VIII and XII passing exactly as before. No new violations; the same deferrals stand, unchanged — IX and XIII in full, and XIV's shared structured-logging infrastructure sub-item only (XIV's correlation-ID/tracing aspect remains a Pass for this endpoint).

**Re-evaluated again after constitution v1.2.0 → v1.6.0** (2026-09-18): Gradle, package-by-layer, the `{"status":"ok"}` response body, and the named `GlobalExceptionHandler`/`ErrorResponse` classes are now reflected throughout this plan, research.md, data-model.md, contracts/healthcheck.md, and quickstart.md (the latter two only where the deferred error-handler naming is a relevant forward-reference, not a build task). No principle assessment changed from Pass to Fail or vice versa — the updates were to *how* Principles II, VIII, and IX are satisfied/deferred, not *whether* they are. The IX/XIII deferrals, and XIV's shared structured-logging infrastructure sub-item, remain unchanged and still fully justified (XIV's correlation-ID/tracing aspect remains a Pass for this endpoint).

**Re-evaluated again after constitution v1.6.0 → v1.7.0** (2026-09-18, this revision): added Constitution Check rows for the two new principles — XV (Security by Design: **Pass**, FR-011's no-auth decision was already made explicit and documented via `/speckit-clarify` before implementation) and XVI (Object-Oriented Design Discipline: **N/A**, no class hierarchy exists to check). Also noted this feature satisfies the new transparency bullet by default, and that Principle II's new DDD-tension acknowledgement doesn't change this feature's package-by-layer structure. No principle assessment changed from Pass to Fail or vice versa.

**Re-evaluated again after constitution v1.7.0 → v1.8.0** (2026-09-18, in response to `/speckit-analyze` finding C1): Governance's deviation-approval clause was generalized from "Any intentional deviation from the API contract (Principle I)" to any MUST requirement in this constitution, and now requires a deferral (as opposed to a permanent, scope-justified exception) to state a path to eventual compliance. This closes a gap the prior wording left open: the Complexity Tracking table below defers two other MUST principles in full (IX, XIII) plus one narrowed sub-item of a third (XIV's shared structured-logging infrastructure only — XIV's correlation-ID/tracing aspect is a Pass, per its own health-endpoint exemption clause) via the same documented-rationale pattern Governance previously only named for Principle I. No change was needed to the table itself — each of its three rows already carries a "Recommendation" naming the future foundation feature that closes the deferral, which is exactly the "path to eventual compliance" the amended clause now requires. No principle assessment changed from Pass to Fail or vice versa.

**Re-evaluated again after constitution v1.8.0 → v1.8.1** (2026-09-18, in response to `/speckit-analyze` finding N1): the constitution's document title was renamed from "E-Links Mock API Constitution" to "JO Mock API Constitution" to match this project's actual identity (repo `ctam-jomockapi2`, base package `uk.gov.hmcts.ctam.jo`); no principle, constraint, or Governance text changed in substance. This feature's own "E-Links" references (spec.md Assumptions, research.md, contracts/healthcheck.md) correctly continue to name the real upstream API being mocked and were not touched. No re-assessment of any Constitution Check row was needed.
