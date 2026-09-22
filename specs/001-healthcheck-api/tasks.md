---

description: "Task list for the Healthcheck API feature"
---

# Tasks: Healthcheck API

**Input**: Design documents from `/specs/001-healthcheck-api/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/healthcheck.md, quickstart.md

**Tests**: Included — Constitution Principle XI ("All significant behaviour MUST be covered by automated tests") and spec.md SC-005 ("100% of automated tests... pass consistently") make automated test coverage mandatory, not optional, for this feature.

**Organization**: Tasks are grouped by user story (spec.md) to enable independent implementation and testing of each story.

**Revision note (2026-09-18)**: This file was rewritten after constitution v1.2.0 → v1.6.0 named Gradle (not Maven), a package-by-layer structure (not package-by-feature), a `{"status":"ok"}` response body (not empty), and `GlobalExceptionHandler`/`ErrorResponse` (for the still-deferred Principle IX) as required conventions. All task IDs, file paths, and assertions below reflect the amended constitution. T005 (`.editorconfig`) was added, and every subsequent task renumbered by one, in response to `/speckit-analyze` finding K1 — the constitution's `.editorconfig` requirement (Technology & Architecture Constraints) had no task anywhere in this file.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are Gradle-standard (`src/main/java/...`, `src/test/java/...`), rooted at the repository root, organised **by layer** under base package `uk.gov.hmcts.ctam.jo` (constitution Principle II), per plan.md Project Structure

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Stand up the minimum Gradle/Spring Boot skeleton this repo does not yet have (plan.md: greenfield repo, no build file exists).

- [ ] T001 Create `build.gradle` at repository root: plugins `org.springframework.boot` version `4.1.1`, `io.spring.dependency-management` version `1.1.7`, `application`; `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`; `application { mainClass = 'uk.gov.hmcts.ctam.jo.Application' }`; `group = 'uk.gov.hmcts.ctam.jo'`; `repositories { mavenCentral() }` (required for Gradle to resolve every dependency below — found by `/speckit-analyze`, finding G1); dependencies `spring-boot-starter-web`, `spring-boot-starter-test` (test scope), `springdoc-openapi-starter-webmvc-ui` version `3.1.1` (Maven Central, confirmed compatible with Spring Boot 4.x — found by `/speckit-analyze`, finding U1). Generate the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) alongside it. (research.md decisions; constitution Principle II, amended 2026-09-18 — Checkstyle/OWASP/SonarQube/JaCoCo plugins from Principle XIII are deliberately NOT added here; see plan.md Complexity Tracking) — confirmed via repo scan (`find` for `*.java`/`build.gradle`, 2026-09-18) that no existing healthcheck-style endpoint or build file exists in this repository, so spec.md EC-005's "reuse or align with an existing implementation" branch is N/A for this feature (found by `/speckit-analyze`, finding E1).
- [ ] T002 [P] Create Spring Boot application entry point `src/main/java/uk/gov/hmcts/ctam/jo/Application.java` (`@SpringBootApplication`, `main` method calling `SpringApplication.run`)
- [ ] T003 [P] Create `src/main/resources/application.yml` with `spring.application.name: ctam-jomockapi` and `server.port: 8080` (no scenario-specific config yet — plan.md Technical Context; renamed from `elinks-jomockapi` to align with the project's established naming — constitution title "JO Mock API", base package `uk.gov.hmcts.ctam.jo`, repo `ctam-jomockapi2` — found by `/speckit-analyze`, finding N2)
- [ ] T004 [P] Update `.gitignore` at repository root to add Gradle/Java build artifact entries (`.gradle/`, `build/`, `*.class`, `.idea/`, `*.iml`) alongside the existing `joh-elinks-api/` entry
- [ ] T005 [P] Create `.editorconfig` at repository root: `root = true`; a default section (`[*]`) with narrower indentation for config-style files (e.g. `indent_size = 2`); a `[*.java]` section with wider indentation (e.g. `indent_size = 4`) and `max_line_length = 120`; `charset = utf-8`, `end_of_line = lf`, `insert_final_newline = true`, `trim_trailing_whitespace = true` (constitution Technology & Architecture Constraints, "Code style/formatting MUST be enforced via a checked-in `.editorconfig`" — this is the one Setup task not tied to a numbered Core Principle; see plan.md Constitution Check note)

**Checkpoint**: `./gradlew bootRun` starts an empty Spring Boot application successfully.

---

## Phase 2: Foundational (Blocking Prerequisites)

**No foundational tasks required beyond Phase 1.** This feature is a single, dependency-free controller with no shared service, repository, mapper, or middleware layer for multiple stories to depend on (plan.md Constitution Check: Principles III, IV, VI, VII are N/A; no auth framework is introduced per the FR-011 no-auth decision). Proceed directly to Phase 3.

---

## Phase 3: User Story 1 - Monitoring Component Verifies Application Availability (Priority: P1) 🎯 MVP

**Goal**: `GET /api/v1/healthcheck` returns `HTTP 200 OK` with body `{"status":"ok"}` whenever the application is up, with no inputs required, and stays safe under repeated calls.

**Independent Test**: `curl -i http://localhost:8080/api/v1/healthcheck` (no params/body) returns `200` with `{"status":"ok"}` every time, including when repeated rapidly in a loop, with no observable state change (spec.md AC-001, AC-002; quickstart.md).

