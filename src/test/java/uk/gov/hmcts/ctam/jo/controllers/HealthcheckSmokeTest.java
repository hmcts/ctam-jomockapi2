package uk.gov.hmcts.ctam.jo.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class HealthcheckSmokeTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void healthcheckRoundTripReturnsOkStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/healthcheck", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"ok\"}");
    }

    @Test
    void healthcheckRespondsWithinOneSecond() {
        Instant start = Instant.now();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/healthcheck", String.class);

        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
    }

    @Test
    void healthcheckAverageResponseTimeIsUnder100MillisAfterWarmUp() {
        String url = "http://localhost:" + port + "/api/v1/healthcheck";
        int warmUpCalls = 5;
        int measuredCalls = 20;

        for (int i = 0; i < warmUpCalls; i++) {
            restTemplate.getForEntity(url, String.class);
        }

        long totalMillis = 0;
        for (int i = 0; i < measuredCalls; i++) {
            Instant start = Instant.now();
            restTemplate.getForEntity(url, String.class);
            totalMillis += Duration.between(start, Instant.now()).toMillis();
        }

        double averageMillis = (double) totalMillis / measuredCalls;
        assertThat(averageMillis).isLessThan(100.0);
    }
}
