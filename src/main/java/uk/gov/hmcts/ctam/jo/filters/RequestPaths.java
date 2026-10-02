package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The request path as the servlet container has decoded and normalised it. Filters use this, never the raw
 * {@code getRequestURI()}, for access and exemption decisions: a raw-URI check could be bypassed with an
 * encoded path such as {@code /%61pi/...} that Spring MVC still routes.
 */
public final class RequestPaths {

    private RequestPaths() {
    }

    public static String path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? request.getServletPath() : request.getServletPath() + pathInfo;
    }
}
