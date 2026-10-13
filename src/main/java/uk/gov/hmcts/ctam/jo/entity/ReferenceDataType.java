package uk.gov.hmcts.ctam.jo.entity;

import lombok.Value;

import java.util.Collections;
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
     * Copies the sets, keeping their iteration order; null aliases or reference fields mean none.
     */
    public ReferenceDataType(String name, Set<String> aliases, String fixture, Set<ReferenceField> referenceFields,
                             boolean uniqueNames) {
        this.name = name;
        this.aliases = copyOf(aliases);
        this.fixture = fixture;
        this.referenceFields = copyOf(referenceFields);
        this.uniqueNames = uniqueNames;
    }

    private static <T> Set<T> copyOf(Set<T> values) {
        return values == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
