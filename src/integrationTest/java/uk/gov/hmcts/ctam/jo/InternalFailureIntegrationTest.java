package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.INTERNAL_SERVER_ERROR;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * An unexpected failure returns 500 in the shared shape and exposes no internals (spec EC-010, FR-026), and is
 * logged at ERROR with the correlation ID (Principles IX and XIV).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class InternalFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferenceDataService service;

    @Test
    void unexpectedExceptionReturns500WithoutInternalsAndIsLoggedAtError(CapturedOutput output) throws Exception {
        given(service.getAll(anyString())).willThrow(new IllegalStateException("boom /secret/path"));

        MockHttpServletResponse response = mockMvc.perform(get(APPOINTMENT_TITLES)
                .header("Authorization", BEARER_TOKEN)
                .header("X-Correlation-Id", "int-500")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(500);
        JsonNode body = assertErrorBody(response.getContentAsString(), INTERNAL_SERVER_ERROR);
        assertThat(body.get("traceId").asString()).isEqualTo("int-500");
        assertThat(response.getContentAsString())
                .doesNotContain("boom", "IllegalStateException", "/secret/path", "at uk.gov");

        assertThat(output.getAll().lines().filter(line -> line.contains("\"correlationId\":\"int-500\"")))
                .anySatisfy(line -> assertThat(line).contains("\"level\":\"ERROR\"", "Unhandled exception"));
    }
}
