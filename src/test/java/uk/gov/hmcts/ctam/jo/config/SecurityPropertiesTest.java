package uk.gov.hmcts.ctam.jo.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SecurityPropertiesTest {

    private static final String MESSAGE = "jo.security.bearer-tokens must contain at least one non-blank token";

    @Test
    void acceptsNonBlankTokens() {
        assertThat(new SecurityProperties(List.of("a", "b")).bearerTokens()).containsExactly("a", "b");
    }

    @Test
    void rejectsNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SecurityProperties(null)).withMessage(MESSAGE);
    }

    @Test
    void rejectsEmpty() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SecurityProperties(List.of())).withMessage(MESSAGE);
    }

    @Test
    void rejectsBlankOrNullEntries() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityProperties(List.of("a", " "))).withMessage(MESSAGE);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecurityProperties(Arrays.asList("a", null))).withMessage(MESSAGE);
    }

    @Test
    void copiesTheListSoLaterChangesHaveNoEffect() {
        List<String> tokens = new ArrayList<>(List.of("a"));
        SecurityProperties properties = new SecurityProperties(tokens);

        tokens.add("b");

        assertThat(properties.bearerTokens()).containsExactly("a");
    }
}
