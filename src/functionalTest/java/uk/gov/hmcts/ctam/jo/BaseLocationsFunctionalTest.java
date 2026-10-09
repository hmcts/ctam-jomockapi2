package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_REFERENCE_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.QUERY_PARAMETERS_NOT_SUPPORTED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RECORD_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.BASE_LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.CONTRACT_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 003's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class BaseLocationsFunctionalTest {

    /**
     * The deprecated alias, which 003 deliberately doesn't serve.
     */
    private static final String ALIAS = "/api/v1/reference_data/base_location";

    private static final String EN_DASH_NAME = "Royal Courts of Justice – Office of the Judge Advocate General";

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll1462BaseLocationsInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(BASE_LOCATIONS);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(1462);

        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_FIELDS);
        });
        assertThat(ids).isSorted().doesNotHaveDuplicates();
    }

    @Test
    void ac001RepeatedNamesAreSeparateRecords() {
        JsonNode results = json(http.get(BASE_LOCATIONS)).get("results");

        Set<String> names = new HashSet<>();
        List<Long> national = new ArrayList<>();
        List<Long> bedfordshire = new ArrayList<>();
        for (JsonNode record : results) {
            String name = record.get("name").asString();
            names.add(name);
            if ("National".equals(name)) {
                national.add(record.get("id").asLong());
            } else if ("Bedfordshire LJA".equals(name)) {
                bedfordshire.add(record.get("id").asLong());
            }
        }
        assertThat(names).hasSize(1276);
        assertThat(national).hasSize(40).startsWith(7200L).endsWith(7590L);
        assertThat(bedfordshire).containsExactly(540L, 550L);
    }

    @Test
    void ac002ReferencePointsMatchTheDataModel() {
        Map<Long, JsonNode> byId = byId(json(http.get(BASE_LOCATIONS)).get("results"));

        assertRecord(byId.get(10L), "Aberconwy", null, "2024-06-03T10:30:00Z");
        assertRecord(byId.get(70L), "Aldridge and Brownhills", "2025-03-31", "2025-04-01T08:00:00Z");
        assertRecord(byId.get(14620L), "Yorkshire & Humberside", null, "2024-06-03T10:30:00Z");
    }

    @Test
    void ac003TheNonAsciiNameIsServedExactly() {
        ResponseEntity<String> response = http.get(BASE_LOCATIONS);

        assertThat(byId(json(response).get("results")).get(10290L).get("name").asString()).isEqualTo(EN_DASH_NAME);
        assertThat(response.getBody()).doesNotContain("\\u2013");
    }

    @Test
    void ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden();
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(BASE_LOCATIONS).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {10, 70, 540, 550, 10290, 14620})
    void ac005ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(BASE_LOCATIONS + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(BASE_LOCATIONS)).get("results")).get(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15", "8885", "14630", "999999"})
    void ac006UnknownIdsReturn404(String id) {
        assertError(http.get(BASE_LOCATIONS + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac007MalformedIdsReturn400(String id) {
        assertError(http.get(BASE_LOCATIONS + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac008IdsAreScopedToTheirType() {
        assertThat(nameOf(BASE_LOCATIONS + "/10")).isEqualTo("Aberconwy");
        assertThat(nameOf(APPOINTMENT_TITLES + "/10")).isEqualTo("Acting Senior Coroner");
        assertThat(nameOf(BASE_LOCATIONS + "/1940")).isEqualTo("Central Buckinghamshire");
        assertThat(nameOf(APPOINTMENT_TITLES + "/1940")).isEqualTo("Vice-President, Employment Tribunal (Scotland)");
    }

    // US3: the deprecated alias is not served.

    @Test
    void ac009TheAliasCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(ALIAS), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"70", "15", "abc"})
    void ac010TheAliasSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(ALIAS + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {ALIAS, ALIAS + "/70"})
    void ac011AuthenticationIsCheckedBeforeTheAlias(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US4: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {BASE_LOCATIONS, BASE_LOCATIONS + "/70", BASE_LOCATIONS + "/abc"})
    void ac012MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac013InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(BASE_LOCATIONS, BASE_LOCATIONS + "/70", BASE_LOCATIONS + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US5: behaviour inherited from 002.

    @ParameterizedTest
    @ValueSource(strings = {BASE_LOCATIONS + "?name=Aberconwy", BASE_LOCATIONS + "?page=2", BASE_LOCATIONS + "?x=",
        BASE_LOCATIONS + "/70?name=Aberconwy", BASE_LOCATIONS + "/70?page=2", BASE_LOCATIONS + "/70?x="})
    void ac016QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {BASE_LOCATIONS + "/", BASE_LOCATIONS + "/70/", BASE_LOCATIONS + "/70/extra"})
    void ec008UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec008OtherMethodsReturn405(String method) {
        for (String path : List.of(BASE_LOCATIONS, BASE_LOCATIONS + "/70")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @Test
    void ec008LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(BASE_LOCATIONS + "/070");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(70);
        assertError(http.get(BASE_LOCATIONS + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    /**
     * The shared 401, and no reference data in the body.
     */
    private static void assertUnauthorized(ResponseEntity<String> response) {
        JsonNode body = assertError(response, 401, UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    private static void assertRecord(JsonNode record, String name, String endDate, String updatedAt) {
        assertThat(record).isNotNull();
        assertThat(record.get("name").asString()).isEqualTo(name);
        if (endDate == null) {
            assertThat(record.get("end_date").isNull()).isTrue();
        } else {
            assertThat(record.get("end_date").asString()).isEqualTo(endDate);
        }
        assertThat(record.get("updated_at").asString()).isEqualTo(updatedAt);
    }

    private static Map<Long, JsonNode> byId(JsonNode results) {
        Map<Long, JsonNode> byId = new HashMap<>();
        results.forEach(record -> byId.put(record.get("id").asLong(), record));
        return byId;
    }

    private String nameOf(String path) {
        ResponseEntity<String> response = http.get(path);
        assertThat(response.getStatusCode().value()).as(path).isEqualTo(200);
        return json(response).get("name").asString();
    }

    private String golden() throws IOException {
        try (InputStream in = Objects.requireNonNull(
                getClass().getResourceAsStream("/golden/base_locations.json"), "golden file missing")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
