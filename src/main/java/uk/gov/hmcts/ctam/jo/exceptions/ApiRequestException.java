package uk.gov.hmcts.ctam.jo.exceptions;

import uk.gov.hmcts.ctam.jo.util.LogSanitiser;

/**
 * Base for request errors whose message is a fixed contract string. The optional diagnostic detail is
 * sanitised on construction and is for logs only: it never reaches a response body.
 */
public abstract class ApiRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String diagnostic;

    protected ApiRequestException(String contractMessage, String diagnostic) {
        super(contractMessage);
        this.diagnostic = LogSanitiser.sanitise(diagnostic);
    }

    public String getDiagnostic() {
        return diagnostic;
    }
}
