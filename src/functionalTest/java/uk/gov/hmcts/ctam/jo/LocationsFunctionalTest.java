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
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 007's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class LocationsFunctionalTest {

    /**
     * The singular name, which 007 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/location";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String END_DATE = "2025-03-31";

    /**
     * Every record's fields, in order: 002's, with 003's reference fields after {@code name}.
     */
    private static final List<String> FIELDS = List.of("id", "name", "type_id", "parent_id", "jurisdiction_id",
                                                       "created_at", "updated_at", "start_date", "end_date");

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll2000LocationsInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(LOCATIONS);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(2000);

        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            // Including parent_id when it is null.
            assertThat(record.propertyNames()).containsExactlyElementsOf(FIELDS);
        });
        assertThat(ids).isSortedAccordingTo(Long::compare).doesNotHaveDuplicates();
        assertThat(ids.getFirst()).isEqualTo(1);
        assertThat(ids.getLast()).isEqualTo(2433);
    }

    @Test
    void ac002RecordsMatchTheReferencePoints() {
        JsonNode results = json(http.get(LOCATIONS)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        assertRecord(byId.get(1L), "Aberconwy", 8, 1722L, 84, false);
        assertRecord(byId.get(9L), "Ashby-de-la-Zouch and Market Bosworth", 8, 1732L, 84, true);
        assertRecord(byId.get(10L), "Atherstone and Coleshill", 8, 1751L, 84, false);
        assertRecord(byId.get(1176L), "Cheshire  LJA", 8, 1697L, 84, false);
        assertRecord(byId.get(1323L), "Cheshire LJA", 8, 1476L, 84, false);
        assertRecord(byId.get(1466L), "London", 6, null, 84, true);
        assertRecord(byId.get(1482L), "London", 1, null, 30, false);
        assertRecord(byId.get(1722L), "Old Gwynedd", 7, 2141L, 84, false);
        assertRecord(byId.get(2141L), "Gwynedd", 6, null, 84, false);
        assertRecord(byId.get(2364L), "Newcastle and North Tyneside", 16, 1820L, 86, true);
        assertRecord(byId.get(2398L), "Royal Courts of Justice – Office of the Judge Advocate General", 2, 1482L, 30,
                     false);
        assertRecord(byId.get(2433L), "Kent and Medway", 16, 1925L, 86, false);
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    @Test
    void ac003NamesAreServedExactly() {
        ResponseEntity<String> response = http.get(LOCATIONS);
        String body = response.getBody();

        // On the raw body, which shows each character as served: two spaces kept, the en dash not escaped.
        assertThat(body).contains(
                "\"name\":\"Cheshire  LJA\"",
                "\"name\":\"Cheshire LJA\"",
                "\"name\":\"Royal Courts of Justice – Office of the Judge Advocate General\"");

        // Repeated names are separate locations, each with its own id.
        JsonNode results = json(response).get("results");
        assertThat(idsNamed(results, "National")).hasSize(40);
        assertThat(idsNamed(results, "London")).hasSize(8);
        assertThat(idsNamed(results, "Unknown")).hasSize(7);
        Map<Long, JsonNode> byId = byId(results);
        assertThat(byId.get(1176L).get("name").asString()).isEqualTo("Cheshire  LJA");
        assertThat(byId.get(1323L).get("name").asString()).isEqualTo("Cheshire LJA");
    }

    @Test
    void ac004Exactly285RecordsAreEnded() {
        ResponseEntity<String> response = http.get(LOCATIONS);
        String body = response.getBody();
        JsonNode results = json(response).get("results");

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(body.split("\"end_date\":\"2025-03-31\"", -1)).hasSize(286);
        assertThat(body.split("\"end_date\":null", -1)).hasSize(1716);

        // Ended by position in id order, not by id value (DR-1).
        List<Long> ended = new ArrayList<>();
        List<Long> expected = new ArrayList<>();
        for (int position = 1; position <= results.size(); position++) {
            JsonNode record = results.get(position - 1);
            if (position % 7 == 0) {
                expected.add(record.get("id").asLong());
            }
            if (!record.get("end_date").isNull()) {
                ended.add(record.get("id").asLong());
                assertThat(record.get("updated_at").asString()).isEqualTo(ENDED_UPDATED_AT);
            }
        }
        assertThat(ended).hasSize(285).containsExactlyElementsOf(expected);
        assertThat(ended.getFirst()).isEqualTo(9);
        assertThat(ended.getLast()).isEqualTo(2364);
    }

    @Test
    void ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("locations.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(LOCATIONS).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {1, 9, 1176, 2141, 2398, 2433})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(LOCATIONS + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(LOCATIONS)).get("results")).get(id));
        if (id == 2141) {
            assertThat(response.getBody()).contains("\"parent_id\":null");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "2434", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(LOCATIONS + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(LOCATIONS + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(LOCATIONS + "/1")).isEqualTo("Aberconwy");
        assertThat(http.nameOf(LOCATIONS + "/10")).isEqualTo("Atherstone and Coleshill");
        assertThat(http.nameOf(BASE_LOCATIONS + "/10")).isEqualTo("Aberconwy");
        assertThat(http.nameOf(APPOINTMENT_TITLES + "/10")).isEqualTo("Acting Senior Coroner");
        assertThat(http.nameOf(CONTRACT_TYPES + "/10")).isEqualTo("Fee-paid");
        assertThat(http.nameOf(GENDERS + "/10")).isEqualTo("Female");
        assertThat(http.nameOf(JUDICIARY_ROLES + "/10")).isEqualTo("Acting Designated Civil Judge");
        assertError(http.get(BASE_LOCATIONS + "/1"), 404, RECORD_NOT_FOUND);
    }

    // US3: the hierarchy.

    @Test
    void ac010EveryParentResolvesWithinLocations() {
        ResponseEntity<String> response = http.get(LOCATIONS);
        Map<Long, JsonNode> byId = byId(json(response).get("results"));

        int withParent = 0;
        for (JsonNode record : byId.values()) {
            JsonNode parent = record.get("parent_id");
            if (!parent.isNull()) {
                withParent++;
                assertThat(byId).as("parent of %d", record.get("id").asLong()).containsKey(parent.asLong());
                assertThat(parent.asLong()).as("parent of %d", record.get("id").asLong())
                        .isNotEqualTo(record.get("id").asLong());
            }
            // Every chain reaches a top-level location within two steps.
            JsonNode current = record;
            for (int step = 0; step < 2 && !current.get("parent_id").isNull(); step++) {
                current = byId.get(current.get("parent_id").asLong());
            }
            assertThat(current.get("parent_id").isNull()).as("chain from %d", record.get("id").asLong()).isTrue();
        }
        assertThat(withParent).isEqualTo(1858);
        assertThat(response.getBody().split("\"parent_id\":null", -1)).hasSize(143);
    }

    @Test
    void ac011EveryBaseLocationParentIsALocation() {
        Set<Long> locationIds = byId(json(http.get(LOCATIONS)).get("results")).keySet();
        Set<Long> parents = new HashSet<>();
        json(http.get(BASE_LOCATIONS)).get("results").forEach(record -> parents.add(record.get("parent_id").asLong()));

        assertThat(parents).hasSize(408);
        assertThat(locationIds).containsAll(parents);
    }

    @Test
    void ac012TypeAndJurisdictionCodesAreCopied() {
        JsonNode results = json(http.get(LOCATIONS)).get("results");

        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("type_id").isIntegralNumber()).isTrue();
            assertThat(record.get("type_id").asLong()).isBetween(1L, 16L);
            assertThat(record.get("jurisdiction_id").isIntegralNumber()).isTrue();
            assertThat(record.get("jurisdiction_id").asLong()).isIn(30L, 34L, 84L, 86L);
        });
    }

    // US4: the singular name is not served.

    @Test
    void ac013TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "abc"})
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
    @ValueSource(strings = {LOCATIONS, LOCATIONS + "/1", LOCATIONS + "/2", LOCATIONS + "/abc"})
    void ac016MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac017InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(LOCATIONS, LOCATIONS + "/1", LOCATIONS + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US6: behaviour inherited from 002. AC-019 is covered by ReferenceDataFunctionalTest's unsupported names.

    @ParameterizedTest
    @ValueSource(strings = {LOCATIONS + "?parent_id=1722", LOCATIONS + "?page=2", LOCATIONS + "?x=",
        LOCATIONS + "/1?name=Aberconwy"})
    void ac020QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCATIONS + "/", LOCATIONS + "/1/", LOCATIONS + "/1/extra"})
    void ec011UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec011OtherMethodsReturn405(String method) {
        for (String path : List.of(LOCATIONS, LOCATIONS + "/1")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCATIONS, LOCATIONS + "/1"})
    void ec011UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec011LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(LOCATIONS + "/0001");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(1);
        assertError(http.get(LOCATIONS + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    /**
     * A null {@code parentId} means a top-level location: the key is present and its value null.
     */
    private static void assertRecord(JsonNode record, String name, long typeId, Long parentId, long jurisdictionId,
                                     boolean ended) {
        assertThat(record).isNotNull();
        assertThat(record.get("name").asString()).isEqualTo(name);
        assertThat(record.get("type_id").asLong()).as("type_id of %s", name).isEqualTo(typeId);
        assertThat(record.has("parent_id")).as("parent_id of %s present", name).isTrue();
        if (parentId == null) {
            assertThat(record.get("parent_id").isNull()).as("parent_id of %s", name).isTrue();
        } else {
            assertThat(record.get("parent_id").asLong()).as("parent_id of %s", name).isEqualTo(parentId);
        }
        assertThat(record.get("jurisdiction_id").asLong()).as("jurisdiction_id of %s", name)
                .isEqualTo(jurisdictionId);
        if (ended) {
            assertThat(record.get("end_date").asString()).as("end_date of %s", name).isEqualTo(END_DATE);
        } else {
            assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        }
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name)
                .isEqualTo(ended ? ENDED_UPDATED_AT : OPEN_UPDATED_AT);
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
