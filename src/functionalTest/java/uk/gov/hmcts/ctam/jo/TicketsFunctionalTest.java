package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_REFERENCE_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.NOT_ACCEPTABLE;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.QUERY_PARAMETERS_NOT_SUPPORTED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RECORD_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
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
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 010's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class TicketsFunctionalTest {

    /**
     * The singular name, which 010 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/ticket";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String END_DATE = "2025-03-31";

    /**
     * Every record's fields, in order: 002's, with the reference field after {@code name}.
     */
    private static final List<String> FIELDS = List.of("id", "name", "ticket_category_id", "created_at", "updated_at",
                                                       "start_date", "end_date");

    /**
     * The records at positions 7, 14, …, 154 (spec AC-003).
     */
    private static final List<Long> ENDED_IDS = List.of(295L, 303L, 310L, 317L, 324L, 331L, 338L, 345L, 352L, 359L,
            367L, 375L, 382L, 394L, 402L, 409L, 1416L, 1455L, 1493L, 1562L, 1600L, 1607L);

    /**
     * One row of the spec's Reference Points table.
     */
    private record Row(long id, String name, long ticketCategoryId, boolean ended) {
    }

    /**
     * The reference points (data-model.md § 4), in id order.
     */
    private static final List<Row> REFERENCE_POINTS = List.of(
            new Row(289, "Welsh Language", 88, false),
            new Row(290, "Administrative Court", 31, false),
            new Row(294, "Civil Authorisation", 31, false),
            new Row(295, "Commercial", 31, true),
            new Row(365, "03 - Disability Living Allowance", 36, false),
            new Row(367, "05 - Industrial Injuries", 36, true),
            new Row(409, "Financial Remedy", 31, true),
            new Row(410, "Financial Remedy", 33, false),
            new Row(1709, "Valuation Tribunal Wales", 83, false));

    /**
     * The repeated names and the two records holding each (data-model.md § 3).
     */
    private static final Map<String, List<Long>> REPEATED = Map.of(
            "Claims Management Services", List.of(323L, 1561L),
            "Community Rights to Bid", List.of(324L, 1493L),
            "Consumer Credit", List.of(326L, 1560L),
            "Financial Remedy", List.of(409L, 410L),
            "National Security", List.of(346L, 1527L),
            "Race Panel", List.of(351L, 1526L),
            "Reserve Forces", List.of(353L, 1491L));

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll159TicketsInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(TICKETS);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(159);
        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            // So no other reference, boolean or text field either.
            assertThat(record.propertyNames()).containsExactlyElementsOf(FIELDS);
        });
        assertThat(ids).isSortedAccordingTo(Long::compare).doesNotHaveDuplicates();
        assertThat(ids.getFirst()).isEqualTo(289L);
        assertThat(ids.getLast()).isEqualTo(1709L);
    }

    @Test
    void ac002ReferencePointsMatchTheTable() {
        JsonNode results = json(http.get(TICKETS)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        REFERENCE_POINTS.forEach(row -> assertRecord(byId.get(row.id()), row));
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    @Test
    void ac003Exactly22RecordsAreEnded() {
        ResponseEntity<String> response = http.get(TICKETS);
        String body = response.getBody();

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(body.split("\"end_date\":\"2025-03-31\"", -1)).hasSize(23);
        assertThat(body.split("\"end_date\":null", -1)).hasSize(138);

        List<Long> ended = new ArrayList<>();
        json(response).get("results").forEach(record -> {
            if (record.get("end_date").isNull()) {
                assertThat(record.get("updated_at").asString()).isEqualTo(OPEN_UPDATED_AT);
            } else {
                ended.add(record.get("id").asLong());
                assertThat(record.get("updated_at").asString()).isEqualTo(ENDED_UPDATED_AT);
            }
        });
        assertThat(ended).containsExactlyElementsOf(ENDED_IDS);
    }

    @Test
    void ac004RepeatedNamesStaySeparate() {
        JsonNode results = json(http.get(TICKETS)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        Set<String> names = new HashSet<>();
        results.forEach(record -> names.add(record.get("name").asString()));
        assertThat(names).hasSize(152);
        REPEATED.forEach((name, ids) -> {
            assertThat(idsNamed(results, name)).as(name).containsExactlyElementsOf(ids);
            assertThat(byId.get(ids.getFirst()).get("ticket_category_id").asLong()).as(name)
                    .isNotEqualTo(byId.get(ids.getLast()).get("ticket_category_id").asLong());
        });
    }

    @Test
    void ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("tickets.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(TICKETS).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {289, 294, 295, 365, 367, 409, 410, 1709})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(TICKETS + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(TICKETS)).get("results")).get(id));
        assertRecord(record, REFERENCE_POINTS.stream().filter(row -> row.id() == id).findFirst().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "288", "297", "1710", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(TICKETS + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(TICKETS + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(TICKETS + "/294")).isEqualTo("Civil Authorisation");
        assertThat(http.nameOf(LOCATIONS + "/294")).isEqualTo("Llanelli");
        assertThat(http.nameOf(TICKETS + "/290")).isEqualTo("Administrative Court");
        assertThat(http.nameOf(APPOINTMENT_TITLES + "/290")).isEqualTo("Commissioner");
    }

    // US3: each ticket's category. AC-011 is ReferenceDataReferencesTest's completeness check.

    @Test
    void ac010EveryTicketCarriesItsCategory() {
        JsonNode results = json(http.get(TICKETS)).get("results");

        Set<Long> categories = new TreeSet<>();
        results.forEach(record -> {
            JsonNode value = record.path("ticket_category_id");
            assertThat(value.isIntegralNumber()).as("ticket %d: ticket_category_id %s", record.get("id").asLong(),
                                                    value).isTrue();
            assertThat(value.asLong()).as("ticket %d", record.get("id").asLong()).isPositive();
            categories.add(value.asLong());
        });
        assertThat(results.size()).isEqualTo(159);
        assertThat(categories).hasSize(41);
        assertThat(categories).first().isEqualTo(31L);
        assertThat(categories).last().isEqualTo(88L);
    }

    /**
     * Only a type that declares the field carries it (FR-012, FR-019).
     */
    @ParameterizedTest
    @ValueSource(strings = {APPOINTMENT_TITLES, BASE_LOCATIONS, CONTRACT_TYPES, GENDERS, JUDICIARY_ROLES, LOCATIONS,
        LOCATION_TYPES, JURISDICTIONS, TICKET_CATEGORIES})
    void ac012OtherTypesDoNotCarryTheTicketCategoryField(String path) {
        ResponseEntity<String> response = http.get(path);

        assertThat(response.getBody()).doesNotContain("ticket_category_id");
        assertThat(json(response).get("results")).isNotEmpty();
    }

    // US4: the singular name is not served.

    @Test
    void ac013TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"294", "297", "abc"})
    void ac014TheSingularSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(SINGULAR + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SINGULAR, SINGULAR + "/294"})
    void ac015AuthenticationIsCheckedBeforeTheSingularName(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US5: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {TICKETS, TICKETS + "/294", TICKETS + "/297", TICKETS + "/abc"})
    void ac016MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac017InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(TICKETS, TICKETS + "/294", TICKETS + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US6: behaviour inherited from 002. AC-019 is covered by ReferenceDataFunctionalTest's unsupported names.

    @ParameterizedTest
    @ValueSource(strings = {TICKETS + "?ticket_category_id=31", TICKETS + "?page=2", TICKETS + "?x=",
        TICKETS + "/294?name=Commercial"})
    void ac020QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {TICKETS + "/", TICKETS + "/294/", TICKETS + "/294/extra"})
    void ec009UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec009OtherMethodsReturn405(String method) {
        for (String path : List.of(TICKETS, TICKETS + "/294")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {TICKETS, TICKETS + "/294"})
    void ec009UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec009LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(TICKETS + "/0294");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(294);
        assertError(http.get(TICKETS + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    private static void assertRecord(JsonNode record, Row row) {
        String name = row.name() + " (" + row.id() + ")";
        assertThat(record).as("record %s", name).isNotNull();
        assertThat(record.get("name").asString()).as("name of %s", name).isEqualTo(row.name());
        assertThat(record.get("ticket_category_id").asLong()).as("ticket_category_id of %s", name)
                .isEqualTo(row.ticketCategoryId());
        if (row.ended()) {
            assertThat(record.get("end_date").asString()).as("end_date of %s", name).isEqualTo(END_DATE);
        } else {
            assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        }
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name)
                .isEqualTo(row.ended() ? ENDED_UPDATED_AT : OPEN_UPDATED_AT);
        assertThat(record.get("created_at").asString()).as("created_at of %s", name)
                .isEqualTo("2024-01-15T09:00:00Z");
        assertThat(record.get("start_date").asString()).as("start_date of %s", name).isEqualTo("2024-01-01");
    }

    private static List<Long> idsNamed(JsonNode results, String name) {
        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            if (record.get("name").asString().equals(name)) {
                ids.add(record.get("id").asLong());
            }
        });
        return ids;
    }
}
