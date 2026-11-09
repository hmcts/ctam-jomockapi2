# Contract: Repository ↔ HMCTS Jenkins Pipeline

What the shared pipeline (`cnp-jenkins-library` 2.13.5, type `java`) expects of this repository, and what the repository guarantees in return. Source: research R-1, R-4, R-9.

## Files the pipeline looks for

| Path | Required by | Notes |
|---|---|---|
| `Jenkinsfile_CNP` | PR pipeline | `@Library("Infrastructure@2.13.5") _`, `withPipeline("java", "ctam", "jomockapi")`, preview→aat vault override, `TEST_BEARER_TOKEN` from `ctam-${env}` / `jo-security-bearer-tokens` |
| `Jenkinsfile_nightly` | nightly pipeline | `cron('H 07 * * 1-5')`, `withNightlyPipeline("java", "ctam", "jomockapi")`, `afterSuccess('build')` runs `check` |
| `gradlew` + `gradle/wrapper/*` | every Gradle stage | already present |
| `Dockerfile`, `.dockerignore` | Docker Build | see `deployment-values.md` |
| `charts/ctam-jomockapi/` | preview deploy | chart name must be `<product>-<component>` |

## Gradle tasks

| Task | Stage | Contract |
|---|---|---|
| `assemble` | Build | Produces `build/libs/ctam-jomockapi.jar` regardless of the checkout folder name. |
| `check` | Test | Same as today, except the OWASP scan is **not** run when `JENKINS_URL` is set. Unit results in `build/test-results/test/`. |
| `dependencyCheckAggregate` | Security; nightly Dependency check | Runs with the pipeline's `-D` database settings. Does not demand `NVD_API_KEY` under Jenkins. The library raises the failure threshold to any CVE (CVSS 0). |
| `sonarqube` | Sonar (if the plugin is active) | Existing configuration, unchanged (research R-11). |
| `smoke` | after preview deploy | Requires `TEST_URL` and `TEST_BEARER_TOKEN`; runs `DeploymentSmokeTest` only; never up to date; results in `build/test-results/smoke/`. |
| `functional` | after preview deploy | Requires `TEST_URL` and `TEST_BEARER_TOKEN`; runs every `functionalTest` class except those tagged `in-process-only`; never up to date; results in `build/test-results/functional/`. |
| `printVersionInit` | Build (`addVersionInfo`) | Provided by the library's `init.gradle`; nothing needed here. |

Neither `smoke` nor `functional` is a dependency of `check` or `build`.

## Failure messages (FR-018)

When a variable is missing, `smoke` or `functional` fails before any test runs:

```text
<task> tests a running copy of the service and needs TEST_URL and TEST_BEARER_TOKEN. Missing: TEST_URL, TEST_BEARER_TOKEN
```

The `Missing:` list names only the variables that are unset or blank, in that order.

## Out of the repository (prerequisites, FR-020)

1. The `ctam` product and its team registered in HMCTS Jenkins team configuration, with the container registry it publishes to.
2. Key Vault `ctam-aat` holding `jo-security-bearer-tokens` (comma-separated, non-blank, not `local-dev-token`/`test-token`).
3. Managed identity `ctam` (`aadIdentityName`) with read access to that secret for the deployed pod; Jenkins able to read it for `loadVaultSecrets`.
4. The repository discovered by the HMCTS Jenkins GitHub organisation folder, with the Jenkins check reporting on pull requests.
