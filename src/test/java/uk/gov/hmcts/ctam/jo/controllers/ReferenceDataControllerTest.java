package uk.gov.hmcts.ctam.jo.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.ctam.jo.config.SecurityProperties;
import uk.gov.hmcts.ctam.jo.config.WebConfig;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataApiResponse;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponseFactory;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;
import uk.gov.hmcts.ctam.jo.filters.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIdFilter;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.endedResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.fullResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.nullableParentResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.parentCategoryResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.referencingResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.response;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.referencingTextResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.salariedResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.textResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.ticketCategoryResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.TOKEN;

@WebMvcTest(ReferenceDataController.class)
@Import({WebConfig.class, ErrorResponseFactory.class, CorrelationIdFilter.class, BearerTokenAuthenticationFilter.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "jo.security.bearer-tokens=" + TOKEN)
class ReferenceDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferenceDataService service;

    @Test
    void collectionReturnsResultsWithSnakeCaseFieldsAndExplicitNullEndDate() throws Exception {
        given(service.getAll("widgets")).willReturn(new ReferenceDataApiResponse(List.of(
                response(10, "Alpha"), endedResponse(70, "Ended"))));

        mockMvc.perform(get("/api/v1/reference_data/widgets").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.*", hasSize(1)))
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].*", hasSize(6)))
                .andExpect(jsonPath("$.results[0].id").value(10))
                .andExpect(jsonPath("$.results[0].name").value("Alpha"))
                .andExpect(jsonPath("$.results[0].created_at").value("2024-01-15T09:00:00Z"))
                .andExpect(jsonPath("$.results[0].updated_at").value("2024-06-03T10:30:00Z"))
                .andExpect(jsonPath("$.results[0].start_date").value("2024-01-01"))
                .andExpect(jsonPath("$.results[0].end_date").value(nullValue()))
                .andExpect(jsonPath("$.results[0]").value(hasKey("end_date")))
                .andExpect(jsonPath("$.results[1].end_date").value("2025-03-31"));

        verify(service).getAll("widgets");
    }

    @Test
    void referenceFieldsAreWrittenAfterNameOnlyWhenTheRecordCarriesThem() throws Exception {
        given(service.getById("widgets", "10")).willReturn(referencingResponse(10, "Alpha", 8, 1722, 84));

        String body = mockMvc.perform(get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(9)))
                .andExpect(jsonPath("$.type_id").value(8))
                .andExpect(jsonPath("$.parent_id").value(1722))
                .andExpect(jsonPath("$.jurisdiction_id").value(84))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).startsWith(
                "{\"id\":10,\"name\":\"Alpha\",\"type_id\":8,\"parent_id\":1722,\"jurisdiction_id\":84,"
                + "\"created_at\":");
    }

    @Test
    void nullableReferenceFieldWithoutAValueIsWrittenAsNull() throws Exception {
        given(service.getById("widgets", "10")).willReturn(nullableParentResponse(10, "Alpha", 6, 84));

        String body = mockMvc.perform(get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(9)))
                .andExpect(jsonPath("$").value(hasKey("parent_id")))
                .andExpect(jsonPath("$.parent_id").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).startsWith(
                "{\"id\":10,\"name\":\"Alpha\",\"type_id\":6,\"parent_id\":null,\"jurisdiction_id\":84,"
                + "\"created_at\":");
    }

    @Test
    void recordsWithoutReferenceFieldsLeaveThemOut() throws Exception {
        given(service.getById("widgets", "10")).willReturn(response(10, "Alpha"));

        mockMvc.perform(get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type_id").doesNotExist())
                .andExpect(jsonPath("$.parent_id").doesNotExist())
                .andExpect(jsonPath("$.jurisdiction_id").doesNotExist())
                .andExpect(jsonPath("$.ticket_category_id").doesNotExist())
                .andExpect(jsonPath("$.parent_category_id").doesNotExist())
                .andExpect(jsonPath("$.salaried").doesNotExist())
                .andExpect(jsonPath("$.integration_code").doesNotExist());
    }

    @Test
    void ticketCategoryFieldIsWrittenAfterNameOnlyWhenTheRecordCarriesIt() throws Exception {
        given(service.getById("widgets", "10")).willReturn(ticketCategoryResponse(10, "Categorised", 31));

        String body = mockMvc.perform(get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(7)))
                .andExpect(jsonPath("$.ticket_category_id").value(31))
                .andExpect(jsonPath("$.type_id").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).startsWith("{\"id\":10,\"name\":\"Categorised\",\"ticket_category_id\":31,\"created_at\":");
    }

    @Test
    void parentCategoryFieldIsWrittenOnlyWhenTheRecordCarriesIt() throws Exception {
        given(service.getById("widgets", "10")).willReturn(parentCategoryResponse(10, "Child", 48L));

        String body = mockMvc.perform(get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(7)))
                .andExpect(jsonPath("$.parent_category_id").value(48))
                .andExpect(jsonPath("$.parent_id").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).startsWith("{\"id\":10,\"name\":\"Child\",\"parent_category_id\":48,\"created_at\":");
    }

    @Test
    void nullParentCategoryIsWrittenAsNull() throws Exception {
        given(service.getById("widgets", "10")).willReturn(parentCategoryResponse(10, "Top", null));

        String body = mockMvc.perform(get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(7)))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).startsWith("{\"id\":10,\"name\":\"Top\",\"parent_category_id\":null,\"created_at\":");
    }

    @Test
    void booleanFieldsAreWrittenAfterTheReferenceFieldsOnlyWhenTheRecordCarriesThem() throws Exception {
        given(service.getById("widgets", "10")).willReturn(salariedResponse(10, "Alpha", false));
        given(service.getById("widgets", "20")).willReturn(fullResponse(20, "Bravo", 8, 1722, 84, true));

        String withoutReferences = mockMvc.perform(
                        get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(7)))
                .andExpect(jsonPath("$.salaried").value(false))
                .andReturn().getResponse().getContentAsString();
        String withReferences = mockMvc.perform(
                        get("/api/v1/reference_data/widgets/20").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(10)))
                .andReturn().getResponse().getContentAsString();

        // false is written, not left out.
        assertThat(withoutReferences).startsWith("{\"id\":10,\"name\":\"Alpha\",\"salaried\":false,\"created_at\":");
        assertThat(withReferences).startsWith(
                "{\"id\":20,\"name\":\"Bravo\",\"type_id\":8,\"parent_id\":1722,\"jurisdiction_id\":84,"
                + "\"salaried\":true,\"created_at\":");
    }

    @Test
    void textFieldsAreWrittenAfterTheReferenceFieldsOnlyWhenTheRecordCarriesThem() throws Exception {
        given(service.getById("widgets", "10")).willReturn(textResponse(10, "Alpha", "WIDGET_A"));
        given(service.getById("widgets", "20")).willReturn(referencingTextResponse(20, "Bravo", 8, 1722, 84,
                                                                                   "WIDGET_B"));

        String withoutReferences = mockMvc.perform(
                        get("/api/v1/reference_data/widgets/10").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(7)))
                .andExpect(jsonPath("$.integration_code").value("WIDGET_A"))
                .andReturn().getResponse().getContentAsString();
        String withReferences = mockMvc.perform(
                        get("/api/v1/reference_data/widgets/20").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(10)))
                .andReturn().getResponse().getContentAsString();

        assertThat(withoutReferences).startsWith(
                "{\"id\":10,\"name\":\"Alpha\",\"integration_code\":\"WIDGET_A\",\"created_at\":");
        assertThat(withReferences).startsWith(
                "{\"id\":20,\"name\":\"Bravo\",\"type_id\":8,\"parent_id\":1722,\"jurisdiction_id\":84,"
                + "\"integration_code\":\"WIDGET_B\",\"created_at\":");
    }

    @Test
    void collectionPassesThePathAttributeNameUnchanged() throws Exception {
        given(service.getAll("Some_Name")).willReturn(new ReferenceDataApiResponse(List.of()));

        mockMvc.perform(get("/api/v1/reference_data/Some_Name").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(0)));

        verify(service).getAll("Some_Name");
    }

    @Test
    void singleRecordReturnsOneObjectNotWrappedInResults() throws Exception {
        given(service.getById("widgets", "70")).willReturn(endedResponse(70, "Ended"));

        mockMvc.perform(get("/api/v1/reference_data/widgets/70").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(jsonPath("$.results").doesNotExist())
                .andExpect(jsonPath("$.id").value(70))
                .andExpect(jsonPath("$.name").value("Ended"))
                .andExpect(jsonPath("$.end_date").value("2025-03-31"));

        verify(service).getById("widgets", "70");
    }

    @Test
    void singleRecordPassesTheRawPathValueToTheService() throws Exception {
        given(service.getById("widgets", "007")).willReturn(response(7, "Seven"));

        mockMvc.perform(get("/api/v1/reference_data/widgets/007").header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk());

        verify(service).getById("widgets", "007");
    }

    @Test
    void unsupportedTypeFromTheServiceReturns400InTheSharedShape() throws Exception {
        given(service.getAll("foo")).willThrow(new UnsupportedReferenceDataTypeException("foo"));
        given(service.getById("foo", "10")).willThrow(new UnsupportedReferenceDataTypeException("foo"));

        for (String path : new String[] {"/api/v1/reference_data/foo", "/api/v1/reference_data/foo/10"}) {
            mockMvc.perform(get(path).header("Authorization", BEARER_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.*", hasSize(3)))
                    .andExpect(jsonPath("$.error").value(UNSUPPORTED_ATTRIBUTE_NAME))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.results").doesNotExist());
        }
    }
}
