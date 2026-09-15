# Phase 0 Research: Reference Data Lookup by Type and ID

**Feature**: `002-reference-data-lookup` | **Date**: 2026-09-15

This feature reuses the language/framework baseline already established by
`001-healthcheck-endpoint` (Java 25, Spring Boot 4.1.1, Maven, base package
`uk.gov.moj.elinks.mock`). Research here focuses on the decisions unique to this
feature: parsing the bundled reference-data extracts, resolving type names (including
deprecated aliases), and simulating authentication for the first time.

## Decision: Reuse the existing language/framework/package baseline

- **Decision**: Java 25, Spring Boot 4.1.1, Maven, base package `uk.gov.moj.elinks.mock`
  — identical to `001-healthcheck-endpoint`.
- **Rationale**: Both features ship in the same application; introducing a second stack
  or package convention would violate Principle II and III for no benefit.
- **Alternatives considered**: None — re-deriving this baseline independently would risk
  drift from `001-healthcheck-endpoint` with no upside.

## Decision: CSV/JSON parsing library

- **Decision**: `jackson-dataformat-csv`, layered on the Jackson stack already provided
  transitively by `spring-boot-starter-web`.
- **Rationale**: The application already uses Jackson for JSON (including the one
  reference-data source file that is JSON, `Jurisdiction.json`). Using Jackson's CSV
  module for the other ten CSV extracts keeps a single parsing paradigm (annotate a
  record class, deserialize) across all 11 types instead of mixing a manual/regex CSV
  reader with Jackson JSON handling, satisfying Principle III (reuse) and XII
  (simplicity).
