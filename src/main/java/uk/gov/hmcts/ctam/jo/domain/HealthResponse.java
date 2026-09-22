package uk.gov.hmcts.ctam.jo.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record HealthResponse(
        @Schema(description = "Literal health indicator; always \"ok\" when the application can serve HTTP requests.",
                example = "ok")
        String status) {
}
