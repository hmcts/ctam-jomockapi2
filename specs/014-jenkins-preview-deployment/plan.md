# Implementation Plan: Jenkins Pipeline and Preview Deployment

**Branch**: `014-jenkins-preview-deployment` | **Date**: 2026-11-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-jenkins-preview-deployment/spec.md`

## Summary

Make the repository buildable and deployable by the shared HMCTS Jenkins Java pipeline (product `ctam`, component `jomockapi`), with pull requests deployed to the preview environment and tested there. Nothing a caller of the API can observe changes.

The work is mostly new files: `Jenkinsfile_CNP`, `Jenkinsfile_nightly`, `settings.gradle`, `Dockerfile`, `.dockerignore`, `lib/applicationinsights.json` and the `charts/ctam-jomockapi` chart. Three existing areas change:

1. **`build.gradle`**: a fixed boot-jar name; when `JENKINS_URL` is set, `check` stops running the OWASP scan and the NVD-key guard stands down (the pipeline's Security stage scans against the shared database); and two new `Test` tasks, `smoke` and `functional`, that run existing compiled tests against `TEST_URL` with `TEST_BEARER_TOKEN`.
2. **Test support**: a `TestTarget` helper (address and token, each overridable by a system property) lets the same scenarios run in-process (unchanged default) or against a deployed copy. A new `DeploymentSmokeTest` checks availability and authentication, with no timing.
3. **Documentation**: README and CLAUDE.md.

The bearer tokens reach the deployed service from Key Vault `ctam-aat`, mounted by the chart and imported with Spring's `configtree`, so no application code or `application.yml` changes. Research found that the library only deploys `master` to AAT and production, and this repository uses `main`, so "preview only" needs no extra configuration (research R-2).

## Technical Context

**Language/Version**: Java 25 (toolchain); Groovy (Jenkinsfiles, Gradle DSL); YAML (Helm)

**Primary Dependencies**: Spring Boot 4.1.1 (unchanged). New external inputs, not Gradle dependencies: `cnp-jenkins-library` 2.13.5 (pinned), HMCTS `java` chart 6.1.1, base image `hmctspublic.azurecr.io/base/java:25-distroless`.

**Storage**: N/A. One Key Vault secret, provisioned outside the repository.

**Testing**: JUnit 5 suites as today. `smoke` and `functional` reuse the `smokeTest` and `functionalTest` compiled classes against a deployed address. Local validation in quickstart steps 1 to 5; Jenkins validation in steps 6 to 8.

**Target Platform**: HMCTS AKS preview environment, through HMCTS Jenkins (CNP). GitHub Actions stays the merge gate.

**Project Type**: Web service (existing). This feature changes build, packaging and deployment only.

**Performance Goals**: None new. The deployed-copy smoke tests have no response-time limit (clarification 2026-11-03); the in-process latency guards stay.

**Constraints**: No change to API behaviour, OpenAPI or Postman (FR-022). No change to GitHub Actions or CodeQL (FR-023). No Terraform (clarification). `check` keeps its four suites and coverage gate. Measured start-up is about 1 s and resident memory about 330 MB (research R-8), within the chart's defaults.

**Scale/Scope**: About 12 new files and 8 edited files. 272 existing functional test methods are expected to run unchanged against preview.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design below.*

Checked against constitution v1.13.0.

| Principle | Assessment |
|---|---|
| I. Contract-First Development | **Pass.** No endpoint, field or status changes (FR-022). |
| II. Java Spring Boot Architecture | **Pass.** No main-code change. Platform probes use the hand-rolled `GET /api/v1/healthcheck` for readiness, liveness *and* startup; no Actuator is added (research R-7). |
| III. Reuse Before Duplication | **Pass.** Deployed-copy runs reuse the existing scenarios through `TestTarget`/`TestTokens`. No second copy of any test (research R-6). |
| IV. Synthetic Data Only | **Pass.** The preview serves the shipped synthetic fixtures (FR-013). The whitelist `.dockerignore` keeps the local-only extracts in `joh-elinks-api/` out of the image (EC-008). |
| V. Deterministic Behaviour | **Pass.** The same fixtures and reference date are deployed, so golden-file comparisons hold behind the ingress. |
| VI. Behavioural Fidelity | **Pass.** Unchanged. |
| VII. Generic Reference-Data Handling | **Pass.** Unchanged; `NoTypeSpecificCodeTest` is unaffected because no main source changes. `DeploymentSmokeTest` is test code and names one route through the existing `ContractPaths`. |
| VIII. Typed API Models | **Pass.** Unchanged. |
| IX. Centralised Validation and Error Handling | **Pass.** Unchanged. |
| X. Configuration Over Hard-Coding | **Pass.** Deployment values come from the chart (environment variable and mounted secret), test targets from environment variables and system properties. Nothing is environment-specific in code. |
| XI. Testability | **Pass (planned).** `DeploymentSmokeTest` runs in-process in `smokeTest` and against preview in `smoke`. All functional scenarios run against preview. Gradle-level behaviour (fixed jar name, scan skipped under Jenkins, missing-variable failures) is validated by quickstart steps 2 and 4. |
| XII. Simplicity and Maintainability | **Pass.** Standard HMCTS template layout, no new Gradle dependency, no infrastructure code. The one trade-off (an idle in-process context during deployed-copy runs) is recorded in research R-6. |
| XIII. Automated Quality Gates | **Pass.** GitHub Actions still runs the full `check` including OWASP on every PR and `main` push, and CodeQL is untouched. On Jenkins the scan runs once, in the Security stage, at a stricter threshold (any CVE, research R-4), so every build path still scans. Inherits 002's open Sonar deferral, which may now also affect Jenkins (research R-11). |
| XIV. Observability & Traceability | **Pass.** Structured logging to stdout unchanged. The Application Insights agent ships without a connection string (no telemetry until DF-12). Correlation IDs are unaffected. |
| XV. Security by Design | **Pass.** Deployed tokens come only from Key Vault. `local-dev-token` is refused (checked on every preview run). A missing or empty secret stops start-up instead of falling back. No new unauthenticated route; the preview uses HMCTS's standard ingress arrangement (FR-014). |
| XVI. Object-Oriented Design Discipline | **Pass.** `TestTarget` is a small single-purpose helper. |
| XVII. Postman Artifacts | **Pass.** No endpoint change, so no Postman change. The environment's `baseUrl` and `bearerToken` variables already allow pointing it at a kept preview copy. |
| XVIII. API Contract and Swagger/OpenAPI | **Pass.** No change. Swagger UI and `/v3/api-docs` are served by the preview copy as locally. |

**Non-principle checks**:
- **CLAUDE.md "Source of truth" rule**: no constitution conflict, so no amendment is needed.
- **Earlier artifacts touched**: `TestTokens` (testFixtures) changes from a constant to a property with the same default; `ReferenceDataHttp` and the three smoke classes build their address through `TestTarget`. README gains a "Jenkins and preview deployment" section, and CLAUDE.md gains the 014 entry and the new commands.

**Result**: No violations. Nothing for Complexity Tracking beyond the inherited Sonar deferral.

**Post-design re-check (after Phase 1)**: Still passes. The design adds no dependency, layer or main-code change. The only behavioural difference between build paths (OWASP inside `check` on Jenkins) keeps the scan on every path and is contracted in `contracts/pipeline-interface.md`.

## Project Structure

### Documentation (this feature)

```text
specs/014-jenkins-preview-deployment/
├── spec.md
├── plan.md                         # this file
├── research.md                     # R-1 … R-13
├── data-model.md                   # settings, rules ST-1 … ST-6, preview lifecycle
├── quickstart.md                   # local steps 1–5, Jenkins steps 6–8
├── contracts/
│   ├── pipeline-interface.md       # files, Gradle tasks, messages, external prerequisites
│   └── deployment-values.md        # image and chart contract
├── checklists/requirements.md
└── tasks.md                        # /speckit-tasks
```

### Source Code (repository root)

```text
Jenkinsfile_CNP                         # NEW  withPipeline("java","ctam", "jomockapi"), vault override, TEST_BEARER_TOKEN
Jenkinsfile_nightly                     # NEW  weekday cron, withNightlyPipeline, afterSuccess('build') → check
settings.gradle                         # NEW  rootProject.name = 'ctam-jomockapi'
build.gradle                            # EDIT bootJar name; onJenkins → no OWASP in check, guard stands down;
                                        #      tasks smoke + functional (TEST_URL / TEST_BEARER_TOKEN)
