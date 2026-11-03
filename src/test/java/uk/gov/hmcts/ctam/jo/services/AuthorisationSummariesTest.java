package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationSummaryResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationsResponse;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 013 FR-044 and FR-025: grouped by jurisdiction name, distinct tickets, both in ascending order.
 */
class AuthorisationSummariesTest {

    private final AuthorisationSummaries summaries = new AuthorisationSummaries();

    @Test
    void groupsByJurisdictionWithDistinctSortedTickets() {
        List<AuthorisationSummaryResponse> summary = summaries.summarise(List.of(
                authorisation("Tribunals", "b ticket"), authorisation("Courts", "Civil Authorisation"),
                authorisation("Tribunals", "a ticket"), authorisation("Tribunals", "b ticket"),
                authorisation("Courts", "Bail")));

        assertThat(summary).containsExactly(
                new AuthorisationSummaryResponse("Courts", List.of("Bail", "Civil Authorisation")),
                new AuthorisationSummaryResponse("Tribunals", List.of("a ticket", "b ticket")));
    }

    @Test
    void noAuthorisationsGiveAnEmptySummary() {
        assertThat(summaries.summarise(List.of())).isEmpty();
    }

    @Test
    void ticketNameSharedByTwoJurisdictionsIsKeptInBoth() {
        assertThat(summaries.summarise(List.of(authorisation("Courts", "Shared"),
                                               authorisation("Tribunals", "Shared"))))
                .containsExactly(new AuthorisationSummaryResponse("Courts", List.of("Shared")),
                                 new AuthorisationSummaryResponse("Tribunals", List.of("Shared")));
    }

    private static AuthorisationsResponse authorisation(String jurisdiction, String ticket) {
        return new AuthorisationsResponse(1, 2, jurisdiction, 30, ticket, 294, LocalDate.parse("2020-01-01"), null);
    }
}
