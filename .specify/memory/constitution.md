<!--
Sync Impact Report
- Version change: 1.9.0 → 1.10.0
- Rationale: Added a new Core Principle (XVIII. API Contract and Swagger/OpenAPI
  Requirements), requiring every new REST API or service in this repository to
  provide and maintain a generated OpenAPI/Swagger specification for every
  implemented endpoint — path, method, parameters, headers, request/response
  schemas, status codes, validation constraints, and error responses, all matching
  the actual implementation and updated whenever the contract changes — with Swagger
  UI exposed wherever the framework supports it and generation reproducible as part
  of the build/runtime, not hand-maintained. This is a new principle, so this is a
  MINOR bump per the versioning policy below.
- Added principles: XVIII. API Contract and Swagger/OpenAPI Requirements
- Modified sections: Development Workflow & Quality Gates (added a bullet requiring a
  matching, regenerated OpenAPI/Swagger specification for any endpoint change,
  cross-referencing Principle XVIII — matching this section's existing pattern of
  citing the principle each quality gate enforces).
- Removed sections: none
- Downstream impact: `/speckit-tasks` MUST now generate explicit tasks for creating
  or updating the Swagger/OpenAPI definition for any feature that adds or changes an
  endpoint; `/speckit-implement` MUST complete API implementation and its
  Swagger/OpenAPI documentation together, not as separate passes; `/speckit-converge`
  MUST verify every implemented endpoint appears in the OpenAPI specification, that
  request/response schemas and documented status codes match actual behaviour, and
  that no undocumented endpoints have been introduced. This principle formalizes,
  repo-wide, the springdoc-based approach this project's first feature
  (001-healthcheck-api) already established ad hoc in its own research.md, and
  directly underpins Principle XVII's requirement that Postman collections stay
  aligned with "the API/OpenAPI specification" — that specification must now
  reliably exist for every endpoint. No existing principle, constraint, or
  governance clause changed in substance.
-->

# JO Mock API Constitution

## Core Principles

### I. Contract-First Development
The supplied Swagger/OpenAPI specification for E-Links is the single source of truth for
the mock API. Endpoint paths, HTTP methods, request/response parameters, response
structures, status codes, filtering behaviour, and pagination behaviour MUST match the
contract exactly. Implementation convenience MUST NOT be used as a justification for
altering the external contract. Any change to the mock's observable HTTP behaviour MUST
be traceable to a corresponding change in the source specification.
**Rationale**: Consumers integrate against the contract, not the implementation. Silent
drift between the mock and the contract breaks the mock's purpose — a reliable stand-in
for the real E-Links API.

### II. Java Spring Boot Architecture
The service MUST be implemented in Java 25 using Spring Boot 4.1.1, built with Gradle (via
the Gradle wrapper), with a component-centric architecture that keeps responsibilities
clearly separated. The codebase MUST be organised by layer, under a single HMCTS-style base
package (the pattern `uk.gov.hmcts.<domain-code>` — e.g. `uk.gov.hmcts.cp` in a surveyed
sibling HMCTS Spring Boot service), with top-level layer packages such as `controllers/`,
`services/`, `repository/`, `mappers/`, `domain/` (DTOs), `entity/`, `exceptions/`, `filters/`,
and `config/`, rather than a package-per-feature structure. Controllers MUST remain thin,
handling only HTTP concerns (request binding, status codes, response shaping). Business
logic MUST live in reusable services/components, not controllers. Dedicated providers,
mappers, validators, and utilities MUST be used where a responsibility is distinct enough
to warrant its own component. The global exception handler required by Principle IX MUST
live in the `exceptions/` layer package as a single `@RestControllerAdvice` class,
conventionally named `GlobalExceptionHandler`, not scattered per-controller try/catch
logic. Health- and readiness-style endpoints MUST
return a small JSON body carrying a `status` field (e.g. `{"status": "ok"}`) rather than
an empty response, and MUST NOT be assumed to be served by Spring Boot Actuator's
`/actuator/health` merely because the actuator dependency happens to be present — a
hand-rolled controller returning this shape is the established convention.
**Rationale**: A consistent, layered structure — matching the convention surveyed from a
sibling HMCTS Spring Boot service — keeps the codebase navigable as
endpoint coverage grows, prevents business logic from becoming entangled with transport
concerns, and lets contributors move between HMCTS Spring Boot services without relearning
project layout. Naming the health-endpoint response shape and the exception handler's
location closes the two gaps most likely to be reinvented differently by each new
endpoint if left unstated. This package-by-layer structure sits in some tension with
HMCTS's general engineering guidance to "design around the domain" (Domain Driven
Design) — that tension is acknowledged, not accidental: this mock's services are thin
and largely stateless (see Principle IV), with no rich domain behaviour a DDD module
boundary would meaningfully protect, and the layered structure is what the real
surveyed sibling repository actually uses. Revisit if a future endpoint's domain logic
grows complex enough that layer packages start hiding more than they clarify.

