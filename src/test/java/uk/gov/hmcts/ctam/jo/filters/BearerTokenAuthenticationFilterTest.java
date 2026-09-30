package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.ctam.jo.config.SecurityProperties;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.errorResponseFactory;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.servletRequest;

class BearerTokenAuthenticationFilterTest {

    private static final String PROTECTED = "/api/v1/reference_data/types";

    private final BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(
            new SecurityProperties(List.of("other-token", "test-token")),
            errorResponseFactory(Instant.EPOCH));

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final AtomicBoolean passedThrough = new AtomicBoolean();

    private final FilterChain chain = (req, res) -> passedThrough.set(true);

    @Test
    void passesAValidToken() throws Exception {
        filter.doFilter(request(PROTECTED, "Bearer test-token"), response, chain);

        assertThat(passedThrough).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void acceptsTheSchemeCaseInsensitively() throws Exception {
        filter.doFilter(request(PROTECTED, "bearer test-token"), response, chain);

        assertThat(passedThrough).isTrue();
    }

    @Test
    void rejectsAMissingHeader() throws Exception {
        filter.doFilter(request(PROTECTED, null), response, chain);

        assertRejected();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic abc", "Bearer", "Bearer ", "Bearer wrong", "Bearer test-token ",
        "Bearer  test-token", "test-token", ""})
    void rejectsInvalidHeaders(String authorization) throws Exception {
        filter.doFilter(request(PROTECTED, authorization), response, chain);

        assertRejected();
    }

    @Test
    void neverEchoesThePresentedToken() throws Exception {
        filter.doFilter(request(PROTECTED, "Bearer leaked-secret"), response, chain);

        assertThat(response.getContentAsString()).doesNotContain("leaked-secret");
    }

    @Test
    void exemptsTheHealthcheck() throws Exception {
        filter.doFilter(request("/api/v1/healthcheck", null), response, chain);

        assertThat(passedThrough).isTrue();
    }

    @Test
    void doesNotFilterApiDocs() throws Exception {
        filter.doFilter(request("/v3/api-docs", null), response, chain);

        assertThat(passedThrough).isTrue();
    }

    @Test
    void doesNotFilterPathsOutsideApi() throws Exception {
        filter.doFilter(request("/swagger-ui/index.html", null), response, chain);

        assertThat(passedThrough).isTrue();
    }

    @Test
    void protectsPathsThatOnlyStartWithTheHealthcheck() throws Exception {
        filter.doFilter(request("/api/v1/healthcheck/extra", null), response, chain);

        assertRejected();
    }

    @Test
    void decidesOnTheDecodedPathNotTheRawUri() throws Exception {
        MockHttpServletRequest request = servletRequest("/%61pi/v1/reference_data/types", PROTECTED);

        filter.doFilter(request, response, chain);

        assertRejected();
    }

    private void assertRejected() throws Exception {
        assertThat(passedThrough).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"error\":\"" + UNAUTHORIZED + "\"");
    }

    private static MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = servletRequest(path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
