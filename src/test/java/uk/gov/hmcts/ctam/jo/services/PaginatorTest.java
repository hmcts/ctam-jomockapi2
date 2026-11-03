package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ctam.jo.domain.PaginationResponse;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * The pagination component at every combination of result count and page size that matters (research R8): AC-007,
 * AC-008, AC-009 and SC-002 at component level.
 */
class PaginatorTest {

    private final Paginator paginator = new Paginator();

    static Stream<Arguments> sizes() {
        return Stream.of(0, 1, 49, 50, 51, 120)
                .flatMap(n -> LongStream.of(1, 7, 50, 120, Long.MAX_VALUE).mapToObj(p -> Arguments.of(n, p)));
    }

    @ParameterizedTest(name = "N={0}, P={1}")
    @MethodSource("sizes")
    void everyPageIsRightAndThePagesTogetherAreTheInputInOrder(int n, long perPage) {
        List<Integer> input = IntStream.range(0, n).boxed().toList();
        // Computed independently of the component, in arbitrary precision.
        BigInteger size = BigInteger.valueOf(perPage);
        long pages = BigInteger.valueOf(n).add(size).subtract(BigInteger.ONE).divide(size).longValueExact();
        List<Integer> union = new ArrayList<>();

        for (long page = 1; page <= pages + 1; page++) {
            Page<Integer> result = paginator.paginate(input, new PageRequest(page, perPage));
            PaginationResponse pagination = result.pagination();

            assertThat(pagination.results()).isEqualTo(n);
            assertThat(pagination.pages()).isEqualTo(pages);
            assertThat(pagination.currentPage()).isEqualTo(page);
            assertThat(pagination.resultsPerPage()).isEqualTo(perPage);
            assertThat(pagination.morePages()).isEqualTo(page < pages);
            BigInteger from = BigInteger.valueOf(page - 1).multiply(size).min(BigInteger.valueOf(n));
            BigInteger to = from.add(size).min(BigInteger.valueOf(n));
            assertThat(result.results()).isEqualTo(input.subList(from.intValueExact(), to.intValueExact()));
            union.addAll(result.results());
        }

        assertThat(union).isEqualTo(input);
    }

    @Test
    void theLargestPageGivesAnEmptySliceWithoutOverflowing() {
        List<Integer> input = IntStream.range(0, 120).boxed().toList();

        Page<Integer> result = paginator.paginate(input, new PageRequest(Long.MAX_VALUE, Long.MAX_VALUE));

        assertThat(result.results()).isEmpty();
        assertThat(result.pagination())
                .isEqualTo(new PaginationResponse(120, 1, Long.MAX_VALUE, Long.MAX_VALUE, false));
        assertThat(paginator.paginate(input, new PageRequest(Long.MAX_VALUE, 50)).results()).isEmpty();
    }

    @Test
    void pageAndPageSizeStartAtOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PageRequest(0, 50));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageRequest(1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageRequest(-1, 50));
        assertThatIllegalArgumentException().isThrownBy(() -> new PageRequest(1, Long.MIN_VALUE));
    }

    @Test
    void theDefaultsAreTheFirstPageOfFifty() {
        assertThat(PageRequest.DEFAULT_PAGE).isEqualTo(1);
        assertThat(PageRequest.DEFAULT_PER_PAGE).isEqualTo(50);
    }
}
