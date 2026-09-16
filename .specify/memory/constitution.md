<!--
Sync Impact Report
- Version change: 1.0.0 → 1.1.0
- Rationale: Adopted quality/observability conventions surveyed from a sibling HMCTS
  Spring Boot service repository (service-api-marketplace) that this project's original
  constitution did not yet address: a shared error response shape, request tracing,
  log-injection prevention, and automated build/CI quality gates. No existing principle
  was removed or redefined, so this is a MINOR bump.
- Modified principles:
  - IX. Centralised Validation and Error Handling — expanded to require one shared error
    response shape (message + correlation/trace identifier) and severity-appropriate
    logging (4xx warn / 5xx error), instead of leaving error body shape unspecified.
- Added principles:
  - XIII. Automated Quality Gates (compiler warnings-as-errors, static analysis,
    dependency vulnerability scanning with a documented suppression file, layered
    automated test suites with coverage reporting, CI-enforced on every change)
  - XIV. Observability & Traceability (correlation/trace ID propagation, shared
    structured logging, log-injection prevention)
- Added sections: none (existing Technology & Architecture Constraints and Development
  Workflow & Quality Gates sections were expanded in place, not replaced)
- Removed sections: none
- Deferred (explicitly out of scope for this amendment, noted for future review): a
  persistence layer/database, containerisation/Helm deployment, and real
  secret-management infrastructure were surveyed from the same sibling repository but
  intentionally NOT adopted, since this project has no database and no deployment target
  yet — revisit if/when either becomes true.
-->

# E-Links Mock API Constitution

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
The service MUST be implemented in Java using Spring Boot, with a component-centric
architecture that keeps responsibilities clearly separated. Controllers MUST remain thin,
handling only HTTP concerns (request binding, status codes, response shaping). Business
logic MUST live in reusable services/components, not controllers. Dedicated providers,
mappers, validators, and utilities MUST be used where a responsibility is distinct enough
to warrant its own component.
**Rationale**: A consistent, layered structure keeps the codebase navigable as endpoint
coverage grows and prevents business logic from becoming entangled with transport
concerns.

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
with reusable mappers translating between them.
**Rationale**: Typed models make the contract enforceable by the compiler, catch schema
drift early, and make mapping logic inspectable and testable instead of implicit.

### IX. Centralised Validation and Error Handling
Validation rules and error responses MUST be consistent across all endpoints. Spring
Boot's global exception handling MUST be used for cross-cutting error behaviour rather
than per-controller try/catch logic. Where authentication is simulated, it MUST be
centralised and configurable rather than duplicated per endpoint. Every error response,
regardless of status code, MUST use one shared error response shape (at minimum: a
human-readable message and the request's correlation/trace identifier, per Principle
XIV) rather than each endpoint or exception handler inventing its own body. Log severity
MUST reflect the failure: client errors (4xx) MUST be logged as warnings, server errors
(5xx) MUST be logged as errors.
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
Every build MUST treat compiler warnings as errors. Static analysis (e.g., a
Checkstyle-equivalent linter) and dependency vulnerability scanning MUST run as part of
the build; any accepted vulnerability finding MUST be recorded in a checked-in
suppression file with a documented reason, never silently ignored. Automated tests MUST
be organised into separately runnable suites — at minimum unit, controller/API, and
contract (Principle XI) — with a measured code-coverage report generated on every build.
Continuous integration MUST run the full build, every test suite, and every quality
check on each pull request and each change to the main branch; a change MUST NOT be
merged with a failing or skipped quality gate.
**Rationale**: Manual enforcement of code quality and dependency hygiene does not scale
and erodes under time pressure; automated, CI-enforced gates are what actually keep the
mock reliable and free of known vulnerabilities as endpoint coverage grows.

### XIV. Observability & Traceability
Every request MUST be assigned a correlation/trace identifier: read from an inbound
correlation header when the caller supplies one, otherwise generated fresh. This
identifier MUST appear in every log line produced while handling that request, in every
error response body (Principle IX), and MUST be echoed back to the caller in a response
header. All application logging MUST go through one shared, structured logging mechanism
rather than ad hoc or scattered loggers, and any caller-supplied value written to a log
line MUST be sanitised first to prevent log injection.
**Rationale**: A mock used for integration testing is only as useful as its
debuggability — a correlation ID is what lets a consumer's reported request be found in
logs; sanitised, structured logging keeps that debugging trustworthy rather than
corruptible by malformed or malicious input.

## Technology & Architecture Constraints

- Primary language and framework: Java with Spring Boot (per Principle II).
- Layering: controller → service/component → provider/mapper/validator/utility, with no
  layer skipping business logic into controllers.
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
- Build tooling MUST enforce compiler warnings-as-errors, static analysis, dependency
  vulnerability scanning, and code-coverage reporting on every build (Principle XIII).

## Development Workflow & Quality Gates

- Any endpoint or behaviour change MUST be checked against the Swagger/OpenAPI
  specification before being considered complete; deviations MUST be explicitly
  documented and agreed (see Governance).
- New or changed endpoint behaviour MUST land with corresponding unit, controller/API,
  and contract test coverage (Principle XI) — implementation without tests is not
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
Any intentional deviation from the API contract (Principle I) MUST be explicitly
documented in the relevant spec or plan and agreed before implementation proceeds — it
MUST NOT be introduced silently during coding.

**Version**: 1.1.0 | **Ratified**: 2026-09-08 | **Last Amended**: 2026-09-15
