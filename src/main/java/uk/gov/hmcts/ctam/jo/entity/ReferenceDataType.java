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
}
