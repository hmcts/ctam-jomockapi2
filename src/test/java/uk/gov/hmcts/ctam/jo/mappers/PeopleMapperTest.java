package uk.gov.hmcts.ctam.jo.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ctam.jo.domain.AppointmentsResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationSummaryResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationsResponse;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;
import uk.gov.hmcts.ctam.jo.repository.FixturePeopleRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.FIRST_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.MINIMAL;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.SECOND_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.repository;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.mapper;

/**
 * 013 FR-021 and FR-044 on minimal.json's first office holder, who has a previous appointment (700001, with
 * authorisation 800001) and a current one (700002, with 800002, ended in 2024, and 800003, open).
 */
class PeopleMapperTest {

    private static final FixturePeopleRepository PEOPLE = repository(MINIMAL);

    private static final List<AuthorisationSummaryResponse> CURRENT = List.of(
            new AuthorisationSummaryResponse("Tribunals", List.of("03 - Disability Living Allowance")));

    private final PeopleMapper mapper = mapper();

    private static OfficeHolder holder(String id) {
        return PEOPLE.findById(id).orElseThrow();
    }

    @Test
    void withoutPreviousAppointmentsOnlyCurrentOnesAndTheAuthorisationsUnderThemAreShown() {
        CurrentOfficeHolderResponse response = mapper.toResponse(holder(FIRST_ID), false);

        assertThat(response.appointments()).extracting(AppointmentsResponse::appointmentId).containsExactly(700002L);
        assertThat(response.authorisationsWithDates()).extracting(AuthorisationsResponse::authorisationId)
                .containsExactly(800002L, 800003L);
        assertThat(response.authorisations()).containsExactly(
                new AuthorisationSummaryResponse("Courts", List.of("Civil Authorisation")),
                new AuthorisationSummaryResponse("Tribunals", List.of("03 - Disability Living Allowance")));
        assertThat(response.authorisationsCurrent()).isEqualTo(CURRENT);
    }

    @Test
    void withPreviousAppointmentsEverythingIsShownAndTheCurrentSummaryAndRolesAreUnchanged() {
        CurrentOfficeHolderResponse without = mapper.toResponse(holder(FIRST_ID), false);
        CurrentOfficeHolderResponse with = mapper.toResponse(holder(FIRST_ID), true);

        assertThat(with.appointments()).extracting(AppointmentsResponse::appointmentId)
                .containsExactly(700001L, 700002L);
        assertThat(with.authorisationsWithDates()).extracting(AuthorisationsResponse::authorisationId)
                .containsExactly(800001L, 800002L, 800003L);
        assertThat(with.authorisationsCurrent()).isEqualTo(without.authorisationsCurrent()).isEqualTo(CURRENT);
        assertThat(with.judiciaryRoles()).isEqualTo(without.judiciaryRoles()).hasSize(2);
    }

    @Test
    void genderIsWrittenOnlyWhenTheRecordShowsIt() {
        assertThat(mapper.toResponse(holder(FIRST_ID), false).gender()).isEqualTo("Female");
        assertThat(mapper.toResponse(holder(SECOND_ID), false).gender()).isNull();
        assertThat(mapper.toResponse(holder(SECOND_ID), false).sex()).isEqualTo("Male");
    }

    @Test
    void derivedNamesAndOwnFieldsAreCopiedThrough() {
        OfficeHolder holder = holder(FIRST_ID);
        OfficeHolder.ResolvedAppointment current = holder.getAppointments().get(1);

        CurrentOfficeHolderResponse response = mapper.toResponse(holder, false);

        assertThat(response.id()).isEqualTo(FIRST_ID);
        assertThat(response.perId()).isEqualTo(900001);
        assertThat(response.emailPersonal()).isEqualTo("alex.fenwick@personal.example");
        assertThat(response.disability()).isTrue();
        AppointmentsResponse appointment = response.appointments().get(0);
        assertThat(appointment.roleName()).isEqualTo(current.getRoleName());
        assertThat(appointment.courtType()).isEqualTo(current.getCourtType());
        assertThat(appointment.location()).isEqualTo(current.getLocation());
        assertThat(appointment.type()).isEqualTo(current.getType());
        assertThat(appointment.isPrincipal()).isTrue();
        assertThat(appointment.circuit()).isEqualTo("Midland");
        assertThat(response.judiciaryRoles().get(0).name()).isEqualTo(holder.getJudiciaryRoles().get(0).getName());
    }

    @Test
    void anOfficeHolderWithNoCurrentAuthorisationHasAnEmptyCurrentSummary() {
        CurrentOfficeHolderResponse second = mapper.toResponse(holder(SECOND_ID), false);

        assertThat(second.authorisationsWithDates()).hasSize(1);
        assertThat(second.authorisationsCurrent()).isEmpty();
    }
}
