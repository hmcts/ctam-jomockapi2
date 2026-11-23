# CNP Onboarding Plan — ctam/jomockapi

Service: `ctam-jomockapi2` (the E-Links API mock)
Product: `ctam` · Component: `jomockapi`
Design: `specs/014-jenkins-preview-deployment/` (preview only; AAT and beyond are deferred item DF-12)

Status badges: ✅ Done · ⚠️ Check first · ○ To do · ⏸ Deferred (DF-12)

---

## Goal

Build every pull request on the HMCTS Cloud Native Platform (CNP) Jenkins and deploy it to the AKS **preview**
environment, where the pipeline runs `./gradlew smoke` and `./gradlew functional` against it:

- **Service**: the Spring Boot mock in the `ctam` Kubernetes namespace, one Helm release per pull request.
- **Bearer token**: a preview-only token defined in `Jenkinsfile_CNP` and passed to the pod as
  `JO_SECURITY_BEARER_TOKENS`. No Key Vault for preview (014 amendment, 2026-11-23).
- **Jenkins pipeline**: builds, tests, scans, pushes the image and deploys on every pull request; a nightly build
  on `main`.

No database, Key Vault or other Azure infrastructure is needed, so there is no Terraform (`infrastructure/`). GitHub Actions
(`.github/workflows/`) stays the merge gate.

---

## Names Reference

Derived names follow the pattern `{product}-{component}` = `ctam-jomockapi`.

| What | Name |
|------|------|
| **GitHub repo** | `ctam-jomockapi2` |
| **GitHub org URL** | `github.com/hmcts/ctam-jomockapi2` |
| **GitHub topic** | `jenkins-cft-a-c` |
| **Default branch** | `main` (the library deploys to AAT only from `master`, so Jenkins does nothing on `main`) |
| **Jenkins product** | `ctam` |
| **Jenkins component** | `jomockapi` |
| **Jenkins job path** | `HMCTS_a_to_c/ctam-jomockapi2` |
| **Jenkins library** | `Infrastructure@2.13.5` |
| **Kubernetes namespace** | `ctam` |
| **Helm release name** | `ctam-jomockapi` |
| **Helm chart** | `charts/ctam-jomockapi` (HMCTS `java` chart 6.1.1) |
| **Docker image** | `{registry}/ctam/jomockapi:{tag}`, registry set by the team configuration |
| **Docker base image** | `hmctsprod.azurecr.io/base/java:25-distroless` |
| **Preview bearer token** | `ctam-preview-token`, `env.TEST_BEARER_TOKEN` in `Jenkinsfile_CNP`, set as `JO_SECURITY_BEARER_TOKENS` by `values.preview.template.yaml` |
| **Internal ingress host** | `ctam-jomockapi-{env}.service.core-compute-{env}.internal` |
| **Preview URL** | `ctam-jomockapi-pr-{N}.preview.platform.hmcts.net` (the exact address is in the Jenkins log) |
| **Health check / probes** | `/api/v1/healthcheck` (no Actuator) |
| **Application port** | `8080` |
| **Boot jar** | `build/libs/ctam-jomockapi.jar` |
| **Java package** | `uk.gov.hmcts.ctam.jo` |
| **Backstage component** | `ctam-jomockapi` (`catalog-info.yaml`) |
| **Owning team (GitHub, Backstage)** | TODO: fill in `.github/CODEOWNERS` and `catalog-info.yaml` |
| **Slack contact** | TODO |

---

## Phase 1 — GitHub repository setup

| # | Step | Status | Notes |
|---|------|--------|-------|
| 1.1 | Repo exists at `github.com/hmcts/ctam-jomockapi2` | ✅ Done | Public |
| 1.2 | GitHub topic `jenkins-cft-a-c` | ✅ Done | Lets the Jenkins organisation scan find the repo |
| 1.3 | `catalog-info.yaml` | ✅ Done | Owner is a placeholder until 1.5 |
| 1.4 | `.github/` CODEOWNERS, renovate.json, PR and issue templates, stale.yml | ✅ Done | |
| 1.5 | Replace the owner placeholders in CODEOWNERS and `catalog-info.yaml` | ○ To do | Before merging |
| 1.6 | Branch protection on `main` | ⚠️ Check first | GitHub Actions `ci.yml` and CodeQL as required checks |
| 1.7 | `SECURITY.md` | ✅ Done | |

---

## Phase 2 — Service repo: Jenkinsfiles, Dockerfile, Helm chart

