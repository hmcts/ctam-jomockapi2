package uk.gov.hmcts.ctam.jo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ctam.jo.domain.HealthResponse;

@RestController
public class HealthcheckController {

    @Operation(summary = "Healthcheck")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping(value = "/api/v1/healthcheck", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthResponse> healthcheck() {
        return ResponseEntity.status(HttpStatus.OK).body(new HealthResponse("ok"));
    }
}
