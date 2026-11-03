package uk.gov.hmcts.ctam.jo.exceptions;

/**
 * A person id that isn't a canonical UUID (FR-005).
 */
public class InvalidPersonIdException extends ApiRequestException {

    public static final String MESSAGE = "id must be a UUID.";

    private static final long serialVersionUID = 1L;

    public InvalidPersonIdException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
