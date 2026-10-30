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
import java.util.TreeSet;
import java.util.stream.LongStream;

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
 * 008's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class LocationTypesFunctionalTest {

    /**
     * The singular name, which 008 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/location_type";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String END_DATE = "2025-03-31";

    /**
     * Every record's fields, in order: 002's, with the reference field and then the text field after {@code name}.
     */
    private static final List<String> FIELDS = List.of("id", "name", "jurisdiction_id", "integration_code",
                                                       "created_at", "updated_at", "start_date", "end_date");

    /**
     * One row of the spec's Records table.
     */
    private record Row(long id, String name, long jurisdictionId, String integrationCode, boolean ended) {
    }

    /**
     * The whole default dataset (data-model.md § 4), in id order.
     */
    private static final List<Row> RECORDS = List.of(
            new Row(1, "Region", 30, "COURTS_REGION", false),
            new Row(2, "Court", 30, "COURTS_COURT_NAME", false),
            new Row(3, "Tier", 34, "TRIBS_TIER", false),
            new Row(4, "Chamber", 34, "TRIBS_CHAMBER", false),
            new Row(5, "Location", 34, "TRIBS_LOCATION", false),
            new Row(6, "Region", 84, "MAGS_REGION", false),
            new Row(7, "AC", 84, "MAGS_AC", true),
            new Row(8, "LJA", 84, "MAGS_AC_LJA", false),
            new Row(9, "Court Name", 84, "MAGS_AC_COURT_NAME", false),
            new Row(10, "Area or Parent AC", 84, "MAGS_AREA_OR_PARENT_AC", false),
            new Row(11, "AC", 84, "MAGS_AREA_AC", false),
            new Row(12, "Subcommittee", 84, "MAGS_AREA_SUBCOMMITTEE", false),
            new Row(13, "Committee", 84, "MAGS_AREA_COMMITTEE", false),
            new Row(14, "Region", 86, "CORONERS_REGION", true),
            new Row(15, "Local Authority", 86, "CORONERS_LOCAL_AUTH", false),
            new Row(16, "Geographic Area", 86, "CORONERS_AREA", false));

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll16LocationTypesInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(LOCATION_TYPES);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(16);
        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            // So no type_id or parent_id either.
            assertThat(record.propertyNames()).containsExactlyElementsOf(FIELDS);
        });
        assertThat(ids).containsExactlyElementsOf(LongStream.rangeClosed(1, 16).boxed().toList());
    }

    @Test
    void ac002RecordsMatchTheRecordsTable() {
        JsonNode results = json(http.get(LOCATION_TYPES)).get("results");

        for (int position = 1; position <= RECORDS.size(); position++) {
            Row row = RECORDS.get(position - 1);
            JsonNode record = results.get(position - 1);
            assertThat(record.get("id").asLong()).as("id at position %d", position).isEqualTo(row.id());
            assertRecord(record, row);
        }
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    @Test
    void ac003RepeatedNamesStaySeparate() {
        JsonNode results = json(http.get(LOCATION_TYPES)).get("results");

        assertThat(idsNamed(results, "Region")).containsExactly(1L, 6L, 14L);
        assertThat(idsNamed(results, "AC")).containsExactly(7L, 11L);
        Map<Long, JsonNode> byId = byId(results);
        assertThat(List.of(1L, 6L, 14L)).map(id -> byId.get(id).get("jurisdiction_id").asLong())
                .containsExactly(30L, 84L, 86L);
        assertThat(List.of(7L, 11L)).map(id -> byId.get(id).get("integration_code").asString())
                .containsExactly("MAGS_AC", "MAGS_AREA_AC");
        Set<String> names = new HashSet<>();
        results.forEach(record -> names.add(record.get("name").asString()));
        assertThat(names).hasSize(13);
    }

    @Test
    void ac004ExactlyIds7And14AreEnded() {
        ResponseEntity<String> response = http.get(LOCATION_TYPES);
        String body = response.getBody();

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(body.split("\"end_date\":\"2025-03-31\"", -1)).hasSize(3);
        assertThat(body.split("\"end_date\":null", -1)).hasSize(15);

        // Ended by position in id order; for this type a record's position is its id (EC-006).
        List<Long> ended = new ArrayList<>();
        json(response).get("results").forEach(record -> {
            if (!record.get("end_date").isNull()) {
                ended.add(record.get("id").asLong());
                assertThat(record.get("updated_at").asString()).isEqualTo(ENDED_UPDATED_AT);
            }
        });
        assertThat(ended).containsExactly(7L, 14L);
    }

    @Test
    void ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("location_types.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(LOCATION_TYPES).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {1, 7, 10, 14, 16})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(LOCATION_TYPES + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(LOCATION_TYPES)).get("results")).get(id));
        assertRecord(record, RECORDS.get((int) id - 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"17", "999", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(LOCATION_TYPES + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(LOCATION_TYPES + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(LOCATION_TYPES + "/1")).isEqualTo("Region");
        assertThat(http.nameOf(LOCATION_TYPES + "/10")).isEqualTo("Area or Parent AC");
        assertThat(http.nameOf(LOCATIONS + "/1")).isEqualTo("Aberconwy");
        assertThat(http.nameOf(BASE_LOCATIONS + "/10")).isEqualTo("Aberconwy");
    }

    // US3: every location's type resolves, with the same jurisdiction.

    @Test
    void ac010EveryTypeIdResolves() {
        Set<Long> typeIds = byId(json(http.get(LOCATION_TYPES)).get("results")).keySet();

        Set<Long> fromLocations = typeIdsOf(LOCATIONS);
        assertThat(fromLocations).containsExactlyElementsOf(LongStream.rangeClosed(1, 16).boxed().toList());
        assertThat(typeIds).containsAll(fromLocations);

        Set<Long> fromBaseLocations = typeIdsOf(BASE_LOCATIONS);
        assertThat(fromBaseLocations).containsExactly(2L, 5L, 8L, 9L, 11L, 12L, 13L, 16L);
        assertThat(typeIds).containsAll(fromBaseLocations);
    }

    @Test
    void ac011JurisdictionsAgreeWithTheirType() {
        Map<Long, JsonNode> types = byId(json(http.get(LOCATION_TYPES)).get("results"));

        for (String path : List.of(LOCATIONS, BASE_LOCATIONS)) {
            json(http.get(path)).get("results").forEach(record -> {
                JsonNode type = types.get(record.get("type_id").asLong());
                assertThat(record.get("jurisdiction_id").asLong())
                        .as("%s %d: jurisdiction_id of type %d", path, record.get("id").asLong(),
                            record.get("type_id").asLong())
                        .isEqualTo(type.get("jurisdiction_id").asLong());
            });
        }
    }

    @Test
    void ac012JurisdictionCodesAreCopied() {
        Map<Long, Integer> counts = new TreeMap<>();
        json(http.get(LOCATION_TYPES)).get("results").forEach(record -> {
            assertThat(record.get("jurisdiction_id").isIntegralNumber()).isTrue();
            counts.merge(record.get("jurisdiction_id").asLong(), 1, Integer::sum);
        });

        assertThat(counts).isEqualTo(Map.of(30L, 2, 34L, 3, 84L, 8, 86L, 3));
    }

    // US4: the singular name is not served.

    @Test
    void ac013TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "17", "abc"})
    void ac014TheSingularSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(SINGULAR + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SINGULAR, SINGULAR + "/1"})
    void ac015AuthenticationIsCheckedBeforeTheSingularName(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US5: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {LOCATION_TYPES, LOCATION_TYPES + "/1", LOCATION_TYPES + "/17", LOCATION_TYPES + "/abc"})
    void ac016MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac017InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(LOCATION_TYPES, LOCATION_TYPES + "/1", LOCATION_TYPES + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US6: behaviour inherited from 002. AC-019 is covered by ReferenceDataFunctionalTest's unsupported names.

    @ParameterizedTest
    @ValueSource(strings = {LOCATION_TYPES + "?jurisdiction_id=84", LOCATION_TYPES + "?page=2",
        LOCATION_TYPES + "?x=", LOCATION_TYPES + "/1?name=Region"})
    void ac020QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCATION_TYPES + "/", LOCATION_TYPES + "/1/", LOCATION_TYPES + "/1/extra"})
    void ec009UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec009OtherMethodsReturn405(String method) {
        for (String path : List.of(LOCATION_TYPES, LOCATION_TYPES + "/1")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCATION_TYPES, LOCATION_TYPES + "/1"})
    void ec009UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec009LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(LOCATION_TYPES + "/0001");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(1);
        assertError(http.get(LOCATION_TYPES + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    /**
     * Only a type that declares the text field carries it (FR-005, FR-019).
     */
    @ParameterizedTest
    @ValueSource(strings = {APPOINTMENT_TITLES, BASE_LOCATIONS, CONTRACT_TYPES, GENDERS, JUDICIARY_ROLES, LOCATIONS,
        JURISDICTIONS, TICKETS, TICKET_CATEGORIES})
    void ec004OtherTypesDoNotCarryIntegrationCode(String path) {
        ResponseEntity<String> response = http.get(path);

        assertThat(response.getBody()).doesNotContain("integration_code");
        assertThat(json(response).get("results")).isNotEmpty();
    }

    private static void assertRecord(JsonNode record, Row row) {
        String name = row.name() + " (" + row.id() + ")";
        assertThat(record).as("record %s", name).isNotNull();
        assertThat(record.get("name").asString()).as("name of %s", name).isEqualTo(row.name());
        assertThat(record.get("jurisdiction_id").asLong()).as("jurisdiction_id of %s", name)
                .isEqualTo(row.jurisdictionId());
        assertThat(record.get("integration_code").asString()).as("integration_code of %s", name)
                .isEqualTo(row.integrationCode());
        if (row.ended()) {
            assertThat(record.get("end_date").asString()).as("end_date of %s", name).isEqualTo(END_DATE);
        } else {
            assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        }
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name)
                .isEqualTo(row.ended() ? ENDED_UPDATED_AT : OPEN_UPDATED_AT);
    }

    private Set<Long> typeIdsOf(String path) {
        Set<Long> typeIds = new TreeSet<>();
        json(http.get(path)).get("results").forEach(record -> typeIds.add(record.get("type_id").asLong()));
        return typeIds;
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
