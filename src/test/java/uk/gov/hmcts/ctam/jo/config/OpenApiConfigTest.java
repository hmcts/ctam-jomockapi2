package uk.gov.hmcts.ctam.jo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIds;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.emptyRegistry;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.registry;

/**
 * The reference-data customiser on hand-built documents, including the shapes springdoc does not produce for
 * the current controllers (no paths, no parameters, no responses). The generated document as a whole is
 * checked by the OpenAPI contract tests in the integration suite.
 */
class OpenApiConfigTest {

    private static final String ATTRIBUTE_NAME = "attribute_name";

    private static final String REFERENCE_DATA_PATH = ApiPaths.REFERENCE_DATA + "/{attribute_name}";

    private static final TypeProperties WIDGETS =
            new TypeProperties("widgets", List.of("widget"), "classpath:reference-data/widgets.json");

    private static final TypeProperties GADGETS =
            new TypeProperties("gadgets", List.of(), "classpath:reference-data/gadgets.json");

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void documentWithoutPathsIsLeftAlone() {
        OpenAPI openApi = new OpenAPI();

        customise(openApi, registry(WIDGETS));

        assertThat(openApi.getPaths()).isNull();
    }

    @Test
    void pathsOutsideReferenceDataAreLeftAlone() {
        Operation operation = new Operation();
        OpenAPI openApi = document(ApiPaths.HEALTHCHECK, operation);

        customise(openApi, registry(WIDGETS));

        assertThat(operation.getParameters()).isNull();
        assertThat(operation.getResponses()).isNull();
    }

    @Test
    void bareOperationGetsCorrelationHeaderAndSharedErrorsInStatusOrder() {
        Operation operation = new Operation();

        customise(document(REFERENCE_DATA_PATH, operation), registry(WIDGETS));

        assertThat(operation.getParameters()).extracting(Parameter::getName).containsExactly(CorrelationIds.HEADER);
        assertThat(operation.getResponses().keySet()).containsExactly("401", "406", "500");
        operation.getResponses().forEach((code, response) ->
                assertThat(response.getHeaders()).containsKey(CorrelationIds.HEADER));
        assertThat(operation.getResponses().get("401").getHeaders()).containsKey("WWW-Authenticate");
        assertThat(operation.getResponses().get("500").getHeaders()).doesNotContainKey("WWW-Authenticate");
    }

    @Test
    void existingResponsesAreKeptAndSortedWithTheSharedOnes() {
        Operation operation = new Operation().responses(new ApiResponses()
                .addApiResponse("404", new ApiResponse().description("Not found."))
                .addApiResponse("200", new ApiResponse().description("OK.")));

        customise(document(REFERENCE_DATA_PATH, operation), registry(WIDGETS));

        assertThat(operation.getResponses().keySet()).containsExactly("200", "401", "404", "406", "500");
        assertThat(operation.getResponses().get("200").getDescription()).isEqualTo("OK.");
    }

    @Test
    void existingCorrelationHeaderIsNotAddedTwice() {
        Operation operation = new Operation().addParametersItem(new HeaderParameter().name(CorrelationIds.HEADER));

        customise(document(REFERENCE_DATA_PATH, operation), registry(WIDGETS));

        assertThat(operation.getParameters()).extracting(Parameter::getName).containsExactly(CorrelationIds.HEADER);
    }

    @Test
    void attributeNameListsCanonicalNamesAndDeprecatedAliases() {
        Parameter attributeName = new PathParameter().name(ATTRIBUTE_NAME);

        customise(document(REFERENCE_DATA_PATH, new Operation().addParametersItem(attributeName)),
                  registry(WIDGETS, GADGETS));

        assertThat(enumOf(attributeName)).isEqualTo(List.of("widgets", "gadgets", "widget"));
        assertThat(attributeName.getDescription())
                .isEqualTo("Can be one of: widgets, gadgets. Also supports deprecated values: widget");
    }

    @Test
    void attributeNameWithoutAliasesHasNoDeprecatedSentence() {
        Parameter attributeName = new PathParameter().name(ATTRIBUTE_NAME);

        customise(document(REFERENCE_DATA_PATH, new Operation().addParametersItem(attributeName)),
                  registry(GADGETS));

        assertThat(attributeName.getDescription()).isEqualTo("Can be one of: gadgets");
    }

    @Test
    void attributeNameWithNoTypesConfiguredHasNoEnum() {
        Parameter attributeName = new PathParameter().name(ATTRIBUTE_NAME);

        customise(document(REFERENCE_DATA_PATH, new Operation().addParametersItem(attributeName)), emptyRegistry());

        assertThat(enumOf(attributeName)).isNull();
        assertThat(attributeName.getDescription()).isEqualTo("Can be one of: ");
    }

    private void customise(OpenAPI openApi, ReferenceDataTypeRegistry registry) {
        config.referenceDataOpenApiCustomizer(registry).customise(openApi);
    }

    // Parameter.getSchema() is a raw Schema; reading the enum as List<?> avoids an unchecked warning (-Werror).
    private static List<?> enumOf(Parameter parameter) {
        return parameter.getSchema().getEnum();
    }

    private static OpenAPI document(String path, Operation operation) {
        return new OpenAPI().paths(new Paths().addPathItem(path, new PathItem().get(operation)));
    }
}
