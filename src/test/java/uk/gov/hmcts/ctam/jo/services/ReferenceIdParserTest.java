package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidReferenceIdException;

import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_REFERENCE_ID;

class ReferenceIdParserTest {

    private final ReferenceIdParser parser = new ReferenceIdParser();

    @ParameterizedTest
    @CsvSource({"0, 0", "7, 7", "007, 7", "9223372036854775807, 9223372036854775807"})
    void acceptsAsciiDigits(String raw, long expected) {
        assertThat(parser.parse(raw)).hasValue(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1", "+1", " 1", "1 ", "1e3", "", "０", "١"})
    void rejectsAnythingElse(String raw) {
        assertThatThrownBy(() -> parser.parse(raw))
                .isInstanceOf(InvalidReferenceIdException.class)
                .hasMessage(MALFORMED_REFERENCE_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "123456789012345678901234567890"})
    void treatsValuesTooLargeForLongAsNoSuchId(String raw) {
        assertThat(parser.parse(raw)).isEqualTo(OptionalLong.empty());
    }

    @Test
    void keepsOnlyASanitisedCopyOfTheRawValueForLogs() {
        assertThatThrownBy(() -> parser.parse("abc\r\nforged"))
                .isInstanceOfSatisfying(InvalidReferenceIdException.class,
                    ex -> assertThat(ex.getDiagnostic()).isEqualTo("abc\\r\\nforged"));
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(InvalidReferenceIdException.class);
    }
}