Dockerfile                              # NEW  base/java:25-distroless, port 8080
.dockerignore                           # NEW  whitelist: jar + lib/applicationinsights.json
lib/applicationinsights.json            # NEW  role ctam-jomockapi, no connection string
charts/ctam-jomockapi/
├── Chart.yaml                          # NEW  java 6.1.1
├── values.yaml                         # NEW  port, probes, keyVaults ctam, SPRING_CONFIG_IMPORT
└── values.preview.template.yaml        # NEW  ${IMAGE_NAME}, ${SERVICE_FQDN}
src/testFixtures/java/uk/gov/hmcts/ctam/jo/testsupport/
└── TestTarget.java                     # NEW  jo.test.base-url or http://localhost:<port>; jo.test.token or TestTokens.TOKEN
src/functionalTest/java/uk/gov/hmcts/ctam/jo/testsupport/ReferenceDataHttp.java   # EDIT address via TestTarget
src/smokeTest/java/uk/gov/hmcts/ctam/jo/
├── DeploymentSmokeTest.java            # NEW  health, one reference-data route, one People route, 401s
├── HealthcheckSmokeTest.java           # EDIT address via TestTarget
├── ReferenceDataSmokeTest.java         # EDIT address via TestTarget
└── PeopleSmokeTest.java                # EDIT address via TestTarget
README.md                               # EDIT Jenkins & preview section, prerequisites, manual runs
CLAUDE.md                               # EDIT 014 entry, commands
```

**Structure Decision**: The existing single Gradle project. New deployment files follow the HMCTS `spring-boot-template` layout (`Jenkinsfile_*`, `Dockerfile`, `lib/`, `charts/<product>-<component>/`). Test changes stay in the existing suites and test fixtures; there is no new source set.

## Implementation order (for `/speckit-tasks`)

1. **Foundation**: `settings.gradle`, boot-jar name, then the `JENKINS_URL` handling of OWASP and the guard. Run quickstart steps 1 and 2.
2. **US3 test plumbing**: `TestTarget`, `TestTokens`, the four address call sites, `DeploymentSmokeTest`, then the `smoke` and `functional` tasks. Run quickstart steps 1 and 4 (against `./start.sh` until the image exists).
3. **US2 packaging**: `Dockerfile`, `.dockerignore`, `lib/applicationinsights.json`, then the chart. Run quickstart steps 3, 4 and 5.
4. **US1 pipeline**: `Jenkinsfile_CNP`, `Jenkinsfile_nightly`.
5. **US4 documentation**: README and CLAUDE.md.
6. **US5 regression**: full `./gradlew check` with OWASP and the Postman collection against `./start.sh`.
7. **On Jenkins** (needs the prerequisites): quickstart steps 6 to 8. Close the verification items: vault name (R-7), Sonar stage (R-11), nightly callback form (R-9), ingress-only failures (R-6/EC-006), App Insights agent start-up (R-5, if not already settled by step 3).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Build-script behaviour (FR-005, FR-018) is verified by recorded `--dry-run` runs (T010, T020) and on Jenkins (T030), not by an automated test (Principle XI) | A nested Gradle build inside `check` (TestKit) contends for this project's own locks or re-downloads every plugin, and FR-023 rules out a CI-only step | The behaviour is three conditions in `build.gradle`, and every Jenkins PR run checks its effect (T030: no scan in Test, one in Security). Revisit if the logic grows (decision 2026-11-03, `/speckit-analyze` C1) |

The inherited item is 002's Sonar deferral ("Sonar step in CI stays inactive"). If the Jenkins Sonar stage is active it will meet the same Automatic Analysis conflict (research R-11), to be raised with the team, not worked around.

## Implementation notes

- **Probe paths**: set all three of `readinessPath`, `livenessPath` and `startupPath`. Missing the startup probe leaves the pod probing `/health/liveness`, which this service doesn't serve (`404`), so the pod never becomes started and is restarted.
- **`SPRING_CONFIG_IMPORT` must not be `optional:`**. That is what makes a missing mount fail closed (ST-1).
- **`TEST_BEARER_TOKEN` may hold several comma-separated tokens**; the Gradle tasks pass only the first to the tests.
- **`smoke`/`functional` must be never up to date** (`outputs.upToDateWhen { false }`) as well as being run with `--rerun-tasks` by the pipeline, so a developer's repeated manual runs also execute.
- **Jacoco** attaches to the new `Test` tasks automatically; their execution data is not added to the merged report or the coverage gate.
- **Chart version**: raise `Chart.yaml` `version` in any later PR that touches the chart (ST-6).
- **Security stage threshold**: expect the first Jenkins Security run to fail on any CVE below 7.0 that GitHub Actions has been tolerating. Fix it by upgrading, or record it in `config/owasp/suppressions.xml` with a reason.

### Baseline (T001)

`./gradlew check -PskipOwasp` on `a783761` (before any 014 change), 2026-11-03: passed.

| Suite | Tests | Failures | Skipped |
|---|---:|---:|---:|
| test | 835 | 0 | 0 |
| integrationTest | 47 | 0 | 0 |
| functionalTest | 718 | 0 | 0 |
| smokeTest | 16 | 0 | 0 |

Merged JaCoCo: lines 1246/1265 = 98.50%, branches 461/493 = 93.51%.

(The functional count is 718 test executions; research R-6's "272" counted annotated test methods, many of which are parameterised.)

### Deployed-copy test tasks (T004 to T010)

- `TestTokens` could not become property-driven: `@TestPropertySource(properties = "jo.security.bearer-tokens=" + TOKEN)` in three `@WebMvcTest` classes needs a compile-time constant. The override moved to `TestTarget.token()`/`bearerToken()`; `TestTokens` is unchanged (research R-6 updated).
- T010 found one functional scenario that hard-coded `"Bearer test-token"` (`PeopleFunctionalTest.otherMethodsAre405AndUnacceptableMediaTypes406`); it failed (401 instead of 405) only against a copy with a different token. Fixed to use `TestTarget.bearerToken()`.
- Results, 2026-11-03, against `java -jar build/libs/ctam-jomockapi.jar` (port 8080 was taken by another local copy, so 18089/18090 were used):
  - `./gradlew smoke` with no variables fails before running tests: `... Missing: TEST_URL, TEST_BEARER_TOKEN`; with only `TEST_URL`: `Missing: TEST_BEARER_TOKEN`.
  - Copy with the default token, `TEST_URL=http://localhost:18089/ TEST_BEARER_TOKEN=local-dev-token,other`: `smoke` 5 tests, 1 skipped (`localDevelopmentTokenIsRefused`), 0 failures; `functional` 718 tests, 0 failures (equal to `functionalTest`).
  - Copy started with `--jo.security.bearer-tokens=demo-token`, `TEST_BEARER_TOKEN=demo-token`: `smoke` 5/5 including the `local-dev-token` refusal; `functional` 718/718.
  - A second run executes again (not UP-TO-DATE). `TEST_URL=http://localhost:9`: 5 of 5 smoke tests fail.
  - `./gradlew check -PskipOwasp --dry-run` lists neither `:smoke` nor `:functional`. In-process `smokeTest` is now 21 (16 + 5), `functionalTest` still 718, `test` gains `TestTargetTest` (6 tests).

