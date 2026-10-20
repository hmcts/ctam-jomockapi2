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
                "widgets", List.of(), FIXTURES + "valid-repeated-names.json", List.of(), List.of(), false));
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
                null));

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
                new TypeProperties("widgets", List.of(), path, ALL_REFERENCE_FIELDS, List.of(), null));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path + ": " + expectedProblem);
    }

    @Test
    void failsStartupWhenARecordCarriesAReferenceFieldItsTypeDoesNotDeclare() {
        // Valid for a type that declares every reference field; this one declares only type_id.
        String path = FIXTURES + "valid-reference-fields.json";
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(
                new TypeProperties("widgets", List.of(), path, List.of("type_id"), List.of(), null));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry))
                .withMessage("Invalid reference-data fixture " + path
                             + ": record id 20: parent_id is not a reference field of widgets");
    }

    @Test
    void loadsTheBooleanFieldsTheTypeDeclares() {
        ReferenceDataTypeRegistry registry = ReferenceDataFixtures.registry(new TypeProperties(
                "widgets", List.of(), FIXTURES + "valid-boolean-fields.json", List.of(), BOOLEAN_FIELDS, null));

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
                new TypeProperties("widgets", List.of(), path, List.of(), BOOLEAN_FIELDS, null));

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
                new TypeProperties("widgets", List.of(), path, List.of(), BOOLEAN_FIELDS, null));

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
                        new ReferenceDataType("widgets", Set.of(), "x", Set.of(), Set.of(), true)));
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
                "reference-field-explicit-null, parent_id", "name-number, name", "name-boolean, name"})
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

    private static FixtureReferenceDataRepository repository(ReferenceDataTypeRegistry registry) {
        return new FixtureReferenceDataRepository(registry, new DefaultResourceLoader(), JsonMapper.builder().build());
    }
}
