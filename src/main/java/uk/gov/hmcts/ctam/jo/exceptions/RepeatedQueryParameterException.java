package uk.gov.hmcts.ctam.jo.exceptions;

/**
 * A query parameter given more than once, whatever its values (FR-016).
 */
public class RepeatedQueryParameterException extends ApiRequestException {

    public static final String MESSAGE = "Query parameters must not be repeated.";

    private static final long serialVersionUID = 1L;

    public RepeatedQueryParameterException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
