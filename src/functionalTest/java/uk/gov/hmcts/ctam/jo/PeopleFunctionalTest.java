package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;
import uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp;
import uk.gov.hmcts.ctam.jo.testsupport.TestTarget;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.FLAG_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_PERSON_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.NOT_ACCEPTABLE;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PAGE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PERSON_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PER_PAGE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.REPEATED_QUERY_PARAMETER;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_QUERY_PARAMETER;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UPDATED_SINCE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UPDATED_SINCE_REQUIRED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.BASE_LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.CONTRACT_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.GENDERS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.JUDICIARY_ROLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.JURISDICTIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATION_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.TICKETS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.TICKET_CATEGORIES;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.APPOINTMENT_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.AUTHORISATION_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.JUDICIARY_ROLE_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.OFFICE_HOLDER_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.OPTIONAL_APPOINTMENT_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.OPTIONAL_OFFICE_HOLDER_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.PAGINATION_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.SUMMARY_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleHttp.idsInOrder;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 013's acceptance scenarios, end to end over real HTTP against the default dataset. Test names start with the
 * scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class PeopleFunctionalTest {

    private static final String BEFORE_EVERY_RECORD = "2000-01-01";

    /**
     * A date inside the dataset's last_updated range; Postman's {@code peopleMidDate}.
     */
    private static final String MID_DATE = "2025-06-01";

    private static final String WHOLE_DATASET = "?updated_since=" + BEFORE_EVERY_RECORD
                                                + "&per_page=120&include_previous_appointments=true";

    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-01-01");

    private static final String UNKNOWN_ID = "/00000000-0000-0000-0000-000000000000";

    @LocalServerPort
    private int port;

    private final PeopleHttp http = new PeopleHttp(() -> port);

    // US1: search by updated_since.

    @Test
    void ac001BeforeEveryRecordReturnsAllInPagesOfFifty() {
        JsonNode body = http.ok("?updated_since=" + BEFORE_EVERY_RECORD);

        assertThat(body.propertyNames()).containsExactly("results", "pagination");
        assertThat(body.get("results")).hasSize(50);
        assertThat(body.get("pagination").propertyNames()).containsExactlyElementsOf(PAGINATION_FIELDS);
        assertThat(body.get("pagination").get("results").asLong()).isEqualTo(120);
    }

    @Test
    void ac002AMidRangeDateReturnsExactlyThePeopleUpdatedOnOrAfterIt() {
        List<String> expected = idsInOrder(http.searchAll(BEFORE_EVERY_RECORD, 120, false).stream()
                .filter(holder -> !date(holder, "last_updated").isBefore(LocalDate.parse(MID_DATE))).toList());

        JsonNode body = http.ok("?updated_since=" + MID_DATE + "&per_page=120");

        assertThat(expected).isNotEmpty().hasSizeLessThan(120);
        assertThat(body.get("pagination").get("results").asLong()).isEqualTo(expected.size());
        assertThat(idsInOrder(body.get("results"))).isEqualTo(expected);
    }

    @Test
    void ac003AFutureDateReturnsNothing() {
        JsonNode body = http.ok("?updated_since=2099-01-01");

        assertThat(body.get("results")).isEmpty();
        JsonNode pagination = body.get("pagination");
        assertThat(pagination.get("results").asLong()).isZero();
        assertThat(pagination.get("pages").asLong()).isZero();
        assertThat(pagination.get("current_page").asLong()).isEqualTo(1);
        assertThat(pagination.get("more_pages").asBoolean()).isFalse();
    }

    @Test
    void ac004RepeatedCallsAreIdenticalAndOrderedByLastUpdatedThenId() {
        String first = http.get(WHOLE_DATASET).getBody();
        for (int call = 1; call < 100; call++) {
            assertThat(http.get(WHOLE_DATASET).getBody()).as("call %d", call + 1).isEqualTo(first);
        }
        List<String> keys = elements(json(http.get(WHOLE_DATASET)).get("results")).stream()
                .map(holder -> holder.get("last_updated").asString() + " " + holder.get("id").asString()).toList();
        assertThat(keys).hasSize(120).isSorted();
    }

    @Test
    void ac005EveryOfficeHolderHasExactlyTheRecordsFieldsWithTheirTypes() {
        for (JsonNode holder : http.searchAll(BEFORE_EVERY_RECORD, 120, true)) {
            assertOfficeHolderShape(holder);
        }
    }

    @Test
    void ec001APersonsOwnLastUpdatedIncludesThem() {
        JsonNode someone = http.searchAll(BEFORE_EVERY_RECORD, 120, false).get(60);

        JsonNode body = http.ok("?updated_since=" + someone.get("last_updated").asString() + "&per_page=120");

        assertThat(idsInOrder(body.get("results"))).contains(someone.get("id").asString());
    }

    @Test
    void ec007PeopleSharingALastUpdatedAreOrderedById() {
        List<JsonNode> all = http.searchAll(BEFORE_EVERY_RECORD, 120, false);
        Map<String, List<String>> idsByDate = new TreeMap<>();
        all.forEach(holder -> idsByDate
                .computeIfAbsent(holder.get("last_updated").asString(), date -> new ArrayList<>())
                .add(holder.get("id").asString()));

        List<List<String>> ties = idsByDate.values().stream().filter(ids -> ids.size() > 1).toList();
        assertThat(ties).isNotEmpty().allSatisfy(ids -> assertThat(ids).isSorted());
    }

    @Test
    void fr025ListsAndSummariesAreInTheirFixedOrder() {
        for (JsonNode holder : http.searchAll(BEFORE_EVERY_RECORD, 120, true)) {
            assertSortedBy(holder.get("appointments"), "appointment_id");
            assertSortedBy(holder.get("judiciary_roles"), "judiciary_role_id");
            assertSortedBy(holder.get("authorisations_with_dates"), "authorisation_id");
            for (String summary : List.of("authorisations", "authorisations_current")) {
                List<JsonNode> entries = elements(holder.get(summary));
                assertThat(entries.stream().map(entry -> entry.get("jurisdiction").asString()).toList()).isSorted();
                entries.forEach(entry -> assertThat(elements(entry.get("tickets")).stream().map(JsonNode::asString)
                                                            .toList()).isSorted().doesNotHaveDuplicates());
            }
        }
    }

    @Test
    void defaultSearchMatchesItsGoldenFile() throws IOException {
        assertThat(http.get("?updated_since=" + BEFORE_EVERY_RECORD).getBody())
                .isEqualTo(golden("people_default_page1.json"));
    }

    // US2: paging.

    @Test
    void ac006DefaultsAreTheFirstPageOfFifty() {
        JsonNode pagination = http.ok("?updated_since=" + BEFORE_EVERY_RECORD).get("pagination");

        assertThat(pagination.get("current_page").asLong()).isEqualTo(1);
        assertThat(pagination.get("results_per_page").asLong()).isEqualTo(50);
        assertThat(pagination.get("pages").asLong()).isEqualTo(3);
        assertThat(pagination.get("more_pages").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 50, 120})
    void ac007AndSc002WalkingEveryPageReturnsEachPersonOnceInOrder(int perPage) {
        List<String> expected = idsInOrder(elements(json(http.get(WHOLE_DATASET)).get("results")));
        long pages = (120 + perPage - 1) / perPage;
        List<String> walked = new ArrayList<>();

        for (long page = 1; page <= pages; page++) {
            JsonNode body = http.ok("?updated_since=" + BEFORE_EVERY_RECORD + "&per_page=" + perPage + "&page=" + page);
            JsonNode pagination = body.get("pagination");
            assertThat(pagination.get("pages").asLong()).isEqualTo(pages);
            assertThat(pagination.get("results").asLong()).isEqualTo(120);
            assertThat(pagination.get("current_page").asLong()).isEqualTo(page);
            assertThat(pagination.get("results_per_page").asLong()).isEqualTo(perPage);
            assertThat(pagination.get("more_pages").asBoolean()).isEqualTo(page < pages);
            assertThat(body.get("results").size()).isEqualTo(page < pages ? perPage : 120 - (pages - 1) * perPage);
            walked.addAll(idsInOrder(body.get("results")));
        }

        assertThat(walked).isEqualTo(expected);
    }

    @Test
    void ac008APagePastTheLastIsEmpty() {
        JsonNode body = http.ok("?updated_since=" + BEFORE_EVERY_RECORD + "&page=4");

        assertThat(body.get("results")).isEmpty();
        JsonNode pagination = body.get("pagination");
        assertThat(pagination.get("current_page").asLong()).isEqualTo(4);
        assertThat(pagination.get("results").asLong()).isEqualTo(120);
        assertThat(pagination.get("pages").asLong()).isEqualTo(3);
        assertThat(pagination.get("results_per_page").asLong()).isEqualTo(50);
        assertThat(pagination.get("more_pages").asBoolean()).isFalse();
    }

    @Test
    void ac009APageSizeLargerThanTheMatchesPutsEveryoneOnPageOne() {
        JsonNode body = http.ok("?updated_since=" + BEFORE_EVERY_RECORD + "&per_page=500");

        assertThat(body.get("results")).hasSize(120);
        assertThat(body.get("pagination").get("pages").asLong()).isEqualTo(1);
        assertThat(body.get("pagination").get("more_pages").asBoolean()).isFalse();
    }

    @Test
    void ec003TheLargestPageAndPageSizeAreAccepted() {
        JsonNode farPage = http.ok("?updated_since=" + BEFORE_EVERY_RECORD + "&page=9223372036854775807");
        assertThat(farPage.get("results")).isEmpty();
        assertThat(farPage.get("pagination").get("current_page").asLong()).isEqualTo(Long.MAX_VALUE);

        JsonNode hugePage = http.ok("?updated_since=" + BEFORE_EVERY_RECORD + "&per_page=9223372036854775807");
        assertThat(hugePage.get("results")).hasSize(120);
        assertThat(hugePage.get("pagination").get("pages").asLong()).isEqualTo(1);
    }

    @Test
    void ec004LeadingZerosAreAllowed() {
        assertThat(http.get("?updated_since=" + BEFORE_EVERY_RECORD + "&page=002").getBody())
                .isEqualTo(http.get("?updated_since=" + BEFORE_EVERY_RECORD + "&page=2").getBody());
    }

    // US3: one office holder.

    @Test
    void ac010OneOfficeHolderIsASingleObject() {
        String id = firstId();

        JsonNode body = http.ok("/" + id);

        assertThat(body.get("id").asString()).isEqualTo(id);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("pagination")).isFalse();
        assertOfficeHolderShape(body);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ac011TheSingleRouteReturnsTheSameObjectAsTheSearch(boolean flag) {
        for (JsonNode holder : http.searchAll(BEFORE_EVERY_RECORD, 120, flag)) {
            assertThat(http.ok("/" + holder.get("id").asString() + "?include_previous_appointments=" + flag))
                    .isEqualTo(holder);
        }
        String id = firstId();
        assertThat(http.get("/" + id).getBody())
                .isEqualTo(http.get("/" + id + "?include_previous_appointments=false").getBody());
    }

    @Test
    void ac012AnUnknownWellFormedIdIsNotFound() {
        assertError(http.get(UNKNOWN_ID), 404, PERSON_NOT_FOUND);
    }

    @Test
    void ac013AnIdThatIsNotAUuidIsMalformed() {
        JsonNode someone = http.searchAll(BEFORE_EVERY_RECORD, 120, false).getFirst();
        for (String id : List.of("abc", someone.get("per_id").asString(), someone.get("personal_code").asString())) {
            JsonNode body = assertError(http.get("/" + id), 400, MALFORMED_PERSON_ID);
            assertThat(body.toString()).doesNotContain(id);
        }
    }

    @Test
    void ec005AnUpperCaseIdMatchesAndIsWrittenInLowerCase() {
        String id = firstId();

        assertThat(http.ok("/" + id.toUpperCase()).get("id").asString()).isEqualTo(id);
    }

    @Test
    void singleOfficeHolderMatchesItsGoldenFile() throws IOException {
        assertThat(http.get("/" + firstId()).getBody()).isEqualTo(golden("person_single.json"));
    }

    // US5: agreement with the reference data.

    @Test
    void ac018EveryGenderResolves() {
        Map<Long, JsonNode> genders = byId(http.referenceData(GENDERS));
        for (JsonNode holder : http.searchAll(BEFORE_EVERY_RECORD, 120, true)) {
            String name = named(genders, holder.get("gender_id").asLong());
            assertThat(holder.get("sex").asString()).isEqualTo(name);
            if (holder.has("gender")) {
                assertThat(holder.get("gender").asString()).isEqualTo(name);
            }
        }
    }

    @Test
    void ac019EveryContractTypeAndRoleResolves() {
        Map<Long, JsonNode> contracts = byId(http.referenceData(CONTRACT_TYPES));
        Map<Long, JsonNode> titles = byId(http.referenceData(APPOINTMENT_TITLES));
        assertThat(allAppointments()).isNotEmpty().allSatisfy(appointment -> {
            assertThat(appointment.get("contract_type").asString())
                    .isEqualTo(named(contracts, appointment.get("contract_type_id").asLong()));
            assertThat(appointment.get("role_name").asString())
                    .isEqualTo(named(titles, appointment.get("role_name_id").asLong()));
        });
    }

    @Test
    void ac020EveryBaseLocationResolvesWithItsTypeParentAndJurisdiction() {
        Map<Long, JsonNode> bases = byId(http.referenceData(BASE_LOCATIONS));
        Map<Long, JsonNode> types = byId(http.referenceData(LOCATION_TYPES));
        Map<Long, JsonNode> locations = byId(http.referenceData(LOCATIONS));
        Map<Long, JsonNode> jurisdictions = byId(http.referenceData(JURISDICTIONS));
        assertThat(allAppointments()).isNotEmpty().allSatisfy(appointment -> {
            JsonNode base = bases.get(appointment.get("base_location_id").asLong());
            assertThat(base).isNotNull();
            assertThat(appointment.get("base_location").asString()).isEqualTo(base.get("name").asString());
            assertThat(appointment.get("court_name").asString()).isEqualTo(base.get("name").asString());
            assertThat(appointment.get("court_type").asString())
                    .isEqualTo(named(types, base.get("type_id").asLong()));
            assertThat(appointment.get("location").asString())
                    .isEqualTo(named(locations, base.get("parent_id").asLong()));
            assertThat(appointment.get("type").asString())
                    .isEqualTo(named(jurisdictions, base.get("jurisdiction_id").asLong()));
        });
    }

    @Test
    void ac021EveryJudiciaryRoleResolves() {
        Map<Long, JsonNode> roles = byId(http.referenceData(JUDICIARY_ROLES));
        List<JsonNode> entries = all(holder -> holder.get("judiciary_roles"));
        assertThat(entries).isNotEmpty().allSatisfy(role -> assertThat(role.get("name").asString())
                .isEqualTo(named(roles, role.get("judiciary_role_name_id").asLong())));
    }

    @Test
    void ac022EveryTicketResolvesWithTheJurisdictionOfItsCategory() {
        Map<Long, JsonNode> tickets = byId(http.referenceData(TICKETS));
        Map<Long, JsonNode> categories = byId(http.referenceData(TICKET_CATEGORIES));
        Map<Long, JsonNode> jurisdictions = byId(http.referenceData(JURISDICTIONS));
        assertThat(all(holder -> holder.get("authorisations_with_dates"))).isNotEmpty().allSatisfy(authorisation -> {
            JsonNode ticket = tickets.get(authorisation.get("ticket_id").asLong());
            assertThat(ticket).isNotNull();
            assertThat(authorisation.get("ticket").asString()).isEqualTo(ticket.get("name").asString());
            long jurisdictionId = categories.get(ticket.get("ticket_category_id").asLong())
                    .get("jurisdiction_id").asLong();
            assertThat(authorisation.get("jurisdiction_id").asLong()).isEqualTo(jurisdictionId);
            assertThat(authorisation.get("jurisdiction").asString()).isEqualTo(named(jurisdictions, jurisdictionId));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ac023EverySummaryAgreesWithTheDatedAuthorisations(boolean flag) {
        Map<String, JsonNode> complete = new TreeMap<>();
        http.searchAll(BEFORE_EVERY_RECORD, 120, true).forEach(holder -> complete.put(holder.get("id").asString(),
                                                                                      holder));
        for (JsonNode holder : http.searchAll(BEFORE_EVERY_RECORD, 120, flag)) {
            assertThat(holder.get("authorisations")).isEqualTo(summarise(elements(
                    holder.get("authorisations_with_dates"))));
            List<JsonNode> current = elements(complete.get(holder.get("id").asString())
                                                      .get("authorisations_with_dates")).stream()
                    .filter(authorisation -> isCurrent(authorisation)).toList();
            assertThat(holder.get("authorisations_current")).isEqualTo(summarise(current));
        }
    }

    // US7: credentials.

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"null, ?updated_since=2000-01-01",
        "Bearer wrong, ?updated_since=2000-01-01", "null, /00000000-0000-0000-0000-000000000000",
        "Bearer wrong, /00000000-0000-0000-0000-000000000000"})
    void ac029RequestsWithoutAValidTokenAreRefused(String authorization, String suffix) {
        ResponseEntity<String> response = http.send(HttpMethod.GET, suffix, authorization);

        assertUnauthorized(response);
        assertThat(json(response).has("appointments")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?updated_since=", "/abc", "/abc?include_previous_appointments=yes"})
    void ac030AuthenticationIsCheckedFirst(String suffix) {
        assertUnauthorized(http.send(HttpMethod.GET, suffix, "Bearer wrong"));
    }

    // US4: previous appointments.

    @Test
    void ac014AndAc015TheFlagAddsPreviousAppointmentsAndTheirAuthorisations() {
        JsonNode person = http.searchAll(BEFORE_EVERY_RECORD, 120, true).stream()
                .filter(holder -> elements(holder.get("appointments")).stream().anyMatch(this::isPrevious))
                .filter(holder -> elements(holder.get("appointments")).stream().anyMatch(this::isCurrent))
                .filter(holder -> elements(holder.get("authorisations_with_dates")).stream()
                        .anyMatch(authorisation -> isUnder(authorisation, holder, false)))
                .findFirst().orElseThrow();
        String id = person.get("id").asString();
        JsonNode with = http.ok("/" + id + "?include_previous_appointments=true");

        for (String suffix : List.of("", "?include_previous_appointments=false")) {
            JsonNode without = http.ok("/" + id + suffix);
            assertThat(elements(without.get("appointments"))).allMatch(this::isCurrent);
            assertThat(without.get("appointments").size()).isLessThan(with.get("appointments").size());
            assertThat(elements(without.get("authorisations_with_dates")))
                    .allMatch(authorisation -> isUnder(authorisation, without, true));
            assertThat(without.get("authorisations_with_dates").size())
                    .isLessThan(with.get("authorisations_with_dates").size());
            assertThat(without.get("authorisations_current")).isEqualTo(with.get("authorisations_current"));
            assertThat(without.get("judiciary_roles")).isEqualTo(with.get("judiciary_roles"));
        }
        assertThat(elements(with.get("appointments"))).anyMatch(this::isPrevious);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ac016EveryAuthorisationIsUnderAnAppointmentInTheSameResponse(boolean flag) {
        for (JsonNode holder : http.searchAll(BEFORE_EVERY_RECORD, 120, flag)) {
            assertThat(elements(holder.get("authorisations_with_dates")))
                    .allMatch(authorisation -> isUnder(authorisation, holder, true));
        }
    }

    @Test
    void ac017TheFlagChangesNoMatchOrderOrPagination() {
        for (String page : List.of("1", "2", "3", "4")) {
            String query = "?updated_since=" + MID_DATE + "&per_page=20&page=" + page;
            JsonNode without = http.ok(query);
            JsonNode with = http.ok(query + "&include_previous_appointments=true");
            assertThat(with.get("pagination")).isEqualTo(without.get("pagination"));
            assertThat(idsInOrder(with.get("results"))).isEqualTo(idsInOrder(without.get("results")));
        }
    }

    @Test
    void ec006SomeoneWithOnlyPreviousAppointmentsKeepsOnlyJudiciaryRoles() {
        List<JsonNode> onlyPrevious = http.searchAll(BEFORE_EVERY_RECORD, 120, true).stream()
                .filter(holder -> elements(holder.get("appointments")).stream().allMatch(this::isPrevious)).toList();
        assertThat(onlyPrevious).isNotEmpty();
        Set<String> withRoles = new HashSet<>();
        for (JsonNode holder : onlyPrevious) {
            JsonNode without = http.ok("/" + holder.get("id").asString());
            for (String list : List.of("appointments", "authorisations_with_dates", "authorisations",
                                       "authorisations_current")) {
                assertThat(without.get(list)).as(list).isEmpty();
            }
            assertThat(without.get("judiciary_roles")).isEqualTo(holder.get("judiciary_roles"));
            if (!without.get("judiciary_roles").isEmpty()) {
                withRoles.add(holder.get("id").asString());
            }
        }
        assertThat(idsInOrder(http.searchAll(BEFORE_EVERY_RECORD, 120, false)))
                .containsAll(idsInOrder(onlyPrevious));
        assertThat(withRoles).as("an office holder with only previous appointments and judiciary roles").isNotEmpty();
    }

    @Test
    void wholeDatasetWithPreviousMatchesItsGoldenFile() throws IOException {
        assertThat(http.get(WHOLE_DATASET).getBody()).isEqualTo(golden("people_all_with_previous.json"));
    }

    // US6: invalid requests.

    // {s}, {f} and {id} are expanded by expand().
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        ?page=1 | updated_since is required.
        ?updated_since= | updated_since is required.
        ?updated_since=yesterday | updated_since must be a valid date.
        ?updated_since=2025-6-1 | updated_since must be a valid date.
        ?updated_since=2025-02-30 | updated_since must be a valid date.
        ?updated_since=2025-06-01T00:00:00Z | updated_since must be a valid date.
        {s}&page=0 | page must be a positive whole number.
        {s}&page=-1 | page must be a positive whole number.
        {s}&page=1.5 | page must be a positive whole number.
        {s}&page=abc | page must be a positive whole number.
        {s}&page= | page must be a positive whole number.
        {s}&page=9223372036854775808 | page must be a positive whole number.
        {s}&per_page=0 | per_page must be a positive whole number.
        {s}&per_page=-1 | per_page must be a positive whole number.
        {s}&per_page=1.5 | per_page must be a positive whole number.
        {s}&per_page=abc | per_page must be a positive whole number.
        {s}&per_page= | per_page must be a positive whole number.
        {s}&per_page=9223372036854775808 | per_page must be a positive whole number.
        {s}&{f}=yes | {f} must be true or false.
        {s}&{f}=TRUE | {f} must be true or false.
        {s}&{f}= | {f} must be true or false.
        {s}&foo=1 | Unsupported query parameter.
        {id}?foo=1 | Unsupported query parameter.
        {id}{s} | Unsupported query parameter.
        {id}?page=1 | Unsupported query parameter.
        {id}?per_page=1 | Unsupported query parameter.
        {s}&page=1&page=2 | Query parameters must not be repeated.
        {id}?{f}=true&{f}=false | Query parameters must not be repeated.
        """)
    void ac024ToAc027InvalidRequestsAre400WithTheirFixedMessage(String abbreviated, String message) {
        String suffix = expand(abbreviated);
        JsonNode body = assertError(http.get(suffix), 400, expand(message));

        String value = suffix.substring(suffix.lastIndexOf('=') + 1);
        if (value.length() > 3) {
            assertThat(body.get("error").asString()).doesNotContain(value);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", UNKNOWN_ID + "/x"})
    void ac028TrailingSlashesAndExtraSegmentsAreNotFound(String suffix) {
        assertError(http.get(suffix), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        {s}&foo=1&foo=2 | Query parameters must not be repeated.
        ?updated_since=bad&page=0 | updated_since must be a valid date.
        {s}&page=0&per_page=0 | page must be a positive whole number.
        {s}&per_page=0&{f}=yes | per_page must be a positive whole number.
        /abc?{f}=yes | {f} must be true or false.
        /abc{s} | Unsupported query parameter.
        """)
    void ec010TheFirstFailingCheckDecidesTheResponse(String abbreviated, String message) {
        assertError(http.get(expand(abbreviated)), 400, expand(message));
    }

    /**
     * The error tables' abbreviations: {s} the search with updated_since, {f} the flag's name, {id} an unknown id.
     */
    private static String expand(String abbreviated) {
        return abbreviated.replace("{s}", "?updated_since=" + BEFORE_EVERY_RECORD)
                .replace("{f}", "include_previous_appointments")
                .replace("{id}", UNKNOWN_ID);
    }

    @Test
    void ec012ADateTimeIsNotADate() {
        assertError(http.get("?updated_since=2025-06-01T00:00:00Z"), 400, UPDATED_SINCE_INVALID);
        assertError(http.get(""), 400, UPDATED_SINCE_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", UNKNOWN_ID})
    void otherMethodsAre405AndUnacceptableMediaTypes406(String suffix) {
        ResponseEntity<String> post = http.send(HttpMethod.POST, suffix, TestTarget.bearerToken());
        assertError(post, 405, METHOD_NOT_ALLOWED);
        assertThat(post.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("GET");

        assertError(http.getAccepting(suffix.isEmpty() ? "?updated_since=2000-01-01" : suffix, "text/plain"), 406,
                    NOT_ACCEPTABLE);
    }

    @Test
    void messagesAreTheContractsOwn() {
        assertError(http.get("?updated_since=2000-01-01&page=0"), 400, PAGE_INVALID);
        assertError(http.get("?updated_since=2000-01-01&per_page=0"), 400, PER_PAGE_INVALID);
        assertError(http.get("?updated_since=2000-01-01&include_previous_appointments=1"), 400, FLAG_INVALID);
        assertError(http.get("?updated_since=2000-01-01&x=1"), 400, UNSUPPORTED_QUERY_PARAMETER);
        assertError(http.get("?updated_since=2000-01-01&page=1&page=1"), 400, REPEATED_QUERY_PARAMETER);
    }

    private String firstId() {
        return json(http.get("?updated_since=" + BEFORE_EVERY_RECORD)).get("results").get(0).get("id").asString();
    }

    private void assertOfficeHolderShape(JsonNode holder) {
        String id = holder.get("id").asString();
        assertThat(id).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertFields(holder, OFFICE_HOLDER_FIELDS, OPTIONAL_OFFICE_HOLDER_FIELDS);
        assertThat(holder.get("per_id").isIntegralNumber()).as(id).isTrue();
        assertThat(holder.get("gender_id").isIntegralNumber()).as(id).isTrue();
        assertThat(holder.get("personal_code").asString()).as(id).matches("^[0-9]+$");
        assertThat(holder.get("disability").isBoolean()).as(id).isTrue();
        for (String text : List.of("title", "known_as", "surname", "fullname", "initials", "email", "sex")) {
            assertThat(holder.get(text).isString()).as("%s %s", id, text).isTrue();
        }
        for (String nullableText : List.of("post_nominals", "work_phone")) {
            assertThat(holder.get(nullableText).isString() || holder.get(nullableText).isNull()).as(id).isTrue();
        }
        assertThat(holder.get("last_updated").asString()).as(id).matches("^\\d{4}-\\d{2}-\\d{2}$");
        for (String nullableDate : List.of("retirement_date", "leaving_on")) {
            JsonNode value = holder.get(nullableDate);
            assertThat(value.isNull() || value.asString().matches("^\\d{4}-\\d{2}-\\d{2}$")).as(id).isTrue();
        }
        assertThat(holder.get("leaving_on").isNull()).as(id).isTrue();
        holder.get("appointments").forEach(appointment -> {
            assertFields(appointment, APPOINTMENT_FIELDS, OPTIONAL_APPOINTMENT_FIELDS);
            assertThat(appointment.get("is_principal").isBoolean()).as(id).isTrue();
            assertThat(appointment.get("appointment_id").isIntegralNumber()).as(id).isTrue();
        });
        holder.get("judiciary_roles").forEach(role -> assertFields(role, JUDICIARY_ROLE_FIELDS, List.of()));
        holder.get("authorisations").forEach(summary -> assertFields(summary, SUMMARY_FIELDS, List.of()));
        holder.get("authorisations_current").forEach(summary -> assertFields(summary, SUMMARY_FIELDS, List.of()));
        holder.get("authorisations_with_dates")
                .forEach(authorisation -> assertFields(authorisation, AUTHORISATION_FIELDS, List.of()));
    }

    /**
     * Exactly the given fields, in order, with optional ones allowed to be missing but never null.
     */
    private static void assertFields(JsonNode node, List<String> fields, List<String> optional) {
        List<String> expected = fields.stream().filter(field -> !optional.contains(field) || node.has(field)).toList();
        assertThat(node.propertyNames()).containsExactlyElementsOf(expected);
        optional.stream().filter(node::has).forEach(field -> assertThat(node.get(field).isNull()).isFalse());
    }

    private static void assertSortedBy(JsonNode list, String idField) {
        List<String> keys = elements(list).stream()
                .map(entry -> entry.get("start_date").asString() + " " + "%019d".formatted(entry.get(idField).asLong()))
                .toList();
        assertThat(keys).isSorted();
    }

    private List<JsonNode> allAppointments() {
        return all(holder -> holder.get("appointments"));
    }

    private List<JsonNode> all(Function<JsonNode, JsonNode> list) {
        return http.searchAll(BEFORE_EVERY_RECORD, 120, true).stream()
                .flatMap(holder -> elements(list.apply(holder)).stream()).toList();
    }

    private static JsonNode summarise(List<JsonNode> authorisations) {
        Map<String, TreeSet<String>> grouped = new TreeMap<>();
        authorisations.forEach(authorisation -> grouped
                .computeIfAbsent(authorisation.get("jurisdiction").asString(), name -> new TreeSet<>())
                .add(authorisation.get("ticket").asString()));
        StringBuilder json = new StringBuilder("[");
        grouped.forEach((jurisdiction, tickets) -> json.append(json.length() > 1 ? "," : "")
                .append("{\"jurisdiction\":").append(quote(jurisdiction)).append(",\"tickets\":[")
                .append(tickets.stream().map(PeopleFunctionalTest::quote).collect(Collectors.joining(",")))
                .append("]}"));
        return JsonTrees.parse(json.append("]").toString());
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private boolean isCurrent(JsonNode dated) {
        JsonNode end = dated.get("end_date");
        return end.isNull() || !LocalDate.parse(end.asString()).isBefore(REFERENCE_DATE);
    }

    private boolean isPrevious(JsonNode dated) {
        return !isCurrent(dated);
    }

    /**
     * Whether the authorisation's appointment is in the holder's appointments, optionally only among current ones.
     */
    private boolean isUnder(JsonNode authorisation, JsonNode holder, boolean anyAppointment) {
        // anyAppointment false: only a previous appointment counts.
        long appointmentId = authorisation.get("appointment_id").asLong();
        return elements(holder.get("appointments")).stream()
                .filter(appointment -> anyAppointment || isPrevious(appointment))
                .anyMatch(appointment -> appointment.get("appointment_id").asLong() == appointmentId);
    }

    private static String named(Map<Long, JsonNode> records, long id) {
        JsonNode record = records.get(id);
        assertThat(record).as("record %d", id).isNotNull();
        return record.get("name").asString();
    }

    private static LocalDate date(JsonNode node, String field) {
        return LocalDate.parse(node.get(field).asString());
    }

    private static List<JsonNode> elements(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).toList();
    }
}
