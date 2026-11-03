package uk.gov.hmcts.ctam.jo.services;

/**
 * Which page of an ordered result set to return, and how many records a page holds. Both start at 1 (FR-013).
 *
 * @param page    the 1-based page number
 * @param perPage the page size
 */
public record PageRequest(long page, long perPage) {

    /**
     * The page returned when the caller names none.
     */
    public static final long DEFAULT_PAGE = 1;

    /**
     * The page size used when the caller names none.
     */
    public static final long DEFAULT_PER_PAGE = 50;

    public PageRequest {
        if (page < 1 || perPage < 1) {
            throw new IllegalArgumentException("page and perPage must be at least 1");
        }
    }
}
