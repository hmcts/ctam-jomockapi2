package uk.gov.hmcts.ctam.jo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

/**
 * One judiciary-role entry as stored in the People fixture (013 data-model.md § 2). The name is resolved from
 * judiciary_role_name_id when the fixture is loaded; judiciary_role_id identifies the entry itself.
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = false)
public class JudiciaryRoleRecord {

    @JsonProperty("judiciary_role_id")
    Long judiciaryRoleId;

    @JsonProperty("judiciary_role_name_id")
    Long judiciaryRoleNameId;

    @JsonProperty("start_date")
    LocalDate startDate;

    /**
     * Null while the role is held.
     */
    @JsonProperty("end_date")
    LocalDate endDate;
}
