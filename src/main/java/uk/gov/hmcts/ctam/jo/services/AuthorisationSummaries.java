package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationSummaryResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationsResponse;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Summarises dated authorisations by jurisdiction (013 FR-044, FR-025): one entry per jurisdiction name, in ascending
 * order, each listing its distinct ticket names in ascending order. Names are compared with {@link String#compareTo}.
 */
@Component
public class AuthorisationSummaries {

    public List<AuthorisationSummaryResponse> summarise(List<AuthorisationsResponse> authorisations) {
        Map<String, SortedSet<String>> byJurisdiction = new TreeMap<>();
        for (AuthorisationsResponse authorisation : authorisations) {
            byJurisdiction.computeIfAbsent(authorisation.jurisdiction(), name -> new TreeSet<>())
                    .add(authorisation.ticket());
        }
        return byJurisdiction.entrySet().stream()
                .map(entry -> new AuthorisationSummaryResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }
}
