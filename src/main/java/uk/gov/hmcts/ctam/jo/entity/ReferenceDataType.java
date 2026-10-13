package uk.gov.hmcts.ctam.jo.entity;

import lombok.Value;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One declared reference-data type: its canonical attribute name, deprecated aliases, fixture location, the
 * reference fields its records carry and whether its record names must be unique.
 */
@Value
public class ReferenceDataType {

    String name;

    Set<String> aliases;

    String fixture;

    Set<ReferenceField> referenceFields;

    boolean uniqueNames;

    /**
     * Copies the sets, so later changes to the caller's sets don't reach the type: aliases keep their iteration
     * order, and reference fields are held as an {@link EnumSet}, in declaration order. Null means none.
     */
    public ReferenceDataType(String name, Set<String> aliases, String fixture, Set<ReferenceField> referenceFields,
                             boolean uniqueNames) {
        this.name = name;
        this.aliases = aliases == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(aliases));
        this.fixture = fixture;
        EnumSet<ReferenceField> fields = EnumSet.noneOf(ReferenceField.class);
        if (referenceFields != null) {
            fields.addAll(referenceFields);
        }
        this.referenceFields = Collections.unmodifiableSet(fields);
        this.uniqueNames = uniqueNames;
    }
}
