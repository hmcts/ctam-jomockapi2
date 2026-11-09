---

description: "Task list for 014 Jenkins Pipeline and Preview Deployment"
---

# Tasks: Jenkins Pipeline and Preview Deployment

**Input**: Design documents from `specs/014-jenkins-preview-deployment/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/pipeline-interface.md, contracts/deployment-values.md, quickstart.md

**Tests**: Included where the spec requires them. `DeploymentSmokeTest` is itself a requirement (FR-016) and runs in both the in-process `smokeTest` suite and the deployed-copy `smoke` task. Gradle- and pipeline-level behaviour is validated by the quickstart steps named in each task.

**Organization**: Grouped by user story. All stories except US4 are P1. Phases follow the plan's implementation order (US3 test plumbing, then US2 packaging, then US1 pipeline) because US2's local check uses US3's tasks and US1's pipeline needs both. Each phase ends with a checkpoint.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1 to US5 from spec.md
- `…/jo/` abbreviates `uk/gov/hmcts/ctam/jo/`

---

## Phase 1: Setup

**Purpose**: Record the baseline that US5 compares against.

- [X] T001 Run `./gradlew check -PskipOwasp` on the branch before any change. Record the per-suite test counts (from build/test-results/<suite>/*.xml) and the overall JaCoCo line and branch coverage (from build/reports/jacoco/test/jacocoTestReport.xml) under `## Implementation notes` → a new `### Baseline (T001)` subsection in specs/014-jenkins-preview-deployment/plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: A build output name that the Dockerfile (US2) and the pipeline (US1) can rely on (FR-004, EC-001, research R-12).

