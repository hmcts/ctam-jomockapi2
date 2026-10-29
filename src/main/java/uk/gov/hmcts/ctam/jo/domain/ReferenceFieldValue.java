package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A reference field's value in a response: present with an id, or present and null for a top-level record of a type
 * that declares the field nullable. A response leaves out a reference field its type doesn't declare by holding no
 * value object at all.
 */
public record ReferenceFieldValue(@JsonValue Long value) {
}
