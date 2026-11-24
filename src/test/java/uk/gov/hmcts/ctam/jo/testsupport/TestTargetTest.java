package uk.gov.hmcts.ctam.jo.testsupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTarget.BASE_URL_PROPERTY;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTarget.TOKEN_PROPERTY;

class TestTargetTest {

    private static final int PORT = 8123;

    @Test
    void usesTheLocalPortWhenNoAddressIsConfigured() {
        withProperty(BASE_URL_PROPERTY, null,
                     () -> assertThat(TestTarget.baseUrl(PORT)).isEqualTo("http://localhost:8123"));
    }

    @Test
    void usesTheConfiguredAddressWithoutItsTrailingSlash() {
        withProperty(BASE_URL_PROPERTY, "https://x.example/",
                     () -> assertThat(TestTarget.baseUrl(PORT)).isEqualTo("https://x.example"));
    }

    @Test
    void ignoresABlankConfiguredAddress() {
        withProperty(BASE_URL_PROPERTY, "  ",
                     () -> assertThat(TestTarget.baseUrl(PORT)).isEqualTo("http://localhost:8123"));
    }

    @Test
    void usesTheTestTokenWhenNoTokenIsConfigured() {
        withProperty(TOKEN_PROPERTY, null, () -> {
            assertThat(TestTarget.token()).isEqualTo(TestTokens.TOKEN);
            assertThat(TestTarget.bearerToken()).isEqualTo(TestTokens.BEARER_TOKEN);
        });
    }

    @Test
    void usesTheConfiguredToken() {
        withProperty(TOKEN_PROPERTY, " deployed-token ", () -> {
            assertThat(TestTarget.token()).isEqualTo("deployed-token");
            assertThat(TestTarget.bearerToken()).isEqualTo("Bearer deployed-token");
        });
    }

    @Test
    void ignoresABlankConfiguredToken() {
        withProperty(TOKEN_PROPERTY, "", () -> assertThat(TestTarget.token()).isEqualTo(TestTokens.TOKEN));
    }

    /**
     * Runs the check with the property set to the value (or cleared for null), then restores its previous value so
     * other unit tests are unaffected.
     */
    private static void withProperty(String property, String value, Runnable check) {
        String previous = value == null ? System.clearProperty(property) : System.setProperty(property, value);
        try {
            check.run();
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }
    }
}
