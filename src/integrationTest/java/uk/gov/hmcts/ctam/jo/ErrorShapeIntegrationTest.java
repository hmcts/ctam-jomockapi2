package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The shared error shape and the order of checks across the full filter chain and MVC (research R3, R7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "jo.security.bearer-tokens=test-token")
class ErrorShapeIntegrationTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void unknownRouteWithATokenReturns404ResourceNotFound() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/does-not-exist")
                .header("Authorization", TOKEN)).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(404);
        JsonNode body = assertSharedShape(response, "Resource not found.");
        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
        assertThat(body.get("traceId").asString()).isEqualTo(response.getHeader("X-Correlation-Id"));
    }

    @Test
    void wrongMethodOnTheHealthcheckReturns405WithNullTraceId() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/healthcheck")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(405);
        JsonNode body = assertSharedShape(response, "Method not allowed.");
        assertThat(body.get("traceId").isNull()).isTrue();
        assertThat(response.getHeader("X-Correlation-Id")).isNull();
        assertThat(response.getHeader("Allow")).isEqualTo("GET");
    }

    @Test
    void authenticationRunsBeforeRouting() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/does-not-exist")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertSharedShape(response, "Unauthorized. Invalid or missing token.");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void suppliedCorrelationIdIsEchoedInHeaderAndTraceId() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/does-not-exist")
                .header("Authorization", TOKEN)
                .header("X-Correlation-Id", "it-123")).andReturn().getResponse();

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("it-123");
        assertThat(assertSharedShape(response, "Resource not found.").get("traceId").asString()).isEqualTo("it-123");
    }

    @Test
    void healthcheckSucceedsWithoutACorrelationIdHeader() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/healthcheck")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-Correlation-Id")).isNull();
    }

    private JsonNode assertSharedShape(MockHttpServletResponse response, String message) throws Exception {
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo(message);
        assertThat(body.get("timestamp").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        return body;
    }
}
