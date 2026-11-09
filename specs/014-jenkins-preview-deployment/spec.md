# Feature Specification: Jenkins Pipeline and Preview Deployment

**Feature Branch**: `014-jenkins-preview-deployment`

**Created**: 2026-11-03

**Status**: Draft

**Input**: User description: "Jenkins deployment (HMCTS CNP pipeline), preview-only. Add what the HMCTS cnp-jenkins-library Java pipeline (`withPipeline("java", "ctam", "jomockapi")`) needs to build, scan, containerise and deploy this mock to a per-pull-request AKS preview environment, and run smoke and functional tests against the deployed instance. Decisions already made: product = ctam, component = jomockapi (so the release/app name is ctam-jomockapi); preview environment only for now (no AAT/demo/Flux HelmRelease in cnp-flux-config yet, to be a later feature); GitHub Actions CI (ci.yml) and CodeQL stay unchanged alongside Jenkins. [...] Out of scope: AAT/demo/prod environments, Flux config, cnp-jenkins-config team registration (an external prerequisite to document), new API endpoints. Any constitution conflict (e.g. Principle XIII suite semantics) must be raised, not worked around." (Full description: see the feature's `/speckit-specify` invocation; the elided parts are restated in the sections below.)

## Context

Until now the mock has only run on a developer's machine (`./start.sh`) and inside GitHub Actions, where every test suite starts the application in-process. Teams that integrate with E-Links have no hosted copy of the mock to call.

HMCTS services are built and deployed by the shared HMCTS Jenkins pipeline (the "CNP" Java pipeline). For each pull request it builds the service, runs its checks, scans its dependencies, packages it as a container image, deploys that image to a short-lived **preview** environment, and then runs the service's **smoke** and **functional** tests against the deployed copy. The pipeline expects certain things of a repository; this feature adds them.

This feature is deliberately limited to preview deployments. Long-lived shared environments (AAT, demo, production) are deployed through separate HMCTS configuration and are a later feature (see Deferred Items).

Decisions already made by the team:

- **Product** `ctam`, **component** `jomockapi`, so the deployed service is named `ctam-jomockapi`.
- **Preview only** for now.
- **GitHub Actions stays.** The existing CI and CodeQL workflows remain the merge gates required by Constitution Principle XIII; Jenkins is added alongside them, not in place of them.

### What the shared pipeline does (behavioural reference)

Read from the HMCTS shared pipeline library on 2026-11-03. The plan must re-confirm these before relying on them.

| Pipeline stage | What it runs in this repository | Notes |
|---|---|---|
| Tool set-up | Detects the Java version from the build and selects a matching JDK | Java 25 is supported |
| Build | `assemble` | |
| Test | `check` | Publishes unit-suite results |
| Security | `dependencyCheckAggregate` against HMCTS's shared vulnerability database | Pipeline supplies the database credentials |
| Sonar | `sonarqube` | Deprecated alias of `sonar`; still provided by the current Sonar plugin |
| Smoke (after deploy) | `smoke`, with `--rerun-tasks` and the deployed address in `TEST_URL` | Reads results from `test-results/smoke` or `test-results/smokeTest` |
| Functional (after deploy) | `functional`, with `--rerun-tasks` and `TEST_URL` | Reads results from `test-results/functional` or `test-results/functionalTest` |

### Deferred Items

- **DF-12 (open)**: deploy to the long-lived shared environments (AAT, and others as needed) through HMCTS's environment configuration, so other teams' services can call a stable copy of the mock. This needs its own feature, environment-specific secrets and the team's decision about which environments are needed.

## Clarifications

### Session 2026-11-03

- Q: Which Jenkins? → A: The shared HMCTS CNP Java pipeline, product `ctam`, component `jomockapi`.
- Q: Which environments? → A: Preview only, for now.
- Q: Keep GitHub Actions? → A: Yes, alongside Jenkins.
- Q: Should this repository create the Key Vault holding the preview bearer tokens, or should it be set up outside the repository? → A: Outside the repository. This feature documents the vault and secret name the chart expects; the platform or product team creates them once. No Terraform (`infrastructure/`) in this feature.
- Q: When `smoke` tests a deployed preview copy, should it only check availability and correctness, or also enforce response-time limits? → A: Availability and correctness only, with no time limits. Latency targets stay in the in-process `smokeTest` suite.
- Q: Should this feature also add a nightly Jenkins pipeline that rebuilds `main` and re-runs the vulnerability scan, without deploying? → A: Yes. A nightly pipeline on `main` that builds, runs `check` and the vulnerability scan, and never deploys.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A Pull Request Is Built and Checked by Jenkins (Priority: P1)

A developer opens a pull request. Jenkins picks it up, builds the service, and runs the same quality gates the project already enforces (compiler warnings as errors, Checkstyle, all four test suites, the coverage gate), plus a dependency vulnerability scan and Sonar analysis. A failure in any of them fails the Jenkins build.

**Why this priority**: Everything else depends on Jenkins being able to build and check the repository at all.

**Independent Test**: Open a pull request with no deployment files changed beyond this feature's; Jenkins reaches the end of its Build, Test, Security and Sonar stages and reports a result on the pull request.

**Acceptance Scenarios**:

1. **Given** a pull request whose code passes `./gradlew check` locally, **When** Jenkins builds it, **Then** the Build, Test, Security and Sonar stages pass.
2. **Given** a pull request that breaks a test, a Checkstyle rule or the coverage gate, **When** Jenkins builds it, **Then** the Test stage fails.
3. **Given** a pull request that adds a dependency with an unsuppressed vulnerability, **When** Jenkins builds it, **Then** the Security stage fails. The shared pipeline fails on any severity, stricter than GitHub Actions' CVSS 7.0 (research R-4).
4. **Given** any Jenkins build, **When** it runs, **Then** the dependency vulnerability scan runs exactly once (in the Security stage), not also inside the Test stage.
5. **Given** the same pull request, **When** GitHub Actions runs, **Then** its behaviour is unchanged: the full `check`, including the vulnerability scan, still runs and is never skipped.
6. **Given** a newly published vulnerability affects a dependency on `main` and nobody opens a pull request, **When** the nightly pipeline runs, **Then** its vulnerability scan fails and nothing is deployed.

---

### User Story 2 - Each Pull Request Gets Its Own Running Copy of the Mock (Priority: P1)

After a pull request's checks pass, Jenkins packages the service and deploys it to the preview environment at its own address. The pipeline's own smoke and functional tests call that copy. By default the pipeline removes it when the run ends; a pull request labelled `enable_keep_helm` keeps it, so a developer or reviewer can call it with a valid bearer token and see the pull request's behaviour.

**Why this priority**: A hosted, per-change copy is the reason for adopting the pipeline.

**Independent Test**: For a pull request labelled `enable_keep_helm`, find the preview address Jenkins reports, call the health check and one authenticated route, and get the expected responses.

**Acceptance Scenarios**:

1. **Given** a pull request whose checks pass, **When** the pipeline reaches deployment, **Then** the service is deployed to the preview environment under the name `ctam-jomockapi` for that pull request and becomes ready.
2. **Given** the preview copy is deployed, **When** anyone calls `GET /api/v1/healthcheck` without credentials, **Then** it returns `200` with `{"status": "ok"}`, exactly as locally.
3. **Given** the preview copy is deployed, **When** a caller sends a request with a token from the environment's Key Vault, **Then** it is accepted; **When** the caller sends the local development token `local-dev-token`, **Then** it is refused with `401`.
4. **Given** the preview copy is deployed, **When** the platform checks whether it is alive and ready, **Then** it uses the existing health-check route, not a separate health framework (Constitution Principle II).
5. **Given** the preview copy is deployed, **When** a caller opens the Swagger UI or the OpenAPI document, **Then** both are served, as locally (Principle XVIII).

---

### User Story 3 - Smoke and Functional Tests Run Against the Deployed Copy (Priority: P1)

After deployment, Jenkins runs smoke tests and then functional tests over real HTTP against the preview copy, not against an in-process copy. A deployment that starts but misbehaves (wrong configuration, missing data, broken authentication) fails the pipeline.

**Why this priority**: Without this, a pull request could deploy a broken service and still pass.

**Independent Test**: Run the smoke and functional tasks on a developer machine against a separately started copy of the mock, by pointing them at its address and giving them a token; they pass. Point them at an address where nothing is running; they fail.

**Acceptance Scenarios**:

1. **Given** a deployed copy at an address, **When** the smoke task runs with that address, **Then** it confirms the health check answers and at least one authenticated reference-data route and one authenticated People route return `200`.
2. **Given** a deployed copy at an address, **When** the functional task runs with that address, **Then** it runs the project's existing functional acceptance scenarios against that copy and they pass.
3. **Given** no address is supplied, **When** the smoke or functional task runs, **Then** it fails at once with a message saying the address (and token) are required, rather than silently testing something else or passing with no tests run.
4. **Given** an address where the service does not answer, **When** the smoke task runs, **Then** it fails.
5. **Given** a developer runs `./gradlew check`, **When** it runs, **Then** the existing in-process `smokeTest` and `functionalTest` suites run exactly as before, and the deployed-copy tasks do not run.

---

### User Story 4 - The Set-Up Is Documented for the Team (Priority: P2)

A team member can find, in the README, how the Jenkins pipeline and preview deployment work: what the pipeline does, where the preview address appears, how the bearer token is supplied, how to run the deployed-copy tests by hand, and what has to be set up outside this repository before the first build.

**Why this priority**: Several prerequisites live outside the repository; without a record of them the first build fails with no obvious cause.

**Independent Test**: A team member who has not seen this feature follows the README to run the deployed-copy tests against a local instance, and lists the external prerequisites correctly.

**Acceptance Scenarios**:

1. **Given** the README, **When** a reader looks for the deployment section, **Then** it lists every external prerequisite (FR-020) and who usually owns it.
2. **Given** the README, **When** a reader wants to run the smoke or functional tests against any running copy, **Then** it gives the exact commands and the settings they need.

---

### User Story 5 - Existing Behaviour Is Unchanged (Priority: P1)

Nothing a caller of the mock can observe changes: every route, response, status code, header and piece of data is the same as before. Local development (`./start.sh`, `./gradlew bootRun`, `./gradlew check`) works as before.

**Why this priority**: This is an infrastructure feature; any change to API behaviour would be a regression.

**Independent Test**: The full existing test suite and the Postman collection pass unchanged.

**Acceptance Scenarios**:

1. **Given** this feature is merged, **When** the existing unit, integration, functional and smoke suites run, **Then** they pass with no change to their expectations.
2. **Given** this feature is merged, **When** the Postman collection runs against a local instance, **Then** it passes unchanged.

### Edge Cases

- **EC-001**: The repository is checked out into a folder with a different name (as Jenkins does). The built application archive still has the same, fixed name, so packaging finds it.
- **EC-002**: The deployed copy takes longer to start than the platform's default readiness wait (it loads and validates every fixture at start-up). The readiness and liveness settings allow for the measured start-up time, so the platform does not restart a copy that is still starting.
- **EC-003**: The secret holding the bearer tokens is missing or empty in the preview environment. The deployment must not fall back to the local development token; the service either fails to start or refuses every authenticated request, and the pipeline fails.
- **EC-004**: The pipeline re-runs the smoke or functional task in the same build (for example against a different deployment). The tasks run again rather than being skipped as up to date.
- **EC-005**: Network latency between the build agent and the preview environment is much higher than on a developer machine. The deployed-copy smoke tests check availability and correctness with no response-time limit, so they do not fail on network delay alone.
- **EC-006**: Some existing functional scenarios may depend on in-process conditions that a deployed copy cannot reproduce (for example a test-only configuration). Any such scenario is identified in planning and either made to work against a deployed copy or explicitly excluded from the deployed-copy run with a recorded reason; none is silently skipped.
- **EC-007**: A pull request comes from a fork. Jenkins handles it according to HMCTS's standard policy; this feature does not change that policy.
- **EC-008**: The packaged image contains anything not needed at run time (sources, test data, local reference extracts under `joh-elinks-api/`, build caches). It must not: the local-only reference extracts in particular must never be packaged (CLAUDE.md, "Reference data").

## Requirements *(mandatory)*

### Functional Requirements

**Pipeline definition**

- **FR-001**: The repository MUST contain a Jenkins pipeline definition that uses the shared HMCTS Java pipeline with product `ctam` and component `jomockapi`. It builds, checks and deploys pull requests; on `main` the shared pipeline deliberately does nothing, because it only treats `master` as a route to live (research R-2). GitHub Actions remains the gate for `main`.
- **FR-002**: The repository MUST contain a nightly pipeline definition using the shared HMCTS nightly pipeline for product `ctam` and component `jomockapi`. It MUST run on `main` once every weekday, build the service, run `check` and the dependency vulnerability scan, fail when either fails, and MUST NOT deploy anywhere (Clarifications 2026-11-03).
- **FR-003**: The pipeline MUST supply the deployed-copy tests with the preview address and a valid bearer token read from the environment's Key Vault, never a token written into the repository.

**Build**

- **FR-004**: The build MUST produce the runnable application archive under a fixed name that does not depend on the folder the repository is checked out into.
- **FR-005**: When the build runs under Jenkins, `check` MUST NOT run the dependency vulnerability scan, because the pipeline's Security stage runs it against the shared HMCTS database. Everywhere else, including GitHub Actions and local runs, `check` MUST behave exactly as today: the scan runs unless explicitly skipped with `-PskipOwasp`, and in GitHub Actions it can never be skipped. Which builds count as "under Jenkins" MUST be decided by a signal Jenkins always sets and GitHub Actions never does.
- **FR-006**: The Gradle task the pipeline's Sonar stage calls MUST exist and run the project's existing Sonar configuration.

**Container image**

- **FR-007**: The repository MUST define a container image built on the HMCTS-provided Java 25 base image, running the application archive from FR-004 on port 8080.
- **FR-008**: The image MUST contain only what is needed to run the service. In particular it MUST NOT contain the local-only reference extracts, sources, tests or build caches (EC-008).
- **FR-009**: The image MUST NOT contain any bearer token other than the application's existing built-in local-development default, which the deployment overrides (FR-012).

**Deployment**

- **FR-010**: The repository MUST contain a deployment chart for the service, built on the standard HMCTS Java chart, that deploys it to the preview environment under the name `ctam-jomockapi`.
- **FR-011**: The chart MUST direct the platform's liveness, readiness and start-up checks to the existing `GET /api/v1/healthcheck` route and send traffic to port 8080. It MUST NOT require a health framework the service does not have (Principle II).
- **FR-012**: The chart MUST supply the bearer tokens to the service from a secret in the product's existing Key Vault for the environment, so the deployed service never accepts `local-dev-token` (EC-003). The repository MUST NOT create the vault or the secret; it names them (FR-020).
- **FR-013**: The chart MUST NOT change any other application setting from its default (People fixture, reference date, reference-data types), so a preview copy serves exactly the shipped synthetic data.
- **FR-014**: The deployment MUST NOT expose the service more widely than HMCTS's standard preview arrangement does.

**Tests against a deployed copy**

- **FR-015**: Gradle tasks named `smoke` and `functional` MUST exist. Each MUST test a running copy of the service at the address given in `TEST_URL`, authenticating with a token from a documented setting, and MUST write its results where the pipeline reads them (`test-results/smoke` or `smokeTest`, and `test-results/functional` or `functionalTest`).
- **FR-016**: `smoke` MUST check that the deployed copy is up and serving: the health check returns `200` with `{"status": "ok"}`; at least one reference-data route and one People route return `200` with the token; and a request without a token returns `401`. It MUST NOT apply any response-time limit; the latency targets stay in the in-process `smokeTest` suite, which runs on every build (EC-005, Clarifications 2026-11-03).
- **FR-017**: `functional` MUST run the project's existing functional acceptance scenarios against the deployed copy, reusing the existing scenario code rather than a second copy of it (Principle III). Any scenario that cannot run against a deployed copy MUST be listed with its reason (EC-006).
- **FR-018**: If `TEST_URL` or the token is not set, `smoke` and `functional` MUST fail before running any test, with a message naming the missing setting.
- **FR-019**: `./gradlew check` MUST NOT run `smoke` or `functional`, and the existing in-process `smokeTest` and `functionalTest` suites MUST keep their current behaviour (apart from gaining `DeploymentSmokeTest`), coverage contribution and latency guards.

**Documentation**

- **FR-020**: The README MUST describe the Jenkins pipeline and preview deployment, how to run `smoke` and `functional` against any running copy, and every prerequisite outside this repository, including at least: registering the `ctam` product and team with the HMCTS Jenkins configuration; the container registry the product publishes to; the Key Vault the preview deployment reads and the exact name of the bearer-token secret in it, with the expected value format (comma-separated tokens); and read access to that secret for both the deployed service and the pipeline (which passes a token to the deployed-copy tests).
- **FR-021**: CLAUDE.md MUST be updated with this feature's summary and the new commands, in the same style as earlier features.

**Unchanged behaviour**

- **FR-022**: This feature MUST NOT change any route, response, status code, header, error message or data the service serves, and MUST NOT change the OpenAPI document or the Postman artifacts' expectations.
- **FR-023**: This feature MUST NOT change the GitHub Actions CI or CodeQL workflows.

### Key Entities

- **Pipeline definition**: names the shared pipeline, the product and the component, and passes the deployed-copy tests their settings.
- **Container image**: the packaged, runnable service.
- **Deployment chart**: how the platform runs the image in the preview environment: name, port, health checks and the secret it reads.
- **Bearer-token secret**: the per-environment tokens the deployed service accepts and the deployed-copy tests use. It lives only in the environment's Key Vault.
- **Deployed-copy test run**: a `smoke` or `functional` run against a given address with a given token.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For a pull request with passing code, the Jenkins pipeline goes from start to a deployed, ready preview copy with smoke and functional tests passed, with no manual step after the external prerequisites are in place.
- **SC-002**: 100% of the existing functional acceptance scenarios either pass against a deployed copy or are listed with a reason for exclusion (expected: none excluded).
- **SC-003**: A preview copy refuses `local-dev-token` and accepts the environment's token, in every deployment.
- **SC-004**: The dependency vulnerability scan runs exactly once per Jenkins build, and exactly as today in every GitHub Actions build.
- **SC-005**: Every existing test, the coverage gate and the Postman collection pass unchanged; no API behaviour changes.
- **SC-006**: A team member can run the deployed-copy smoke tests against a local instance in under 5 minutes by following the README.

## Assumptions

- The shared HMCTS pipeline behaves as described in "What the shared pipeline does"; the plan re-confirms the task names, result locations and how the pipeline passes the preview address.
- An HMCTS base image for Java 25 is available; if not, the plan raises it rather than changing the Java version (the constitution pins Java 25).
- Preview deployments use HMCTS's standard arrangement: one copy per pull request at an address the pipeline provides, removed at the end of each pipeline run unless the pull request carries the `enable_keep_helm` label (research R-3).
- The shared pipeline only treats a branch named `master` as a route to live (AAT, then production). This repository's default branch is `main`, so Jenkins builds and deploys pull requests only and does nothing on `main`; GitHub Actions remains the gate for `main` (research R-2). Renaming the default branch to `master` would start AAT and production deployments and is out of scope (DF-12).
- The bearer-token secret for the preview environment is created outside this repository, in the product's existing per-environment Key Vault, managed by the HMCTS platform team or the product team (Clarifications, 2026-11-03). This feature documents the vault and secret name and does not provision infrastructure (no Terraform). The plan confirms which vault the standard HMCTS chart reads for preview deployments.
- The deployed-copy tests share code with the existing in-process suites (for example by choosing the target address and token from settings), so a scenario is written once.
- Adding the `smoke` and `functional` tasks is consistent with Constitution Principle XIII: the four required suites stay as they are and `check` still runs them all. If planning finds a conflict, it is raised before implementation, not worked around.
- Postman artifacts (Principle XVII) and the OpenAPI document (Principle XVIII) need no change, because no endpoint changes. The Postman environment already takes the base address as a variable, so it can be pointed at a preview copy.
- Out of scope: AAT, demo, perftest, ITHC and production; environment configuration outside this repository (Flux); Jenkins team registration itself (documented as a prerequisite only); infrastructure provisioning; new or changed endpoints.
