package uk.gov.hmcts.ctam.jo.exceptions;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;
import uk.gov.hmcts.ctam.jo.util.LogSanitiser;

import java.util.Map;

/**
 * Renders errors raised outside Spring MVC (for example an exception escaping a servlet filter) in the
 * shared {@link ErrorResponse} shape, replacing Spring Boot's default {@code /error} page. It sits here,
 * next to the handler and factory, because it renders errors rather than serving a business route.
 */
@Hidden
@Slf4j
@RestController
@RequiredArgsConstructor
public class JsonErrorController implements ErrorController {

    private static final Map<Integer, String> MESSAGES = Map.of(
            HttpStatus.UNAUTHORIZED.value(), ErrorMessages.UNAUTHORIZED,
            HttpStatus.NOT_FOUND.value(), ErrorMessages.RESOURCE_NOT_FOUND,
            HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorMessages.METHOD_NOT_ALLOWED,
            HttpStatus.NOT_ACCEPTABLE.value(), ErrorMessages.NOT_ACCEPTABLE,
            HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorMessages.INTERNAL_SERVER_ERROR);

    private final ErrorResponseFactory errorResponseFactory;

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        int requested = statusCode(request);
        int status = MESSAGES.containsKey(requested) ? requested : HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = MESSAGES.get(status);
        Object correlationId = request.getAttribute(CorrelationIds.ATTRIBUTE);

        logError(request, status, correlationId);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
        if (correlationId instanceof String id) {
            // The container may clear headers set earlier in the filter chain before dispatching here.
            builder.header(CorrelationIds.HEADER, id);
        }
        return builder.body(errorResponseFactory.create(request, message));
    }

    private static int statusCode(HttpServletRequest request) {
        return request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer code ? code : -1;
    }

    private static void logError(HttpServletRequest request, int status, Object correlationId) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = LogSanitiser.sanitise(uri instanceof String value ? value : null);
        boolean restoreMdc = correlationId instanceof String && MDC.get(CorrelationIds.MDC_KEY) == null;
        if (restoreMdc) {
            MDC.put(CorrelationIds.MDC_KEY, (String) correlationId);
        }
        try {
            if (status >= 500) {
                Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                if (exception instanceof Throwable throwable) {
                    log.error("Error dispatch: status={} path={}", status, path, throwable);
                } else {
                    log.error("Error dispatch: status={} path={}", status, path);
                }
            } else {
                log.warn("Error dispatch: status={} path={}", status, path);
            }
        } finally {
            if (restoreMdc) {
                MDC.remove(CorrelationIds.MDC_KEY);
            }
        }
    }
}
