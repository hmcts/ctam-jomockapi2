package uk.gov.hmcts.ctam.jo.filters;

/**
 * Names shared by everything that reads or writes the request's correlation ID.
 */
public final class CorrelationIds {

    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

    /**
     * An inbound {@link #HEADER} value is used only if it matches this; otherwise a UUID is generated.
     * Enforced by {@link CorrelationIdFilter} and published in the OpenAPI document, from this one place.
     */
    public static final String VALID_PATTERN = "^[A-Za-z0-9._-]{1,64}$";

    /**
     * Request attribute that keeps the ID available on the container's {@code /error} dispatch.
     */
    public static final String ATTRIBUTE = "uk.gov.hmcts.ctam.jo.correlationId";

    private CorrelationIds() {
    }
}
