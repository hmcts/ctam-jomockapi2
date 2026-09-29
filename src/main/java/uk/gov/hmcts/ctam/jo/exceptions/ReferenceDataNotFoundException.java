package uk.gov.hmcts.ctam.jo.exceptions;

public class ReferenceDataNotFoundException extends ApiRequestException {

    public static final String MESSAGE = "Reference data record not found.";

    private static final long serialVersionUID = 1L;

    public ReferenceDataNotFoundException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
