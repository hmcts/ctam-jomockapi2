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
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ErrorResponseAssertions.assertErrorBody;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.BEARER_TOKEN;

/**
 * Real-HTTP calls and checks shared by the functional test classes, so each type's class doesn't copy them.
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
