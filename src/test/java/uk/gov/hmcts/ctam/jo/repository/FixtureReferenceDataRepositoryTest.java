package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;
import uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.CREATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.END_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.emptyRegistry;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.recordBuilder;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.type;

class FixtureReferenceDataRepositoryTest {

    private static final String FIXTURES = "classpath:reference-data/";

    private static final List<String> ALL_REFERENCE_FIELDS = List.of("type_id", "parent_id", "jurisdiction_id");

    private static final List<String> BOOLEAN_FIELDS = List.of("salaried");

    private static final List<String> TEXT_FIELDS = List.of("integration_code");

    @Test
    void loadsRecordsAndReturnsThemInAscendingIdOrder() {
        ReferenceDataTypeRegistry registry = registry("widgets", "valid-unsorted.json");
        ReferenceDataRepository repository = repository(registry);

        List<ReferenceDataRecord> records = repository.findAll(registry.resolve("widgets"));

        assertThat(records).extracting(ReferenceDataRecord::getId).containsExactly(10L, 20L, 30L);
        // valid-unsorted.json: Alpha was never updated after creation, and has ended.
        assertThat(records.getFirst()).isEqualTo(
                recordBuilder(10, "Alpha").updatedAt(CREATED_AT).endDate(END_DATE).build());
        assertThat(records.get(1).getEndDate()).isNull();
    }

