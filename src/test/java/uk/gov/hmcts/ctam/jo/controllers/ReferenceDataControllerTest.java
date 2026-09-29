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
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.exceptions.ErrorResponseFactory;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;
import uk.gov.hmcts.ctam.jo.filters.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ctam.jo.filters.CorrelationIdFilter;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferenceDataController.class)
@Import({WebConfig.class, ErrorResponseFactory.class, CorrelationIdFilter.class, BearerTokenAuthenticationFilter.class})
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "jo.security.bearer-tokens=test-token")
class ReferenceDataControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferenceDataService service;

    @Test
    void collectionReturnsResultsWithSnakeCaseFieldsAndExplicitNullEndDate() throws Exception {
        given(service.getAll("widgets")).willReturn(new ReferenceDataApiResponse(List.of(
                new ReferenceDataResponse(10, "Alpha", Instant.parse("2024-01-15T09:00:00Z"),
                        Instant.parse("2024-06-03T10:30:00Z"), LocalDate.parse("2024-01-01"), null),
                new ReferenceDataResponse(70, "Ended", Instant.parse("2024-01-15T09:00:00Z"),
                        Instant.parse("2025-04-01T08:00:00Z"), LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2025-03-31")))));

        mockMvc.perform(get("/api/v1/reference_data/widgets").header("Authorization", TOKEN))
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
    void collectionPassesThePathAttributeNameUnchanged() throws Exception {
        given(service.getAll("Some_Name")).willReturn(new ReferenceDataApiResponse(List.of()));

        mockMvc.perform(get("/api/v1/reference_data/Some_Name").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(0)));

        verify(service).getAll("Some_Name");
    }

    @Test
    void singleRecordReturnsOneObjectNotWrappedInResults() throws Exception {
        given(service.getById("widgets", "70")).willReturn(new ReferenceDataResponse(70, "Ended",
                Instant.parse("2024-01-15T09:00:00Z"), Instant.parse("2025-04-01T08:00:00Z"),
                LocalDate.parse("2024-01-01"), LocalDate.parse("2025-03-31")));

        mockMvc.perform(get("/api/v1/reference_data/widgets/70").header("Authorization", TOKEN))
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
        given(service.getById("widgets", "007")).willReturn(new ReferenceDataResponse(7, "Seven",
                Instant.parse("2024-01-15T09:00:00Z"), Instant.parse("2024-06-03T10:30:00Z"),
                LocalDate.parse("2024-01-01"), null));

        mockMvc.perform(get("/api/v1/reference_data/widgets/007").header("Authorization", TOKEN))
                .andExpect(status().isOk());

        verify(service).getById("widgets", "007");
    }

    @Test
    void unsupportedTypeFromTheServiceReturns400InTheSharedShape() throws Exception {
        given(service.getAll("foo")).willThrow(new UnsupportedReferenceDataTypeException("foo"));
        given(service.getById("foo", "10")).willThrow(new UnsupportedReferenceDataTypeException("foo"));

        for (String path : new String[] {"/api/v1/reference_data/foo", "/api/v1/reference_data/foo/10"}) {
            mockMvc.perform(get(path).header("Authorization", TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.*", hasSize(3)))
                    .andExpect(jsonPath("$.error").value("Unsupported reference data attribute_name."))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.results").doesNotExist());
        }
    }
}
