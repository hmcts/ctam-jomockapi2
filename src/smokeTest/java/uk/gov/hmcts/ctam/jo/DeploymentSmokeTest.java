package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;
import uk.gov.hmcts.ctam.jo.testsupport.TestTarget;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.PEOPLE;

/**
 * Checks that a copy of the service is up and serving: the health check answers, one reference-data route and one
 * People route answer with the token, and requests without a valid token are refused. It runs in-process in the
 * {@code smokeTest} suite and, through the {@code smoke} Gradle task, against the deployed copy at {@code TEST_URL}
 * (014 FR-016). It deliberately has no response-time limit: the latency guards stay in-process (014 EC-005).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class DeploymentSmokeTest {

    /**
     * The built-in local development token, which a deployed copy must refuse (014 FR-012).
     */
    private static final String LOCAL_DEVELOPMENT_TOKEN = "local-dev-token";

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void healthcheckIsUp() {
        ResponseEntity<String> response = get("/api/v1/healthcheck", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(JsonTrees.parse(response.getBody()).get("status").asString()).isEqualTo("ok");
    }

    @Test
    void referenceDataIsServed() {
        assertThat(get(APPOINTMENT_TITLES, TestTarget.token()).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void peopleAreServed() {
        assertThat(get(PEOPLE + "?updated_since=2000-01-01", TestTarget.token()).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void requestWithoutTokenIsRefused() {
        assertThat(get(APPOINTMENT_TITLES, null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void localDevelopmentTokenIsRefused() {
        assumeFalse(LOCAL_DEVELOPMENT_TOKEN.equals(TestTarget.token()),
                    "the configured token is the local development token");

        assertThat(get(APPOINTMENT_TITLES, LOCAL_DEVELOPMENT_TOKEN).getStatusCode().value()).isEqualTo(401);
    }

    /**
     * A GET to the path on the target copy; a null token sends no {@code Authorization} header.
     */
    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(URI.create(TestTarget.baseUrl(port) + path), HttpMethod.GET,
                                     new HttpEntity<>(headers), String.class);
    }
}