### Tests for User Story 1 ⚠️

> Write these tests FIRST; confirm they fail (404, since no controller exists yet) before implementing.

- [ ] T006 [P] [US1] Write `HealthcheckControllerTest` in `src/test/java/uk/gov/hmcts/ctam/jo/controllers/HealthcheckControllerTest.java` (`@WebMvcTest(HealthcheckController.class)` + `MockMvc`): asserts `GET /api/v1/healthcheck` → status `200` with JSON body `{"status":"ok"}` (`jsonPath("$.status").value("ok")`), sent with no `Authorization` header/credentials (spec.md FR-002, FR-003, FR-011, AC-001, AC-002, SC-001)
- [ ] T007 [P] [US1] Write `HealthcheckSmokeTest` in `src/test/java/uk/gov/hmcts/ctam/jo/controllers/HealthcheckSmokeTest.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`): asserts a real HTTP round trip to `/api/v1/healthcheck` returns `200` with body `{"status":"ok"}` (proves the full application context wires the endpoint end-to-end)
- [ ] T008 [US1] Add a test case to `HealthcheckControllerTest` asserting `GET /api/v1/healthcheck` with an unexpected query parameter (e.g. `?unexpected=1`) and a request body still returns `200` with `{"status":"ok"}` — extra/unrecognised input is ignored, not rejected (depends on T006 creating the file; spec.md EC-004)

### Implementation for User Story 1

- [ ] T009 [US1] Create `HealthResponse` in `src/main/java/uk/gov/hmcts/ctam/jo/domain/HealthResponse.java`: `public record HealthResponse(String status)` (research.md Decision: no Lombok needed on a record, no MapStruct mapper needed — no domain/entity source to translate from)
- [ ] T010 [US1] Implement `HealthcheckController` in `src/main/java/uk/gov/hmcts/ctam/jo/controllers/HealthcheckController.java`: `@RestController`, `@GetMapping("/api/v1/healthcheck")` method returning `ResponseEntity<HealthResponse>` / `HttpStatus.OK` with body `new HealthResponse("ok")`, no parameters, no injected dependencies (depends on T001–T005, T009; research.md: no service layer)
- [ ] T011 [US1] Add repeated-call assertions to `HealthcheckControllerTest`: (a) loop `GET /api/v1/healthcheck` sequentially several times in one test method, and (b) invoke it concurrently from multiple threads (e.g. an `ExecutorService`/parallel stream firing several `MockMvc` calls at once) — confirming every call independently returns `200` with `{"status":"ok"}` and no shared/mutated state under either access pattern (depends on T006 creating the file and T010 for a meaningful assertion; spec.md FR-004, FR-005, EC-001/EC-002, AC-004; proves the narrower, automated half of SC-003 and the concurrent-access half of EC-001/EC-002 — see spec.md SC-003 for what remains an untested design expectation (sustained, long-duration polling), not a load/soak-tested claim)
- [ ] T012 [US1] Add response-time assertions to `HealthcheckSmokeTest`: (a) a coarse single-call sanity assertion (the round trip completes within 1 second) as a regression guard against gross failure, and (b) a warm-up-then-average assertion — discard the first 5 calls as JIT/connection warm-up, then assert the mean of the next 20 calls is under 100ms — giving spec.md SC-002's `<100ms` "under normal operating conditions" target an actual automated signal while avoiding the single-call flakiness (cold start, GC pauses) that a strict per-call ceiling would introduce (depends on T007 creating the file and T010 for the endpoint to exist; spec.md SC-002)

