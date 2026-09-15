---

description: "Task list template for feature implementation"
---

# Tasks: Reference Data Lookup by Type and ID

**Input**: Design documents from `/specs/002-reference-data-lookup/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md,
contracts/reference-data-by-id.md, quickstart.md

**Tests**: Included. The constitution (Principle XI, Development Workflow & Quality
Gates) mandates unit/controller/contract test coverage for all endpoint behaviour, so
test tasks are required here, not optional.

**Organization**: Tasks are grouped by user story (US1 P1, US2 P2, US3 P3) per
`spec.md`, following Setup → Foundational → User Story → Polish.

**Amendment note (constitution v1.0.0 → v1.1.0, 2026-09-15)**: This task list
originally planned a feature-local `error.GlobalExceptionHandler` created from scratch.
It now reuses the shared `ErrorResponse`/`GlobalExceptionHandler`/`CorrelationIdFilter`
infrastructure established by `001-healthcheck-endpoint` instead (Principle III — no
duplication), per `research.md`'s "Reuse the shared error/observability infrastructure"
decision. All task IDs from T004 onward were renumbered to insert this; see the diff
against the previous version for exact mapping if needed.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths below are relative to the repository root, per `plan.md`'s Project Structure
  (base package `uk.gov.moj.elinks.mock`)
- Tasks marked "skip if already present" account for this feature sharing the single
  application skeleton (and, per the v1.1.0 amendment, the shared error/observability
  infrastructure) with `001-healthcheck-endpoint`, whichever feature is implemented
  first

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Ensure the Maven/Spring Boot project skeleton exists and carries this
feature's new dependencies.

- [ ] T001 Ensure the Maven project skeleton exists: `pom.xml` (Spring Boot 4.1.1
      parent, Java 25 source/target), `src/main/java/uk/gov/moj/elinks/mock/`,
      `src/main/resources/`, `src/test/java/uk/gov/moj/elinks/mock/`, and a
      `.gitignore` covering Maven's `target/` directory. Skip creation if
      `001-healthcheck-endpoint` already established this skeleton — verify it matches
      `plan.md` → Project Structure rather than duplicating it.
- [ ] T002 Ensure `spring-boot-starter-web` and `spring-boot-starter-test` are present
      in `pom.xml` (skip if already added); add the `jackson-dataformat-csv` dependency,
      per `research.md` → CSV/JSON parsing library decision.
- [ ] T003 [P] Ensure `src/main/resources/application.yml` has the default server
      configuration (skip if already present); add a `mock.security.enabled: true`
      property, per `research.md` → Simulated authentication decision.
- [ ] T004 [P] Confirm the build-quality gates established by `001-healthcheck-endpoint`
      (`maven-compiler-plugin` warnings-as-errors, checkstyle, OWASP dependency-check,
      JaCoCo, and the CI workflow) already cover this feature's added dependency
      (`jackson-dataformat-csv`) — run `./mvnw verify` and confirm no new violations; no
      new tooling is needed (Constitution Principle XIII).

**Checkpoint**: Project builds (`./mvnw compile`) with the new dependency resolved.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The generic reference-data registry, bundled data, and loader that every
user story in this feature depends on (Constitution Principle VII), plus confirming
this feature builds on — rather than duplicates — the shared error/observability
infrastructure from `001-healthcheck-endpoint`.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T005 [P] Ensure the Spring Boot application entry point
      `src/main/java/uk/gov/moj/elinks/mock/ElinksMockApplication.java` exists (skip if
      already created by `001-healthcheck-endpoint`).
- [ ] T006 [P] Ensure `server.servlet.context-path: /elinks` is set in
      `src/main/resources/application.yml` (skip if already set by
      `001-healthcheck-endpoint`).
- [ ] T007 [P] Ensure the shared `error.ErrorResponse` record, `error.GlobalExceptionHandler`,
      `tracing.CorrelationIdFilter`, and `logging.LogSanitizer` already exist (created by
      `001-healthcheck-endpoint`, per its `research.md` → Shared error response shape
      and request correlation decision). Do **not** create parallel,
      `referencedata`-scoped equivalents — this feature only extends the existing
      `GlobalExceptionHandler` (T028) and reuses the rest as-is (Constitution
      Principles III, IX, XIV; see this feature's `research.md` → Reuse the shared
      error/observability infrastructure decision).
- [ ] T008 [P] Copy the 11 reference-data extracts from
      `joh-elinks-api/ReferenceData/` into `src/main/resources/referencedata/`,
      renamed per `research.md`'s bundling decision:
      `eLinks_..._REF_AppointmentTitle.csv` → `appointment_titles.csv`,
      `eLinks_..._REF_BaseLocation.csv` → `base_locations.csv`,
      `eLinks_..._REF_ContractType.csv` → `contract_types.csv`,
      `eLinks_..._REF_Gender.csv` → `genders.csv`,
      `eLinks_..._REF_JudiciaryRole.csv` → `judiciary_roles.csv`,
      `eLinks_..._REF_Jurisdiction.json` → `jurisdictions.json`,
      `eLinks_..._REF_LocationType.csv` → `location_types.csv`,
      `eLinks_..._REF_Location.csv` → `locations.csv`,
      `eLinks_..._REF_Ticket.csv` → `tickets.csv`,
      `eLinks_..._REF_TicketCategory.csv` → `ticket_categories.csv`,
      `eLinks_..._REF_TicketCategoryType.csv` → `ticket_category_types.csv`.
- [ ] T009 [P] Create the common `ReferenceDataRecord` interface in
      `src/main/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataRecord.java`
      exposing the shared fields per `data-model.md` → Reference Data Record: `id`
      (long), `name` (String), `startDate` (LocalDate), `endDate` (LocalDate, nullable),
      `createdAt`, `updatedAt` (OffsetDateTime/Instant).
- [ ] T010 [P] Create the 11 per-type record classes implementing
      `ReferenceDataRecord`, in `src/main/java/uk/gov/moj/elinks/mock/referencedata/model/`,
      with the additional fields listed in `data-model.md` → Per-type additional
      fields: `AppointmentTitleRecord.java` (none extra), `GenderRecord.java` (none
      extra), `JudiciaryRoleRecord.java` (none extra), `JurisdictionRecord.java` (none
      extra), `BaseLocationRecord.java` (`typeId`, `parentId` nullable,
      `jurisdictionId`), `LocationRecord.java` (`typeId`, `parentId` nullable,
      `jurisdictionId`), `ContractTypeRecord.java` (`salaried`, nullable boolean),
      `LocationTypeRecord.java` (`jurisdictionId`, `integrationCode`),
      `TicketCategoryTypeRecord.java` (`jurisdictionId`, `integrationCode`),
      `TicketCategoryRecord.java` (`typeId`, `jurisdictionId`, `parentCategoryId`
      nullable), `TicketRecord.java` (`ticketCategoryId`).
- [ ] T011 Create the `ReferenceDataType` enum in
      `src/main/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataType.java` with
      the 11 entries from `data-model.md` → Reference Data Type registry, each carrying
      its canonical (plural) name, deprecated (singular) alias, bundled resource path
      (from T008), and record class (from T010). Depends on T008, T010.
- [ ] T012 Create `ReferenceDataLoader` in
      `src/main/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataLoader.java`
      that, at application startup, parses each `ReferenceDataType`'s bundled resource
      (Jackson CSV for `.csv`, Jackson `ObjectMapper` for the `jurisdictions.json`
      `results` array) into an immutable `Map<Long, ReferenceDataRecord>` keyed by
      `id`, per `research.md`: normalize both source date formats (`dd/MM/yyyy` and
      `yyyy-MM-dd`) to ISO-8601, and exclude any row with a missing/non-numeric `id`
      with a startup warning log (via `LogSanitizer` for any logged raw value) rather
      than failing startup. Depends on T009, T011.

**Checkpoint**: Application starts (`./mvnw spring-boot:run`) and loads all 11 types
into memory (verifiable via startup logs), even though no endpoint exists yet —
foundation ready for User Story 1.

---

## Phase 3: User Story 1 - Retrieve a single reference-data record by type and ID (Priority: P1) 🎯 MVP

**Goal**: `GET /elinks/api/v5/reference_data/{attribute_name}/{reference_id}` returns
the matching record (200) for any of the 11 canonical type names and an existing
numeric ID, returns 404 when the ID doesn't exist in that type, and requires simulated
authentication (401 when missing/invalid), per `contracts/reference-data-by-id.md`.

**Independent Test**: `quickstart.md` scenarios 1–3 — successful lookup (e.g.
`jurisdictions`/`30`), unknown ID within a valid type (404), and missing authentication
(401) — using canonical type names only.

### Tests for User Story 1 ⚠️

> Write these tests FIRST; confirm they FAIL (404/no controller) before implementing.

- [ ] T013 [P] [US1] Contract test in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataContractTest.java`
      asserting, per `contracts/reference-data-by-id.md`: path/method, a successful
      canonical-name lookup returns `200` with `application/json` and an
      `X-Correlation-Id` header; an unknown ID returns `404` with the shared
      `ErrorResponse` body shape; a request without credentials returns `401` with the
      shared `ErrorResponse` body shape.
