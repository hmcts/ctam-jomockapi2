package uk.gov.hmcts.ctam.jo.exceptions;

/**
 * A query parameter's value is missing, empty or malformed. Each message names the parameter, from the caller's
 * constant, never from caller input (research R5).
 */
public final class InvalidQueryParameterException extends ApiRequestException {

    private static final long serialVersionUID = 1L;

    private InvalidQueryParameterException(String message, String diagnostic) {
        super(message, diagnostic);
    }

    /**
     * The failure "{@code <name>} is required.".
     */
    public static InvalidQueryParameterException required(String name) {
        return new InvalidQueryParameterException(name + " is required.", name + " missing or empty");
    }

    /**
     * The failure "{@code <name>} must be a valid date.".
     */
    public static InvalidQueryParameterException invalidDate(String name) {
        return new InvalidQueryParameterException(name + " must be a valid date.", name + " is not YYYY-MM-DD");
    }

    /**
     * The failure "{@code <name>} must be a positive whole number.".
     */
    public static InvalidQueryParameterException notPositiveWholeNumber(String name) {
        return new InvalidQueryParameterException(name + " must be a positive whole number.",
                                                  name + " is not a positive whole number");
    }

    /**
     * The failure "{@code <name>} must be true or false.".
     */
    public static InvalidQueryParameterException notBoolean(String name) {
        return new InvalidQueryParameterException(name + " must be true or false.", name + " is not true or false");
    }
}