### Image and chart (T011 to T018)

- Docker and Helm are not installed on the implementation machine, and downloading a standalone Helm binary was refused by the session's permission policy. **T014 (image build and run) and T018 (`helm lint`/`helm template`) remain open** for a machine with Docker and Helm, or are covered by the first Jenkins run (T030/T031), which builds the image and deploys the chart.
- Instead, the chart's three YAML files were parsed and every key in contracts/deployment-values.md was checked (port 8080, all three probe paths, the aliased `jo-security-bearer-tokens` secret, `SPRING_CONFIG_IMPORT`, `java` 6.1.1).
- The key-vault design (FR-012, EC-003, ST-1) was proved with the real jar, simulating the CSI mount with a directory holding a file `jo.security.bearer-tokens`:
  - with `SPRING_CONFIG_IMPORT=configtree:<dir>/` and the file containing `vault-token-a,vault-token-b` plus a newline, `local-dev-token` → 401, both vault tokens → 200;
  - with the directory missing, start-up fails ("Config data resource 'config tree [...]' ... does not exist");
  - with the file empty, start-up fails ("jo.security.bearer-tokens must contain at least one non-blank token").

### Jenkins handling in build.gradle and Jenkinsfiles (T019 to T022)

`--dry-run` results, 2026-11-03 (count of `:dependencyCheckAggregate` in the task graph):

