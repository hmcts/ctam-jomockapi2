package uk.gov.hmcts.ctam.jo.entity;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * An optional field holding the id of a record in another reference-data type. A type declares the fields its
 * records carry ({@code reference-fields}); records of every other type never carry them.
 */
public enum ReferenceField {

    TYPE_ID("type_id", ReferenceDataRecord::getTypeId),
    PARENT_ID("parent_id", ReferenceDataRecord::getParentId),
    JURISDICTION_ID("jurisdiction_id", ReferenceDataRecord::getJurisdictionId);

    private final String fieldName;

    private final Function<ReferenceDataRecord, Long> accessor;

    ReferenceField(String fieldName, Function<ReferenceDataRecord, Long> accessor) {
        this.fieldName = fieldName;
        this.accessor = accessor;
    }

    /**
     * The field's name in fixtures, configuration and responses.
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * The record's value for this field, or null if the record doesn't carry it.
     */
    public Long valueOf(ReferenceDataRecord record) {
        return accessor.apply(record);
    }

    public static Optional<ReferenceField> fromFieldName(String fieldName) {
        return Arrays.stream(values()).filter(field -> field.fieldName.equals(fieldName)).findFirst();
    }
}
