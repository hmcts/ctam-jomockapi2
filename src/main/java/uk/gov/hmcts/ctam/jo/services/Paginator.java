package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.domain.PaginationResponse;

import java.util.List;

/**
 * The one pagination component (FR-017 to FR-019, Principles III and VI). It knows nothing about the records it pages,
 * so every paginated endpoint uses it unchanged. Page and page size may each be as large as {@link Long#MAX_VALUE};
 * the arithmetic never overflows (research R8).
 */
@Component
public class Paginator {

    /**
     * Returns the requested page of {@code ordered}. A page past the last is empty, with the same pagination block as
     * any other page apart from {@code current_page}.
     */
    public <T> Page<T> paginate(List<T> ordered, PageRequest request) {
        long results = ordered.size();
        long perPage = request.perPage();
        long page = request.page();
        long pages = results / perPage + (results % perPage == 0 ? 0 : 1);
        List<T> slice = List.of();
        if (page <= pages) {
            // page <= pages, so (page - 1) * perPage < results, which is an int: no overflow.
            int from = (int) ((page - 1) * perPage);
            int to = (int) (from + Math.min(perPage, results - from));
            slice = ordered.subList(from, to);
        }
        return new Page<>(slice, new PaginationResponse(results, pages, page, perPage, page < pages));
    }
}
