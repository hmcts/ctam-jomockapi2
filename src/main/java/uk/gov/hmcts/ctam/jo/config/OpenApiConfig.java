package uk.gov.hmcts.ctam.jo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorMessages;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * OpenAPI document settings (Principle XVIII, research R10). Everything that depends on the configured
 * reference-data types, or applies to every reference-data operation, is added here in one place rather
 * than per controller method.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    private static final String ATTRIBUTE_NAME = "attribute_name";

    private static final String ERROR_RESPONSE_SCHEMA = "#/components/schemas/ErrorResponse";

    private static final Parameter CORRELATION_ID_REQUEST_HEADER = new HeaderParameter()
            .name(CorrelationIds.HEADER)
            .required(false)
            .description("Optional correlation ID. Used if it matches the pattern; otherwise a UUID is generated. "
                         + "Echoed in the `X-Correlation-Id` response header and in `traceId` on errors.")
            .schema(new StringSchema().pattern(CorrelationIds.VALID_PATTERN));

    private static final Header CORRELATION_ID_RESPONSE_HEADER = new Header()
            .description("The correlation ID in use: the caller's, if valid, or a generated UUID.")
            .schema(new StringSchema());

    private static final Header WWW_AUTHENTICATE_HEADER = new Header()
            .description("Authentication scheme the caller must use.")
            .schema(new StringSchema().example("Bearer"));

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("JO Mock API").version("v1")
                              .description("Mock of the E-Links API, serving synthetic data only."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")));
    }

    @Bean
    public OpenApiCustomizer referenceDataOpenApiCustomizer(ReferenceDataTypeRegistry registry) {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> {
                if (path.startsWith(ApiPaths.REFERENCE_DATA + "/")) {
                    item.readOperations().forEach(operation -> document(operation, registry));
                }
            });
        };
    }

    private static void document(Operation operation, ReferenceDataTypeRegistry registry) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            parameters.stream()
                    .filter(parameter -> ATTRIBUTE_NAME.equals(parameter.getName()))
                    .forEach(parameter -> describeAttributeName(parameter, registry));
        }
        boolean hasCorrelationHeader = parameters != null && parameters.stream()
                .anyMatch(parameter -> CorrelationIds.HEADER.equals(parameter.getName()));
        if (!hasCorrelationHeader) {
            operation.addParametersItem(CORRELATION_ID_REQUEST_HEADER);
        }
        ApiResponses responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
        addSharedErrorResponses(responses);
        responses.forEach(OpenApiConfig::addResponseHeaders);
        operation.setResponses(inStatusCodeOrder(responses));
    }

    /**
     * The errors every reference-data operation can return, whatever its route: 401 from the authentication
     * filter, 406 from content negotiation and 500 from the catch-all handler. Operation-specific responses
     * (200, 400, 404) stay on the controller methods.
     */
    private static void addSharedErrorResponses(ApiResponses responses) {
        responses.addApiResponse("401", errorResponse("Missing or invalid Bearer token.", ErrorMessages.UNAUTHORIZED));
        responses.addApiResponse("406", errorResponse("The Accept header excludes application/json.",
                                                      ErrorMessages.NOT_ACCEPTABLE));
        responses.addApiResponse("500", errorResponse("Unexpected failure.", ErrorMessages.INTERNAL_SERVER_ERROR));
    }

    private static ApiResponse errorResponse(String description, String message) {
        Map<String, String> example = new LinkedHashMap<>();
        example.put("error", message);
        example.put("timestamp", "2026-09-23T10:15:30Z");
        example.put("traceId", "3f1c2a9e-8b7d-4e21-9a55-0c6d1e2f3a4b");
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                        .schema(new Schema<Object>().$ref(ERROR_RESPONSE_SCHEMA))
                        .example(example)));
    }

    private static ApiResponses inStatusCodeOrder(ApiResponses responses) {
        ApiResponses ordered = new ApiResponses();
        responses.keySet().stream().sorted().forEach(code -> ordered.addApiResponse(code, responses.get(code)));
        return ordered;
    }

    private static void describeAttributeName(Parameter parameter, ReferenceDataTypeRegistry registry) {
        List<String> canonical = registry.canonicalAttributeNames();
        List<String> aliases = registry.deprecatedAttributeNames();

        StringSchema schema = new StringSchema();
        if (!registry.supportedAttributeNames().isEmpty()) {
            schema.setEnum(registry.supportedAttributeNames());
        }
        parameter.setSchema(schema);

        String description = "Can be one of: " + String.join(", ", canonical);
        if (!aliases.isEmpty()) {
            description += ". Also supports deprecated values: " + String.join(", ", aliases);
        }
        parameter.setDescription(description);
    }

    private static void addResponseHeaders(String code, ApiResponse response) {
        response.addHeaderObject(CorrelationIds.HEADER, CORRELATION_ID_RESPONSE_HEADER);
        if ("401".equals(code)) {
            response.addHeaderObject(HttpHeaders.WWW_AUTHENTICATE, WWW_AUTHENTICATE_HEADER);
        }
    }
}
