package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedQueryParameterException;

/**
 * Rejects any query parameter on reference-data routes (FR-027), so no parameter is ever accepted and then
 * silently ignored. Only matched controller routes are checked: an unmatched path reaches Spring's
 * static-resource handler instead and must still get the route's {@code 404} (routes are checked first).
 */
@Component
public class NoQueryParametersInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            throw new UnsupportedQueryParameterException(query);
        }
        return true;
    }
}
