package uk.gov.hmcts.ctam.jo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.List;

/**
 * One office holder as stored in the People fixture (013 data-model.md § 2). It holds only reference ids, never the
 * names they stand for: those are resolved from the served reference data when the fixture is loaded. Boxed types, so
 * the loader can tell a missing value from a zero or false one; whether a key may be missing or null is checked on
 * the JSON before binding.
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = false)
public class OfficeHolderRecord {

    @JsonProperty("id")
    String id;

    @JsonProperty("last_updated")
    LocalDate lastUpdated;

    @JsonProperty("per_id")
    Long perId;

    @JsonProperty("personal_code")
    String personalCode;

    @JsonProperty("title")
    String title;

    @JsonProperty("known_as")
    String knownAs;

    @JsonProperty("surname")
    String surname;

    @JsonProperty("fullname")
    String fullname;

    @JsonProperty("initials")
    String initials;

    @JsonProperty("post_nominals")
    String postNominals;

    @JsonProperty("email")
    String email;

    /**
     * Null when the office holder has none; the response then leaves the field out.
     */
    @JsonProperty("email_personal")
    String emailPersonal;

    @JsonProperty("gender_id")
    Long genderId;

    /**
     * Whether the response writes the optional {@code gender} field as well as {@code sex}.
     */
    @JsonProperty("show_gender")
    Boolean showGender;

    @JsonProperty("work_phone")
    String workPhone;

    @JsonProperty("disability")
    Boolean disability;

    @JsonProperty("retirement_date")
    LocalDate retirementDate;

    @JsonProperty("leaving_on")
    LocalDate leavingOn;

    @JsonProperty("appointments")
    List<AppointmentRecord> appointments;

    @JsonProperty("judiciary_roles")
    List<JudiciaryRoleRecord> judiciaryRoles;

    /**
     * The dated authorisations.
     */
    @JsonProperty("authorisations")
    List<AuthorisationRecord> authorisations;
}
