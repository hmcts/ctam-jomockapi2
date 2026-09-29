package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * A caller whose Accept header excludes JSON still gets the shared error shape, as JSON (spec EC-008, FR-025).
 * Uses the real service: a mocked one would hide how the route itself negotiates.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotAcceptableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void nonJsonAcceptReturns406AsJson() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/reference_data/appointment_titles")
                .header("Authorization", "Bearer test-token")
                .header("Accept", "text/plain")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(406);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo("Not acceptable.");
        assertThat(body.get("traceId").asString()).isEqualTo(response.getHeader("X-Correlation-Id"));
    }
}
