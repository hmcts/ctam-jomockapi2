package uk.gov.hmcts.ctam.jo.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceFieldValue;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;

import java.util.List;

/**
 * Maps internal fixture records to API DTOs. Any unmapped target property fails the build. The record's type decides
 * which reference fields the response carries.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, imports = ReferenceField.class)
public interface ReferenceDataMapper {

    @Mapping(target = "typeId", expression = "java(referenceField(record, ReferenceField.TYPE_ID, type))")
    @Mapping(target = "parentId", expression = "java(referenceField(record, ReferenceField.PARENT_ID, type))")
    @Mapping(target = "jurisdictionId",
             expression = "java(referenceField(record, ReferenceField.JURISDICTION_ID, type))")
    @Mapping(target = "ticketCategoryId",
             expression = "java(referenceField(record, ReferenceField.TICKET_CATEGORY_ID, type))")
    ReferenceDataResponse toResponse(ReferenceDataRecord record, @Context ReferenceDataType type);

    List<ReferenceDataResponse> toResponses(List<ReferenceDataRecord> records, @Context ReferenceDataType type);

    /**
     * No value object for a field the type doesn't declare, so the response leaves it out; otherwise the record's
     * value, which is null only for a field the type declares nullable.
     */
    default ReferenceFieldValue referenceField(ReferenceDataRecord record, ReferenceField field,
                                               ReferenceDataType type) {
        return type.getReferenceFields().contains(field) ? new ReferenceFieldValue(field.readFrom(record)) : null;
    }
}
