package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidQueryParameterException;
import uk.gov.hmcts.ctam.jo.util.StrictFormats;

import java.time.LocalDate;

/**
 * Turns raw query-parameter strings into values, with one fixed message per parameter and kind of failure (research
 * R5). Controllers bind every parameter as an optional string, so Spring's own conversion messages never reach a
 * caller and the order of checks stays with the caller. Messages are built from the parameter's name, never from the
 * value.
 */
@Component
public class QueryParameterParser {

    /**
     * A required real calendar date written exactly as {@code YYYY-MM-DD}.
     *
     * @throws InvalidQueryParameterException if the value is missing, empty or not such a date
     */
    public LocalDate requiredDate(String name, String raw) {
        if (raw == null || raw.isEmpty()) {
            throw InvalidQueryParameterException.required(name);
        }
        return StrictFormats.strictDate(raw).orElseThrow(() -> InvalidQueryParameterException.invalidDate(name));
    }

    /**
     * An optional positive whole number in ASCII decimal digits, leading zeros allowed, up to {@link Long#MAX_VALUE}.
     *
     * @throws InvalidQueryParameterException if the value is present and not such a number
     */
    public long positiveWholeNumber(String name, String raw, long defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        if (!StrictFormats.isDigits(raw)) {
            throw InvalidQueryParameterException.notPositiveWholeNumber(name);
        }
        long value;
        try {
            value = Long.parseLong(raw);
        } catch (NumberFormatException tooLarge) {
            throw InvalidQueryParameterException.notPositiveWholeNumber(name);
        }
        if (value < 1) {
            throw InvalidQueryParameterException.notPositiveWholeNumber(name);
        }
        return value;
    }

    /**
     * An optional flag: exactly {@code true} or {@code false}; absent means false.
     *
     * @throws InvalidQueryParameterException if the value is present and anything else
     */
    public boolean flag(String name, String raw) {
        if (raw == null) {
            return false;
        }
        return switch (raw) {
            case "true" -> true;
            case "false" -> false;
            default -> throw InvalidQueryParameterException.notBoolean(name);
        };
    }
}
