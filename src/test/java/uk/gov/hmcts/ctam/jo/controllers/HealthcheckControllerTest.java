package uk.gov.hmcts.ctam.jo.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.hmcts.ctam.jo.config.SecurityProperties;
import uk.gov.hmcts.ctam.jo.config.WebConfig;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponseFactory;
import uk.gov.hmcts.ctam.jo.filters.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIdFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.TOKEN;

@WebMvcTest(HealthcheckController.class)
@Import({WebConfig.class, ErrorResponseFactory.class, CorrelationIdFilter.class, BearerTokenAuthenticationFilter.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "jo.security.bearer-tokens=" + TOKEN)
class HealthcheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthcheckResponseContainsOnlyStatusFieldAndNoSensitiveHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/healthcheck"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(1)))
                .andExpect(jsonPath("$.status").value("ok"))
                .andReturn();

        MockHttpServletResponse response = result.getResponse();

        assertThat(response.getHeaderNames())
                .noneMatch(name -> name.equalsIgnoreCase("X-Application-Context")
                        || name.equalsIgnoreCase("Server")
                        || name.toLowerCase().contains("stack"));
        assertThat(response.getContentAsString())
                .doesNotContainIgnoringCase("exception")
                .doesNotContainIgnoringCase("stacktrace")
                .doesNotContain("java.lang");
    }

    @Test
    void healthcheckIgnoresUnexpectedQueryParameterAndBody() throws Exception {
        mockMvc.perform(get("/api/v1/healthcheck")
                        .param("unexpected", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ignored\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void healthcheckReturnsConsistentResultUnderConcurrentAccess() throws Exception {
        int callCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < callCount; i++) {
                calls.add(() -> mockMvc.perform(get("/api/v1/healthcheck"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
            }

            List<Future<String>> results = executor.invokeAll(calls);
            for (Future<String> result : results) {
                assertThat(result.get()).isEqualTo("{\"status\":\"ok\"}");
            }
        } finally {
            executor.shutdown();
        }
    }
}
