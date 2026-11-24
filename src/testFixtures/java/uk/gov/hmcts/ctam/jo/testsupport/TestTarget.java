package uk.gov.hmcts.ctam.jo.testsupport;

/**
 * The copy of the service the full-context suites send their HTTP requests to, and the token they send. By default
 * that is the copy the test started on its random local port, with {@link TestTokens#TOKEN}. The deployed-copy Gradle
 * tasks {@code smoke} and {@code functional} set {@link #BASE_URL_PROPERTY} from {@code TEST_URL} and
 * {@link #TOKEN_PROPERTY} from {@code TEST_BEARER_TOKEN}, so the same scenarios test a running copy instead
 * (014 FR-015). {@link TestTokens} stays a set of compile-time constants because test annotations use it.
 */
public final class TestTarget {

    /**
     * The system property holding the base address of a running copy, for example {@code https://host}.
     */
    public static final String BASE_URL_PROPERTY = "jo.test.base-url";

    /**
     * The system property holding the token to send to a running copy.
     */
    public static final String TOKEN_PROPERTY = "jo.test.token";

    private TestTarget() {
    }

    /**
     * The base address to call, with no trailing slash: {@link #BASE_URL_PROPERTY} when it is set and not blank,
     * otherwise {@code http://localhost:<localPort>}.
     */
    public static String baseUrl(int localPort) {
        String configured = configured(BASE_URL_PROPERTY);
        if (configured == null) {
            return "http://localhost:" + localPort;
        }
        return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
    }

    /**
     * The token to send: {@link #TOKEN_PROPERTY} when it is set and not blank, otherwise {@link TestTokens#TOKEN}.
     */
    public static String token() {
        String configured = configured(TOKEN_PROPERTY);
        return configured == null ? TestTokens.TOKEN : configured;
    }

    /**
     * The {@code Authorization} header value that carries {@link #token()}.
     */
    public static String bearerToken() {
        return "Bearer " + token();
    }

    private static String configured(String property) {
        String value = System.getProperty(property);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
