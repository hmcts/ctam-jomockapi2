# CLAUDE.md

Guidance for Claude Code working in this repository.

## What this project is

A mock implementation of the **E-Links API** (Ministry of Justice / judiciary.uk) — a Java/Spring Boot service standing in for the real E-Links API for integration testing and development, serving synthetic data only. See `README.md` for the full overview.

## Source of truth

**`.specify/memory/constitution.md` is the authoritative source** for architecture, tech stack, layering, testing, and quality-gate rules — not this file. Read it before making any non-trivial change. Key pins as of the current version:

- Java 25, Spring Boot 4.1.1, built with **Gradle** (via the wrapper) — never Maven
- Package-by-layer under base package `uk.gov.hmcts.ctam.jo` (`controllers/`, `services/`, `repository/`, `mappers/`, `domain/`, `entity/`, `exceptions/`, `filters/`, `config/`) — not package-by-feature
- Controllers stay thin; business logic lives in services
- Typed DTOs everywhere (no `Map<String, Object>`); Lombok for boilerplate, MapStruct for mapping
- One shared error response shape via a single `GlobalExceptionHandler`
- Every request gets a correlation ID (`X-Correlation-Id`), except health/readiness/info/metrics-style endpoints, which are explicitly exempted
- All significant behaviour MUST have automated tests (unit, controller/API, contract)
- Every endpoint MUST appear in a generated (springdoc) OpenAPI spec, kept in step with the code, with Swagger UI exposed (Principle XVIII)
- Every endpoint MUST have Postman artifacts in `postman/` (collection + environment, with test scripts and no hard-coded environment values), updated in the same feature (Principle XVII)
- Gradle dependency locking is on: after changing dependencies, regenerate `gradle.lockfile` with `./gradlew dependencies --write-locks`

The constitution is currently at **v1.13.0** (last amended 2026-10-16).

If a change would conflict with the constitution, that's a blocker. Never work around it silently, and never amend the constitution on your own initiative. Instead:

1. **Stop and ask first.** Explain the conflict, which principle(s) it touches, the amendment you propose (the wording, the version bump and why), and what else it would affect (specs, plans, tasks, code). Wait for explicit human approval before editing `.specify/memory/constitution.md` or running `/speckit-constitution`.
2. **Apply only the approved change.** Once approved, make exactly that amendment via `/speckit-constitution`, with no other changes bundled in, and keep it in its own commit so the diff is easy to review.
3. **Get it reviewed.** Show the resulting diff and the version/date changes, and treat the amendment as provisional until a human has reviewed it (for example in the PR). Don't build dependent work on it until then.

This applies even when an amendment looks minor or obvious (a PATCH-level wording fix, for example). Constitution changes are always explicit, deliberate and human-reviewed.

## Development workflow

