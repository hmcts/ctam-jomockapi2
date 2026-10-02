package uk.gov.hmcts.ctam.jo.util;

import org.owasp.encoder.Encode;

/**
 * Makes caller-supplied values safe to log: control characters, quotes and non-ASCII characters are
 * escaped, so a value can never forge or split a log line (Principle XIV).
 */
public final class LogSanitiser {

    private LogSanitiser() {
    }

    public static String sanitise(String value) {
        return value == null ? "null" : Encode.forJava(value);
    }
}
