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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.APPOINTMENT_TITLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.BASE_LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.CONTRACT_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.GENDERS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.JUDICIARY_ROLES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.JURISDICTIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATIONS;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.LOCATION_TYPES;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.TICKETS;
import static uk.gov.hmcts.ctam.jo.testsupport.TestTokens.TOKEN;

/**
 * A latency regression guard for each configured type's authenticated collection call (002 research R13, 003
 * research R7), so a larger collection is held to the same bound. Every measured call must also return 200.
 * The default 1 s p95 bound is reliable on shared CI runners; {@code -Dperf.strict=true} applies the 100 ms
 * developer target. The response body is covered by the functional suite.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class ReferenceDataSmokeTest {

    private static final int WARM_UP_CALLS = 5;

    private static final int MEASURED_CALLS = 20;

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APPOINTMENT_TITLES, BASE_LOCATIONS, CONTRACT_TYPES, GENDERS, JUDICIARY_ROLES,
        LOCATIONS, LOCATION_TYPES, JURISDICTIONS, TICKETS})
    void collectionP95LatencyIsWithinBound(String path, TestReporter reporter) {
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
        reporter.publishEntry("p95 " + path, p95.toMillis() + " ms");

        Duration bound = Boolean.getBoolean("perf.strict") ? Duration.ofMillis(100) : Duration.ofSeconds(1);
        assertThat(p95).as("p95 of %d calls", MEASURED_CALLS).isLessThan(bound);
    }

    private ResponseEntity<String> get(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        return restTemplate.exchange("http://localhost:" + port + path, HttpMethod.GET, new HttpEntity<>(headers),
                                     String.class);
    }
}
