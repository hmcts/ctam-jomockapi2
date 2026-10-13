package uk.gov.hmcts.ctam.jo.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every reference-data type is served by the same generic code (spec AC-013, FR-005): no main source file
 * names a particular type, and only the registry knows about deprecated aliases.
 */
class NoTypeSpecificCodeTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final String REGISTRY = "ReferenceDataTypeRegistry.java";

    private static List<Path> sources;

    @BeforeAll
    static void collectMainSources() throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCES)) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertThat(sources).as("main sources found under %s", MAIN_SOURCES.toAbsolutePath()).isNotEmpty();
    }

    // One fragment per configured type; each type's feature adds its own. Checks code, comments and Javadoc alike,
    // in any spelling: snake_case, spaced words or camelCase ("base_location", "base location", "BaseLocation").
    @ParameterizedTest
    @ValueSource(strings = {"appointment", "base_location"})
    void noMainSourceFileNamesASpecificReferenceDataType(String fragment) {
        Pattern spellings = spellingsOf(fragment);
        assertThat(sources)
                .filteredOn(path -> spellings.matcher(read(path)).find())
                .as("main sources mentioning %s", fragment)
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"base_location", "base_locations", "BASE_LOCATION", "base location", "Base Location",
        "base-location", "BaseLocation", "baseLocation", "BaseLocationHelper", "myBaseLocation"})
    void theGuardRecognisesEverySpelling(String text) {
        assertThat(spellingsOf("base_location").matcher(text).find()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"database location", "databaseLocation", "base", "location", "baseline location"})
    void theGuardIgnoresOtherWords(String text) {
        assertThat(spellingsOf("base_location").matcher(text).find()).isFalse();
    }

    /**
     * Matches the fragment's words, case-insensitively, joined by nothing, a space, an underscore or a hyphen, and
     * starting a word or a camelCase hump (so "database location" doesn't match "base_location").
     */
    private static Pattern spellingsOf(String fragment) {
        String words = Arrays.stream(fragment.split("_")).map(Pattern::quote).collect(Collectors.joining("[\\s_-]?"));
        return Pattern.compile("(?:(?<![A-Za-z])|(?<=[a-z])(?=[A-Z]))(?i:" + words + ")");
    }

    @Test
    void onlyTheRegistryResolvesAliases() {
        assertThat(sources)
                .filteredOn(path -> !path.getFileName().toString().equals(REGISTRY))
                .filteredOn(path -> read(path).contains("getAliases()") || read(path).contains(".aliases()"))
                .as("main sources other than %s that read aliases", REGISTRY)
                .isEmpty();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }
}
