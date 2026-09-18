# Phase 0 Research: Healthcheck API

## Decision: Java 25 + Spring Boot 4.1.1 + Gradle

**Rationale**: Constitution Principle II mandates Java 25, Spring Boot 4.1.1, and Gradle explicitly (amended after this feature's initial planning, which had incorrectly inferred Maven — corrected here). None of these are discretionary choices; they are fixed constitutional requirements. The repository has no build file yet, so this feature stands up the Gradle wrapper (`gradlew`) from scratch.

**Alternatives considered**: None — all three are pinned by Principle II, not evaluated as options.

## Decision: Base package `uk.gov.hmcts.elinks`, organised by layer

**Rationale**: Constitution Principle II now mandates an HMCTS-style base package following the pattern `uk.gov.hmcts.<domain-code>`, organised by layer (`controllers/`, `services/`, `repository/`, `mappers/`, `domain/`, `entity/`, `exceptions/`, `filters/`, `config/`) rather than by feature. `elinks` is the domain code for this mock (the E-Links API). This supersedes this feature's original `uk.gov.hmcts.reform.ctam.jomockapi` / package-by-feature (`healthcheck/`) decision, made before the constitution was amended to specify this.

**Alternatives considered**: Retaining package-by-feature — rejected; it's no longer just a style preference, Principle II now requires layer packages. Only the layers this feature actually needs are created (`controllers/`, `domain/`) — `services/`, `repository/`, `mappers/`, `entity/`, `exceptions/`, `filters/`, `config/` stay absent until a feature actually needs them (Principle XII: no speculative structure).

## Decision: JSON response body `{"status": "ok"}`, via a `HealthResponse` record

**Rationale**: Constitution Principle II now states the health/readiness-endpoint response convention explicitly: a JSON body with a `status` field, not an empty response, and not assumed to be Spring Boot Actuator's `/actuator/health`. This supersedes this feature's original "no response body" decision, which was made before that convention existed in the constitution and reasoned from "no convention exists in this greenfield repo" — a convention now does exist, at the constitution level.

This does **not** conflict with Principle I (Contract-First): the Swagger/OpenAPI contract for `GET /api/v5/healthcheck` specifies `200` and the description "Service is healthy" but is silent on response body/schema (spec.md's own Assumptions). Principle I requires fidelity to what the contract *does* specify (path, method, status code — all unchanged here); it does not forbid adding detail in the space the contract leaves undefined. Principle II's convention fills exactly that gap, so no contract deviation is introduced and none needs to be documented under Governance's deviation-approval process.

**Alternatives considered**: Empty body (the original decision) — superseded by the constitution amendment. `{"status": "UP"}` (Spring Boot Actuator's convention, using upper-case) — rejected in favour of matching the constitution's own stated example (`{"status": "ok"}`) exactly, for consistency with the convention as codified.

## Decision: A `HealthResponse` record in `domain/`; still no service/mapper layer

**Rationale**: The response body is now a value, not nothing, so a typed model is required by Principle VIII ("Endpoints MUST use explicit request and response DTOs... Generic `Map<String, Object>`-style structures MUST NOT be used"). A single-field Java `record HealthResponse(String status)` satisfies this with no boilerplate to reduce — records already generate `equals`/`hashCode`/accessors/`toString` natively, so Lombok (named by Principle VIII for DTO/entity boilerplate) has nothing to add here. No mapper is needed either: the controller constructs the literal constant `new HealthResponse("ok")` directly — there is no domain/entity representation to translate from, so MapStruct (named by Principle VIII for the "reusable mappers" it already required) has nothing to map. No service class is introduced, per Principle XII (Simplicity) and this feature's original reasoning — a single literal response has no behaviour to extract into a service.

**Alternatives considered**: A `HealthcheckService` returning the constant — rejected as pure indirection with no behaviour to hide or reuse, unchanged from the original decision.

## Decision: No authentication (unauthenticated endpoint)

**Rationale**: Resolved via `/speckit-clarify` (see spec.md Clarifications) — public, no-auth access, scoped only to this endpoint. No Spring Security dependency is introduced by this feature since none exists yet and none is needed to satisfy this decision. Unaffected by the constitution amendments.

**Alternatives considered**: Already evaluated during clarification (see spec.md).

## Decision: OpenAPI documentation via `springdoc-openapi-starter-webmvc-ui`

**Rationale**: Satisfies FR-010 by generating the Swagger/OpenAPI document from annotations on the controller method (`@Operation`, `@ApiResponse`) and the `HealthResponse` record (as the declared response type) rather than hand-maintaining a separate spec file that can drift from the code (reinforces Principle I, contract fidelity, for this and every future endpoint). Unaffected by the constitution amendments beyond the response now having a real schema to generate.

**Alternatives considered**: Hand-written static OpenAPI YAML — rejected; guarantees drift risk between documented and actual behaviour as more endpoints are added, which is exactly what Principle I exists to prevent.

## Decision: Test strategy — controller test + smoke test + generated-docs contract check

**Rationale**: Constitution Principle XI requires unit, controller/API, and contract test coverage. For an endpoint with no business logic, "unit" and "controller" coverage collapse into one `@WebMvcTest`/MockMvc test class (success `200` with `{"status":"ok"}` body, no-input success, non-`GET` method rejected — spec.md AC-001, AC-002, AC-003, SC-005). A `@SpringBootTest` smoke test proves the full application context wires the endpoint end-to-end. "Contract" coverage is satisfied by asserting the generated `/v3/api-docs` payload documents `GET /api/v5/healthcheck` → `200` "Service is healthy" with the `status` field schema (FR-010, AC-006), since no separate machine-readable source contract file exists in this repo to diff against (only the binary `joh-elinks-api/swagger-ui-elinks-api-v5.pdf`).

**Alternatives considered**: A binary diff or text-extraction comparison against the PDF — rejected as brittle and disproportionate for one endpoint; revisit if/when a machine-readable source spec is added to the repo.

## Resolved: No outstanding `NEEDS CLARIFICATION` markers

All Technical Context fields above are resolved. No unknowns remain for Phase 1.