- **Alternatives considered**: Apache Commons CSV (viable, but introduces a second,
  unrelated parsing API purely for the 10 CSV files while JSON still needs Jackson —
  more moving parts for no behavioural benefit); hand-rolled CSV splitting (rejected —
  fragile against quoted fields and inconsistent with Principle XI's testability goals).

## Decision: Bundle reference data as classpath resources, not read from `joh-elinks-api/`

- **Decision**: Copy the 11 extracts into `src/main/resources/referencedata/` at build
  time, renamed to a simple `<type>.csv` / `.json` convention (e.g. `jurisdictions.json`,
  `base_locations.csv`), rather than reading them from `joh-elinks-api/ReferenceData/` at
  runtime.
- **Rationale**: A packaged Spring Boot application (a runnable JAR) must be
  self-contained; reading from a path outside the JAR would make the mock's data
  dependent on the working directory or an external checkout being present, breaking
  Principle V (deterministic behaviour) as soon as the app runs anywhere other than this
  repository checkout. `joh-elinks-api/ReferenceData/` remains the source-of-truth
  extract location referenced by `spec.md`; the bundled copy is a build-time input.
- **Alternatives considered**: Loading via an absolute/relative filesystem path at
  startup (rejected — not portable, violates Principle V); embedding the data directly
  in Java code as literals (rejected — defeats Principle X's configuration-over-hard-
  coding intent and makes the dataset unreviewable as data).

## Decision: Eager, in-memory loading at application startup

- **Decision**: All 11 types are parsed once at startup into immutable
  `Map<Long, ReferenceDataRecord>` instances (one per type), held for the life of the
  application instance.
- **Rationale**: Dataset sizes are small (low hundreds of rows at most), so eager loading
  costs negligible startup time and gives every request in-memory, sub-millisecond
  lookups with no per-request I/O — directly supporting SC-004 and Principle V
  (determinism: no lazy-loading race or partial-load state is possible).
- **Alternatives considered**: Lazy/on-demand parsing per request (rejected — adds
  latency and repeated-parsing risk for no benefit given the dataset size); a database
  or external cache (rejected — unjustified complexity per Principle XII for static,
  bundled, read-only data).

## Decision: Date normalization across mixed source formats

- **Decision**: Parse both source date formats found in the extracts (`dd/MM/yyyy`, e.g.
  in `BaseLocation`/`LocationType`, and `yyyy-MM-dd`, e.g. in `Location`) during loading,
  and always serialize `start_date`/`end_date` in API responses as ISO-8601
  (`yyyy-MM-dd`), matching the example shown in the source specification.
- **Rationale**: The API contract's example values are ISO-8601; consumers should see one
  consistent date format regardless of which source file happened to use which format.
  Normalizing once at load time (rather than per-response) keeps the controller and
  service free of formatting concerns (Principle II — thin controller).
- **Alternatives considered**: Passing source strings through unchanged (rejected — would
  leak an inconsistent, source-format-dependent shape to consumers, contradicting FR-010's
  identical-shape guarantee across types).

## Decision: Handling malformed rows in the source extracts

- **Decision**: A source row with a missing or non-numeric `id` (observed once, in
  `ContractType.csv`) is excluded from the loaded dataset for that type, with a
  startup-time warning logged; it is never exposed via the API.
- **Rationale**: FR-008 requires the endpoint to treat a non-numeric *requested* ID as
  invalid; the same principle extends naturally to source data that cannot be assigned a
  valid ID — such a row cannot ever be looked up by ID and must not silently corrupt the
  in-memory map (e.g. by using a blank string as a map key). Logging (rather than failing
  startup) keeps the mock resilient to the kind of minor data-quality issues real
  extracts contain, per Principle V (the app must still start deterministically).
- **Alternatives considered**: Failing application startup on any malformed row
  (rejected — one bad row in a synthetic fixture file should not take the whole mock
  down); silently assigning a synthetic fallback ID (rejected — would fabricate an ID not
  present in the source data, risking an unexpected/undocumented lookup match).

## Decision: Reference-data type resolution (current + deprecated names)

- **Decision**: A single `ReferenceDataType` enum lists all 11 types, each carrying its
  canonical (plural) name, its deprecated (singular) alias, and its backing resource; a
  small resolver does an exact-match lookup of the requested `attribute_name` against
  both names across all enum values.
- **Rationale**: Directly implements Constitution Principle VII — aliases are resolved
  "centrally, in one place" rather than per-endpoint. An unmatched `attribute_name`
  naturally falls out as "no enum value matched" → not-found (FR-007), with no separate
  validation list to keep in sync.
- **Alternatives considered**: A `Map<String, ReferenceDataType>` built by hand (works,
  but the enum-per-type approach also gives compile-time exhaustiveness when adding the
  per-type record class, so it was preferred); per-endpoint `if/else` alias handling
  (rejected — directly violates Principle VII).

## Decision: Typed per-type response models over a generic map

- **Decision**: Each of the 11 types has its own Java `record` DTO (e.g.
  `JurisdictionRecord`, `TicketRecord`) implementing a shared `ReferenceDataRecord`
  interface exposing the common fields (`id`, `name`, `startDate`, `endDate`,
  `createdAt`, `updatedAt`); type-specific extra fields (e.g. `jurisdictionId`,
  `salaried`, `parentCategoryId`) are additional record components on the specific type.
- **Rationale**: Satisfies Principle VIII (typed models, no `Map<String,Object>`) while
  the *mechanism* that loads and looks up any type remains generic (Principle VII) —
  genericity lives in the loader/service/resolver, not in the response shape.
- **Alternatives considered**: One shared DTO with a generic `attributes: Map<String,
  Object>` field for type-specific data (rejected — exactly the pattern Principle VIII
  prohibits, since every type's extra fields are fully known upfront from the CSV/JSON
  headers).

## Decision: Simulated authentication

- **Decision**: A single, shared Spring interceptor checks for a well-formed
  `Authorization: Bearer <non-blank-token>` header on requests to endpoints that require
  it (this endpoint is the first); it does not validate the token against any real
  identity provider — presence of a non-blank bearer token is sufficient to simulate an
  authenticated caller. Whether the check is enforced is controlled by an
  `application.yml` property (defaulting to enabled).
- **Rationale**: The source specification shows an authorization requirement (lock icon)
  on this operation but, being a mock, there is no real credential to validate against
  (Principle IV — synthetic only). Principle IX requires simulated authentication to be
  centralised and configurable, and Principle X requires it be configuration-driven, not
  hard-coded — a single interceptor with a toggle satisfies both directly.
- **Alternatives considered**: Full OAuth2/JWT validation against a real or embedded
  identity provider (rejected — unjustified complexity per Principle XII for a mock with
  no real users); per-controller manual header checks (rejected — violates Principle IX's
  centralisation requirement and would need duplicating into every future authenticated
  endpoint).

## Decision: Contract-test approach (no machine-readable OpenAPI file available)

- **Decision**: Same approach as `001-healthcheck-endpoint` — the contract test pins the
  path, path parameters, supported type names (current + deprecated), and status codes
  extracted directly from `joh-elinks-api/swagger-ui-elinks-api-v5.pdf`, since no
  machine-readable OpenAPI source file exists in the repository.
- **Rationale**: Consistency with the established approach (Principle III); revisiting a
  checked-in machine-readable OpenAPI document remains a project-wide decision out of
  scope for this single-endpoint feature (see `001-healthcheck-endpoint`'s research.md).
- **Alternatives considered**: None new — this reuses the prior feature's resolved
  decision rather than re-litigating it.

## Decision: Reuse the shared error/observability infrastructure (Constitution v1.1.0)

- **Decision**: This feature does not create its own error-response DTO or its own
  `@RestControllerAdvice`. It reuses the shared `ErrorResponse` record and
  `GlobalExceptionHandler` established by `001-healthcheck-endpoint` (skip creation if
  already present), adding one new handler method to the existing
  `GlobalExceptionHandler` for the `reference_id` type-mismatch case (FR-008), and
  relies on the already-registered `CorrelationIdFilter` for every response's
  correlation ID. The `SimulatedAuthInterceptor`'s `401` response and the controller's
  `404` responses (FR-006, FR-007) both use the shared `ErrorResponse` shape.
- **Rationale**: Constitution Principle IX (amended) and Principle XIV (added) apply
  project-wide, and Principle III (Reuse Before Duplication) forbids a second,
  parallel error-handling mechanism existing alongside the one `001` establishes.
  Building this feature's own `GlobalExceptionHandler`/`ErrorResponse` — as this
  feature's plan originally assumed, before the v1.1.0 amendment — would create exactly
  the duplication Principle III prohibits.
- **Alternatives considered**: A second, `referencedata`-scoped exception handler (the
  original plan) — superseded once the constitution was amended to require one shared
  mechanism; kept as historical context only, not a live alternative.

## Decision: 401 response body for simulated authentication (Constitution v1.1.0)

- **Decision**: `SimulatedAuthInterceptor` (a `HandlerInterceptor`, which runs before any
  `@RestControllerAdvice` gets a chance to handle a thrown exception) constructs and
  writes the shared `ErrorResponse` JSON body directly onto the `HttpServletResponse`
  when rejecting a request, reading the trace ID from MDC (populated by
  `CorrelationIdFilter`, which — as a servlet `Filter` — always runs before a
  `HandlerInterceptor`).
- **Rationale**: `HandlerInterceptor.preHandle` returning `false` does not go through
  `@RestControllerAdvice`, so the interceptor must build the shared error shape itself
  to satisfy the amended Principle IX; the filter-before-interceptor execution order is
  a guarantee of the Servlet/Spring MVC request lifecycle, so MDC is reliably populated
  by the time the interceptor runs.
- **Alternatives considered**: Throwing a `ResponseStatusException` from the interceptor
  — rejected because interceptor exceptions are handled by Spring MVC's own exception
  resolution, which is less predictable to keep in the shared shape than writing the
  body directly; converting the interceptor to a servlet `Filter` instead — rejected as
  an unrelated, larger change with no behavioural benefit for this feature.

## Resolved unknowns

All items originally marked as candidates for `NEEDS CLARIFICATION` in the Technical
Context are resolved above. No open unknowns remain for Phase 1 design.
