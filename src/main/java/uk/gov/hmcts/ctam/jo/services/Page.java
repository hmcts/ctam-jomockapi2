package uk.gov.hmcts.ctam.jo.services;

import uk.gov.hmcts.ctam.jo.domain.PaginationResponse;

import java.util.List;

/**
 * One page of an ordered result set, with the pagination block that describes it.
 *
 * @param results    the records on this page, in the result set's order
 * @param pagination where this page sits in the whole result set
 * @param <T>        the record type
 */
public record Page<T>(List<T> results, PaginationResponse pagination) {

    public Page {
        results = List.copyOf(results);
    }
}
