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
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * One authenticated live round trip, plus a latency regression guard (research R13). The default 1 s p95
 * bound is reliable on shared CI runners; {@code -Dperf.strict=true} applies the 100 ms developer target.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class ReferenceDataSmokeTest {

    private static final String COLLECTION = "/api/v1/reference_data/appointment_titles";

    private static final int WARM_UP_CALLS = 5;

    private static final int MEASURED_CALLS = 20;

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void authenticatedCollectionRoundTripReturnsAllTitles() {
        ResponseEntity<String> response = get(COLLECTION);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(JsonMapper.builder().build().readTree(response.getBody()).get("results").size()).isEqualTo(194);
    }

    @Test
    void collectionP95LatencyIsWithinBound() {
        for (int i = 0; i < WARM_UP_CALLS; i++) {
            get(COLLECTION);
        }
        List<Duration> durations = new ArrayList<>();
        for (int i = 0; i < MEASURED_CALLS; i++) {
            long start = System.nanoTime();
            assertThat(get(COLLECTION).getStatusCode().value()).isEqualTo(200);
            durations.add(Duration.ofNanos(System.nanoTime() - start));
        }
        Collections.sort(durations);
        Duration p95 = durations.get((int) Math.ceil(0.95 * MEASURED_CALLS) - 1);

        Duration bound = Boolean.getBoolean("perf.strict") ? Duration.ofMillis(100) : Duration.ofSeconds(1);
        assertThat(p95).as("p95 of %d calls", MEASURED_CALLS).isLessThan(bound);
    }

    private ResponseEntity<String> get(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-token");
        return restTemplate.exchange("http://localhost:" + port + path, HttpMethod.GET, new HttpEntity<>(headers),
                                     String.class);
    }
}
