package uk.gov.hmcts.ctam.jo.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Builds and writes the shared {@link ErrorResponse}. Used by the exception handler, the filters and the
 * {@code /error} controller, so every error has the same shape wherever it is raised.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final Clock clock;

    private final JsonMapper jsonMapper;

    public ErrorResponse create(HttpServletRequest request, String message) {
        return new ErrorResponse(message, Instant.now(clock).truncatedTo(ChronoUnit.SECONDS), traceId(request));
    }

    /**
     * Writes the error body straight to the servlet response, bypassing content negotiation. Used where
     * Spring MVC's message converters are not available (filters) or would refuse JSON ({@code 406}).
     */
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        // Plain application/json, as on success responses: JSON is UTF-8 by definition (RFC 8259), and Jackson
        // writes UTF-8 bytes to the output stream, so no charset parameter is needed.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), create(request, message));
    }

    private static String traceId(HttpServletRequest request) {
        String fromMdc = MDC.get(CorrelationIds.MDC_KEY);
        if (fromMdc != null) {
            return fromMdc;
        }
        return request.getAttribute(CorrelationIds.ATTRIBUTE) instanceof String fromAttribute ? fromAttribute : null;
    }
}
