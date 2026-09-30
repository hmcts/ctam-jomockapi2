package uk.gov.hmcts.ctam.jo.testsupport;

/**
 * Routes typed from contracts/reference-data-api.md. Tests call these, never main code's own route constants,
 * so a renamed route fails the build instead of changing the contract without anyone noticing.
 */
public final class ContractPaths {

    public static final String APPOINTMENT_TITLES = "/api/v1/reference_data/appointment_titles";

    private ContractPaths() {
    }
}
