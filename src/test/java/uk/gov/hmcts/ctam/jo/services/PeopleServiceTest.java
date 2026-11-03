package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ctam.jo.domain.AppointmentsResponse;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.domain.PaginationResponse;
import uk.gov.hmcts.ctam.jo.domain.PeopleApiResponse;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidPersonIdException;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidQueryParameterException;
import uk.gov.hmcts.ctam.jo.exceptions.PersonNotFoundException;
import uk.gov.hmcts.ctam.jo.repository.PeopleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.FLAG_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.MALFORMED_PERSON_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PAGE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PERSON_NOT_FOUND;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.PER_PAGE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UPDATED_SINCE_INVALID;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractMessages.UPDATED_SINCE_REQUIRED;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.FIRST_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.MINIMAL;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.SECOND_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.mapper;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.repository;

/**
 * The People service with real parsing, paging and mapping over a stubbed repository (013 US1 to US4).
 */
class PeopleServiceTest {

    /**
     * 120 office holders, three per day from 2025-01-01, so ties on last_updated are ordered by id (EC-007).
     */
    private static final List<OfficeHolder> MANY = IntStream.range(0, 120)
            .mapToObj(index -> holder("%08d-0000-4000-8000-000000000000".formatted(index),
                                      LocalDate.parse("2025-01-01").plusDays(index / 3)))
            .sorted(Comparator.comparing(OfficeHolder::getLastUpdated).thenComparing(OfficeHolder::getId))
            .toList();

    private static PeopleService service(List<OfficeHolder> holders) {
        PeopleRepository stub = new PeopleRepository() {
            @Override
            public List<OfficeHolder> findAll() {
                return holders;
            }

            @Override
            public Optional<OfficeHolder> findById(String lowerCaseId) {
                return holders.stream().filter(holder -> holder.getId().equals(lowerCaseId)).findFirst();
            }
        };
        return new PeopleService(stub, new QueryParameterParser(), new PersonIdParser(), new Paginator(),
                                 mapper());
    }

    private final PeopleService many = service(MANY);

    private final PeopleService minimal = service(repository(MINIMAL).findAll());

    @Test
    void theFilterIsInclusiveAndKeepsTheOrder() {
        PeopleApiResponse response = many.search("2025-01-10", null, "500", null);

        assertThat(response.pagination().results()).isEqualTo(120 - 27);
        assertThat(response.results()).extracting(CurrentOfficeHolderResponse::lastUpdated)
                .allMatch(date -> !date.isBefore(LocalDate.parse("2025-01-10")))
                .contains(LocalDate.parse("2025-01-10"));
        assertThat(response.results()).extracting(CurrentOfficeHolderResponse::id)
                .containsExactlyElementsOf(MANY.subList(27, 120).stream().map(OfficeHolder::getId).toList());
    }

    @Test
    void tiesOnLastUpdatedAreOrderedById() {
        PeopleApiResponse response = many.search("2025-01-01", null, "3", null);

        assertThat(response.results()).extracting(CurrentOfficeHolderResponse::id).containsExactly(
                "00000000-0000-4000-8000-000000000000", "00000001-0000-4000-8000-000000000000",
                "00000002-0000-4000-8000-000000000000");
    }

    @Test
    void noMatchesGiveNoPages() {
        PeopleApiResponse response = many.search("2099-01-01", null, null, null);

        assertThat(response.results()).isEmpty();
        assertThat(response.pagination()).isEqualTo(new PaginationResponse(0, 0, 1, 50, false));
    }

    @Test
    void theDefaultsAreTheFirstPageOfFifty() {
        PeopleApiResponse response = many.search("2000-01-01", null, null, null);

        assertThat(response.results()).hasSize(50);
        assertThat(response.pagination()).isEqualTo(new PaginationResponse(120, 3, 1, 50, true));
    }

    @Test
    void leadingZerosAreAllowed() {
        assertThat(many.search("2000-01-01", "007", "10", null).pagination().currentPage()).isEqualTo(7);
        assertThat(many.search("2000-01-01", "002", null, null)).isEqualTo(many.search("2000-01-01", "2", null, null));
    }

