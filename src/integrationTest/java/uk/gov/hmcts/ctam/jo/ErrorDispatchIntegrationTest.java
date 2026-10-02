package uk.gov.hmcts.ctam.jo;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.INTERNAL_SERVER_ERROR;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.TOKEN;

/**
 * An exception escaping the filter chain is rendered by the container's {@code /error} dispatch. It must still
 * use the shared shape, keep the correlation ID and expose no internals (finding C2). Real HTTP is needed:
 * MockMvc does not perform the error dispatch.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "jo.security.bearer-tokens=" + TOKEN)
class ErrorDispatchIntegrationTest {

    private static final String ESCAPE_PATH = "/api/v1/test-escape";

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void exceptionEscapingAFilterIsRenderedInTheSharedShape() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        headers.set("X-Correlation-Id", "esc-123");

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + ESCAPE_PATH,
                                                                HttpMethod.GET, new HttpEntity<>(headers),
                                                                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("esc-123");
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode body = assertErrorBody(response.getBody(), INTERNAL_SERVER_ERROR);
        assertThat(body.get("traceId").asString()).isEqualTo("esc-123");
        assertThat(response.getBody()).doesNotContain("escaped", "IllegalStateException", "secret");
    }

    @TestConfiguration
    static class EscapingFilterConfig {

        @Bean
        FilterRegistrationBean<Filter> escapingFilter() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((request, response, chain) -> {
                throw new IllegalStateException("escaped /secret");
            });
            registration.addUrlPatterns(ESCAPE_PATH);
            // After BearerTokenAuthenticationFilter (HIGHEST_PRECEDENCE + 10), so the request is authenticated.
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
            return registration;
        }
    }
}
