package uk.gov.hmcts.ctam.jo.exceptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String SECRET_DETAIL = "SECRET-internal-detail";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorResponseFactory(
            Clock.fixed(Instant.parse("2026-09-23T10:15:30Z"), ZoneOffset.UTC), jsonMapper));

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reference_data/x");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    static Stream<Arguments> badRequests() {
        return Stream.of(
                Arguments.of(new UnsupportedReferenceDataTypeException(SECRET_DETAIL),
                             "Unsupported reference data attribute_name."),
                Arguments.of(new InvalidReferenceIdException(SECRET_DETAIL),
                             "reference_id must be a non-negative whole number."),
                Arguments.of(new UnsupportedQueryParameterException(SECRET_DETAIL),
                             "Query parameters are not supported on this endpoint."));
    }

    @ParameterizedTest
    @MethodSource("badRequests")
    void requestErrorsReturn400WithTheFixedMessage(ApiRequestException ex, String expectedMessage) throws Exception {
        handler.handleBadRequest(ex, request, response);

        assertError(400, expectedMessage);
    }

    @Test
    void unknownRecordReturns404RecordNotFound() throws Exception {
        handler.handleRecordNotFound(new ReferenceDataNotFoundException(SECRET_DETAIL), request, response);

        assertError(404, "Reference data record not found.");
    }

    @Test
    void noResourceFoundReturns404ResourceNotFound() throws Exception {
        handler.handleNoRoute(new NoResourceFoundException(HttpMethod.GET, "/" + SECRET_DETAIL, SECRET_DETAIL),
                              request, response);

        assertError(404, "Resource not found.");
    }

    @Test
    void noHandlerFoundReturns404ResourceNotFound() throws Exception {
        handler.handleNoRoute(new NoHandlerFoundException("GET", "/" + SECRET_DETAIL, null), request, response);

        assertError(404, "Resource not found.");
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                                       request, response);

        assertError(405, "Method not allowed.");
        assertThat(response.getHeader("Allow")).isEqualTo("GET");
    }

    @Test
    void notAcceptableReturns406AsJson() throws Exception {
        handler.handleNotAcceptable(new HttpMediaTypeNotAcceptableException(SECRET_DETAIL), request, response);

        assertError(406, "Not acceptable.");
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternals() throws Exception {
        handler.handleUnexpected(new IllegalStateException(SECRET_DETAIL), request, response);

        assertError(500, "Internal server error.");
        assertThat(response.getContentAsString()).doesNotContain("IllegalStateException", "java.");
    }

    @Test
    void traceIdComesFromTheCorrelationId() throws Exception {
        MDC.put(CorrelationIds.MDC_KEY, "trace-1");

        handler.handleRecordNotFound(new ReferenceDataNotFoundException("x"), request, response);

        assertThat(body().get("traceId").asString()).isEqualTo("trace-1");
    }

    private void assertError(int status, String message) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = body();
        assertThat(body.propertyNames()).containsExactly("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo(message);
        assertThat(body.get("timestamp").asString()).isEqualTo("2026-09-23T10:15:30Z");
        assertThat(response.getContentAsString()).doesNotContain(SECRET_DETAIL);
    }

    private JsonNode body() throws Exception {
        return jsonMapper.readTree(response.getContentAsString());
    }
}
