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
     * Null unless the type declares {@link ReferenceField#TYPE_ID}. Whether a fixture may leave a reference field out
     * or set it to {@code null} depends on the type's declarations, so the repository decides, not this class.
     */
    @JsonProperty("type_id")
    Long typeId;

    /**
     * Null unless the type declares {@link ReferenceField#PARENT_ID}, or declares it nullable and the record has no
     * value. The repository decides whether a null or a missing key is allowed.
     */
    @JsonProperty("parent_id")
    Long parentId;

    /**
     * Null unless the type declares {@link ReferenceField#JURISDICTION_ID}. The repository decides whether a null or
     * a missing key is allowed.
     */
    @JsonProperty("jurisdiction_id")
    Long jurisdictionId;

    /**
     * Null unless the type declares {@link ReferenceField#TICKET_CATEGORY_ID}. The repository decides whether a null
     * or a missing key is allowed.
     */
    @JsonProperty("ticket_category_id")
    Long ticketCategoryId;

    /**
     * Null unless the type declares {@link ReferenceField#PARENT_CATEGORY_ID}, or declares it nullable and the record
     * has no value. The repository decides whether a null or a missing key is allowed.
     */
    @JsonProperty("parent_category_id")
    Long parentCategoryId;

    /**
     * Null unless the type declares {@link BooleanField#SALARIED}. An explicit {@code null} fails, as for every
     * boolean field.
     */
    @JsonProperty("salaried")
    @JsonSetter(nulls = Nulls.FAIL)
    Boolean salaried;

    /**
     * Null unless the type declares {@link TextField#INTEGRATION_CODE}. An explicit {@code null} fails, as for every
     * text field.
     */
    @JsonProperty("integration_code")
    @JsonSetter(nulls = Nulls.FAIL)
    String integrationCode;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;

    @JsonProperty("start_date")
    LocalDate startDate;

    @JsonProperty("end_date")
    LocalDate endDate;
}
