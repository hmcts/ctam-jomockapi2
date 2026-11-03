package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * One office holder, as both People routes return it (013 spec § Records). Fields are in the Swagger's order; the
 * optional gender and email_personal are left out when unset, and every nullable required field is always written.
 *
 * @param id the office holder's id, in lower case
 * @param lastUpdated when the record last changed
 * @param perId the personnel id
 * @param personalCode the personal code
 * @param title the title
 * @param knownAs the name the office holder is known by
 * @param surname the surname
 * @param fullname title, known-as name and surname
 * @param initials the initials
 * @param postNominals post-nominal letters; may be null
 * @param email the work email address
 * @param sex the gender's name
 * @param gender the gender's name, or null to leave it out
 * @param genderId the gender's id
 * @param workPhone the work phone number; may be null
 * @param disability whether a disability is declared
 * @param emailPersonal the personal email address, or null to leave it out
 * @param retirementDate the planned retirement date; may be null
 * @param leavingOn the leaving date; may be null
 * @param appointments the appointments in this response
 * @param judiciaryRoles every judiciary role
 * @param authorisations the tickets in authorisationsWithDates, by jurisdiction
 * @param authorisationsCurrent the tickets held now, by jurisdiction
 * @param authorisationsWithDates the dated authorisations in this response
 */
@Schema(description = "A current judicial office holder.")
@JsonPropertyOrder({"id", "last_updated", "per_id", "personal_code", "title", "known_as", "surname", "fullname",
    "initials", "post_nominals", "email", "sex", "gender", "gender_id", "work_phone", "disability", "email_personal",
    "retirement_date", "leaving_on", "appointments", "judiciary_roles", "authorisations", "authorisations_current",
    "authorisations_with_dates"})
public record CurrentOfficeHolderResponse(
        @Schema(description = "The office holder's id, in lower case.",
                format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7f")
        @JsonProperty("id")
        String id,

        @Schema(description = "When the record last changed; filtered by updated_since.",
                format = "date", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-06-14")
        @JsonProperty("last_updated")
        LocalDate lastUpdated,

        @Schema(description = "The office holder's personnel id.",
                format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "900017")
        @JsonProperty("per_id")
        long perId,

        @Schema(description = "The office holder's personal code: digits.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "9100113")
        @JsonProperty("personal_code")
        String personalCode,

        @Schema(description = "The office holder's title.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "District Judge")
        @JsonProperty("title")
        String title,

        @Schema(description = "The name the office holder is known by.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Alex")
        @JsonProperty("known_as")
        String knownAs,

        @Schema(description = "The office holder's surname.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Fenwick")
        @JsonProperty("surname")
        String surname,

        @Schema(description = "Title, known-as name and surname.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "District Judge Alex Fenwick")
        @JsonProperty("fullname")
        String fullname,

        @Schema(description = "The office holder's initials.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "AF")
        @JsonProperty("initials")
        String initials,

        @Schema(description = "Post-nominal letters. Null when there are none.",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "KC")
        @JsonProperty("post_nominals")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String postNominals,

        @Schema(description = "Work email address, in the reserved .example domain.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "alex.fenwick@judiciary.example")
        @JsonProperty("email")
        String email,

        @Schema(description = "The name of the gender_id record.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Female")
        @JsonProperty("sex")
        String sex,

        @Schema(description = "The name of the gender_id record. Left out for some office holders.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "Female")
        @JsonProperty("gender")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String gender,

        @Schema(description = "The id of the office holder's gender.",
                format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @JsonProperty("gender_id")
        long genderId,

        @Schema(description = "Work phone number, in the range reserved for drama. Null when there is none.",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "020 7946 0017")
        @JsonProperty("work_phone")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String workPhone,

        @Schema(description = "Whether the office holder has declared a disability.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
        @JsonProperty("disability")
        boolean disability,

        @Schema(description = "Personal email address, in the reserved .example domain. Left out when unset.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "alex.fenwick@personal.example")
        @JsonProperty("email_personal")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String emailPersonal,

        @Schema(description = "Planned retirement date. Null when unset.",
                format = "date", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "2034-09-30")
        @JsonProperty("retirement_date")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        LocalDate retirementDate,

        @Schema(description = "Leaving date. Null for a current office holder.",
                format = "date", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        @JsonProperty("leaving_on")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        LocalDate leavingOn,

        @Schema(description = "Current appointments, and previous ones when include_previous_appointments is true; by "
                              + "start_date, then appointment_id.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("appointments")
        List<AppointmentsResponse> appointments,

        @Schema(description = "Every judiciary role, by start_date, then judiciary_role_id.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("judiciary_roles")
        List<JudiciaryRoleResponse> judiciaryRoles,

        @Schema(description = "The tickets in authorisations_with_dates, by jurisdiction.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("authorisations")
        List<AuthorisationSummaryResponse> authorisations,

        @Schema(description = "The tickets held now, by jurisdiction, whatever include_previous_appointments says.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("authorisations_current")
        List<AuthorisationSummaryResponse> authorisationsCurrent,

        @Schema(description = "The dated authorisations under the appointments in this response, by start_date, then "
                              + "authorisation_id.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("authorisations_with_dates")
        List<AuthorisationsResponse> authorisationsWithDates) {
}
