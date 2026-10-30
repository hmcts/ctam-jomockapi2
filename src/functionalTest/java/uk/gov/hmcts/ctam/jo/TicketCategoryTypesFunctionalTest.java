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
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.JURISDICTIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATION_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.TICKET_CATEGORIES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.TICKET_CATEGORY_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.byId;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.integralField;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * 012's acceptance scenarios, end to end over real HTTP. Test names start with the scenario they cover.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class TicketCategoryTypesFunctionalTest {

    /**
     * The singular name, which 012 doesn't serve: deprecated names are out of scope.
     */
    private static final String SINGULAR = "/api/v1/reference_data/ticket_category_type";

    private static final String OPEN_UPDATED_AT = "2024-06-03T10:30:00Z";

    /**
     * Every record's fields, in order: 002's, with the declared reference field and text field after {@code name}.
     */
    private static final List<String> FIELDS = List.of("id", "name", "jurisdiction_id", "integration_code",
                                                       "created_at", "updated_at", "start_date", "end_date");

    /**
     * One row of the spec's Records table.
     */
    private record Row(long id, String name, long jurisdictionId, String integrationCode) {
    }

    /**
     * The whole default dataset (data-model.md § 3), in id order.
     */
    private static final List<Row> RECORDS = List.of(
            new Row(1, "Court Type", 30, "COURTS_COURT_TYPE"),
            new Row(2, "Tribunal Type", 34, "TRIBS_TYPE"),
            new Row(3, "Tribunal Name", 34, "TRIBS_NAME"),
            new Row(4, "Court Type", 84, "MAGS_COURT_TYPE"),
            new Row(5, "Skills Category", 87, "SKILLS_CATEGORY"));

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    // US1: the collection.

    @Test
    void ac001ReturnsAll5TicketCategoryTypesInAscendingIdOrder() {
        ResponseEntity<String> response = http.get(TICKET_CATEGORY_TYPES);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode results = json(response).get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(5);
        List<Long> ids = new ArrayList<>();
        results.forEach(record -> {
            ids.add(record.get("id").asLong());
            assertThat(record.propertyNames()).containsExactlyElementsOf(FIELDS);
        });
        assertThat(ids).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void ac002RecordsMatchTheRecordsTable() {
        JsonNode results = json(http.get(TICKET_CATEGORY_TYPES)).get("results");

        for (int position = 1; position <= RECORDS.size(); position++) {
            Row row = RECORDS.get(position - 1);
            JsonNode record = results.get(position - 1);
            assertThat(record.get("id").asLong()).as("id at position %d", position).isEqualTo(row.id());
            assertRecord(record, row);
        }
    }

    @Test
    void ac003NoTicketCategoryTypeIsEnded() {
        ResponseEntity<String> response = http.get(TICKET_CATEGORY_TYPES);

        // Counted on the raw body, so a formatting change fails here instead of passing silently.
        assertThat(response.getBody().split("\"end_date\":null", -1)).hasSize(6);
        assertThat(json(response).get("results")).allSatisfy(record -> {
            assertThat(record.get("end_date").isNull()).isTrue();
            assertThat(record.get("updated_at").asString()).isEqualTo(OPEN_UPDATED_AT);
        });
    }

    @Test
    void ac004CourtTypeIsTheOnlyRepeatedName() {
        JsonNode results = json(http.get(TICKET_CATEGORY_TYPES)).get("results");
        Set<String> names = new HashSet<>();
        Set<String> codes = new HashSet<>();
        Map<Long, String> courtTypes = new TreeMap<>();
        results.forEach(record -> {
            names.add(record.get("name").asString());
            codes.add(record.get("integration_code").asString());
            if ("Court Type".equals(record.get("name").asString())) {
                courtTypes.put(integralField("type", record, "jurisdiction_id"),
                               record.get("integration_code").asString());
            }
        });

        assertThat(names).hasSize(4);
        assertThat(courtTypes).containsExactly(Map.entry(30L, "COURTS_COURT_TYPE"), Map.entry(84L, "MAGS_COURT_TYPE"));
        assertThat(byId(results).get(1L).get("name").asString()).isEqualTo("Court Type");
        assertThat(byId(results).get(4L).get("name").asString()).isEqualTo("Court Type");
        // A fact about the data (EC-003), not a requirement.
        assertThat(codes).hasSize(5);
    }

    @Test
    void ac005RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
        String golden = golden("ticket_category_types.json");
        for (int i = 0; i < 5; i++) {
            assertThat(http.get(TICKET_CATEGORY_TYPES).getBody()).isEqualTo(golden);
        }
    }

    // US2: a single record.

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5})
    void ac006ReturnsTheRecordEqualToTheCollectionElement(long id) {
        ResponseEntity<String> response = http.get(TICKET_CATEGORY_TYPES + "/" + id);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode record = json(response);
        assertThat(record.has("results")).isFalse();
        assertThat(record).isEqualTo(byId(json(http.get(TICKET_CATEGORY_TYPES)).get("results")).get(id));
        assertRecord(record, RECORDS.stream().filter(row -> row.id() == id).findFirst().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"6", "30", "31", "999999"})
    void ac007UnknownIdsReturn404(String id) {
        assertError(http.get(TICKET_CATEGORY_TYPES + "/" + id), 404, RECORD_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
    void ac008MalformedIdsReturn400(String id) {
        assertError(http.get(TICKET_CATEGORY_TYPES + "/" + id), 400, MALFORMED_REFERENCE_ID);
    }

    @Test
    void ac009IdsAreScopedToTheirType() {
        assertThat(http.nameOf(TICKET_CATEGORY_TYPES + "/1")).isEqualTo("Court Type");
        assertThat(http.nameOf(LOCATION_TYPES + "/1")).isEqualTo("Region");
        assertThat(http.nameOf(TICKET_CATEGORY_TYPES + "/4")).isEqualTo("Court Type");
        assertThat(http.nameOf(LOCATION_TYPES + "/4")).isEqualTo("Chamber");
    }

    // US3: every ticket category's type resolves. AC-013's completeness half is ReferenceDataReferencesTest's.

    @Test
    void ac010EveryTicketCategoryTypeIdResolves() {
        Set<Long> typeIds = byId(json(http.get(TICKET_CATEGORY_TYPES)).get("results")).keySet();
        JsonNode categories = json(http.get(TICKET_CATEGORIES)).get("results");
        Map<Long, Integer> counts = new TreeMap<>();

        // So an empty response can't pass.
        assertThat(categories.size()).isEqualTo(54);
        categories.forEach(record -> {
            long typeId = integralField("category", record, "type_id");
            assertThat(typeId).as("type_id of category %d", record.get("id").asLong()).isIn(typeIds);
            counts.merge(typeId, 1, Integer::sum);
        });
        assertThat(counts).isEqualTo(Map.of(1L, 3, 2L, 3, 3L, 46, 4L, 1, 5L, 1));
    }

    @Test
    void ac011EveryTicketCategoryIsInItsTypesJurisdiction() {
        Map<Long, JsonNode> types = byId(json(http.get(TICKET_CATEGORY_TYPES)).get("results"));
        JsonNode categories = json(http.get(TICKET_CATEGORIES)).get("results");

        assertThat(categories.size()).isEqualTo(54);
        categories.forEach(record -> {
            JsonNode type = types.get(integralField("category", record, "type_id"));
            assertThat(type).as("type of category %d", record.get("id").asLong()).isNotNull();
            assertThat(integralField("category", record, "jurisdiction_id"))
                    .as("jurisdiction_id of category %d", record.get("id").asLong())
                    .isEqualTo(integralField("type", type, "jurisdiction_id"));
        });
    }

    @Test
    void ac012EveryTypeJurisdictionResolves() {
        Set<Long> jurisdictionIds = byId(json(http.get(JURISDICTIONS)).get("results")).keySet();
        Map<Long, JsonNode> types = byId(json(http.get(TICKET_CATEGORY_TYPES)).get("results"));

        assertThat(types).hasSize(5);
        types.values().forEach(record -> assertThat(integralField("type", record, "jurisdiction_id"))
                .isIn(jurisdictionIds));
        // Skills (87) is referenced here as well as by ticket category 88 (AC-013).
        assertThat(integralField("type", types.get(5L), "jurisdiction_id")).isEqualTo(87L);
        assertThat(http.nameOf(JURISDICTIONS + "/87")).isEqualTo("Skills");
    }

    // US4: the singular name is not served.

    @Test
    void ac014TheSingularCollectionIsUnsupported() {
        JsonNode body = assertError(http.get(SINGULAR), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("results")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "6", "abc"})
    void ac015TheSingularSingleRecordRouteIsUnsupported(String id) {
        JsonNode body = assertError(http.get(SINGULAR + "/" + id), 400, UNSUPPORTED_ATTRIBUTE_NAME);

        assertThat(body.has("id")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SINGULAR, SINGULAR + "/1"})
    void ac016AuthenticationIsCheckedBeforeTheSingularName(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
        assertUnauthorized(http.send(HttpMethod.GET, path, "Bearer wrong-token"));
    }

    // US5: requests without valid credentials.

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORY_TYPES, TICKET_CATEGORY_TYPES + "/4", TICKET_CATEGORY_TYPES + "/6",
        TICKET_CATEGORY_TYPES + "/abc"})
    void ac017MissingTokenIsRejected(String path) {
        assertUnauthorized(http.send(HttpMethod.GET, path, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer wrong-token", "Basic dGVzdA==", "Bearer "})
    void ac018InvalidTokenIsRejectedWithTheSameMessage(String authorization) {
        for (String path : List.of(TICKET_CATEGORY_TYPES, TICKET_CATEGORY_TYPES + "/4",
                                   TICKET_CATEGORY_TYPES + "/abc")) {
            assertUnauthorized(http.send(HttpMethod.GET, path, authorization));
        }
    }

    // US6: behaviour inherited from 002. AC-020 is covered by ReferenceDataFunctionalTest's unsupported names.

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORY_TYPES + "?jurisdiction_id=30", TICKET_CATEGORY_TYPES + "?page=2",
        TICKET_CATEGORY_TYPES + "?x=", TICKET_CATEGORY_TYPES + "/4?name=Court%20Type"})
    void ac021QueryParametersReturn400(String path) {
        assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORY_TYPES + "/", TICKET_CATEGORY_TYPES + "/4/",
        TICKET_CATEGORY_TYPES + "/4/extra"})
    void ec010UnmatchedPathsReturn404(String path) {
        assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void ec010OtherMethodsReturn405(String method) {
        for (String path : List.of(TICKET_CATEGORY_TYPES, TICKET_CATEGORY_TYPES + "/4")) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);
            assertThat(body.has("results")).as(path).isFalse();
            assertThat(body.has("id")).as(path).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {TICKET_CATEGORY_TYPES, TICKET_CATEGORY_TYPES + "/4"})
    void ec010UnacceptableMediaTypesReturn406(String path) {
        JsonNode body = assertError(http.getAccepting(path, "application/xml"), 406, NOT_ACCEPTABLE);
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    @Test
    void ec010LeadingZerosAndOversizedIds() {
        ResponseEntity<String> response = http.get(TICKET_CATEGORY_TYPES + "/004");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("id").asLong()).isEqualTo(4);
        assertError(http.get(TICKET_CATEGORY_TYPES + "/99999999999999999999"), 404, RECORD_NOT_FOUND);
    }

    private static void assertRecord(JsonNode record, Row row) {
        String name = row.name() + " (" + row.id() + ")";
        assertThat(record).as("record %s", name).isNotNull();
        assertThat(record.get("name").asString()).as("name of %s", name).isEqualTo(row.name());
        assertThat(integralField(name, record, "jurisdiction_id")).as("jurisdiction_id of %s", name)
                .isEqualTo(row.jurisdictionId());
        assertThat(record.get("integration_code").asString()).as("integration_code of %s", name)
                .isEqualTo(row.integrationCode());
        assertThat(record.get("end_date").isNull()).as("end_date of %s", name).isTrue();
        assertThat(record.get("updated_at").asString()).as("updated_at of %s", name).isEqualTo(OPEN_UPDATED_AT);
        assertThat(record.get("created_at").asString()).as("created_at of %s", name)
                .isEqualTo("2024-01-15T09:00:00Z");
        assertThat(record.get("start_date").asString()).as("start_date of %s", name).isEqualTo("2024-01-01");
    }
}
