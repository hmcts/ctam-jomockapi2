package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.PEOPLE;
import static uk.gov.hmcts.ctam.jo.testsupport.JsonTrees.parse;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * Asserts the generated {@code /v3/api-docs} against 013's contracts/people-api.md and the spec's Records section
 * (FR-053, AC-033), and checks actual responses against the document's own schemas with a small walker, so the
 * document can't drift from what the routes return.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class PeopleOpenApiContractTest {

    private static final String SINGLE = PEOPLE + "/{id}";

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    /**
     * Each schema's properties in order, and which are required and nullable (013 spec § Records).
     */
    private static final Map<String, List<String>> PROPERTIES = Map.of(
            "PeopleApiResponse", List.of("results", "pagination"),
            "PaginationResponse", List.of("results", "pages", "current_page", "results_per_page", "more_pages"),
            "CurrentOfficeHolderResponse", List.of("id", "last_updated", "per_id", "personal_code", "title",
                    "known_as", "surname", "fullname", "initials", "post_nominals", "email", "sex", "gender",
                    "gender_id", "work_phone", "disability", "email_personal", "retirement_date", "leaving_on",
                    "appointments", "judiciary_roles", "authorisations", "authorisations_current",
                    "authorisations_with_dates"),
            "AppointmentsResponse", List.of("appointment_id", "role_name", "role_name_id", "type", "court_name",
                    "court_type", "circuit", "bench", "advisory_committee_area", "location", "base_location",
                    "base_location_id", "is_principal", "start_date", "end_date", "contract_type", "contract_type_id"),
            "JudiciaryRoleResponse", List.of("name", "judiciary_role_id", "judiciary_role_name_id", "start_date",
                    "end_date"),
            "AuthorisationSummaryResponse", List.of("jurisdiction", "tickets"),
            "AuthorisationsResponse", List.of("authorisation_id", "appointment_id", "jurisdiction",
                    "jurisdiction_id", "ticket", "ticket_id", "start_date", "end_date"));

    private static final Map<String, Set<String>> OPTIONAL = Map.of(
            "CurrentOfficeHolderResponse", Set.of("gender", "email_personal"),
            "AppointmentsResponse", Set.of("circuit", "bench", "advisory_committee_area"));

    private static final Map<String, Set<String>> NULLABLE = Map.of(
            "CurrentOfficeHolderResponse", Set.of("post_nominals", "work_phone", "retirement_date", "leaving_on"),
            "AppointmentsResponse", Set.of("end_date"),
            "JudiciaryRoleResponse", Set.of("end_date"),
            "AuthorisationsResponse", Set.of("end_date"));

    @LocalServerPort
    private int port;

    private JsonNode root;

    @BeforeEach
    void fetchApiDocs() {
        root = parse(new TestRestTemplate().getForObject("http://localhost:" + port + "/v3/api-docs", String.class));
    }

    @Test
    void bothOperationsAreIdentifiedTaggedAndSecured() {
        assertThat(search().path("operationId").asString()).isEqualTo("searchPeople");
        assertThat(single().path("operationId").asString()).isEqualTo("getPerson");
        for (JsonNode operation : List.of(search(), single())) {
            assertThat(operation.path("tags").get(0).asString()).isEqualTo("People");
            assertThat(operation.path("tags")).hasSize(1);
            assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
        }
    }

    @Test
    void searchParametersFollowTheContract() {
        JsonNode updatedSince = parameter(search(), "updated_since");
        assertThat(updatedSince.path("in").asString()).isEqualTo("query");
        assertThat(updatedSince.path("required").asBoolean()).isTrue();
        assertThat(updatedSince.path("schema").path("type").asString()).isEqualTo("string");
        assertThat(updatedSince.path("schema").path("format").asString()).isEqualTo("date");

        for (Map.Entry<String, Integer> paging : Map.of("page", 1, "per_page", 50).entrySet()) {
            JsonNode parameter = parameter(search(), paging.getKey());
            assertThat(parameter.path("in").asString()).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).isFalse();
            JsonNode schema = parameter.path("schema");
            assertThat(schema.path("type").asString()).isEqualTo("integer");
            assertThat(schema.path("format").asString()).isEqualTo("int64");
            assertThat(schema.path("minimum").asInt()).isEqualTo(1);
            assertThat(schema.path("default").asInt()).isEqualTo(paging.getValue());
        }
        assertFlag(search());
        assertThat(queryParameterNames(search()))
                .containsExactly("updated_since", "page", "per_page", "include_previous_appointments");
    }

    @Test
    void singleParametersFollowTheContract() {
        JsonNode id = parameter(single(), "id");
        assertThat(id.path("in").asString()).isEqualTo("path");
        assertThat(id.path("required").asBoolean()).isTrue();
        assertThat(id.path("schema").path("type").asString()).isEqualTo("string");
        assertThat(id.path("schema").path("format").asString()).isEqualTo("uuid");
        assertThat(id.path("schema").path("pattern").asString()).isEqualTo(UUID_PATTERN);
        assertFlag(single());
        assertThat(queryParameterNames(single())).containsExactly("include_previous_appointments");
    }

    @Test
    void responsesCarryTheCorrelationHeaderAndTheSharedErrors() {
        assertThat(search().path("responses").propertyNames()).containsExactly("200", "400", "401", "406", "500");
        assertThat(single().path("responses").propertyNames())
                .containsExactly("200", "400", "401", "404", "406", "500");
        for (JsonNode operation : List.of(search(), single())) {
            assertThat(parameter(operation, "X-Correlation-Id").path("in").asString()).isEqualTo("header");
            operation.path("responses").properties().forEach(response -> {
                assertThat(response.getValue().path("headers").has("X-Correlation-Id")).as(response.getKey()).isTrue();
                assertThat(response.getValue().path("headers").has("WWW-Authenticate"))
                        .as(response.getKey()).isEqualTo("401".equals(response.getKey()));
            });
        }
        assertThat(schemaRef(search().path("responses").path("200"))).isEqualTo("PeopleApiResponse");
        assertThat(schemaRef(single().path("responses").path("200"))).isEqualTo("CurrentOfficeHolderResponse");
        assertThat(schemaRef(single().path("responses").path("404"))).isEqualTo("ErrorResponse");
    }

    @Test
    void schemasHaveExactlyTheSpecsPropertiesRequiredAndNullableFlags() {
        PROPERTIES.forEach((name, properties) -> {
            JsonNode schema = schema(name);
            assertThat(schema.path("properties").propertyNames()).as(name).containsExactlyElementsOf(properties);
            Set<String> optional = OPTIONAL.getOrDefault(name, Set.of());
            assertThat(texts(schema.path("required"))).as(name).containsExactlyInAnyOrderElementsOf(
                    properties.stream().filter(property -> !optional.contains(property)).toList());
            Set<String> nullable = NULLABLE.getOrDefault(name, Set.of());
            properties.forEach(property -> assertThat(isNullable(schema.path("properties").path(property)))
                    .as("%s.%s nullable", name, property).isEqualTo(nullable.contains(property)));
        });
        JsonNode holder = schema("CurrentOfficeHolderResponse").path("properties");
        assertThat(holder.path("id").path("format").asString()).isEqualTo("uuid");
        assertThat(holder.path("last_updated").path("format").asString()).isEqualTo("date");
        assertThat(holder.path("per_id").path("format").asString()).isEqualTo("int64");
    }

    @Test
    void theWholeDatasetMatchesTheDocumentsSchemas() {
        JsonNode body = getJson(PEOPLE + "?updated_since=2000-01-01&per_page=120&include_previous_appointments=true");

        assertThat(body.path("results")).hasSize(120);
        assertMatches(body, "PeopleApiResponse", "$");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?include_previous_appointments=true"})
    void singleResponseMatchesTheDocumentsSchema(String query) {
        String id = getJson(PEOPLE + "?updated_since=2000-01-01&per_page=120").path("results").get(60).path("id")
                .asString();

        assertMatches(getJson(PEOPLE + "/" + id + query), "CurrentOfficeHolderResponse", "$");
    }

    /**
     * Checks a value against a component schema: property names a subset of the schema's, every required property
     * present, null only where nullable, and each value of the schema's type and format; follows $ref and items.
     */
    private void assertMatches(JsonNode value, String schemaName, String at) {
        JsonNode schema = schema(schemaName);
        Set<String> declared = Set.copyOf(schema.path("properties").propertyNames());
        assertThat(declared).as(at).containsAll(value.propertyNames());
        texts(schema.path("required")).forEach(required -> assertThat(value.has(required))
                .as("%s.%s required", at, required).isTrue());
        value.properties().forEach(entry -> assertValue(entry.getValue(), schema.path("properties")
                .path(entry.getKey()), at + "." + entry.getKey()));
    }

    private void assertValue(JsonNode value, JsonNode propertySchema, String at) {
        if (propertySchema.has("$ref")) {
            assertThat(value.isObject()).as(at).isTrue();
            assertMatches(value, refName(propertySchema.path("$ref").asString()), at);
            return;
        }
        if (value.isNull()) {
            assertThat(isNullable(propertySchema)).as("%s is null but not nullable", at).isTrue();
            return;
        }
        String type = baseType(propertySchema);
        switch (type) {
            case "array" -> {
                assertThat(value.isArray()).as(at).isTrue();
                for (int index = 0; index < value.size(); index++) {
                    assertValue(value.get(index), propertySchema.path("items"), at + "[" + index + "]");
                }
            }
            case "integer" -> assertThat(value.isIntegralNumber()).as(at).isTrue();
            case "boolean" -> assertThat(value.isBoolean()).as(at).isTrue();
            case "string" -> {
                assertThat(value.isString()).as(at).isTrue();
                String format = propertySchema.path("format").asString();
                if ("date".equals(format)) {
                    assertThat(value.asString()).as(at).matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
                } else if ("uuid".equals(format)) {
                    assertThat(value.asString()).as(at)
                            .matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
                }
            }
            default -> throw new AssertionError(at + ": unexpected schema type " + type);
        }
    }

    /**
     * OpenAPI 3.1 writes a nullable type as {@code ["string", "null"]}; 3.0 as {@code nullable: true}.
     */
    private static boolean isNullable(JsonNode propertySchema) {
        JsonNode type = propertySchema.path("type");
        return propertySchema.path("nullable").asBoolean(false)
               || type.isArray() && texts(type).contains("null");
    }

    private static String baseType(JsonNode propertySchema) {
        JsonNode type = propertySchema.path("type");
        if (type.isArray()) {
            return texts(type).stream().filter(name -> !"null".equals(name)).findFirst().orElseThrow();
        }
        return type.asString();
    }

    private static void assertFlag(JsonNode operation) {
        JsonNode flag = parameter(operation, "include_previous_appointments");
        assertThat(flag.path("in").asString()).isEqualTo("query");
        assertThat(flag.path("required").asBoolean()).isFalse();
        assertThat(flag.path("schema").path("type").asString()).isEqualTo("boolean");
        assertThat(flag.path("schema").path("default").isBoolean()).isTrue();
        assertThat(flag.path("schema").path("default").asBoolean()).isFalse();
    }

    private JsonNode getJson(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
        String body = new TestRestTemplate().exchange(URI.create("http://localhost:" + port + path), HttpMethod.GET,
                                                       new HttpEntity<>(headers), String.class).getBody();
        return parse(body);
    }

    private JsonNode search() {
        return root.path("paths").path(PEOPLE).path("get");
    }

    private JsonNode single() {
        return root.path("paths").path(SINGLE).path("get");
    }

    private JsonNode schema(String name) {
        JsonNode schema = root.path("components").path("schemas").path(name);
        assertThat(schema.isMissingNode()).as("schema %s", name).isFalse();
        return schema;
    }

    private static JsonNode parameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asString())) {
                return parameter;
            }
        }
        throw new AssertionError("no parameter " + name);
    }

    private static List<String> queryParameterNames(JsonNode operation) {
        List<String> names = new ArrayList<>();
        operation.path("parameters").forEach(parameter -> {
            if ("query".equals(parameter.path("in").asString())) {
                names.add(parameter.path("name").asString());
            }
        });
        return names;
    }

    private static String schemaRef(JsonNode response) {
        return refName(response.path("content").path("application/json").path("schema").path("$ref").asString());
    }

    private static String refName(String ref) {
        return ref.replace("#/components/schemas/", "");
    }

    private static List<String> texts(JsonNode array) {
        List<String> texts = new ArrayList<>();
        array.forEach(item -> texts.add(item.asString()));
        return texts;
    }
}
