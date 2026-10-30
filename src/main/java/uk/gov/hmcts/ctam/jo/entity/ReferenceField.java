package uk.gov.hmcts.ctam.jo.entity;

import java.util.function.Function;

/**
 * An optional field holding the id of a record in another reference-data type. A type declares the fields its
 * records carry ({@code reference-fields}); records of every other type never carry them.
 */
public enum ReferenceField implements DeclaredField<Long> {

    TYPE_ID("type_id", ReferenceDataRecord::getTypeId),
    PARENT_ID("parent_id", ReferenceDataRecord::getParentId),
    JURISDICTION_ID("jurisdiction_id", ReferenceDataRecord::getJurisdictionId),
    TICKET_CATEGORY_ID("ticket_category_id", ReferenceDataRecord::getTicketCategoryId);

    private final String fieldName;

    private final Function<ReferenceDataRecord, Long> accessor;

    ReferenceField(String fieldName, Function<ReferenceDataRecord, Long> accessor) {
        this.fieldName = fieldName;
        this.accessor = accessor;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public Long readFrom(ReferenceDataRecord record) {
        return accessor.apply(record);
    }
}
