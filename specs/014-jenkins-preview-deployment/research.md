# Research: Jenkins Pipeline and Preview Deployment

**Feature**: 014-jenkins-preview-deployment | **Date**: 2026-11-03

Sources were read on 2026-11-03 from these HMCTS repositories: `cnp-jenkins-library` (release 2.13.5: `GradleBuilder`, `ProjectBranch`, `sectionBuildAndTest`, `withNightlyPipeline`, `sectionNightlyTests`, `resources/uk/gov/hmcts/gradle/init.gradle`, `SonarProperties`, README), `chart-java` (release 6.1.1: `values.yaml`, README), `chart-library` (README), `cnp-java-base` (README) and `spring-boot-template` (Jenkinsfiles, Dockerfile, chart, `build.gradle`, `lib/applicationinsights.json`). Local measurements were taken on this repository at `a783761`.

## R-1 Gradle tasks the pipeline calls

**Decision**: Keep `assemble`, `check`, `dependencyCheckAggregate` and `sonarqube` as they are. Add two `Test` tasks, `smoke` and `functional` (R-6).

**Findings**: `GradleBuilder` runs `assemble` (Build), `check` (Test; publishes `**/test-results/test/*.xml` and Checkstyle HTML), `dependencyCheckAggregate` (Security, with the shared PostgreSQL vulnerability database passed as `-D` properties), `--info <PR properties> sonarqube` (Sonar), `--rerun-tasks smoke` and `--rerun-tasks functional` (after deployment; results read from `**/test-results/smoke|smokeTest/*.xml` and `**/test-results/functional|functionalTest/*.xml`). Every call adds `--no-daemon --init-script init.gradle`. `setupToolVersion` selects `/usr/lib/jvm/temurin-25-jdk-*` when `build.gradle` contains `JavaLanguageVersion.of(25)`, which it does. `sonarqube` exists locally as the Sonar plugin's deprecated alias of `sonar`.

**Alternatives considered**: Renaming the existing `smokeTest`/`functionalTest` suites to `smoke`/`functional`. Rejected: `check` would then call the deployed-copy tasks, Principle XIII's suite names would change, and Gradle's name matching would make `smoke` ambiguous with `smokeTestClasses`.

## R-2 Branches: only pull requests deploy on this repository

**Decision**: Use the standard `withPipeline` with no deployment overrides. Document in the README that Jenkins builds and deploys pull requests only, and that `main` is gated by GitHub Actions.

**Findings**: `ProjectBranch.isMaster()` is `branchName == 'master'`, and the whole build, test and deploy section is wrapped in `onPathToLive`, which runs only for `PR-*`, `master` and `preview` branches. This repository's default branch is `main`, so a `main` build only checks out the code. Pull requests deploy to `preview`, so the "preview only" decision needs no extra configuration. `nonDeployableApp()` would also stop preview deployments, so it is not used.

**Risk**: If the default branch is ever renamed `master`, Jenkins starts deploying to AAT and then production. That belongs to DF-12 and is recorded in the spec's Assumptions and the README.

## R-3 Preview lifetime and labels

**Findings**: The pipeline uninstalls the preview Helm release at the end of each run. The `enable_keep_helm` label keeps it, and `only_deploy` skips tests (and deliberately fails, so the PR can't be merged). On pull requests, `enforceChartVersionBumped` requires the chart's `version` to be increased whenever the chart changes.

**Decision**: The spec's User Story 2 now says the copy stays up only with `enable_keep_helm`. Tasks must bump `charts/ctam-jomockapi/Chart.yaml` `version` whenever the chart changes, and the README says so.

## R-4 Vulnerability scan on Jenkins (FR-005)

**Decision**: Treat a build as running under Jenkins when the environment variable `JENKINS_URL` is set and not blank (Jenkins sets it on every build; GitHub Actions never does). Under Jenkins:
- `check` does not depend on `dependencyCheckAggregate`. Locally it behaves exactly like `-PskipOwasp`; elsewhere nothing changes.
- The existing "CI must have `NVD_API_KEY`" guard does not fire, because Jenkins uses the shared database with `autoUpdate = false` and needs no NVD key.

**Findings**:
- The library's `init.gradle` reconfigures `dependencyCheck` on Jenkins: `autoUpdate = false`, **`failBuildOnCVSS = 0`** (any CVE fails, compared with this repository's 7.0), a JSON report, and only `runtimeClasspath` scanned. So the Jenkins Security stage is *stricter* than GitHub Actions. A low-severity CVE that GitHub Actions tolerates fails Jenkins until it is fixed or added to `config/owasp/suppressions.xml` with a reason (Principle XIII already requires that).
- Without the change, `check` on Jenkins would run the scan a second time without the shared database's connection settings.
- The guard currently tests `CI == 'true'`. HMCTS agents may set `CI`, which would fail the Security stage for lack of an NVD key.

