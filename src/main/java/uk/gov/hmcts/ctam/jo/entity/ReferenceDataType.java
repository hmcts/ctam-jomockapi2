package uk.gov.hmcts.ctam.jo.entity;

import lombok.Value;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One declared reference-data type: its canonical attribute name, deprecated aliases, fixture path, the reference
 * fields and boolean fields its records carry, whether its record names must be unique, which of its reference
 * fields may be null, and the text fields its records carry.
 */
@Value
public class ReferenceDataType {

    String name;

    Set<String> aliases;

    String fixture;

    Set<ReferenceField> referenceFields;

    Set<BooleanField> booleanFields;

    boolean uniqueNames;

    /**
     * Always a subset of {@link #referenceFields}: the registry rejects any other declaration.
     */
    Set<ReferenceField> nullableReferenceFields;

    Set<TextField> textFields;

    /**
     * Copies the sets, so later changes to the caller's sets don't reach the type: aliases keep their iteration
     * order, and reference, boolean, nullable reference and text fields are held as {@link EnumSet}s, in declaration
     * order. Null means none.
     */
    public ReferenceDataType(String name, Set<String> aliases, String fixture, Set<ReferenceField> referenceFields,
                             Set<BooleanField> booleanFields, boolean uniqueNames,
                             Set<ReferenceField> nullableReferenceFields, Set<TextField> textFields) {
        this.name = name;
        this.aliases = aliases == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(aliases));
        this.fixture = fixture;
        this.referenceFields = copyOf(referenceFields, ReferenceField.class);
        this.booleanFields = copyOf(booleanFields, BooleanField.class);
        this.uniqueNames = uniqueNames;
        this.nullableReferenceFields = copyOf(nullableReferenceFields, ReferenceField.class);
        this.textFields = copyOf(textFields, TextField.class);
    }

    private static <E extends Enum<E>> Set<E> copyOf(Set<E> values, Class<E> type) {
        EnumSet<E> copy = EnumSet.noneOf(type);
        if (values != null) {
            copy.addAll(values);
        }
        return Collections.unmodifiableSet(copy);
    }
}