### III. Reuse Before Duplication
Common behaviour MUST be implemented once and reused across endpoints. Duplicated logic
across controllers or services is a defect, not a style preference. Shared components
MUST be used for pagination, filtering, validation, mapping, error handling, synthetic
data generation, and reference-data handling.
**Rationale**: E-Links exposes many structurally similar endpoints; without enforced
reuse, the codebase would accumulate near-duplicate implementations that drift apart and
multiply maintenance cost and bug surface.

### IV. Synthetic Data Only
The API MUST serve only synthetic/mock data. Production data or real personal data MUST
NEVER be used, stored, or referenced. Synthetic data MUST be realistic and internally
consistent, and relationships between people, appointments, roles, jurisdictions,
tickets, authorisations, locations, and reference data MUST remain coherent (e.g., an
appointment MUST reference a person and jurisdiction that actually exist in the dataset).
**Rationale**: This is a mock service; using or risking exposure of real personal data
would defeat its purpose and create unacceptable compliance risk. Coherent synthetic data
also makes the mock genuinely useful for downstream integration testing.

### V. Deterministic Behaviour
The same request against the same configured dataset MUST return the same result every
time. Runtime randomness that makes responses or test outcomes unpredictable MUST be
avoided. Data generation MUST use fixed fixtures or seeded, deterministic generation
strategies.
**Rationale**: Determinism is what makes the mock trustworthy for automated testing and
reproducible bug investigation. A mock that returns different data on each call is worse
than no mock.

### VI. Behavioural Fidelity
Request parameters MUST have real, observable effects on responses; parameters MUST NOT
be accepted and silently ignored. Filtering parameters defined by the contract — including
`updated_since`, `left_since`, and `deleted_since` — MUST be implemented with genuine
filtering logic against the dataset. Pagination MUST be implemented consistently through
a single reusable pagination component across all paginated endpoints.
**Rationale**: A mock that accepts parameters without honouring them gives consumers false
confidence and hides integration bugs that would surface against the real API.

### VII. Generic Reference-Data Handling
Reference-data endpoints MUST be served through a common, reusable mechanism rather than
bespoke per-type implementations. Adding a new reference-data type MUST NOT require
duplicating endpoint logic. Deprecated aliases defined by the API contract MUST be
resolved centrally, in one place, rather than handled ad hoc per endpoint.
**Rationale**: Reference data is structurally uniform across many types; a generic
mechanism avoids the combinatorial duplication that would otherwise accompany each new
reference-data type, and centralises the one piece of legacy complexity (deprecated
aliases) that would otherwise leak into every consumer.

### VIII. Typed API Models
Endpoints MUST use explicit request and response DTOs derived from the API contract.
Generic `Map<String, Object>`-style structures MUST NOT be used where the schema is known.
Internal domain models MUST be kept separate from external API DTOs where the two diverge,
with reusable mappers translating between them. Lombok MUST be used, consistently across
the codebase rather than ad hoc per class, to reduce DTO/entity boilerplate (getters,
setters, `equals`/`hashCode`, and similar). MapStruct MUST be used as the mapping-library
convention for the reusable mappers this principle already requires, rather than
hand-written or ad hoc mapping code.
**Rationale**: Typed models make the contract enforceable by the compiler, catch schema
drift early, and make mapping logic inspectable and testable instead of implicit. Naming
Lombok and MapStruct as the established tools — rather than leaving "reduce boilerplate"
and "reusable mappers" unnamed — is what keeps every DTO/entity/mapper in the codebase
looking the same regardless of who wrote it.

