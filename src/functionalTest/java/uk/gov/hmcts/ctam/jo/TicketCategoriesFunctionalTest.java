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
import java.util.TreeMap;

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
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.integralField;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 011's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class TicketCategoriesFunctionalTest {

    /**
     * The singular name, which 011 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/ticket_category";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String END_DATE = "2025-03-31";

    /**
     * Every record's fields, in order: 002's, with the three reference fields after {@code name}.
     */
    private static final List<String> FIELDS = List.of("id", "name", "type_id", "jurisdiction_id",
            "parent_category_id", "created_at", "updated_at", "start_date", "end_date");

    /**
     * The records at positions 7, 14, …, 49 (spec AC-003).
     */
    private static final List<Long> ENDED_IDS = List.of(38L, 45L, 52L, 59L, 66L, 73L, 80L);

    /**
     * The categories with no parent (spec AC-012).
     */
    private static final List<Long> TOP_LEVEL_IDS = List.of(31L, 32L, 33L, 35L, 43L, 48L, 85L, 88L);

    /**
     * The categories no ticket refers to (data-model.md § 3).
     */
    private static final List<Long> UNREFERENCED_IDS = List.of(35L, 43L, 48L, 51L, 53L, 60L, 61L, 63L, 64L, 65L, 66L,
            79L, 81L);

    /**
     * One row of the spec's Reference Points table.
     */
    private record Row(long id, String name, long typeId, long jurisdictionId, Long parentCategoryId, boolean ended) {
    }

    /**
     * The reference points (data-model.md § 4), in id order.
     */
    private static final List<Row> REFERENCE_POINTS = List.of(
            new Row(31, "Civil", 1, 30, null, false),
            new Row(35, "First tier Tribunal", 2, 34, null, false),
            new Row(38, "Immigration and Asylum Chamber", 3, 34, 35L, true),
            new Row(44, "Administrative Appeals Chamber", 3, 34, 43L, false),
            new Row(45, "Immigration and Asylum Chamber", 3, 34, 43L, true),
            new Row(48, "Other Tribunal", 2, 34, null, false),
            new Row(56, "Employment Tribunal (England & Wales)", 3, 34, 48L, false),
            new Row(81, "Transport Tribunal", 3, 34, 48L, false),
            new Row(85, "Court Type", 4, 84, null, false),
            new Row(88, "Languages", 5, 87, null, false));

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll54TicketCategoriesInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(TICKET_CATEGORIES);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(54);
        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            // So no other reference, boolean or text field either.
            assertThat(record.propertyNames()).containsExactlyElementsOf(FIELDS);
        });
        assertThat(ids).isSortedAccordingTo(Long::compare).doesNotHaveDuplicates();
        assertThat(ids.getFirst()).isEqualTo(31L);
        assertThat(ids.getLast()).isEqualTo(88L);
    }

    @Test
    void ac002ReferencePointsMatchTheTable() {
        JsonNode results = json(http.get(TICKET_CATEGORIES)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        REFERENCE_POINTS.forEach(row -> assertRecord(byId.get(row.id()), row));
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    @Test
    void ac003Exactly7RecordsAreEnded() {
        ResponseEntity<String> response = http.get(TICKET_CATEGORIES);
        String body = response.getBody();

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(body.split("\"end_date\":\"2025-03-31\"", -1)).hasSize(8);
        assertThat(body.split("\"end_date\":null", -1)).hasSize(48);

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
    void ac004TheRepeatedNameStaysSeparate() {
        JsonNode results = json(http.get(TICKET_CATEGORIES)).get("results");

        Set<String> names = new HashSet<>();
        List<List<Long>> immigration = new ArrayList<>();
        results.forEach(record -> {
            names.add(record.get("name").asString());
            if (record.get("name").asString().equals("Immigration and Asylum Chamber")) {
                immigration.add(List.of(record.get("id").asLong(), record.get("parent_category_id").asLong()));
            }
        });
        assertThat(names).hasSize(53);
        assertThat(immigration).containsExactly(List.of(38L, 35L), List.of(45L, 43L));
    }

    @Test
    void ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("ticket_categories.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(TICKET_CATEGORIES).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {31, 38, 45, 56, 81, 85, 88})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(TICKET_CATEGORIES + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record.has("parent_category_id")).isTrue();
        assertThat(record).isEqualTo(byId(json(http.get(TICKET_CATEGORIES)).get("results")).get(id));
        assertRecord(record, REFERENCE_POINTS.stream().filter(row -> row.id() == id).findFirst().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "30", "34", "84", "89", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(TICKET_CATEGORIES + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(TICKET_CATEGORIES + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(TICKET_CATEGORIES + "/31")).isEqualTo("Civil");
        assertThat(http.nameOf(LOCATIONS + "/31")).isEqualTo("Bexley L317");
        assertThat(http.nameOf(TICKET_CATEGORIES + "/40")).isEqualTo("Tax Chamber");
        assertThat(http.nameOf(GENDERS + "/40")).isEqualTo("Prefer not to say");
    }

    // US3: every ticket's category resolves.

    @Test
    void ac010EveryTicketsCategoryResolves() {
        Set<Long> categoryIds = byId(json(http.get(TICKET_CATEGORIES)).get("results")).keySet();
        JsonNode tickets = json(http.get(TICKETS)).get("results");

        // So an empty response can't pass.
        assertThat(tickets.size()).isEqualTo(159);
        tickets.forEach(ticket -> assertThat(integralField("ticket", ticket, "ticket_category_id"))
                .as("ticket %d: ticket_category_id", ticket.get("id").asLong())
                .isIn(categoryIds));
    }

    @Test
    void ac011UnreferencedCategoriesAreServed() {
        Set<Long> referenced = new HashSet<>();
        json(http.get(TICKETS)).get("results")
                .forEach(ticket -> referenced.add(integralField("ticket", ticket, "ticket_category_id")));

        for (long id : UNREFERENCED_IDS) {
            assertThat(http.get(TICKET_CATEGORIES + "/" + id).getStatusCode().value()).as("category %d", id)
                    .isEqualTo(200);
            assertThat(referenced).as("tickets referring to %d", id).doesNotContain(id);
        }
    }

    // US4: the hierarchy and its references.

    @Test
    void ac012TheHierarchyHasTwoLevels() {
        JsonNode results = json(http.get(TICKET_CATEGORIES)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        List<Long> topLevel = new ArrayList<>();
        Map<Long, Integer> children = new TreeMap<>();
        results.forEach(record -> {
            long id = record.get("id").asLong();
            JsonNode parent = record.get("parent_category_id");
            assertThat(parent).as("category %d: parent_category_id present", id).isNotNull();
            if (parent.isNull()) {
                topLevel.add(id);
            } else {
                assertThat(parent.asLong()).as("category %d: own parent", id).isNotEqualTo(id);
                assertThat(parentOf(byId, id, parent).get("parent_category_id").isNull())
                        .as("category %d: parent %d is top-level", id, parent.asLong()).isTrue();
                children.merge(parent.asLong(), 1, Integer::sum);
            }
        });
        assertThat(topLevel).containsExactlyElementsOf(TOP_LEVEL_IDS);
        assertThat(children).isEqualTo(Map.of(35L, 7, 43L, 4, 48L, 35));
    }

    @Test
    void ac013JurisdictionsResolveAndAgreeWithTheParent() {
        Set<Long> jurisdictionIds = byId(json(http.get(JURISDICTIONS)).get("results")).keySet();
        JsonNode results = json(http.get(TICKET_CATEGORIES)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        Map<Long, Integer> counts = new TreeMap<>();
        List<Long> skills = new ArrayList<>();
        results.forEach(record -> {
            long id = record.get("id").asLong();
            long jurisdiction = integralField("category", record, "jurisdiction_id");
            assertThat(jurisdiction).as("category %d: jurisdiction_id", id).isIn(jurisdictionIds);
            counts.merge(jurisdiction, 1, Integer::sum);
            if (jurisdiction == 87) {
                skills.add(id);
            }
            JsonNode parent = record.get("parent_category_id");
            if (!parent.isNull()) {
                assertThat(jurisdiction).as("category %d: jurisdiction of parent %d", id, parent.asLong())
                        .isEqualTo(integralField("category", parentOf(byId, id, parent), "jurisdiction_id"));
            }
        });
        assertThat(counts).isEqualTo(Map.of(30L, 3, 34L, 49, 84L, 1, 87L, 1));
        assertThat(skills).containsExactly(88L);
    }

    /**
     * The values only; that they refer to the unserved ticket category types is ReferenceDataReferencesTest's
     * concern (AC-014, DF-10).
     */
    @Test
    void ac014TypeIdsAreCopied() {
        Map<Long, Integer> counts = new TreeMap<>();
        json(http.get(TICKET_CATEGORIES)).get("results")
                .forEach(record -> counts.merge(integralField("category", record, "type_id"), 1, Integer::sum));

        assertThat(counts).isEqualTo(Map.of(1L, 3, 2L, 3, 3L, 46, 4L, 1, 5L, 1));
    }

    /**
     * Only a type that declares the field carries it (FR-014).
     */
    @ParameterizedTest
    @ValueSource(strings = {APPOINTMENT_TITLES, BASE_LOCATIONS, CONTRACT_TYPES, GENDERS, JUDICIARY_ROLES, LOCATIONS,
        LOCATION_TYPES, JURISDICTIONS, TICKETS})
    void fr014OtherTypesDoNotCarryTheParentCategoryField(String path) {
        ResponseEntity<String> response = http.get(path);

        assertThat(response.getBody()).doesNotContain("parent_category_id");
        assertThat(json(response).get("results")).isNotEmpty();
    }

    // US5: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORIES, TICKET_CATEGORIES + "/31", TICKET_CATEGORIES + "/34",
        TICKET_CATEGORIES + "/abc"})
    void ac015MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac016InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(TICKET_CATEGORIES, TICKET_CATEGORIES + "/31", TICKET_CATEGORIES + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US6: the singular name is not served.

    @Test
    void ac017TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"31", "34", "abc"})
    void ac018TheSingularSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(SINGULAR + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SINGULAR, SINGULAR + "/31"})
    void ac019AuthenticationIsCheckedBeforeTheSingularName(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US7: behaviour inherited from 002. AC-021 is covered by ReferenceDataFunctionalTest's unsupported names.

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORIES + "?parent_category_id=48", TICKET_CATEGORIES + "?page=2",
        TICKET_CATEGORIES + "?x=", TICKET_CATEGORIES + "/31?name=Civil"})
    void ac022QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORIES + "/", TICKET_CATEGORIES + "/31/", TICKET_CATEGORIES + "/31/extra"})
    void ec011UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec011OtherMethodsReturn405(String method) {
        for (String path : List.of(TICKET_CATEGORIES, TICKET_CATEGORIES + "/31")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORIES, TICKET_CATEGORIES + "/31"})
    void ec011UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec011LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(TICKET_CATEGORIES + "/031");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(31);
        assertError(http.get(TICKET_CATEGORIES + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    /**
     * The record's parent, failing with the category named if the parent isn't served.
     */
    private static JsonNode parentOf(Map<Long, JsonNode> byId, long id, JsonNode parent) {
        JsonNode record = byId.get(parent.asLong());
        assertThat(record).as("category %d: parent %d is served", id, parent.asLong()).isNotNull();
        return record;
    }

    private static void assertRecord(JsonNode record, Row row) {
        String name = row.name() + " (" + row.id() + ")";
        assertThat(record).as("record %s", name).isNotNull();
        assertThat(record.get("name").asString()).as("name of %s", name).isEqualTo(row.name());
        assertThat(integralField("category", record, "type_id")).as("type_id of %s", name).isEqualTo(row.typeId());
        assertThat(integralField("category", record, "jurisdiction_id")).as("jurisdiction_id of %s", name)
                .isEqualTo(row.jurisdictionId());
        JsonNode parent = record.get("parent_category_id");
        assertThat(parent).as("parent_category_id of %s present", name).isNotNull();
        if (row.parentCategoryId() == null) {
            assertThat(parent.isNull()).as("parent_category_id of %s", name).isTrue();
        } else {
            assertThat(parent.asLong()).as("parent_category_id of %s", name).isEqualTo(row.parentCategoryId());
        }
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
}
