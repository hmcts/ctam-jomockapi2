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

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1)
- Paths below are relative to the repository root, per `plan.md`'s Project Structure
  (base package `uk.gov.moj.elinks.mock`)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Bootstrap the Maven/Spring Boot project skeleton — this is the first
feature in the repository, so no project scaffolding exists yet.

- [ ] T001 Create the Maven project skeleton: `pom.xml` (Spring Boot 4.1.1 parent, Java 25
      source/target), `src/main/java/uk/gov/moj/elinks/mock/`,
      `src/main/resources/`, `src/test/java/uk/gov/moj/elinks/mock/`, and a `.gitignore`
      covering Maven's `target/` directory, per `plan.md` → Project Structure.
- [ ] T002 Add `spring-boot-starter-web` and `spring-boot-starter-test` dependencies to
      `pom.xml`, per `research.md` → Framework & build tool / Testing stack decisions.
- [ ] T003 [P] Create `src/main/resources/application.yml` with the default server
      configuration (e.g. `server.port: 8080`), per `plan.md` → Technical Context.

**Checkpoint**: Project builds (`./mvnw compile`) with no source files yet beyond config.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core application wiring that the healthcheck endpoint (and any future
endpoint) needs to be reachable at all.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T004 [P] Create the Spring Boot application entry point
      `src/main/java/uk/gov/moj/elinks/mock/ElinksMockApplication.java`
      (`@SpringBootApplication`, standard `main` method).
- [ ] T005 [P] Add `server.servlet.context-path: /elinks` to
      `src/main/resources/application.yml`, per `research.md` → API base path decision
      and `contracts/healthcheck.md` (server base path `/elinks`), so every operation
      path defined in the source specification resolves correctly for this and future
      endpoints.

**Checkpoint**: Application starts (`./mvnw spring-boot:run`) and serves under `/elinks`,
even though no endpoint exists yet — foundation ready for User Story 1.

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

- [ ] T006 [P] [US1] Contract test in
      `src/test/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckContractTest.java`
      asserting, per `contracts/healthcheck.md`: `GET /elinks/api/v5/healthcheck`
      returns HTTP `200` with an empty response body.
- [ ] T007 [P] [US1] Controller/API test (Spring `MockMvc`) in
      `src/test/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckControllerTest.java`
      covering, per `spec.md` Acceptance Scenarios, Edge Cases, and Success Criteria:
      (a) `200` + empty body with no `Authorization` header; (b) an unexpected query
      parameter (e.g. `?foo=bar`) is ignored and still returns `200` + empty body;
      (c) two consecutive calls return identical responses; (d) `POST
      /elinks/api/v5/healthcheck` returns `405 Method Not Allowed`; (e) the request
      completes well within one second (SC-003 / FR-006 — e.g. assert elapsed wall-clock
      time around the `MockMvc` call is comfortably under a 1s bound).

### Implementation for User Story 1

- [ ] T008 [US1] Implement a thin `HealthcheckController` in
      `src/main/java/uk/gov/moj/elinks/mock/healthcheck/HealthcheckController.java`
      with a single `@GetMapping("/api/v5/healthcheck")` handler that returns `200 OK`
      with no response body (e.g. `ResponseEntity.ok().build()`) — no service, mapper,
      or DTO needed since the operation performs no computation and returns no payload
      (FR-002, FR-003, FR-004). Confirm T006 and T007 now pass.

**Checkpoint**: User Story 1 is fully functional and independently testable — this is
the feature's MVP and its entire scope.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Final validation against the documented contract and quickstart.

- [ ] T009 Run through `quickstart.md` end-to-end against the running application
      (`./mvnw spring-boot:run`, then the `curl` checks for the healthy path, repeated
      calls, and the unsupported-method case), timing the healthy-path `curl` call
      (e.g. `curl -w "%{time_total}\n"`) to confirm it completes well under one second
      (SC-003), and confirm every expected outcome holds.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001–T003) completion — blocks Phase 3.
- **User Story 1 (Phase 3)**: Depends on Foundational (T004–T005) completion.
- **Polish (Phase 4)**: Depends on User Story 1 (T006–T008) completion.

### Within User Story 1

- Tests (T006, T007) MUST be written and MUST fail before implementation (T008).
- T008 depends on T006 and T007 (and on T004/T005 from Foundational).

### Parallel Opportunities

- T003 can run in parallel with T002 (different files: `application.yml` vs `pom.xml`),
  once T001 exists.
- T004 and T005 can run in parallel (different files: `ElinksMockApplication.java` vs
  `application.yml`), once Setup is complete.
- T006 and T007 can run in parallel (different test files), once Foundational is
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

1. Complete Phase 1: Setup (T001–T003).
2. Complete Phase 2: Foundational (T004–T005) — CRITICAL, blocks the story.
3. Complete Phase 3: User Story 1 (T006–T008).
4. **STOP and VALIDATE**: run Phase 4 (T009) against the running application.
5. Feature complete — this is the entire scope of `001-healthcheck-endpoint`.

---

## Notes

- This feature has exactly one user story, so there is no cross-story integration risk;
  every task above serves US1 directly or the shared bootstrap it depends on.
- Per the constitution (Principle VIII), no `Map<String, Object>` is used anywhere —
  the response has no body, so no DTO is needed at all.
- Per the constitution (Principle IX), the `405` behaviour for unsupported methods
  (T007's scenario d) is expected to come from Spring's default MVC handling, not a
  bespoke per-endpoint check — if the default response doesn't already satisfy that
  scenario, add a global exception handler rather than special-casing this controller.
- Commit after each task or logical group.
