# Quickstart: Validate Jenkins Pipeline and Preview Deployment

**Feature**: 014-jenkins-preview-deployment

Steps 1 to 5 run on a developer machine and prove everything that can be proved locally. Steps 6 to 8 need the external prerequisites (`contracts/pipeline-interface.md` § Out of the repository) and run on HMCTS Jenkins. Each step lists the requirement it validates.

## Prerequisites

- JDK 25 available to Gradle (the wrapper provisions it), Docker, `curl`, optionally `helm` 3.
- A clean checkout of branch `014-jenkins-preview-deployment`.

## 1. Existing gates unchanged (FR-019, FR-022, SC-005)

```bash
./gradlew check -PskipOwasp
```

Expected: passes, with the same suites and coverage gate as before; `DeploymentSmokeTest` now also runs in `smokeTest`. Run without `-PskipOwasp` once with `NVD_API_KEY` set to confirm the scan still runs locally.

## 2. Fixed jar name, and Jenkins skips the scan inside `check` (FR-004, FR-005)

```bash
./gradlew assemble && ls build/libs/ctam-jomockapi.jar
./gradlew check --dry-run | grep -c dependencyCheckAggregate                     # expect 1 or more
JENKINS_URL=http://jenkins.example ./gradlew check --dry-run | grep -c dependencyCheckAggregate   # expect 0
CI=true JENKINS_URL=http://jenkins.example ./gradlew dependencyCheckAggregate --dry-run   # expect no NVD_API_KEY error
CI=true ./gradlew dependencyCheckAggregate --dry-run                             # expect the existing NVD_API_KEY error
```

## 3. Image builds and runs (FR-007 to FR-009, research R-5)

```bash
docker build -t ctam-jomockapi:local .
docker run --rm ctam-jomockapi:local ls /opt/app                    # distroless: if there is no ls, inspect with `docker image inspect` / `docker save` instead
docker run --rm -p 8080:8080 -e JO_SECURITY_BEARER_TOKENS=demo-token ctam-jomockapi:local
curl -s localhost:8080/api/v1/healthcheck                      # {"status":"ok"}
```

Expected: the service starts with the Application Insights agent present and no connection string. `/opt/app` holds only the jar, `applicationinsights.json` and the agent. No `joh-elinks-api`, `src` or `.gradle` in the image.

## 4. Deployed-copy tests against a local copy (FR-015 to FR-018, SC-006)

With the container from step 3 still running:

```bash
./gradlew smoke                                                      # fails at once: Missing: TEST_URL, TEST_BEARER_TOKEN
TEST_URL=http://localhost:8080 TEST_BEARER_TOKEN=demo-token ./gradlew smoke functional
TEST_URL=http://localhost:9 TEST_BEARER_TOKEN=demo-token ./gradlew smoke     # fails: nothing answering
```

Expected: the second command passes. The results are in `build/test-results/smoke/` and `build/test-results/functional/`, and the functional count equals the in-process `functionalTest` count (minus any `in-process-only` tests, expected none). Running `./start.sh` with `TEST_BEARER_TOKEN=local-dev-token` also passes, because the `local-dev-token` refusal check is skipped when that is the configured token.

## 5. Chart renders as intended (FR-010 to FR-014)

```bash
helm dependency update charts/ctam-jomockapi
helm template ctam-jomockapi charts/ctam-jomockapi --set global.environment=aat \
  | grep -E 'containerPort|/api/v1/healthcheck|SPRING_CONFIG_IMPORT|jo-security-bearer-tokens|jo.security.bearer-tokens'
```

Expected: port 8080; readiness, liveness and startup probes on `/api/v1/healthcheck`; `SPRING_CONFIG_IMPORT=configtree:/mnt/secrets/ctam/`; the secret mounted under the alias.

## 6. First pull request on Jenkins (US1, US2, US3)

Open the PR with the label `enable_keep_helm` and check:
- Build, Test (`check`), Security, Sonar (if active; research R-11) and Docker Build pass. The Security log shows one `dependencyCheckAggregate` run, and the Test log shows none.
- The preview deploy becomes ready. **Record the Key Vault the pod mounted** (expected `ctam-aat`, research R-7).
- `smoke` and `functional` pass against `TEST_URL`. Investigate any functional failure that happens only behind the ingress: either fix it or tag it `in-process-only` with the reason and list it in the README (EC-006).
- With the release kept: `curl -H 'Authorization: Bearer local-dev-token' https://<preview-host>/api/v1/reference_data/genders` returns `401`, and the vault token returns `200`.

## 7. Nightly (FR-002)

Trigger the nightly job on `main` by hand, then check:
- Build, the `check` callback and Dependency check all run, and nothing is deployed.
- Record whether the `afterSuccess('build')` closure needed `sh` or `steps.sh` (research R-9).

## 8. GitHub Actions unchanged (FR-023, SC-004)

On the same PR, the `CI` workflow runs `./gradlew check` including the OWASP scan, exactly as before, and CodeQL runs as before.
