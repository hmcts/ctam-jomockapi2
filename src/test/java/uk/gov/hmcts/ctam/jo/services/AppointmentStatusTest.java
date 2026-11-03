package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.appointmentStatus;

/**
 * 013 FR-020 with the shipped reference date 2026-01-01: current means open, or ending on or after it.
 */
class AppointmentStatusTest {

    private final AppointmentStatus status = appointmentStatus();

    @Test
    void anOpenAppointmentIsCurrent() {
        assertThat(status.isCurrent(null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-01-01", "2026-06-30", "2099-12-31"})
    void oneEndingOnOrAfterTheReferenceDateIsCurrent(String endDate) {
        assertThat(status.isCurrent(LocalDate.parse(endDate))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-12-31", "2001-01-01"})
    void oneEndingBeforeItIsPrevious(String endDate) {
        assertThat(status.isCurrent(LocalDate.parse(endDate))).isFalse();
    }
}
