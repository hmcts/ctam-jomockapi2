package uk.gov.hmcts.ctam.jo.testsupport;

/**
 * The Bearer token the full-context test suites accept. It must match {@code jo.security.bearer-tokens} in
 * src/integrationTest, src/functionalTest and src/smokeTest {@code resources/application-test.yml}.
 */
public final class TestTokens {

    public static final String TOKEN = "test-token";

    /**
     * The {@code Authorization} header value that carries {@link #TOKEN}.
     */
    public static final String BEARER_TOKEN = "Bearer " + TOKEN;

    private TestTokens() {
    }
}
