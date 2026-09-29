package uk.gov.hmcts.ctam.jo.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;

import java.util.List;

/**
 * Maps internal fixture records to API DTOs. Any unmapped target property fails the build.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReferenceDataMapper {

    ReferenceDataResponse toResponse(ReferenceDataRecord record);

    List<ReferenceDataResponse> toResponses(List<ReferenceDataRecord> records);
}
