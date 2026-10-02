package uk.gov.hmcts.ctam.jo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Accepted Bearer tokens ({@code jo.security.bearer-tokens}). The application refuses to start with an empty
 * or blank token set, so no configuration can serve protected routes without a token (FR-017).
 */
@Validated
@ConfigurationProperties("jo.security")
public record SecurityProperties(List<String> bearerTokens) {

    public SecurityProperties {
        if (bearerTokens == null || bearerTokens.isEmpty()
                || bearerTokens.stream().anyMatch(SecurityProperties::blank)) {
            throw new IllegalArgumentException("jo.security.bearer-tokens must contain at least one non-blank token");
        }
        bearerTokens = List.copyOf(bearerTokens);
    }

    private static boolean blank(String token) {
        return token == null || token.isBlank();
    }
}
