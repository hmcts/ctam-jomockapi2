package uk.gov.hmcts.ctam.jo.exceptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseFactoryTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:15:30.123Z");

    private final ErrorResponseFactory factory =
            new ErrorResponseFactory(Clock.fixed(NOW, ZoneOffset.UTC), JsonMapper.builder().build());

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesMdcCorrelationIdWhenSet() {
        MDC.put(CorrelationIds.MDC_KEY, "from-mdc");
        request.setAttribute(CorrelationIds.ATTRIBUTE, "from-attribute");

        ErrorResponse error = factory.create(request, "Resource not found.");

        assertThat(error).isEqualTo(
                new ErrorResponse("Resource not found.", Instant.parse("2026-09-23T10:15:30Z"), "from-mdc"));
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

        factory.write(request, response, HttpStatus.UNAUTHORIZED, "Unauthorized. Invalid or missing token.");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"error\":\"Unauthorized. Invalid or missing token.\","
                        + "\"timestamp\":\"2026-09-23T10:15:30Z\",\"traceId\":\"abc-123\"}");
    }

    @Test
    void writeKeepsNullTraceIdInTheBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory.write(request, response, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed.");

        assertThat(response.getContentAsString()).contains("\"traceId\":null");
    }
}