### IX. Centralised Validation and Error Handling
Validation rules and error responses MUST be consistent across all endpoints. Spring
Boot's global exception handling MUST be used for cross-cutting error behaviour rather
than per-controller try/catch logic (Principle II names where this handler lives). Where
authentication is simulated, it MUST be centralised and configurable rather than
duplicated per endpoint. Every error response, regardless of status code, MUST use one
shared error response shape — at minimum: a human-readable message in a field named
`error`, a timestamp, and the request's correlation/trace identifier (per Principle
XIV) — conventionally a record such as `ErrorResponse(String error, Instant timestamp,
String traceId)` in the `exceptions/` layer package, produced by the single
`GlobalExceptionHandler` (Principle II) — rather than each endpoint or exception handler
inventing its own body. This shared shape MUST be applied to every error response the
application produces, including framework-level responses (e.g. 404 not-found, 405
method-not-allowed), not only exceptions explicitly raised by application code. Log
severity MUST reflect the failure: client errors (4xx) MUST be logged as warnings,
server errors (5xx) MUST be logged as errors.
**Rationale**: Inconsistent error shapes or per-endpoint validation logic make the mock
unpredictable for consumers and multiply the places a fix must be applied. One shared
error shape lets consumers write a single error-handling path against the mock, matching
how they would integrate against the real API; severity-appropriate logging keeps log
volume a meaningful signal of operational risk rather than noise.

### X. Configuration Over Hard-Coding
Mock scenarios, dataset size, fixtures, authentication behaviour, and error scenarios
MUST be externally configurable. Environment-specific behaviour MUST NOT be hard-coded
into controllers or services.
**Rationale**: Consumers and testers need to exercise different scenarios (empty
datasets, error injection, large pages) without code changes; configuration is what makes
the mock adaptable to their needs.

### XI. Testability
All significant behaviour MUST be covered by automated tests. This MUST include unit
tests for reusable components and service logic, controller/API tests for endpoint
behaviour, and contract tests verifying compatibility with the Swagger/OpenAPI
specification.
**Rationale**: Automated coverage at each layer is what lets the team change the
implementation with confidence while Principle I (contract fidelity) and Principle V
(determinism) remain provably true, not just assumed.

### XII. Simplicity and Maintainability
Clear, maintainable solutions MUST be preferred over unnecessary abstraction.
Abstraction MUST only be introduced where responsibilities are genuinely shared across
multiple call sites — not speculatively. The solution MUST remain easy to extend, test,
and understand for a new contributor.
**Rationale**: This project's value is its reliability as a stand-in for a real API;
over-engineered abstraction increases the cost of verifying that reliability without a
corresponding benefit.

### XIII. Automated Quality Gates
Every build MUST treat compiler warnings as errors (e.g. a `-Werror`-equivalent compiler
flag). Static analysis (Checkstyle, plus a code-quality platform such as SonarQube) and
dependency vulnerability scanning (e.g. OWASP dependency-check) MUST run as part of the
build — typically wired together via a single shared build-tool plugin rather than each
configured as a fully separate, ad hoc tool — with any accepted vulnerability finding
recorded in a checked-in suppression file at a conventional, discoverable path (e.g.
`config/owasp/`), with a documented reason, never silently ignored. Dependency freshness
MUST be checked by an automated dependency-update-checking plugin as part of, or
alongside, the same build, rather than tracked manually. Automated tests MUST be organised
into separately runnable suites providing at least the coverage required by Principle
XI — following the reference convention, this means at minimum `unit`, `integration`,
`functional`, and `smoke` suites — with a measured code-coverage report (e.g. JaCoCo)
generated on every build. Continuous integration (e.g. GitHub Actions) MUST run the full
build, every test suite, and every quality check — typically via a single build-tool
target such as Gradle's `check` — on each pull request and each change to the main
branch; a change MUST NOT be merged with a failing or skipped quality gate. A separate
static application security testing (SAST) scan (e.g. CodeQL) MUST run independently of
the main build pipeline, both on a recurring schedule and on pull requests/pushes.
**Rationale**: Manual enforcement of code quality and dependency hygiene does not scale
and erodes under time pressure; automated, CI-enforced gates are what actually keep the
mock reliable and free of known vulnerabilities as endpoint coverage grows. Naming
concrete tooling (rather than "a linter", generically) is what let this survive contact
with a real sibling implementation without drifting from it.

