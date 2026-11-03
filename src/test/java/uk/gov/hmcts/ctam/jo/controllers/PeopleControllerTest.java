package uk.gov.hmcts.ctam.jo.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.ctam.jo.config.SecurityProperties;
import uk.gov.hmcts.ctam.jo.config.WebConfig;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.domain.PaginationResponse;
import uk.gov.hmcts.ctam.jo.domain.PeopleApiResponse;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponseFactory;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidPersonIdException;
import uk.gov.hmcts.ctam.jo.exceptions.PersonNotFoundException;
import uk.gov.hmcts.ctam.jo.filters.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIdFilter;
import uk.gov.hmcts.ctam.jo.services.PeopleService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_PERSON_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.NOT_ACCEPTABLE;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PERSON_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.REPEATED_QUERY_PARAMETER;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_QUERY_PARAMETER;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.FIRST_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.MINIMAL;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.repository;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.mapper;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.TOKEN;

/**
 * The People routes' binding, routing and HTTP errors, with the service mocked (013 FR-001 to FR-007, FR-015, FR-016).
 */
@WebMvcTest(PeopleController.class)
@Import({WebConfig.class, ErrorResponseFactory.class, CorrelationIdFilter.class, BearerTokenAuthenticationFilter.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "jo.security.bearer-tokens=" + TOKEN)
class PeopleControllerTest {

    private static final String PEOPLE = "/api/v1/people";

    private static final CurrentOfficeHolderResponse PERSON =
            mapper().toResponse(repository(MINIMAL).findById(FIRST_ID).orElseThrow(), false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PeopleService service;

    private ResultActions perform(String uri) throws Exception {
        return mockMvc.perform(get(uri).header("Authorization", BEARER_TOKEN));
    }

    @Test
    void searchReturnsResultsAndPagination() throws Exception {
        given(service.search("2000-01-01", null, null, null)).willReturn(
                new PeopleApiResponse(List.of(PERSON), new PaginationResponse(1, 1, 1, 50, false)));

        perform(PEOPLE + "?updated_since=2000-01-01")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.results[0].id").value(FIRST_ID))
                .andExpect(jsonPath("$.pagination.results_per_page").value(50))
                .andExpect(jsonPath("$.pagination.more_pages").value(false));
    }

    @Test
    void theServiceReceivesTheRawStrings() throws Exception {
        given(service.search(any(), any(), any(), any())).willReturn(
                new PeopleApiResponse(List.of(), new PaginationResponse(0, 0, 7, 9, false)));

        perform(PEOPLE + "?updated_since=x&page=007&per_page=9&include_previous_appointments=TRUE")
                .andExpect(status().isOk());

        verify(service).search("x", "007", "9", "TRUE");
    }

    @Test
    void singleReturnsOneObject() throws Exception {
        given(service.getById(FIRST_ID, "true")).willReturn(PERSON);

        perform(PEOPLE + "/" + FIRST_ID + "?include_previous_appointments=true")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(FIRST_ID))
                .andExpect(jsonPath("$.results").doesNotExist())
                .andExpect(jsonPath("$.pagination").doesNotExist());
    }

    @Test
    void anUnknownPersonIs404() throws Exception {
        given(service.getById(FIRST_ID, null)).willThrow(new PersonNotFoundException(FIRST_ID));

        perform(PEOPLE + "/" + FIRST_ID).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(PERSON_NOT_FOUND));
    }

    @Test
    void malformedIdIs400() throws Exception {
        given(service.getById("abc", null)).willThrow(new InvalidPersonIdException("abc"));

        perform(PEOPLE + "/abc").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(MALFORMED_PERSON_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {PEOPLE + "/", PEOPLE + "/" + FIRST_ID + "/x"})
    void trailingSlashesAndExtraSegmentsAre404(String uri) throws Exception {
        perform(uri).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value(RESOURCE_NOT_FOUND));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"updated_since=2000-01-01", "page=1", "per_page=1", "foo=1"})
    void searchOnlyParametersAreUndefinedOnTheSingleRoute(String query) throws Exception {
        perform(PEOPLE + "/" + FIRST_ID + "?" + query).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(UNSUPPORTED_QUERY_PARAMETER));
        verifyNoInteractions(service);
    }

    @Test
    void anUndefinedParameterOnTheSearchIs400() throws Exception {
        perform(PEOPLE + "?updated_since=2000-01-01&foo=1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(UNSUPPORTED_QUERY_PARAMETER));
        verifyNoInteractions(service);
    }

    @Test
    void repeatedParameterIs400() throws Exception {
        perform(PEOPLE + "?updated_since=2000-01-01&page=1&page=2").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(REPEATED_QUERY_PARAMETER));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {PEOPLE, PEOPLE + "/" + FIRST_ID})
    void otherMethodsAre405WithAllowGet(String uri) throws Exception {
        mockMvc.perform(post(uri).header("Authorization", BEARER_TOKEN))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.error").value(METHOD_NOT_ALLOWED));
    }

    @ParameterizedTest
    @ValueSource(strings = {PEOPLE + "?updated_since=2000-01-01", PEOPLE + "/" + FIRST_ID})
    void anAcceptHeaderExcludingJsonIs406(String uri) throws Exception {
        mockMvc.perform(get(uri).header("Authorization", BEARER_TOKEN).header("Accept", "text/plain"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.error").value(NOT_ACCEPTABLE));
    }
}
