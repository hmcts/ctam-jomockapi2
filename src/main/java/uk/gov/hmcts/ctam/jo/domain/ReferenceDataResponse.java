package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One reference-data record as returned by the API: the E-Links {@code ReferenceDataResponse} plus
 * {@code name} (deviation D-1) and, for types that declare them, the reference fields {@code type_id},
 * {@code parent_id} and {@code jurisdiction_id} (deviation D-2). A reference field is left out of the JSON when
 * the record doesn't carry it.
 */
@Schema(description = "One reference-data record.")
@JsonPropertyOrder({"id", "name", "type_id", "parent_id", "jurisdiction_id", "created_at", "updated_at",
    "start_date", "end_date"})
public record ReferenceDataResponse(
        @Schema(description = "Stable reference-data identifier.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @JsonProperty("id")
        long id,

        @Schema(description = "Human-readable title (deviation D-1 from the E-Links schema).",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Acting Senior Coroner")
        @JsonProperty("name")
        String name,

        @Schema(description = "Id of the record's type, in E-Links' numbering. Present only for types whose "
                              + "records carry it (deviation D-2 from the E-Links schema).",
                format = "int64", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "8")
        @JsonProperty("type_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long typeId,

        @Schema(description = "Id of the record's parent record, in E-Links' numbering. Present only for types whose "
                              + "records carry it (deviation D-2 from the E-Links schema).",
                format = "int64", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1722")
        @JsonProperty("parent_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long parentId,

        @Schema(description = "Id of the record's jurisdiction, in E-Links' numbering. Present only for types whose "
                              + "records carry it (deviation D-2 from the E-Links schema).",
                format = "int64", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "84")
        @JsonProperty("jurisdiction_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long jurisdictionId,

        @Schema(description = "When the record was created (UTC).", format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-15T09:00:00Z")
        @JsonProperty("created_at")
        Instant createdAt,

        @Schema(description = "When the record was last updated (UTC).", format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-06-03T10:30:00Z")
        @JsonProperty("updated_at")
        Instant updatedAt,

        @Schema(description = "Date from which the record is valid.", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
        @JsonProperty("start_date")
        LocalDate startDate,

        @Schema(description = "Date the record stopped being valid. Null if it is still current.", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "2025-03-31")
        @JsonProperty("end_date")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        LocalDate endDate) {
}