### XIV. Observability & Traceability
Every request MUST be assigned a correlation/trace identifier, via a conventional
`X-Correlation-Id` header: read from that inbound header when the caller supplies one,
otherwise generated fresh (e.g. a UUID), and held for the duration of the request in a
conventional MDC key (`correlationId`). This identifier MUST appear in every log line
produced while handling that request, in every error response body (Principle IX), and
MUST be echoed back to the caller in a response header. Health, readiness, info, and
metrics-style endpoints MAY be exempted from correlation-ID and tracing instrumentation,
since they carry no business request to trace and are typically polled at high
frequency without downstream effect; any such exemption MUST be scoped only to that
class of endpoint, not used as precedent to exempt endpoints more broadly. All
application logging MUST go through one shared, structured logging mechanism — Logback
as the logging implementation, bridging any log4j-style API into it, and reusing a
shared internal logging library where one already exists rather than each service
reinventing structured logging — instead of ad hoc or scattered loggers. Any
caller-supplied value written to a log line MUST be sanitised first, via an
output-encoding library (e.g. OWASP Java Encoder), to prevent log injection.
**Rationale**: A mock used for integration testing is only as useful as its
debuggability — a correlation ID is what lets a consumer's reported request be found in
logs; sanitised, structured logging keeps that debugging trustworthy rather than
corruptible by malformed or malicious input. Naming the concrete header and MDC key keeps
every service in this family interoperable with the same tracing convention. The
health/info/metrics exemption is a deliberate, named exception — those endpoints have no
business request to correlate, and instrumenting them adds overhead to code paths that
exist specifically to be cheap and frequently polled.

