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
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 004's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class ContractTypesFunctionalTest {

    /**
     * The deprecated alias, which 004 deliberately doesn't serve.
     */
    private static final String ALIAS = "/api/v1/reference_data/contract_type";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";

    private static final String ENDED_UPDATED_AT = "2025-04-01T08:00:00Z";

    private static final String END_DATE = "2025-03-31";

    /**
     * 002's fields, with the boolean field {@code salaried} after {@code name} (spec FR-005).
     */
    private static final List<String> CONTRACT_TYPE_FIELDS = List.of("id", "name", "salaried", "created_at",
            "updated_at", "start_date", "end_date");

    private static final List<String> SALARIED_NAMES = List.of("SPTW-10%", "SPTW-20%", "SPTW-30%", "SPTW-40%",
            "SPTW-50%", "SPTW-60%", "SPTW-70%", "SPTW-75%", "SPTW-80%", "SPTW-90%", "Salaried");

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll14ContractTypesInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(CONTRACT_TYPES);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();

        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_TYPE_FIELDS);
        });
        assertThat(ids).containsExactly(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 110L, 120L, 130L, 140L);
    }

    @Test
    void ac002ReferencePointsMatchTheDataModel() {
        JsonNode results = json(http.get(CONTRACT_TYPES)).get("results");
        Map<Long, JsonNode> byId = byId(results);

        assertRecord(byId.get(10L), "Fee-paid", false, null, OPEN_UPDATED_AT);
        assertRecord(byId.get(20L), "SPTW-10%", true, null, OPEN_UPDATED_AT);
        assertRecord(byId.get(70L), "SPTW-60%", true, END_DATE, ENDED_UPDATED_AT);
        assertRecord(byId.get(120L), "Salaried", true, null, OPEN_UPDATED_AT);
        assertRecord(byId.get(130L), "Unknown", false, null, OPEN_UPDATED_AT);
        assertRecord(byId.get(140L), "Voluntary", false, END_DATE, ENDED_UPDATED_AT);
        assertThat(results).allSatisfy(record -> {
            assertThat(record.get("created_at").asString()).isEqualTo("2024-01-15T09:00:00Z");
            assertThat(record.get("start_date").asString()).isEqualTo("2024-01-01");
        });
    }

    @Test
    void ac003SalariedIsABooleanOnEveryRecord() {
        List<String> salaried = new ArrayList<>();
        List<String> notSalaried = new ArrayList<>();
        for (JsonNode record : json(http.get(CONTRACT_TYPES)).get("results")) {
            assertThat(record.get("salaried").isBoolean()).as("salaried of %s", record.get("id")).isTrue();
            if (record.get("salaried").asBoolean()) {
                salaried.add(record.get("name").asString());
            } else {
                notSalaried.add(record.get("name").asString());
            }
        }

        assertThat(salaried).containsExactlyElementsOf(SALARIED_NAMES);
        assertThat(notSalaried).containsExactly("Fee-paid", "Unknown", "Voluntary");
    }

    @Test
    void ac004RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("contract_types.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(CONTRACT_TYPES).getBody()).isEqualTo(golden);
        }
    }

    @Test
    void ac005NamesWithPercentAndHyphensAreServedExactly() {
        ResponseEntity<String> response = http.get(CONTRACT_TYPES);
        Map<Long, JsonNode> byId = byId(json(response).get("results"));

        assertThat(byId.get(90L).get("name").asString()).isEqualTo("SPTW-75%");
        assertThat(byId.get(10L).get("name").asString()).isEqualTo("Fee-paid");
        assertThat(response.getBody()).contains("\"SPTW-75%\"");
    }

    @Test
    void ec004TheExtractsEndDatesAreNotReproduced() {
        Map<Long, JsonNode> byId = byId(json(http.get(CONTRACT_TYPES)).get("results"));

        // Ended in the extract, open here: 002's position rule decides which records are ended.
        assertThat(byId.get(90L).get("end_date").isNull()).as("SPTW-75%").isTrue();
        assertThat(byId.get(130L).get("end_date").isNull()).as("Unknown").isTrue();
        assertThat(byId.get(70L).get("end_date").asString()).isEqualTo(END_DATE);
        assertThat(byId.get(140L).get("end_date").asString()).isEqualTo(END_DATE);
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {10, 20, 70, 120, 130, 140})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(CONTRACT_TYPES + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record.get("salaried").isBoolean()).isTrue();
        assertThat(record).isEqualTo(byId(json(http.get(CONTRACT_TYPES)).get("results")).get(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15", "150", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(CONTRACT_TYPES + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(CONTRACT_TYPES + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(CONTRACT_TYPES + "/10")).isEqualTo("Fee-paid");
        assertThat(http.nameOf(APPOINTMENT_TITLES + "/10")).isEqualTo("Acting Senior Coroner");
        assertThat(http.nameOf(BASE_LOCATIONS + "/10")).isEqualTo("Aberconwy");
    }

    // US3: the deprecated alias is not served.

    @Test
    void ac010TheAliasCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(ALIAS), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"70", "15", "abc"})
    void ac011TheAliasSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(ALIAS + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {ALIAS, ALIAS + "/70"})
    void ac012AuthenticationIsCheckedBeforeTheAlias(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US4: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {CONTRACT_TYPES, CONTRACT_TYPES + "/120", CONTRACT_TYPES + "/15", CONTRACT_TYPES + "/abc"})
    void ac013MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac014InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(CONTRACT_TYPES, CONTRACT_TYPES + "/120", CONTRACT_TYPES + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US5: existing behaviour is unchanged, and behaviour inherited from 002.

    @Test
    void ac015OtherTypesNeverCarrySalaried() {
        for (String path : List.of(APPOINTMENT_TITLES, BASE_LOCATIONS)) {
            assertThat(json(http.get(path)).get("results"))
                    .as(path)
                    .allSatisfy(record -> assertThat(record.has("salaried")).isFalse());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {CONTRACT_TYPES + "?salaried=true", CONTRACT_TYPES + "?name=Fee-paid",
        CONTRACT_TYPES + "?x=", CONTRACT_TYPES + "/120?salaried=true"})
    void ac017QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {CONTRACT_TYPES + "/", CONTRACT_TYPES + "/120/", CONTRACT_TYPES + "/120/extra"})
    void ec009UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec009OtherMethodsReturn405(String method) {
        for (String path : List.of(CONTRACT_TYPES, CONTRACT_TYPES + "/120")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {CONTRACT_TYPES, CONTRACT_TYPES + "/120"})
    void ec009UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec009LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(CONTRACT_TYPES + "/0120");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(120);
        assertError(http.get(CONTRACT_TYPES + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    private static void assertRecord(JsonNode record, String name, boolean salaried, String endDate,
                                     String updatedAt) {
        assertThat(record).isNotNull();
        assertThat(record.get("name").asString()).isEqualTo(name);
        assertThat(record.get("salaried").asBoolean()).as("salaried of %s", name).isEqualTo(salaried);
        if (endDate == null) {
            assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        } else {
            assertThat(record.get("end_date").asString()).as("end_date of %s", name).isEqualTo(endDate);
        }
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name).isEqualTo(updatedAt);
    }
}
