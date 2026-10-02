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
     * The reference-data route prefix, for both the collection and the single-record route.
     */
    public static final String REFERENCE_DATA = "/api/v1/reference_data";

    private ApiPaths() {
    }
}
