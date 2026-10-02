package uk.gov.hmcts.ctam.jo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ctam.jo.config.ApiPaths;
import uk.gov.hmcts.ctam.jo.config.OpenApiConfig;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataApiResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponse;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidReferenceIdException;
import uk.gov.hmcts.ctam.jo.exceptions.ReferenceDataNotFoundException;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataService;
import uk.gov.hmcts.ctam.jo.services.ReferenceIdParser;

/**
 * Reference-data routes. Binds the request and shapes the response only; every configured type is served
 * by the same code. Only responses specific to one operation are documented here. OpenApiConfig adds the
 * {@code attribute_name} values from the registry, the correlation headers and the 401/406/500 responses
 * that every reference-data operation shares.
 */
@RestController
@RequestMapping(ApiPaths.REFERENCE_DATA)
@Tag(name = "Reference Data", description = "Synthetic reference data, served for every configured type.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
public class ReferenceDataController {

    private static final String ERROR_EXAMPLE_START = "{\"error\":\"";

    private static final String ERROR_EXAMPLE_END =
            "\",\"timestamp\":\"2026-09-23T10:15:30Z\",\"traceId\":\"3f1c2a9e-8b7d-4e21-9a55-0c6d1e2f3a4b\"}";

    private final ReferenceDataService service;

    @Operation(operationId = "getReferenceData", summary = "Get reference data",
               description = "Returns every record of the requested reference-data type, in ascending id order.")
    @ApiResponse(responseCode = "200", description = "Every record of the type.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ReferenceDataApiResponse.class)))
    @ApiResponse(responseCode = "400", description = "Unsupported attribute_name, or a query parameter was sent.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = ERROR_EXAMPLE_START
                                            + UnsupportedReferenceDataTypeException.MESSAGE + ERROR_EXAMPLE_END)))
    @GetMapping(value = "/{attribute_name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReferenceDataApiResponse> getReferenceData(
            @PathVariable("attribute_name") String attributeName) {
        return ResponseEntity.ok(service.getAll(attributeName));
    }

    @Operation(operationId = "getReferenceDataById", summary = "Get reference data by id",
               description = "Returns one record of the requested reference-data type.")
    @ApiResponse(responseCode = "200", description = "The record, as a single object.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ReferenceDataResponse.class)))
    @ApiResponse(responseCode = "400",
                 description = "Unsupported attribute_name, malformed reference_id, or a query parameter was sent.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = ERROR_EXAMPLE_START
                                            + InvalidReferenceIdException.MESSAGE + ERROR_EXAMPLE_END)))
    @ApiResponse(responseCode = "404", description = "No record has this reference_id.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = ERROR_EXAMPLE_START
                                            + ReferenceDataNotFoundException.MESSAGE + ERROR_EXAMPLE_END)))
    @GetMapping(value = "/{attribute_name}/{reference_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReferenceDataResponse> getReferenceDataById(
            @PathVariable("attribute_name") String attributeName,
            @Parameter(description = "Record id: decimal digits only. Leading zeros are allowed (007 means 7).",
                       example = "70", schema = @Schema(type = "string", pattern = ReferenceIdParser.PATTERN))
            @PathVariable("reference_id") String referenceId) {
        return ResponseEntity.ok(service.getById(attributeName, referenceId));
    }
}
