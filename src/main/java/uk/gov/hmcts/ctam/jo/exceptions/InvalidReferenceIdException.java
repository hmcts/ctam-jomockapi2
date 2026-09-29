package uk.gov.hmcts.ctam.jo.exceptions;

public class InvalidReferenceIdException extends ApiRequestException {

    public static final String MESSAGE = "reference_id must be a non-negative whole number.";

    private static final long serialVersionUID = 1L;

    public InvalidReferenceIdException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
