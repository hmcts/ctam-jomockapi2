package uk.gov.hmcts.ctam.jo.entity;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * An office holder as served: a validated fixture record with every name resolved from the served reference data
 * (013 data-model.md § 3), and every list in its FR-025 order. Built once, when the fixture is loaded; responses are
 * mapped from it per request.
 */
@Value
@Builder
public class OfficeHolder {

    String id;

    LocalDate lastUpdated;

    long perId;

    String personalCode;

    String title;

    String knownAs;

    String surname;

    String fullname;

    String initials;

    String postNominals;

    String email;

    String emailPersonal;

    /**
     * The name of the gender_id record, served as sex and, when {@link #showGender} is true, as gender.
     */
    String sex;

    boolean showGender;

    long genderId;

    String workPhone;

    boolean disability;

    LocalDate retirementDate;

    LocalDate leavingOn;

    /**
     * Every appointment, current and previous, by start date, then appointment id.
     */
    List<ResolvedAppointment> appointments;

    /**
     * Every judiciary role, by start date, then judiciary_role_id.
     */
    List<ResolvedJudiciaryRole> judiciaryRoles;

    /**
     * Every dated authorisation, under current and previous appointments, by start date, then authorisation id.
     */
    List<ResolvedAuthorisation> authorisationsWithDates;

    /**
     * An appointment with its names resolved: role name, contract type, and the base location's own name, the name
     * of its type (court type), of its parent (location) and of its jurisdiction (type).
     */
    @Value
    @Builder
    public static class ResolvedAppointment {

        long appointmentId;

        String roleName;

        long roleNameId;

        String type;

        String courtName;

        String courtType;

        String circuit;

        String bench;

        String advisoryCommitteeArea;

        String location;

        String baseLocation;

        long baseLocationId;

        boolean principal;

        LocalDate startDate;

        LocalDate endDate;

        String contractType;

        long contractTypeId;
    }

    /**
     * A judiciary role with its name resolved from judiciary_role_name_id.
     */
    @Value
    @Builder
    public static class ResolvedJudiciaryRole {

        String name;

        long judiciaryRoleId;

        long judiciaryRoleNameId;

        LocalDate startDate;

        LocalDate endDate;
    }

    /**
     * A dated authorisation with its ticket name, and the id and name of the jurisdiction of the ticket's category.
     */
    @Value
    @Builder
    public static class ResolvedAuthorisation {

        long authorisationId;

        long appointmentId;

        String jurisdiction;

        long jurisdictionId;

        String ticket;

        long ticketId;

        LocalDate startDate;

        LocalDate endDate;
    }
}
