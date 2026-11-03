package uk.gov.hmcts.ctam.jo.exceptions;

/**
 * A well-formed person id that matches no one (FR-006).
 */
public class PersonNotFoundException extends ApiRequestException {

    public static final String MESSAGE = "Person not found.";

    private static final long serialVersionUID = 1L;

    public PersonNotFoundException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
