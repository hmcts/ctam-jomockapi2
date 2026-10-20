package uk.gov.hmcts.ctam.jo.entity;

import java.util.function.Function;

/**
 * An optional yes/no field. A type declares the fields its records carry ({@code boolean-fields}); records of every
 * other type never carry them.
 */
public enum BooleanField implements DeclaredField<Boolean> {

    SALARIED("salaried", ReferenceDataRecord::getSalaried);

    private final String fieldName;

    private final Function<ReferenceDataRecord, Boolean> accessor;

    BooleanField(String fieldName, Function<ReferenceDataRecord, Boolean> accessor) {
        this.fieldName = fieldName;
        this.accessor = accessor;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public Boolean readFrom(ReferenceDataRecord record) {
        return accessor.apply(record);
    }
}
