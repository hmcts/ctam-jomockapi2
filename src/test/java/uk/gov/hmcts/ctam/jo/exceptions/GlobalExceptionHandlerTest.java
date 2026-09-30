package uk.gov.hmcts.ctam.jo.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.INTERNAL_SERVER_ERROR;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_REFERENCE_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.NOT_ACCEPTABLE;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.QUERY_PARAMETERS_NOT_SUPPORTED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RECORD_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.errorResponseFactory;

class GlobalExceptionHandlerTest {

    private static final String SECRET_DETAIL = "SECRET-internal-detail";

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(errorResponseFactory(Instant.parse("2026-09-23T10:15:30Z")));

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reference_data/x");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    static Stream<Arguments> badRequests() {
        return Stream.of(
                Arguments.of(new UnsupportedReferenceDataTypeException(SECRET_DETAIL),
                             UNSUPPORTED_ATTRIBUTE_NAME),
                Arguments.of(new InvalidReferenceIdException(SECRET_DETAIL),
                             MALFORMED_REFERENCE_ID),
                Arguments.of(new UnsupportedQueryParameterException(SECRET_DETAIL),
                             QUERY_PARAMETERS_NOT_SUPPORTED));
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

        assertError(404, RECORD_NOT_FOUND);
    }

    @Test
    void noResourceFoundReturns404ResourceNotFound() throws Exception {
        handler.handleNoRoute(new NoResourceFoundException(HttpMethod.GET, "/" + SECRET_DETAIL, SECRET_DETAIL),
                              request, response);

        assertError(404, RESOURCE_NOT_FOUND);
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                                       request, response);

        assertError(405, METHOD_NOT_ALLOWED);
        assertThat(response.getHeader("Allow")).isEqualTo("GET");
    }

    @Test
    void notAcceptableReturns406AsJson() throws Exception {
        handler.handleNotAcceptable(new HttpMediaTypeNotAcceptableException(SECRET_DETAIL), request, response);

        assertError(406, NOT_ACCEPTABLE);
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternals() throws Exception {
        handler.handleUnexpected(new IllegalStateException(SECRET_DETAIL), request, response);

        assertError(500, INTERNAL_SERVER_ERROR);
        assertThat(response.getContentAsString()).doesNotContain("IllegalStateException", "java.");
    }

    private void assertError(int status, String message) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = assertErrorBody(response.getContentAsString(), message);
        assertThat(body.get("timestamp").asString()).isEqualTo("2026-09-23T10:15:30Z");
        assertThat(response.getContentAsString()).doesNotContain(SECRET_DETAIL);
    }
}