    @Test
    void findAllIsUnmodifiable() {
        ReferenceDataTypeRegistry registry = registry("widgets", "valid-unsorted.json");
        List<ReferenceDataRecord> records = repository(registry).findAll(registry.resolve("widgets"));

        assertThatThrownBy(records::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findsARecordById() {
        ReferenceDataTypeRegistry registry = registry("widgets", "valid-unsorted.json");
        ReferenceDataRepository repository = repository(registry);
        ReferenceDataType widgets = registry.resolve("widgets");

        assertThat(repository.findById(widgets, 20)).map(ReferenceDataRecord::getName).contains("Bravo");
        assertThat(repository.findById(widgets, 15)).isEmpty();
        assertThat(repository.findById(widgets, Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void loadsRecordsThatShareANameWhenTheTypeAllowsIt() {
        // Different real locations can share a name, so such a type allows repeats; ids still may not repeat.
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(new TypeProperties(
                "widgets", List.of(), FIXTURES + "valid-repeated-names.json", List.of(), List.of(), false, List.of(),
                List.of()));
        ReferenceDataRepository repository = repository(registry);
        ReferenceDataType widgets = registry.resolve("widgets");

        assertThat(repository.findAll(widgets))
                .extracting(ReferenceDataRecord::getId, ReferenceDataRecord::getName)
                .containsExactly(tuple(5L, "A"), tuple(6L, "A"));
        assertThat(repository.findById(widgets, 5)).map(ReferenceDataRecord::getName).contains("A");
        assertThat(repository.findById(widgets, 6)).map(ReferenceDataRecord::getName).contains("A");
    }

    @Test
    void loadsTheReferenceFieldsTheTypeDeclares() {
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(new TypeProperties(
                "widgets", List.of(), FIXTURES + "valid-reference-fields.json", ALL_REFERENCE_FIELDS, List.of(),
                null, List.of(), List.of()));

        assertThat(repository(registry).findAll(registry.resolve("widgets")))
                .extracting(ReferenceDataRecord::getId, ReferenceDataRecord::getTypeId,
                            ReferenceDataRecord::getParentId, ReferenceDataRecord::getJurisdictionId)
                .containsExactly(tuple(10L, 8L, 1722L, 84L), tuple(20L, 2L, 1482L, 30L));
    }

    @Test
    void recordsOfATypeWithoutReferenceFieldsCarryNone() {
        ReferenceDataTypeRegistry registry = registry("widgets", "valid-unsorted.json");

        assertThat(repository(registry).findAll(registry.resolve("widgets"))).allSatisfy(record -> {
            assertThat(record.getTypeId()).isNull();
            assertThat(record.getParentId()).isNull();
            assertThat(record.getJurisdictionId()).isNull();
        });
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            reference-field-missing      | record id 5: parent_id is required
            reference-field-not-positive | record id 5: jurisdiction_id must be greater than 0
            """)
    void failsStartupWhenADeclaredReferenceFieldIsInvalid(String fixture, String expectedProblem) {
        String path = FIXTURES + "invalid/" + fixture + ".json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                new TypeProperties("widgets", List.of(), path, ALL_REFERENCE_FIELDS, List.of(), null, List.of(),
                                   List.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path + ": " + expectedProblem);
    }

    @Test
    void failsStartupWhenARecordCarriesAReferenceFieldItsTypeDoesNotDeclare() {
        // Valid for a type that declares every reference field; this one declares only type_id.
        String path = FIXTURES + "valid-reference-fields.json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                new TypeProperties("widgets", List.of(), path, List.of("type_id"), List.of(), null, List.of(),
                                   List.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path
                             + ": record id 20: parent_id is not a reference field of widgets");
    }

    @Test
    void loadsANullValueForANullableReferenceField() {
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                nullableParent(FIXTURES + "valid-nullable-reference-fields.json"));

        assertThat(repository(registry).findAll(registry.resolve("widgets")))
                .extracting(ReferenceDataRecord::getId, ReferenceDataRecord::getTypeId,
                            ReferenceDataRecord::getParentId, ReferenceDataRecord::getJurisdictionId)
                .containsExactly(tuple(10L, 6L, null, 84L), tuple(20L, 2L, 10L, 30L));
    }

    /**
     * A nullable reference field must still be present in every record, and a value it holds must be a positive id.
     * The string case fails while parsing, so only the field is checked.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            nullable-reference-field-missing      | record id 5: parent_id is required
            nullable-reference-field-not-positive | record id 5: parent_id must be greater than 0
            """)
    void failsStartupWhenANullableReferenceFieldIsInvalid(String fixture, String expectedProblem) {
        String path = FIXTURES + "invalid/" + fixture + ".json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(nullableParent(path));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path + ": " + expectedProblem);
    }

    @Test
    void failsStartupWhenANullableReferenceFieldIsAString() {
        String path = FIXTURES + "invalid/nullable-reference-field-string.json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(nullableParent(path));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessageStartingWith("Invalid reference-data fixture " + path + ": ")
                .withMessageContaining("parent_id");
    }

    @Test
    void failsStartupWhenADeclaredReferenceFieldThatIsNotNullableIsNull() {
        String path = FIXTURES + "invalid/non-nullable-reference-field-explicit-null.json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                new TypeProperties("widgets", List.of(), path, ALL_REFERENCE_FIELDS, List.of(), null, List.of(),
                                   List.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path + ": record id 5: parent_id is required");
    }

    @Test
    void loadsTheBooleanFieldsTheTypeDeclares() {
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(new TypeProperties(
                "widgets", List.of(), FIXTURES + "valid-boolean-fields.json", List.of(), BOOLEAN_FIELDS, null,
                List.of(), List.of()));

        assertThat(repository(registry).findAll(registry.resolve("widgets")))
                .extracting(ReferenceDataRecord::getId, ReferenceDataRecord::getSalaried)
                .containsExactly(tuple(10L, false), tuple(20L, true));
    }

    @Test
    void recordsOfATypeWithoutBooleanFieldsCarryNone() {
        ReferenceDataTypeRegistry registry = registry("widgets", "valid-unsorted.json");

        assertThat(repository(registry).findAll(registry.resolve("widgets")))
                .allSatisfy(record -> assertThat(record.getSalaried()).isNull());
    }

    @Test
    void failsStartupWhenADeclaredBooleanFieldIsMissing() {
        String path = FIXTURES + "invalid/boolean-field-missing.json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                new TypeProperties("widgets", List.of(), path, List.of(), BOOLEAN_FIELDS, null, List.of(), List.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path + ": record id 5: salaried is required");
    }

    // Only JSON true and false are booleans: no null, 0/1, "true" or "" (004 FR-018).
    @ParameterizedTest
    @ValueSource(strings = {"boolean-field-explicit-null", "boolean-field-number", "boolean-field-string",
        "boolean-field-empty-string"})
    void failsStartupWhenADeclaredBooleanFieldIsNotAJsonBoolean(String fixture) {
        String path = FIXTURES + "invalid/" + fixture + ".json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                new TypeProperties("widgets", List.of(), path, List.of(), BOOLEAN_FIELDS, null, List.of(), List.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessageStartingWith("Invalid reference-data fixture " + path + ": ")
                .withMessageContaining("salaried");
    }

    @Test
    void failsStartupWhenARecordCarriesABooleanFieldItsTypeDoesNotDeclare() {
        String path = FIXTURES + "valid-boolean-fields.json";

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "valid-boolean-fields.json")))
                .withMessage("Invalid reference-data fixture " + path
                             + ": record id 20: salaried is not a boolean field of widgets");
    }

    @Test
    void loadsTheTextFieldsTheTypeDeclares() {
        ReferenceDataTypeRegistry registry =
                ReferenceDataFixtures.registry(declaringText(FIXTURES + "valid-text-fields.json"));

        assertThat(repository(registry).findAll(registry.resolve("widgets")))
                .extracting(ReferenceDataRecord::getId, ReferenceDataRecord::getIntegrationCode)
                .containsExactly(tuple(10L, "WIDGET_A"), tuple(20L, "WIDGET_B"));
    }

    @Test
    void recordsOfATypeWithoutTextFieldsCarryNone() {
        ReferenceDataTypeRegistry registry = registry("widgets", "valid-unsorted.json");

        assertThat(repository(registry).findAll(registry.resolve("widgets")))
                .allSatisfy(record -> assertThat(record.getIntegrationCode()).isNull());
    }

    /**
     * A declared text field must be present in every record, as a non-empty string with no leading or trailing
     * whitespace, as for names (008 FR-019).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            text-field-missing   | record id 5: integration_code is required
            text-field-empty     | record id 5: integration_code must not be blank
            text-field-blank     | record id 5: integration_code must not be blank
            text-field-untrimmed | record id 5: integration_code must be trimmed
            """)
    void failsStartupWhenADeclaredTextFieldIsInvalid(String fixture, String expectedProblem) {
        String path = FIXTURES + "invalid/" + fixture + ".json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(declaringText(path));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path + ": " + expectedProblem);
    }

    // Only JSON strings are text: no null, number, boolean or object. These fail while the JSON is read, whether or
    // not the type declares the field; failsStartupOnUnparseableFixtures covers a type that doesn't (008 FR-019).
    @ParameterizedTest
    @ValueSource(strings = {"text-field-explicit-null", "text-field-number", "text-field-boolean", "text-field-object"})
    void failsStartupWhenADeclaredTextFieldIsNotAJsonString(String fixture) {
        String path = FIXTURES + "invalid/" + fixture + ".json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(declaringText(path));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessageStartingWith("Invalid reference-data fixture " + path + ": ")
                .withMessageContaining("integration_code")
                // Jackson names the record by its index in the array: the first record is [0].
                .withMessageContaining("[0]");
    }

    @Test
    void failsStartupWhenARecordCarriesATextFieldItsTypeDoesNotDeclare() {
        String path = FIXTURES + "valid-text-fields.json";

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "valid-text-fields.json")))
                .withMessage("Invalid reference-data fixture " + path
                             + ": record id 20: integration_code is not a text field of widgets");
    }

    @Test
    void anEmptyArrayGivesAnEmptyList() {
        ReferenceDataTypeRegistry registry = registry("widgets", "empty.json");

        assertThat(repository(registry).findAll(registry.resolve("widgets"))).isEmpty();
    }

    @Test
    void zeroRegistryTypesLoadsNothing() {
        ReferenceDataTypeRegistry registry = emptyRegistry();
        ReferenceDataRepository repository = repository(registry);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.findAll(
                        new ReferenceDataType("widgets", Set.of(), "x", Set.of(), Set.of(), true, Set.of(), null)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            id-missing                 | record at position 1: id is required
            created-at-number          | record at position 1: created_at must be an ISO-8601 string
            updated-at-fractional-number | record at position 1: updated_at must be an ISO-8601 string
            start-date-array           | record at position 1: start_date must be an ISO-8601 string
            end-date-number            | record at position 1: end_date must be an ISO-8601 string
            id-not-positive            | record id 0: id must be greater than 0
            id-duplicate               | record id 5: duplicate id
            name-blank                 | record id 5: name is required
            name-missing               | record id 5: name is required
            name-untrimmed             | record id 5: name must be trimmed
            created-at-missing         | record id 5: created_at is required
            updated-at-missing         | record id 5: updated_at is required
            start-date-missing         | record id 5: start_date is required
            fractional-seconds         | record id 5: timestamps must be whole seconds
            fractional-seconds-updated | record id 5: timestamps must be whole seconds
            updated-before-created     | record id 5: updated_at must not be before created_at
            end-before-start           | record id 5: end_date must not be before start_date
            null-record                | null record
            null-document              | must be a JSON array of records
            reference-field-explicit-null | record id 5: parent_id is not a reference field of widgets
            """)
    void failsStartupOnAnInvalidRecord(String fixture, String expectedProblem) {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "invalid/" + fixture + ".json")))
                .withMessage("Invalid reference-data fixture " + FIXTURES + "invalid/" + fixture + ".json: "
                             + expectedProblem);
    }

    @ParameterizedTest
    @CsvSource({"unknown-property, colour", "not-an-array, ''", "malformed, ''",
                "reference-field-fractional, type_id", "reference-field-string, jurisdiction_id",
                "reference-field-empty-string, parent_id", "id-string, id",
                "name-number, name", "name-boolean, name", "text-field-explicit-null, integration_code",
                "text-field-number, integration_code", "text-field-boolean, integration_code",
                "text-field-object, integration_code"})
    void failsStartupOnUnparseableFixtures(String fixture, String expectedFragment) {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "invalid/" + fixture + ".json")))
                .withMessageStartingWith(
                        "Invalid reference-data fixture " + FIXTURES + "invalid/" + fixture + ".json: ")
                .withMessageContaining(expectedFragment);
    }

    @Test
    void failsStartupOnARepeatedNameByDefault() {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "valid-repeated-names.json")))
                .withMessage("Invalid reference-data fixture " + FIXTURES
                             + "valid-repeated-names.json: record id 6: duplicate name");
    }

    @Test
    void failsStartupWhenTheFixtureDoesNotExist() {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "missing.json")))
                .withMessage("Invalid reference-data fixture " + FIXTURES + "missing.json: resource not found");
    }

    private static ReferenceDataTypeRegistry registry(String name, String fixture) {
        return ReferenceDataFixtures.registry(type(name, List.of(), FIXTURES + fixture));
    }

    /**
     * A type declaring every reference field, with {@code parent_id} nullable.
     */
    private static TypeProperties nullableParent(String fixture) {
        return new TypeProperties("widgets", List.of(), fixture, ALL_REFERENCE_FIELDS, List.of(), null,
                                  List.of("parent_id"), List.of());
    }

    /**
     * A type declaring the text field {@code integration_code} and nothing else.
     */
    private static TypeProperties declaringText(String fixture) {
        return new TypeProperties("widgets", List.of(), fixture, List.of(), List.of(), null, List.of(), TEXT_FIELDS);
    }

    private static FixtureReferenceDataRepository repository(ReferenceDataTypeRegistry registry) {
        return new FixtureReferenceDataRepository(registry, new DefaultResourceLoader(), JsonMapper.builder().build());
    }
}