- [ ] T014 [P] [US1] Controller/API test (Spring `MockMvc`) in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataControllerTest.java`
      covering, per `spec.md` Acceptance Scenarios and Edge Cases: (a) `200` with full
      field set for a JSON-sourced type (`jurisdictions/30`) and a CSV-sourced type
      with extra fields (e.g. `ticket_categories/31`); (b) a record with a null
      `end_date` still returns that field as null/absent, not omitted; (c) a record
      whose `start_date`/`end_date` window excludes today is still returned (FR-012);
      (d) an unknown ID within a valid type returns `404` with the shared
      `ErrorResponse` body (`message`, `timestamp`, `traceId`); (e) a missing
      `Authorization` header returns `401` with the shared `ErrorResponse` body; (f) a
      blank/empty bearer token returns `401`; (g) a malformed `Authorization` header
      (e.g. `Authorization: sometoken` with no `Bearer` scheme, or `Authorization:
      Basic xyz`) returns `401`, giving "invalid credentials" (FR-009) a concrete,
      tested meaning distinct from "missing" (analysis finding U1); (h) calling the
      same valid type/ID twice in a row returns byte-for-byte identical response
      bodies (FR-010, analysis finding E2); (i) requesting the path with the
      `reference_id` segment absent (e.g.
      `GET /elinks/api/v5/reference_data/jurisdictions/`) does not return a successful
      lookup (FR-004, analysis finding E1); (j) every response in (a)–(i) carries an
      `X-Correlation-Id` header, and on error responses the body's `traceId` matches it
      (Constitution Principle XIV).
- [ ] T015 [P] [US1] Unit test in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataServiceTest.java`
      for `findByTypeAndId`: existing ID returns the record, unknown ID returns empty,
      and the same numeric ID in two different types (e.g. `genders`/`1` vs
      `locations`/`1`) returns the correctly scoped, distinct record for each (Edge
      Cases, `spec.md`).
