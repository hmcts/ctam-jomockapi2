# Contract: Image and Chart

What the image and the `ctam-jomockapi` chart guarantee to the platform and to callers of a preview copy. Source: research R-5, R-7, R-8.

## Image

| Property | Value |
|---|---|
| Base | `hmctspublic.azurecr.io/base/java:25-distroless` |
| Contents of `/opt/app/` | `ctam-jomockapi.jar`, `applicationinsights.json`, the Application Insights agent added by the base image |
| Not in the image | sources, tests, `joh-elinks-api/`, `.gradle/`, any other build output (whitelist `.dockerignore`) |
| Port | `8080` (`EXPOSE 8080`; `server.port` default unchanged) |
| Command | `CMD ["ctam-jomockapi.jar"]` |
| Built-in token | only `application.yml`'s existing `local-dev-token` default, always overridden when deployed |

## Chart `charts/ctam-jomockapi`

`Chart.yaml`: `apiVersion: v2`, `name: ctam-jomockapi`, `version` raised on every chart change, dependency `java` `6.1.1` from `oci://hmctspublic.azurecr.io/helm`.

`values.yaml` (`java:`):

| Key | Value | Why |
|---|---|---|
| `applicationPort` | `8080` | chart default is 4550 |
| `image` | `hmctspublic.azurecr.io/ctam/jomockapi:latest` | overridden by the pipeline |
| `ingressHost` | `ctam-jomockapi-{{ .Values.global.environment }}.service.core-compute-{{ .Values.global.environment }}.internal` | template convention |
| `aadIdentityName` | `ctam` | Key Vault access |
| `readinessPath`, `livenessPath`, `startupPath` | `/api/v1/healthcheck` | Principle II; chart defaults are Actuator paths |
| `keyVaults.ctam.secrets` | `- name: jo-security-bearer-tokens`<br>`  alias: jo.security.bearer-tokens` | mounted at `/mnt/secrets/ctam/jo.security.bearer-tokens` |
| `environment.SPRING_CONFIG_IMPORT` | `configtree:/mnt/secrets/ctam/` | the mounted secret overrides the local default; not optional (EC-003) |

Nothing else is set. The People fixture, reference date and reference-data types keep their defaults (FR-013); probe timings and resources keep the chart defaults (research R-7, R-8).

`values.preview.template.yaml` (`java:`): `image: ${IMAGE_NAME}`, `ingressHost: ${SERVICE_FQDN}`.

## Behaviour of a preview copy

Identical to a local copy (FR-022), except:
- it accepts only the tokens in `ctam-aat` / `jo-security-bearer-tokens`, so `local-dev-token` gets `401`;
- it is reached through the preview ingress at the address the pipeline reports.
