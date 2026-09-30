package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidReferenceIdException;

import java.util.OptionalLong;
import java.util.regex.Pattern;

/**
 * Parses the {@code reference_id} path value (research R4). Only ASCII digits are accepted, leading zeros
 * included; a value too large for {@code long} cannot be a real id, so it means "no such id" rather than
 * "malformed".
 */
@Component
public class ReferenceIdParser {

    /**
     * A well-formed {@code reference_id}: ASCII digits only. Enforced here and published in the OpenAPI
     * document ({@code ReferenceDataController}) from this one place.
     */
    public static final String PATTERN = "^[0-9]+$";

    // Checked before Long.parseLong, which would also accept a sign and non-ASCII digits.
    private static final Pattern DIGITS_ONLY = Pattern.compile(PATTERN);

    /**
     * Returns the id, or empty when the value is well formed but too large to be an id.
     *
     * @throws InvalidReferenceIdException if the value is not digits only
     */
    public OptionalLong parse(String raw) {
        if (raw == null || !DIGITS_ONLY.matcher(raw).matches()) {
            throw new InvalidReferenceIdException(raw);
        }
        try {
            return OptionalLong.of(Long.parseLong(raw));
        } catch (NumberFormatException overflow) {
            return OptionalLong.empty();
        }
    }
}
