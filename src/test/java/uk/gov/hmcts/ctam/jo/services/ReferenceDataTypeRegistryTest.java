package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.entity.BooleanField;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.emptyRegistry;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.registry;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.type;

class ReferenceDataTypeRegistryTest {

    private static final TypeProperties WIDGETS =
            type("widgets", List.of("widget"), "classpath:reference-data/widgets.json");

    private static final TypeProperties GADGETS =
            type("gadgets", List.of("gadget", "old_gadgets"), "classpath:reference-data/gadgets.json");

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
                .isThrownBy(() -> registry(type(name, List.of(), "classpath:x.json")))
                .withMessageContaining("must match");
    }

    @Test
    void failsStartupOnANullName() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(type(null, List.of(), "classpath:x.json")));
    }

    @Test
    void failsStartupOnAnInvalidAlias() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(type("widgets", List.of("Widget"), "classpath:x.json")))
                .withMessageContaining("alias of widgets");
    }

    @Test
    void failsStartupWhenATypeListsItsOwnNameAsAnAlias() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(type("widgets", List.of("widgets"), "classpath:x.json")))
                .withMessageContaining("own name");
    }

    @Test
    void failsStartupOnADuplicateAliasWithinAType() {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(type("widgets", List.of("w", "w"), "classpath:x.json")))
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
                .isThrownBy(() -> registry(WIDGETS, type(secondType, aliases, "classpath:y.json")))
                .withMessage("Invalid reference-data configuration: '" + clash + "' is declared by both widgets and "
                             + secondType);
    }

    @Test
    void typesDeclareNoReferenceFieldsByDefault() {
        assertThat(registry.resolve("widgets").getReferenceFields()).isEmpty();
    }

    @Test
    void typesRequireUniqueNamesByDefault() {
        assertThat(registry.resolve("widgets").isUniqueNames()).isTrue();
    }

    @Test
    void typesCanRequireUniqueNamesExplicitly() {
        ReferenceDataTypeRegistry unique = registry(
                new TypeProperties("widgets", List.of(), "classpath:x.json", List.of(), List.of(), true,
                                   List.of()));

        assertThat(unique.resolve("widgets").isUniqueNames()).isTrue();
    }

    @Test
    void typesCanAllowRepeatedNames() {
        ReferenceDataTypeRegistry repeating = registry(
                new TypeProperties("widgets", List.of(), "classpath:x.json", List.of(), List.of(), false,
                                   List.of()));

        assertThat(repeating.resolve("widgets").isUniqueNames()).isFalse();
    }

    @Test
    void resolvesTheDeclaredReferenceFields() {
        ReferenceDataTypeRegistry declaring = registry(new TypeProperties("widgets", List.of(), "classpath:x.json",
                List.of("jurisdiction_id", "type_id", "parent_id"), List.of(), null, List.of()));

        assertThat(declaring.resolve("widgets").getReferenceFields())
                .containsExactly(ReferenceField.TYPE_ID, ReferenceField.PARENT_ID, ReferenceField.JURISDICTION_ID);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            an unknown field    | colour_id | type_id   | unknown reference field 'colour_id'
            a repeated field    | type_id   | type_id   | duplicate reference field type_id
            a field name's case | type_id   | Parent_Id | unknown reference field 'Parent_Id'
            """)
    void failsStartupOnAnInvalidReferenceField(String rule, String first, String second, String expected) {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(
                        new TypeProperties("widgets", List.of(), "classpath:x.json", List.of(first, second), List.of(),
                                           null, List.of())))
                .withMessage("Invalid reference-data type widgets: " + expected);
    }

    @Test
    void failsStartupOnAnEmptyListEntry() {
        List<String> withEmptyEntry = Arrays.asList("type_id", null);

        assertThatIllegalStateException()
                .isThrownBy(() -> registry(
                        new TypeProperties("widgets", List.of(), "classpath:x.json", withEmptyEntry, List.of(), null,
                                           List.of())))
                .withMessage("Invalid reference-data type widgets: empty entry in reference-fields");
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(
                        new TypeProperties("widgets", withEmptyEntry, "classpath:x.json", List.of(), List.of(), null,
                                           List.of())))
                .withMessage("Invalid reference-data type widgets: empty entry in aliases");
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(
                        new TypeProperties("widgets", List.of(), "classpath:x.json", List.of(), withEmptyEntry, null,
                                           List.of())))
                .withMessage("Invalid reference-data type widgets: empty entry in boolean-fields");
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(nullable(List.of("parent_id"), withEmptyEntry)))
                .withMessage("Invalid reference-data type widgets: empty entry in nullable-reference-fields");
    }

    @Test
    void typesDeclareNoNullableReferenceFieldsByDefault() {
        ReferenceDataTypeRegistry declaring = registry(new TypeProperties("widgets", List.of(), "classpath:x.json",
                List.of("parent_id"), List.of(), null, null));

        assertThat(declaring.resolve("widgets").getNullableReferenceFields()).isEmpty();
        assertThat(registry.resolve("widgets").getNullableReferenceFields()).isEmpty();
    }

    @Test
    void resolvesTheDeclaredNullableReferenceFields() {
        ReferenceDataTypeRegistry declaring =
                registry(nullable(List.of("type_id", "parent_id", "jurisdiction_id"), List.of("parent_id")));

        assertThat(declaring.resolve("widgets").getNullableReferenceFields())
                .isEqualTo(EnumSet.of(ReferenceField.PARENT_ID));
        assertThat(declaring.resolve("widgets").getReferenceFields())
                .containsExactly(ReferenceField.TYPE_ID, ReferenceField.PARENT_ID, ReferenceField.JURISDICTION_ID);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            a field not in reference-fields | type_id   | parent_id            | \
            nullable reference field parent_id is not a reference field
            an unknown field                | parent_id | colour               | \
            unknown nullable reference field 'colour'
            a repeated field                | parent_id | parent_id, parent_id | \
            duplicate nullable reference field parent_id
            """)
    void failsStartupOnAnInvalidNullableReferenceField(String rule, String referenceFields, String nullableFields,
                                                       String expected) {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(nullable(List.of(referenceFields), List.of(nullableFields.split(", ")))))
                .withMessage("Invalid reference-data type widgets: " + expected);
    }

    @Test
    void resolvesTheDeclaredBooleanFields() {
        ReferenceDataTypeRegistry declaring = registry(new TypeProperties(
                "widgets", List.of(), "classpath:x.json", List.of(), List.of("salaried"), null, List.of()));

        assertThat(declaring.resolve("widgets").getBooleanFields()).containsExactly(BooleanField.SALARIED);
        assertThat(declaring.resolve("widgets").getReferenceFields()).isEmpty();
    }

    @Test
    void typesDeclareNoBooleanFieldsByDefault() {
        assertThat(registry.resolve("widgets").getBooleanFields()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            an unknown field    | salaried | paid     | unknown boolean field 'paid'
            a repeated field    | salaried | salaried | duplicate boolean field salaried
            a field name's case | salaried | Salaried | unknown boolean field 'Salaried'
            a reference field   | salaried | type_id  | unknown boolean field 'type_id'
            """)
    void failsStartupOnAnInvalidBooleanField(String rule, String first, String second, String expected) {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(
                        new TypeProperties("widgets", List.of(), "classpath:x.json", List.of(), List.of(first, second),
                                           null, List.of())))
                .withMessage("Invalid reference-data type widgets: " + expected);
    }

    private static TypeProperties nullable(List<String> referenceFields, List<String> nullableReferenceFields) {
        return new TypeProperties("widgets", List.of(), "classpath:x.json", referenceFields, List.of(), null,
                                  nullableReferenceFields);
    }

    @Test
    void failsStartupOnAnEmptyTypesEntry() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new ReferenceDataTypeRegistry(
                        new ReferenceDataProperties(Arrays.asList(WIDGETS, null))))
                .withMessage("Invalid reference-data configuration: empty entry in types");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void failsStartupWithoutAFixture(String fixture) {
        assertThatIllegalStateException()
                .isThrownBy(() -> registry(type("widgets", List.of(), fixture)))
                .withMessageContaining("fixture is required");
    }
}
