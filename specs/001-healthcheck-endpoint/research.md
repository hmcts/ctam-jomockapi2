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

- **Decision**: Spring Boot 4.1.1, pinned per project direction, with Gradle (Groovy
  DSL) as the build tool, using `spring-boot-starter-web`.
- **Rationale**: Spring Boot is mandated by Principle II, and 4.1.1 is the project's
  explicitly chosen release. Gradle is chosen — superseding the project's initial
  Maven default — to align with the sibling HMCTS/CNP Spring Boot service repository
  (`service-api-marketplace`) that the constitution's v1.1.0 quality-gate amendment
  was itself modelled on: matching its build tool means this project can also adopt
  its actual Gradle plugins (`uk.gov.hmcts.java`, `jacoco`) directly rather than
  hand-rolling Maven equivalents of HMCTS/CNP tooling that doesn't exist for Maven.
  Only `spring-boot-starter-web` is needed for this feature — no persistence,
  security, or reference-data starters are required yet since the healthcheck
  endpoint has no data or auth dependency (FR-002, FR-003).
- **Alternatives considered**: Maven — the initial default before this project
  committed to aligning with the CNP framework's Gradle-based tooling; superseded,
  not rejected on technical grounds. Spring Boot 3.x — this was the initial default
  before the project's target stack was pinned; superseded, not rejected on technical
  grounds. A minimal non-Spring HTTP server (rejected — violates Principle II
  directly).

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

## Decision: Shared error response shape and request correlation (Constitution v1.1.0, Principles IX & XIV)

- **Decision**: Introduce two small, shared components used by every future endpoint,
  not just healthcheck: an `ErrorResponse(String message, Instant timestamp, String
  traceId)` DTO returned by every non-2xx response, and a `CorrelationIdFilter`
  (`OncePerRequestFilter`) that reads an inbound `X-Correlation-Id` header (or generates
  a UUID if absent), stores it in SLF4J MDC for the duration of the request, echoes it
  back as a response header on every response (2xx included), and is cleared afterwards.
  A `GlobalExceptionHandler` (`@RestControllerAdvice`) maps framework-level failures
  (unsupported method, no matching route, unhandled exception) to this shared
  `ErrorResponse` shape, reading the trace ID from MDC.
- **Rationale**: The constitution (amended IX, and new XIV) now mandates one shared
  error shape carrying a correlation/trace ID, and a correlation ID on every request,
  project-wide. Since this is the first feature and establishes the base skeleton, it is
  the natural place to introduce this shared infrastructure once, so later features
  extend it instead of each inventing their own.
  The healthcheck endpoint's own `200` response stays an empty body per FR-004 — the
  correlation-ID header is added regardless (headers are not "body"), and the shared
  `ErrorResponse`/`GlobalExceptionHandler` only become observable on this endpoint via
  its one error case (an unsupported HTTP method → 405).
- **Alternatives considered**: Excluding health/status endpoints from correlation-ID
  tracing (a pattern seen in a sibling HMCTS repository) — rejected because this
  project's constitution states no such exemption, and FR-004 only constrains the
  *body*, not response headers, so there is no actual conflict to resolve by excluding
  it. Deferring this infrastructure until `002` needs it — rejected because the
  constitution's requirement is already ratified and applies to this feature's own
  405 case, not just to later features.

## Decision: Automated quality gates for the Gradle build (Constitution Principle XIII)

- **Decision**: Configure `build.gradle` with: the `uk.gov.hmcts.java` Gradle plugin
  (the same HMCTS/CNP plugin `service-api-marketplace` uses), which applies Checkstyle
  and the OWASP `org.owasp.dependencycheck` plugin with HMCTS default settings in one
  step; accepted vulnerability false positives recorded in a checked-in
  `config/owasp/suppressions.xml`; the `jacoco` plugin generating a coverage report via
  `jacocoTestReport`; `tasks.withType(JavaCompile)` compiler args including
  `-Xlint:unchecked -Werror` (warnings as build errors); and a GitHub Actions workflow
  (`.github/workflows/ci.yml`) running `./gradlew check` on every pull request and push
  to `main` — mirroring `service-api-marketplace`'s own `ci.yml` exactly. All three
  layers Principle XIII requires (unit, controller/API, contract) currently fit inside
  Gradle's default `test` source set, since none of this feature's tests need a running
  application; Gradle natively supports adding dedicated `integrationTest`/
  `functionalTest`/`smokeTest` source sets later (as `service-api-marketplace` does,
  via its `configureSourceSet` helper) if and when a future feature needs a genuine
  integration-test layer, without needing extra plugins to do so.
- **Rationale**: Principle XIII is a project-wide MUST; as the feature that bootstraps
  the Gradle project, this is where the build-tooling baseline must be established so
  every later feature inherits it rather than each retrofitting its own. Reusing the
  actual HMCTS/CNP plugin (rather than assembling equivalent checks from generic Gradle
  plugins) is the most direct way to satisfy the constitution's explicit intent to
  align with the CNP framework's own tooling, not just its behaviour.
- **Alternatives considered**: Assembling equivalent behaviour from generic community
  Gradle plugins (a standalone `checkstyle` plugin plus a separately-configured OWASP
  plugin) — rejected in favour of the single `uk.gov.hmcts.java` plugin, which is the
  literal tool the CNP framework's own services use and keeps this project's build
  config traceable to that convention rather than an independent reconstruction of it.
  Pre-building dedicated `integrationTest`/`functionalTest`/`smokeTest` Gradle source
  sets now — deferred as unused complexity (Principle XII) until a feature actually
  needs a running-application test layer. A heavyweight, off-the-shelf Checkstyle
  ruleset (e.g., Google's) on top of the HMCTS plugin's defaults — deferred in favour of
  the plugin's own default ruleset to avoid a large one-off reformatting pass unrelated
  to this feature's actual scope.

## Resolved unknowns

All items originally marked as candidates for `NEEDS CLARIFICATION` in the Technical
Context are resolved above. No open unknowns remain for Phase 1 design.
