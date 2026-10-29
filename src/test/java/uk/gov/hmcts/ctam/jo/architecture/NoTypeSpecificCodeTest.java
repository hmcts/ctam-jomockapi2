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
    // in any spelling: snake_case, spaced or wrapped words, camelCase or upper case ("base_location",
    // "base location", "BaseLocation", "XBASE_LOCATION"). Jurisdictions' and tickets' fragments are the plural, because
    // the generic reference fields jurisdiction_id (003) and ticket_category_id (010) contain the singular (009 and 010
    // Clarifications).
    @ParameterizedTest
    @ValueSource(strings = {"appointment", "base_location", "contract_type", "gender", "judiciary_role", "location",
        "jurisdictions", "tickets"})
    void noMainSourceFileNamesASpecificReferenceDataType(String fragment) {
        Pattern spellings = spellingsOf(fragment);
        assertThat(sources)
                .filteredOn(path -> spellings.matcher(read(path)).find())
                .as("main sources mentioning %s", fragment)
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"base_location", "base_locations", "BASE_LOCATION", "base location", "Base Location",
        "base-location", "BaseLocation", "baseLocation", "BaseLocationHelper", "myBaseLocation", "XBASE_LOCATION",
        "HMCTSBaseLocation", "base\n     * location"})
    void theGuardRecognisesEverySpelling(String text) {
        assertThat(spellingsOf("base_location").matcher(text).find()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"contract_type", "contract_types", "CONTRACT_TYPE", "contract type", "Contract Type",
        "contract-type", "ContractType", "contractType", "myContractType", "XCONTRACT_TYPE", "contract\n     * type"})
    void theGuardRecognisesEveryContractTypeSpelling(String text) {
        assertThat(spellingsOf("contract_type").matcher(text).find()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"appointment", "AppointmentTitle", "DEFAULTAPPOINTMENT_TITLES", "myAppointments"})
    void theGuardRecognisesASingleWordFragmentInEverySpelling(String text) {
        assertThat(spellingsOf("appointment").matcher(text).find()).isTrue();
    }

    // No exemption for gender_id or the plural: the People feature adds its own reviewed exception (005 DF-3).
    @ParameterizedTest
    @ValueSource(strings = {"gender", "genders", "GENDER", "Gender", "GenderHelper", "myGender", "XGENDER", "genderId",
        "gender_id"})
    void theGuardRecognisesEveryGenderSpelling(String text) {
        assertThat(spellingsOf("gender").matcher(text).find()).isTrue();
    }

    // No exemption for the plural, judiciary_role_id or judiciary_role_name_id: the People feature adds its own
    // reviewed exception (006 DF-4).
    @ParameterizedTest
    @ValueSource(strings = {"judiciary_role", "judiciary_roles", "JUDICIARY_ROLE", "judiciary role", "Judiciary Role",
        "judiciary-role", "JudiciaryRole", "judiciaryRole", "myJudiciaryRole", "XJUDICIARY_ROLE", "judiciary_role_id",
        "judiciaryRoleNameId", "judiciary\n     * role"})
    void theGuardRecognisesEveryJudiciaryRoleSpelling(String text) {
        assertThat(spellingsOf("judiciary_role").matcher(text).find()).isTrue();
    }

    // No exemption: the People feature adds its own reviewed exception for its location fields (007 DF-6).
    @ParameterizedTest
    @ValueSource(strings = {"location", "locations", "LOCATION", "Location", "LocationHelper", "myLocation",
        "XLOCATION", "location_id", "base_location", "baseLocation"})
    void theGuardRecognisesEveryLocationSpelling(String text) {
        assertThat(spellingsOf("location").matcher(text).find()).isTrue();
    }

    // Each contains "location" straight after a lower-case letter, so each exercises the boundary rule.
    @ParameterizedTest
    @ValueSource(strings = {"allocation", "relocation", "dislocation", "collocation"})
    void theGuardIgnoresWordsContainingLocation(String text) {
        assertThat(spellingsOf("location").matcher(text).find()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"database location", "databaseLocation", "base", "location", "baseline location"})
    void theGuardIgnoresOtherWords(String text) {
        assertThat(spellingsOf("base_location").matcher(text).find()).isFalse();
    }

    // Field names such as salaried, and words that merely start with "contract", are not type names.
    @ParameterizedTest
    @ValueSource(strings = {"contract", "type", "contractual type", "salaried", "BooleanField"})
    void theGuardIgnoresOtherWordsNearContractType(String text) {
        assertThat(spellingsOf("contract_type").matcher(text).find()).isFalse();
    }

    // Each contains "gender" straight after a lower-case letter, so each exercises the boundary rule.
    @ParameterizedTest
    @ValueSource(strings = {"engender", "engendered", "agender", "transgender"})
    void theGuardIgnoresWordsContainingGender(String text) {
        assertThat(spellingsOf("gender").matcher(text).find()).isFalse();
    }

    // The last has the fragment straight after a lower-case letter, so it exercises the boundary rule.
    @ParameterizedTest
    @ValueSource(strings = {"judiciary", "role", "judicial role", "JudicialRole", "judiciary member role",
        "nonjudiciary role"})
    void theGuardIgnoresOtherWordsNearJudiciaryRole(String text) {
        assertThat(spellingsOf("judiciary_role").matcher(text).find()).isFalse();
    }

    // No exemption: the plural matches no main source.
    @ParameterizedTest
    @ValueSource(strings = {"jurisdictions", "Jurisdictions", "JURISDICTIONS", "myJurisdictions", "JurisdictionsHelper",
        "XJURISDICTIONS"})
    void theGuardRecognisesEveryJurisdictionsSpelling(String text) {
        assertThat(spellingsOf("jurisdictions").matcher(text).find()).isTrue();
    }

    // The generic reference field and its OpenAPI description, which name the singular only.
    @ParameterizedTest
    @ValueSource(strings = {"jurisdiction_id", "jurisdictionId", "JURISDICTION_ID", "getJurisdictionId",
        "Id of the record's jurisdiction"})
    void theGuardIgnoresTheGenericJurisdictionField(String text) {
        assertThat(spellingsOf("jurisdictions").matcher(text).find()).isFalse();
    }

    // No exemption: the plural matches no main source.
    @ParameterizedTest
    @ValueSource(strings = {"tickets", "Tickets", "TICKETS", "myTickets", "TicketsHelper", "XTICKETS"})
    void theGuardRecognisesEveryTicketsSpelling(String text) {
        assertThat(spellingsOf("tickets").matcher(text).find()).isTrue();
    }

    // The generic reference field and its OpenAPI description, which name the singular only.
    @ParameterizedTest
    @ValueSource(strings = {"ticket_category_id", "ticketCategoryId", "TICKET_CATEGORY_ID", "getTicketCategoryId",
        "Id of the record's ticket category"})
    void theGuardIgnoresTheGenericTicketCategoryField(String text) {
        assertThat(spellingsOf("tickets").matcher(text).find()).isFalse();
    }

    /**
     * Matches the fragment's words, case-insensitively, joined by any run of whitespace, underscores, hyphens or
     * Javadoc asterisks (so a wrapped comment line still matches), and starting anywhere except straight after a
     * lower-case letter unless the fragment starts upper-case there (a camelCase hump). So "XBASE_LOCATION" and
     * "myBaseLocation" match, but "database location" doesn't.
     */
    private static Pattern spellingsOf(String fragment) {
        String words = Arrays.stream(fragment.split("_")).map(Pattern::quote)
                .collect(Collectors.joining("[\\s_*-]*"));
        return Pattern.compile("(?:(?<![a-z])|(?=[A-Z]))(?i:" + words + ")");
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
