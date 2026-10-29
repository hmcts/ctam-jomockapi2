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
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.BASE_LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.GENDERS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.JURISDICTIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATION_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 009's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class JurisdictionsFunctionalTest {

    /**
     * The singular name, which 009 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/jurisdiction";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";

    /**
     * Every record's fields, in order: 002's, with no declared field.
     */
    private static final List<String> FIELDS = List.of("id", "name", "created_at", "updated_at", "start_date",
                                                       "end_date");

    /**
     * One row of the spec's Records table.
     */
    private record Row(long id, String name) {
    }

    /**
     * The whole default dataset (data-model.md § 3), in id order.
     */
    private static final List<Row> RECORDS = List.of(
            new Row(30, "Courts"),
            new Row(34, "Tribunals"),
            new Row(84, "Magistrates"),
            new Row(86, "Coroners"),
            new Row(87, "Skills"));

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll5JurisdictionsInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(JURISDICTIONS);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(5);
        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            // So no reference, boolean or text field either.
            assertThat(record.propertyNames()).containsExactlyElementsOf(FIELDS);
        });
        assertThat(ids).containsExactly(30L, 34L, 84L, 86L, 87L);
    }

    @Test
    void ac002RecordsMatchTheRecordsTable() {
        JsonNode results = json(http.get(JURISDICTIONS)).get("results");

        for (int position = 1; position <= RECORDS.size(); position++) {
            Row row = RECORDS.get(position - 1);
            JsonNode record = results.get(position - 1);
            assertThat(record.get("id").asLong()).as("id at position %d", position).isEqualTo(row.id());
            assertRecord(record, row);
        }
    }

    @Test
    void ac003NoJurisdictionIsEnded() {
        ResponseEntity<String> response = http.get(JURISDICTIONS);

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(response.getBody().split("\"end_date\":null", -1)).hasSize(6);
        assertThat(json(response).get("results")).allSatisfy(record -> {
            assertThat(record.get("end_date").isNull()).isTrue();
            assertThat(record.get("updated_at").asString()).isEqualTo(OPEN_UPDATED_AT);
        });
    }

    @Test
    void ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("jurisdictions.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(JURISDICTIONS).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {30, 34, 84, 86, 87})
    void ac005ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(JURISDICTIONS + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(JURISDICTIONS)).get("results")).get(id));
        assertRecord(record, RECORDS.stream().filter(row -> row.id() == id).findFirst().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "31", "88", "999", "999999"})
    void ac006UnknownIdsReturn404(String id) {
        assertError(http.get(JURISDICTIONS + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac007MalformedIdsReturn400(String id) {
        assertError(http.get(JURISDICTIONS + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac008IdsAreScopedToTheirType() {
        assertThat(http.nameOf(JURISDICTIONS + "/84")).isEqualTo("Magistrates");
        assertThat(http.nameOf(LOCATIONS + "/84")).isEqualTo("Central Devon Sub Committee");
        assertThat(http.nameOf(JURISDICTIONS + "/30")).isEqualTo("Courts");
        assertThat(http.nameOf(GENDERS + "/30")).isEqualTo("Other");
    }

    // US3: every jurisdiction reference resolves. AC-010 is ReferenceDataReferencesTest's completeness check.

    @ParameterizedTest
    @ValueSource(strings = {LOCATIONS, BASE_LOCATIONS, LOCATION_TYPES})
    void ac009EveryJurisdictionIdResolves(String path) {
        Set<Long> jurisdictionIds = byId(json(http.get(JURISDICTIONS)).get("results")).keySet();
        Map<String, Integer> counts = Map.of(LOCATIONS, 2000, BASE_LOCATIONS, 1462, LOCATION_TYPES, 16);

        JsonNode results = json(http.get(path)).get("results");

        // So an empty response can't pass.
        assertThat(results.size()).isEqualTo(counts.get(path));
        results.forEach(record -> assertThat(jurisdictionIdOf(path, record)).isIn(jurisdictionIds));
    }

    @Test
    void ac011SkillsIsServedButUnreferenced() {
        assertThat(http.nameOf(JURISDICTIONS + "/87")).isEqualTo("Skills");

        for (String path : List.of(LOCATIONS, BASE_LOCATIONS, LOCATION_TYPES)) {
            JsonNode results = json(http.get(path)).get("results");
            assertThat(results).isNotEmpty();
            results.forEach(record -> assertThat(jurisdictionIdOf(path, record)).isNotEqualTo(87L));
        }
    }

    // US4: the singular name is not served.

    @Test
    void ac012TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"30", "31", "abc"})
    void ac013TheSingularSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(SINGULAR + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SINGULAR, SINGULAR + "/30"})
    void ac014AuthenticationIsCheckedBeforeTheSingularName(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US5: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {JURISDICTIONS, JURISDICTIONS + "/84", JURISDICTIONS + "/31", JURISDICTIONS + "/abc"})
    void ac015MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac016InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(JURISDICTIONS, JURISDICTIONS + "/84", JURISDICTIONS + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US6: behaviour inherited from 002. AC-018 is covered by ReferenceDataFunctionalTest's unsupported names.

    @ParameterizedTest
    @ValueSource(strings = {JURISDICTIONS + "?name=Courts", JURISDICTIONS + "?page=2", JURISDICTIONS + "?x=",
        JURISDICTIONS + "/84?name=Courts"})
    void ac019QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {JURISDICTIONS + "/", JURISDICTIONS + "/84/", JURISDICTIONS + "/84/extra"})
    void ec009UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec009OtherMethodsReturn405(String method) {
        for (String path : List.of(JURISDICTIONS, JURISDICTIONS + "/84")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {JURISDICTIONS, JURISDICTIONS + "/84"})
    void ec009UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec009LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(JURISDICTIONS + "/084");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(84);
        assertError(http.get(JURISDICTIONS + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    /**
     * The record's {@code jurisdiction_id}, failing with the record named if it is absent or not a whole number.
     */
    private static long jurisdictionIdOf(String path, JsonNode record) {
        JsonNode value = record.path("jurisdiction_id");
        assertThat(value.isIntegralNumber()).as("%s %d: jurisdiction_id %s", path, record.get("id").asLong(), value)
                .isTrue();
        return value.asLong();
    }

    private static void assertRecord(JsonNode record, Row row) {
        String name = row.name() + " (" + row.id() + ")";
        assertThat(record).as("record %s", name).isNotNull();
        assertThat(record.get("name").asString()).as("name of %s", name).isEqualTo(row.name());
        assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name).isEqualTo(OPEN_UPDATED_AT);
        assertThat(record.get("created_at").asString()).as("created_at of %s", name)
                .isEqualTo("2024-01-15T09:00:00Z");
        assertThat(record.get("start_date").asString()).as("start_date of %s", name).isEqualTo("2024-01-01");
    }
}
