package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidPersonIdException;
import uk.gov.hmcts.ctam.jo.util.StrictFormats;

import java.util.Locale;

/**
 * Parses a person id path value (FR-005, EC-005): a UUID in its canonical 8-4-4-4-12 hexadecimal form, in either
 * letter case. {@code UUID.fromString} isn't used, because it accepts non-canonical forms such as {@code 1-1-1-1-1}.
 */
@Component
public class PersonIdParser {

    /**
     * A well-formed person id. Enforced here and published in the OpenAPI document from this one place.
     */
    public static final String PATTERN = StrictFormats.UUID_PATTERN;

    /**
     * Returns the id in lower case, the form ids are stored and written in.
     *
     * @throws InvalidPersonIdException if the value isn't a canonical UUID
     */
    public String parse(String raw) {
        if (!StrictFormats.isUuid(raw)) {
            throw new InvalidPersonIdException(raw);
        }
        return raw.toLowerCase(Locale.ROOT);
    }
}
