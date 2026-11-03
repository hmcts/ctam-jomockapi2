package uk.gov.hmcts.ctam.jo.exceptions;

/**
 * A query parameter the route doesn't declare (FR-015).
 */
public class UndefinedQueryParameterException extends ApiRequestException {

    public static final String MESSAGE = "Unsupported query parameter.";

    private static final long serialVersionUID = 1L;

    public UndefinedQueryParameterException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
