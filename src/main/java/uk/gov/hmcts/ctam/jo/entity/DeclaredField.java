package uk.gov.hmcts.ctam.jo.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * An optional record field that a type declares in its configuration ({@code reference-fields},
 * {@code boolean-fields}). Records of a type that doesn't declare the field never carry it.
 *
 * @param <T> the field's value type
 */
public interface DeclaredField<T> {

    /**
     * The field's name in fixtures, configuration and responses.
     */
    String fieldName();

    /**
     * The record's value for this field, or null if the record doesn't carry it.
     */
    T readFrom(ReferenceDataRecord record);

    /**
     * The field of the given kind with this name; the match is exact and case-sensitive.
     */
    static <E extends Enum<E> & DeclaredField<?>> Optional<E> byName(Class<E> kind, String fieldName) {
        return Arrays.stream(kind.getEnumConstants()).filter(field -> field.fieldName().equals(fieldName)).findFirst();
    }
}
