package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * One appointment an office holder holds or held (013 spec § Records). Every name is read from the served reference
 * data (013 data-model.md § 3).
 *
 * @param appointmentId the appointment's id
 * @param roleName the role held
 * @param roleNameId the id of the role held
 * @param type the name of the base location's jurisdiction
 * @param courtName the base location's name
 * @param courtType the name of the base location's type
 * @param circuit the circuit, or null to leave it out
 * @param bench the bench, or null to leave it out
 * @param advisoryCommitteeArea the advisory committee area, or null to leave it out
 * @param location the name of the base location's parent
 * @param baseLocation the base location's name
 * @param baseLocationId the base location's id
 * @param isPrincipal whether this is the principal appointment
 * @param startDate when it started
 * @param endDate when it ends or ended; null while open
 * @param contractType the contract type's name
 * @param contractTypeId the contract type's id
 */
@Schema(description = "An appointment the office holder holds or held.")
@JsonPropertyOrder({"appointment_id", "role_name", "role_name_id", "type", "court_name", "court_type", "circuit",
    "bench", "advisory_committee_area", "location", "base_location", "base_location_id", "is_principal", "start_date",
    "end_date", "contract_type", "contract_type_id"})
public record AppointmentsResponse(
        @Schema(description = "The appointment's id.",
                format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "700031")
        @JsonProperty("appointment_id")
        long appointmentId,

        @Schema(description = "The name of the role_name_id record.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "District Judge")
        @JsonProperty("role_name")
        String roleName,

        @Schema(description = "The id of the role held.",
                format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @JsonProperty("role_name_id")
        long roleNameId,

        @Schema(description = "The name of the base location's jurisdiction.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Courts")
        @JsonProperty("type")
        String type,

        @Schema(description = "The base location's name.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Leeds Combined Court Centre")
        @JsonProperty("court_name")
        String courtName,

        @Schema(description = "The name of the base location's type.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "County Court")
        @JsonProperty("court_type")
        String courtType,

        @Schema(description = "The circuit. Left out when unset.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "North Eastern")
        @JsonProperty("circuit")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String circuit,

        @Schema(description = "The bench. Left out when unset.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "Central Bench")
        @JsonProperty("bench")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String bench,

        @Schema(description = "The advisory committee area. Left out when unset.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "West Yorkshire")
        @JsonProperty("advisory_committee_area")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String advisoryCommitteeArea,

        @Schema(description = "The name of the base location's parent.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Yorkshire")
        @JsonProperty("location")
        String location,

        @Schema(description = "The base location's name.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Leeds Combined Court Centre")
        @JsonProperty("base_location")
        String baseLocation,

        @Schema(description = "The base location's id.",
                format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "1032")
        @JsonProperty("base_location_id")
        long baseLocationId,

        @Schema(description = "Whether this is the office holder's principal appointment.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        @JsonProperty("is_principal")
        boolean isPrincipal,

        @Schema(description = "When the appointment started.",
                format = "date", requiredMode = Schema.RequiredMode.REQUIRED, example = "2019-04-01")
        @JsonProperty("start_date")
        LocalDate startDate,

        @Schema(description = "When the appointment ends or ended. Null while it is open.",
                format = "date", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "2028-03-31")
        @JsonProperty("end_date")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        LocalDate endDate,

        @Schema(description = "The name of the contract_type_id record.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Salaried")
        @JsonProperty("contract_type")
        String contractType,

        @Schema(description = "The id of the contract type.",
                format = "int64", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
        @JsonProperty("contract_type_id")
        long contractTypeId) {
}
