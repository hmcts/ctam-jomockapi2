package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The tickets an office holder holds in one jurisdiction (013 FR-044).
 *
 * @param jurisdiction the jurisdiction's name
 * @param tickets      the distinct ticket names, in ascending order
 */
@Schema(description = "The tickets held in one jurisdiction.")
@JsonPropertyOrder({"jurisdiction", "tickets"})
public record AuthorisationSummaryResponse(
        @Schema(description = "The jurisdiction's name.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Courts")
        @JsonProperty("jurisdiction")
        String jurisdiction,

        @ArraySchema(arraySchema = @Schema(description = "The distinct ticket names, in ascending order.",
                                           requiredMode = Schema.RequiredMode.REQUIRED),
                     schema = @Schema(example = "Civil Authorisation"))
        @JsonProperty("tickets")
        List<String> tickets) {
}
