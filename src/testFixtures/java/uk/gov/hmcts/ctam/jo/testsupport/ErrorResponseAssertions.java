package uk.gov.hmcts.ctam.jo.testsupport;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one check that an HTTP body is the shared error response (Principle IX), shared by every test suite
 * through Gradle test fixtures. Callers add their own checks (status, headers, traceId value) on the result.
 */
public final class ErrorResponseAssertions {

    private ErrorResponseAssertions() {
    }

    /**
     * Asserts that {@code json} is exactly {@code {"error", "timestamp", "traceId"}}, in that order, with the
     * given message and a whole-second UTC timestamp, and returns the parsed body.
     */
    public static JsonNode assertErrorBody(String json, String expectedMessage) {
        JsonNode body = JsonTrees.parse(json);
        assertThat(body.propertyNames()).containsExactly("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo(expectedMessage);
        assertThat(body.get("timestamp").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        return body;
    }
}
