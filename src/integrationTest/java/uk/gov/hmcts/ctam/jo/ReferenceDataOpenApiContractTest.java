package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.JsonTrees.parse;

/**
 * Asserts the generated {@code /v3/api-docs} against the contracts (Principle XVIII):
 * contracts/reference-data-api.md, and the healthcheck contract from specs/001-healthcheck-api.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class ReferenceDataOpenApiContractTest {

    private static final String COLLECTION_PATH = "/api/v1/reference_data/{attribute_name}";

    private static final String SINGLE_PATH = "/api/v1/reference_data/{attribute_name}/{reference_id}";

    @LocalServerPort
    private int port;

    private JsonNode root;

    @BeforeEach
    void fetchApiDocs() {
        String apiDocs = new TestRestTemplate().getForObject("http://localhost:" + port + "/v3/api-docs", String.class);
        root = parse(apiDocs);
    }

    @Test
    void openApiDocumentsHealthcheckEndpointPerContract() {
        JsonNode operation = root.path("paths").path("/api/v1/healthcheck").path("get");

        assertThat(operation.path("summary").asString()).isEqualTo("Healthcheck");

        JsonNode okResponse = operation.path("responses").path("200");
        assertThat(okResponse.path("description").asString()).isEqualTo("Service is healthy");

        JsonNode properties = schema(schemaRef(okResponse).replace("#/components/schemas/", "")).path("properties");
        assertThat(properties.propertyNames()).containsExactly("status");
        assertThat(properties.path("status").path("type").asString()).isEqualTo("string");
    }

    @Test
    void collectionOperationIsIdentifiedAndTagged() {
        JsonNode operation = collection();

        assertThat(operation.path("operationId").asString()).isEqualTo("getReferenceData");
        assertThat(operation.path("summary").asString()).isEqualTo("Get reference data");
        assertThat(texts(operation.path("tags"))).containsExactly("Reference Data");
    }

    @Test
    void attributeNameEnumComesFromTheRegistry() {
        JsonNode parameter = parameter(collection(), "attribute_name");

        assertThat(parameter.path("in").asString()).isEqualTo("path");
        assertThat(parameter.path("required").asBoolean()).isTrue();
        assertThat(texts(parameter.path("schema").path("enum")))
                .containsExactly("appointment_titles", "appointment_title");
        assertThat(parameter.path("description").asString())
                .isEqualTo("Can be one of: appointment_titles. Also supports deprecated values: appointment_title");
    }

    @Test
    void collectionDocumentsExactlyTheContractResponses() {
        JsonNode responses = collection().path("responses");

        assertThat(responses.propertyNames()).containsExactlyInAnyOrder("200", "400", "401", "406", "500");
        assertThat(schemaRef(responses.path("200"))).isEqualTo("#/components/schemas/ReferenceDataApiResponse");
        for (String code : List.of("400", "401", "406", "500")) {
            assertThat(schemaRef(responses.path(code))).as(code).isEqualTo("#/components/schemas/ErrorResponse");
        }
    }

    @Test
    void collectionRequiresBearerAuth() {
        assertThat(collection().path("security").get(0).has("bearerAuth")).isTrue();
        JsonNode scheme = root.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(scheme.path("type").asString()).isEqualTo("http");
        assertThat(scheme.path("scheme").asString()).isEqualTo("bearer");
    }

    @Test
    void referenceDataResponseMatchesTheContractSchema() {
        JsonNode properties = schema("ReferenceDataResponse").path("properties");

        assertThat(properties.propertyNames())
                .containsExactly("id", "name", "created_at", "updated_at", "start_date", "end_date");
        assertThat(properties.path("id").path("format").asString()).isEqualTo("int64");
        assertThat(properties.path("created_at").path("format").asString()).isEqualTo("date-time");
        assertThat(properties.path("updated_at").path("format").asString()).isEqualTo("date-time");
        assertThat(properties.path("start_date").path("format").asString()).isEqualTo("date");
        assertThat(properties.path("end_date").path("format").asString()).isEqualTo("date");
        assertThat(isNullable(properties.path("end_date"))).as("end_date nullable").isTrue();
        assertThat(isNullable(properties.path("name"))).as("name not nullable").isFalse();
    }

    @Test
    void onlyTheTwoVersionedReferenceDataPathsExist() {
        List<String> referenceDataPaths = new ArrayList<>();
        root.path("paths").propertyNames().forEach(path -> {
            if (path.contains("reference_data")) {
                referenceDataPaths.add(path);
            }
        });

        assertThat(referenceDataPaths).allMatch(path -> path.startsWith("/api/v1/reference_data/"));
        assertThat(referenceDataPaths).containsExactlyInAnyOrder(COLLECTION_PATH, SINGLE_PATH);
    }

    @Test
    void singleRecordOperationMatchesTheContract() {
        JsonNode operation = single();

        assertThat(operation.path("operationId").asString()).isEqualTo("getReferenceDataById");
        assertThat(operation.path("summary").asString()).isEqualTo("Get reference data by id");
        assertThat(texts(operation.path("tags"))).containsExactly("Reference Data");
        assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();

        JsonNode referenceId = parameter(operation, "reference_id");
        assertThat(referenceId.path("in").asString()).isEqualTo("path");
        assertThat(referenceId.path("required").asBoolean()).isTrue();
        assertThat(referenceId.path("schema").path("type").asString()).isEqualTo("string");
        assertThat(referenceId.path("schema").path("pattern").asString()).isEqualTo("^[0-9]+$");
        assertThat(texts(parameter(operation, "attribute_name").path("schema").path("enum")))
                .containsExactly("appointment_titles", "appointment_title");

        JsonNode responses = operation.path("responses");
        assertThat(responses.propertyNames()).containsExactlyInAnyOrder("200", "400", "401", "404", "406", "500");
        assertThat(schemaRef(responses.path("200"))).isEqualTo("#/components/schemas/ReferenceDataResponse");
        for (String code : List.of("400", "401", "404", "406", "500")) {
            assertThat(schemaRef(responses.path(code))).as(code).isEqualTo("#/components/schemas/ErrorResponse");
        }
    }

    @Test
    void singleRecordDocumentsCorrelationAndAuthenticationHeaders() {
        assertHeadersDocumented(single());
    }

    @Test
    void correlationAndAuthenticationHeadersAreDocumented() {
        assertHeadersDocumented(collection());
    }

    private static void assertHeadersDocumented(JsonNode operation) {
        JsonNode correlation = parameter(operation, "X-Correlation-Id");

        assertThat(correlation.path("in").asString()).isEqualTo("header");
        assertThat(correlation.path("required").asBoolean(false)).isFalse();
        assertThat(correlation.path("schema").path("pattern").asString()).isEqualTo("^[A-Za-z0-9._-]{1,64}$");

        operation.path("responses").properties().forEach(entry -> {
            assertThat(entry.getValue().path("headers").has("X-Correlation-Id")).as(entry.getKey()).isTrue();
            assertThat(entry.getValue().path("headers").has("WWW-Authenticate"))
                    .as(entry.getKey()).isEqualTo("401".equals(entry.getKey()));
        });
    }

    private JsonNode single() {
        return root.path("paths").path(SINGLE_PATH).path("get");
    }

    private JsonNode collection() {
        return root.path("paths").path(COLLECTION_PATH).path("get");
    }

    private JsonNode schema(String name) {
        return root.path("components").path("schemas").path(name);
    }

    private static JsonNode parameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asString())) {
                return parameter;
            }
        }
        throw new AssertionError("No parameter " + name + " in " + operation);
    }

    private static String schemaRef(JsonNode response) {
        return response.path("content").path("application/json").path("schema").path("$ref").asString();
    }

    /**
     * OpenAPI 3.0 uses {@code nullable: true}; 3.1 lists {@code "null"} among the types.
     */
    private static boolean isNullable(JsonNode property) {
        return property.path("nullable").asBoolean(false) || texts(property.path("type")).contains("null");
    }

    private static List<String> texts(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asString()));
        return values;
    }
}
