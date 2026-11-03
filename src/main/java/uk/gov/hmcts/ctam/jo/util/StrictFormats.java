package uk.gov.hmcts.ctam.jo.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The strict text formats shared by request parsing and fixture validation (Principle III), so a value a fixture may
 * hold and the value a caller may send follow one definition: ASCII digits, a real {@code YYYY-MM-DD} date and a
 * canonical UUID.
 */
public final class StrictFormats {

    /**
     * A canonical 8-4-4-4-12 hexadecimal UUID, in either letter case.
     */
    public static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final Pattern UUID = Pattern.compile(UUID_PATTERN);

    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");

    private static final Pattern DATE = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);

    private StrictFormats() {
    }

    /**
     * Whether the text is one or more ASCII digits (no sign, space or other script's digits).
     */
    public static boolean isDigits(String text) {
        return text != null && DIGITS.matcher(text).matches();
    }

    /**
     * The date, when the text is a real calendar date written exactly as {@code YYYY-MM-DD}.
     */
    public static Optional<LocalDate> strictDate(String text) {
        if (text == null || !DATE.matcher(text).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text, STRICT_DATE));
        } catch (DateTimeException notARealDate) {
            return Optional.empty();
        }
    }

    /**
     * Whether the text is a canonical UUID, in either letter case.
     */
    public static boolean isUuid(String text) {
        return text != null && UUID.matcher(text).matches();
    }

    /**
     * Whether the text is a canonical UUID in lower case, the form ids are stored and written in.
     */
    public static boolean isLowerCaseUuid(String text) {
        return isUuid(text) && text.equals(text.toLowerCase(Locale.ROOT));
    }
}
