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
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every reference between served types resolves in the shipped fixtures (constitution Principle IV; 007 FR-012), so
 * the check needs no running service. A reference to a type not served yet is a forward reference, which the feature
 * adding it records as a deferred item; the completeness test fails until the feature that serves the target type
 * adds its row here. Fields that a record and the record it refers to both hold agree, so the served data is
 * internally consistent (constitution Principle IV; 008 FR-012).
 */
class ReferenceDataReferencesTest {

    private static final Path MAIN_FIXTURES = Path.of("src/main/resources/reference-data");

    private static final Path MAIN_CONFIGURATION = Path.of("src/main/resources/application.yml");

    /**
     * The type each reference field points at, in E-Links' numbering.
     */
    private static final Map<String, String> TARGET_TYPES = Map.of(
            "type_id", "location_types",
            "parent_id", "locations",
            "jurisdiction_id", "jurisdictions");

    /**
     * One reference between served types: every non-null {@code field} in {@code source} is an id in {@code target}.
     */
    record Reference(String source, String field, String target) {

        boolean selfReferencing() {
            return source.equals(target);
        }

        @Override
        public String toString() {
            return source + "." + field + " -> " + target;
        }
    }

    static Stream<Reference> references() {
        return Stream.of(
                new Reference("locations", "parent_id", "locations"),
                // Closes 003 DF-1 for parent_id.
                new Reference("base_locations", "parent_id", "locations"),
                // Close 007 DF-5 and 003 DF-1 for type_id.
                new Reference("locations", "type_id", "location_types"),
                new Reference("base_locations", "type_id", "location_types"),
                // Close 003 DF-1, 007 DF-5 and 008 DF-7 for jurisdiction_id.
                new Reference("locations", "jurisdiction_id", "jurisdictions"),
                new Reference("base_locations", "jurisdiction_id", "jurisdictions"),
                new Reference("location_types", "jurisdiction_id", "jurisdictions"));
    }

    /**
     * Every {@code source} record's {@code field} equals the {@code field} of the {@code target} record its
     * {@code via} reference names.
     */
    record Agreement(String source, String via, String target, String field) {

        @Override
        public String toString() {
            return source + "." + field + " = " + target + "." + field + " via " + via;
        }
    }

    static Stream<Agreement> agreements() {
        return Stream.of(
                new Agreement("locations", "type_id", "location_types", "jurisdiction_id"),
                new Agreement("base_locations", "type_id", "location_types", "jurisdiction_id"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("references")
    void everyReferenceResolves(Reference reference) throws IOException {
        Set<Long> targetIds = new HashSet<>();
        read(reference.target()).forEach(record -> targetIds.add(record.get("id").asLong()));

        for (JsonNode record : read(reference.source())) {
            JsonNode value = record.get(reference.field());
            if (value != null && !value.isNull()) {
                assertThat(targetIds).as("%s record %d: %s %d", fixture(reference.source()), record.get("id").asLong(),
                                         reference.field(), value.asLong())
                        .contains(value.asLong());
            }
        }
    }

    /**
     * No record is its own parent, and following parents from any record reaches a record without one, so the
     * hierarchy has no cycles.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("references")
    void selfReferencesHaveNoCycles(Reference reference) throws IOException {
        if (!reference.selfReferencing()) {
            return;
        }
        Map<Long, Long> parents = new HashMap<>();
        for (JsonNode record : read(reference.source())) {
            JsonNode value = record.get(reference.field());
            assertThat(value).as("%s record %d: %s", fixture(reference.source()), record.get("id").asLong(),
                                 reference.field())
                    .isNotNull();
            parents.put(record.get("id").asLong(), value.isNull() ? null : value.asLong());
        }

        parents.forEach((id, parent) -> {
            assertThat(parent).as("%s of %d", reference.field(), id).isNotEqualTo(id);
            Long current = id;
            int steps = 0;
            while (current != null && steps <= parents.size()) {
                current = parents.get(current);
                steps++;
            }
            assertThat(current).as("chain of %s from %d", reference.field(), id).isNull();
        });
    }

    /**
     * Every reference field a served type declares whose target type is also served is listed in
     * {@link #references()}.
     */
    @Test
    void everyReferenceBetweenServedTypesIsChecked() throws IOException {
        List<TypeProperties> types = new Binder(ConfigurationPropertySources.from(
                new YamlPropertySourceLoader().load("application.yml", new FileSystemResource(MAIN_CONFIGURATION))))
                .bind("jo.reference-data", ReferenceDataProperties.class)
                .orElseThrow(() -> new AssertionError("jo.reference-data is not configured"))
                .types();
        Set<String> served = new HashSet<>();
        types.forEach(type -> served.add(type.name()));

        List<Reference> required = new ArrayList<>();
        for (TypeProperties type : types) {
            for (String field : type.referenceFields()) {
                String target = TARGET_TYPES.get(field);
                assertThat(target).as("target type of %s", field).isNotNull();
                if (served.contains(target)) {
                    required.add(new Reference(type.name(), field, target));
                }
            }
        }

        assertThat(references()).containsExactlyInAnyOrderElementsOf(required);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("agreements")
    void everyAgreementHolds(Agreement agreement) throws IOException {
        Map<Long, JsonNode> targets = new HashMap<>();
        read(agreement.target()).forEach(record -> targets.put(record.get("id").asLong(), record));

        for (JsonNode record : read(agreement.source())) {
            String source = fixture(agreement.source()) + " record " + record.get("id").asLong();
            long targetId = integral(record, agreement.via(), source);
            JsonNode target = targets.get(targetId);
            assertThat(target).as("%s: %s %d", source, agreement.via(), targetId).isNotNull();
            assertThat(integral(record, agreement.field(), source))
                    .as("%s: %s, against %s record %d", source, agreement.field(), fixture(agreement.target()),
                        targetId)
                    .isEqualTo(integral(target, agreement.field(),
                                        fixture(agreement.target()) + " record " + targetId));
        }
    }

    /**
     * The field's value, failing with a message if it is missing or not an integer, so an absent value can't be read
     * as 0 and pass.
     */
    private static long integral(JsonNode record, String field, String description) {
        JsonNode value = record.get(field);
        assertThat(value != null && value.isIntegralNumber()).as("%s: %s is an integer", description, field).isTrue();
        return value.asLong();
    }

    private static String fixture(String type) {
        return type + ".json";
    }

    /**
     * Read from the file system, not the classpath, where a test resource of the same name would come first.
     */
    private static JsonNode read(String type) throws IOException {
        return JsonTrees.parse(Files.readString(MAIN_FIXTURES.resolve(fixture(type)), StandardCharsets.UTF_8));
    }
}
