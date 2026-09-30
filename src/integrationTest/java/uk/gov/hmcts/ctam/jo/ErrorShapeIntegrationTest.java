package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.NOT_ACCEPTABLE;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * The shared error shape and the order of checks across the full filter chain and MVC (research R3, R7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorShapeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownRouteWithATokenReturns404ResourceNotFound() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/does-not-exist")
                .header("Authorization", BEARER_TOKEN)
                .header("X-Correlation-Id", "it-123")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(404);
        JsonNode body = assertSharedShape(response, RESOURCE_NOT_FOUND);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("it-123");
        assertThat(body.get("traceId").asString()).isEqualTo("it-123");
    }

    @Test
    void wrongMethodOnTheHealthcheckReturns405WithNullTraceId() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/healthcheck")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(405);
        JsonNode body = assertSharedShape(response, METHOD_NOT_ALLOWED);
        assertThat(body.get("traceId").isNull()).isTrue();
        assertThat(response.getHeader("X-Correlation-Id")).isNull();
        assertThat(response.getHeader("Allow")).isEqualTo("GET");
    }

    @Test
    void authenticationRunsBeforeRouting() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/does-not-exist")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertSharedShape(response, UNAUTHORIZED);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void nonJsonAcceptReturns406AsJson() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get(APPOINTMENT_TITLES)
                .header("Authorization", BEARER_TOKEN)
                .header("Accept", "text/plain")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(406);
        JsonNode body = assertSharedShape(response, NOT_ACCEPTABLE);
        assertThat(body.get("traceId").asString()).isEqualTo(response.getHeader("X-Correlation-Id"));
    }

    @Test
    void healthcheckSucceedsWithoutACorrelationIdHeader() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/healthcheck")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-Correlation-Id")).isNull();
    }

    private JsonNode assertSharedShape(MockHttpServletResponse response, String message) throws Exception {
        assertThat(response.getContentType()).startsWith("application/json");
        return assertErrorBody(response.getContentAsString(), message);
    }
}
