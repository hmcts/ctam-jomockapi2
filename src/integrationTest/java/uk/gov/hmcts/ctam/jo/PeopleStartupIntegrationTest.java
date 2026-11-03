package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import uk.gov.hmcts.ctam.jo.domain.PeopleApiResponse;
import uk.gov.hmcts.ctam.jo.exceptions.PersonNotFoundException;
import uk.gov.hmcts.ctam.jo.services.PeopleService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The service validates the configured People fixture at start-up and refuses to start with an invalid one, naming the
 * rule and the record (013 FR-046); an empty fixture is valid (EC-013).
 */
class PeopleStartupIntegrationTest {

    private static ConfigurableApplicationContext start(String... properties) {
        return new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                // Command-line arguments, so they override application.yml (builder properties are only defaults).
                .run(Arrays.stream(properties).map(property -> "--" + property).toArray(String[]::new));
    }

    @Test
    void duplicatedIdStopsStartUpWithTheDocumentedMessage() {
        assertThatThrownBy(() -> start("jo.people.fixture=classpath:people/invalid-duplicate-id.json"))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid People fixture classpath:people/invalid-duplicate-id.json: duplicate id "
                            + "(office holder 11111111-1111-4111-8111-111111111111)");
    }

    @Test
    void referenceTypeThatIsNotConfiguredStopsStartUp() {
        assertThatThrownBy(() -> start("jo.people.reference-types.gender-id=widgets"))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jo.people.reference-types.gender-id 'widgets' is not a configured "
                                      + "reference-data type");
    }

    @Test
    void anEmptyFixtureStartsAndEverySearchIsEmpty() {
        try (ConfigurableApplicationContext context = start("jo.people.fixture=classpath:people/empty.json")) {
            PeopleService service = context.getBean(PeopleService.class);

            PeopleApiResponse response = service.search("2000-01-01", null, null, null);

            assertThat(response.results()).isEmpty();
            assertThat(response.pagination().pages()).isZero();
            assertThatThrownBy(() -> service.getById("11111111-1111-4111-8111-111111111111", null))
                    .isInstanceOf(PersonNotFoundException.class);
        }
    }
}