**Alternatives considered**: Passing `-PskipOwasp` from Jenkins. Rejected: the library builds the `check` command itself and offers no per-stage arguments. Detecting Jenkins by `BUILD_NUMBER`. Rejected: other CI systems set it too.

## R-5 Container image

**Decision**: `Dockerfile` based on `hmctspublic.azurecr.io/base/java:25-distroless` (listed in `cnp-java-base`). It sets `ARG APP_INSIGHTS_AGENT_VERSION` (as the template and base image README do), copies `lib/applicationinsights.json` and `build/libs/ctam-jomockapi.jar` to `/opt/app/`, `EXPOSE 8080`, and `CMD ["ctam-jomockapi.jar"]`. A whitelist `.dockerignore` (`*`, then `!build/libs/ctam-jomockapi.jar`, `!lib/applicationinsights.json`) keeps sources, tests, `joh-elinks-api/` and caches out of the image (FR-008). The pipeline appends one line of its own to `.dockerignore`, which this layout tolerates.

**Findings**: The base image's default `JAVA_AGENT_OPTIONS` points at the Application Insights agent jar, which is only added when `APP_INSIGHTS_AGENT_VERSION` is set. Following the template avoids the JVM failing to start on a missing agent. `lib/applicationinsights.json` names the role `ctam-jomockapi` and has **no** connection string, because App Insights is not provisioned (DF-12). The agent then sends no telemetry; structured logs still go to stdout (Principle XIV). PR builds pass `--build-arg DEV_MODE=true`, which this Dockerfile ignores.

**Verification**: Quickstart step 3 builds and runs the image locally to confirm it starts with the agent present and no connection string. If the agent refuses to start, the fallback is to drop the agent and override `JAVA_AGENT_OPTIONS` to empty in the chart, recorded as a deviation from the template.

## R-6 Running existing tests against a deployed copy (FR-015 to FR-019)

**Decision**:
1. A test-fixtures helper, `TestTarget`, returns the base address: the system property `jo.test.base-url` (any trailing `/` removed) when set, otherwise `http://localhost:<port>`. `TestTarget.token()`/`bearerToken()` read the system property `jo.test.token`, defaulting to `TestTokens.TOKEN` (`test-token`). *Implementation note*: `TestTokens.TOKEN` itself had to stay a compile-time constant because `@WebMvcTest` classes use it in `@TestPropertySource` annotations, so the override lives in `TestTarget`, and request-sending code calls `TestTarget.token()`. The four places that build `http://localhost:` addresses (`ReferenceDataHttp` and the three smoke classes) use `TestTarget`. No scenario is copied (Principle III).
2. `functional` is a `Test` task over the `functionalTest` suite's compiled classes and runtime classpath. `smoke` is the same over the `smokeTest` suite, filtered to a new class `DeploymentSmokeTest`. Both pass `jo.test.base-url` from `TEST_URL` and `jo.test.token` from `TEST_BEARER_TOKEN` (first comma-separated entry). Both are never up to date, are not wired into `check`, and fail in `doFirst` with a message naming any missing variable (FR-018). Gradle writes their results to `build/test-results/smoke` and `build/test-results/functional`, which the pipeline reads.
3. `DeploymentSmokeTest` (FR-016): health check `200` `{"status":"ok"}`; `GET /api/v1/reference_data/appointment_titles` `200`; `GET /api/v1/people?updated_since=2000-01-01` `200`; no token `401`; and `local-dev-token` `401` unless the configured token *is* `local-dev-token` (a developer pointing it at `./start.sh`). It has no timing assertions (clarification 2026-11-03). It also runs in-process as part of `smokeTest`, so `check` keeps it working.

