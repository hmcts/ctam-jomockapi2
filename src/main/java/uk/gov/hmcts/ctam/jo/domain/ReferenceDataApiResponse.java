package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The collection response: every record of the requested type, in ascending id order.
 */
@Schema(description = "Every record of the requested reference-data type.")
public record ReferenceDataApiResponse(
        @Schema(description = "Every record of the requested type, in ascending id order.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("results")
        List<ReferenceDataResponse> results) {
}
