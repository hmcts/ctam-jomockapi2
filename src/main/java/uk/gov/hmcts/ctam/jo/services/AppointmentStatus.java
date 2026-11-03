package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties;

import java.time.LocalDate;

/**
 * Decides whether an appointment, or a dated authorisation, is current (013 FR-020): its end date is null or on or
 * after the configured reference date. The clock is never read, so the same request gives the same response every day
 * (Principle V, deviation D-8).
 */
@Component
public class AppointmentStatus {

    private final LocalDate referenceDate;

    public AppointmentStatus(PeopleProperties properties) {
        this.referenceDate = properties.referenceDate();
    }

    /**
     * Whether something ending on {@code endDate} (null: open) is current on the reference date.
     */
    public boolean isCurrent(LocalDate endDate) {
        return endDate == null || !endDate.isBefore(referenceDate);
    }
}
