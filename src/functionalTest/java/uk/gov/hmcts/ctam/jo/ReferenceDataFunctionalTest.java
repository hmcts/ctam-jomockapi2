package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_REFERENCE_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.METHOD_NOT_ALLOWED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.QUERY_PARAMETERS_NOT_SUPPORTED;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RECORD_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNSUPPORTED_ATTRIBUTE_NAME;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.CONTRACT_FIELDS;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertError;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.assertUnauthorized;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.golden;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataHttp.json;

/**
 * The spec's acceptance scenarios, end to end over real HTTP. One nested class per user story.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class ReferenceDataFunctionalTest {

    @LocalServerPort
    private int port;

    private final ReferenceDataHttp http = new ReferenceDataHttp(() -> port);

    @Nested
    class CollectionTests {

        @Test
        void ac001ReturnsAll194TitlesInAscendingIdOrder() {
            ResponseEntity<String> response = http.get(APPOINTMENT_TITLES);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().getContentType()).hasToString("application/json");
            JsonNode results = json(response).get("results");
            assertThat(results.isArray()).isTrue();
            assertThat(results.size()).isEqualTo(194);

            List<Long> ids = new ArrayList<>();
            results.forEach(record -> ids.add(record.get("id").asLong()));
            assertThat(ids).isSorted().doesNotHaveDuplicates();

            assertRecord(results, 10, "Acting Senior Coroner", null);
            assertRecord(results, 70, "Area Coroner", "2025-03-31");
            assertRecord(results, 1940, "Vice-President, Employment Tribunal (Scotland)", null);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/v5/reference_data/appointment_titles", "/reference_data/appointment_titles"})
        void ac012OtherBasePathsReturn404(String path) {
            assertThat(http.get(path).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        void ac015RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
            String golden = golden("appointment_titles.json");
            for (int i = 0; i < 100; i++) {
                assertThat(http.get(APPOINTMENT_TITLES).getBody()).isEqualTo(golden);
            }
        }

        @Test
        void everyRecordHasExactlyTheSixContractFields() {
            json(http.get(APPOINTMENT_TITLES)).get("results").forEach(record ->
                    assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_FIELDS));
        }

        private void assertRecord(JsonNode results, long id, String name, String endDate) {
            JsonNode record = null;
            for (JsonNode candidate : results) {
                if (candidate.get("id").asLong() == id) {
                    record = candidate;
                }
            }
            assertThat(record).as("record %d", id).isNotNull();
            assertThat(record.get("name").asString()).isEqualTo(name);
            if (endDate == null) {
                assertThat(record.get("end_date").isNull()).isTrue();
            } else {
                assertThat(record.get("end_date").asString()).isEqualTo(endDate);
            }
        }
    }

    @Nested
    class SingleRecordTests {

        @Test
        void ac004ReturnsTheSingleRecordEqualToTheCollectionElement() {
            ResponseEntity<String> response = http.get(APPOINTMENT_TITLES + "/70");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().getContentType()).hasToString("application/json");
            JsonNode record = json(response);
            assertThat(record.has("results")).isFalse();
            assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_FIELDS);
            assertThat(record.get("name").asString()).isEqualTo("Area Coroner");
            assertThat(record.get("end_date").asString()).isEqualTo("2025-03-31");

            JsonNode fromCollection = null;
            for (JsonNode candidate : json(http.get(APPOINTMENT_TITLES)).get("results")) {
                if (candidate.get("id").asLong() == 70) {
                    fromCollection = candidate;
                }
            }
            assertThat(record).isEqualTo(fromCollection);
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
        void ac008MalformedIdsReturn400(String id) {
            assertError(http.get(APPOINTMENT_TITLES + "/" + id), 400, MALFORMED_REFERENCE_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"15", "999999", "007", "99999999999999999999"})
        void ac009AndEc003UnknownIdsReturn404(String id) {
            assertError(http.get(APPOINTMENT_TITLES + "/" + id), 404, RECORD_NOT_FOUND);
        }

        @Test
        void ec003LeadingZerosAreAccepted() {
            ResponseEntity<String> response = http.get(APPOINTMENT_TITLES + "/010");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).get("id").asLong()).isEqualTo(10);
        }

        @ParameterizedTest
        @ValueSource(strings = {APPOINTMENT_TITLES + "?page=2", APPOINTMENT_TITLES + "/70?name=Judge"})
        void ac020QueryParametersReturn400(String path) {
            assertError(http.get(path), 400, QUERY_PARAMETERS_NOT_SUPPORTED);
        }

        @ParameterizedTest
        @ValueSource(strings = {APPOINTMENT_TITLES + "/", APPOINTMENT_TITLES + "/70/", APPOINTMENT_TITLES + "/1/extra",
            APPOINTMENT_TITLES + "/?page=2", APPOINTMENT_TITLES + "/1/extra?x=1"})
        void ec002Ec004Ec005UnmatchedRoutesReturn404BeforeQueryChecks(String path) {
            assertError(http.get(path), 404, RESOURCE_NOT_FOUND);
        }

        @ParameterizedTest
        @CsvSource({
            "POST, " + APPOINTMENT_TITLES, "PUT, " + APPOINTMENT_TITLES,
            "PATCH, " + APPOINTMENT_TITLES, "DELETE, " + APPOINTMENT_TITLES,
            "POST, " + APPOINTMENT_TITLES + "/70", "PUT, " + APPOINTMENT_TITLES + "/70",
            "PATCH, " + APPOINTMENT_TITLES + "/70", "DELETE, " + APPOINTMENT_TITLES + "/70",
            "POST, /api/v1/reference_data/appointment_title"
        })
        void ec007OtherMethodsReturn405(String method, String path) {
            JsonNode body = assertError(http.send(HttpMethod.valueOf(method), path), 405, METHOD_NOT_ALLOWED);

            assertThat(body.has("results")).isFalse();
            assertThat(body.has("id")).isFalse();
        }
    }

    @Nested
    class AuthenticationTests {

        @ParameterizedTest
        @CsvSource({
            "GET, " + APPOINTMENT_TITLES, "GET, " + APPOINTMENT_TITLES + "/70",
            "GET, /api/v1/reference_data/jurisdictions", "GET, " + APPOINTMENT_TITLES + "/abc",
            "GET, " + APPOINTMENT_TITLES + "?page=2", "GET, " + APPOINTMENT_TITLES + "/",
            "POST, " + APPOINTMENT_TITLES
        })
        void ac010AndEc009MissingTokenIsRejectedBeforeAnyOtherCheck(String method, String path) {
            assertUnauthorized(http.send(HttpMethod.valueOf(method), path, null));
        }

        // "Bearer test-token " (trailing space) is not listed: RFC 9110 §5.5 excludes trailing whitespace from a
        // field value, so Tomcat delivers exactly "Bearer test-token". The filter's unit test covers that case;
        // here, whitespace that survives HTTP parsing is used instead.
        @ParameterizedTest
        @ValueSource(strings = {"Basic dGVzdA==", "Bearer", "Bearer ", "Bearer wrong", "Bearer  test-token",
            "Bearer test-token x"})
        void ac011InvalidCredentialsAreRejectedOnBothRoutes(String authorization) {
            assertUnauthorized(http.send(HttpMethod.GET, APPOINTMENT_TITLES, authorization));
            assertUnauthorized(http.send(HttpMethod.GET, APPOINTMENT_TITLES + "/70", authorization));
        }

        @Test
        void ac018HealthcheckNeedsNoToken() {
            ResponseEntity<String> response = http.send(HttpMethod.GET, "/api/v1/healthcheck", null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("{\"status\":\"ok\"}");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/%61pi/v1/reference_data/appointment_titles",
            "/api/v1/%72eference_data/appointment_titles",
            "/./api/v1/reference_data/appointment_titles",
            "/api;x=1/v1/reference_data/appointment_titles"
        })
        void encodedOrNormalisedPathsCannotBypassAuthentication(String path) {
            ResponseEntity<String> response = http.send(HttpMethod.GET, path, null);

            // Tomcat may reject a form outright with 400; the only failure is reference data without a token.
            assertThat(response.getStatusCode().value()).isIn(400, 401);
            assertThat(response.getBody()).doesNotContain("results", "Acting Senior Coroner");
            if (response.getStatusCode().value() == 401) {
                assertUnauthorized(response);
            }
        }

        @Test
        void anEncodedHealthcheckPathIsStillTheHealthcheck() {
            ResponseEntity<String> response = http.send(HttpMethod.GET, "/api/v1/%68ealthcheck", null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("{\"status\":\"ok\"}");
        }
    }

    @Nested
    class AliasTests {

        private static final String ALIAS = "/api/v1/reference_data/appointment_title";

        @ParameterizedTest(name = "suffix \"{0}\"")
        @ValueSource(strings = {"", "/10", "/70", "/15", "/abc", "?page=2"})
        void ac002Ac003Ac005Ac006TheAliasBehavesExactlyLikeTheCanonicalName(String suffix) {
            ResponseEntity<String> canonical = http.get(APPOINTMENT_TITLES + suffix);
            ResponseEntity<String> alias = http.get(ALIAS + suffix);

            assertThat(alias.getStatusCode()).isEqualTo(canonical.getStatusCode());
            assertThat(alias.getHeaders().getContentType()).isEqualTo(canonical.getHeaders().getContentType());
            if (canonical.getStatusCode().is2xxSuccessful()) {
                assertThat(alias.getBody()).isEqualTo(canonical.getBody());
            } else {
                assertThat(withoutVolatileFields(alias)).isEqualTo(withoutVolatileFields(canonical));
            }
        }

        @ParameterizedTest(name = "suffix \"{0}\"")
        @ValueSource(strings = {"", "/70", "/15"})
        void ac016TheAliasIsTransparentToTheCaller(String suffix) {
            ResponseEntity<String> canonical = http.get(APPOINTMENT_TITLES + suffix);
            ResponseEntity<String> alias = http.get(ALIAS + suffix);

            assertThat(alias.getHeaders().headerNames()).isEqualTo(canonical.getHeaders().headerNames());
            if (alias.getStatusCode().is2xxSuccessful()) {
                assertThat(alias.getBody()).doesNotContain("appointment_title");
            }
        }

        /**
         * Error bodies differ only in when they were produced and which request they belong to.
         */
        private JsonNode withoutVolatileFields(ResponseEntity<String> response) {
            ObjectNode body = (ObjectNode) json(response);
            body.remove("timestamp");
            body.remove("traceId");
            return body;
        }
    }

    @Nested
    class UnsupportedTypeTests {

        // Also covers 003 AC-009/AC-010 (via base_location), AC-015 and EC-006
        // (specs/003-base-locations-reference-data), 004 AC-016 and EC-007
        // (specs/004-contract-types-reference-data), 005 AC-015 and EC-007
        // (specs/005-genders-reference-data), 006 AC-016 and EC-011
        // (specs/006-judiciary-roles-reference-data), and 007 AC-019 and EC-010
        // (specs/007-locations-reference-data).
        @ParameterizedTest
        @ValueSource(strings = {
            // The other 15 E-Links attribute names: five canonical types, their five deprecated aliases, and
            // base_location, contract_type, gender, judiciary_role and location, which 003–007 don't serve.
            "jurisdictions", "location_types",
            "ticket_categories", "ticket_category_types", "tickets",
            "base_location", "contract_type", "gender", "judiciary_role", "jurisdiction", "location_type",
            "location", "ticket_category", "ticket_category_type", "ticket",
            // Unknown names and case variants (EC-001).
            "foo", "Appointment_Titles", "APPOINTMENT_TITLES", "appointment_titles_",
            "Base_Locations", "BASE_LOCATIONS", "base-locations",
            "Contract_Types", "CONTRACT_TYPES", "contract-types",
            "Genders", "GENDERS", "genders_",
            "Judiciary_roles", "JUDICIARY_ROLES", "judiciary-roles", "judiciaryRoles", "judiciary_roles_",
            "Locations", "LOCATIONS", "locations_", "Location"
        })
        void ac007Ac014UnsupportedNamesAreRejectedOnBothRoutes(String name) {
            String collection = "/api/v1/reference_data/" + name;
            for (String path : new String[] {collection, collection + "/10"}) {
                JsonNode body = assertError(http.get(path), 400, UNSUPPORTED_ATTRIBUTE_NAME);
                assertThat(body.has("results")).as(path).isFalse();
                assertThat(body.has("id")).as(path).isFalse();
            }
        }

        @Test
        void theTypeIsCheckedBeforeTheId() {
            assertError(http.get("/api/v1/reference_data/foo/abc"), 400, UNSUPPORTED_ATTRIBUTE_NAME);
        }
    }
}
