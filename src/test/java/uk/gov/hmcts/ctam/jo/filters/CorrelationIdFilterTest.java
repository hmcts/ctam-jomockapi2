package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.servletRequest;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final AtomicReference<String> mdcDuringChain = new AtomicReference<>();

    private final FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(CorrelationIds.MDC_KEY));

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void echoesAValidInboundHeader() throws Exception {
        MockHttpServletRequest request = servletRequest("/api/v1/reference_data/x");
        request.addHeader(CorrelationIds.HEADER, "abc-123_X.9");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER)).isEqualTo("abc-123_X.9");
        assertThat(mdcDuringChain.get()).isEqualTo("abc-123_X.9");
        assertThat(request.getAttribute(CorrelationIds.ATTRIBUTE)).isEqualTo("abc-123_X.9");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc\r\nInjected: header",
        "has spaces",
        "12345678901234567890123456789012345678901234567890123456789012345",
        ""
    })
    void replacesAnInvalidInboundHeaderWithAUuid(String invalid) throws Exception {
        MockHttpServletRequest request = servletRequest("/api/v1/reference_data/x");
        request.addHeader(CorrelationIds.HEADER, invalid);

        filter.doFilter(request, response, chain);

        String generated = response.getHeader(CorrelationIds.HEADER);
        assertThat(generated).isNotEqualTo(invalid);
        assertThat(UUID.fromString(generated)).hasToString(generated);
    }

    @Test
    void acceptsExactly64Characters() throws Exception {
        String maxLength = "a".repeat(64);
        MockHttpServletRequest request = servletRequest("/api/v1/reference_data/x");
        request.addHeader(CorrelationIds.HEADER, maxLength);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER)).isEqualTo(maxLength);
    }

    @Test
    void generatesAUuidWhenTheHeaderIsMissing() throws Exception {
        filter.doFilter(servletRequest("/api/v1/reference_data/x"), response, chain);

        String generated = response.getHeader(CorrelationIds.HEADER);
        assertThat(UUID.fromString(generated)).hasToString(generated);
        assertThat(mdcDuringChain.get()).isEqualTo(generated);
    }

    @Test
    void clearsMdcAfterwardsButKeepsTheRequestAttribute() throws Exception {
        MockHttpServletRequest request = servletRequest("/api/v1/reference_data/x");
        request.addHeader(CorrelationIds.HEADER, "keep-me");

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
        assertThat(request.getAttribute(CorrelationIds.ATTRIBUTE)).isEqualTo("keep-me");
    }

    @Test
    void clearsMdcEvenWhenTheChainThrows() {
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(servletRequest("/api/v1/reference_data/x"), response, failing);
        } catch (Exception expected) {
            // the exception itself is not under test
        }

        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
    }

    @Test
    void skipsTheHealthcheck() throws Exception {
        MockHttpServletRequest request = servletRequest("/api/v1/healthcheck");
        request.addHeader(CorrelationIds.HEADER, "ignored");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER)).isNull();
        assertThat(request.getAttribute(CorrelationIds.ATTRIBUTE)).isNull();
        assertThat(mdcDuringChain.get()).isNull();
    }

    @Test
    void doesNotSkipPathsThatOnlyStartWithTheHealthcheck() throws Exception {
        filter.doFilter(servletRequest("/api/v1/healthcheck/extra"), response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER)).isNotNull();
    }
}
