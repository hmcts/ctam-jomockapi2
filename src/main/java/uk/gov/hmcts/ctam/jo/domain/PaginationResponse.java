package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The pagination block of a paginated response (FR-017). Built only by {@code Paginator}, and shared unchanged with
 * the later leavers and deleted features (DF-11), which wrap it in their own response records.
 *
 * @param results        the number of matching records across all pages
 * @param pages          the number of pages: the results divided by the page size, rounded up; 0 when nothing matches
 * @param currentPage    the requested page, even when it is past the last page
 * @param resultsPerPage the effective page size, requested or default
 * @param morePages      true exactly when the current page is before the last page
 */
@Schema(description = "Where this page sits in the whole result set.")
@JsonPropertyOrder({"results", "pages", "current_page", "results_per_page", "more_pages"})
public record PaginationResponse(
        @Schema(description = "The number of matching records across all pages.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
        @JsonProperty("results")
        long results,

        @Schema(description = "The number of pages: results divided by results_per_page, rounded up; 0 when nothing "
                              + "matches.", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "3")
        @JsonProperty("pages")
        long pages,

        @Schema(description = "The requested page, starting at 1.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @JsonProperty("current_page")
        long currentPage,

        @Schema(description = "The page size in effect: per_page, or 50 by default.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
        @JsonProperty("results_per_page")
        long resultsPerPage,

        @Schema(description = "True when a later page holds more results.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "true")
        @JsonProperty("more_pages")
        boolean morePages) {
}
