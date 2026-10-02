package uk.gov.hmcts.ctam.jo.exceptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.errorResponseFactory;

class ErrorResponseFactoryTest {

    // Fractional seconds on purpose: the factory must truncate them.
    private static final Instant NOW = Instant.parse("2026-09-23T10:15:30.123Z");

    private final ErrorResponseFactory factory = errorResponseFactory(NOW);

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesMdcCorrelationIdWhenSet() {
        MDC.put(CorrelationIds.MDC_KEY, "from-mdc");
        request.setAttribute(CorrelationIds.ATTRIBUTE, "from-attribute");

        ErrorResponse error = factory.create(request, RESOURCE_NOT_FOUND);

        assertThat(error).isEqualTo(
                new ErrorResponse(RESOURCE_NOT_FOUND, Instant.parse("2026-09-23T10:15:30Z"), "from-mdc"));
    }

    @Test
    void fallsBackToRequestAttributeWhenMdcIsUnset() {
        request.setAttribute(CorrelationIds.ATTRIBUTE, "from-attribute");

        assertThat(factory.create(request, "x").traceId()).isEqualTo("from-attribute");
    }

    @Test
    void traceIdIsNullWhenNeitherIsSet() {
        assertThat(factory.create(request, "x").traceId()).isNull();
    }

    @Test
    void writeSetsStatusContentTypeAndSharedBody() throws Exception {
        MDC.put(CorrelationIds.MDC_KEY, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory.write(request, response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"error\":\"" + UNAUTHORIZED + "\","
                        + "\"timestamp\":\"2026-09-23T10:15:30Z\",\"traceId\":\"abc-123\"}");
    }

    @Test
    void writeKeepsNullTraceIdInTheBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory.write(request, response, HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED);

        assertThat(response.getContentAsString()).contains("\"traceId\":null");
    }
}
