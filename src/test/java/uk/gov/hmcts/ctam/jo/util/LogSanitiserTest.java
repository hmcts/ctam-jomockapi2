package uk.gov.hmcts.ctam.jo.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitiserTest {

    @Test
    void escapesCarriageReturnAndLineFeedSoNoLogLineCanBeForged() {
        String sanitised = LogSanitiser.sanitise("abc\r\nINFO forged entry");

        assertThat(sanitised).isEqualTo("abc\\r\\nINFO forged entry").doesNotContain("\r", "\n");
    }

    @Test
    void escapesTab() {
        assertThat(LogSanitiser.sanitise("a\tb")).isEqualTo("a\\tb");
    }

    @Test
    void escapesQuotesAndBackslash() {
        assertThat(LogSanitiser.sanitise("\"x\" 'y' \\z")).isEqualTo("\\\"x\\\" \\'y\\' \\\\z");
    }

    @Test
    void escapesNonAsciiSoOutputIsPlainAscii() {
        String sanitised = LogSanitiser.sanitise("é日 ");

        assertThat(sanitised).isEqualTo("\\351\\u65e5\\u2028");
        assertThat(sanitised.chars()).allMatch(c -> c >= 0x20 && c < 0x7f);
    }

    @Test
    void leavesPlainValuesUnchanged() {
        assertThat(LogSanitiser.sanitise("ref-abc_123.v2")).isEqualTo("ref-abc_123.v2");
    }

    @Test
    void rendersNullAsTheWordNull() {
        assertThat(LogSanitiser.sanitise(null)).isEqualTo("null");
    }
}
