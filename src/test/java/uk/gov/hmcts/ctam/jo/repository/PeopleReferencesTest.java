package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties.ReferenceTypes;
import uk.gov.hmcts.ctam.jo.domain.AppointmentsResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationSummaryResponse;
import uk.gov.hmcts.ctam.jo.domain.AuthorisationsResponse;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.mappers.PeopleMapper;
import uk.gov.hmcts.ctam.jo.services.AppointmentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.DEFAULT;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.appointmentStatus;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.properties;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.referenceData;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.registry;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.repository;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.mapper;

/**
 * Every reference the default People dataset makes to reference data resolves, and every name beside it is that
 * record's name (013 FR-037 to FR-045, SC-004). The checks look each id up through {@code ReferenceDataRepository}, not
 * through the code that derives the names, and cover both flag values. Closes 004 DF-2, 005 DF-3, 006 DF-4, 007 DF-6
 * and 010 DF-9.
 */
class PeopleReferencesTest {

    private static final List<CurrentOfficeHolderResponse> RESPONSES = new ArrayList<>();

    private static ReferenceTypes types;

    @BeforeAll
    static void mapEveryOfficeHolderWithBothFlagValues() {
        types = properties(DEFAULT).referenceTypes();
        PeopleMapper mapper = mapper();
        repository(DEFAULT).findAll().forEach(holder -> {
            RESPONSES.add(mapper.toResponse(holder, false));
            RESPONSES.add(mapper.toResponse(holder, true));
        });
    }

    @Test
    void ac018EveryGenderResolvesAndNamesMatch() {
        assertThat(RESPONSES).allSatisfy(person -> {
            String name = record(types.genderId(), person.genderId()).getName();
            assertThat(person.sex()).as(person.id()).isEqualTo(name);
            if (person.gender() != null) {
                assertThat(person.gender()).as(person.id()).isEqualTo(name);
            }
        });
    }

    @Test
    void ac019EveryContractTypeAndRoleResolvesAndNamesMatch() {
        assertThat(appointments()).allSatisfy(appointment -> {
            assertThat(appointment.contractType())
                    .isEqualTo(record(types.contractTypeId(), appointment.contractTypeId()).getName());
            assertThat(appointment.roleName())
                    .isEqualTo(record(types.roleNameId(), appointment.roleNameId()).getName());
        });
    }

    @Test
    void ac020EveryBaseLocationResolvesWithItsTypeParentAndJurisdiction() {
        assertThat(appointments()).allSatisfy(appointment -> assertBaseLocation(appointment));
    }

    @Test
    void ac021EveryJudiciaryRoleResolvesAndNamesMatch() {
        assertThat(RESPONSES.stream().flatMap(person -> person.judiciaryRoles().stream()))
                .allSatisfy(role -> assertThat(role.name())
                        .isEqualTo(record(types.judiciaryRoleNameId(), role.judiciaryRoleNameId()).getName()));
    }

    @Test
    void ac022EveryTicketResolvesWithTheJurisdictionOfItsCategory() {
        assertThat(authorisations()).allSatisfy(authorisation -> {
            ReferenceDataRecord ticket = record(types.ticketId(), authorisation.ticketId());
            assertThat(authorisation.ticket()).isEqualTo(ticket.getName());
            long jurisdictionId = record(types.ticketCategoryId(), ticket.getTicketCategoryId()).getJurisdictionId();
            assertThat(authorisation.jurisdictionId()).isEqualTo(jurisdictionId);
            assertThat(authorisation.jurisdiction())
                    .isEqualTo(record(types.jurisdictionId(), jurisdictionId).getName());
        });
    }

    @Test
    void ac023EverySummaryAgreesWithTheDatedAuthorisations() {
        AppointmentStatus status = appointmentStatus();
        assertThat(RESPONSES).allSatisfy(person -> {
            assertThat(person.authorisations()).as(person.id()).isEqualTo(summarise(person.authorisationsWithDates()));
            Set<Long> shown = person.appointments().stream().map(AppointmentsResponse::appointmentId)
                    .collect(Collectors.toSet());
            assertThat(person.authorisationsWithDates()).as(person.id())
                    .allMatch(authorisation -> shown.contains(authorisation.appointmentId()));
            List<AuthorisationsResponse> current = RESPONSES.stream()
                    .filter(other -> other.id().equals(person.id()))
                    .flatMap(other -> other.authorisationsWithDates().stream())
                    .filter(authorisation -> status.isCurrent(authorisation.endDate()))
                    .distinct()
                    .toList();
            assertThat(person.authorisationsCurrent()).as(person.id()).isEqualTo(summarise(current));
        });
    }

