package uk.gov.hmcts.ctam.jo.entity;

import java.util.function.Function;

/**
 * An optional text field. A type declares the fields its records carry ({@code text-fields}); records of every other
 * type never carry them.
 */
public enum TextField implements DeclaredField<String> {

    INTEGRATION_CODE("integration_code", ReferenceDataRecord::getIntegrationCode);

    private final String fieldName;

    private final Function<ReferenceDataRecord, String> accessor;

    TextField(String fieldName, Function<ReferenceDataRecord, String> accessor) {
        this.fieldName = fieldName;
        this.accessor = accessor;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    public String readFrom(ReferenceDataRecord record) {
        return accessor.apply(record);
    }
}