**Findings**: Every functional test sends its requests through `ReferenceDataHttp` (`PeopleHttp` uses it too) and compares responses with golden files of the shipped data. No functional or smoke test uses captured output, mocks, injected beans or test-only property sources (searched for `OutputCapture`, `@Autowired`, `@MockitoBean`, `@TestPropertySource`, `@DynamicPropertySource`, `@DirtiesContext`: none). The only test-profile setting is the token. So all 272 functional test methods are expected to pass against a deployed copy (SC-002).

**Trade-off accepted**: The suites stay `@SpringBootTest(RANDOM_PORT)`, so a deployed-copy run still starts an unused in-process copy (about 1 s, R-8). That keeps every scenario in one place. Skipping the context in remote mode would need a second test style for marginal gain.

**Risk (EC-006)**: The preview ingress (Traefik) may answer some unusual requests itself (for example encoded slashes) before they reach the service, so a scenario that expects the service's own error body could fail only behind the ingress. The first preview run is the check. Any scenario affected is tagged `in-process-only` with its reason in a comment and listed in the README, and `functional` excludes that tag. Nothing is skipped silently.

**Alternatives considered**: A separate deployed-copy test source set. Rejected: it duplicates scenarios (Principle III) and lets them drift. Running the existing latency smoke tests remotely. Rejected by the 2026-11-03 clarification.

## R-7 Helm chart

**Decision**: `charts/ctam-jomockapi/` with `Chart.yaml` (apiVersion v2, `name: ctam-jomockapi`, `version: 0.0.1`, one dependency `java` `6.1.1` from `oci://hmctspublic.azurecr.io/helm`), `values.yaml` and `values.preview.template.yaml` (`image: ${IMAGE_NAME}`, `ingressHost: ${SERVICE_FQDN}`, as in the template).

`values.yaml` (under `java:`):
- `applicationPort: 8080`
- `image: hmctspublic.azurecr.io/ctam/jomockapi:latest`. The pipeline overrides it; the registry is whatever the team configuration names.
- `ingressHost: ctam-jomockapi-{{ .Values.global.environment }}.service.core-compute-{{ .Values.global.environment }}.internal`
- `aadIdentityName: ctam`
- `readinessPath`, `livenessPath` and **`startupPath`**: `/api/v1/healthcheck`. The chart defaults all three to Actuator paths, and the startup probe is easy to miss (Principle II).
- `keyVaults.ctam.secrets: [{name: jo-security-bearer-tokens, alias: jo.security.bearer-tokens}]`
- `environment.SPRING_CONFIG_IMPORT: "configtree:/mnt/secrets/ctam/"`

**Findings**:
- The chart mounts Key Vault secrets as files at `/mnt/secrets/<vault>/<alias>` through the CSI driver; it does not set environment variables. Spring Boot's `configtree` import turns the file `jo.security.bearer-tokens` into that property, trims the trailing newline, and an imported source takes precedence over `application.yml`'s `${JO_SECURITY_BEARER_TOKENS:local-dev-token}` default. Setting the import through the `SPRING_CONFIG_IMPORT` environment variable means **no application or `application.yml` change** (FR-022).
- The import is deliberately *not* `optional:`. A missing mount stops start-up instead of silently falling back to `local-dev-token`. A missing secret leaves the pod waiting for its volume, and an empty secret makes `SecurityProperties` refuse to start (it rejects an empty or blank token list), so every EC-003 case fails the deployment.
- Azure Key Vault secret names allow only letters, digits and `-`, hence `jo-security-bearer-tokens` with a dotted alias.
- The README pairs preview deployments with `aat` vaults (`overrideVaultEnvironments(['preview': 'aat'])` for the tests). The chart's vault name gets the environment suffix, so preview is expected to read `ctam-aat`. **Verify on the first preview deployment.** If the platform resolves another vault, only the README and the Jenkinsfile override change.

**Probe timings (EC-002)**: The chart defaults stay. The startup probe allows `startupDelay 10 + startupPeriod 10 × startupFailureThreshold 20` = 210 s. Measured start-up is about 1.0 s locally (R-8); a pod with a 250m CPU request may be 10 to 30 times slower, still well within the window.

## R-8 Start-up time and memory (measured)

