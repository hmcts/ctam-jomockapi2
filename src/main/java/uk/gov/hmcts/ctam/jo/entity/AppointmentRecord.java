package uk.gov.hmcts.ctam.jo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

/**
 * One appointment as stored in the People fixture (013 data-model.md § 2): reference ids only. The role name,
 * contract type and everything read from the base location are resolved when the fixture is loaded.
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = false)
public class AppointmentRecord {

    @JsonProperty("appointment_id")
    Long appointmentId;

    @JsonProperty("role_name_id")
    Long roleNameId;

    @JsonProperty("base_location_id")
    Long baseLocationId;

    @JsonProperty("contract_type_id")
    Long contractTypeId;

    @JsonProperty("is_principal")
    Boolean isPrincipal;

    @JsonProperty("start_date")
    LocalDate startDate;

    /**
     * Null while the appointment is open.
     */
    @JsonProperty("end_date")
    LocalDate endDate;

    /**
     * Null when unset; the response then leaves it out, as for bench and advisory_committee_area.
     */
    @JsonProperty("circuit")
    String circuit;

    @JsonProperty("bench")
    String bench;

    @JsonProperty("advisory_committee_area")
    String advisoryCommitteeArea;
}
