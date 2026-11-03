package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The People search response: one page of office holders and the pagination block (013 FR-017).
 *
 * @param results    the office holders on this page, by last_updated, then id
 * @param pagination where this page sits in the whole result set
 */
@Schema(description = "One page of the office holders updated on or after updated_since.")
@JsonPropertyOrder({"results", "pagination"})
public record PeopleApiResponse(
        @Schema(description = "The office holders on this page, by ascending last_updated, then id.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("results")
        List<CurrentOfficeHolderResponse> results,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("pagination")
        PaginationResponse pagination) {
}
