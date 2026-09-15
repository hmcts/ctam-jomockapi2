# Implementation Plan: Reference Data Lookup by Type and ID

**Branch**: `002-reference-data-lookup` | **Date**: 2026-09-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-reference-data-lookup/spec.md`

## Summary

Expose a mock `GET /elinks/api/v5/reference_data/{attribute_name}/{reference_id}`
endpoint that resolves one of the 11 reference-data types (current or deprecated alias
name) plus a numeric ID into the single matching synthetic reference-data record,
sourced from the extracts in `joh-elinks-api/ReferenceData/`. The technical approach
introduces the project's first generic reference-data mechanism (Constitution Principle
VII) — a type registry, a resource loader, and a lookup service shared by this endpoint
and reusable by future reference-data endpoints — plus the project's first centralised,
simulated authentication check (Principle IX), since this is also the first endpoint in
the contract that requires authentication.

## Technical Context

**Language/Version**: Java 25 (LTS) — consistent with `001-healthcheck-endpoint`

**Primary Dependencies**: Spring Boot 4.1.1, `spring-boot-starter-web`,
`jackson-dataformat-csv` (for parsing the bundled CSV reference-data extracts using the
same Jackson stack already provided by `spring-boot-starter-web`)

**Storage**: N/A (no database) — reference data is bundled, read-only, synthetic data
loaded from classpath resources into an immutable in-memory structure at startup

**Testing**: JUnit 5 + `spring-boot-starter-test` (`MockMvc` for controller/API tests),
plus plain unit tests for the reusable resolver/loader/service components

**Target Platform**: JVM, deployed as a standard Spring Boot application (Linux server /
container) — same runtime as `001-healthcheck-endpoint`

**Project Type**: Single web-service (Maven, standard Spring Boot layout); this feature
extends the same application skeleton established by `001-healthcheck-endpoint` rather
than creating a second one

**Performance Goals**: Sub-second response per record lookup (SC-004); in-memory lookup
after startup load, no per-request I/O

**Constraints**: Authentication required (FR-009); no date-validity filtering (FR-012);
deprecated alias resolution must be centralised, not per-endpoint (FR-003, Constitution
Principle VII); numeric-ID validation distinct from not-found handling (FR-006–FR-008)

**Scale/Scope**: 11 reference-data types, each a few dozen to a few hundred rows (largest
observed: `Location`/`BaseLocation`, low hundreds of rows) — trivially held in memory

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Contract-First Development | PASS | Path, method, path parameters, the 11 supported `attribute_name` values (+ deprecated aliases), and the 200/401 responses are sourced directly from `joh-elinks-api/swagger-ui-elinks-api-v5.pdf`. The undocumented 400/404 behaviours are explicit, recorded assumptions (`spec.md` → Assumptions), not silent additions. |
| II. Java Spring Boot Architecture | PASS | Java 25 + Spring Boot 4.1.1, matching `001-healthcheck-endpoint`. Thin controller delegates to a `ReferenceDataService`; parsing/resolution logic lives in dedicated components (loader, resolver), not the controller. |
| III. Reuse Before Duplication | PASS | The type resolver, resource loader, and lookup service are built once as shared components under a `referencedata` package, designed for reuse by any future reference-data endpoint (e.g., the list variant) rather than being coupled to this one controller. |
| IV. Synthetic Data Only | PASS | All served data originates from the checked-in synthetic extracts in `joh-elinks-api/ReferenceData/`; no production or real personal data is introduced. |
| V. Deterministic Behaviour | PASS | Data is loaded once at startup into an immutable structure; the same request always returns the same result for a given running instance (FR-010). No random or time-dependent behaviour (FR-012 removes any date-based variability). |
| VI. Behavioural Fidelity | PASS | Both path parameters (`attribute_name`, `reference_id`) have real, observable effects (type resolution and record selection); neither is accepted and ignored. |
| VII. Generic Reference-Data Handling | PASS | A single `ReferenceDataType` registry plus a generic loader/service handles all 11 types and both their current and deprecated names centrally — adding a 12th type would mean adding one registry entry and one record class, not a new endpoint or duplicated logic. |
| VIII. Typed API Models | PASS | Each reference-data type has its own typed Java record DTO (sharing common fields via a base interface) built from its known CSV/JSON schema; no `Map<String,Object>` response shape is used despite the generic lookup mechanism underneath. |
| IX. Centralised Validation and Error Handling *(shape amended v1.1.0)* | PASS | Simulated authentication is implemented once as a shared, configurable interceptor (this is the first endpoint to need it), not duplicated per controller. Global exception handling converts a malformed (non-numeric) ID and an unresolved type/ID into the FR-006–FR-008 responses consistently, all using the shared `ErrorResponse` shape (message + trace ID) established by `001-healthcheck-endpoint` and extended, not duplicated, here (`research.md`). |
| X. Configuration Over Hard-Coding | PASS | Whether simulated authentication is enforced is driven by `application.yml`, not hard-coded into the interceptor or controller, so it can be toggled per environment/test without code changes. |
| XI. Testability | PASS | Unit tests planned for the resolver (alias resolution), loader (parsing + malformed-row handling), and service (lookup/not-found); controller/API tests for status codes and body shape; a contract test pinning the documented path/parameters/status codes. |
| XII. Simplicity and Maintainability | PASS | No persistence layer, caching layer, or speculative abstraction beyond what Principle VII already requires (a generic type/lookup mechanism); per-type extra fields are plain record components, not a rules engine. |

| XIII. Automated Quality Gates *(added v1.1.0)* | PASS | This feature adds no new build-tooling requirement — it inherits the compiler-warnings-as-errors, checkstyle, dependency-check, JaCoCo, and CI setup established by `001-healthcheck-endpoint` (skip if already present, per `tasks.md`). |
| XIV. Observability & Traceability *(added v1.1.0)* | PASS | Every response from this endpoint carries the correlation ID via the shared `CorrelationIdFilter` (from `001`); no new tracing infrastructure is introduced here (`research.md`). |

No violations requiring justification. Complexity Tracking is not needed.

*Post-Phase 1 re-check*: `data-model.md` and `contracts/reference-data-by-id.md`
confirm the design stays within one generic lookup mechanism (Principle VII) plus typed
per-type DTOs (Principle VIII), with authentication and error handling both centralised
(Principle IX) rather than embedded in the controller. No new violations were introduced
by the detailed design.

*Amendment re-check (constitution v1.0.0 → v1.1.0, 2026-09-15)*: This plan was
originally written against v1.0.0 (Principles I–XII only) and assumed this feature would
create its own `error.GlobalExceptionHandler` from scratch. Rows for XIII/XIV are now
included above, the IX row is updated, and `research.md`/`tasks.md` now have this
feature extend the shared error/observability infrastructure from
`001-healthcheck-endpoint` instead of duplicating it (Principle III).

## Project Structure

### Documentation (this feature)

```text
specs/002-reference-data-lookup/
├── plan.md                       # This file (/speckit-plan command output)
├── research.md                   # Phase 0 output (/speckit-plan command)
├── data-model.md                 # Phase 1 output (/speckit-plan command)
├── quickstart.md                 # Phase 1 output (/speckit-plan command)
├── contracts/                    # Phase 1 output (/speckit-plan command)
│   └── reference-data-by-id.md
├── checklists/
│   └── requirements.md
└── tasks.md                      # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

