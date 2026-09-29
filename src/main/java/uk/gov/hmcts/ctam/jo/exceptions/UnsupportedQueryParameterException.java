package uk.gov.hmcts.ctam.jo.exceptions;

public class UnsupportedQueryParameterException extends ApiRequestException {

    public static final String MESSAGE = "Query parameters are not supported on this endpoint.";

    private static final long serialVersionUID = 1L;

    public UnsupportedQueryParameterException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
