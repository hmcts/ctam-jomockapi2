package uk.gov.hmcts.ctam.jo.config;

/**
 * Route paths shared by the controllers that serve them and the filters, interceptor and OpenAPI customiser
 * that attach to them. Declared once here so renaming a route moves everything attached to it, and so the
 * filters don't depend on the controllers package.
 */
public final class ApiPaths {

    /**
     * The healthcheck. The authentication and correlation-ID filters exempt exactly this path.
     */
    public static final String HEALTHCHECK = "/api/v1/healthcheck";

    /**
     * The platform health check: the same response as {@link #HEALTHCHECK}, at the path the HMCTS Jenkins pipeline
     * calls after every preview deploy (its library hard-codes {@code /health}; 014 research R-15). It is outside
     * {@code /api/}, so it is unauthenticated, and the correlation-ID filter exempts it as it does the healthcheck.
     */
    public static final String PLATFORM_HEALTH = "/health";

    /**
     * The reference-data route prefix, for both the collection and the single-record route.
     */
    public static final String REFERENCE_DATA = "/api/v1/reference_data";

    /**
     * The People route prefix: the search ({@code /api/v1/people}) and the single route ({@code /api/v1/people/{id}}).
     */
    public static final String PEOPLE = "/api/v1/people";

    private ApiPaths() {
    }
}
