package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * One judiciary role an office holder holds or held (013 spec § Records).
 *
 * @param name                the judiciary role's name
 * @param judiciaryRoleId     the entry's own id
 * @param judiciaryRoleNameId the id of the judiciary role it names
 * @param startDate           when it started
 * @param endDate             when it ended; null while held
 */
@Schema(description = "A judiciary role the office holder holds or held.")
@JsonPropertyOrder({"name", "judiciary_role_id", "judiciary_role_name_id", "start_date", "end_date"})
public record JudiciaryRoleResponse(
        @Schema(description = "The judiciary role's name.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Magistrate")
        @JsonProperty("name")
        String name,

        @Schema(description = "This entry's own id.", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "600012")
        @JsonProperty("judiciary_role_id")
        long judiciaryRoleId,

        @Schema(description = "The id of the judiciary role named.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @JsonProperty("judiciary_role_name_id")
        long judiciaryRoleNameId,

        @Schema(description = "When the role started.", format = "date", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "2020-01-06")
        @JsonProperty("start_date")
        LocalDate startDate,

        @Schema(description = "When the role ended. Null while it is held.", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "2024-03-31")
        @JsonProperty("end_date")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        LocalDate endDate) {
}
