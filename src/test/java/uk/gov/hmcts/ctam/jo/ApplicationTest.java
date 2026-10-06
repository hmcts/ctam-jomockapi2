package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

/**
 * {@code main} only hands over to Spring Boot. The started application itself is covered by the full-context
 * suites, which boot it through {@code @SpringBootTest} rather than {@code main}.
 */
class ApplicationTest {

    @Test
    void mainStartsSpringBootWithTheApplicationClassAndArguments() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            Application.main(args);

            springApplication.verify(() -> SpringApplication.run(Application.class, args));
        }
    }
}
