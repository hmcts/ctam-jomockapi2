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
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.CONTRACT_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 005's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class GendersFunctionalTest {

    /**
     * The deprecated alias, which 005 deliberately doesn't serve.
     */
    private static final String ALIAS = "/api/v1/reference_data/gender";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll4GendersInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(GENDERS);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();

        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_FIELDS);
        });
        assertThat(ids).containsExactly(10L, 20L, 30L, 40L);
    }

    @Test
    void ac002RecordsMatchTheReferencePoints() {
        JsonNode results = json(http.get(GENDERS)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        assertRecord(byId.get(10L), "Female");
        assertRecord(byId.get(20L), "Male");
        assertRecord(byId.get(30L), "Other");
        assertRecord(byId.get(40L), "Prefer not to say");
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    // ac002 already checks every end_date is null and each name through the parsed JSON; this checks the raw body.
    @Test
    void ac003NoGenderIsEndedAndNamesAreServedExactly() {
        String body = http.get(GENDERS).getBody();

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(body.split("\"end_date\":null", -1)).hasSize(5);
        assertThat(body).contains("\"name\":\"Prefer not to say\"");
    }

    @Test
    void ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("genders.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(GENDERS).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {10, 20, 30, 40})
    void ac005ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(GENDERS + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(GENDERS)).get("results")).get(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15", "50", "999999"})
    void ac006UnknownIdsReturn404(String id) {
        assertError(http.get(GENDERS + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac007MalformedIdsReturn400(String id) {
        assertError(http.get(GENDERS + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac008IdsAreScopedToTheirType() {
        assertThat(http.nameOf(GENDERS + "/10")).isEqualTo("Female");
        assertThat(http.nameOf(APPOINTMENT_TITLES + "/10")).isEqualTo("Acting Senior Coroner");
        assertThat(http.nameOf(BASE_LOCATIONS + "/10")).isEqualTo("Aberconwy");
        assertThat(http.nameOf(CONTRACT_TYPES + "/10")).isEqualTo("Fee-paid");
    }

    // US3: the deprecated alias is not served.

    @Test
    void ac009TheAliasCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(ALIAS), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20", "15", "abc"})
    void ac010TheAliasSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(ALIAS + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {ALIAS, ALIAS + "/20"})
    void ac011AuthenticationIsCheckedBeforeTheAlias(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US4: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {GENDERS, GENDERS + "/20", GENDERS + "/15", GENDERS + "/abc"})
    void ac012MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac013InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(GENDERS, GENDERS + "/20", GENDERS + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US5: behaviour inherited from 002.

    @ParameterizedTest
    @ValueSource(strings = {GENDERS + "?name=Male", GENDERS + "?page=2", GENDERS + "?x=", GENDERS + "/20?name=Male"})
    void ac016QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {GENDERS + "/", GENDERS + "/20/", GENDERS + "/20/extra"})
    void ec009UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec009OtherMethodsReturn405(String method) {
        for (String path : List.of(GENDERS, GENDERS + "/20")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {GENDERS, GENDERS + "/20"})
    void ec009UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec009LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(GENDERS + "/020");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(20);
        assertError(http.get(GENDERS + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    /**
     * Every gender is open (spec EC-004).
     */
    private static void assertRecord(JsonNode record, String name) {
        assertThat(record).isNotNull();
        assertThat(record.get("name").asString()).isEqualTo(name);
        assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name).isEqualTo(OPEN_UPDATED_AT);
    }
}
