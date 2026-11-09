# The E-Links mock API image, built by the HMCTS Jenkins pipeline after `./gradlew assemble`
# (specs/014-jenkins-preview-deployment, research R-5).
#
# The Application Insights agent is included because the base image's default JAVA_AGENT_OPTIONS expects it.
# lib/applicationinsights.json has no connection string (App Insights is not provisioned; 014 DF-12), so the
# agent sends nothing. Structured logs still go to stdout.

# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10
FROM hmctspublic.azurecr.io/base/java:25-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/ctam-jomockapi.jar /opt/app/

EXPOSE 8080
CMD [ "ctam-jomockapi.jar" ]
