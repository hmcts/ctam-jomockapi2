package uk.gov.hmcts.ctam.jo.exceptions;

/**
 * Fixed error messages from the contract that are not tied to one exception class. They never echo
 * caller input.
 */
public final class ErrorMessages {

    public static final String UNAUTHORIZED = "Unauthorized. Invalid or missing token.";

    public static final String RESOURCE_NOT_FOUND = "Resource not found.";

    public static final String METHOD_NOT_ALLOWED = "Method not allowed.";

    public static final String NOT_ACCEPTABLE = "Not acceptable.";

    public static final String INTERNAL_SERVER_ERROR = "Internal server error.";

    private ErrorMessages() {
    }
}