| # | Step | Status | Notes |
|---|------|--------|-------|
| 2.1 | `Jenkinsfile_CNP` | ✅ Done | `withPipeline('java', 'ctam', 'jomockapi')`, `env.TEST_BEARER_TOKEN` (preview token, no vault) |
| 2.2 | `Jenkinsfile_nightly` | ✅ Done | Weekdays ~07:00 on `main`: build, `check`, dependency check; never deploys |
| 2.3 | `settings.gradle` and the fixed boot jar name | ✅ Done | `rootProject.name = 'ctam-jomockapi'` |
| 2.4 | `Dockerfile` and whitelist `.dockerignore` | ✅ Done | Only the jar and `lib/applicationinsights.json` enter the image |
| 2.5 | `lib/applicationinsights.json` | ✅ Done | Role `ctam-jomockapi`, no connection string (App Insights not provisioned) |
| 2.6 | Chart `charts/ctam-jomockapi/` (`Chart.yaml`, `values.yaml`, `values.preview.template.yaml`, `templates/NOTES.txt`) | ✅ Done | Raise `version` in every PR that changes the chart |
| 2.7 | Gradle tasks `smoke` and `functional` against `TEST_URL` | ✅ Done | |
| 2.8 | Local Docker check | ✅ Done | Image built and run; 5 smoke and 718 functional tests passed against it |
| 2.9 | `values.aat.template.yaml`, `enableAksStagingDeployment()` | ⏸ Deferred | DF-12 |
| 2.10 | Terraform (`infrastructure/`, `.terraform-version`), `Jenkinsfile_parameterized` | ⏸ Deferred | Not needed for preview; DF-12 if an AAT vault is managed here |

---

## Phase 3 — cnp-jenkins-config

| # | Step | Status | PR target |
|---|------|--------|-----------|
| 3.1 | `ctam` team in `team-config.yml`: namespace `ctam`, AAD group, Slack channels, container registry | ⚠️ Check first | `cnp-jenkins-config` |
| 3.2 | `deployment-controls.yml` entry | ⏸ Deferred | `cnp-jenkins-config`; only needed for AAT and beyond |
| 3.3 | `terraform-infra-approvals/ctam-jomockapi2.json` | ⏸ Deferred | Only if Terraform is added |

---

## Phase 4 — cnp-flux-config: namespace and workload identity

| # | Step | Status | Notes |
|---|------|--------|-------|
| 4.1 | `apps/ctam/` namespace kustomizations, including preview | ⚠️ Check first | The preview namespace must exist before the first deploy |
| 4.2 | Workload identity service account for `ctam` in preview | ⏸ Not needed | Preview reads no Key Vault (014 amendment, 2026-11-23); DF-12 |
| 4.3 | `apps/ctam/ctam-jomockapi/` HelmRelease, image policy, env patches | ⏸ Deferred | DF-12 (AAT and beyond) |

---

## Phase 5 — Azure

| # | Step | Status | Notes |
|---|------|--------|-------|
| 5.1–5.4 | Key Vault `ctam-aat`, secret, managed identity, Jenkins access | ⏸ Not needed for preview | `ctam-aat` doesn't exist (PR #25 build 5), so preview uses a token from `Jenkinsfile_CNP` instead (014 amendment, 2026-11-23). Needed again for AAT (DF-12) |

---

## Phase 6 — First Jenkins run

| # | Step | Status | Notes |
|---|------|--------|-------|
| 6.1 | Jenkins organisation scan finds the repo | ✅ Done | Ask in `#platops-help` if it doesn't appear under `HMCTS_a_to_c` |
| 6.2 | First PR build | 🔄 In progress | PR #25: registry, Sonar and quality gate fixed; vault removed. Stages: Build → Test → Security → Docker build → Deploy (preview) → Smoke → Functional |
| 6.3 | Nightly run on `main` | ○ To do | |

---

## Phase 7 — Verify deployment

| # | Step | Status | Command / URL |
|---|------|--------|---------------|
| 7.1 | Preview health check | ○ To do | `https://ctam-jomockapi-pr-{N}.preview.platform.hmcts.net/api/v1/healthcheck` → `{"status":"ok"}` |
| 7.2 | Preview rejects `local-dev-token` | ○ To do | `401` (covered by `DeploymentSmokeTest`) |
| 7.3 | Keep a preview copy for manual testing | ○ To do | Label the PR `enable_keep_helm` |
| 7.4 | Jenkins check added to branch protection | ⏸ Optional | `continuous-integration/jenkins/pr-merge` |

---

## Known gotchas

- **`main`, not `master`**: the library only treats `master` as a route to live, so nothing deploys from `main`.
  Renaming the default branch would start AAT and production deployments (DF-12).
- **No Actuator**: the chart's default probes (`/health/readiness`, `/health/liveness`) don't exist; all three
  probes use `/api/v1/healthcheck`.
- **The bearer-token secret is mandatory**: `SPRING_CONFIG_IMPORT` is deliberately not `optional:`, so a missing or
  empty secret stops start-up instead of falling back to `local-dev-token`.
- **App Insights**: the agent logs "No connection string provided" at start-up and stays off. Expected.
- **OWASP in Jenkins**: `check` skips the scan when `JENKINS_URL` is set, because the Security stage runs it at the
  library's stricter any-CVE threshold.
- **Chart version**: the pipeline fails a PR that changes the chart without raising `Chart.yaml` `version`.
