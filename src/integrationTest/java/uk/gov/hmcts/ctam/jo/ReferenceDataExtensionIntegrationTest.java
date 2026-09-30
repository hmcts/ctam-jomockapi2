package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_REFERENCE_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.QUERY_PARAMETERS_NOT_SUPPORTED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RECORD_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.JsonTrees.parse;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * A new type needs only a configuration entry and a fixture: {@code test_widgets} and {@code empty_things}
 * are declared here in test properties and served by the same routes, validation, authentication and
 * errors, with no endpoint code (spec AC-019, EC-011, SC-007).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "jo.reference-data.types[0].name=appointment_titles",
    "jo.reference-data.types[0].aliases[0]=appointment_title",
    "jo.reference-data.types[0].fixture=classpath:reference-data/appointment_titles.json",
    "jo.reference-data.types[1].name=test_widgets",
    "jo.reference-data.types[1].aliases[0]=test_widget",
    "jo.reference-data.types[1].fixture=classpath:reference-data/test_widgets.json",
    "jo.reference-data.types[2].name=empty_things",
    "jo.reference-data.types[2].fixture=classpath:reference-data/empty_things.json"
})
class ReferenceDataExtensionIntegrationTest {

    private static final String BASE = "/api/v1/reference_data/";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ac019TheNewTypeIsServedInAscendingIdOrder() throws Exception {
        MockHttpServletResponse response = perform(authorised(BASE + "test_widgets"));

        assertThat(response.getStatus()).isEqualTo(200);
        List<Long> ids = new ArrayList<>();
        json(response).get("results").forEach(record -> ids.add(record.get("id").asLong()));
        assertThat(ids).containsExactly(3L, 5L, 9L);
    }

    @Test
    void ac019TheNewTypesAliasAndSingleRecordRouteWork() throws Exception {
        MockHttpServletResponse response = perform(authorised(BASE + "test_widget/9"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json(response).get("name").asString()).isEqualTo("Widget Nine");
        assertThat(json(response).get("end_date").asString()).isEqualTo("2025-03-31");
    }

    @Test
    void ac019TheNewTypeGetsTheSameValidationAuthenticationAndErrors() throws Exception {
        assertError(perform(authorised(BASE + "test_widgets/4")), 404, RECORD_NOT_FOUND);
        assertError(perform(authorised(BASE + "test_widgets/x")), 400,
                    MALFORMED_REFERENCE_ID);
        assertError(perform(get(BASE + "test_widgets")), 401, UNAUTHORIZED);
        assertError(perform(authorised(BASE + "test_widgets?a=1")), 400,
                    QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @Test
    void ec011AnEmptyFixtureGivesEmptyResultsAndEveryIdIsUnknown() throws Exception {
        MockHttpServletResponse collection = perform(authorised(BASE + "empty_things"));

        assertThat(collection.getStatus()).isEqualTo(200);
        assertThat(collection.getContentAsString()).isEqualTo("{\"results\":[]}");
        assertError(perform(authorised(BASE + "empty_things/1")), 404, RECORD_NOT_FOUND);
    }

    @Test
    void theOpenApiEnumListsEveryConfiguredName() throws Exception {
        JsonNode apiDocs = json(perform(get("/v3/api-docs")));
        JsonNode parameters = apiDocs.path("paths").path("/api/v1/reference_data/{attribute_name}")
                .path("get").path("parameters");

        List<String> names = new ArrayList<>();
        for (JsonNode parameter : parameters) {
            if ("attribute_name".equals(parameter.path("name").asString())) {
                parameter.path("schema").path("enum").forEach(value -> names.add(value.asString()));
            }
        }
        assertThat(names).containsExactly(
                "appointment_titles", "test_widgets", "empty_things", "appointment_title", "test_widget");
    }

    @Test
    void nameReusedAsAnotherTypesAliasStopsStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(RegistryOnly.class)
                .withPropertyValues(
                        "jo.reference-data.types[0].name=appointment_titles",
                        "jo.reference-data.types[0].fixture=classpath:reference-data/appointment_titles.json",
                        "jo.reference-data.types[1].name=test_widgets",
                        "jo.reference-data.types[1].aliases[0]=appointment_titles",
                        "jo.reference-data.types[1].fixture=classpath:reference-data/test_widgets.json")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("'appointment_titles' is declared by both"));
    }

    private MockHttpServletRequestBuilder authorised(String path) {
        return get(path).header("Authorization", BEARER_TOKEN);
    }

    private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private JsonNode json(MockHttpServletResponse response) throws Exception {
        return parse(response.getContentAsString());
    }

    private void assertError(MockHttpServletResponse response, int status, String message) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertErrorBody(response.getContentAsString(), message);
    }

    /**
     * Just the properties binding and the registry, for the startup-failure case. Deliberately not annotated
     * {@code @Configuration}: a nested configuration class would replace the application's own in the
     * {@code @SpringBootTest} context above.
     */
    @EnableConfigurationProperties(ReferenceDataProperties.class)
    static class RegistryOnly {

        @Bean
        ReferenceDataTypeRegistry referenceDataTypeRegistry(ReferenceDataProperties properties) {
            return new ReferenceDataTypeRegistry(properties);
        }
    }
}
