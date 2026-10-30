package uk.gov.hmcts.ctam.jo.testsupport;

/**
 * Routes typed from contracts/reference-data-api.md. Tests call these, never main code's own route constants,
 * so a renamed route fails the build instead of changing the contract without anyone noticing.
 */
public final class ContractPaths {

    public static final String APPOINTMENT_TITLES = "/api/v1/reference_data/appointment_titles";

    public static final String BASE_LOCATIONS = "/api/v1/reference_data/base_locations";

    public static final String CONTRACT_TYPES = "/api/v1/reference_data/contract_types";

    public static final String GENDERS = "/api/v1/reference_data/genders";
    public static final String JUDICIARY_ROLES = "/api/v1/reference_data/judiciary_roles";

    public static final String LOCATIONS = "/api/v1/reference_data/locations";

    public static final String LOCATION_TYPES = "/api/v1/reference_data/location_types";

    public static final String JURISDICTIONS = "/api/v1/reference_data/jurisdictions";

    public static final String TICKETS = "/api/v1/reference_data/tickets";

    public static final String TICKET_CATEGORIES = "/api/v1/reference_data/ticket_categories";

    private ContractPaths() {
    }
}