- [X] T002 Create settings.gradle at the repository root containing only `rootProject.name = 'ctam-jomockapi'` (with a one-line comment: fixes the project name so the build output doesn't depend on the checkout folder, 014 FR-004)
- [X] T003 In build.gradle add `tasks.named('bootJar') { archiveFileName = 'ctam-jomockapi.jar' }` with a comment citing 014 FR-004 and the Dockerfile. Run `./gradlew assemble` and confirm build/libs/ctam-jomockapi.jar exists (quickstart step 2, first line). Confirm `./gradlew dependencies --write-locks` leaves gradle.lockfile unchanged (`git diff --exit-code gradle.lockfile`)

**Checkpoint**: The jar name is fixed; nothing else has changed.

---

## Phase 3: User Story 3 - Smoke and Functional Tests Run Against the Deployed Copy (Priority: P1)

**Goal**: The same scenarios can test either an in-process copy (unchanged default) or a running copy at `TEST_URL`, through new `smoke` and `functional` tasks that `check` never runs.

**Independent Test**: Quickstart step 4 against `./start.sh` (`TEST_URL=http://localhost:8080 TEST_BEARER_TOKEN=local-dev-token ./gradlew smoke functional` passes; `./gradlew smoke` with no variables fails at once naming both), and `./gradlew check -PskipOwasp` still passes with the same suite counts plus `DeploymentSmokeTest`.

### Test target plumbing (research R-6; data-model.md § 1, ST-3)

- [X] T004 [P] [US3] Create src/testFixtures/java/…/jo/testsupport/TestTarget.java: a final utility class with `public static final String BASE_URL_PROPERTY = "jo.test.base-url"` and `public static String baseUrl(int localPort)`. When the system property is set and not blank, return it with any trailing `/` removed; otherwise return `"http://localhost:" + localPort`. Add a class Javadoc explaining that the deployed-copy tasks `smoke` and `functional` set the property from `TEST_URL` (014 FR-015), and a private constructor
- [X] T005 [P] [US3] Write src/test/java/…/jo/testsupport/TestTargetTest.java (the unit suite already sees test fixtures through the `java-test-fixtures` plugin). With `jo.test.base-url` unset, `baseUrl(8123)` is `http://localhost:8123`; set to `https://x.example/`, it is `https://x.example`; set to `"  "` (blank), it falls back to `http://localhost:8123`. Each test sets and clears the property in `try/finally` so other unit tests are unaffected. Run `./gradlew test`. Depends on T004
- [X] T006 [P] [US3] ~~Make `TestTokens.TOKEN` read `jo.test.token`~~ **Changed during implementation**: `TestTokens.TOKEN` must stay a compile-time constant because three `@WebMvcTest` classes use it in `@TestPropertySource` annotations. Instead add `TOKEN_PROPERTY = "jo.test.token"`, `token()` (the property when set and not blank, else `TestTokens.TOKEN`) and `bearerToken()` to src/testFixtures/java/…/jo/testsupport/TestTarget.java, with unit tests in TestTargetTest (T005). src/testFixtures/java/…/jo/testsupport/TestTokens.java is unchanged
- [X] T007 [US3] Replace each `TestTokens.TOKEN`/`BEARER_TOKEN` used to *send* a request with `TestTarget.token()`/`bearerToken()` (ReferenceDataHttp, PeopleSmokeTest, ReferenceDataSmokeTest), and the hard-coded `"Bearer test-token"` in src/functionalTest/java/…/jo/PeopleFunctionalTest.java `otherMethodsAre405AndUnacceptableMediaTypes406` (found by T010: it failed only against a copy with another token). Replace each hard-coded `"http://localhost:" + port…` with `TestTarget.baseUrl(port…)` in src/functionalTest/java/…/jo/testsupport/ReferenceDataHttp.java (the `exchange` call, keeping `URI.create` so `%` isn't re-encoded), src/smokeTest/java/…/jo/HealthcheckSmokeTest.java (both URLs), src/smokeTest/java/…/jo/ReferenceDataSmokeTest.java and src/smokeTest/java/…/jo/PeopleSmokeTest.java (keeping `URI.create`). Confirm with `grep -rn '"http://localhost' src/functionalTest src/smokeTest` that none is left. Depends on T004

### Deployment smoke test (FR-016, ST-1; clarification 2026-11-03: no timing)

- [X] T008 [US3] Create src/smokeTest/java/…/jo/DeploymentSmokeTest.java (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, `@LocalServerPort`, a `TestRestTemplate`, every URL built with `TestTarget.baseUrl(port)`, paths from `ContractPaths`). The class Javadoc says it checks that a copy is up and serving, runs in-process in `smokeTest` and against `TEST_URL` in `smoke`, and deliberately has no response-time limit (014 FR-016, EC-005). Tests:
  - `healthcheckIsUp`: `GET /api/v1/healthcheck` with no token returns `200`, and the body's `status` field (parsed with `JsonTrees`) equals `ok`
  - `referenceDataIsServed`: `GET ContractPaths.APPOINTMENT_TITLES` with `TestTokens.TOKEN` returns `200`
  - `peopleAreServed`: `GET ContractPaths.PEOPLE + "?updated_since=2000-01-01"` with the token returns `200`
  - `requestWithoutTokenIsRefused`: `GET ContractPaths.APPOINTMENT_TITLES` with no `Authorization` header returns `401`
  - `localDevelopmentTokenIsRefused`: `assumeFalse("local-dev-token".equals(TestTokens.TOKEN), "configured token is the local development token")`, then the same GET with `Bearer local-dev-token` returns `401` (ST-1)

  Run `./gradlew smokeTest`: all five pass in-process. Depends on T004, T006

### Deployed-copy Gradle tasks (contracts/pipeline-interface.md; FR-015, FR-017 to FR-019, ST-2)

- [X] T009 [US3] In build.gradle, after the suites block, register two `Test` tasks through one shared closure (Principle III), each with `group = 'verification'` and a description that says it tests a running copy at `TEST_URL` and is not part of `check`:
  - `smoke`: `testClassesDirs`/`classpath` from `testing.suites.smokeTest.sources` (`output.classesDirs`, `runtimeClasspath`); `filter { includeTestsMatching 'uk.gov.hmcts.ctam.jo.DeploymentSmokeTest' }`
  - `functional`: the same from `testing.suites.functionalTest.sources`; `useJUnitPlatform { excludeTags 'in-process-only' }` (EC-006). Use `useJUnitPlatform()` with no tags for `smoke`

  Both: `outputs.upToDateWhen { false }`; and a `doFirst` that reads `TEST_URL` and `TEST_BEARER_TOKEN` from the environment and, if either is unset or blank, throws `GradleException` with exactly the message in contracts/pipeline-interface.md § Failure messages, listing only the missing ones in that order. Otherwise it sets `systemProperty 'jo.test.base-url', <TEST_URL>` and `systemProperty 'jo.test.token', <first comma-separated entry of TEST_BEARER_TOKEN, trimmed>`. Neither task is added to `check`, and their execution data is not added to `suiteExecutionData`. Add a comment block citing 014 FR-015 to FR-019 and research R-1/R-6. Results land in build/test-results/smoke and build/test-results/functional by default; don't change `reports` or `reports.junitXml` locations
- [X] T010 [US3] Validate quickstart step 4 against `./start.sh` (local-dev-token):
  - `./gradlew smoke` fails before running tests with `Missing: TEST_URL, TEST_BEARER_TOKEN`
  - `TEST_URL=http://localhost:8080/ TEST_BEARER_TOKEN=local-dev-token,other ./gradlew smoke functional` passes, the trailing slash and the second token being ignored; `localDevelopmentTokenIsRefused` is reported as skipped
  - the number of tests in build/test-results/functional/*.xml equals the `functionalTest` count from T001
  - re-running the same command executes the tests again (not UP-TO-DATE)
  - `TEST_URL=http://localhost:9 TEST_BEARER_TOKEN=x ./gradlew smoke` fails
  - `./gradlew check -PskipOwasp --dry-run` lists neither `:smoke` nor `:functional`

  Record the outcome in plan.md § Implementation notes

**Checkpoint**: US3 works against any running copy; `check` is unchanged apart from `DeploymentSmokeTest` in `smokeTest`.

---

## Phase 4: User Story 2 - Each Pull Request Gets Its Own Running Copy (Priority: P1)

**Goal**: An image and chart that run the service on preview with probes on the existing health check and bearer tokens from Key Vault only.

**Independent Test**: Quickstart steps 3 to 5 locally: the image starts and passes `smoke functional`, and `helm template` renders port 8080, the three probe paths, the non-optional `configtree` import and the aliased secret. On Jenkins: quickstart step 6 (Phase 9).

### Image (contracts/deployment-values.md § Image; research R-5; FR-007 to FR-009, EC-008)

- [X] T011 [P] [US2] Create lib/applicationinsights.json: `{"role": {"name": "ctam-jomockapi"}}` with no `connectionString` (App Insights isn't provisioned; DF-12). JSON doesn't allow comments, so the reason goes in the Dockerfile comment (T012)
- [X] T012 [US2] Create Dockerfile:
  - `# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java` and `ARG APP_INSIGHTS_AGENT_VERSION=3.7.10` before `FROM hmctspublic.azurecr.io/base/java:25-distroless`
  - `COPY lib/applicationinsights.json /opt/app/`, `COPY build/libs/ctam-jomockapi.jar /opt/app/`, `EXPOSE 8080`, `CMD ["ctam-jomockapi.jar"]`
  - a header comment: built by the HMCTS pipeline after `assemble`, see specs/014 research R-5; the agent is present because the base image's default `JAVA_AGENT_OPTIONS` expects it, and without a connection string it sends nothing

  Depends on T003, T011
- [X] T013 [P] [US2] Create .dockerignore as a whitelist: `*`, then `!build/libs/ctam-jomockapi.jar` and `!lib/applicationinsights.json`, with a comment that only these two files may enter the image (014 FR-008; never `joh-elinks-api/`) and that the pipeline appends its own lines
- [ ] T014 [US2] Validate quickstart step 3: `./gradlew assemble && docker build -t ctam-jomockapi:local .`, then `docker run --rm -p 8080:8080 -e JO_SECURITY_BEARER_TOKENS=demo-token ctam-jomockapi:local`. The service starts (record how the agent behaves with no connection string), `curl -s localhost:8080/api/v1/healthcheck` returns `{"status":"ok"}`, and `TEST_URL=http://localhost:8080 TEST_BEARER_TOKEN=demo-token ./gradlew smoke functional` passes, including `localDevelopmentTokenIsRefused`. Inspect the image filesystem (`docker image save` and list the top layer, or `docker create` + `docker export | tar -t`) and confirm /opt/app holds only the jar, `applicationinsights.json` and the agent, with no `src`, `joh-elinks-api`, `.gradle` or `specs`. If the agent prevents start-up, apply research R-5's fallback and record it. Record results in plan.md § Implementation notes

### Chart (contracts/deployment-values.md § Chart; research R-7; FR-010 to FR-014)

- [X] T015 [P] [US2] Create charts/ctam-jomockapi/Chart.yaml: `apiVersion: v2`, `appVersion: "1.0"`, `description: A Helm chart for the E-Links mock API (ctam-jomockapi)`, `name: ctam-jomockapi`, `home: https://github.com/hmcts/ctam-jomockapi2`, `version: 0.0.1`, `maintainers: [{name: HMCTS ctam team}]`, `dependencies: [{name: java, version: 6.1.1, repository: 'oci://hmctspublic.azurecr.io/helm'}]`. Also create charts/ctam-jomockapi/.helmignore listing `.git/`, `.gitignore`, `*.swp`, `*.bak`, `*.tmp`, `*~`, `.idea/` and `.vscode/`
- [X] T016 [P] [US2] Create charts/ctam-jomockapi/values.yaml with exactly the `java:` keys in contracts/deployment-values.md (`applicationPort`, `image`, `ingressHost`, `aadIdentityName`, `readinessPath`, `livenessPath`, `startupPath`, `keyVaults.ctam.secrets` with `name: jo-security-bearer-tokens` / `alias: jo.security.bearer-tokens`, `environment.SPRING_CONFIG_IMPORT: "configtree:/mnt/secrets/ctam/"`). Add YAML comments: the three probe paths replace the chart's Actuator defaults (Principle II); the import must not be `optional:` so a missing secret fails start-up instead of falling back to `local-dev-token` (014 EC-003); nothing else is overridden (FR-013)
- [X] T017 [P] [US2] Create charts/ctam-jomockapi/values.preview.template.yaml with `java:` → `image: ${IMAGE_NAME}` and `ingressHost: ${SERVICE_FQDN}` under a `# Don't modify below here` comment, as in the HMCTS template
- [ ] T018 [US2] Validate quickstart step 5: `helm dependency update charts/ctam-jomockapi` (needs network access to `hmctspublic.azurecr.io`), `helm lint charts/ctam-jomockapi`, and `helm template ctam-jomockapi charts/ctam-jomockapi --set global.environment=aat`. Confirm container port 8080, readiness/liveness/startup probes on `/api/v1/healthcheck`, `SPRING_CONFIG_IMPORT=configtree:/mnt/secrets/ctam/`, and a CSI volume for vault `ctam` with object `jo-security-bearer-tokens` aliased `jo.security.bearer-tokens`. Don't commit charts/ctam-jomockapi/charts/ or Chart.lock if the HMCTS template doesn't (add `charts/*/charts/` to .gitignore if needed). Record results in plan.md § Implementation notes. Depends on T015 to T017

**Checkpoint**: US2's artifacts work locally; the preview deployment itself is checked in Phase 9.

---

## Phase 5: User Story 1 - A Pull Request Is Built and Checked by Jenkins (Priority: P1)

**Goal**: Jenkins can build, check, scan and analyse the repository, with the OWASP scan run once (in the Security stage) and GitHub Actions unchanged; a nightly job rebuilds `main`, runs `check` and the scan, and deploys nothing.

**Independent Test**: Quickstart step 2 locally (the scan disappears from `check`'s task graph only when `JENKINS_URL` is set, and the NVD-key guard behaves as specified); on Jenkins, quickstart steps 6 to 8 (Phase 9).

### Build changes (research R-4; FR-005, ST-4)

- [X] T019 [US1] In build.gradle, beside `skipOwasp`, add `def onJenkins = providers.environmentVariable('JENKINS_URL').map { it.trim() }.filter { !it.isEmpty() }.isPresent()` with a comment: Jenkins sets `JENKINS_URL` on every build and GitHub Actions never does; under Jenkins the pipeline's Security stage runs `dependencyCheckAggregate` against the shared HMCTS database, so `check` must not run it again (014 FR-005, research R-4). Then:
  - make the `check` → `dependencyCheckAggregate` wiring conditional on `!skipOwasp && !onJenkins`
  - make the `gradle.taskGraph.whenReady` guard throw only when `!onJenkins` (besides its existing conditions), and extend its comment: Jenkins needs no NVD key because the library sets `autoUpdate = false`
  - leave the `dependencyCheckAggregate` `doFirst` warning, `dependencyCheck { … }` settings and `-PskipOwasp` behaviour unchanged
- [X] T020 [US1] Validate quickstart step 2 (all with `--dry-run`, recording the output in plan.md § Implementation notes):
  - `./gradlew check` lists `:dependencyCheckAggregate`
  - `JENKINS_URL=http://jenkins.example ./gradlew check` doesn't
  - `JENKINS_URL='  ' ./gradlew check` still lists it
  - `CI=true JENKINS_URL=http://jenkins.example ./gradlew dependencyCheckAggregate` raises no NVD error
  - `CI=true ./gradlew dependencyCheckAggregate` (no `NVD_API_KEY`) fails with the existing message
  - `./gradlew check -PskipOwasp` doesn't list it

### Jenkinsfiles (contracts/pipeline-interface.md; research R-2, R-9)

- [X] T021 [P] [US1] Create Jenkinsfile_CNP:
  - `#!groovy`, then `@Library("Infrastructure@2.13.5") _`, `def type = "java"`, `def product = "ctam"`, `def component = "jomockapi"`
  - `def vaultOverrides = ['preview': 'aat']`, and `def secrets = ['ctam-${env}': [secret('jo-security-bearer-tokens', 'TEST_BEARER_TOKEN')]]` with the key in **single quotes**
  - the `static LinkedHashMap<String, Object> secret(String secretName, String envVar)` helper from the library README
  - `withPipeline(type, product, component) { overrideVaultEnvironments(vaultOverrides); loadVaultSecrets(secrets) }`

  Comments: only pull requests build and deploy (to preview) because the library treats only `master` as a route to live and this repository's default branch is `main` (014 research R-2; DF-12); the pipeline sets `TEST_URL`; label `enable_keep_helm` keeps the preview copy
- [X] T022 [P] [US1] Create Jenkinsfile_nightly:
  - `#!groovy`, then `properties([pipelineTriggers([cron('H 07 * * 1-5')])])` with the template's comment on `H`
  - `@Library("Infrastructure@2.13.5") _`, the same `type`/`product`/`component`
  - `withNightlyPipeline(type, product, component) { afterSuccess('build') { sh './gradlew --no-daemon --init-script init.gradle check' } }`

  Comments: the standard nightly runs Build and Dependency check and never deploys; the callback adds `check` (014 FR-002, clarification 2026-11-03); `init.gradle` is the library's mirror script, already written by the Build stage; if the closure can't call `sh` directly, use `steps.sh` (research R-9; verified in T032)

**Checkpoint**: The repository satisfies contracts/pipeline-interface.md locally; Jenkins runs come in Phase 9.

---

## Phase 6: User Story 4 - The Set-Up Is Documented for the Team (Priority: P2)

**Goal**: A team member can understand, run and set up the pipeline from the README alone (FR-020, FR-021).

**Independent Test**: Following only the README, a team member runs `smoke` and `functional` against `./start.sh` in under 5 minutes (SC-006) and lists every external prerequisite.

- [X] T023 [US4] Add a `## Jenkins pipeline and preview deployment` section to README.md (and a table-of-contents entry if the README has one) covering:
  - what runs on a pull request: the stage table from spec § "What the shared pipeline does"; one OWASP run in the Security stage at the stricter any-CVE threshold (research R-4)
  - preview: per-PR copy named `ctam-jomockapi`, the address shown in the Jenkins log, removed at the end of the run unless the PR is labelled `enable_keep_helm`; `only_deploy` exists but deliberately fails the build
  - `main`: Jenkins does nothing (only `master` is a route to live in the library), GitHub Actions remains the gate, and renaming the branch to `master` would start AAT/production deployments (DF-12)
  - nightly: weekday 07:xx on `main`, build + `check` + dependency check, no deployment
  - running the deployed-copy tests by hand: the exact commands from quickstart step 4, `TEST_URL` and `TEST_BEARER_TOKEN` (first of comma-separated tokens), result folders, and that `local-dev-token` refusal is skipped when it is the configured token
  - the token: Key Vault `ctam-aat`, secret `jo-security-bearer-tokens`, comma-separated, never `local-dev-token`/`test-token`; mounted by the chart and imported with `configtree`; missing or empty means the pod doesn't start
  - chart rule: raise `charts/ctam-jomockapi/Chart.yaml` `version` in any PR that changes the chart
  - external prerequisites with usual owner (platform team / product team), exactly the four items in contracts/pipeline-interface.md § Out of the repository
  - functional scenarios that run only in-process (tag `in-process-only`): "none" until T031 says otherwise
  - pull requests from forks follow HMCTS's standard Jenkins policy (EC-007)
- [X] T024 [P] [US4] Update CLAUDE.md:
  - add a `014-jenkins-preview-deployment — implemented` entry under "Current repo state" in the existing style (files added, the `JENKINS_URL` behaviour of `check`, `smoke`/`functional`, `DeploymentSmokeTest`, `TestTarget`/`TestTokens` properties, Key Vault secret and `configtree`, preview-only via `main`, DF-12 open)
  - add the new commands to "Commands": `TEST_URL=… TEST_BEARER_TOKEN=… ./gradlew smoke functional` and `docker build -t ctam-jomockapi:local .`
  - add a short note under "Test suites" that `smoke`/`functional` are deployed-copy tasks outside `check`

**Checkpoint**: Documentation complete.

---

## Phase 7: User Story 5 - Existing Behaviour Is Unchanged (Priority: P1)

**Goal**: Prove FR-022, FR-023 and SC-005.

**Independent Test**: The comparisons below match the T001 baseline.

- [X] T025 [US5] Run `./gradlew check` **with** the OWASP scan (`NVD_API_KEY` set). It passes; the per-suite counts equal T001's, plus 5 in `smokeTest` (T008) and 3 in `test` (T005); JaCoCo coverage is at or above T001's figures and above the gate. Record in plan.md § Implementation notes
- [ ] T026 [P] [US5] With `./start.sh` running, run `npx newman run postman/ctam-jomockapi.postman_collection.json -e postman/ctam-jomockapi.postman_environment.json`; all assertions pass unchanged
- [X] T027 [P] [US5] Confirm `git diff main...HEAD --stat -- src/main .github postman` shows no change (FR-022, FR-023) and that `NoTypeSpecificCodeTest` still passes (part of T025). Record in plan.md § Implementation notes

**Checkpoint**: Everything observable is unchanged.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T028 [P] Re-read spec FR-001 to FR-023 and SC-001 to SC-006 against the delivered files. Tick off what is proved locally, and list what remains for Phase 9 in plan.md § Implementation notes
- [X] T029 Run `/code-review` (or the team's review) on the branch diff, and address findings before opening the PR. The PR description lists the external prerequisites and asks for the `enable_keep_helm` label on the first run

---

## Phase 9: Verification on HMCTS Jenkins (needs the external prerequisites)

**Purpose**: Close research R-5, R-7, R-9, R-11 and EC-006, which can only be checked on Jenkins (quickstart steps 6 to 8). These tasks run after the PR is opened. Each records its outcome in plan.md § Implementation notes; any change it forces goes into the same PR.

- [ ] T030 [US1] On the first PR run (label `enable_keep_helm`), confirm that `JENKINS_URL` is set on the agent (echo it in the Test stage log, e.g. with a temporary `before('test')` callback in Jenkinsfile_CNP that is removed afterwards, and record it; research R-4), and that Build, Test, Security, Sonar (if active) and Docker Build stages pass, that the Test log has no `dependencyCheckAggregate` and the Security log has exactly one. If Security fails on CVEs below 7.0, upgrade the dependency or add a suppression with a reason to config/owasp/suppressions.xml. If the Sonar stage fails because of SonarCloud Automatic Analysis, stop and raise it with the team (research R-11); don't disable it
- [ ] T031 [US3] On the same run, confirm the preview pod becomes ready and record which Key Vault it mounted (expected `ctam-aat`; if different, update Jenkinsfile_CNP's override and README). Confirm `smoke` and `functional` pass against `TEST_URL` with every functional test executed. For any test that fails only behind the ingress: fix it if it is a test bug; otherwise tag it `@Tag("in-process-only")` with a comment giving the reason, and list it in README.md (EC-006, SC-002). With the copy kept, confirm `local-dev-token` gets `401` and the vault token gets `200` from the preview address, and that Swagger UI and `/v3/api-docs` are served (US2 scenarios 3 and 5)
- [ ] T032 [US1] Trigger the nightly job on `main` by hand. Confirm Build, the `check` callback and Dependency check run, and nothing deploys. If the callback needs `steps.sh`, or callbacks can't run Gradle, fix Jenkinsfile_nightly or apply the research R-9 fallback (and record the spec change it implies). Record the result
- [ ] T033 [US5] Confirm on the same PR that the GitHub Actions `CI` workflow ran the full `check` including OWASP and that CodeQL ran, as before (quickstart step 8)

---

## Dependencies & Execution Order

- **Phase 1 → Phase 2** → everything else.
- **US3 (Phase 3)**: T004, T006 [P] → T005, T007, T008 → T009 → T010.
- **US2 (Phase 4)**: needs T003 (jar name) and, for T014, Phase 3's `smoke`/`functional` tasks. T011, T013, T015, T016 and T017 can run in parallel; T012 needs T011; T018 needs T015 to T017.
- **US1 (Phase 5)**: T019 → T020. T021 and T022 [P] are independent files and can be written any time after Phase 2, but are only meaningful once US2 and US3 exist.
- **US4 (Phase 6)**: after Phases 3 to 5 so it documents what exists. T024 [P] with T023.
- **US5 (Phase 7)**: after all code changes. T026 and T027 [P].
- **Phase 9**: after the PR is open and the prerequisites exist. T030 → T031; T032 and T033 are independent.

## Parallel Examples

```text
Phase 3:  T004 TestTarget.java  ||  T006 TestTokens.java;  then T005 TestTargetTest.java || T007 address call sites
Phase 4:  T011 applicationinsights.json || T013 .dockerignore || T015 Chart.yaml || T016 values.yaml || T017 values.preview.template.yaml
Phase 5:  T021 Jenkinsfile_CNP  ||  T022 Jenkinsfile_nightly
Phase 7:  T026 Postman run      ||  T027 diff check
```

## Implementation Strategy

- **MVP** = Phases 1 to 3 (US3). The deployed-copy tests work against any running copy, which is useful even before Jenkins exists and de-risks the rest.
- **Increment 2** = Phase 4 (US2): a runnable image and chart, proved locally.
- **Increment 3** = Phase 5 (US1): Jenkinsfiles and the OWASP handling. After this the PR can be opened.
- **Then** Phases 6 to 8 before review, and Phase 9 on the live pipeline once the external prerequisites are in place.
