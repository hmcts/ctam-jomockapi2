package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.PEOPLE;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.TOKEN;

/**
 * 013 SC-009: the People requests meet 002's latency targets unchanged, with every measured call returning 200. The
 * default 1 s p95 bound is reliable on shared CI runners; {@code -Dperf.strict=true} applies the 100 ms developer
 * target. The response bodies are covered by the functional suite.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class PeopleSmokeTest {

    private static final int WARM_UP_CALLS = 5;

    private static final int MEASURED_CALLS = 20;

    private static final String DEFAULT_SEARCH = "?updated_since=2000-01-01";

    /**
     * Stands for the first office holder of the default search, looked up before measuring.
     */
    private static final String SINGLE = "single office holder";

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SINGLE, DEFAULT_SEARCH,
        DEFAULT_SEARCH + "&per_page=120&include_previous_appointments=true"})
    void p95LatencyIsWithinBound(String request, TestReporter reporter) {
        String path = PEOPLE + (SINGLE.equals(request) ? "/" + firstId() : request);
        for (int i = 0; i < WARM_UP_CALLS; i++) {
            get(path);
        }
        List<Duration> durations = new ArrayList<>();
        for (int i = 0; i < MEASURED_CALLS; i++) {
            long start = System.nanoTime();
            assertThat(get(path).getStatusCode().value()).isEqualTo(200);
            durations.add(Duration.ofNanos(System.nanoTime() - start));
        }
        Collections.sort(durations);
        Duration p95 = durations.get((int) Math.ceil(0.95 * MEASURED_CALLS) - 1);
        reporter.publishEntry("p95 " + request, p95.toMillis() + " ms");

        Duration bound = Boolean.getBoolean("perf.strict") ? Duration.ofMillis(100) : Duration.ofSeconds(1);
        assertThat(p95).as("p95 of %d calls", MEASURED_CALLS).isLessThan(bound);
    }

    private String firstId() {
        ResponseEntity<String> search = get(PEOPLE + DEFAULT_SEARCH);
        assertThat(search.getStatusCode().value()).isEqualTo(200);
        return JsonTrees.parse(search.getBody()).get("results").get(0).get("id").asString();
    }

    private ResponseEntity<String> get(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        return restTemplate.exchange(URI.create("http://localhost:" + port + path), HttpMethod.GET,
                                     new HttpEntity<>(headers), String.class);
    }
}
