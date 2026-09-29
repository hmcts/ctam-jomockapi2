package uk.gov.hmcts.ctam.jo.services;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataApiResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.exceptions.InvalidReferenceIdException;
import uk.gov.hmcts.ctam.jo.exceptions.ReferenceDataNotFoundException;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;
import uk.gov.hmcts.ctam.jo.mappers.ReferenceDataMapper;
import uk.gov.hmcts.ctam.jo.repository.ReferenceDataRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ReferenceDataServiceTest {

    private final ReferenceDataTypeRegistry registry = new ReferenceDataTypeRegistry(new ReferenceDataProperties(
            List.of(new TypeProperties("widgets", List.of("widget"), "classpath:widgets.json"),
                    new TypeProperties("gadgets", List.of(), "classpath:gadgets.json"))));

    private final ReferenceDataRepository repository = mock(ReferenceDataRepository.class);

    private final ReferenceDataMapper mapper = Mappers.getMapper(ReferenceDataMapper.class);

    private final ReferenceDataService service =
            new ReferenceDataService(registry, repository, mapper, new ReferenceIdParser());

    @Test
    void passesTheResolvedTypeToTheRepository() {
        ReferenceDataType gadgets = registry.resolve("gadgets");
        given(repository.findAll(gadgets)).willReturn(List.of(record(10)));

        service.getAll("gadgets");

        verify(repository).findAll(gadgets);
    }

    @Test
    void resolvesAnAliasToTheSameType() {
        given(repository.findAll(any())).willReturn(List.of());

        service.getAll("widget");

        verify(repository).findAll(registry.resolve("widgets"));
    }

    @Test
    void keepsTheRepositoryOrder() {
        given(repository.findAll(any())).willReturn(List.of(record(10), record(20), record(30)));

        ReferenceDataApiResponse response = service.getAll("widgets");

        assertThat(response.results()).extracting(ReferenceDataResponse::id).containsExactly(10L, 20L, 30L);
    }

    @Test
    void anEmptyFixtureGivesEmptyResults() {
        given(repository.findAll(any())).willReturn(List.of());

        assertThat(service.getAll("widgets").results()).isEmpty();
    }

    @Test
    void anUnsupportedNameNeverReachesTheRepository() {
        assertThatThrownBy(() -> service.getAll("unknown")).isInstanceOf(UnsupportedReferenceDataTypeException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void getByIdReturnsTheMappedRecord() {
        ReferenceDataType widgets = registry.resolve("widgets");
        given(repository.findById(widgets, 70)).willReturn(Optional.of(record(70)));

        assertThat(service.getById("widget", "070").id()).isEqualTo(70);
        verify(repository).findById(widgets, 70);
    }

    @Test
    void getByIdThrowsNotFoundForAnUnknownId() {
        given(repository.findById(any(), anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("widgets", "15"))
                .isInstanceOf(ReferenceDataNotFoundException.class)
                .hasMessage("Reference data record not found.");
    }

    @Test
    void getByIdTreatsOverflowAsNotFoundWithoutALookup() {
        assertThatThrownBy(() -> service.getById("widgets", "99999999999999999999"))
                .isInstanceOf(ReferenceDataNotFoundException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void getByIdRejectsAMalformedId() {
        assertThatThrownBy(() -> service.getById("widgets", "abc")).isInstanceOf(InvalidReferenceIdException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void getByIdChecksTheTypeBeforeTheId() {
        assertThatThrownBy(() -> service.getById("unknown", "abc"))
                .isInstanceOf(UnsupportedReferenceDataTypeException.class);
    }

    private static ReferenceDataRecord record(long id) {
        return ReferenceDataRecord.builder()
                .id(id)
                .name("Record " + id)
                .createdAt(Instant.parse("2024-01-15T09:00:00Z"))
                .updatedAt(Instant.parse("2024-06-03T10:30:00Z"))
                .startDate(LocalDate.parse("2024-01-01"))
                .build();
    }
}
