package uk.gov.hmcts.ctam.jo.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * The one shared error body returned for every error the application produces (Principle IX).
 */
@Schema(description = "Shared error response body.")
public record ErrorResponse(
        @Schema(description = "Human-readable error message.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Unauthorized. Invalid or missing token.")
        String error,

        @Schema(description = "When the error was produced (UTC).", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "2026-09-23T10:15:30Z")
        Instant timestamp,

        @Schema(description = "The request's correlation ID. Null only on the exempt healthcheck path.",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                example = "3f1c2a9e-8b7d-4e21-9a55-0c6d1e2f3a4b")
        String traceId) {
}
