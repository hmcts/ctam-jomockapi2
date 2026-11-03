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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ctam.jo.config.ApiPaths;
import uk.gov.hmcts.ctam.jo.config.OpenApiConfig;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.domain.PeopleApiResponse;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponse;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidPersonIdException;
import uk.gov.hmcts.ctam.jo.exceptions.PersonNotFoundException;
import uk.gov.hmcts.ctam.jo.services.PeopleService;
import uk.gov.hmcts.ctam.jo.services.PersonIdParser;

import static uk.gov.hmcts.ctam.jo.config.OpenApiConfig.ERROR_EXAMPLE_END;
import static uk.gov.hmcts.ctam.jo.config.OpenApiConfig.ERROR_EXAMPLE_START;

/**
 * The People routes (013 FR-001, FR-002). Every query parameter is bound as an optional string and handed to the
 * service unchanged, so the service checks them in the contract's order with its fixed messages (research R5). Only
 * responses specific to one operation are documented here; OpenApiConfig adds the correlation headers and the shared
 * 401, 406 and 500 responses.
 */
@RestController
@RequestMapping(ApiPaths.PEOPLE)
@Tag(name = "People", description = "Synthetic judicial office holders.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
public class PeopleController {

    private static final String FLAG_DESCRIPTION = "true adds previous appointments, and the dated authorisations "
            + "under them, to appointments, authorisations_with_dates and authorisations. An appointment is previous "
            + "when it ended before the configured reference date. Exactly true or false; defaults to false.";

    private final PeopleService service;

    @Operation(operationId = "searchPeople", summary = "Search people",
               description = "Returns one page of the office holders whose last_updated is on or after "
                             + "updated_since, by ascending last_updated, then id.")
    @ApiResponse(responseCode = "200", description = "The found people.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PeopleApiResponse.class)))
    @ApiResponse(responseCode = "400",
                 description = "A parameter is missing, malformed, undefined on this route or repeated.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = ERROR_EXAMPLE_START
                                            + "updated_since is required." + ERROR_EXAMPLE_END)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PeopleApiResponse> searchPeople(
            @Parameter(description = "Returns office holders updated on or after this date: YYYY-MM-DD only.",
                       required = true, example = "2025-06-01", schema = @Schema(type = "string", format = "date"))
            @RequestParam(name = PeopleService.UPDATED_SINCE, required = false) String updatedSince,
            @Parameter(description = "The page to return, from 1. Defaults to 1.",
                       schema = @Schema(type = "integer", format = "int64", minimum = "1", defaultValue = "1"))
            @RequestParam(name = PeopleService.PAGE, required = false) String page,
            @Parameter(description = "The page size, from 1. Defaults to 50.",
                       schema = @Schema(type = "integer", format = "int64", minimum = "1", defaultValue = "50"))
            @RequestParam(name = PeopleService.PER_PAGE, required = false) String perPage,
            @Parameter(description = FLAG_DESCRIPTION, schema = @Schema(type = "boolean", defaultValue = "false"))
            @RequestParam(name = PeopleService.INCLUDE_PREVIOUS_APPOINTMENTS, required = false)
            String includePreviousAppointments) {
        return ResponseEntity.ok(service.search(updatedSince, page, perPage, includePreviousAppointments));
    }

    @Operation(operationId = "getPerson", summary = "Get one person",
               description = "Returns one office holder, the same object a search returns for them.")
    @ApiResponse(responseCode = "200", description = "The found person.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CurrentOfficeHolderResponse.class)))
    @ApiResponse(responseCode = "400",
                 description = "Malformed id, or a parameter is malformed, undefined on this route or repeated.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = ERROR_EXAMPLE_START
                                            + InvalidPersonIdException.MESSAGE + ERROR_EXAMPLE_END)))
    @ApiResponse(responseCode = "404", description = "No office holder has this id.",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = ERROR_EXAMPLE_START
                                            + PersonNotFoundException.MESSAGE + ERROR_EXAMPLE_END)))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrentOfficeHolderResponse> getPerson(
            @Parameter(description = "The office holder's id: a UUID, in either letter case.",
                       example = "0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7f",
                       schema = @Schema(type = "string", format = "uuid", pattern = PersonIdParser.PATTERN))
            @PathVariable("id") String id,
            @Parameter(description = FLAG_DESCRIPTION, schema = @Schema(type = "boolean", defaultValue = "false"))
            @RequestParam(name = PeopleService.INCLUDE_PREVIOUS_APPOINTMENTS, required = false)
            String includePreviousAppointments) {
        return ResponseEntity.ok(service.getById(id, includePreviousAppointments));
    }
}