### XV. Security by Design
Security implications MUST be considered at design time for every new endpoint,
component, or architectural decision, not retrofitted after implementation. Where a
security control is deliberately relaxed or omitted (e.g., an unauthenticated endpoint,
per Principle II's health-endpoint exemption), that decision MUST be explicit and
documented in the relevant spec or plan and agreed before implementation proceeds — the
same standard Governance already requires for API contract deviations (Principle I) —
rather than being an unstated oversight. The concrete controls this principle's design
review surfaces are implemented elsewhere in this constitution: dependency
vulnerability scanning and centralised, configurable authentication (Principles IX,
XIII), and log-injection sanitisation (Principle XIV).
**Rationale**: HMCTS's own engineering guidance states security must be "baked in, not
bolted on" — retrofitted security is more expensive and more likely to miss systemic
issues than security considered from the outset. For a mock standing in for a real
government service, even a deliberately relaxed security posture (this mock's very
first endpoint is unauthenticated) must be a conscious, traceable decision, not an
accident of what nobody thought to add.

### XVI. Object-Oriented Design Discipline
Object-oriented code MUST follow the SOLID principles (Single Responsibility,
Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion).
Composition MUST be preferred over inheritance for code reuse; inheritance is reserved
for genuine is-a relationships, not for sharing behaviour between otherwise-unrelated
classes. Classes and methods MUST talk only to their immediate collaborators, not reach
through an object to manipulate objects it merely holds a reference to (the Law of
Demeter) — e.g., a controller MUST NOT chain calls through a service's returned object
to reach a third object's internals.
**Rationale**: These are HMCTS's own stated object-oriented coding standards. Naming
them explicitly, rather than leaving "well-designed OO code" as an unstated expectation,
gives the peer review Development Workflow & Quality Gates already requires a concrete
standard to check new code against.

### XVII. API Testability and Postman Artifacts
For every new API, REST service, or externally callable endpoint implemented in this
repository, the implementation MUST include corresponding Postman artifacts, delivered
alongside the service code in the same feature rather than deferred to a later pass. A
Postman Collection MUST contain a request for every newly implemented endpoint, with
the correct HTTP method, URL, headers, request body, query parameters, and path
parameters. Configurable values — including `baseUrl`, authentication tokens, tenant
IDs, client IDs, and other environment-specific configuration — MUST be expressed as
Postman environment variables, never hard-coded into requests. Collections MUST
include representative sample requests for successful scenarios and, where
applicable, requests covering significant error scenarios. Every request MUST carry
Postman test scripts validating the expected HTTP status code, response structure,
mandatory response fields, and any important business assertions. The Postman
collection MUST be kept aligned with the API/OpenAPI specification and MUST be
updated whenever an existing endpoint is modified — a stale collection is a defect,
not a footnote. Postman artifacts MUST be stored in a repository-controlled location,
conventionally `/postman/`, following the naming pattern
`/postman/<service-name>.postman_collection.json` and
`/postman/<service-name>.postman_environment.json`. A service implementation is NOT
considered complete until its corresponding Postman collection has been created or
updated and can successfully exercise the implemented API. `/speckit-tasks` MUST
generate explicit tasks for creating or updating the Postman collection for any
feature that adds or changes an endpoint; `/speckit-implement` MUST implement those
Postman artifacts alongside the service; `/speckit-converge` MUST check that every
implemented API endpoint is represented in the Postman collection and that the
collection remains consistent with the API specification, raising a finding wherever
it does not.
**Rationale**: A Postman collection is the fastest way for a human integrator,
tester, or another team to actually exercise a newly built endpoint without reading
source code first — matching this project's purpose as a stand-in API other teams
integrate against. Environment variables keep collections portable across
local/dev/staging without hand-editing requests, and embedded test scripts turn
manual exploration into a repeatable regression check. Binding Postman delivery into
every Spec Kit phase — tasks, implement, and converge — is what prevents "add a
collection later" from quietly becoming "never," the way ad hoc testing conventions
usually drift.

### XVIII. API Contract and Swagger/OpenAPI Requirements
Every new REST API or service in this repository MUST provide and maintain a
generated OpenAPI/Swagger specification, created or updated for every implemented
endpoint as part of the same feature — not deferred to a later pass. Each endpoint's
specification MUST define its path, HTTP method, request parameters, request
headers, request body schema, response schemas, HTTP status codes, validation
constraints, and error responses. Endpoints, request fields, and response fields
MUST carry meaningful descriptions, and representative request/response examples
MUST be included where appropriate. The OpenAPI specification MUST match the actual
implementation and MUST be updated whenever an existing API contract changes — an
undocumented endpoint is an incomplete implementation, not a follow-up item. Swagger
UI MUST be exposed wherever the service framework supports it, and Swagger/OpenAPI
generation MUST be reproducible as part of the application build or runtime
configuration, not a hand-maintained file that can drift from the code (per this
project's springdoc-based approach, established in specs/001-healthcheck-api's
research.md). The generated OpenAPI document SHOULD be available at a standard
location such as `/v3/api-docs`, and Swagger UI SHOULD be available through a
standard route such as `/swagger-ui.html` or the framework-equivalent route.
`/speckit-tasks` MUST generate explicit tasks for creating or updating the
Swagger/OpenAPI definition for any feature that adds or changes an endpoint;
`/speckit-implement` MUST complete API implementation and its Swagger/OpenAPI
documentation together, not as separate passes; `/speckit-converge` MUST verify
that every implemented endpoint appears in the OpenAPI specification, that
request/response schemas and documented HTTP status codes match actual behaviour,
and that no undocumented endpoints have been introduced.
**Rationale**: Consumers of this mock — and this project's own Postman collections,
required to stay aligned with "the API/OpenAPI specification" by Principle XVII —
depend on that specification being both present and accurate; a spec that drifts
from the code is worse than no spec at all, since it actively misleads integrators
rather than merely leaving them uninformed. Requiring reproducible, build-time
generation rather than a hand-maintained file is what keeps the two from diverging
as endpoints are added or changed, and matches Principle I's contract-fidelity
concern applied to this mock's own published documentation.

## Technology & Architecture Constraints

- Primary language, framework, and build tool: Java 25 with Spring Boot 4.1.1, built with
  Gradle (per Principle II).
- Layering: a package-by-layer structure (`controllers/` → `services/` →
  `repository/`/`mappers/`/`domain/`/`entity/`, with `exceptions/`, `filters/`, and
  `config/` alongside), with no layer skipping business logic into controllers
  (Principle II).
- Shared infrastructure (per Principle III) MUST exist for: pagination, filtering,
  validation, DTO mapping, global error handling, synthetic data generation,
  reference-data resolution (including deprecated alias handling), correlation-ID
  propagation, and structured logging (Principle XIV).
- Data models: contract-derived DTOs at the API boundary (Principle VIII); internal
  domain/entity models MAY differ from DTOs where that separation adds clarity, mapped
  via reusable mappers.
- All environment- and scenario-specific behaviour (dataset size, error injection,
  simulated auth) MUST be driven by externalised configuration (Principle X), not
  conditional code paths hard-coded per environment.
- Build tooling MUST enforce compiler warnings-as-errors, Checkstyle and SonarQube static
  analysis, OWASP dependency-check vulnerability scanning (suppression file at a
  conventional path, e.g. `config/owasp/`), automated dependency-freshness checking, and
  JaCoCo code-coverage reporting on every build, with a separate CodeQL SAST scan
  (Principle XIII).
- Code style/formatting MUST be enforced via a checked-in `.editorconfig` (or
  equivalent) rather than left to individual contributor preference — conventionally
  narrower indentation for config-style files, wider indentation for Java source, and a
  generous but bounded max line length (on the order of 120 characters) to keep diffs
  and reviews readable.

## Development Workflow & Quality Gates

- Any endpoint or behaviour change MUST be checked against the Swagger/OpenAPI
  specification before being considered complete; deviations MUST be explicitly
  documented and agreed (see Governance).
- New or changed endpoint behaviour MUST land with corresponding unit, controller/API,
  and contract test coverage (Principle XI) — implementation without tests is not
  considered done.
- New or changed endpoint behaviour MUST land with a created or updated Postman
  collection exercising it (Principle XVII) — a service is not considered done until
  its Postman artifacts can successfully call the implemented API.
- New or changed endpoint behaviour MUST land with a matching, regenerated
  OpenAPI/Swagger specification (Principle XVIII) — an undocumented endpoint is not
  considered done.
- Reviews MUST check for duplicated logic that should instead use or extend an existing
  shared component (Principle III) before approving new endpoint-specific logic.
- Reviews MUST reject use of untyped `Map<String, Object>` response/request handling
  where a contract schema exists (Principle VIII).
- Every pull request MUST pass the automated build, full test suite, and all quality
  gates in CI (Principle XIII) before it may be merged; CI failures MUST NOT be bypassed.
- Pull requests MUST confirm tests and documentation were updated where relevant, and
  MUST be reviewed before merge.
- Dependency updates MUST be tracked and applied regularly rather than left stale
  (Principle XIII).
- Specifications, plans, task breakdowns, and the decisions behind them MUST be kept in
  this repository (specs/, this constitution, PR descriptions) rather than in private
  channels — working in the open, per HMCTS's transparency principle — so any
  contributor can reconstruct why a decision was made without asking.

## Governance

This constitution takes precedence over implementation convenience and over any other
project practice or guidance document where they conflict. It is mandatory for all
future specifications, plans, tasks, and implementation work produced for this project.

**Amendment procedure**: Amendments are made by editing this file. Each amendment MUST
update the Sync Impact Report at the top of the file, bump the version per the versioning
policy below, and update the Last Amended date. Amendments that remove or redefine a
principle MUST state the rationale for the change.

**Versioning policy**: This constitution follows semantic versioning:
- MAJOR: backward-incompatible governance changes, or removal/redefinition of a principle.
- MINOR: a new principle or section added, or materially expanded guidance.
- PATCH: wording clarifications, typo fixes, or non-semantic refinements.

**Compliance review**: All specifications, plans, and task breakdowns produced via the
Spec Kit workflow MUST be checked against these principles before implementation begins.
Any intentional deviation from a MUST requirement in this constitution — including but
not limited to the API contract (Principle I) — MUST be explicitly documented in the
relevant spec or plan (e.g., a plan's Complexity Tracking section or equivalent) and
agreed before implementation proceeds; it MUST NOT be introduced silently during coding.
Where the deviation is a deferral rather than a permanent, scope-justified exception, the
documentation MUST state a path to eventual compliance (e.g., a named future feature that
will close it), not merely a reason the gap exists today.

**Version**: 1.10.0 | **Ratified**: 2026-09-08 | **Last Amended**: 2026-09-22