| Command | Result |
|---|---|
| `./gradlew check` | 1 (scan runs) |
| `JENKINS_URL=http://jenkins.example ./gradlew check` | 0 |
| `JENKINS_URL='  ' ./gradlew check` | 1 (blank is not Jenkins) |
| `./gradlew check -PskipOwasp` | 0 |
| `CI=true JENKINS_URL=… ./gradlew dependencyCheckAggregate` | BUILD SUCCESSFUL (no NVD-key demand) |
| `CI=true ./gradlew dependencyCheckAggregate` | fails: "NVD_API_KEY is not set; OWASP dependency-check cannot run in CI" |
| `CI=true ./gradlew check` (GitHub Actions) | fails the same way without a key, so CI never scans keyless or skips |

Jenkinsfiles pin `Infrastructure@2.13.5`. `afterSuccess(String, Closure)` is confirmed in the library's `CommonPipelineDsl`; whether its closure can call `sh` directly is checked on the first nightly run (T033).

### Requirements check (T028)

| Req | Status | Evidence |
|---|---|---|
| FR-001 | Delivered | `Jenkinsfile_CNP`; PR-only behaviour per research R-2 |
| FR-002 | Delivered, Jenkins run pending | `Jenkinsfile_nightly` (weekday cron, `afterSuccess('build')` → `check`); T033 |
| FR-003 | Delivered, Jenkins run pending | `loadVaultSecrets` → `TEST_BEARER_TOKEN`; pipeline sets `TEST_URL`; T031 |
| FR-004 | Proved locally | `build/libs/ctam-jomockapi.jar` (T003) |
| FR-005 | Proved locally | dry-run table above (T020) |
| FR-006 | Unchanged task exists | `sonarqube` alias; stage behaviour T030 (research R-11) |
| FR-007 to FR-009 | Delivered, image build pending | `Dockerfile`, whitelist `.dockerignore`; T014 (needs Docker) or T030 |
| FR-010, FR-011, FR-013 | Delivered, render pending | chart keys checked by YAML parse; T018 (needs Helm) or T031 |
| FR-012 | Proved locally (mechanism) | configtree runs above: override, missing mount and empty secret |
| FR-014 | Delivered | standard chart ingress, nothing added |
| FR-015 to FR-019 | Proved locally | T010 results above |
| FR-020, FR-021 | Delivered | README § Jenkins pipeline and preview deployment; CLAUDE.md 014 entry |
| FR-022, FR-023 | Proved | `git diff 013-people-api -- src/main .github postman` is empty |
| SC-001, SC-003 (preview) | Pending | T030, T031 |
| SC-002 | Proved locally | 718/718 functional against two running copies |
| SC-004 | Proved locally (task graph) | T020; Jenkins logs T030 |
| SC-005 | See T025 | full `check` with OWASP |
| SC-006 | Proved | the README commands, as run in T010, take under a minute |

