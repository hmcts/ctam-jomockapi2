package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.entity.BooleanField;
import uk.gov.hmcts.ctam.jo.entity.DeclaredField;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.repository.ReferenceDataFixtureRulesTest.ReferencePoint.point;

/**
 * Every shipped fixture follows the generation rules (002 data-model.md § Fixture generation rules, 003
 * data-model.md § 3, 004 data-model.md § 3, 005 data-model.md § 3), so ids stay stable without the local extract:
 * names in code-point order (repeats allowed), {@code id = 10 × position}, every seventh record ended, fixed
 * timestamps and start dates, and the reference and boolean fields the type declares in {@code application.yml}, in
 * the documented field order.
 */
class ReferenceDataFixtureRulesTest {

    private static final Path MAIN_FIXTURES = Path.of("src/main/resources/reference-data");

    private static final String CREATED_AT = "2024-01-15T09:00:00Z";
    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String START_DATE = "2024-01-01";
    private static final String END_DATE = "2025-03-31";

    private static final String FIXTURE_PREFIX = "classpath:reference-data/";

    private static final Path MAIN_CONFIGURATION = Path.of("src/main/resources/application.yml");

    /**
     * The E-Links jurisdictions: 30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners.
     */
    private static final Set<Long> JURISDICTION_IDS = Set.of(30L, 34L, 84L, 86L);

    /**
     * A record the tests and Postman rely on; a null end date means the record is open. References and booleans hold
     * the values of the type's declared reference and boolean fields, in declaration order, and are empty when the
     * type declares none; a point must list every declared field's value.
     */
    record ReferencePoint(long id, String name, String endDate, List<Long> references, List<Boolean> booleans) {

        static ReferencePoint point(long id, String name, String endDate) {
            return new ReferencePoint(id, name, endDate, List.of(), List.of());
        }

        ReferencePoint withReferences(Long... values) {
            return new ReferencePoint(id, name, endDate, List.of(values), booleans);
        }

        ReferencePoint withBooleans(Boolean... values) {
            return new ReferencePoint(id, name, endDate, references, List.of(values));
        }
    }

    /**
     * One shipped fixture with its expected size, how many records are {@code true} for each declared boolean field,
     * and its reference points.
     */
    record Fixture(String file, int records, int distinctNames, Map<String, Integer> trueCounts,
                   List<ReferencePoint> referencePoints) {

        /**
         * How the shipped configuration declares the fixture's type, so this test can't drift from it.
         */
        Declaration declaration() {
            Declaration declaration = declarations().get(file);
            if (declaration == null) {
                throw new AssertionError("no type in application.yml uses the fixture " + FIXTURE_PREFIX + file);
            }
            return declaration;
        }

        @Override
        public String toString() {
            return file;
        }
    }

    /**
     * A type's declared reference and boolean fields, each in response field order, and whether its names must be
     * unique.
     */
    record Declaration(List<String> referenceFields, List<String> booleanFields, boolean uniqueNames) {
    }

    private static Map<String, Declaration> declarations;

