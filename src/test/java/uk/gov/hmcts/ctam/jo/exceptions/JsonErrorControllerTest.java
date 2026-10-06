package uk.gov.hmcts.ctam.jo.exceptions;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.INTERNAL_SERVER_ERROR;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.NOT_ACCEPTABLE;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.errorResponseFactory;

/**
 * The {@code /error} dispatch, driven through the request attributes the container sets. The real dispatch over
 * HTTP is covered by {@code ErrorDispatchIntegrationTest}.
 */
class JsonErrorControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:15:30Z");

    private final JsonErrorController controller = new JsonErrorController(errorResponseFactory(NOW));

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    static Stream<Arguments> knownStatuses() {
        return Stream.of(
                Arguments.of(401, UNAUTHORIZED),
                Arguments.of(404, RESOURCE_NOT_FOUND),
                Arguments.of(405, METHOD_NOT_ALLOWED),
                Arguments.of(406, NOT_ACCEPTABLE),
                Arguments.of(500, INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("knownStatuses")
    void knownStatusKeepsItsStatusAndFixedMessage(int status, String message) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/reference_data/x");
        request.setAttribute(CorrelationIds.ATTRIBUTE, "corr-1");

        ResponseEntity<ErrorResponse> response = controller.error(request);

        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getHeaders().getFirst(CorrelationIds.HEADER)).isEqualTo("corr-1");
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(message, NOW, "corr-1"));
        // The ID is put in the MDC only for logging, and must not leak into the next request on this thread.
        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 418, 503})
    void unmappedStatusBecomes500(int status) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);

        ResponseEntity<ErrorResponse> response = controller.error(request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    void missingStatusBecomes500() {
        ResponseEntity<ErrorResponse> response = controller.error(request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    void exceptionDetailIsNeverExposed() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("SECRET-detail"));

        ResponseEntity<ErrorResponse> response = controller.error(request);

        assertThat(response.getBody().error()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().toString()).doesNotContain("SECRET");
    }

    @Test
    void withoutCorrelationIdNoHeaderIsSetAndTraceIdIsNull() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        ResponseEntity<ErrorResponse> response = controller.error(request);

        assertThat(response.getHeaders().containsHeader(CorrelationIds.HEADER)).isFalse();
        assertThat(response.getBody().traceId()).isNull();
    }

    @Test
    void existingMdcCorrelationIdIsLeftInPlace() {
        MDC.put(CorrelationIds.MDC_KEY, "corr-1");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(CorrelationIds.ATTRIBUTE, "corr-1");

        ResponseEntity<ErrorResponse> response = controller.error(request);

        assertThat(response.getBody().traceId()).isEqualTo("corr-1");
        // Set by someone else, so it is theirs to remove.
        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isEqualTo("corr-1");
    }
}