### Regression run and review (T025, T029)

- `./gradlew check` **with** the OWASP scan (no `NVD_API_KEY`; the cached NVD database was updated keyless), 2026-11-03: BUILD SUCCESSFUL in 1 h 24 min; "Found 0 vulnerabilities in project ctam-jomockapi". Suites: `test` 841 (835 + 6 `TestTargetTest`; T025 expected +3 before the token methods moved into `TestTarget`), `integrationTest` 47, `functionalTest` 718, `smokeTest` 21 (16 + 5 `DeploymentSmokeTest`). JaCoCo unchanged: lines 98.50%, branches 93.51%.
- Self-review of the diff found one defect, fixed: `TEST_BEARER_TOKEN` with a blank first entry (`,abc`) would have sent a blank token, and `TestTarget` would silently have fallen back to `test-token`. The tasks now use the first non-blank entry and report the variable as missing if there is none (verified: `" , "` → `Missing: TEST_BEARER_TOKEN`; `",demo-token"` → smoke 5/5 against a copy accepting `demo-token`).
- T026 (Postman via newman) not run: newman is not installed and fetching it with `npx` downloads external code, which this session's policy refused for Helm. `postman/` and `src/main` are unchanged against `013-people-api` (T027), so the collection's behaviour cannot have changed.
