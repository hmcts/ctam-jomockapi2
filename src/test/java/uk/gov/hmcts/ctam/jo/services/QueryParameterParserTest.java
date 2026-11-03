package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidQueryParameterException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.FLAG_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PAGE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PER_PAGE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UPDATED_SINCE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UPDATED_SINCE_REQUIRED;

/**
 * Every valid and invalid class of FR-010, FR-013 and FR-014 (research R5).
 */
class QueryParameterParserTest {

    private static final String FLAG = "include_previous_appointments";

    private final QueryParameterParser parser = new QueryParameterParser();

    @ParameterizedTest
    @ValueSource(strings = {"2025-06-01", "2024-02-29"})
    void realDateParses(String raw) {
        assertThat(parser.requiredDate("updated_since", raw)).isEqualTo(LocalDate.parse(raw));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void missingOrEmptyDateIsRequired(String raw) {
        assertRejected(() -> parser.requiredDate("updated_since", raw), UPDATED_SINCE_REQUIRED, raw);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-02-30", "2023-02-29", "2025-6-1", "20250601", "2025-06-01T00:00:00Z", "2025-06-01 ",
        "+2025-06-01", "yesterday", "２０２５-06-01"})
    void anythingElseIsNotAValidDate(String raw) {
        assertRejected(() -> parser.requiredDate("updated_since", raw), UPDATED_SINCE_INVALID, raw);
    }

    @ParameterizedTest
    @CsvSource({"page, 1", "per_page, 50"})
    void anAbsentNumberGivesItsDefault(String name, long defaultValue) {
        assertThat(parser.positiveWholeNumber(name, null, defaultValue)).isEqualTo(defaultValue);
    }

    @ParameterizedTest
    @CsvSource({"page, 1, 1", "page, 007, 7", "page, 9223372036854775807, 9223372036854775807", "per_page, 1, 1",
        "per_page, 007, 7", "per_page, 9223372036854775807, 9223372036854775807"})
    void positiveWholeNumberParses(String name, String raw, long expected) {
        assertThat(parser.positiveWholeNumber(name, raw, 99)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "00", "-1", "+1", "1.0", "1e3", " 1", "abc", "９", "9223372036854775808"})
    void anythingElseIsNotAPositiveWholeNumber(String raw) {
        assertRejected(() -> parser.positiveWholeNumber("page", raw, 1), PAGE_INVALID, raw);
        assertRejected(() -> parser.positiveWholeNumber("per_page", raw, 50), PER_PAGE_INVALID, raw);
    }

    @Test
    void anAbsentFlagIsFalse() {
        assertThat(parser.flag(FLAG, null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true, true", "false, false"})
    void exactlyTrueOrFalseParses(String raw, boolean expected) {
        assertThat(parser.flag(FLAG, raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRUE", "True", "1", "yes", ""})
    void anyOtherFlagValueIsRejected(String raw) {
        assertRejected(() -> parser.flag(FLAG, raw), FLAG_INVALID, raw);
    }

    private static void assertRejected(Runnable parse, String message, String raw) {
        assertThatThrownBy(parse::run)
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessage(message)
                .satisfies(ex -> {
                    if (raw != null && !raw.isBlank()) {
                        assertThat(ex.getMessage()).doesNotContain(raw);
                    }
                });
    }
}