This repo is built feature-by-feature using the [Spec Kit](https://github.com/github/spec-kit) workflow. Each feature lives under `specs/<feature-name>/` with its own `spec.md`, `plan.md`, `tasks.md`, and supporting design docs. Typical flow for a new feature:

```
/speckit-specify   → write/refine the spec
/speckit-clarify    → resolve ambiguities
/speckit-plan       → produce the implementation plan
/speckit-tasks      → generate the task list
/speckit-analyze    → cross-check spec/plan/tasks/constitution before implementing
/speckit-implement  → execute the tasks
```

Always run `/speckit-analyze` before `/speckit-implement` — it catches coverage gaps, terminology drift, and constitution conflicts across the three core artifacts.

## Current repo state

- **`001-healthcheck-api` — done, merged to `main`.** Gradle/Spring Boot skeleton, `GET /api/v1/healthcheck` → `{"status": "ok"}` (unauthenticated, exempt from correlation IDs), springdoc OpenAPI/Swagger UI, Postman collection and environment, and `start.sh`.
- **`002-reference-data-api` — implemented** (branch `002-reference-data-api`). `GET /api/v1/reference_data/{attribute_name}` and `GET /api/v1/reference_data/{attribute_name}/{reference_id}`, serving `appointment_titles` (194 synthetic records in `src/main/resources/reference-data/`) and the deprecated alias `appointment_title`. Types are declared in `jo.reference-data.types` in `application.yml`; adding one needs only a config entry and a fixture (see README "Adding a reference-data type"). It also built the shared infrastructure:
  - Bearer-token auth (`BearerTokenAuthenticationFilter`, tokens from `jo.security.bearer-tokens` / `JO_SECURITY_BEARER_TOKENS`; can't be turned off)
  - correlation IDs (`CorrelationIdFilter`, `X-Correlation-Id`), structured logstash JSON logging, and `LogSanitiser` for caller values
  - one `ErrorResponse` shape from `ErrorResponseFactory`, used by `GlobalExceptionHandler`, the filters and `JsonErrorController` (`/error`)
  - Lombok/MapStruct, `-Werror`, Checkstyle (HMCTS plugin), OWASP dependency-check, JaCoCo, Sonar config, `dependencyUpdates`, and CI/CodeQL workflows in `.github/workflows/`
- **`003-base-locations-reference-data` — implemented** (branch `003-base-locations-reference-data`). Adds `base_locations` through configuration and a fixture only: 1,462 synthetic records, one per source row (1,276 distinct names, since different locations share names). No deprecated alias is served, so `base_location` returns `400` (constitution v1.11.0, Principle VII). Records also carry the extract's E-Links `type_id`, `parent_id` and `jurisdiction_id` (amendment 2026-10-13, FR-018): forward references to `location_types`, `locations` and `jurisdictions`, allowed by constitution v1.12.0 (Principle IV) and recorded as DF-1, so the features adding those types must test that every reference resolves. Two generic changes: a type can allow repeated names with `unique-names: false` (FR-017; default stays unique, `base_locations` opts out), and a type can declare `reference-fields` (any of `type_id`, `parent_id`, `jurisdiction_id`; modelled by the `ReferenceField` enum), validated at start-up and written in responses only when present, so `appointment_titles` responses are unchanged. Adds `ReferenceDataFixtureRulesTest` (generation rules for every shipped fixture), widens `NoTypeSpecificCodeTest` to `base_location`, and moves the functional HTTP helpers into `src/functionalTest/.../testsupport/ReferenceDataHttp`.
- **`004-contract-types-reference-data` — implemented** (branch `004-contract-types-reference-data`). Adds `contract_types` through configuration and a fixture: 14 synthetic records generated from the extract's names and `salaried` flag, with 002's ids and dates (the extract's own end dates are not reproduced). No deprecated alias is served, so `contract_type` returns `400`. One generic change: a type can declare `boolean-fields` (currently only `salaried`; modelled by the `BooleanField` enum), read strictly as JSON booleans at start-up and written in responses only when present, so `appointment_titles` and `base_locations` responses are unchanged; `salaried` is contract deviation D-3. DF-2: the People/appointments feature must point `contract_type_id`/`contract_type` at these records and test that every reference resolves. Widens `NoTypeSpecificCodeTest` to `contract_type`.
- **`005-genders-reference-data` — implemented** (branch `005-genders-reference-data`). Adds `genders` through configuration and a fixture only, with no main-code change: 4 synthetic records (Female, Male, Other, Prefer not to say) generated from the extract's names with 002's ids and dates, so none is ended. 002's FR-023 is read across all shipped default data, so no waiver is needed. No deprecated alias is served, so `gender` returns `400`. DF-3: the People feature must point `gender_id` at these records, test that every reference resolves, and add a reviewed `NoTypeSpecificCodeTest` exception for its own `gender`/`gender_id` fields. Widens `NoTypeSpecificCodeTest` to `gender`, with no exemption.
- **Test suites** (Gradle JVM Test Suite plugin), all run by `check`: `test` (unit and `@WebMvcTest`, `src/test`), `integrationTest` (full context and OpenAPI contract tests), `functionalTest` (every acceptance scenario over real HTTP), `smokeTest` (latency guards over live HTTP calls, each call also asserting `200`). The full-context suites use `@ActiveProfiles("test")` with token `test-token`.
- `NoTypeSpecificCodeTest` fails the build if any main source file (comments included) names a specific reference-data type in any spelling (snake_case, spaced words or camelCase), or if anything other than `ReferenceDataTypeRegistry` reads aliases.

## Commands

```bash
./start.sh                                   # build and run on http://localhost:8080 (foreground; wraps ./gradlew bootRun)
./gradlew bootRun                            # run the application locally
./gradlew check                              # every quality gate: -Werror compile, Checkstyle, all four suites, JaCoCo report + coverage gate (90% lines, 80% branches), OWASP
./gradlew check -PskipOwasp                  # the same without the slow OWASP/NVD scan (local only; CI never skips it)
./gradlew test                               # unit suite
./gradlew integrationTest                    # full-context and OpenAPI contract tests
./gradlew functionalTest                     # acceptance scenarios over real HTTP
./gradlew smokeTest                          # latency guards (add -Dperf.strict=true for the 100 ms p95 target)
./gradlew dependencyUpdates                  # dependency freshness report
./gradlew build                              # full build (compile + test)
./gradlew dependencies --write-locks         # regenerate gradle.lockfile after dependency changes
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json   # run the Postman checks against a running instance
```

With the app running: Swagger UI at `http://localhost:8080/swagger-ui/index.html`, OpenAPI document at `http://localhost:8080/v3/api-docs`.

## Reference data

Reference extracts (the E-Links Swagger PDF and reference-data files) are local-only reference material. They are gitignored and never committed, and the build doesn't depend on them. Only public names (appointment titles, base-location, contract-type and gender names), the base locations' `type_id`, `parent_id` and `jurisdiction_id` reference codes and the contract types' `salaried` flag were taken from them, once each, to generate the checked-in synthetic fixtures (see `specs/002-reference-data-api/data-model.md` § Fixture generation rules, `specs/003-base-locations-reference-data/data-model.md` § 3, `specs/004-contract-types-reference-data/data-model.md` § 3 and `specs/005-genders-reference-data/data-model.md` § 3).
