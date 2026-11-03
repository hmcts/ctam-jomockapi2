package uk.gov.hmcts.ctam.jo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * The People dataset ({@code jo.people}, research R4): the fixture's location, the reference date that decides whether
 * an appointment is current (deviation D-8), and which configured reference-data type each People reference names.
 * Type names live only in configuration, never in Java (013 FR-052). Every setting is required.
 *
 * @param fixture        the People fixture, as a Spring resource location
 * @param referenceDate  the date standing in for "today": an appointment ending before it is previous (FR-020)
 * @param referenceTypes the reference-data type each People reference names
 */
@Validated
@ConfigurationProperties("jo.people")
public record PeopleProperties(
        @NotBlank String fixture,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
        @Valid @NotNull ReferenceTypes referenceTypes) {

    /**
     * The canonical name of the reference-data type that each People reference names. The type base_location_id
     * names must declare {@code type_id}, {@code parent_id} and {@code jurisdiction_id}, the type ticket_id names must
     * declare {@code ticket_category_id}, and the type a ticket's ticket_category_id names must declare
     * {@code jurisdiction_id}; that is checked when the fixture is loaded.
     *
     * @param genderId             the type a gender_id names
     * @param roleNameId           the type an appointment's role_name_id names
     * @param contractTypeId       the type a contract_type_id names
     * @param baseLocationId       the type a base_location_id names
     * @param baseLocationTypeId   the type a base location's type_id names (court_type)
     * @param baseLocationParentId the type a base location's parent_id names (location)
     * @param jurisdictionId       the type every jurisdiction_id names
     * @param judiciaryRoleNameId  the type a judiciary_role_name_id names
     * @param ticketId             the type a ticket_id names
     * @param ticketCategoryId     the type a ticket's ticket_category_id names
     */
    public record ReferenceTypes(
            @NotBlank String genderId,
            @NotBlank String roleNameId,
            @NotBlank String contractTypeId,
            @NotBlank String baseLocationId,
            @NotBlank String baseLocationTypeId,
            @NotBlank String baseLocationParentId,
            @NotBlank String jurisdictionId,
            @NotBlank String judiciaryRoleNameId,
            @NotBlank String ticketId,
            @NotBlank String ticketCategoryId) {
    }
}
