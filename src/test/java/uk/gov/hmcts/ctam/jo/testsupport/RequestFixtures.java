package uk.gov.hmcts.ctam.jo.testsupport;

import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponseFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Servlet requests and the error writer, as the filter and handler unit tests need them.
 */
public final class RequestFixtures {

    private RequestFixtures() {
    }

    /**
     * A GET whose container-decoded servlet path equals {@code path}, as the filters see a normal request.
     */
    public static MockHttpServletRequest servletRequest(String path) {
        return servletRequest(path, path);
    }

    /**
     * A GET whose raw request URI and decoded servlet path differ, e.g. {@code /%61pi/...} and {@code /api/...}.
     */
    public static MockHttpServletRequest servletRequest(String rawUri, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", rawUri);
        request.setServletPath(servletPath);
        return request;
    }

    /**
     * An {@link ErrorResponseFactory} whose clock always reads {@code now}.
     */
    public static ErrorResponseFactory errorResponseFactory(Instant now) {
        return new ErrorResponseFactory(Clock.fixed(now, ZoneOffset.UTC), JsonMapper.builder().build());
    }
}