Two runs of `java -jar` (Temurin 25.0.4, Apple Silicon): "Started Application in 1.04 s / 0.96 s", health check answering after 1.4 s, resident memory 317 to 327 MB. The chart's preview limits (`devmemoryLimits 1024Mi`) are enough. No resource overrides are needed.

## R-9 Jenkinsfiles

**Decision**:
- Pin the library: `@Library("Infrastructure@2.13.5") _`. The library README asks every Jenkinsfile to pin a release; Renovate raises bumps.
- `Jenkinsfile_CNP`: `withPipeline("java", "ctam", "jomockapi")` with `overrideVaultEnvironments(['preview': 'aat'])` and `loadVaultSecrets(['ctam-${env}': [secret('jo-security-bearer-tokens', 'TEST_BEARER_TOKEN')]])`. The `secret(...)` helper is copied from the library README, and the vault key is single-quoted so the pipeline substitutes `${env}`. `TEST_URL` is set by the pipeline itself.
- `Jenkinsfile_nightly`: `pipelineTriggers([cron('H 07 * * 1-5')])` as in the template, `withNightlyPipeline("java", "ctam", "jomockapi")`, plus an `afterSuccess('build')` callback that runs `./gradlew --no-daemon --init-script init.gradle check`.

**Findings**: The standard nightly pipeline runs only Checkout, Build (`assemble`) and Dependency check (any failure marks the build failed at the end); it deploys nothing. FR-002 (clarified 2026-11-03) also asks for `check`, hence the callback. The `init.gradle` that `builder.build()` writes into the workspace is reused, so dependencies resolve through the HMCTS mirror. **Verify on the first nightly run** that the callback's closure can run `sh` (the README's examples use `steps.<step>`; use whichever form works and record it). The fallback, if callbacks can't run Gradle, is to drop `check` from the nightly run and record that in the spec. GitHub Actions already runs `check` on every push to `main`.

## R-10 Jenkins resolves dependencies through a mirror

**Findings**: `init.gradle` replaces all plugin and dependency repositories with the HMCTS Artifactory `maven-remotes` mirror (`PREFER_SETTINGS`). Dependency locking (`gradle.lockfile`) pins versions, not repositories, so it keeps working. If the mirror lacks an artifact, the Build stage fails with a resolution error. That is an external prerequisite to raise with the platform team, not something to work around in `build.gradle`.

## R-11 Sonar stage

**Findings**: The Sonar stage runs only if the Jenkins instance has the Sonar plugin active, inside `withSonarQubeEnv("SonarQube")`, and then waits for the quality gate. `build.gradle` hard-codes `sonar.host.url=https://sonarcloud.io`, and the SonarCloud project uses Automatic Analysis, which rejects analyses submitted from CI (002 plan, Complexity Tracking).

**Decision**: No change in this feature. **Verify on the first PR run.** If the Sonar stage is active and fails for this reason, raise it with the team. The options are the same as 002's deferral (switch Automatic Analysis off, or point Jenkins at the platform SonarQube); neither is decided here. Do not disable the stage silently.

## R-12 Fixed build output name (FR-004, EC-001)

**Decision**: Add `settings.gradle` with `rootProject.name = 'ctam-jomockapi'` (matching `spring.application.name`), and set `bootJar { archiveFileName = 'ctam-jomockapi.jar' }` so the Dockerfile's path never depends on the project name.

**Findings**: Without `settings.gradle` the project is named after its folder. Locally that gives `build/libs/ctam-jomockapi2.jar`; Jenkins workspaces have other names. The rename also changes the plain and test-fixtures jar names, which nothing references. The Sonar project key and the lockfile don't depend on the project name.

## R-13 Constitution impact

None of the changes conflicts with v1.13.0. In particular:
- Principle XIII: the four suites and `check` are unchanged in GitHub Actions. On Jenkins the scan moves from `check` to the dedicated Security stage, so it still runs on every build path. GitHub Actions remains the merge gate. CodeQL is untouched.
- Principle II: probes use the hand-rolled health check; no Actuator is added.
- Principle X: deployment-specific values come from configuration (environment variable and mounted secret), not code.
- Principle XV: the deployed copy needs a bearer token from Key Vault, `local-dev-token` is refused, and the health check stays the only unauthenticated route.
- Principles XVII and XVIII: no endpoint changes, so the Postman and OpenAPI artifacts are unchanged.
