package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.emptyRegistry;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.registry;

class ReferenceDataTypeRegistryTest {

    private static final TypeProperties WIDGETS =
            new TypeProperties("widgets", List.of("widget"), "classpath:reference-data/widgets.json");

    private static final TypeProperties GADGETS =
            new TypeProperties("gadgets", List.of("gadget", "old_gadgets"), "classpath:reference-data/gadgets.json");

    private final ReferenceDataTypeRegistry registry = registry(WIDGETS, GADGETS);

    @Test
    void resolvesACanonicalName() {
        ReferenceDataType type = registry.resolve("widgets");

        assertThat(type.getName()).isEqualTo("widgets");
        assertThat(type.getAliases()).containsExactly("widget");
        assertThat(type.getFixture()).isEqualTo("classpath:reference-data/widgets.json");
    }

    @Test
    void resolvesAnAliasToTheSameType() {
        assertThat(registry.resolve("widget")).isSameAs(registry.resolve("widgets"));
        assertThat(registry.resolve("old_gadgets").getName()).isEqualTo("gadgets");
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "Widgets", "WIDGETS", "widgets ", " widgets", "widgets/", ""})
    void rejectsUnknownNamesAndCaseVariants(String attributeName) {
        assertThatThrownBy(() -> registry.resolve(attributeName))
                .isInstanceOf(UnsupportedReferenceDataTypeException.class)
                .hasMessage(UNSUPPORTED_ATTRIBUTE_NAME);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> registry.resolve(null)).isInstanceOf(UnsupportedReferenceDataTypeException.class);
    }

    @Test
    void listsCanonicalNamesFirstThenAliasesInConfigurationOrder() {
        assertThat(registry.supportedAttributeNames())
                .containsExactly("widgets", "gadgets", "widget", "gadget", "old_gadgets");
    }

    @Test
    void separatesCanonicalNamesFromDeprecatedAliases() {
        assertThat(registry.canonicalAttributeNames()).containsExactly("widgets", "gadgets");
        assertThat(registry.deprecatedAttributeNames()).containsExactly("widget", "gadget", "old_gadgets");
    }

    @Test
    void listsTypesInConfigurationOrder() {
        assertThat(registry.types()).extracting(ReferenceDataType::getName).containsExactly("widgets", "gadgets");
    }

    @Test
    void zeroTypesIsValid() {
        ReferenceDataTypeRegistry empty = emptyRegistry();

        assertThat(empty.types()).isEmpty();
        assertThat(empty.supportedAttributeNames()).isEmpty();
        assertThat(empty.canonicalAttributeNames()).isEmpty();
        assertThat(empty.deprecatedAttributeNames()).isEmpty();
        assertThatThrownBy(() -> empty.resolve("widgets")).isInstanceOf(UnsupportedReferenceDataTypeException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Widgets", "1widgets", "_widgets", "wid-gets", "wid gets", ""})
    void failsStartupOnAnInvalidName(String name) {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(new TypeProperties(name, List.of(), "classpath:x.json")))
                .withMessageContaining("must match");
    }

    @Test
    void failsStartupOnANullName() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(new TypeProperties(null, List.of(), "classpath:x.json")));
    }

    @Test
    void failsStartupOnAnInvalidAlias() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(new TypeProperties("widgets", List.of("Widget"), "classpath:x.json")))
                .withMessageContaining("alias of widgets");
    }

    @Test
    void failsStartupWhenATypeListsItsOwnNameAsAnAlias() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(new TypeProperties("widgets", List.of("widgets"), "classpath:x.json")))
                .withMessageContaining("own name");
    }

    @Test
    void failsStartupOnADuplicateAliasWithinAType() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(new TypeProperties("widgets", List.of("w", "w"), "classpath:x.json")))
                .withMessageContaining("duplicate alias");
    }

    /**
     * Names and aliases share one namespace: no string may be declared twice, whichever roles it plays.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "a duplicate type name,                       widgets, '',     widgets",
        "an alias clashing with another type's name,  gadgets, widgets, widgets",
        "two types sharing an alias,                  gadgets, widget,  widget"
    })
    void failsStartupWhenANameOrAliasIsDeclaredTwice(String rule, String secondType, String secondTypeAlias,
                                                     String clash) {
        List<String> aliases = secondTypeAlias.isEmpty() ? List.of() : List.of(secondTypeAlias);

        assertThatIllegalStateException()
                .isThrownBy(() -> registry(WIDGETS, new TypeProperties(secondType, aliases, "classpath:y.json")))
                .withMessage("Invalid reference-data configuration: '" + clash + "' is declared by both widgets and "
                             + secondType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void failsStartupWithoutAFixture(String fixture) {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(new TypeProperties("widgets", List.of(), fixture)))
                .withMessageContaining("fixture is required");
    }
}