This feature extends the single Maven/Spring Boot application skeleton established by
`001-healthcheck-endpoint` (same `pom.xml`, same base package). It adds a new
`referencedata` feature package and the project's first shared `security` package:

```text
pom.xml

src/
├── main/
│   ├── java/uk/gov/moj/elinks/mock/
│   │   ├── ElinksMockApplication.java        # from 001; unchanged by this feature
│   │   ├── healthcheck/                      # from 001; unchanged by this feature
│   │   ├── tracing/                          # from 001; unchanged — CorrelationIdFilter reused as-is
│   │   ├── logging/                          # from 001; unchanged — LogSanitizer reused as-is
│   │   ├── error/
│   │   │   ├── ErrorResponse.java            # from 001; unchanged by this feature
│   │   │   └── GlobalExceptionHandler.java   # from 001; EXTENDED here with one new handler (FR-008)
│   │   ├── security/
│   │   │   └── SimulatedAuthInterceptor.java # shared, configurable auth check (Principle IX); writes ErrorResponse directly on 401
│   │   └── referencedata/
│   │       ├── ReferenceDataType.java        # enum: 11 types, canonical + deprecated names, resource path
│   │       ├── ReferenceDataRecord.java      # common-field interface (id, name, start/end date, timestamps)
│   │       ├── model/                        # one record class per type (AppointmentTitle, BaseLocation, ...)
│   │       ├── ReferenceDataLoader.java      # parses bundled CSV/JSON into in-memory maps at startup
│   │       ├── ReferenceDataService.java     # generic findByTypeAndId(type, id)
│   │       └── ReferenceDataController.java  # thin controller; 404s via ResponseStatusException → shared handler
│   └── resources/
│       ├── application.yml                   # adds mock.security.* config (from 001, extended)
│       └── referencedata/                    # bundled synthetic extracts, one file per type (see research.md)
└── test/
    └── java/uk/gov/moj/elinks/mock/
        ├── error/
        │   └── GlobalExceptionHandlerTest.java       # from 001; EXTENDED here for the new handler method
        ├── security/
        │   └── SimulatedAuthInterceptorTest.java
        └── referencedata/
            ├── ReferenceDataTypeResolutionTest.java   # unit: canonical + deprecated alias resolution
            ├── ReferenceDataLoaderTest.java           # unit: parsing, malformed-row handling
            ├── ReferenceDataServiceTest.java          # unit: found / not-found lookups
            ├── ReferenceDataControllerTest.java       # controller/API test (MockMvc)
            └── ReferenceDataContractTest.java         # contract test vs. source spec
```

**Structure Decision**: Single Maven-based Spring Boot application at the repository
root, unchanged from `001-healthcheck-endpoint` (Principle II). This feature adds one
new feature package (`referencedata`) and one new shared package (`security`) alongside
`healthcheck`, per Principle III — shared behaviour (authentication, reference-data
resolution) lives in its own package precisely because more than this one endpoint is
expected to need it.

## Complexity Tracking

*No entries — Constitution Check reported no violations.*
