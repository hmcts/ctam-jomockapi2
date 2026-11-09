# Data Model: Jenkins Pipeline and Preview Deployment

**Feature**: 014-jenkins-preview-deployment | **Date**: 2026-11-03

This feature adds no API data and changes no fixture. Its "data" is the configuration that connects the pipeline, the image, the chart, the running service and the deployed-copy tests. The table names each setting once; the contracts in `contracts/` say who sets and reads it.

## 1. Settings

| Setting | Kind | Set by | Read by | Value / rule |
|---|---|---|---|---|
| `JENKINS_URL` | environment variable | Jenkins (every build) | `build.gradle` | Present and not blank means "running under Jenkins" (research R-4). Never set by GitHub Actions. |
| `CI` | environment variable | GitHub Actions (`true`) | `build.gradle` guard | Unchanged meaning: in CI outside Jenkins, `NVD_API_KEY` is required whenever the scan runs. |
| `TEST_URL` | environment variable | Jenkins (preview address); a developer by hand | `smoke`, `functional` | Absolute `http(s)` URL of a running copy, with or without a trailing `/`. Required. |
| `TEST_BEARER_TOKEN` | environment variable | Jenkins `loadVaultSecrets` from `ctam-aat` / `jo-security-bearer-tokens`; a developer by hand | `smoke`, `functional` | Non-blank. If it holds comma-separated tokens (the secret's format), the first non-blank one is used; none counts as missing. Required. |
| `jo.test.base-url` | JVM system property | `smoke`, `functional` (from `TEST_URL`) | `TestTarget` | Absent for the in-process suites, which then use `http://localhost:<port>`. |
| `jo.test.token` | JVM system property | `smoke`, `functional` (from `TEST_BEARER_TOKEN`) | `TestTarget.token()` | Absent for every other suite, which then uses `TestTokens.TOKEN` (`test-token`, a compile-time constant). |
| `jo-security-bearer-tokens` | Key Vault secret in `ctam-aat` | Platform or product team (outside the repo) | chart (mount), Jenkins (tests) | Comma-separated, non-blank tokens. It must not be `local-dev-token` or `test-token`. |
| `/mnt/secrets/ctam/jo.security.bearer-tokens` | mounted file | chart `keyVaults` (CSI driver) | Spring `configtree` import | Contents of the secret above. |
| `SPRING_CONFIG_IMPORT` | container environment variable | chart `values.yaml` | Spring Boot | `configtree:/mnt/secrets/ctam/` (deliberately not `optional:`). |
| `jo.security.bearer-tokens` | application property | the mounted file (deployed); `JO_SECURITY_BEARER_TOKENS` or the default (local) | `SecurityProperties` | Existing rule unchanged: at least one non-blank token or the service refuses to start. |
| `IMAGE_NAME`, `SERVICE_FQDN` | template variables | Jenkins | `values.preview.template.yaml` | The PR image and preview host. |

## 2. Rules and checks

- **ST-1** (FR-012, EC-003): A deployed copy never accepts `local-dev-token`. This holds because the mounted secret takes precedence over the default, a missing mount fails start-up (non-optional import), and an empty secret fails `SecurityProperties`. `DeploymentSmokeTest` checks it on every preview run.
- **ST-2** (FR-018): `smoke` and `functional` fail before running any test when `TEST_URL` or `TEST_BEARER_TOKEN` is unset or blank, naming each missing variable.
- **ST-3** (FR-019): When `jo.test.base-url` and `jo.test.token` are absent, every existing suite behaves exactly as today (same address, same token).
- **ST-4** (FR-005): The `check` → `dependencyCheckAggregate` dependency is removed only when `JENKINS_URL` is set, or when `-PskipOwasp` is given as today. Running `dependencyCheckAggregate` directly always works.
- **ST-5** (FR-004): The boot jar is always `build/libs/ctam-jomockapi.jar`.
- **ST-6** (research R-3): Every pull request that changes anything under `charts/ctam-jomockapi/` raises `Chart.yaml` `version`.

## 3. Lifecycle of a preview copy

```text
PR opened/updated
  → Build → Static checks / Container build (check, Security, Sonar, image)
  → Deploy to preview (namespace/host per PR, chart + ctam-aat secret)
  → smoke (TEST_URL) → functional (TEST_URL)
  → Helm release uninstalled at end of run, unless the PR is labelled enable_keep_helm
```
