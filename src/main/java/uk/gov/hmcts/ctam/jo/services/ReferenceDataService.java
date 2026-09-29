package uk.gov.hmcts.ctam.jo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataApiResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.exceptions.ReferenceDataNotFoundException;
import uk.gov.hmcts.ctam.jo.mappers.ReferenceDataMapper;
import uk.gov.hmcts.ctam.jo.repository.ReferenceDataRepository;

import java.util.OptionalLong;

/**
 * Serves every configured reference-data type through one code path: resolve the type, read its records,
 * map them to DTOs. Nothing here is specific to any one type (Principle VII).
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final ReferenceDataTypeRegistry registry;

    private final ReferenceDataRepository repository;

    private final ReferenceDataMapper mapper;

    private final ReferenceIdParser referenceIdParser;

    public ReferenceDataApiResponse getAll(String attributeName) {
        ReferenceDataType type = registry.resolve(attributeName);
        return new ReferenceDataApiResponse(mapper.toResponses(repository.findAll(type)));
    }

    /**
     * Checks in the contract's order: the type first, then the id's format, then the lookup. An id too large
     * to exist is treated like any other unknown id.
     */
    public ReferenceDataResponse getById(String attributeName, String rawReferenceId) {
        ReferenceDataType type = registry.resolve(attributeName);
        OptionalLong id = referenceIdParser.parse(rawReferenceId);
        if (id.isEmpty()) {
            throw notFound(type, rawReferenceId);
        }
        return repository.findById(type, id.getAsLong())
                .map(mapper::toResponse)
                .orElseThrow(() -> notFound(type, rawReferenceId));
    }

    private static ReferenceDataNotFoundException notFound(ReferenceDataType type, String rawReferenceId) {
        return new ReferenceDataNotFoundException(type.getName() + "/" + rawReferenceId);
    }
}
