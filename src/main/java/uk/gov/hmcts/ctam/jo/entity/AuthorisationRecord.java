package uk.gov.hmcts.ctam.jo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

/**
 * One dated authorisation as stored in the People fixture (013 data-model.md § 2), held under one of the office
 * holder's appointments. The ticket name and jurisdiction are resolved when the fixture is loaded.
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = false)
public class AuthorisationRecord {

    @JsonProperty("authorisation_id")
    Long authorisationId;

    @JsonProperty("appointment_id")
    Long appointmentId;

    @JsonProperty("ticket_id")
    Long ticketId;

    @JsonProperty("start_date")
    LocalDate startDate;

    /**
     * Null while the authorisation is held.
     */
    @JsonProperty("end_date")
    LocalDate endDate;
}
