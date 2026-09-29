package uk.gov.hmcts.ctam.jo;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * An exception escaping the filter chain is rendered by the container's {@code /error} dispatch. It must still
 * use the shared shape, keep the correlation ID and expose no internals (finding C2). Real HTTP is needed:
 * MockMvc does not perform the error dispatch.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "jo.security.bearer-tokens=test-token")
class ErrorDispatchIntegrationTest {

    private static final String ESCAPE_PATH = "/api/v1/test-escape";

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void exceptionEscapingAFilterIsRenderedInTheSharedShape() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-token");
        headers.set("X-Correlation-Id", "esc-123");

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port + ESCAPE_PATH,
                                                                HttpMethod.GET, new HttpEntity<>(headers),
                                                                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("esc-123");
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode body = jsonMapper.readTree(response.getBody());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo("Internal server error.");
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
