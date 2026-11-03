package uk.gov.hmcts.ctam.jo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ctam.jo.domain.CurrentOfficeHolderResponse;
import uk.gov.hmcts.ctam.jo.domain.PeopleApiResponse;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;
import uk.gov.hmcts.ctam.jo.exceptions.PersonNotFoundException;
import uk.gov.hmcts.ctam.jo.mappers.PeopleMapper;
import uk.gov.hmcts.ctam.jo.repository.PeopleRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Serves the People routes (013 FR-001, FR-002): parse the parameters in the contract's order, filter, page, map. The
 * controller passes every parameter through as the raw string, so every check and its message happens here, in the
 * order of EC-010.
 */
@Service
@RequiredArgsConstructor
public class PeopleService {

    /**
     * The search route's date parameter.
     */
    public static final String UPDATED_SINCE = "updated_since";

    public static final String PAGE = "page";

    public static final String PER_PAGE = "per_page";

    /**
     * The flag both routes accept.
     */
    public static final String INCLUDE_PREVIOUS_APPOINTMENTS = "include_previous_appointments";

    private final PeopleRepository repository;

    private final QueryParameterParser parameters;

    private final PersonIdParser personIdParser;

    private final Paginator paginator;

    private final PeopleMapper mapper;

    /**
     * One page of the office holders whose last_updated is on or after {@code updatedSince} (FR-011), by last_updated,
     * then id (FR-012). Parameters are checked in the contract's order: updated_since, page, per_page, then the flag.
     */
    public PeopleApiResponse search(String updatedSince, String page, String perPage, String includePrevious) {
        LocalDate since = parameters.requiredDate(UPDATED_SINCE, updatedSince);
        long pageNumber = parameters.positiveWholeNumber(PAGE, page, PageRequest.DEFAULT_PAGE);
        long pageSize = parameters.positiveWholeNumber(PER_PAGE, perPage, PageRequest.DEFAULT_PER_PAGE);
        boolean includePreviousAppointments = parameters.flag(INCLUDE_PREVIOUS_APPOINTMENTS, includePrevious);

        List<OfficeHolder> matches = repository.findAll().stream()
                .filter(holder -> !holder.getLastUpdated().isBefore(since))
                .toList();
        Page<OfficeHolder> result = paginator.paginate(matches, new PageRequest(pageNumber, pageSize));
        return new PeopleApiResponse(result.results().stream()
                                             .map(holder -> mapper.toResponse(holder, includePreviousAppointments))
                                             .toList(),
                                     result.pagination());
    }

    /**
     * One office holder, the same object a search returns for them with the same flag (FR-026). The flag is checked
     * first, then the id's format, then the lookup (EC-010).
     */
    public CurrentOfficeHolderResponse getById(String rawId, String includePrevious) {
        boolean includePreviousAppointments = parameters.flag(INCLUDE_PREVIOUS_APPOINTMENTS, includePrevious);
        String id = personIdParser.parse(rawId);
        return repository.findById(id)
                .map(holder -> mapper.toResponse(holder, includePreviousAppointments))
                .orElseThrow(() -> new PersonNotFoundException(id));
    }
}
