package uk.gov.hmcts.ctam.jo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One reference-data record as stored in a fixture file. Kept separate from the API DTOs so the fixture
 * format is not tied to the contract (research R9).
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = false)
public class ReferenceDataRecord {

    @JsonProperty("id")
    long id;

    @JsonProperty("name")
    String name;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;

    @JsonProperty("start_date")
    LocalDate startDate;

    @JsonProperty("end_date")
    LocalDate endDate;
}
