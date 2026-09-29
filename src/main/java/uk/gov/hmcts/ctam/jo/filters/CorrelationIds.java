package uk.gov.hmcts.ctam.jo.filters;

/**
 * Names shared by everything that reads or writes the request's correlation ID.
 */
public final class CorrelationIds {

    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

    /**
     * Request attribute that keeps the ID available on the container's {@code /error} dispatch.
     */
    public static final String ATTRIBUTE = "uk.gov.hmcts.ctam.jo.correlationId";

    private CorrelationIds() {
    }
}