    static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture("appointment_titles.json", 194, 194, Map.of(), List.of(
                        point(10, "Acting Senior Coroner", null),
                        point(70, "Area Coroner", END_DATE),
                        point(1940, "Vice-President, Employment Tribunal (Scotland)", null))),
                new Fixture("base_locations.json", 1462, 1276, Map.of(), List.of(
                        point(10, "Aberconwy", null).withReferences(8L, 1722L, 84L),
                        point(70, "Aldridge and Brownhills", END_DATE).withReferences(8L, 1749L, 84L),
                        point(540, "Bedfordshire LJA", null).withReferences(8L, 1791L, 84L),
                        point(550, "Bedfordshire LJA", null).withReferences(8L, 1475L, 84L),
                        point(10290, "Royal Courts of Justice – Office of the Judge Advocate General", END_DATE)
                                .withReferences(2L, 1482L, 30L),
                        point(14620, "Yorkshire & Humberside", null).withReferences(5L, 1031L, 34L))),
                // 004 FR-008: "Salaried" and the ten SPTW percentages are salaried; the other three are not.
                new Fixture("contract_types.json", 14, 14, Map.of("salaried", 11), List.of(
                        point(10, "Fee-paid", null).withBooleans(false),
                        point(20, "SPTW-10%", null).withBooleans(true),
                        point(70, "SPTW-60%", END_DATE).withBooleans(true),
                        point(120, "Salaried", null).withBooleans(true),
                        point(130, "Unknown", null).withBooleans(false),
                        point(140, "Voluntary", END_DATE).withBooleans(false))),
                new Fixture("genders.json", 4, 4, Map.of(), List.of(
                        point(10, "Female", null),
                        point(20, "Male", null),
                        point(30, "Other", null),
                        point(40, "Prefer not to say", null))));
    }

    @Test
    void everyShippedFixtureIsChecked() throws IOException {
        // Read from the file system, not the classpath, which could include other suites' fixtures.
        List<String> shipped;
        try (Stream<Path> paths = Files.list(MAIN_FIXTURES)) {
            shipped = paths.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".json")).toList();
        }

        assertThat(fixtures().map(Fixture::file)).containsExactlyInAnyOrderElementsOf(shipped);
        assertThat(declarations().keySet()).containsExactlyInAnyOrderElementsOf(shipped);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void followsTheGenerationRules(Fixture fixture) throws IOException {
        JsonNode records = read(fixture.file());
        Declaration declaration = fixture.declaration();
        final List<String> referenceFields = declaration.referenceFields();
        final List<String> booleanFields = declaration.booleanFields();

        assertThat(records.size()).isEqualTo(fixture.records());
        if (declaration.uniqueNames()) {
            assertThat(fixture.distinctNames()).as("distinct names of a type with unique names")
                    .isEqualTo(fixture.records());
        }
        Set<String> names = new HashSet<>();
        Map<String, Integer> trueCounts = new HashMap<>();
        booleanFields.forEach(field -> trueCounts.put(field, 0));
        String previous = null;
        for (int position = 1; position <= records.size(); position++) {
            JsonNode record = records.get(position - 1);
            String name = record.get("name").asString();
            assertThat(record.get("id").asLong()).as("id at position %d", position).isEqualTo(10L * position);
            if (previous != null) {
                assertThat(compareCodePoints(previous, name)).as("order at position %d", position)
                        .isLessThanOrEqualTo(0);
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
            assertThat(record.propertyNames()).as("fields at position %d", position)
                    .containsExactlyElementsOf(fieldOrder(referenceFields, booleanFields));
            for (String field : referenceFields) {
                assertThat(record.get(field).isIntegralNumber()).as("%s at position %d", field, position).isTrue();
                assertThat(record.get(field).asLong()).as("%s at position %d", field, position).isPositive();
            }
            for (String field : booleanFields) {
                assertThat(record.get(field).isBoolean()).as("%s at position %d", field, position).isTrue();
                if (record.get(field).asBoolean()) {
                    trueCounts.merge(field, 1, Integer::sum);
                }
            }
            if (referenceFields.contains(ReferenceField.JURISDICTION_ID.fieldName())) {
                assertThat(JURISDICTION_IDS).as("jurisdiction_id at position %d", position)
                        .contains(record.get("jurisdiction_id").asLong());
            }
            names.add(name);
            previous = name;
        }
        assertThat(names).hasSize(fixture.distinctNames());
        assertThat(trueCounts).as("true records per boolean field").isEqualTo(fixture.trueCounts());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void referencePointsMatchTheDataModel(Fixture fixture) throws IOException {
        JsonNode records = read(fixture.file());
        List<String> referenceFields = fixture.declaration().referenceFields();
        List<String> booleanFields = fixture.declaration().booleanFields();

        for (ReferencePoint point : fixture.referencePoints()) {
            JsonNode record = recordWithId(records, point.id());
            assertThat(record.get("name").asString()).as("name of %d", point.id()).isEqualTo(point.name());
            if (point.endDate() == null) {
                assertThat(record.get("end_date").isNull()).as("end_date of %d", point.id()).isTrue();
            } else {
                assertThat(record.get("end_date").asString()).as("end_date of %d", point.id())
                        .isEqualTo(point.endDate());
            }
            // Always compared, so a point that leaves out a declared field's value fails instead of being skipped.
            List<Long> references = referenceFields.stream().map(field -> record.get(field).asLong()).toList();
            assertThat(point.references()).as("%s of %d", referenceFields, point.id()).isEqualTo(references);
            List<Boolean> booleans = booleanFields.stream().map(field -> record.get(field).asBoolean()).toList();
            assertThat(point.booleans()).as("%s of %d", booleanFields, point.id()).isEqualTo(booleans);
        }
    }

    /**
     * 002's layout, with the reference fields and then the boolean fields after {@code name} when the type declares
     * them.
     */
    private static List<String> fieldOrder(List<String> referenceFields, List<String> booleanFields) {
        List<String> fields = new ArrayList<>(List.of("id", "name"));
        fields.addAll(referenceFields);
        fields.addAll(booleanFields);
        fields.addAll(List.of("created_at", "updated_at", "start_date", "end_date"));
        return fields;
    }

    private static JsonNode recordWithId(JsonNode records, long id) {
        for (JsonNode record : records) {
            if (record.get("id").asLong() == id) {
                return record;
            }
        }
        throw new AssertionError("no record with id " + id);
    }

    /**
     * Binds the shipped {@code jo.reference-data} configuration as the application does, once, and maps each
     * fixture file to its type's declaration. Not a static initialiser, so a bad configuration fails the tests
     * that use it with a clear message instead of failing class initialisation.
     */
    private static synchronized Map<String, Declaration> declarations() {
        if (declarations == null) {
            declarations = bindDeclarations();
        }
        return declarations;
    }

    private static Map<String, Declaration> bindDeclarations() {
        ReferenceDataProperties properties;
        try {
            // Read from the file system, not the classpath, where test resources come first.
            properties = new Binder(ConfigurationPropertySources.from(
                    new YamlPropertySourceLoader().load("application.yml", new FileSystemResource(MAIN_CONFIGURATION))))
                    .bind("jo.reference-data", ReferenceDataProperties.class)
                    .orElseThrow(() -> new AssertionError("jo.reference-data is not configured"));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + MAIN_CONFIGURATION, e);
        }
        Map<String, Declaration> byFixture = new HashMap<>();
        for (TypeProperties type : properties.types()) {
            if (!type.fixture().startsWith(FIXTURE_PREFIX)) {
                throw new AssertionError("type " + type.name() + " has a fixture outside " + FIXTURE_PREFIX + ": "
                                         + type.fixture());
            }
            String file = type.fixture().substring(FIXTURE_PREFIX.length());
            if (byFixture.put(file, new Declaration(
                    inResponseOrder(ReferenceField.values(), type.referenceFields()),
                    inResponseOrder(BooleanField.values(), type.booleanFields()), type.uniqueNames())) != null) {
                throw new AssertionError("more than one type uses the fixture " + type.fixture());
            }
        }
        return Map.copyOf(byFixture);
    }

    /**
     * The declared field names, in the order the response writes them (the enum's order).
     */
    private static List<String> inResponseOrder(DeclaredField<?>[] fields, List<String> declared) {
        return Arrays.stream(fields).map(DeclaredField::fieldName).filter(declared::contains).toList();
    }

    /**
     * Code-point order, which differs from {@link String#compareTo} (UTF-16 units) for characters outside the
     * Basic Multilingual Plane.
     */
    private static int compareCodePoints(String first, String second) {
        int[] a = first.codePoints().toArray();
        int[] b = second.codePoints().toArray();
        return Arrays.compare(a, b);
    }

    /**
     * Read from the file system, not the classpath, where a test resource of the same name would come first.
     */
    private static JsonNode read(String file) throws IOException {
        return JsonTrees.parse(Files.readString(MAIN_FIXTURES.resolve(file), StandardCharsets.UTF_8));
    }
}