- [ ] T016 [P] [US1] Unit test in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataLoaderTest.java`
      verifying each of the 11 types loads at least one known sample record with the
      expected field values from the bundled source data, and that the malformed row
      in `contract_types.csv` (blank `id`) is excluded rather than loaded.
- [ ] T017 [P] [US1] Unit test in
      `src/test/java/uk/gov/moj/elinks/mock/security/SimulatedAuthInterceptorTest.java`
      verifying: a request with a non-blank `Authorization: Bearer <token>` header is
      allowed through; a missing header is rejected with `401` and the shared
      `ErrorResponse` body; a blank/empty token is rejected with `401`; a header with
      no `Bearer` scheme (e.g. `Authorization: sometoken` or `Authorization: Basic
      xyz`) is rejected with `401` (analysis finding U1); when
      `mock.security.enabled=false`, a request with no header is allowed through.

### Implementation for User Story 1

- [ ] T018 [US1] Create `ReferenceDataTypeResolver` in
      `src/main/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataTypeResolver.java`
      resolving a raw `attribute_name` string to a `ReferenceDataType` by exact match
      against each type's canonical name only for now (deprecated-alias matching is
      added in User Story 2); returns empty when nothing matches. Depends on T011.
- [ ] T019 [US1] Create `ReferenceDataService` in
      `src/main/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataService.java`
      with `findByTypeAndId(ReferenceDataType type, long id): Optional<ReferenceDataRecord>`
      delegating to `ReferenceDataLoader`'s in-memory maps (FR-005, FR-006, FR-011).
      Depends on T012.
- [ ] T020 [US1] Create `SimulatedAuthInterceptor` in
      `src/main/java/uk/gov/moj/elinks/mock/security/SimulatedAuthInterceptor.java`
      requiring a non-blank `Authorization: Bearer <token>` header when
      `mock.security.enabled=true`; on rejection, write the shared `ErrorResponse`
      (T007) as the `401` response body directly (a `HandlerInterceptor` does not go
      through `GlobalExceptionHandler`), reading the trace ID from MDC per
      `research.md` → 401 response body decision. Register it via a `WebMvcConfigurer`
      in `src/main/java/uk/gov/moj/elinks/mock/security/SecurityWebConfig.java` applied
      to `/api/v5/reference_data/**` (FR-009). Depends on T003, T007.
- [ ] T021 [US1] Create `ReferenceDataController` in
      `src/main/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataController.java`
      with `@GetMapping("/api/v5/reference_data/{attributeName}/{referenceId}")`
      (`referenceId` typed `Long`) that resolves the type (T018), looks up the record
      (T019), and returns `200` with the record as JSON, or throws a
      `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` when the type or the ID does
      not resolve (FR-005, FR-006, FR-007) so the shared `GlobalExceptionHandler` (T007)
      produces the `ErrorResponse` body. Depends on T018, T019, T020.
- [ ] T022 [US1] Run T013–T017 and fix any failures until all pass.

**Checkpoint**: User Story 1 is fully functional and independently testable — this is
the feature's MVP.

---

## Phase 4: User Story 2 - Look up a record using a deprecated (legacy) type name (Priority: P2)

**Goal**: Requesting a record using any of the 11 deprecated singular type names
returns the identical record as its current plural equivalent, per FR-003.

**Independent Test**: `quickstart.md` scenario 4 — request the same ID once via
`jurisdictions` and once via `jurisdiction`, diff the two response bodies.

### Tests for User Story 2 ⚠️

- [ ] T023 [P] [US2] Controller/API test in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataControllerTest.java`
      asserting, for each of the 11 type pairs in `data-model.md` → Reference Data Type
      registry, that a lookup via the deprecated alias returns a response body
      byte-for-byte identical to the same ID via the canonical name.
- [ ] T024 [P] [US2] Unit test in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataTypeResolutionTest.java`
      verifying `ReferenceDataTypeResolver` resolves both the canonical and deprecated
      name to the same `ReferenceDataType` for all 11 entries, and still returns empty
      for a name that is neither.

### Implementation for User Story 2

- [ ] T025 [US2] Extend `ReferenceDataTypeResolver` (T018) to also match each type's
      deprecated alias field on `ReferenceDataType`, per FR-003 and Constitution
      Principle VII (alias resolution centralised in one place). Depends on T018.
- [ ] T026 [US2] Run T023–T024 and fix any failures until all pass.

**Checkpoint**: User Stories 1 and 2 both work independently.

---

## Phase 5: User Story 3 - Receive predictable errors for unsupported types or unknown IDs (Priority: P3)

**Goal**: A malformed (non-numeric) `reference_id` returns `400 Bad Request`, distinct
from the `404` used for an unsupported type name or an unknown-but-numeric ID, per
FR-007 and FR-008.

**Independent Test**: `quickstart.md` scenarios 5–6 — an unsupported type name (404)
and a non-numeric `reference_id` (400).

### Tests for User Story 3 ⚠️

- [ ] T027 [P] [US3] Controller/API test in
      `src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataControllerTest.java`
      asserting: a non-numeric `reference_id` path segment (e.g. `not-a-number`)
      returns `400 Bad Request` with the shared `ErrorResponse` body shape; a type name
      that is neither a current nor a deprecated value (e.g. `not_a_real_type`) returns
      `404 Not Found`.

### Implementation for User Story 3

- [ ] T028 [US3] Add one new handler method to the **existing, shared**
      `src/main/java/uk/gov/moj/elinks/mock/error/GlobalExceptionHandler.java`
      (established by `001-healthcheck-endpoint`; do not create a new handler class,
      per `research.md` → Reuse the shared error/observability infrastructure
      decision) mapping a path-variable type-mismatch on `referenceId` to `400 Bad
      Request` with the shared `ErrorResponse` body (FR-008), per Constitution
      Principle IX — centralised, not a per-controller check. Depends on T007.
- [ ] T029 [US3] Run T027 and fix any failures until it passes (the unsupported-type
      `404` case should already pass unchanged from User Story 1's resolver
      behaviour — this task confirms it, it does not require new production code for
      that half of the scenario).

**Checkpoint**: All three user stories are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation against the documented contract and quickstart.

- [ ] T030 [P] Run through `quickstart.md` end-to-end against the running application
      (`./mvnw spring-boot:run`), executing all 7 scenarios and confirming every
      expected status/body (including the shared `ErrorResponse` shape on every error
      and an `X-Correlation-Id` header on every response), timing scenario 1's `curl`
      call (`curl -w "%{time_total}\n"`) to confirm it completes well under one second
      (SC-004).
- [ ] T031 Verify the application startup logs include the malformed-row warning for
      `contract_types.csv` (T012/T016) and confirm that row's data is not retrievable
      via any `reference_id`.
- [ ] T032 [P] Verify `mock.security.enabled` is genuinely configurable (Principle X):
      set it to `false`, restart, confirm scenario 3 from `quickstart.md` (missing
      auth) now returns `200` instead of `401`, then restore it to `true`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001–T004) — blocks Phase 3.
- **User Story 1 (Phase 3)**: Depends on Foundational (T005–T012).
- **User Story 2 (Phase 4)**: Depends on User Story 1 (T018, the resolver it extends).
- **User Story 3 (Phase 5)**: Depends on User Story 1 (T021, the controller whose error
  paths it completes) and on Foundational (T007, the shared handler it extends);
  independent of User Story 2.
- **Polish (Phase 6)**: Depends on all three user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: No dependency on other stories — this is the MVP.
- **User Story 2 (P2)**: Extends US1's resolver (T018) rather than duplicating it;
  cannot be meaningfully tested until US1's controller/service exist.
- **User Story 3 (P3)**: Extends the shared `GlobalExceptionHandler` (T007/T028), not
  US1's controller itself; independent of User Story 2 (touches a different concern —
  malformed input vs. alias names).

### Within Each User Story

- Tests MUST be written and MUST fail before implementation.
- Registry/resolver/service/interceptor before the controller that wires them together.
- Story complete (tests passing) before moving to the next priority.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel once T001 exists (different files/verification).
- T005, T006, T007, T008, T009 can run in parallel once Setup is complete (different
  files/verification).
- T010's 11 record classes can be written in parallel with each other and with
  T005–T009 (all different files); T011 depends on T008 and T010.
- T013–T017 (all US1 tests) can run in parallel once Foundational is complete (different
  files, no dependencies on each other).
- T023–T024 (US2 tests) can run in parallel with each other.

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Contract test in src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataContractTest.java"
Task: "Controller/API test in src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataControllerTest.java"
Task: "Service unit test in src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataServiceTest.java"
Task: "Loader unit test in src/test/java/uk/gov/moj/elinks/mock/referencedata/ReferenceDataLoaderTest.java"
Task: "Auth interceptor unit test in src/test/java/uk/gov/moj/elinks/mock/security/SimulatedAuthInterceptorTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T004).
2. Complete Phase 2: Foundational (T005–T012) — CRITICAL, blocks all stories.
3. Complete Phase 3: User Story 1 (T013–T022).
4. **STOP and VALIDATE**: run `quickstart.md` scenarios 1–3 against the running app.
5. Deploy/demo if ready — this alone satisfies FR-002, FR-005, FR-006, FR-009, FR-010,
   FR-011, FR-012 for all 11 canonical type names.

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. Add User Story 1 → test independently → MVP.
3. Add User Story 2 → test independently (deprecated-alias parity).
4. Add User Story 3 → test independently (malformed-ID handling).
5. Polish (Phase 6) → full quickstart validation.

---

## Notes

- Per Constitution Principle VIII, no `Map<String, Object>` response shape is used
  anywhere — every type has its own typed record class (T010) sharing common fields via
  `ReferenceDataRecord` (T009).
- Per Constitution Principle VII, all alias/type-resolution logic lives in exactly one
  place (`ReferenceDataTypeResolver`), extended in place (T025) rather than duplicated.
- Per Constitution Principle IX (amended v1.1.0) and Principle III, authentication
  (T020), malformed-input handling (T028), and not-found handling (T021) are all
  centralised and all produce the one shared `ErrorResponse` shape — this feature
  extends the `GlobalExceptionHandler` from `001-healthcheck-endpoint` rather than
  creating its own.
- Per Constitution Principle XIV (added v1.1.0), every response from this feature
  carries the correlation ID via the shared `CorrelationIdFilter` from
  `001-healthcheck-endpoint` — no new tracing code is needed here.
- Commit after each task or logical group.
