package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * Structured logging (Principles IX and XIV): 4xx at WARN with the correlation ID, and no caller value able to
 * start a new log line. The 5xx-at-ERROR case is checked on the same request as the 500 response, in
 * InternalFailureIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientErrorIsLoggedAtWarnWithTheCorrelationId(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/v1/reference_data/foo").header("Authorization", BEARER_TOKEN)
                .header("X-Correlation-Id", "log-123"));

        assertThat(linesContaining(output, "\"correlationId\":\"log-123\""))
                .anySatisfy(line -> assertThat(line).contains("\"level\":\"WARN\"", "Request rejected"));
    }

    @Test
    void injectedCorrelationIdIsReplacedAndCannotStartALogLine(CapturedOutput output) throws Exception {
        String returned = mockMvc.perform(get("/api/v1/reference_data/foo").header("Authorization", BEARER_TOKEN)
                        .header("X-Correlation-Id", "a%0D%0Ainjected"))
                .andReturn().getResponse().getHeader("X-Correlation-Id");
        mockMvc.perform(get("/api/v1/reference_data/foo").header("Authorization", BEARER_TOKEN)
                .header("X-Correlation-Id", "a\r\ninjected"));

        assertThat(returned).isNotEqualTo("a%0D%0Ainjected").matches("[0-9a-f-]{36}");
        assertThat(output.getAll().lines()).noneMatch(line -> line.startsWith("injected"));
    }

    @Test
    void encodedCrlfInThePathIsLoggedSanitised(CapturedOutput output) throws Exception {
        int status = mockMvc.perform(get(URI.create("/api/v1/reference_data/foo%0D%0AFAKE"))
                .header("Authorization", BEARER_TOKEN)).andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(400);
        assertThat(output.getAll().lines()).noneMatch(line -> line.startsWith("FAKE"));
        assertThat(linesContaining(output, "FAKE")).isNotEmpty()
                .allSatisfy(line -> assertThat(line).startsWith("{"));
    }

    private static List<String> linesContaining(CapturedOutput output, String text) {
        return output.getAll().lines().filter(line -> line.contains(text)).toList();
    }
}
