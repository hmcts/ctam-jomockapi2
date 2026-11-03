package uk.gov.hmcts.ctam.jo.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every reference-data type is served by the same generic code (spec AC-013, FR-005): no main source file
 * names a particular type, and only the registry knows about deprecated aliases.
 *
 * <p>One reviewed exception (013 FR-052, research R1): the People sources in {@link #PEOPLE_SOURCES} may use the People
 * contract's own names ({@link #PEOPLE_CONTRACT_NAMES}), which contain type names. In those files every spelling of a
 * type name that isn't a contract name fails first ({@link #NON_CONTRACT_TYPE_NAMES}); then the contract names are
 * masked and the usual scan runs on what remains.
 */
class NoTypeSpecificCodeTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final String REGISTRY = "ReferenceDataTypeRegistry.java";

    /**
     * One fragment per configured type ({@link #noMainSourceFileNamesASpecificReferenceDataType}).
     */
    private static final List<String> FRAGMENTS = List.of("appointment", "base_location", "contract_type", "gender",
            "judiciary_role", "location", "jurisdictions", "tickets", "ticket_categories", "ticket_category_type");

    private static List<Path> sources;

    /**
     * The reviewed People main sources (013 FR-052), relative to {@link #MAIN_SOURCES}. Adding one is a reviewed
     * change: {@link #listedPeopleSourcesAreExactlyThePlannedOnes} pins the list.
     */
    static final List<Path> PEOPLE_SOURCES = Stream.of(
            "config/PeopleProperties.java",
            "controllers/PeopleController.java",
            "domain/AppointmentsResponse.java",
            "domain/AuthorisationSummaryResponse.java",
            "domain/AuthorisationsResponse.java",
            "domain/CurrentOfficeHolderResponse.java",
            "domain/JudiciaryRoleResponse.java",
            "entity/AppointmentRecord.java",
            "entity/AuthorisationRecord.java",
            "entity/JudiciaryRoleRecord.java",
            "entity/OfficeHolder.java",
            "entity/OfficeHolderRecord.java",
            "mappers/PeopleMapper.java",
            "repository/FixturePeopleRepository.java",
            "repository/PeopleDatasetValidator.java",
            "repository/PeopleRepository.java",
            "services/AppointmentStatus.java",
            "services/AuthorisationSummaries.java",
            "services/PeopleService.java")
            .map(name -> MAIN_SOURCES.resolve("uk/gov/hmcts/ctam/jo").resolve(name))
            .toList();

    /**
     * The People contract's names that the listed sources may use, in code and comments, alone or as a run of words
     * inside a longer identifier (013 FR-052). The singular judiciary_role is the Swagger's JudiciaryRoleResponse.
     */
    static final List<String> PEOPLE_CONTRACT_NAMES = List.of("appointment", "appointments", "appointment_id",
            "include_previous_appointments", "gender", "gender_id", "location", "base_location", "base_location_id",
            "contract_type", "contract_type_id", "judiciary_roles", "judiciary_role", "judiciary_role_id",
            "judiciary_role_name_id", "ticket", "ticket_id", "tickets", "jurisdiction", "jurisdiction_id");

    /**
     * The canonical type names that aren't People contract names. Any spelling of one fails a listed source before
     * masking, so masking a contract name can never let one through. Where the singular isn't a contract name either,
     * it's checked too (it also matches the plural): appointment_title and ticket_category_type. The other singulars
     * are contract names (gender, location, contract_type, base_location) or occur in generic or People configuration
     * names (location_type in base_location_type_id, ticket_category in ticket_category_id), so their plurals are used.
     */
    static final List<String> NON_CONTRACT_TYPE_NAMES = List.of("appointment_title", "base_locations",
            "contract_types", "genders", "locations", "location_types", "jurisdictions", "ticket_categories",
            "ticket_category_type");

    /**
     * One word of an identifier or comment: an upper-case run not followed by lower case, a capitalised or lower-case
     * word, or digits. So "baseLocationTypeId", "BASE_LOCATION_ID" and "base location" all split into words.
     */
    private static final Pattern WORD = Pattern.compile("[A-Z]+(?![a-z])|[A-Z]?[a-z]+|[0-9]+");

    /**
     * What may sit between two words of one run: nothing (a camelCase hump), or whitespace, underscores, hyphens and
     * Javadoc asterisks, as {@link #spellingsOf} allows.
     */
    private static final Pattern WORD_GAP = Pattern.compile("[\\s_*-]*");

    @BeforeAll
    static void collectMainSources() throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCES)) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertThat(sources).as("main sources found under %s", MAIN_SOURCES.toAbsolutePath()).isNotEmpty();
    }

    static Stream<String> fragments() {
        return FRAGMENTS.stream();
    }

    // One fragment per configured type; each type's feature adds its own. Checks code, comments and Javadoc alike,
    // in any spelling: snake_case, spaced or wrapped words, camelCase or upper case ("base_location",
    // "base location", "BaseLocation", "XBASE_LOCATION"). Jurisdictions', tickets' and ticket categories' fragments are
    // the plural, because the generic reference fields jurisdiction_id (003) and ticket_category_id (010) contain the
    // singular (009, 010 and 011). No field contains ticket_category_type, so that fragment is the singular (012).
    @ParameterizedTest
    @MethodSource("fragments")
    void noMainSourceFileNamesASpecificReferenceDataType(String fragment) {
        Pattern spellings = spellingsOf(fragment);
        assertThat(sources)
                .filteredOn(path -> spellings.matcher(scannedText(path)).find())
                .as("main sources mentioning %s", fragment)
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("nonContractTypeNames")
    void peopleSourcesNameNoNonContractType(String typeName) {
        Pattern spellings = spellingsOf(typeName);
        assertThat(PEOPLE_SOURCES)
                .filteredOn(path -> spellings.matcher(read(path)).find())
                .as("People sources naming %s", typeName)
                .isEmpty();
    }

    static Stream<String> nonContractTypeNames() {
        return NON_CONTRACT_TYPE_NAMES.stream();
    }

    @Test
    void everyListedPeopleSourceExists() {
        assertThat(PEOPLE_SOURCES).allSatisfy(path -> assertThat(path).isRegularFile());
    }

    @Test
    void listedPeopleSourcesAreExactlyThePlannedOnes() {
        assertThat(PEOPLE_SOURCES).extracting(path -> path.getFileName().toString()).containsExactlyInAnyOrder(
                "PeopleProperties.java", "PeopleController.java", "AppointmentsResponse.java",
                "AuthorisationSummaryResponse.java", "AuthorisationsResponse.java", "CurrentOfficeHolderResponse.java",
                "JudiciaryRoleResponse.java", "AppointmentRecord.java", "AuthorisationRecord.java",
                "JudiciaryRoleRecord.java", "OfficeHolder.java", "OfficeHolderRecord.java", "PeopleMapper.java",
                "FixturePeopleRepository.java", "PeopleDatasetValidator.java", "PeopleRepository.java",
                "AppointmentStatus.java", "AuthorisationSummaries.java", "PeopleService.java");
    }

    @Test
    void referenceDataSourcesUseNoPeopleCode() {
        Pattern people = Pattern.compile("People|Person|OfficeHolder|PeopleProperties");
        assertThat(sources)
                .filteredOn(path -> path.getFileName().toString().startsWith("ReferenceData")
                                    || path.getFileName().toString().startsWith("FixtureReferenceData")
                                    || path.getFileName().toString().equals("ReferenceIdParser.java")
                                    || path.getParent().getFileName().toString().equals("entity")
                                       && !PEOPLE_SOURCES.contains(path))
                .filteredOn(path -> people.matcher(read(path)).find())
                .as("reference-data sources mentioning People code (013 FR-051)")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"getAppointments", "AppointmentsResponse", "AppointmentRecord", "baseLocationId",
        "BASE_LOCATION_ID", "includePreviousAppointments", "judiciaryRoleNameId", "getTickets", "ResolvedAppointment",
        "AppointmentStatus", "currentAppointments", "baseLocationTypeId", "baseLocationParentId",
        "JudiciaryRoleRecord", "JudiciaryRoleResponse", "an appointment's end date", "withGenderId",
        "include_previous_appointments", "the base\n     * location's parent", "ticket_id, jurisdiction_id"})
    void theMaskRemovesContractNames(String text) {
        String masked = maskContractNames(text);
        assertThat(FRAGMENTS).noneMatch(fragment -> spellingsOf(fragment).matcher(masked).find());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AppointmentTitle", "appointmentTitles", "ticketCategoryType", "baseLocations",
        "locationTypes", "Genders", "ContractTypes", "jurisdictions", "ticket categories"})
    void theNonContractCheckRejectsOtherTypeNamesBeforeMasking(String text) {
        assertThat(NON_CONTRACT_TYPE_NAMES).anyMatch(name -> spellingsOf(name).matcher(text).find());
    }

    @ParameterizedTest
    @ValueSource(strings = {"getAppointments", "baseLocationTypeId", "withGenderId", "JudiciaryRoleRecord",
        "ticketCategoryId", "jurisdiction_id", "ticket_category_id", "an appointment's end date"})
    void theNonContractCheckAllowsContractNames(String text) {
        assertThat(NON_CONTRACT_TYPE_NAMES).noneMatch(name -> spellingsOf(name).matcher(text).find());
    }

    // Masking only ever blanks a whole word run equal to a contract name, never part of a longer type name.
    @ParameterizedTest
    @ValueSource(strings = {"baseLocations", "Genders", "LocationsHelper", "myJurisdictions", "ContractTypes"})
    void theMaskKeepsOtherTypeNames(String text) {
        String masked = maskContractNames(text);
        assertThat(FRAGMENTS).anyMatch(fragment -> spellingsOf(fragment).matcher(masked).find());
    }

    @ParameterizedTest
    @ValueSource(strings = {"getAppointments", "AppointmentTitle", "myLocation", "GenderHelper", "baseLocationId"})
    void fileOutsideTheListGetsNoExemption(String text) throws IOException {
        Path unlisted = Files.createTempFile("Unlisted", ".java");
        try {
            Files.writeString(unlisted, "class Unlisted { String " + text + "; }", StandardCharsets.UTF_8);
            String scanned = scannedText(unlisted);
            assertThat(scanned).contains(text);
            assertThat(FRAGMENTS).anyMatch(fragment -> spellingsOf(fragment).matcher(scanned).find());
        } finally {
            Files.delete(unlisted);
        }
    }

    /**
     * What the fragment scan reads: a listed People source with its contract names masked, any other file as is.
     */
    private static String scannedText(Path path) {
        String text = read(path);
        return PEOPLE_SOURCES.contains(path) ? maskContractNames(text) : text;
    }

    /**
     * Blanks every run of consecutive words equal to a People contract name, longest name first, wherever it sits in
     * an identifier or comment (research R1). Words are compared case-insensitively.
     */
    static String maskContractNames(String text) {
        List<int[]> words = new ArrayList<>();
        Matcher word = WORD.matcher(text);
        while (word.find()) {
            words.add(new int[] {word.start(), word.end()});
        }
        List<List<String>> names = PEOPLE_CONTRACT_NAMES.stream()
                .map(name -> List.of(name.split("_")))
                .sorted(Comparator.comparingInt((List<String> name) -> name.size()).reversed())
                .toList();
        char[] masked = text.toCharArray();
        int index = 0;
        while (index < words.size()) {
            int length = longestNameAt(text, words, index, names);
            if (length == 0) {
                index++;
                continue;
            }
            Arrays.fill(masked, words.get(index)[0], words.get(index + length - 1)[1], ' ');
            index += length;
        }
        return new String(masked);
    }

    private static int longestNameAt(String text, List<int[]> words, int index, List<List<String>> names) {
        for (List<String> name : names) {
            if (index + name.size() <= words.size() && runMatches(text, words, index, name)) {
                return name.size();
            }
        }
        return 0;
    }

    private static boolean runMatches(String text, List<int[]> words, int index, List<String> name) {
        for (int offset = 0; offset < name.size(); offset++) {
            int[] span = words.get(index + offset);
            if (!text.substring(span[0], span[1]).equalsIgnoreCase(name.get(offset))) {
                return false;
            }
            if (offset > 0 && !WORD_GAP.matcher(text.substring(words.get(index + offset - 1)[1], span[0])).matches()) {
                return false;
            }
        }
        return true;
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

    // No exemption for gender_id or the plural outside the reviewed People sources (013 FR-052; 005 DF-3).
    @ParameterizedTest
    @ValueSource(strings = {"gender", "genders", "GENDER", "Gender", "GenderHelper", "myGender", "XGENDER", "genderId",
        "gender_id"})
    void theGuardRecognisesEveryGenderSpelling(String text) {
        assertThat(spellingsOf("gender").matcher(text).find()).isTrue();
    }

    // No exemption for the plural, judiciary_role_id or judiciary_role_name_id outside the reviewed People sources
    // (013 FR-052; 006 DF-4).
    @ParameterizedTest
    @ValueSource(strings = {"judiciary_role", "judiciary_roles", "JUDICIARY_ROLE", "judiciary role", "Judiciary Role",
        "judiciary-role", "JudiciaryRole", "judiciaryRole", "myJudiciaryRole", "XJUDICIARY_ROLE", "judiciary_role_id",
        "judiciaryRoleNameId", "judiciary\n     * role"})
    void theGuardRecognisesEveryJudiciaryRoleSpelling(String text) {
        assertThat(spellingsOf("judiciary_role").matcher(text).find()).isTrue();
    }

    // No exemption outside the reviewed People sources (013 FR-052; 007 DF-6).
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

    // No exemption: the plural matches no main source.
    @ParameterizedTest
    @ValueSource(strings = {"ticket_categories", "TicketCategories", "TICKET_CATEGORIES", "ticket categories",
        "myTicketCategories", "XTICKET_CATEGORIES"})
    void theGuardRecognisesEveryTicketCategoriesSpelling(String text) {
        assertThat(spellingsOf("ticket_categories").matcher(text).find()).isTrue();
    }

    // The generic reference fields and their OpenAPI description, which name the singular only.
    @ParameterizedTest
    @ValueSource(strings = {"ticket_category_id", "ticketCategoryId", "TICKET_CATEGORY_ID", "parent_category_id",
        "parentCategoryId", "Id of the record's ticket category"})
    void theGuardIgnoresTheGenericTicketCategoryFields(String text) {
        assertThat(spellingsOf("ticket_categories").matcher(text).find()).isFalse();
    }

    // No exemption: the singular, which also covers the plural, matches no main source.
    @ParameterizedTest
    @ValueSource(strings = {"ticket_category_type", "ticket_category_types", "TicketCategoryTypeMapper",
        "ticketCategoryType", "TICKET_CATEGORY_TYPES", "ticket category type", "XTICKET_CATEGORY_TYPE"})
    void theGuardRecognisesEveryTicketCategoryTypeSpelling(String text) {
        assertThat(spellingsOf("ticket_category_type").matcher(text).find()).isTrue();
    }

    // The other ticket-category names, which aren't this type's.
    @ParameterizedTest
    @ValueSource(strings = {"ticket_category_id", "ticketCategoryId", "ticket_categories", "TicketCategories",
        "the record's ticket category"})
    void theGuardIgnoresOtherTicketCategoryNames(String text) {
        assertThat(spellingsOf("ticket_category_type").matcher(text).find()).isFalse();
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
