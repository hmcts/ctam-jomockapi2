package uk.gov.hmcts.ctam.jo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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

    /**
     * Boxed so that a fixture record without an id is reported as such, rather than read as 0.
     */
    @JsonProperty("id")
    Long id;

    @JsonProperty("name")
    String name;

    /**
     * Null unless the type declares {@link ReferenceField#TYPE_ID}. A fixture leaves an undeclared reference field
     * out rather than setting it to {@code null}, so an explicit {@code null} fails, as for every reference field.
     */
    @JsonProperty("type_id")
    @JsonSetter(nulls = Nulls.FAIL)
    Long typeId;

    /**
     * Null unless the type declares {@link ReferenceField#PARENT_ID}.
     */
    @JsonProperty("parent_id")
    @JsonSetter(nulls = Nulls.FAIL)
    Long parentId;

    /**
     * Null unless the type declares {@link ReferenceField#JURISDICTION_ID}.
     */
    @JsonProperty("jurisdiction_id")
    @JsonSetter(nulls = Nulls.FAIL)
    Long jurisdictionId;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;

    @JsonProperty("start_date")
    LocalDate startDate;

    @JsonProperty("end_date")
    LocalDate endDate;
}
