package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.CREATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.END_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.emptyRegistry;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.recordBuilder;

class FixtureReferenceDataRepositoryTest {

    private static final String FIXTURES = "classpath:reference-data/";

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
    void anEmptyArrayGivesAnEmptyList() {
        ReferenceDataTypeRegistry registry = registry("widgets", "empty.json");

        assertThat(repository(registry).findAll(registry.resolve("widgets"))).isEmpty();
    }

    @Test
    void zeroRegistryTypesLoadsNothing() {
        ReferenceDataTypeRegistry registry = emptyRegistry();
        ReferenceDataRepository repository = repository(registry);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.findAll(new ReferenceDataType("widgets", Set.of(), "x")));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            id-not-positive        | record id 0: id must be greater than 0
            id-duplicate           | record id 5: duplicate id
            name-blank             | record id 5: name is required
            name-missing           | record id 5: name is required
            name-untrimmed         | record id 5: name must be trimmed
            name-duplicate         | record id 6: duplicate name
            created-at-missing     | record id 5: created_at is required
            updated-at-missing     | record id 5: updated_at is required
            start-date-missing     | record id 5: start_date is required
            fractional-seconds     | record id 5: timestamps must be whole seconds
            updated-before-created | record id 5: updated_at must not be before created_at
            end-before-start       | record id 5: end_date must not be before start_date
            null-record            | null record
            """)
    void failsStartupOnAnInvalidRecord(String fixture, String expectedProblem) {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "invalid/" + fixture + ".json")))
                .withMessage("Invalid reference-data fixture " + FIXTURES + "invalid/" + fixture + ".json: "
                             + expectedProblem);
    }

    @ParameterizedTest
    @CsvSource({"unknown-property, colour", "not-an-array, ''", "malformed, ''"})
    void failsStartupOnUnparseableFixtures(String fixture, String expectedFragment) {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "invalid/" + fixture + ".json")))
                .withMessageStartingWith(
                        "Invalid reference-data fixture " + FIXTURES + "invalid/" + fixture + ".json: ")
                .withMessageContaining(expectedFragment);
    }

    @Test
    void failsStartupWhenTheFixtureDoesNotExist() {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(registry("widgets", "missing.json")))
                .withMessage("Invalid reference-data fixture " + FIXTURES + "missing.json: resource not found");
    }

    private static ReferenceDataTypeRegistry registry(String name, String fixture) {
        return ReferenceDataFixtures.registry(new TypeProperties(name, List.of(), FIXTURES + fixture));
    }

    private static FixtureReferenceDataRepository repository(ReferenceDataTypeRegistry registry) {
        return new FixtureReferenceDataRepository(registry, new DefaultResourceLoader(), JsonMapper.builder().build());
    }
}
