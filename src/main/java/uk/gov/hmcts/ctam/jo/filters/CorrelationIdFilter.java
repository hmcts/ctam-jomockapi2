package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request (except the healthcheck) a correlation ID: the caller's {@code X-Correlation-Id} if it
 * is well formed, otherwise a new UUID. The ID goes into MDC for logging, into a request attribute for the
 * {@code /error} dispatch, and back to the caller in the response header (Principle XIV).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private static final String HEALTHCHECK_PATH = "/api/v1/healthcheck";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HEALTHCHECK_PATH.equals(RequestPaths.path(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationId(request.getHeader(CorrelationIds.HEADER));
        request.setAttribute(CorrelationIds.ATTRIBUTE, correlationId);
        response.setHeader(CorrelationIds.HEADER, correlationId);
        MDC.put(CorrelationIds.MDC_KEY, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }

    private static String correlationId(String inbound) {
        return inbound != null && VALID_ID.matcher(inbound).matches() ? inbound : UUID.randomUUID().toString();
    }
}
