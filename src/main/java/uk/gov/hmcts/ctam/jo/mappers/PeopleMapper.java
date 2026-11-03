package uk.gov.hmcts.ctam.jo.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ctam.jo.domain.AppointmentsResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationSummaryResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationsResponse;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.domain.JudiciaryRoleResponse;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedAppointment;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedAuthorisation;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedJudiciaryRole;
import uk.gov.hmcts.ctam.jo.services.AppointmentStatus;
import uk.gov.hmcts.ctam.jo.services.AuthorisationSummaries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps a resolved office holder to the response both People routes return (013 research R7). Any unmapped target
 * property fails the build. What the response holds depends on whether previous appointments are included:
 * <ul>
 *   <li>{@code appointments}: every appointment when they are, otherwise the current ones;</li>
 *   <li>{@code authorisations_with_dates}: those under the appointments in the response;</li>
 *   <li>{@code authorisations}: a summary of those;</li>
 *   <li>{@code authorisations_current}: a summary of the current dated authorisations, whatever the flag;</li>
 *   <li>{@code judiciary_roles}: every one, whatever the flag.</li>
 * </ul>
 * Each list is worked out once per office holder, then handed to the generated mapping.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class PeopleMapper {

    private AppointmentStatus appointmentStatus;

    private AuthorisationSummaries authorisationSummaries;

    @Autowired
    public void setAppointmentStatus(AppointmentStatus appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    @Autowired
    public void setAuthorisationSummaries(AuthorisationSummaries authorisationSummaries) {
        this.authorisationSummaries = authorisationSummaries;
    }

    /**
     * The office holder as one request with this flag value sees them.
     */
    public CurrentOfficeHolderResponse toResponse(OfficeHolder holder, boolean includePreviousAppointments) {
        List<ResolvedAppointment> appointments = holder.getAppointments().stream()
                .filter(resolved -> includePreviousAppointments || appointmentStatus.isCurrent(resolved.getEndDate()))
                .toList();
        Set<Long> shown = appointments.stream().map(ResolvedAppointment::getAppointmentId).collect(Collectors.toSet());
        List<AuthorisationsResponse> withDates = toAuthorisations(holder.getAuthorisationsWithDates().stream()
                .filter(resolved -> shown.contains(resolved.getAppointmentId()))
                .toList());
        List<AuthorisationsResponse> current = toAuthorisations(holder.getAuthorisationsWithDates().stream()
                .filter(resolved -> appointmentStatus.isCurrent(resolved.getEndDate()))
                .toList());
        return toResponse(holder, toAppointments(appointments), withDates,
                          authorisationSummaries.summarise(withDates), authorisationSummaries.summarise(current));
    }

    @Mapping(target = "gender", expression = "java(holder.isShowGender() ? holder.getSex() : null)")
    @Mapping(target = "appointments", source = "appointments")
    @Mapping(target = "judiciaryRoles", source = "holder.judiciaryRoles")
    @Mapping(target = "authorisationsWithDates", source = "authorisationsWithDates")
    @Mapping(target = "authorisations", source = "authorisations")
    @Mapping(target = "authorisationsCurrent", source = "authorisationsCurrent")
    protected abstract CurrentOfficeHolderResponse toResponse(OfficeHolder holder,
                                                              List<AppointmentsResponse> appointments,
                                                              List<AuthorisationsResponse> authorisationsWithDates,
                                                              List<AuthorisationSummaryResponse> authorisations,
                                                              List<AuthorisationSummaryResponse> authorisationsCurrent);

    @Mapping(target = "isPrincipal", source = "principal")
    protected abstract AppointmentsResponse toResponse(ResolvedAppointment resolved);

    protected abstract JudiciaryRoleResponse toResponse(ResolvedJudiciaryRole resolved);

    protected abstract AuthorisationsResponse toResponse(ResolvedAuthorisation resolved);

    protected abstract List<AppointmentsResponse> toAppointments(List<ResolvedAppointment> resolved);

    protected abstract List<JudiciaryRoleResponse> toJudiciaryRoles(List<ResolvedJudiciaryRole> resolved);

    protected abstract List<AuthorisationsResponse> toAuthorisations(List<ResolvedAuthorisation> resolved);
}
