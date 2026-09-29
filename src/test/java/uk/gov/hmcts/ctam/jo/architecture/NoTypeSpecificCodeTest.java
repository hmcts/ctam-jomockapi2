package uk.gov.hmcts.ctam.jo.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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

    @Test
    void noMainSourceFileNamesTheAppointmentTitleType() {
        // Checks code, comments and Javadoc alike.
        assertThat(sources)
                .filteredOn(path -> read(path).toLowerCase(Locale.ROOT).contains("appointment"))
                .as("main sources mentioning a specific reference-data type")
                .isEmpty();
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
