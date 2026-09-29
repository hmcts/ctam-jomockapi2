package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * The spec's acceptance scenarios, end to end over real HTTP. One nested class per user story.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class ReferenceDataFunctionalTest {

    private static final String COLLECTION = "/api/v1/reference_data/appointment_titles";

    private static final String BAD_ID = "reference_id must be a non-negative whole number.";

    private static final String RECORD_NOT_FOUND = "Reference data record not found.";

    private static final String NO_QUERY = "Query parameters are not supported on this endpoint.";

    private static final String RESOURCE_NOT_FOUND = "Resource not found.";

    private static final String UNAUTHORIZED = "Unauthorized. Invalid or missing token.";

    private static final List<String> CONTRACT_FIELDS =
            List.of("id", "name", "created_at", "updated_at", "start_date", "end_date");

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    ResponseEntity<String> get(String path) {
        return send(HttpMethod.GET, path);
    }

    ResponseEntity<String> send(HttpMethod method, String path) {
        return send(method, path, "Bearer test-token");
    }

    /**
     * Sends the path exactly as given (URI.create, so {@code %} is not re-encoded); a null authorization is omitted.
     */
    ResponseEntity<String> send(HttpMethod method, String path, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return restTemplate.exchange(URI.create("http://localhost:" + port + path), method,
                                     new HttpEntity<>(headers), String.class);
    }

    /**
     * Asserts the shared error shape, the exact message, and that traceId matches the response header.
     */
    JsonNode assertError(ResponseEntity<String> response, int status, String message) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode body = json(response);
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("error", "timestamp", "traceId");
        assertThat(body.get("error").asString()).isEqualTo(message);
        assertThat(body.get("traceId").asString()).isEqualTo(response.getHeaders().getFirst("X-Correlation-Id"));
        return body;
    }

    JsonNode json(ResponseEntity<String> response) {
        return jsonMapper.readTree(response.getBody());
    }

    @Nested
    class CollectionTests {

        @Test
        void ac001ReturnsAll194TitlesInAscendingIdOrder() {
            ResponseEntity<String> response = get(COLLECTION);

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
            assertThat(get(path).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        void ac015RepeatedCallsAreIdenticalAndMatchTheGoldenFile() throws IOException {
            String golden = golden();
            for (int i = 0; i < 100; i++) {
                assertThat(get(COLLECTION).getBody()).isEqualTo(golden);
            }
        }

        @Test
        void everyRecordHasExactlyTheSixContractFields() {
            json(get(COLLECTION)).get("results").forEach(record ->
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

        private String golden() throws IOException {
            try (InputStream in = Objects.requireNonNull(
                    getClass().getResourceAsStream("/golden/appointment_titles.json"), "golden file missing")) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    @Nested
    class SingleRecordTests {

        @Test
        void ac004ReturnsTheSingleRecordEqualToTheCollectionElement() {
            ResponseEntity<String> response = get(COLLECTION + "/70");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().getContentType()).hasToString("application/json");
            JsonNode record = json(response);
            assertThat(record.has("results")).isFalse();
            assertThat(record.propertyNames()).containsExactlyElementsOf(CONTRACT_FIELDS);
            assertThat(record.get("name").asString()).isEqualTo("Area Coroner");
            assertThat(record.get("end_date").asString()).isEqualTo("2025-03-31");

            JsonNode fromCollection = null;
            for (JsonNode candidate : json(get(COLLECTION)).get("results")) {
                if (candidate.get("id").asLong() == 70) {
                    fromCollection = candidate;
                }
            }
            assertThat(record).isEqualTo(fromCollection);
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "12x", "1.5", "-1"})
        void ac008MalformedIdsReturn400(String id) {
            assertError(get(COLLECTION + "/" + id), 400, BAD_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"15", "999999", "007", "99999999999999999999"})
        void ac009AndEc003UnknownIdsReturn404(String id) {
            assertError(get(COLLECTION + "/" + id), 404, RECORD_NOT_FOUND);
        }

        @Test
        void ec003LeadingZerosAreAccepted() {
            ResponseEntity<String> response = get(COLLECTION + "/010");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(json(response).get("id").asLong()).isEqualTo(10);
        }

        @ParameterizedTest
        @ValueSource(strings = {COLLECTION + "?page=2", COLLECTION + "/70?name=Judge"})
        void ac020QueryParametersReturn400(String path) {
            assertError(get(path), 400, NO_QUERY);
        }

        @ParameterizedTest
        @ValueSource(strings = {COLLECTION + "/", COLLECTION + "/70/", COLLECTION + "/1/extra",
            COLLECTION + "/?page=2", COLLECTION + "/1/extra?x=1"})
        void ec002Ec004Ec005UnmatchedRoutesReturn404BeforeQueryChecks(String path) {
            assertError(get(path), 404, RESOURCE_NOT_FOUND);
        }

        @ParameterizedTest
        @CsvSource({
            "POST, " + COLLECTION, "PUT, " + COLLECTION, "PATCH, " + COLLECTION, "DELETE, " + COLLECTION,
            "POST, " + COLLECTION + "/70", "PUT, " + COLLECTION + "/70", "PATCH, " + COLLECTION + "/70",
            "DELETE, " + COLLECTION + "/70", "POST, /api/v1/reference_data/appointment_title"
        })
        void ec007OtherMethodsReturn405(String method, String path) {
            JsonNode body = assertError(send(HttpMethod.valueOf(method), path), 405, "Method not allowed.");

            assertThat(body.has("results")).isFalse();
            assertThat(body.has("id")).isFalse();
        }
    }

    @Nested
    class AuthenticationTests {

        @ParameterizedTest
        @CsvSource({
            "GET, " + COLLECTION, "GET, " + COLLECTION + "/70", "GET, /api/v1/reference_data/genders",
            "GET, " + COLLECTION + "/abc", "GET, " + COLLECTION + "?page=2", "GET, " + COLLECTION + "/",
            "POST, " + COLLECTION
        })
        void ac010AndEc009MissingTokenIsRejectedBeforeAnyOtherCheck(String method, String path) {
            assertUnauthorized(send(HttpMethod.valueOf(method), path, null));
        }

        // "Bearer test-token " (trailing space) is not listed: RFC 9110 §5.5 excludes trailing whitespace from a
        // field value, so Tomcat delivers exactly "Bearer test-token". The filter's unit test covers that case;
        // here, whitespace that survives HTTP parsing is used instead.
        @ParameterizedTest
        @ValueSource(strings = {"Basic dGVzdA==", "Bearer", "Bearer ", "Bearer wrong", "Bearer  test-token",
            "Bearer test-token x"})
        void ac011InvalidCredentialsAreRejectedOnBothRoutes(String authorization) {
            assertUnauthorized(send(HttpMethod.GET, COLLECTION, authorization));
            assertUnauthorized(send(HttpMethod.GET, COLLECTION + "/70", authorization));
        }

        @Test
        void ac018HealthcheckNeedsNoToken() {
            ResponseEntity<String> response = send(HttpMethod.GET, "/api/v1/healthcheck", null);

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
            ResponseEntity<String> response = send(HttpMethod.GET, path, null);

            // Tomcat may reject a form outright with 400; the only failure is reference data without a token.
            assertThat(response.getStatusCode().value()).isIn(400, 401);
            assertThat(response.getBody()).doesNotContain("results", "Acting Senior Coroner");
            if (response.getStatusCode().value() == 401) {
                assertUnauthorized(response);
            }
        }

        @Test
        void anEncodedHealthcheckPathIsStillTheHealthcheck() {
            ResponseEntity<String> response = send(HttpMethod.GET, "/api/v1/%68ealthcheck", null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("{\"status\":\"ok\"}");
        }

        /**
         * AC-010 / AC-011 / SC-005: the shared 401, and no reference data in the body.
         */
        private void assertUnauthorized(ResponseEntity<String> response) {
            JsonNode body = assertError(response, 401, UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
            assertThat(body.has("results")).isFalse();
            assertThat(body.has("id")).isFalse();
        }
    }

    @Nested
    class AliasTests {

        private static final String ALIAS = "/api/v1/reference_data/appointment_title";

        @ParameterizedTest(name = "suffix \"{0}\"")
        @ValueSource(strings = {"", "/10", "/70", "/15", "/abc", "?page=2"})
        void ac002Ac003Ac005Ac006TheAliasBehavesExactlyLikeTheCanonicalName(String suffix) {
            ResponseEntity<String> canonical = get(COLLECTION + suffix);
            ResponseEntity<String> alias = get(ALIAS + suffix);

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
            ResponseEntity<String> canonical = get(COLLECTION + suffix);
            ResponseEntity<String> alias = get(ALIAS + suffix);

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

        private static final String UNSUPPORTED = "Unsupported reference data attribute_name.";

        @ParameterizedTest
        @ValueSource(strings = {
            // The other 20 E-Links attribute names: ten canonical types and their ten deprecated aliases.
            "base_locations", "contract_types", "genders", "judiciary_roles", "jurisdictions", "location_types",
            "locations", "ticket_categories", "ticket_category_types", "tickets",
            "base_location", "contract_type", "gender", "judiciary_role", "jurisdiction", "location_type",
            "location", "ticket_category", "ticket_category_type", "ticket",
            // Unknown names and case variants (EC-001).
            "foo", "Appointment_Titles", "APPOINTMENT_TITLES", "appointment_titles_"
        })
        void ac007Ac014UnsupportedNamesAreRejectedOnBothRoutes(String name) {
            String collection = "/api/v1/reference_data/" + name;
            for (String path : new String[] {collection, collection + "/10"}) {
                JsonNode body = assertError(get(path), 400, UNSUPPORTED);
                assertThat(body.has("results")).as(path).isFalse();
                assertThat(body.has("id")).as(path).isFalse();
            }
        }

        @Test
        void theTypeIsCheckedBeforeTheId() {
            assertError(get("/api/v1/reference_data/foo/abc"), 400, UNSUPPORTED);
        }
    }
}
