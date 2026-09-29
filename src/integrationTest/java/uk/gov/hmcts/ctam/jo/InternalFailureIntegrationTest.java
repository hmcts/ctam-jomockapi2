package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * An unexpected failure returns 500 in the shared shape and exposes no internals (spec EC-010, FR-026).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ReferenceDataService service;

    @Test
    void unexpectedExceptionReturns500WithoutInternals() throws Exception {
        given(service.getAll(anyString())).willThrow(new IllegalStateException("boom /secret/path"));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/reference_data/appointment_titles")
                .header("Authorization", "Bearer test-token")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(500);
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo("Internal server error.");
        assertThat(response.getContentAsString())
                .doesNotContain("boom", "IllegalStateException", "/secret/path", "at uk.gov");
    }
}