**Checkpoint**: User Story 1 is fully functional and independently testable — this is the MVP.

---

## Phase 4: User Story 2 - Operator Confirms Healthcheck Behaviour Without Leaking Internal Detail (Priority: P2)

**Goal**: The response body/headers and the published OpenAPI documentation reveal only the `status` field — nothing else.

**Independent Test**: Inspect a successful response for absence of sensitive data beyond `status`, and inspect `/v3/api-docs` for the documented contract only (spec.md AC-005, AC-006).

### Tests for User Story 2 ⚠️

- [ ] T013 [US2] Add an assertion to `HealthcheckControllerTest` that the `200` response body contains only the `status` field (no other JSON properties) and no unexpected headers (e.g. no stack trace, no server internals) are present (depends on T006 creating the file; spec.md FR-007, AC-005)
- [ ] T014 [P] [US2] Write `HealthcheckOpenApiContractTest` in `src/test/java/uk/gov/hmcts/ctam/jo/controllers/HealthcheckOpenApiContractTest.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`): fetches `/v3/api-docs` and asserts the JSON documents `GET /api/v1/healthcheck` with operation summary "Healthcheck", a `200` response described "Service is healthy", and a response schema limited to a single `status` string property (spec.md FR-010, AC-006; contracts/healthcheck.md)

### Implementation for User Story 2

- [ ] T015 [US2] Annotate `HealthcheckController#healthcheck()` (from T010) with springdoc's `@Operation(summary = "Healthcheck")` and `@ApiResponse(responseCode = "200", description = "Service is healthy")`, letting springdoc derive the response schema from the `HealthResponse` record (T009) — no `@Schema` beyond that (depends on T009, T010; research.md OpenAPI decision)

**Checkpoint**: User Stories 1 AND 2 both work independently — the endpoint functions and its observable surface (body + docs) is confirmed clean.

---

## Phase 5: User Story 3 - Unsupported Method Does Not Trigger Healthcheck Behaviour (Priority: P3)

**Goal**: A non-`GET` request to `/api/v1/healthcheck` never triggers the healthcheck success response.

**Independent Test**: `curl -i -X POST http://localhost:8080/api/v1/healthcheck` returns `405 Method Not Allowed`, not `200` (spec.md AC-003; quickstart.md).

### Tests for User Story 3 ⚠️

- [ ] T016 [US3] Add a test to `HealthcheckControllerTest` asserting `POST /api/v1/healthcheck` (and one other non-`GET` method, e.g. `PUT`) returns `405 Method Not Allowed`, confirming the success handler is never invoked (depends on T006 creating the file; spec.md FR-008, AC-003)

### Implementation for User Story 3

- [ ] T017 [US3] Verify `HealthcheckController` (T010) maps only `@GetMapping` with no catch-all `@RequestMapping`; adjust if T016 fails so non-`GET` methods are rejected by Spring's default method-not-allowed handling rather than any custom logic (depends on T010, T016)

**Checkpoint**: All three user stories are independently functional — the endpoint is complete per spec.md.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all stories; no new behaviour.

