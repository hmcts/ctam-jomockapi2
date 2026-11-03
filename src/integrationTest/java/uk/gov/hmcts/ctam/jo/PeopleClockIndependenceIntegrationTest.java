package uk.gov.hmcts.ctam.jo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.domain.AppointmentsResponse;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.domain.PeopleApiResponse;
import uk.gov.hmcts.ctam.jo.services.PeopleService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether an appointment is current is decided by the configured reference date, never the clock (013 FR-020, D-8,
 * SC-003): the same requests give the same responses with the clock years apart, and only moving the reference date
 * changes them.
 */
class PeopleClockIndependenceIntegrationTest {

    /**
     * The current appointments that end between 2026-03-31 and 2029-12-31 in the default dataset (G-11; recorded in
     * plan.md § Implementation notes, T014).
     */
    private static final int ENDING_BEFORE_2030 = 8;

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    @Configuration
    static class EarlyClock {
        @Bean
        @Primary
        Clock earlyClock() {
            return clockAt("2026-01-02T09:00:00Z");
        }
    }

    @Configuration
    static class LateClock {
        @Bean
        @Primary
        Clock lateClock() {
            return clockAt("2031-07-01T09:00:00Z");
        }
    }

    private static ConfigurableApplicationContext start(Class<?> clock, String... properties) {
        return new SpringApplicationBuilder(Application.class, clock)
                .web(WebApplicationType.NONE)
                .profiles("test")
                // Command-line arguments, so they override application.yml (builder properties are only defaults).
                .run(Arrays.stream(properties).map(property -> "--" + property).toArray(String[]::new));
    }

    /**
     * The whole-dataset search's JSON without and with the flag.
     */
    private static List<String> bodies(ConfigurableApplicationContext context) {
        PeopleService service = context.getBean(PeopleService.class);
        JsonMapper mapper = context.getBean(JsonMapper.class);
        return List.of(mapper.writeValueAsString(service.search("2000-01-01", null, "120", "false")),
                       mapper.writeValueAsString(service.search("2000-01-01", null, "120", "true")));
    }

    @Test
    void responsesAreTheSameWithTheClockYearsApart() {
        List<String> early;
        List<String> late;
        try (ConfigurableApplicationContext context = start(EarlyClock.class)) {
            assertThat(context.getBean(Clock.class).instant()).isEqualTo(Instant.parse("2026-01-02T09:00:00Z"));
            early = bodies(context);
        }
        try (ConfigurableApplicationContext context = start(LateClock.class)) {
            assertThat(context.getBean(Clock.class).instant()).isEqualTo(Instant.parse("2031-07-01T09:00:00Z"));
            late = bodies(context);
        }

        assertThat(late).isEqualTo(early);
        assertThat(early.get(0)).isNotEqualTo(early.get(1));
    }

    @Test
    void movingTheReferenceDateMakesExactlyTheAppointmentsEndingBeforeItPrevious() {
        PeopleApiResponse shipped;
        PeopleApiResponse moved;
        PeopleApiResponse movedWithFlag;
        try (ConfigurableApplicationContext context = start(LateClock.class)) {
            shipped = context.getBean(PeopleService.class).search("2000-01-01", null, "120", null);
        }
        try (ConfigurableApplicationContext context = start(LateClock.class, "jo.people.reference-date=2030-01-01")) {
            PeopleService service = context.getBean(PeopleService.class);
            moved = service.search("2000-01-01", null, "120", null);
            movedWithFlag = service.search("2000-01-01", null, "120", "true");
        }

        assertThat(moved.pagination()).isEqualTo(shipped.pagination()).isEqualTo(movedWithFlag.pagination());
        assertThat(ids(moved)).isEqualTo(ids(shipped)).isEqualTo(ids(movedWithFlag));
        assertThat(appointments(shipped) - appointments(moved)).isEqualTo(ENDING_BEFORE_2030);
        assertThat(moved.results().stream().flatMap(holder -> holder.appointments().stream()))
                .allMatch(appointment -> isOpenOrEndsOnOrAfter(appointment, "2030-01-01"));
    }

    private static List<String> ids(PeopleApiResponse response) {
        return response.results().stream().map(CurrentOfficeHolderResponse::id).toList();
    }

    private static long appointments(PeopleApiResponse response) {
        return response.results().stream().mapToLong(holder -> holder.appointments().size()).sum();
    }

    private static boolean isOpenOrEndsOnOrAfter(AppointmentsResponse appointment, String date) {
        return appointment.endDate() == null || !appointment.endDate().isBefore(LocalDate.parse(date));
    }
}