    @Test
    void ec008AppointmentsAtBaseLocationsSharingANameEachUseTheirOwnRecord() {
        Map<String, List<AppointmentsResponse>> byName = appointments().stream()
                .collect(Collectors.groupingBy(AppointmentsResponse::baseLocation));
        List<AppointmentsResponse> shared = byName.values().stream()
                .filter(group -> group.stream().map(AppointmentsResponse::baseLocationId).distinct().count() >= 2)
                .findFirst().orElseThrow(() -> new AssertionError("no two base locations share a name"));
        AppointmentsResponse first = shared.get(0);
        AppointmentsResponse second = shared.stream()
                .filter(appointment -> appointment.baseLocationId() != first.baseLocationId())
                .findFirst().orElseThrow();

        for (AppointmentsResponse appointment : List.of(first, second)) {
            assertBaseLocation(appointment);
        }
        ReferenceDataRecord firstBase = record(types.baseLocationId(), first.baseLocationId());
        ReferenceDataRecord secondBase = record(types.baseLocationId(), second.baseLocationId());
        assertThat(first.baseLocation()).isEqualTo(second.baseLocation());
        if (!firstBase.getParentId().equals(secondBase.getParentId())) {
            assertThat(first.location()).isEqualTo(record(types.baseLocationParentId(), firstBase.getParentId())
                                                           .getName());
            assertThat(second.location()).isEqualTo(record(types.baseLocationParentId(), secondBase.getParentId())
                                                            .getName());
        }
    }

    /**
     * Each closed deferred item has cases to assert, so an empty dataset can't pass the checks above vacuously.
     */
    @Test
    void everyClosedDeferredItemHasCasesToCheck() {
        Map<String, Long> cases = Map.of(
                "DF-2 contract types", appointments().stream().count(),
                "DF-3 genders", RESPONSES.stream().count(),
                "DF-4 judiciary roles", RESPONSES.stream().mapToLong(person -> person.judiciaryRoles().size()).sum(),
                "DF-6 base locations", appointments().stream().map(AppointmentsResponse::baseLocationId).distinct()
                        .count(),
                "DF-9 tickets", authorisations().stream().count());
        assertThat(cases).allSatisfy((item, count) -> assertThat(count).as(item).isPositive());
    }

    private static void assertBaseLocation(AppointmentsResponse appointment) {
        ReferenceDataRecord base = record(types.baseLocationId(), appointment.baseLocationId());
        assertThat(appointment.baseLocation()).isEqualTo(base.getName());
        assertThat(appointment.courtName()).isEqualTo(base.getName());
        assertThat(appointment.courtType()).isEqualTo(record(types.baseLocationTypeId(), base.getTypeId()).getName());
        assertThat(appointment.location())
                .isEqualTo(record(types.baseLocationParentId(), base.getParentId()).getName());
        assertThat(appointment.type()).isEqualTo(record(types.jurisdictionId(), base.getJurisdictionId()).getName());
    }

    private static List<AuthorisationSummaryResponse> summarise(List<AuthorisationsResponse> authorisations) {
        Map<String, TreeSet<String>> grouped = new TreeMap<>();
        authorisations.forEach(authorisation -> grouped
                .computeIfAbsent(authorisation.jurisdiction(), name -> new TreeSet<>()).add(authorisation.ticket()));
        return grouped.entrySet().stream()
                .map(entry -> new AuthorisationSummaryResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    private static List<AppointmentsResponse> appointments() {
        return flat(CurrentOfficeHolderResponse::appointments);
    }

    private static List<AuthorisationsResponse> authorisations() {
        return flat(CurrentOfficeHolderResponse::authorisationsWithDates);
    }

    private static <T> List<T> flat(Function<CurrentOfficeHolderResponse, List<T>> list) {
        return RESPONSES.stream().flatMap(person -> list.apply(person).stream()).toList();
    }

    private static ReferenceDataRecord record(String type, Long id) {
        assertThat(id).as("%s id", type).isNotNull();
        return referenceData().findById(registry().resolve(type), id)
                .orElseThrow(() -> new AssertionError(type + " " + id + " does not resolve"));
    }
}
