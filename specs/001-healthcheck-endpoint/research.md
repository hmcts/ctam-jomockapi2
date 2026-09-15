# Phase 0 Research: Healthcheck Endpoint

**Feature**: `001-healthcheck-endpoint` | **Date**: 2026-09-08

This is the first feature implemented in this repository, so research covers both the
narrow endpoint behaviour and the minimal project bootstrap decisions needed to serve it,
per the constitution's Java/Spring Boot mandate (Principle II).

## Decision: Language & runtime baseline

- **Decision**: Java 25 (LTS), pinned per project direction.
- **Rationale**: Java 25 is the project's explicitly chosen LTS baseline. It satisfies
  the constitution's general Java/Spring Boot mandate (Principle II) and is required by
  the project's chosen Spring Boot 4.1.1 release line (see below).
- **Alternatives considered**: Java 21 (also LTS) — this was the initial default before
  the project's target stack was pinned to Java 25 / Spring Boot 4.1.1; superseded, not
  rejected on technical grounds.

## Decision: Framework & build tool

- **Decision**: Spring Boot 4.1.1, pinned per project direction, with Maven as the build
  tool, using `spring-boot-starter-web`.
- **Rationale**: Spring Boot is mandated by Principle II, and 4.1.1 is the project's
  explicitly chosen release. Maven is chosen over Gradle as the more common default for
  enterprise Java/Spring Boot services and keeps the build declarative and easy to
  reason about; no project-specific reason favors Gradle. Only
  `spring-boot-starter-web` is needed for this feature — no persistence, security, or
  reference-data starters are required yet since the healthcheck endpoint has no data or
  auth dependency (FR-002, FR-003).
- **Alternatives considered**: Spring Boot 3.x — this was the initial default before the
  project's target stack was pinned; superseded, not rejected on technical grounds.
  Gradle (viable, no material advantage for this project); a minimal non-Spring HTTP
  server (rejected — violates Principle II directly).

## Decision: Testing stack

- **Decision**: JUnit 5 + `spring-boot-starter-test`, using Spring's `MockMvc` for
  controller/API-level tests and a plain unit test for any reusable component this
  feature introduces.
- **Rationale**: This is the standard, idiomatic Spring Boot testing stack and directly
  satisfies Principle XI (unit tests for reusable components, controller/API tests for
  endpoint behaviour). `MockMvc` allows asserting the exact HTTP status and empty body
  without standing up a real server, keeping tests fast and deterministic (Principle V).
- **Alternatives considered**: RestAssured against a running server (heavier, unnecessary
  for a single stateless endpoint with no external dependencies).

## Decision: Contract test approach (no machine-readable OpenAPI file available)

- **Decision**: The contract test for this feature asserts directly against the
  behaviour documented in `joh-elinks-api/swagger-ui-elinks-api-v5.pdf` (path, method,
  status code, empty body) rather than validating against a machine-readable
  OpenAPI/Swagger document, because only a rendered PDF export of the Swagger UI is
  available in the repository — no `.yaml`/`.json` OpenAPI source file exists.
- **Rationale**: Principle XI requires contract tests "to ensure compatibility with the
  Swagger/OpenAPI specification." Without a machine-readable spec to validate against
  programmatically (e.g., with an OpenAPI validator library), the closest faithful
  substitute is a test that pins the exact contract details extracted from the PDF. This
  keeps the test meaningful and traceable to the source of truth (Principle I) without
  inventing tooling this feature doesn't need.
- **Alternatives considered**: Transcribing the full E-Links OpenAPI document into a
  checked-in `.yaml` file and validating against it with a schema-validation library —
  deferred as out of scope for this single-endpoint feature; worth revisiting once more
  endpoints are mocked and a shared OpenAPI artifact becomes valuable project-wide
  infrastructure (see Assumptions in `spec.md`).

## Decision: API base path

- **Decision**: The application serves the endpoint at `/elinks/api/v5/healthcheck`,
  combining the server base path `/elinks` shown in the source specification's
  "Servers" field with the operation path `/api/v5/healthcheck`.
- **Rationale**: Principle I requires preserving the contract's paths exactly; the
  source specification defines the server context as `/elinks` and the operation path
  underneath it, so the mock's effective path must combine both to be a faithful stand-in.
- **Alternatives considered**: Serving only `/api/v5/healthcheck` at the application
  root — rejected because it would silently drop the `/elinks` context path defined by
  the contract's servers block.

## Decision: Base package naming

- **Decision**: `uk.gov.moj.elinks.mock`.
- **Rationale**: The source specification is hosted under a `judiciary.uk` domain and
  this repository lives under an `MOJ` (Ministry of Justice) workspace, so a UK
  government MoJ-style package namespace is the most contextually accurate reasonable
  default. This is a low-impact, easily renamed convention rather than a load-bearing
  design decision.
- **Alternatives considered**: A generic `com.example` style package — rejected as less
  meaningful for a project with clear organisational context.

## Resolved unknowns

All items originally marked as candidates for `NEEDS CLARIFICATION` in the Technical
Context are resolved above. No open unknowns remain for Phase 1 design.
