package uk.gov.hmcts.ctam.jo.testsupport;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UNAUTHORIZED;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * Real-HTTP calls, checks and record lookups shared by the functional test classes, so each type's class doesn't
 * copy them.
 */
public final class ReferenceDataHttp {

    /**
     * The fields of a record whose type declares no reference fields.
     */
    public static final List<String> CONTRACT_FIELDS =
            List.of("id", "name", "created_at", "updated_at", "start_date", "end_date");

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private final IntSupplier port;

    /**
     * Takes the port lazily, because {@code @LocalServerPort} is injected after the test instance is built.
     */
    public ReferenceDataHttp(IntSupplier port) {
        this.port = port;
    }

    public ResponseEntity<String> get(String path) {
        return send(HttpMethod.GET, path);
    }

    /**
     * An authorised GET that asks for the given media type.
     */
    public ResponseEntity<String> getAccepting(String path, String accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
        headers.set(HttpHeaders.ACCEPT, accept);
        return exchange(HttpMethod.GET, path, headers);
    }

    public ResponseEntity<String> send(HttpMethod method, String path) {
        return send(method, path, BEARER_TOKEN);
    }

    /**
     * Sends the path exactly as given (URI.create, so {@code %} is not re-encoded); a null authorization is omitted.
     */
    public ResponseEntity<String> send(HttpMethod method, String path, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return exchange(method, path, headers);
    }

    /**
     * The name of the single record at the path, which must return {@code 200}.
     */
    public String nameOf(String path) {
        ResponseEntity<String> response = get(path);
        assertThat(response.getStatusCode().value()).as(path).isEqualTo(200);
        return json(response).get("name").asString();
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, HttpHeaders headers) {
        return restTemplate.exchange(URI.create("http://localhost:" + port.getAsInt() + path), method,
                                     new HttpEntity<>(headers), String.class);
    }

    /**
     * Asserts the shared error shape, the exact message, and that traceId matches the response header.
     */
    public static JsonNode assertError(ResponseEntity<String> response, int status, String message) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        JsonNode body = assertErrorBody(response.getBody(), message);
        assertThat(body.get("traceId").asString()).isEqualTo(response.getHeaders().getFirst("X-Correlation-Id"));
        return body;
    }

    /**
     * 002's AC-010 / AC-011 / SC-005: the shared 401, and no reference data in the body.
     */
    public static void assertUnauthorized(ResponseEntity<String> response) {
        JsonNode body = assertError(response, 401, UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(body.has("results")).isFalse();
        assertThat(body.has("id")).isFalse();
    }

    /**
     * A collection's records keyed by id.
     */
    public static Map<Long, JsonNode> byId(JsonNode results) {
        Map<Long, JsonNode> byId = new HashMap<>();
        results.forEach(record -> byId.put(record.get("id").asLong(), record));
        return byId;
    }

    /**
     * The record's {@code field}, failing with {@code context} and the record's id if it is absent or not a whole
     * number, so a missing value can't be read as 0 and pass.
     */
    public static long integralField(String context, JsonNode record, String field) {
        JsonNode value = record.path(field);
        assertThat(value.isIntegralNumber()).as("%s %d: %s %s", context, record.get("id").asLong(), field, value)
                .isTrue();
        return value.asLong();
    }

    public static JsonNode json(ResponseEntity<String> response) {
        return JsonTrees.parse(response.getBody());
    }

    /**
     * A golden response body from {@code src/functionalTest/resources/golden/}.
     */
    public static String golden(String file) throws IOException {
        try (InputStream in = Objects.requireNonNull(
                ReferenceDataHttp.class.getResourceAsStream("/golden/" + file), "golden file missing: " + file)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
