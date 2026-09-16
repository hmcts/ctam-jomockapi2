---

description: "Task list template for feature implementation"
---

# Tasks: Healthcheck Endpoint

**Input**: Design documents from `/specs/001-healthcheck-endpoint/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/healthcheck.md, quickstart.md

**Tests**: Included. The constitution (Principle XI, Development Workflow & Quality Gates)
mandates unit/controller/contract test coverage for all endpoint behaviour, so test tasks
are required here, not optional.

**Organization**: This feature has a single user story (US1, P1). Tasks are still grouped
by phase (Setup → Foundational → User Story → Polish) per the standard structure.

**Amendment note (constitution v1.0.0 → v1.1.0, 2026-09-15)**: This task list was
originally scoped to just the healthcheck endpoint. It now also bootstraps the
project-wide shared infrastructure required by the amended Principle IX and new
Principles XIII/XIV (build-quality gates, correlation IDs, shared error shape), since
this is the feature that establishes the base skeleton — see `research.md` and `plan.md`
for the corresponding decisions. Tasks T004–T005 and T009–T012 are new; all later task
IDs were renumbered accordingly.

**Build tool note (2026-09-16)**: This task list bootstraps a Gradle project, to align
with the CNP framework's own `service-api-marketplace` tooling — see `research.md` →
Framework & build tool / Automated quality gates decisions.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1)
- Paths below are relative to the repository root, per `plan.md`'s Project Structure
  (base package `uk.gov.moj.elinks.mock`)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Bootstrap the Gradle/Spring Boot project skeleton and the build-quality
tooling required project-wide — this is the first feature in the repository, so no
project scaffolding exists yet.

- [ ] T001 Create the Gradle project skeleton: `settings.gradle`, `build.gradle`
      (`org.springframework.boot` 4.1.1 and `io.spring.dependency-management` plugins,
      Java 25 toolchain), the Gradle wrapper (`gradlew`, `gradlew.bat`,
      `gradle/wrapper/`), `src/main/java/uk/gov/moj/elinks/mock/`,
      `src/main/resources/`, `src/test/java/uk/gov/moj/elinks/mock/`, and a `.gitignore`
      covering Gradle's `.gradle/` and `build/` directories, per `plan.md` → Project
      Structure.
- [ ] T002 Add `spring-boot-starter-web` and `spring-boot-starter-test` dependencies to
      `build.gradle`, per `research.md` → Framework & build tool / Testing stack
      decisions.
- [ ] T003 [P] Create `src/main/resources/application.yml` with the default server
      configuration (e.g. `server.port: 8080`), per `plan.md` → Technical Context.
- [ ] T004 [P] Configure `build.gradle` build-quality plugins per `research.md` →
      Automated quality gates decision (Constitution Principle XIII): apply the
      `uk.gov.hmcts.java` plugin (applies Checkstyle and the OWASP
      `org.owasp.dependencycheck` plugin with HMCTS/CNP default settings), pointing its
      suppression file at `config/owasp/suppressions.xml` (create the file, empty
      `<suppressions/>` root, if no findings are yet suppressed); apply the `jacoco`
      plugin and configure `jacocoTestReport`; add `-Xlint:unchecked -Werror` to
      `tasks.withType(JavaCompile)` compiler args.
- [ ] T005 [P] Create `.github/workflows/ci.yml` running `./gradlew check` on every pull
      request and every push to `main`, per `research.md` → Automated quality gates
      decision (Constitution Principle XIII).

**Checkpoint**: Project builds (`./gradlew check`) with no source files yet beyond
config; checkstyle, dependency-check, and JaCoCo reports are generated with no
violations.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core application wiring, plus the shared error-response, correlation-ID,
and logging infrastructure that this endpoint (and every future endpoint) needs.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T006 [P] Create the Spring Boot application entry point
      `src/main/java/uk/gov/moj/elinks/mock/ElinksMockApplication.java`
      (`@SpringBootApplication`, standard `main` method).
- [ ] T007 [P] Add `server.servlet.context-path: /elinks` to
      `src/main/resources/application.yml`, per `research.md` → API base path decision
      and `contracts/healthcheck.md` (server base path `/elinks`), so every operation
      path defined in the source specification resolves correctly for this and future
      endpoints.
- [ ] T008 [P] Create the shared `ErrorResponse` record in
      `src/main/java/uk/gov/moj/elinks/mock/error/ErrorResponse.java` with fields
      `message` (String), `timestamp` (Instant), `traceId` (String), per `research.md` →
      Shared error response shape decision (Constitution Principle IX).
- [ ] T009 [P] Create `CorrelationIdFilter` in
      `src/main/java/uk/gov/moj/elinks/mock/tracing/CorrelationIdFilter.java`
      (`OncePerRequestFilter`) that reads an inbound `X-Correlation-Id` header or
      generates a UUID if absent, puts it in SLF4J MDC for the duration of the request,
      echoes it back as a response header on every response, and clears it afterwards;
      register it so it runs on every request (Constitution Principle XIV).
- [ ] T010 [P] Create `LogSanitizer` in
      `src/main/java/uk/gov/moj/elinks/mock/logging/LogSanitizer.java` with a static
      method that strips/escapes CR and LF characters from a caller-supplied string
      before it is safe to log, per `research.md` → Shared error response shape and
      request correlation decision (Constitution Principle XIV).
- [ ] T011 Create `GlobalExceptionHandler` in
      `src/main/java/uk/gov/moj/elinks/mock/error/GlobalExceptionHandler.java`
      (`@RestControllerAdvice`) with handlers for
      `HttpRequestMethodNotSupportedException` → `405` and a fallback `Exception` → `500`,
      each returning the shared `ErrorResponse` (T008) with the current request's trace
      ID read from MDC (T009). Depends on T008, T009.

**Checkpoint**: Application starts (`./gradlew bootRun`) and serves under `/elinks`,
every response carries an `X-Correlation-Id` header, and an unsupported-method request
against any path returns the shared `ErrorResponse` shape — foundation ready for User
Story 1.

---

## Phase 3: User Story 1 - Confirm the mock service is running (Priority: P1) 🎯 MVP

**Goal**: Expose `GET /elinks/api/v5/healthcheck` returning `200 OK` with an empty body,
requiring no authentication and no parameters, per `contracts/healthcheck.md`.

**Independent Test**: Start the application and call
`GET /elinks/api/v5/healthcheck` with no credentials — verify `200 OK` with an empty
body, repeatably and regardless of extra query parameters, per `spec.md` Acceptance
Scenarios 1–3.

### Tests for User Story 1 ⚠️

> Write these tests FIRST; confirm they FAIL (404, since the controller doesn't exist
> yet) before implementing.

- [ ] T012 [P] [US1] Contract test in
      `src/test/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckContractTest.java`
      asserting, per `contracts/healthcheck.md`: `GET /elinks/api/v5/healthcheck`
      returns HTTP `200` with an empty response body and an `X-Correlation-Id` response
      header present; a `POST` to the same path returns `405` with the shared
      `ErrorResponse` body shape (`message`, `timestamp`, `traceId`), and `traceId`
      matches the request's `X-Correlation-Id`.
- [ ] T013 [P] [US1] Controller/API test (Spring `MockMvc`) in
      `src/test/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckControllerTest.java`
      covering, per `spec.md` Acceptance Scenarios, Edge Cases, and Success Criteria:
      (a) `200` + empty body with no `Authorization` header; (b) an unexpected query
      parameter (e.g. `?foo=bar`) is ignored and still returns `200` + empty body;
      (c) two consecutive calls return identical responses; (d) `POST
      /elinks/api/v5/healthcheck` returns `405 Method Not Allowed` with the shared
      `ErrorResponse` body; (e) the request completes well within one second (SC-003 /
      FR-006); (f) supplying an inbound `X-Correlation-Id` header causes the exact same
      value to be echoed back in the response.

### Implementation for User Story 1

- [ ] T014 [US1] Implement a thin `HealthcheckController` in
      `src/main/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckController.java`
      with a single `@GetMapping("/api/v5/healthcheck")` handler that returns `200 OK`
      with no response body (e.g. `ResponseEntity.ok().build()`) — no service, mapper,
      or DTO needed since the operation performs no computation and returns no payload
      (FR-002, FR-003, FR-004); the `X-Correlation-Id` header and the `405` handling
      come from the shared Foundational infrastructure (T009, T011) automatically.
      Confirm T012 and T013 now pass.

**Checkpoint**: User Story 1 is fully functional and independently testable — this is
the feature's MVP and its entire scope.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Final validation against the documented contract and quickstart.

- [ ] T015 Run through `quickstart.md` end-to-end against the running application
      (`./gradlew bootRun`, then the `curl` checks for the healthy path, repeated
      calls, and the unsupported-method case), timing the healthy-path `curl` call
      (e.g. `curl -w "%{time_total}\n"`) to confirm it completes well under one second
      (SC-003), confirming the `X-Correlation-Id` header appears on every response, and
      confirming every expected outcome holds.
- [ ] T016 Run `./gradlew check` and confirm the checkstyle, OWASP dependency-check, and
      JaCoCo reports all generate cleanly (Constitution Principle XIII) — this is the
      gate every future feature's CI run (T005) will also enforce.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001–T005) completion — blocks Phase 3.
- **User Story 1 (Phase 3)**: Depends on Foundational (T006–T011) completion.
- **Polish (Phase 4)**: Depends on User Story 1 (T012–T014) completion.

### Within User Story 1

- Tests (T012, T013) MUST be written and MUST fail before implementation (T014).
- T014 depends on T012 and T013 (and on T006/T007 from Foundational; T009/T011 supply
  the correlation-ID header and 405 body automatically without further wiring).

### Parallel Opportunities

- T003 and T004 can run in parallel with T002 (different files: `application.yml` and
  `build.gradle` plugin config vs `build.gradle` dependencies — coordinate if editing
  the same `build.gradle` block), once T001 exists; T005 (`.github/workflows/ci.yml`)
  is independent of all three.
- T006, T007, T008, T009, T010 can run in parallel (different files), once Setup is
  complete; T011 depends on T008 and T009.
- T012 and T013 can run in parallel (different test files), once Foundational is
  complete.

---

## Parallel Example: User Story 1

```bash
# Launch both tests for User Story 1 together:
Task: "Contract test in src/test/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckContractTest.java"
Task: "Controller/API test in src/test/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckControllerTest.java"
```

---

## Implementation Strategy

### MVP First (and only) — User Story 1

1. Complete Phase 1: Setup (T001–T005).
2. Complete Phase 2: Foundational (T006–T011) — CRITICAL, blocks the story.
3. Complete Phase 3: User Story 1 (T012–T014).
4. **STOP and VALIDATE**: run Phase 4 (T015–T016) against the running application.
5. Feature complete — this is the entire scope of `001-healthcheck-endpoint`, plus the
   shared infrastructure later features depend on.

---

## Notes

- This feature has exactly one user story, so there is no cross-story integration risk;
  every task above serves US1 directly or the shared bootstrap/quality-gate/
  observability infrastructure it and later features depend on.
- Per the constitution (Principle VIII), no `Map<String, Object>` is used anywhere —
  the healthcheck response has no body, so no DTO is needed there; the shared
  `ErrorResponse` (T008) is itself a typed record, not a map.
- Per the constitution (Principle IX, amended v1.1.0), the `405` behaviour for
  unsupported methods (T013's scenario d) is handled by the shared
  `GlobalExceptionHandler` (T011), returning the shared `ErrorResponse` body — not
  Spring's default error page and not a bespoke per-endpoint check.
- Per the constitution (Principle XIV, added v1.1.0), `CorrelationIdFilter` (T009)
  applies to every request in the application, not just this endpoint's — later
  features do not need to re-implement it.
- Commit after each task or logical group.
