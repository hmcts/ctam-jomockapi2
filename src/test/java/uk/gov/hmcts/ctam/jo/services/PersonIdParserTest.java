package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidPersonIdException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_PERSON_ID;

/**
 * FR-005 and EC-005: a canonical UUID in either case, written back in lower case; anything else is malformed.
 */
class PersonIdParserTest {

    private static final String ID = "0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7f";

    private final PersonIdParser parser = new PersonIdParser();

    @ParameterizedTest
    @ValueSource(strings = {ID, "0B7C4F2E-5A1D-4C3B-9E8F-2D6A1B0C9E7F", "0b7C4f2E-5a1D-4c3B-9e8F-2d6A1b0C9e7F"})
    void canonicalUuidInEitherCaseIsReturnedInLowerCase(String raw) {
        assertThat(parser.parse(raw)).isEqualTo(ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "19759", "9100120", "0b7c4f2e5a1d4c3b9e8f2d6a1b0c9e7f", "1-1-1-1-1",
        "{0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7f}", ID + " ", "", "0b7c4f2e-5a1d-4c3b-9e8f-2d6a1b0c9e7g"})
    void anythingElseIsMalformed(String raw) {
        assertThatThrownBy(() -> parser.parse(raw))
                .isInstanceOf(InvalidPersonIdException.class)
                .hasMessage(MALFORMED_PERSON_ID);
    }
}