    @Test
    void hugePageSizePutsEveryMatchOnPageOne() {
        PeopleApiResponse response = many.search("2000-01-01", null, "9223372036854775807", null);

        assertThat(response.results()).hasSize(120);
        assertThat(response.pagination()).isEqualTo(new PaginationResponse(120, 1, 1, Long.MAX_VALUE, false));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "null, 0, abc, yes, " + UPDATED_SINCE_REQUIRED,
        "yesterday, 0, abc, yes, " + UPDATED_SINCE_INVALID,
        "2000-01-01, 0, abc, yes, " + PAGE_INVALID,
        "2000-01-01, 1, abc, yes, " + PER_PAGE_INVALID,
        "2000-01-01, 1, 1, yes, " + FLAG_INVALID})
    void parametersAreCheckedInTheContractsOrder(String updatedSince, String page, String perPage, String flag,
                                                 String message) {
        assertThatThrownBy(() -> many.search(updatedSince, page, perPage, flag))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessage(message);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"false", "true"})
    void theSingleRouteReturnsTheSameObjectAsTheSearch(String flag) {
        for (String id : List.of(FIRST_ID, SECOND_ID)) {
            CurrentOfficeHolderResponse fromSearch = minimal.search("2000-01-01", null, null, flag).results().stream()
                    .filter(holder -> holder.id().equals(id)).findFirst().orElseThrow();
            assertThat(minimal.getById(id, flag)).isEqualTo(fromSearch);
        }
    }

    @Test
    void anUpperCaseIdMatches() {
        assertThat(minimal.getById(FIRST_ID.toUpperCase(), null).id()).isEqualTo(FIRST_ID);
    }

    @Test
    void anUnknownWellFormedIdIsNotFound() {
        assertThatThrownBy(() -> minimal.getById("33333333-3333-4333-8333-333333333333", null))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessage(PERSON_NOT_FOUND);
    }

    @Test
    void malformedIdIsRejectedButABadFlagIsReportedFirst() {
        assertThatThrownBy(() -> minimal.getById("abc", null))
                .isInstanceOf(InvalidPersonIdException.class)
                .hasMessage(MALFORMED_PERSON_ID);
        assertThatThrownBy(() -> minimal.getById("abc", "yes"))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessage(FLAG_INVALID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "false")
    void withoutTheFlagOnlyCurrentAppointmentsAreShown(String flag) {
        assertThat(minimal.getById(FIRST_ID, flag).appointments()).extracting(AppointmentsResponse::appointmentId)
                .containsExactly(700002L);
    }

    @Test
    void withTheFlagPreviousAppointmentsAreShownToo() {
        assertThat(minimal.getById(FIRST_ID, "true").appointments()).extracting(AppointmentsResponse::appointmentId)
                .containsExactly(700001L, 700002L);
    }

    @Test
    void anOfficeHolderWithOnlyPreviousAppointmentsKeepsOnlyJudiciaryRolesWithoutTheFlag() {
        OfficeHolder first = repository(MINIMAL).findById(FIRST_ID).orElseThrow();
        List<OfficeHolder.ResolvedAppointment> previousOnly = new ArrayList<>(first.getAppointments().subList(0, 1));
        OfficeHolder onlyPrevious = OfficeHolder.builder()
                .id(FIRST_ID).lastUpdated(first.getLastUpdated()).sex("Female")
                .appointments(previousOnly)
                .judiciaryRoles(first.getJudiciaryRoles())
                .authorisationsWithDates(first.getAuthorisationsWithDates().subList(0, 1))
                .build();

        CurrentOfficeHolderResponse without = service(List.of(onlyPrevious)).getById(FIRST_ID, null);

        assertThat(without.appointments()).isEmpty();
        assertThat(without.authorisationsWithDates()).isEmpty();
        assertThat(without.authorisations()).isEmpty();
        assertThat(without.authorisationsCurrent()).isEmpty();
        assertThat(without.judiciaryRoles()).hasSize(2);
        assertThat(service(List.of(onlyPrevious)).search("2000-01-01", null, null, null).results()).hasSize(1);
    }

    @Test
    void theFlagNeverChangesWhoMatchesTheirOrderOrThePagination() {
        for (String page : List.of("1", "2")) {
            PeopleApiResponse without = minimal.search("2000-01-01", page, "1", "false");
            PeopleApiResponse with = minimal.search("2000-01-01", page, "1", "true");

            assertThat(with.pagination()).isEqualTo(without.pagination());
            assertThat(with.results()).extracting(CurrentOfficeHolderResponse::id)
                    .isEqualTo(without.results().stream().map(CurrentOfficeHolderResponse::id).toList());
        }
    }

    private static OfficeHolder holder(String id, LocalDate lastUpdated) {
        return OfficeHolder.builder().id(id).lastUpdated(lastUpdated).sex("Female").appointments(List.of())
                .judiciaryRoles(List.of()).authorisationsWithDates(List.of()).build();
    }
}