- [ ] T018 [P] Run every `quickstart.md` validation step manually against a locally running instance (`./gradlew bootRun`) and confirm actual output matches each documented expected outcome
- [ ] T019 [P] Run `./gradlew test` and confirm all of T006, T007, T008, T011, T012, T013, T014, T016 pass with zero compiler warnings (plan.md Technical Context; a one-off manual check standing in for constitution Principle XIII's warnings-as-errors gate, whose full CI enforcement is deferred per plan.md Complexity Tracking)
- [ ] T020 Review `HealthcheckController` (T010) to confirm (a) no exception is caught/swallowed there, so an unexpected internal failure falls through to Spring Boot's default error handling instead of a forced `200` (spec.md EC-003, FR-009), and (b) no dependency (database, external HTTP call, cache, filesystem, queue, or message broker) is injected into or called from the controller (spec.md FR-006). This is a documented manual-review check, not an automated test: the endpoint has no dependency or fault-injection seam to trigger a real internal failure against, and plan.md's Complexity Tracking defers building the constitution's `GlobalExceptionHandler`/`ErrorResponse` (Principle IX) to a future foundation feature — this review only confirms nothing in this feature's own code overrides Spring's default behaviour or introduces a dependency check in the meantime.
- [ ] T021 Update the "Status" section of `README.md` to reflect that the healthcheck endpoint now exists, replacing "No application code exists yet"

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Empty for this feature; Phase 3 depends directly on Phase 1.
- **User Story 1 (Phase 3)**: Depends on Phase 1 only. Delivers the MVP.
- **User Story 2 (Phase 4)**: Depends on T009/T010 (US1's `HealthResponse`/controller) existing; adds documentation/annotations on top of it, plus a new assertion appended into US1's `HealthcheckControllerTest.java` (T013) — it does not alter any of US1's existing assertions or behaviour.
- **User Story 3 (Phase 5)**: Depends on T010 (US1's controller) existing; verifies default behaviour, no new dependency on US2.
- **Polish (Phase 6)**: Depends on Phases 3–5 all being complete.

### User Story Dependencies

- **US1 (P1)**: No dependency on US2/US3 — independently testable and shippable as the MVP.
- **US2 (P2)**: Builds on the same controller/record US1 creates (T009, T010) but adds only annotations/tests — including appending one new assertion to US1's `HealthcheckControllerTest.java` (T013) — without changing US1's own behaviour or existing test assertions.
- **US3 (P3)**: Builds on the same controller US1 creates (T010) but adds only verification/tests — does not change US1 or US2.

### Parallel Opportunities

- T002, T003, T004, T005 (Setup) can run in parallel once T001 exists.
- T006 and T007 (US1 tests) can run in parallel with each other — they create two independent new files.
- T008, T011, T013, T016 all append to the single file T006 creates (`HealthcheckControllerTest.java`) and so must be applied sequentially relative to T006 and to each other (not run as simultaneous edits), even though they have no logical dependency on one another's content. T012 similarly must follow T007 (`HealthcheckSmokeTest.java`).
- T014 (US2 test) is a new file and can run in parallel with anything above.
- T018, T019 (Polish) can run in parallel.

---

## Parallel Example: User Story 1

```bash
# Launch both User Story 1 file-creation tests together (T008/T011 must follow T006 sequentially, so excluded here):
Task: "Write HealthcheckControllerTest in src/test/java/uk/gov/hmcts/ctam/jo/controllers/HealthcheckControllerTest.java"
Task: "Write HealthcheckSmokeTest in src/test/java/uk/gov/hmcts/ctam/jo/controllers/HealthcheckSmokeTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Skip Phase 2 (empty for this feature).
3. Complete Phase 3: User Story 1 (T006–T012).
4. **STOP and VALIDATE**: run `./gradlew test` and `curl` the endpoint per quickstart.md.
5. This alone satisfies the feature's primary purpose — a working `GET /api/v1/healthcheck` → `200` with `{"status":"ok"}`.

### Incremental Delivery

1. Setup → Phase 3 (US1) → validate → this is a deployable MVP.
2. Add Phase 4 (US2) → validate response/doc cleanliness independently.
3. Add Phase 5 (US3) → validate method rejection independently.
4. Phase 6 (Polish) → final full-suite validation and README update.

---

## Notes

- No `[Story]` label on Setup, Foundational (empty), or Polish tasks, per task format rules.
- All three user stories share one controller/record pair (T009, T010) by design — spec.md's own acceptance scenarios describe three angles on the same single endpoint, not three separate features; each story still has its own independently runnable test task(s).
- Constitution Principles IX (shared error response shape — `GlobalExceptionHandler`/`ErrorResponse`), XIII (CI quality gates), and XIV (correlation ID / structured logging) are intentionally out of scope for this task list — see plan.md Complexity Tracking for the documented deferral rationale for all three.
- Constitution v1.5.0 → v1.6.0 named the concrete `ErrorResponse`/`GlobalExceptionHandler` class identities for Principle IX; this file's T020 and plan.md's Complexity Tracking were updated to reference them by name so the future foundation feature has an already-agreed target.
- **This file was fully rewritten on 2026-09-18** after constitution v1.2.0 → v1.6.0 named Gradle, package-by-layer structure, the `{"status":"ok"}` response body, and `GlobalExceptionHandler`/`ErrorResponse` as required conventions — none of which existed when this file was first generated. Every task ID, file path, and body-related assertion above reflects the amended constitution; there is no prior numbering to reconcile against since the whole file was regenerated rather than patched.
- **T005 (`.editorconfig`) added 2026-09-18** in response to `/speckit-analyze` finding K1, and every task from T006 onward renumbered by one accordingly. Unlike the IX/XIII deferrals (and XIV's shared structured-logging sub-item), this constitution requirement (Technology & Architecture Constraints) is cheap enough (one static file, no dependency/build/CI change) that it's implemented now rather than deferred.
- **Reviewed against constitution v1.6.0 → v1.7.0** (2026-09-18): the two new principles (XV Security by Design, XVI Object-Oriented Design Discipline) required no task changes — see plan.md's Constitution Check rows for both (XV: Pass, already satisfied via the FR-011 `/speckit-clarify` record; XVI: N/A, no class hierarchy exists to check).
- **`/speckit-analyze` remediation (2026-09-18)**: T006 now references FR-011 (it already sends unauthenticated requests; only the traceability reference was missing) and T011 now references FR-004/FR-005/AC-004 (spec.md's previously-unlabeled repeated-call scenario is now `AC-004`). No test behaviour changed — traceability only. Separately, constitution v1.7.0 → v1.8.0 generalized Governance's deviation-approval clause beyond Principle I (see plan.md's re-check log) — no task changes resulted, since T001-T021 already implement everything this feature's Complexity Tracking table doesn't defer.
- **`/speckit-analyze` remediation (2026-09-18, findings E1/E2)**: T011 now also requires a concurrent-call assertion (previously sequential-only), closing the gap where EC-001/EC-002's "multiple monitoring sources simultaneously" language had no concurrent-access test. T012 now also requires a warm-up-then-average assertion anchored to spec.md SC-002's actual `<100ms` figure (previously only a loose `<1s` sanity bound with no task giving SC-002 any real automated signal). Both task IDs are unchanged — only their descriptions were broadened — so no renumbering was needed.
- **`/speckit-analyze` remediation (2026-09-18, findings A1/E3)**: spec.md FR-002's wording was clarified so "described as 'Service is healthy'" unambiguously attaches to the `200` response rather than the `status` field (no behaviour or task change). spec.md FR-006 now carries the same "verified by manual code review" parenthetical pattern already used by FR-009, pointing at T020; T020 itself was broadened to also confirm no dependency is wired into `HealthcheckController`, giving FR-006 an explicit (manual) verification point instead of relying only on the absence of code to call.
- **`/speckit-analyze` remediation (2026-09-18, findings C1/E1/N2)**: plan.md's Constitution Check Principle XI row now names FR-006/FR-009 as documented exceptions to "all behaviour covered by automated tests" (previously an unqualified Pass, with the exception visible only in spec.md — finding C1). T001 now records that a repo scan confirmed no pre-existing healthcheck implementation or build file, resolving spec.md EC-005 for this feature (finding E1). T003's `spring.application.name` was renamed from `elinks-jomockapi` to `ctam-jomockapi` to match the project's established naming (constitution title, base package, repo name — finding N2); no other file referenced the old value.
- **`/speckit-analyze` remediation (2026-09-19, finding U1)**: T001 previously left `springdoc-openapi-starter-webmvc-ui`'s version unpinned ("a version compatible with Spring Boot 4.x") while every other dependency in the same task had an exact version. It now pins `3.1.1` — confirmed via Maven Central as the current release and the first `springdoc-openapi` major line tracking Spring Boot 4.x. spec.md's Key Entities section was also updated (finding D1) to name `HealthResponse` alongside "Health status" for direct traceability to data-model.md, since every other artifact used the implementation name.
- **`/speckit-analyze` remediation (2026-09-19, finding G1)**: T001 listed plugins and dependencies but never declared a Gradle `repositories` block, without which none of T001's dependencies (not just springdoc) could be resolved. T001 now explicitly includes `repositories { mavenCentral() }`.
