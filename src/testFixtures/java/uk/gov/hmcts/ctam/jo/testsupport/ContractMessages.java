package uk.gov.hmcts.ctam.jo.testsupport;

/**
 * The fixed error messages, typed from contracts/reference-data-api.md § Error messages and, for the People routes,
 * 013's contracts/people-api.md § Errors.
 *
 * <p>Tests assert against these, never against main code's own constants ({@code ErrorMessages}, the
 * exceptions' {@code MESSAGE}), so a message changed in main code fails the build instead of changing the
 * contract without anyone noticing.
 */
public final class ContractMessages {

    public static final String UNSUPPORTED_ATTRIBUTE_NAME = "Unsupported reference data attribute_name.";

    public static final String MALFORMED_REFERENCE_ID = "reference_id must be a non-negative whole number.";

    public static final String QUERY_PARAMETERS_NOT_SUPPORTED = "Query parameters are not supported on this endpoint.";

    public static final String UNAUTHORIZED = "Unauthorized. Invalid or missing token.";

    public static final String RECORD_NOT_FOUND = "Reference data record not found.";

    public static final String RESOURCE_NOT_FOUND = "Resource not found.";

    public static final String METHOD_NOT_ALLOWED = "Method not allowed.";

    public static final String NOT_ACCEPTABLE = "Not acceptable.";

    public static final String INTERNAL_SERVER_ERROR = "Internal server error.";

    public static final String UPDATED_SINCE_REQUIRED = "updated_since is required.";

    public static final String UPDATED_SINCE_INVALID = "updated_since must be a valid date.";

    public static final String PAGE_INVALID = "page must be a positive whole number.";

    public static final String PER_PAGE_INVALID = "per_page must be a positive whole number.";

    public static final String FLAG_INVALID = "include_previous_appointments must be true or false.";

    public static final String UNSUPPORTED_QUERY_PARAMETER = "Unsupported query parameter.";

    public static final String REPEATED_QUERY_PARAMETER = "Query parameters must not be repeated.";

    public static final String MALFORMED_PERSON_ID = "id must be a UUID.";

    public static final String PERSON_NOT_FOUND = "Person not found.";

    private ContractMessages() {
    }
}
