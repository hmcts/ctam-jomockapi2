package uk.gov.hmcts.ctam.jo.entity;

import lombok.Value;

import java.util.Set;

/**
 * One declared reference-data type: its canonical attribute name, deprecated aliases and fixture location.
 */
@Value
public class ReferenceDataType {

    String name;

    Set<String> aliases;

    String fixture;
}
