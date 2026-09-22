package uk.gov.hmcts.ctam.jo.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class HealthcheckOpenApiContractTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void openApiDocumentsHealthcheckEndpointPerContract() throws Exception {
        String apiDocsJson = restTemplate.getForObject(
                "http://localhost:" + port + "/v3/api-docs", String.class);

        JsonNode root = new ObjectMapper().readTree(apiDocsJson);
        JsonNode getOperation = root.path("paths").path("/api/v1/healthcheck").path("get");

        assertThat(getOperation.path("summary").asText()).isEqualTo("Healthcheck");

        JsonNode okResponse = getOperation.path("responses").path("200");
        assertThat(okResponse.path("description").asText()).isEqualTo("Service is healthy");

        JsonNode schemaRef = okResponse.path("content").path("application/json").path("schema");
        String schemaName = schemaRef.path("$ref").asText().replace("#/components/schemas/", "");
        JsonNode properties = root.path("components").path("schemas")
                .path(schemaName).path("properties");

        assertThat(properties.properties()).hasSize(1);
        assertThat(properties.path("status").path("type").asText()).isEqualTo("string");
    }
}
