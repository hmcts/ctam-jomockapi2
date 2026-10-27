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
import java.util.List;
import java.util.Map;
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
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.CONTRACT_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 006's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class JudiciaryRolesFunctionalTest {

    /**
     * The singular name, which 006 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/judiciary_role";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";
    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";
    private static final String END_DATE = "2025-03-31";

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll164JudiciaryRolesInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(JUDICIARY_ROLES);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();

        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_FIELDS);
        });
        assertThat(ids).containsExactlyElementsOf(LongStream.rangeClosed(1, 164).map(i -> 10 * i).boxed().toList());
    }

    @Test
    void ac002RecordsMatchTheReferencePoints() {
        JsonNode results = json(http.get(JUDICIARY_ROLES)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        assertRecord(byId.get(10L), "Acting Designated Civil Judge", false);
        assertRecord(byId.get(70L), "Acting Senior Costs Judge", true);
        assertRecord(byId.get(140L), "Boundary Commission for England – Deputy Chair", true);
        assertRecord(byId.get(150L), "Boundary Commission for Wales – Deputy Chair", false);
        assertRecord(byId.get(170L), "Business & Property Courts Supervising", false);
        assertRecord(byId.get(340L), "Course Director for COP (JC)", false);
        assertRecord(byId.get(960L), "Judge in Charge of Jury List (King’s Bench)", false);
        assertRecord(byId.get(970L), "Judge in Charge of Jury List (Queen's Bench)", false);
        assertRecord(byId.get(1070L), "Judicial Commissioner Investigatory Powers Commiss", false);
        assertRecord(byId.get(1280L), "Principle Judge - Land Registration", false);
        assertRecord(byId.get(1400L), "Senior Family Liaison Judge", true);
        assertRecord(byId.get(1610L), "Vice President of the Unified Tribunals", true);
        assertRecord(byId.get(1640L), "Wales Training Course Director (JC)", false);
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    @Test
    void ac003NamesAreServedExactly() {
        ResponseEntity<String> response = http.get(JUDICIARY_ROLES);
        String body = response.getBody();
        Map<Long, String> nonAscii = Map.of(
                140L, "Boundary Commission for England – Deputy Chair",
                150L, "Boundary Commission for Wales – Deputy Chair",
                450L, "Deputy Chairman – Advisory Committee",
                580L, "Director of Magistrates’ Training (JC)",
                960L, "Judge in Charge of Jury List (King’s Bench)",
                990L, "Judge in Charge of Non-Jury List (King’s Bench)",
                1180L, "Magistrates’ Liaison Judge");

        // On the raw body, which shows each character as served, not escaped: every non-ASCII name, then ASCII
        // punctuation, the typo, the truncation and near-duplicates.
        nonAscii.values().forEach(name -> assertThat(body).contains("\"name\":\"" + name + "\""));
        assertThat(body).contains(
                "\"name\":\"Judge in Charge of Jury List (Queen's Bench)\"",
                "\"name\":\"Principle Judge - Residential Property\"",
                "\"name\":\"Judicial Commissioner Investigatory Powers Commiss\"",
                "\"name\":\"Vice President Kings Bench\"",
                "\"name\":\"Vice President of King's Bench\"",
                "\"name\":\"Vice President Queens Bench\"");

        // And each non-ASCII name at its id.
        Map<Long, JsonNode> byId = byId(json(response).get("results"));
        nonAscii.forEach((id, name) -> assertThat(byId.get(id).get("name").asString()).as("name of %d", id)
                .isEqualTo(name));
    }

    @Test
    void ac004Exactly23RecordsAreEnded() {
        ResponseEntity<String> response = http.get(JUDICIARY_ROLES);
        String body = response.getBody();
        JsonNode results = json(response).get("results");

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(body.split("\"end_date\":\"2025-03-31\"", -1)).hasSize(24);
        assertThat(body.split("\"end_date\":null", -1)).hasSize(142);

        List<Long> ended = new ArrayList<>();
        results.forEach(record -> {
            if (!record.get("end_date").isNull()) {
                ended.add(record.get("id").asLong());
                assertThat(record.get("updated_at").asString()).isEqualTo(ENDED_UPDATED_AT);
            }
        });
        assertThat(ended).containsExactlyElementsOf(LongStream.rangeClosed(1, 23).map(i -> 70 * i).boxed().toList());

        // End-dated in the extract, open here: the extract's end dates are not reproduced (EC-007).
        Map<Long, JsonNode> byId = byId(results);
        assertRecord(byId.get(1320L), "Regional Judge", false);
        assertRecord(byId.get(1590L), "Vice President Queens Bench", false);
    }

    @Test
    void ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("judiciary_roles.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(JUDICIARY_ROLES).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {10, 70, 150, 960, 970, 1070, 1640})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(JUDICIARY_ROLES + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(JUDICIARY_ROLES)).get("results")).get(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15", "1650", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(JUDICIARY_ROLES + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(JUDICIARY_ROLES + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(JUDICIARY_ROLES + "/10")).isEqualTo("Acting Designated Civil Judge");
        assertThat(http.nameOf(APPOINTMENT_TITLES + "/10")).isEqualTo("Acting Senior Coroner");
        assertThat(http.nameOf(BASE_LOCATIONS + "/10")).isEqualTo("Aberconwy");
        assertThat(http.nameOf(CONTRACT_TYPES + "/10")).isEqualTo("Fee-paid");
        assertThat(http.nameOf(GENDERS + "/10")).isEqualTo("Female");
    }

    // US3: the singular name is not served.

    @Test
    void ac010TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20", "15", "abc"})
    void ac011TheSingularSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(SINGULAR + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SINGULAR, SINGULAR + "/20"})
    void ac012AuthenticationIsCheckedBeforeTheSingularName(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US4: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {JUDICIARY_ROLES, JUDICIARY_ROLES + "/20", JUDICIARY_ROLES + "/15",
        JUDICIARY_ROLES + "/abc"})
    void ac013MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac014InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(JUDICIARY_ROLES, JUDICIARY_ROLES + "/20", JUDICIARY_ROLES + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US5: behaviour inherited from 002. A plain query value, so the test doesn't depend on how the helper encodes
    // %20; Postman and the quickstart use ?name=Mentor%20Judge.

    @ParameterizedTest
    @ValueSource(strings = {JUDICIARY_ROLES + "?name=Judge", JUDICIARY_ROLES + "?page=2", JUDICIARY_ROLES + "?x=",
        JUDICIARY_ROLES + "/20?name=Judge"})
    void ac017QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {JUDICIARY_ROLES + "/", JUDICIARY_ROLES + "/20/", JUDICIARY_ROLES + "/20/extra"})
    void ec012UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec012OtherMethodsReturn405(String method) {
        for (String path : List.of(JUDICIARY_ROLES, JUDICIARY_ROLES + "/20")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {JUDICIARY_ROLES, JUDICIARY_ROLES + "/20"})
    void ec012UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec012LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(JUDICIARY_ROLES + "/020");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(20);
        assertError(http.get(JUDICIARY_ROLES + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    private static void assertRecord(JsonNode record, String name, boolean ended) {
        assertThat(record).isNotNull();
        assertThat(record.get("name").asString()).isEqualTo(name);
        if (ended) {
            assertThat(record.get("end_date").asString()).as("end_date of %s", name).isEqualTo(END_DATE);
        } else {
            assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        }
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name)
                .isEqualTo(ended ? ENDED_UPDATED_AT : OPEN_UPDATED_AT);
    }
}
