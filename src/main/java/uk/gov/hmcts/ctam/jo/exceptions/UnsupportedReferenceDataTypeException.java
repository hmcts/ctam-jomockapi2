package uk.gov.hmcts.ctam.jo.exceptions;

public class UnsupportedReferenceDataTypeException extends ApiRequestException {

    public static final String MESSAGE = "Unsupported reference data attribute_name.";

    private static final long serialVersionUID = 1L;

    public UnsupportedReferenceDataTypeException(String diagnostic) {
        super(MESSAGE, diagnostic);
    }
}
