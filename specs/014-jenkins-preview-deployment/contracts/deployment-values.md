# Contract: Image and Chart

What the image and the `ctam-jomockapi` chart guarantee to the platform and to callers of a preview copy. Source: research R-5, R-7, R-8.

## Image

| Property | Value |
|---|---|
| Base | `hmctsprod.azurecr.io/base/java:25-distroless` |
| Contents of `/opt/app/` | `ctam-jomockapi.jar`, `applicationinsights.json`, the Application Insights agent added by the base image |
| Not in the image | sources, tests, `joh-elinks-api/`, `.gradle/`, any other build output (whitelist `.dockerignore`) |
| Port | `8080` (`EXPOSE 8080`; `server.port` default unchanged) |
| Command | `CMD ["ctam-jomockapi.jar"]` |
| Built-in token | only `application.yml`'s existing `local-dev-token` default, always overridden when deployed |

## Chart `charts/ctam-jomockapi`

`Chart.yaml`: `apiVersion: v2`, `name: ctam-jomockapi`, `version` raised on every chart change, dependency `java` `6.1.1` from `oci://hmctsprod.azurecr.io/helm`.

`values.yaml` (`java:`):

| Key | Value | Why |
|---|---|---|
| `applicationPort` | `8080` | chart default is 4550 |
| `image` | `hmctsprod.azurecr.io/ctam/jomockapi:latest` | overridden by the pipeline |
| `ingressHost` | `ctam-jomockapi-{{ .Values.global.environment }}.service.core-compute-{{ .Values.global.environment }}.internal` | template convention |
| `readinessPath`, `livenessPath`, `startupPath` | `/api/v1/healthcheck` | Principle II; chart defaults are Actuator paths |

No bearer tokens, Key Vault, managed identity or `SPRING_CONFIG_IMPORT` (amended 2026-11-23: preview uses no Key Vault). Nothing else is set. The People fixture, reference date and reference-data types keep their defaults (FR-013); probe timings and resources keep the chart defaults (research R-7, R-8).

`values.preview.template.yaml` (`java:`), substituted by the pipeline with `envsubst`:

| Key | Value | Why |
|---|---|---|
| `environment.JO_SECURITY_BEARER_TOKENS` | `"${TEST_BEARER_TOKEN}"` (quoted, so a missing token renders `""`, not `null`) | the preview token defined in `Jenkinsfile_CNP` (FR-003, FR-012); empty if unset, which stops start-up (EC-003) |
| `image` | `${IMAGE_NAME}` | set by the pipeline |
| `ingressHost` | `${SERVICE_FQDN}` | set by the pipeline |

## Behaviour of a preview copy

Identical to a local copy (FR-022), except:
- it accepts only the preview token from `Jenkinsfile_CNP` (`TEST_BEARER_TOKEN`), so `local-dev-token` gets `401`;
- it is reached through the preview ingress at the address the pipeline reports;
- the pipeline's post-deploy check calls `GET /health`, which answers like `/api/v1/healthcheck` (FR-024, research R-15). Probes still use `/api/v1/healthcheck`.
