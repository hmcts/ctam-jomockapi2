package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Tokens are configurable but authentication cannot be turned off (spec AC-017, FR-017). No class here
 * activates the {@code test} profile: its {@code application-test.yml} sets the token list directly, which
 * would hide the {@code JO_SECURITY_BEARER_TOKENS} placeholder these tests exercise.
 */
class AuthenticationConfigurationIntegrationTest {

    private static final String EXPECTED_STARTUP_FAILURE =
            "jo.security.bearer-tokens must contain at least one non-blank token";

    private static final String COLLECTION = "/api/v1/reference_data/appointment_titles";

    static int status(MockMvc mockMvc, String token) throws Exception {
        return mockMvc.perform(get(COLLECTION).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    /**
     * Starts the real application with the given properties as command-line arguments, which outrank
     * application.yml (SpringApplicationBuilder.properties() would only set defaults, which it overrides).
     */
    static void assertStartupFails(String... properties) {
        String[] args = Arrays.stream(properties).map(property -> "--" + property).toArray(String[]::new);
        assertThatThrownBy(() -> new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(args).close())
                .hasRootCauseMessage(EXPECTED_STARTUP_FAILURE);
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "jo.security.bearer-tokens=rotated-token")
    class RotatedToken {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void ac017TheNewTokenIsAcceptedAndOldOnesAreRejected() throws Exception {
            assertThat(status(mockMvc, "rotated-token")).isEqualTo(200);
            assertThat(status(mockMvc, "test-token")).isEqualTo(401);
            assertThat(status(mockMvc, "local-dev-token")).isEqualTo(401);
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = {"jo.security.bearer-tokens[0]=a", "jo.security.bearer-tokens[1]=b"})
    class TwoTokens {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void bothConfiguredTokensAreAccepted() throws Exception {
            assertThat(status(mockMvc, "a")).isEqualTo(200);
            assertThat(status(mockMvc, "b")).isEqualTo(200);
            assertThat(status(mockMvc, "test-token")).isEqualTo(401);
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "JO_SECURITY_BEARER_TOKENS=env-a,env-b")
    class EnvironmentOverride {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void theEnvironmentVariableReplacesTheDefaultToken() throws Exception {
            assertThat(status(mockMvc, "env-a")).isEqualTo(200);
            assertThat(status(mockMvc, "env-b")).isEqualTo(200);
            assertThat(status(mockMvc, "local-dev-token")).isEqualTo(401);
            assertThat(status(mockMvc, "test-token")).isEqualTo(401);
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ExtendWith(OutputCaptureExtension.class)
    @TestPropertySource(properties = "jo.security.bearer-tokens=test-token")
    class TokensAreNeverLogged {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void neitherARejectedNorAnAcceptedTokenAppearsInTheLogs(CapturedOutput output) throws Exception {
            assertThat(status(mockMvc, "super-secret-value")).isEqualTo(401);
            assertThat(status(mockMvc, "test-token")).isEqualTo(200);

            assertThat(output.getAll())
                    .contains("Authentication failed")
                    .doesNotContain("super-secret-value", "test-token");
        }
    }

    @Nested
    class CannotBeTurnedOff {

        @ParameterizedTest
        @ValueSource(strings = {"jo.security.bearer-tokens=", "JO_SECURITY_BEARER_TOKENS="})
        void emptyTokenSetStopsStartup(String property) {
            assertStartupFails(property);
        }

        @Test
        void blankTokenStopsStartup() {
            assertStartupFails("jo.security.bearer-tokens[0]=a", "jo.security.bearer-tokens[1]= ");
        }
    }
}
