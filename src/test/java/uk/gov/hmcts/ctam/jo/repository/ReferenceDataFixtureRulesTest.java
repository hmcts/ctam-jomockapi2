package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every shipped fixture follows the generation rules (002 data-model.md § Fixture generation rules, 003
 * data-model.md § 3), so ids stay stable without the local extract: names in code-point order (repeats allowed),
 * {@code id = 10 × position}, every seventh record ended, and fixed timestamps and start dates.
 */
class ReferenceDataFixtureRulesTest {

    private static final Path MAIN_FIXTURES = Path.of("src/main/resources/reference-data");

    private static final String CREATED_AT = "2024-01-15T09:00:00Z";
    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String START_DATE = "2024-01-01";
    private static final String END_DATE = "2025-03-31";

    /**
     * A record the tests and Postman rely on; a null end date means the record is open.
     */
    record ReferencePoint(long id, String name, String endDate) {
    }

    /**
     * One shipped fixture with its expected size and reference points.
     */
    record Fixture(String file, int records, int distinctNames, List<ReferencePoint> referencePoints) {

        @Override
        public String toString() {
            return file;
        }
    }

    static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture("appointment_titles.json", 194, 194, List.of(
                        new ReferencePoint(10, "Acting Senior Coroner", null),
                        new ReferencePoint(70, "Area Coroner", END_DATE),
                        new ReferencePoint(1940, "Vice-President, Employment Tribunal (Scotland)", null))),
                new Fixture("base_locations.json", 1462, 1276, List.of(
                        new ReferencePoint(10, "Aberconwy", null),
                        new ReferencePoint(70, "Aldridge and Brownhills", END_DATE),
                        new ReferencePoint(10290, "Royal Courts of Justice – Office of the Judge Advocate General",
                                           END_DATE),
                        new ReferencePoint(14620, "Yorkshire & Humberside", null))));
    }

    @Test
    void everyShippedFixtureIsChecked() throws IOException {
        // Read from the file system, not the classpath, which could include other suites' fixtures.
        List<String> shipped;
        try (Stream<Path> paths = Files.list(MAIN_FIXTURES)) {
            shipped = paths.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".json")).toList();
        }

        assertThat(fixtures().map(Fixture::file)).containsExactlyInAnyOrderElementsOf(shipped);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void followsTheGenerationRules(Fixture fixture) throws IOException {
        JsonNode records = read(fixture.file());

        assertThat(records.size()).isEqualTo(fixture.records());
        Set<String> names = new HashSet<>();
        String previous = null;
        for (int position = 1; position <= records.size(); position++) {
            JsonNode record = records.get(position - 1);
            String name = record.get("name").asString();
            assertThat(record.get("id").asLong()).as("id at position %d", position).isEqualTo(10L * position);
            if (previous != null) {
                assertThat(previous.compareTo(name)).as("order at position %d", position).isLessThanOrEqualTo(0);
            }
            boolean ended = position % 7 == 0;
            assertThat(record.get("end_date").isNull()).as("end_date at position %d", position).isEqualTo(!ended);
            if (ended) {
                assertThat(record.get("end_date").asString()).isEqualTo(END_DATE);
            }
            assertThat(record.get("updated_at").asString()).as("updated_at at position %d", position)
                    .isEqualTo(ended ? ENDED_UPDATED_AT : OPEN_UPDATED_AT);
            assertThat(record.get("created_at").asString()).isEqualTo(CREATED_AT);
            assertThat(record.get("start_date").asString()).isEqualTo(START_DATE);
            names.add(name);
            previous = name;
        }
        assertThat(names).hasSize(fixture.distinctNames());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void referencePointsMatchTheDataModel(Fixture fixture) throws IOException {
        JsonNode records = read(fixture.file());

        for (ReferencePoint point : fixture.referencePoints()) {
            JsonNode record = records.get((int) (point.id() / 10) - 1);
            assertThat(record.get("id").asLong()).isEqualTo(point.id());
            assertThat(record.get("name").asString()).as("name of %d", point.id()).isEqualTo(point.name());
            if (point.endDate() == null) {
                assertThat(record.get("end_date").isNull()).as("end_date of %d", point.id()).isTrue();
            } else {
                assertThat(record.get("end_date").asString()).as("end_date of %d", point.id())
                        .isEqualTo(point.endDate());
            }
        }
    }

    private static JsonNode read(String file) throws IOException {
        try (InputStream in = Objects.requireNonNull(
                ReferenceDataFixtureRulesTest.class.getResourceAsStream("/reference-data/" + file), file)) {
            return JsonTrees.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
