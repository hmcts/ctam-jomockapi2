package uk.gov.hmcts.ctam.jo.entity;

import lombok.Value;

import java.util.Set;

/**
 * One declared reference-data type: its canonical attribute name, deprecated aliases, fixture location and the
 * reference fields its records carry.
 */
@Value
public class ReferenceDataType {

    String name;

    Set<String> aliases;

    String fixture;

    Set<ReferenceField> referenceFields;

    /**
     * Null aliases or reference fields mean none.
     */
    public ReferenceDataType(String name, Set<String> aliases, String fixture, Set<ReferenceField> referenceFields) {
        this.name = name;
        this.aliases = aliases == null ? Set.of() : aliases;
        this.fixture = fixture;
        this.referenceFields = referenceFields == null ? Set.of() : referenceFields;
    }
}
