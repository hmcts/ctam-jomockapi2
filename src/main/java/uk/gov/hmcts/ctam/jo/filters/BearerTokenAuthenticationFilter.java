package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.ctam.jo.config.ApiPaths;
import uk.gov.hmcts.ctam.jo.config.SecurityProperties;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorMessages;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponseFactory;
import uk.gov.hmcts.ctam.jo.util.LogSanitiser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Simulated Bearer-token authentication for everything under {@code /api/} except the healthcheck. It runs
 * before routing, so unauthenticated callers cannot probe which routes exist (research R2, R3). The
 * exemption is a code constant, not configuration, so authentication cannot be switched off.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PREFIX = "/api/";

    // A code constant, not configuration, so the exemption can't be widened by editing settings.
    private static final String HEALTHCHECK_PATH = ApiPaths.HEALTHCHECK;

    private static final String BEARER_SCHEME = "Bearer";

    private final SecurityProperties securityProperties;

    private final ErrorResponseFactory errorResponseFactory;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = RequestPaths.path(request);
        return !path.startsWith(PROTECTED_PREFIX) || HEALTHCHECK_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            reject(request, response, "missing");
        } else if (!isAccepted(authorization)) {
            reject(request, response, "invalid");
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isAccepted(String authorization) {
        int space = authorization.indexOf(' ');
        if (space < 0 || !BEARER_SCHEME.equalsIgnoreCase(authorization.substring(0, space))) {
            return false;
        }
        String token = authorization.substring(space + 1);
        return !token.isEmpty() && matchesConfiguredToken(token.getBytes(StandardCharsets.UTF_8));
    }

    private boolean matchesConfiguredToken(byte[] presented) {
        boolean matched = false;
        for (String accepted : securityProperties.bearerTokens()) {
            // Constant-time comparison, and no early exit, so timing reveals nothing about the tokens.
            matched |= MessageDigest.isEqual(presented, accepted.getBytes(StandardCharsets.UTF_8));
        }
        return matched;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        // Never log the Authorization header or the token.
        log.warn("Authentication failed: status=401 reason={} path={}", reason,
                 LogSanitiser.sanitise(RequestPaths.path(request)));
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_SCHEME);
        errorResponseFactory.write(request, response, HttpStatus.UNAUTHORIZED, ErrorMessages.UNAUTHORIZED);
    }
}
