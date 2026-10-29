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
import uk.gov.hmcts.ctam.jo.entity.TextField;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.repository.ReferenceDataFixtureRulesTest.ReferencePoint.point;

/**
 * Every shipped fixture follows the generation rules (002 data-model.md § Fixture generation rules, 003 to 006
 * data-model.md § 3, 007 and 008 data-model.md § 4), so ids stay stable without the local extract: the fixture's id
 * rule ({@link IdRule}), every seventh record ended, fixed timestamps and start dates, the reference, text and boolean
 * fields the type declares in {@code application.yml}, in the documented field order (a reference field null only where
 * the type declares it nullable; a text field a non-empty, trimmed string in its known format), and non-ASCII
 * characters written literally.
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
     * The jurisdictions the shipped location data refers to: 30 Courts, 34 Tribunals, 84 Magistrates, 86 Coroners.
     * Skills (87) is served but unreferenced (009 EC-003).
     */
    private static final Set<Long> JURISDICTION_IDS = Set.of(30L, 34L, 84L, 86L);

    /**
     * The format each text field's values take in the shipped data, where one is known (008 EC-004).
     */
    private static final Map<String, Pattern> TEXT_FORMATS =
            Map.of("integration_code", Pattern.compile("[A-Z_]+"));

    /**
     * How a fixture's ids are assigned.
     */
    enum IdRule {

        /**
         * 002's rule: names in code-point order (repeats allowed) and {@code id = 10 × position}.
         */
        POSITIONAL,

        /**
         * 007 DR-1: the extract's own ids, kept unchanged, so they can only be checked to be positive and strictly
         * ascending; names are in no particular order.
         */
        SOURCE
    }

    /**
     * A record the tests and Postman rely on; a null end date means the record is open. References, texts and booleans
     * hold the values of the type's declared reference, text and boolean fields, in declaration order, and are empty
     * when the type declares none; a point must list every declared field's value, with null for a null reference.
     */
    record ReferencePoint(long id, String name, String endDate, List<Long> references, List<String> texts,
                          List<Boolean> booleans) {

        static ReferencePoint point(long id, String name, String endDate) {
            return new ReferencePoint(id, name, endDate, List.of(), List.of(), List.of());
        }

        ReferencePoint withReferences(Long... values) {
            // Arrays.asList, not List.of, so a top-level point can expect a null reference.
            return new ReferencePoint(id, name, endDate, Arrays.asList(values), texts, booleans);
        }

        ReferencePoint withTexts(String... values) {
            return new ReferencePoint(id, name, endDate, references, List.of(values), booleans);
        }

        ReferencePoint withBooleans(Boolean... values) {
            return new ReferencePoint(id, name, endDate, references, texts, List.of(values));
        }
    }

    /**
     * One shipped fixture with its expected size, how many records are {@code true} for each declared boolean field,
     * its id rule and its reference points.
     */
    record Fixture(String file, int records, int distinctNames, Map<String, Integer> trueCounts, IdRule ids,
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
     * A type's declared reference, boolean and text fields, each in response field order, whether its names must be
     * unique, and which reference fields may be null.
     */
    record Declaration(List<String> referenceFields, List<String> booleanFields, boolean uniqueNames,
                       List<String> nullableReferenceFields, List<String> textFields) {
    }

    private static Map<String, Declaration> declarations;

    static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture("appointment_titles.json", 194, 194, Map.of(), IdRule.POSITIONAL, List.of(
                        point(10, "Acting Senior Coroner", null),
                        point(70, "Area Coroner", END_DATE),
                        point(1940, "Vice-President, Employment Tribunal (Scotland)", null))),
                new Fixture("base_locations.json", 1462, 1276, Map.of(), IdRule.POSITIONAL, List.of(
                        point(10, "Aberconwy", null).withReferences(8L, 1722L, 84L),
                        point(70, "Aldridge and Brownhills", END_DATE).withReferences(8L, 1749L, 84L),
                        point(540, "Bedfordshire LJA", null).withReferences(8L, 1791L, 84L),
                        point(550, "Bedfordshire LJA", null).withReferences(8L, 1475L, 84L),
                        point(10290, "Royal Courts of Justice – Office of the Judge Advocate General", END_DATE)
                                .withReferences(2L, 1482L, 30L),
                        point(14620, "Yorkshire & Humberside", null).withReferences(5L, 1031L, 34L))),
                // 004 FR-008: "Salaried" and the ten SPTW percentages are salaried; the other three are not.
                new Fixture("contract_types.json", 14, 14, Map.of("salaried", 11), IdRule.POSITIONAL, List.of(
                        point(10, "Fee-paid", null).withBooleans(false),
                        point(20, "SPTW-10%", null).withBooleans(true),
                        point(70, "SPTW-60%", END_DATE).withBooleans(true),
                        point(120, "Salaried", null).withBooleans(true),
                        point(130, "Unknown", null).withBooleans(false),
                        point(140, "Voluntary", END_DATE).withBooleans(false))),
                new Fixture("genders.json", 4, 4, Map.of(), IdRule.POSITIONAL, List.of(
                        point(10, "Female", null),
                        point(20, "Male", null),
                        point(30, "Other", null),
                        point(40, "Prefer not to say", null))),
                // 006 data-model.md § 3: names exactly as in the extract, including ’, – and the truncated name.
                new Fixture("judiciary_roles.json", 164, 164, Map.of(), IdRule.POSITIONAL, List.of(
                        point(10, "Acting Designated Civil Judge", null),
                        point(70, "Acting Senior Costs Judge", END_DATE),
                        point(140, "Boundary Commission for England – Deputy Chair", END_DATE),
                        point(150, "Boundary Commission for Wales – Deputy Chair", null),
                        point(170, "Business & Property Courts Supervising", null),
                        point(340, "Course Director for COP (JC)", null),
                        point(960, "Judge in Charge of Jury List (King’s Bench)", null),
                        point(970, "Judge in Charge of Jury List (Queen's Bench)", null),
                        point(1070, "Judicial Commissioner Investigatory Powers Commiss", null),
                        point(1280, "Principle Judge - Land Registration", null),
                        point(1400, "Senior Family Liaison Judge", END_DATE),
                        point(1610, "Vice President of the Unified Tribunals", END_DATE),
                        point(1640, "Wales Training Course Director (JC)", null))),
                // 007 data-model.md § 4: the extract's ids; null parents for top-level locations; names exactly as in
                // the extract, including two spaces in "Cheshire  LJA" and the en dash.
                new Fixture("locations.json", 2000, 1523, Map.of(), IdRule.SOURCE, List.of(
                        point(1, "Aberconwy", null).withReferences(8L, 1722L, 84L),
                        point(9, "Ashby-de-la-Zouch and Market Bosworth", END_DATE).withReferences(8L, 1732L, 84L),
                        point(10, "Atherstone and Coleshill", null).withReferences(8L, 1751L, 84L),
                        point(1176, "Cheshire  LJA", null).withReferences(8L, 1697L, 84L),
                        point(1323, "Cheshire LJA", null).withReferences(8L, 1476L, 84L),
                        point(1466, "London", END_DATE).withReferences(6L, null, 84L),
                        point(1482, "London", null).withReferences(1L, null, 30L),
                        point(1722, "Old Gwynedd", null).withReferences(7L, 2141L, 84L),
                        point(2141, "Gwynedd", null).withReferences(6L, null, 84L),
                        point(2364, "Newcastle and North Tyneside", END_DATE).withReferences(16L, 1820L, 86L),
                        point(2398, "Royal Courts of Justice – Office of the Judge Advocate General", null)
                                .withReferences(2L, 1482L, 30L),
                        point(2433, "Kent and Medway", null).withReferences(16L, 1925L, 86L))),
                // 008: the extract's ids, 1 to 16; "Region" and "AC" repeat.
                new Fixture("location_types.json", 16, 13, Map.of(), IdRule.SOURCE, List.of(
                        point(1, "Region", null).withReferences(30L).withTexts("COURTS_REGION"),
                        point(6, "Region", null).withReferences(84L).withTexts("MAGS_REGION"),
                        point(7, "AC", END_DATE).withReferences(84L).withTexts("MAGS_AC"),
                        point(10, "Area or Parent AC", null).withReferences(84L).withTexts("MAGS_AREA_OR_PARENT_AC"),
                        point(11, "AC", null).withReferences(84L).withTexts("MAGS_AREA_AC"),
                        point(14, "Region", END_DATE).withReferences(86L).withTexts("CORONERS_REGION"),
                        point(16, "Geographic Area", null).withReferences(86L).withTexts("CORONERS_AREA"))),
                // 009 data-model.md § 3: the extract's ids, with gaps; none ended.
                new Fixture("jurisdictions.json", 5, 5, Map.of(), IdRule.SOURCE, List.of(
                        point(30, "Courts", null),
                        point(34, "Tribunals", null),
                        point(84, "Magistrates", null),
                        point(86, "Coroners", null),
                        point(87, "Skills", null))));
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
        final List<String> nullableFields = declaration.nullableReferenceFields();
        final List<String> textFields = declaration.textFields();

        assertThat(records.size()).isEqualTo(fixture.records());
        if (declaration.uniqueNames()) {
            assertThat(fixture.distinctNames()).as("distinct names of a type with unique names")
                    .isEqualTo(fixture.records());
        }
        Set<String> names = new HashSet<>();
        Map<String, Integer> trueCounts = new HashMap<>();
        booleanFields.forEach(field -> trueCounts.put(field, 0));
        String previous = null;
        long previousId = 0;
        for (int position = 1; position <= records.size(); position++) {
            JsonNode record = records.get(position - 1);
            String name = record.get("name").asString();
            long id = record.get("id").asLong();
            if (fixture.ids() == IdRule.POSITIONAL) {
                assertThat(id).as("id at position %d", position).isEqualTo(10L * position);
                if (previous != null) {
                    assertThat(compareCodePoints(previous, name)).as("order at position %d", position)
                            .isLessThanOrEqualTo(0);
                }
            } else {
                assertThat(record.get("id").isIntegralNumber()).as("id at position %d", position).isTrue();
                assertThat(id).as("id at position %d", position).isGreaterThan(previousId);
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
                    .containsExactlyElementsOf(fieldOrder(referenceFields, textFields, booleanFields));
            for (String field : referenceFields) {
                if (record.get(field).isNull() && nullableFields.contains(field)) {
                    continue;
                }
                assertThat(record.get(field).isIntegralNumber()).as("%s at position %d", field, position).isTrue();
                assertThat(record.get(field).asLong()).as("%s at position %d", field, position).isPositive();
            }
            for (String field : textFields) {
                assertThat(record.get(field).isString()).as("%s at position %d", field, position).isTrue();
                String text = record.get(field).asString();
                assertThat(text).as("%s at position %d", field, position).isNotBlank().isEqualTo(text.strip());
                if (TEXT_FORMATS.containsKey(field)) {
                    assertThat(text).as("%s at position %d", field, position).matches(TEXT_FORMATS.get(field));
                }
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
            previousId = id;
        }
        assertThat(names).hasSize(fixture.distinctNames());
        assertThat(trueCounts).as("true records per boolean field").isEqualTo(fixture.trueCounts());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void referencePointsMatchTheDataModel(Fixture fixture) throws IOException {
        JsonNode records = read(fixture.file());
        List<String> referenceFields = fixture.declaration().referenceFields();
        List<String> textFields = fixture.declaration().textFields();
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
            List<Long> references = referenceFields.stream()
                    .map(field -> record.get(field).isNull() ? null : record.get(field).asLong()).toList();
            assertThat(point.references()).as("%s of %d", referenceFields, point.id()).isEqualTo(references);
            List<String> texts = textFields.stream().map(field -> record.get(field).asString()).toList();
            assertThat(point.texts()).as("%s of %d", textFields, point.id()).isEqualTo(texts);
            List<Boolean> booleans = booleanFields.stream().map(field -> record.get(field).asBoolean()).toList();
            assertThat(point.booleans()).as("%s of %d", booleanFields, point.id()).isEqualTo(booleans);
        }
    }

    /**
     * Non-ASCII characters are written literally (006 FR-008), which the parsed records can't show: an escaped
     * character parses to the same name.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void writesNonAsciiCharactersLiterally(Fixture fixture) throws IOException {
        String text = Files.readString(MAIN_FIXTURES.resolve(fixture.file()), StandardCharsets.UTF_8);

        // A JSON escape: a backslash that is not itself escaped, then u and four hex digits. An escaped backslash
        // followed by a u, as in a path-like name, is not an escape and doesn't match.
        assertThat(text).as("%s has no escaped characters", fixture.file())
                .doesNotContainPattern("(?<!\\\\)(?:\\\\\\\\)*\\\\u[0-9A-Fa-f]{4}");
    }

    /**
     * 002's layout, with the reference fields, then the text fields, then the boolean fields after {@code name} when
     * the type declares them.
     */
    private static List<String> fieldOrder(List<String> referenceFields, List<String> textFields,
                                           List<String> booleanFields) {
        List<String> fields = new ArrayList<>(List.of("id", "name"));
        fields.addAll(referenceFields);
        fields.addAll(textFields);
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
                    inResponseOrder(BooleanField.values(), type.booleanFields()), type.uniqueNames(),
                    inResponseOrder(ReferenceField.values(), type.nullableReferenceFields()),
                    inResponseOrder(TextField.values(), type.textFields()))) != null) {
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
