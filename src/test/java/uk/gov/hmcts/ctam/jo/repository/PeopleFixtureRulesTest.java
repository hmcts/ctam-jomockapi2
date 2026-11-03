package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shipped People fixture follows every generation rule (013 data-model.md § 4, G-1 to G-20), so it is verified on
 * every build however it was produced (research R3). Failure messages name the office holder's id.
 */
class PeopleFixtureRulesTest {

    private static final Path FIXTURE = Path.of("src/main/resources/people/office_holders.json");

    private static final Path REFERENCE_DATA = Path.of("src/main/resources/reference-data");

    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-01-01");

    private static String text;

    private static List<JsonNode> people;

    @BeforeAll
    static void readFixture() throws IOException {
        text = Files.readString(FIXTURE, StandardCharsets.UTF_8);
        people = elements(JsonTrees.parse(text));
    }

    @Test
    void g01ThereAreExactly120OfficeHolders() {
        assertThat(people).hasSize(120);
    }

    @Test
    void g02IdsAreUniqueLowerCaseVersion4Uuids() {
        assertThat(people).allSatisfy(person -> assertThat(id(person))
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"));
        assertThat(people.stream().map(PeopleFixtureRulesTest::id).distinct()).hasSize(120);
    }

    @Test
    void g03PerIdsAndPersonalCodesFollowTheirRanges() {
        assertThat(people.stream().map(person -> person.get("per_id").asLong()).collect(Collectors.toSet()))
                .isEqualTo(LongStream.rangeClosed(900001, 900120).boxed().collect(Collectors.toSet()));
        assertThat(people.stream().map(person -> person.get("personal_code").asString()).collect(Collectors.toSet()))
                .isEqualTo(LongStream.range(0, 120).mapToObj(index -> String.valueOf(9100001 + 7 * index))
                                   .collect(Collectors.toSet()));
    }

    @Test
    void g04EntryIdsAreConsecutiveAndUniqueAcrossTheDataset() {
        assertConsecutiveFrom(700001, all("appointments", "appointment_id"));
        assertConsecutiveFrom(800001, all("authorisations", "authorisation_id"));
        assertConsecutiveFrom(600001, all("judiciary_roles", "judiciary_role_id"));
    }

    @Test
    void g05NamesAreWordsAndNoPairRepeats() {
        Set<String> pairs = new HashSet<>();
        assertThat(people).allSatisfy(person -> {
            assertThat(person.get("known_as").asString()).as(id(person)).matches("^[A-Za-z' -]+$");
            assertThat(person.get("surname").asString()).as(id(person)).matches("^[A-Za-z' -]+$");
            assertThat(pairs.add(person.get("known_as").asString() + "|" + person.get("surname").asString()))
                    .as("repeated name %s", id(person)).isTrue();
        });
    }

    @Test
    void g06TitlesFullnamesAndInitialsAreDerived() {
        Set<String> titles = Set.of("District Judge", "Deputy District Judge", "Circuit Judge", "Recorder",
                                    "Tribunal Judge", "Employment Judge", "Magistrate");
        assertThat(people).allSatisfy(person -> {
            String title = person.get("title").asString();
            String knownAs = person.get("known_as").asString();
            String surname = person.get("surname").asString();
            assertThat(titles).as(id(person)).contains(title);
            assertThat(person.get("fullname").asString()).as(id(person))
                    .isEqualTo(title + " " + knownAs + " " + surname);
            assertThat(person.get("initials").asString()).as(id(person))
                    .isEqualTo(("" + knownAs.charAt(0) + surname.charAt(0)).toUpperCase());
        });
    }

    @Test
    void g07EmailsUseTheReservedDomains() {
        Set<String> emails = new HashSet<>();
        assertThat(people).allSatisfy(person -> {
            String email = person.get("email").asString();
            String local = email.substring(0, email.indexOf('@'));
            String expected = (person.get("known_as").asString() + "." + person.get("surname").asString())
                    .toLowerCase().replace(" ", "").replace("'", "");
            assertThat(local).as(id(person)).isIn(expected, expected + "." + person.get("per_id").asLong());
            assertThat(email).as(id(person)).endsWith("@judiciary.example");
            assertThat(emails.add(email)).as("repeated email %s", id(person)).isTrue();
            JsonNode personal = person.get("email_personal");
            if (!personal.isNull()) {
                assertThat(personal.asString()).as(id(person)).isEqualTo(local + "@personal.example");
            }
        });
    }

    @Test
    void g08WorkPhonesAreUniqueInTheDramaRange() {
        List<String> phones = people.stream().map(person -> person.get("work_phone"))
                .filter(phone -> !phone.isNull()).map(JsonNode::asString).toList();
        assertThat(phones).allMatch(phone -> phone.matches("^020 7946 0[0-9]{3}$")).doesNotHaveDuplicates();
    }

    @Test
    void g09PostNominalsRetirementAndLeavingDates() {
        assertThat(people).allSatisfy(person -> {
            JsonNode postNominals = person.get("post_nominals");
            assertThat(postNominals.isNull() || Set.of("KC", "OBE", "MBE").contains(postNominals.asString()))
                    .as(id(person)).isTrue();
            JsonNode retirement = person.get("retirement_date");
            if (!retirement.isNull()) {
                assertThat(date(retirement)).as(id(person)).isAfter(date(person.get("last_updated")));
            }
            assertThat(person.get("leaving_on").isNull()).as(id(person)).isTrue();
        });
    }

    @Test
    void g10GendersCycleAndEveryFifthPersonHidesGender() throws IOException {
        List<Long> genders = ids("genders");
        List<JsonNode> inPerIdOrder = new ArrayList<>(people);
        inPerIdOrder.sort((a, b) -> Long.compare(a.get("per_id").asLong(), b.get("per_id").asLong()));
        for (int index = 0; index < inPerIdOrder.size(); index++) {
            JsonNode person = inPerIdOrder.get(index);
            assertThat(person.get("gender_id").asLong()).as(id(person)).isEqualTo(genders.get(index % genders.size()));
            assertThat(person.get("show_gender").asBoolean()).as(id(person)).isEqualTo(index % 5 != 4);
        }
    }

    @Test
    void g11AppointmentDatesSitEitherSideOfTheReferenceDate() {
        int endingBefore2030 = 0;
        for (JsonNode person : people) {
            assertThat(person.get("appointments").size()).as(id(person)).isBetween(1, 3);
            for (JsonNode appointment : person.get("appointments")) {
                assertThat(date(appointment.get("start_date"))).as(id(person)).isBefore(REFERENCE_DATE);
                JsonNode end = appointment.get("end_date");
                if (!end.isNull() && !date(end).isBefore(REFERENCE_DATE)) {
                    assertThat(date(end)).as(id(person)).isAfterOrEqualTo(LocalDate.parse("2026-03-31"));
                    if (date(end).isBefore(LocalDate.parse("2030-01-01"))) {
                        endingBefore2030++;
                    }
                }
            }
        }
        assertThat(endingBefore2030).isGreaterThanOrEqualTo(5);
    }

    @Test
    void g12TheDatasetHasEveryMixOfAppointments() {
        long both = people.stream().filter(person -> hasCurrent(person) && hasPrevious(person)).count();
        long onlyPrevious = people.stream().filter(person -> !hasCurrent(person)).count();
        long several = people.stream().filter(person -> person.get("appointments").size() >= 2).count();
        assertThat(both).isGreaterThanOrEqualTo(20);
        assertThat(onlyPrevious).isGreaterThanOrEqualTo(3);
        assertThat(several).isGreaterThanOrEqualTo(30);
    }

    @Test
    void g13ExactlyOnePrincipalAppointment() {
        assertThat(people).allSatisfy(person -> {
            List<JsonNode> appointments = elements(person.get("appointments"));
            List<JsonNode> pool = hasCurrent(person)
                    ? appointments.stream().filter(PeopleFixtureRulesTest::isCurrent).toList() : appointments;
            assertThat(pool.stream().filter(appointment -> appointment.get("is_principal").asBoolean()).count())
                    .as(id(person)).isEqualTo(1);
            if (hasCurrent(person)) {
                assertThat(appointments.stream().filter(appointment -> !isCurrent(appointment))
                                   .noneMatch(appointment -> appointment.get("is_principal").asBoolean()))
                        .as(id(person)).isTrue();
            }
        });
    }

    @Test
    void g14ReferencesNameRecordsThatAreStillCurrent() throws IOException {
        Map<Long, JsonNode> titles = records("appointment_titles");
        Map<Long, JsonNode> contracts = records("contract_types");
        Map<Long, JsonNode> roles = records("judiciary_roles");
        Map<Long, JsonNode> tickets = records("tickets");
        Map<Long, JsonNode> bases = records("base_locations");
        Set<Long> tolerated = Set.of(294L, 365L, 367L);
        for (JsonNode person : people) {
            for (JsonNode appointment : person.get("appointments")) {
                assertOpen(titles, appointment.get("role_name_id").asLong(), person);
                assertOpen(contracts, appointment.get("contract_type_id").asLong(), person);
                assertOpen(bases, appointment.get("base_location_id").asLong(), person);
                assertThat(bases.get(appointment.get("base_location_id").asLong()).get("parent_id").isNull())
                        .as(id(person)).isFalse();
            }
            for (JsonNode role : person.get("judiciary_roles")) {
                assertOpen(roles, role.get("judiciary_role_name_id").asLong(), person);
            }
            for (JsonNode authorisation : person.get("authorisations")) {
                long ticket = authorisation.get("ticket_id").asLong();
                if (!tolerated.contains(ticket)) {
                    assertOpen(tickets, ticket, person);
                }
            }
        }
        Map<String, Set<Long>> baseIdsByName = new HashMap<>();
        people.forEach(person -> person.get("appointments").forEach(appointment -> {
            long baseId = appointment.get("base_location_id").asLong();
            baseIdsByName.computeIfAbsent(bases.get(baseId).get("name").asString(), name -> new HashSet<>())
                    .add(baseId);
        }));
        assertThat(baseIdsByName.values()).as("base locations sharing a name (EC-008)")
                .anyMatch(ids -> ids.size() >= 2);
    }

    @Test
    void g15OptionalAppointmentDetailsComeFromSmallListsAndAreSometimesUnset() {
        Map<String, Set<String>> lists = Map.of(
                "circuit", Set.of("North Eastern", "Midland", "Western", "South Eastern", "Northern", "Wales"),
                "bench", Set.of("Central Bench", "North Bench", "South Bench", "East Bench"),
                "advisory_committee_area", Set.of("Greater London", "West Yorkshire", "Merseyside", "Kent",
                                                  "Devon and Cornwall"));
        List<JsonNode> appointments = people.stream().flatMap(person -> elements(person.get("appointments")).stream())
                .toList();
        lists.forEach((field, values) -> {
            long set = appointments.stream().filter(appointment -> !appointment.get(field).isNull()).count();
            assertThat(set).as(field).isBetween(appointments.size() / 4L, appointments.size() * 3L / 4);
            assertThat(appointments).filteredOn(appointment -> !appointment.get(field).isNull())
                    .allMatch(appointment -> values.contains(appointment.get(field).asString()));
        });
    }

    @Test
    void g16JudiciaryRolesAreBothEndedAndOpen() {
        assertThat(people).allSatisfy(person -> assertThat(person.get("judiciary_roles").size()).as(id(person))
                .isBetween(0, 2));
        List<JsonNode> roles = people.stream().flatMap(person -> elements(person.get("judiciary_roles")).stream())
                .toList();
        assertThat(roles.stream().filter(role -> role.get("end_date").isNull()).count()).isGreaterThanOrEqualTo(10);
        assertThat(roles.stream().filter(role -> !role.get("end_date").isNull()).count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void g17AuthorisationsSitWithinTheirAppointmentAndTicket() throws IOException {
        Map<Long, JsonNode> tickets = records("tickets");
        int ended = 0;
        int open = 0;
        for (JsonNode person : people) {
            assertThat(person.get("authorisations").size()).as(id(person)).isBetween(0, 4);
            Map<Long, JsonNode> appointments = new HashMap<>();
            person.get("appointments").forEach(appointment -> appointments.put(
                    appointment.get("appointment_id").asLong(), appointment));
            for (JsonNode authorisation : person.get("authorisations")) {
                JsonNode appointment = appointments.get(authorisation.get("appointment_id").asLong());
                assertThat(appointment).as(id(person)).isNotNull();
                assertThat(date(authorisation.get("start_date"))).as(id(person))
                        .isAfterOrEqualTo(date(appointment.get("start_date")));
                JsonNode end = authorisation.get("end_date");
                if (!appointment.get("end_date").isNull()) {
                    assertThat(end.isNull()).as(id(person)).isFalse();
                    assertThat(date(end)).as(id(person)).isBeforeOrEqualTo(date(appointment.get("end_date")));
                }
                JsonNode ticketEnd = tickets.get(authorisation.get("ticket_id").asLong()).get("end_date");
                if (!ticketEnd.isNull()) {
                    assertThat(end.isNull()).as(id(person)).isFalse();
                    assertThat(date(end)).as(id(person)).isBeforeOrEqualTo(date(ticketEnd));
                }
                if (end.isNull()) {
                    open++;
                } else {
                    ended++;
                }
            }
        }
        assertThat(ended).isGreaterThanOrEqualTo(10);
        assertThat(open).isGreaterThanOrEqualTo(10);
    }

    @Test
    void g18SomeoneHoldsTheSwaggerExamplesTickets() {
        assertThat(people).anyMatch(person -> elements(person.get("authorisations")).stream()
                .map(authorisation -> authorisation.get("ticket_id").asLong())
                .collect(Collectors.toSet()).containsAll(Set.of(294L, 365L, 367L)));
    }

    @Test
    void g19LastUpdatedCoversEveryDateAndIsSpreadOutWithTies() {
        assertThat(people).allSatisfy(person -> {
            LocalDate lastUpdated = date(person.get("last_updated"));
            for (String list : List.of("appointments", "judiciary_roles", "authorisations")) {
                for (JsonNode entry : person.get(list)) {
                    assertThat(lastUpdated).as(id(person)).isAfterOrEqualTo(date(entry.get("start_date")));
                    if (!entry.get("end_date").isNull()) {
                        assertThat(lastUpdated).as(id(person)).isAfterOrEqualTo(date(entry.get("end_date")));
                    }
                }
            }
        });
        assertThat(people.stream().map(person -> person.get("last_updated").asString().substring(0, 7)).distinct()
                           .count()).isGreaterThanOrEqualTo(12);
        assertThat(people.stream().map(person -> person.get("last_updated").asString()).distinct().count())
                .isLessThan(people.size());
    }

    @Test
    void g20EveryOptionalFieldIsBothPresentAndAbsent() {
        Map<String, Function<JsonNode, List<JsonNode>>> holders = Map.of(
                "email_personal", List::of, "post_nominals", List::of, "work_phone", List::of,
                "retirement_date", List::of,
                "circuit", person -> elements(person.get("appointments")),
                "bench", person -> elements(person.get("appointments")),
                "advisory_committee_area", person -> elements(person.get("appointments")));
        holders.forEach((field, within) -> {
            List<JsonNode> values = people.stream().flatMap(person -> within.apply(person).stream())
                    .map(node -> node.get(field)).toList();
            assertThat(values).as(field).anyMatch(JsonNode::isNull).anyMatch(value -> !value.isNull());
        });
        assertThat(people).anyMatch(person -> person.get("show_gender").asBoolean())
                .anyMatch(person -> !person.get("show_gender").asBoolean());
    }

    @Test
    void theFileIsInLastUpdatedThenIdOrder() {
        List<String> keys = people.stream().map(person -> person.get("last_updated").asString() + " " + id(person))
                .toList();
        assertThat(keys).isSorted();
    }

    @Test
    void theFileWritesNoCharacterAsAnEscape() {
        assertThat(text).doesNotContain("\\u");
    }

    private static void assertOpen(Map<Long, JsonNode> records, long id, JsonNode person) {
        JsonNode record = records.get(id);
        assertThat(record).as("%s refers to %d", id(person), id).isNotNull();
        assertThat(record.get("end_date").isNull()).as("%s refers to ended %d", id(person), id).isTrue();
    }

    private static void assertConsecutiveFrom(long first, List<Long> ids) {
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(new HashSet<>(ids))
                .isEqualTo(LongStream.range(first, first + ids.size()).boxed().collect(Collectors.toSet()));
    }

    private static List<Long> all(String list, String field) {
        return people.stream().flatMap(person -> elements(person.get(list)).stream())
                .map(entry -> entry.get(field).asLong()).toList();
    }

    private static boolean hasCurrent(JsonNode person) {
        return elements(person.get("appointments")).stream().anyMatch(PeopleFixtureRulesTest::isCurrent);
    }

    private static boolean hasPrevious(JsonNode person) {
        return elements(person.get("appointments")).stream().anyMatch(appointment -> !isCurrent(appointment));
    }

    private static boolean isCurrent(JsonNode appointment) {
        JsonNode end = appointment.get("end_date");
        return end.isNull() || !date(end).isBefore(REFERENCE_DATE);
    }

    private static Map<Long, JsonNode> records(String type) throws IOException {
        Map<Long, JsonNode> byId = new HashMap<>();
        elements(JsonTrees.parse(Files.readString(REFERENCE_DATA.resolve(type + ".json"), StandardCharsets.UTF_8)))
                .forEach(record -> byId.put(record.get("id").asLong(), record));
        return byId;
    }

    private static List<Long> ids(String type) throws IOException {
        return records(type).keySet().stream().sorted().toList();
    }

    private static List<JsonNode> elements(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).toList();
    }

    private static LocalDate date(JsonNode node) {
        return LocalDate.parse(node.asString());
    }

    private static String id(JsonNode person) {
        return person.get("id").asString();
    }
}
