package uk.gov.hmcts.ctam.jo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The formats request parsing and fixture validation share (Principle III).
 */
class StrictFormatsTest {

    @Test
    void realDateParses() {
        assertThat(StrictFormats.strictDate("2024-02-29")).contains(LocalDate.parse("2024-02-29"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2023-02-29", "2025-6-1", "2025-06-01T00:00:00Z", "+2025-06-01", "２０２５-06-01"})
    void anythingElseIsNotADate(String text) {
        assertThat(StrictFormats.strictDate(text)).isEmpty();
    }

    @Test
    void digitsAreAsciiOnly() {
        assertThat(StrictFormats.isDigits("007")).isTrue();
        assertThat(StrictFormats.isDigits(null)).isFalse();
        assertThat(StrictFormats.isDigits("")).isFalse();
        assertThat(StrictFormats.isDigits("９")).isFalse();
        assertThat(StrictFormats.isDigits("-1")).isFalse();
    }

    @Test
    void uuidsAreCanonicalAndStoredOnesLowerCase() {
        String lower = "0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7f";

        assertThat(StrictFormats.isUuid(lower)).isTrue();
        assertThat(StrictFormats.isUuid(lower.toUpperCase())).isTrue();
        assertThat(StrictFormats.isUuid("1-1-1-1-1")).isFalse();
        assertThat(StrictFormats.isUuid(null)).isFalse();
        assertThat(StrictFormats.isLowerCaseUuid(lower)).isTrue();
        assertThat(StrictFormats.isLowerCaseUuid(lower.toUpperCase())).isFalse();
    }
}
