package uk.gov.hmcts.ctam.jo.testsupport;

import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceFieldValue;
import uk.gov.hmcts.ctam.jo.entity.BooleanField;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;
import uk.gov.hmcts.ctam.jo.entity.TextField;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reference-data test objects for the unit suite, all built from the same standard values so no test has to
 * restate them.
 */
public final class ReferenceDataFixtures {

    public static final Instant CREATED_AT = Instant.parse("2024-01-15T09:00:00Z");

    public static final Instant UPDATED_AT = Instant.parse("2024-06-03T10:30:00Z");

    /**
     * When a record with an end date was last updated.
     */
    public static final Instant ENDED_UPDATED_AT = Instant.parse("2025-04-01T08:00:00Z");

    public static final LocalDate START_DATE = LocalDate.parse("2024-01-01");

    public static final LocalDate END_DATE = LocalDate.parse("2025-03-31");

    private ReferenceDataFixtures() {
    }

    /**
     * A current record (no end date) with the standard values; override any field before {@code build()}.
     */
    public static ReferenceDataRecord.ReferenceDataRecordBuilder recordBuilder(long id, String name) {
        return ReferenceDataRecord.builder()
                .id(id)
                .name(name)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .startDate(START_DATE);
    }

    public static ReferenceDataRecord record(long id, String name) {
        return recordBuilder(id, name).build();
    }

    public static ReferenceDataRecord record(long id, String name, LocalDate endDate) {
        return recordBuilder(id, name).endDate(endDate).build();
    }

    public static ReferenceDataResponse response(long id, String name) {
        return new ReferenceDataResponse(id, name, null, null, null, null, null, null, null, CREATED_AT, UPDATED_AT,
                                         START_DATE, null);
    }

    public static ReferenceDataResponse endedResponse(long id, String name) {
        return new ReferenceDataResponse(id, name, null, null, null, null, null, null, null, CREATED_AT,
                                         ENDED_UPDATED_AT, START_DATE, END_DATE);
    }

    /**
     * A current record that carries all three reference fields.
     */
    public static ReferenceDataResponse referencingResponse(long id, String name, long typeId, long parentId,
                                                            long jurisdictionId) {
        return new ReferenceDataResponse(id, name, value(typeId), value(parentId), value(jurisdictionId), null, null,
                                         null, null, CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    /**
     * A current top-level record of a type that declares all three reference fields, with {@code parent_id}
     * nullable: the parent is present and null.
     */
    public static ReferenceDataResponse nullableParentResponse(long id, String name, long typeId,
                                                               long jurisdictionId) {
        return new ReferenceDataResponse(id, name, value(typeId), new ReferenceFieldValue(null), value(jurisdictionId),
                                         null, null, null, null, CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    /**
     * A current record that carries the boolean field {@code salaried} and no reference fields.
     */
    public static ReferenceDataResponse salariedResponse(long id, String name, boolean salaried) {
        return new ReferenceDataResponse(id, name, null, null, null, null, null, null, salaried, CREATED_AT, UPDATED_AT,
                                         START_DATE, null);
    }

    /**
     * A current record that carries the text field {@code integration_code} and no reference fields.
     */
    public static ReferenceDataResponse textResponse(long id, String name, String integrationCode) {
        return new ReferenceDataResponse(id, name, null, null, null, null, null, integrationCode, null, CREATED_AT,
                                         UPDATED_AT, START_DATE, null);
    }

    /**
     * A current record that carries only the reference field {@code parent_category_id}; a null value is a present
     * null, as for a top-level record.
     */
    public static ReferenceDataResponse parentCategoryResponse(long id, String name, Long parentCategoryId) {
        return new ReferenceDataResponse(id, name, null, null, null, null, new ReferenceFieldValue(parentCategoryId),
                                         null, null, CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    /**
     * A current record that carries only the reference field {@code ticket_category_id}.
     */
    public static ReferenceDataResponse ticketCategoryResponse(long id, String name, long ticketCategoryId) {
        return new ReferenceDataResponse(id, name, null, null, null, value(ticketCategoryId), null, null, null,
                                         CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    /**
     * A current record that carries all three reference fields and the boolean field {@code salaried}.
     */
    public static ReferenceDataResponse fullResponse(long id, String name, long typeId, long parentId,
                                                     long jurisdictionId, boolean salaried) {
        return new ReferenceDataResponse(id, name, value(typeId), value(parentId), value(jurisdictionId), null, null,
                                         null, salaried, CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    /**
     * A current record that carries all three reference fields and the text field {@code integration_code}.
     */
    public static ReferenceDataResponse referencingTextResponse(long id, String name, long typeId, long parentId,
                                                                long jurisdictionId, String integrationCode) {
        return new ReferenceDataResponse(id, name, value(typeId), value(parentId), value(jurisdictionId), null, null,
                                         integrationCode, null, CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    public static ReferenceFieldValue value(long id) {
        return new ReferenceFieldValue(id);
    }

    /**
     * A resolved type declaring no reference, boolean or text fields.
     */
    public static ReferenceDataType plainType() {
        return resolvedType(Set.of(), Set.of(), Set.of(), Set.of());
    }

    /**
     * A resolved type declaring the three reference fields 003 added, none of them nullable.
     */
    public static ReferenceDataType referencingType() {
        return resolvedType(firstReferenceFields(), Set.of(), Set.of(), Set.of());
    }

    /**
     * A resolved type declaring the three reference fields 003 added, with {@code parent_id} nullable.
     */
    public static ReferenceDataType nullableParentType() {
        return resolvedType(firstReferenceFields(), EnumSet.of(ReferenceField.PARENT_ID), Set.of(), Set.of());
    }

    /**
     * A resolved type declaring the boolean field {@code salaried} and no reference fields.
     */
    public static ReferenceDataType salariedType() {
        return resolvedType(Set.of(), Set.of(), EnumSet.of(BooleanField.SALARIED), Set.of());
    }

    /**
     * A resolved type declaring the three reference fields 003 added and {@code salaried}.
     */
    public static ReferenceDataType fullType() {
        return resolvedType(firstReferenceFields(), Set.of(), EnumSet.of(BooleanField.SALARIED), Set.of());
    }

    /**
     * A resolved type declaring every reference field, including any added later, none of them nullable.
     */
    public static ReferenceDataType everyReferenceType() {
        return resolvedType(EnumSet.allOf(ReferenceField.class), Set.of(), Set.of(), Set.of());
    }

    /**
     * A resolved type declaring only the reference field {@code parent_category_id}, nullable.
     */
    public static ReferenceDataType parentCategoryType() {
        return resolvedType(EnumSet.of(ReferenceField.PARENT_CATEGORY_ID),
                            EnumSet.of(ReferenceField.PARENT_CATEGORY_ID), Set.of(), Set.of());
    }

    /**
     * A resolved type declaring only the reference field {@code ticket_category_id}.
     */
    public static ReferenceDataType ticketCategoryType() {
        return resolvedType(EnumSet.of(ReferenceField.TICKET_CATEGORY_ID), Set.of(), Set.of(), Set.of());
    }

    /**
     * A resolved type declaring the text field {@code integration_code} and no reference or boolean fields.
     */
    public static ReferenceDataType textType() {
        return resolvedType(Set.of(), Set.of(), Set.of(), EnumSet.of(TextField.INTEGRATION_CODE));
    }

    private static Set<ReferenceField> firstReferenceFields() {
        return EnumSet.of(ReferenceField.TYPE_ID, ReferenceField.PARENT_ID, ReferenceField.JURISDICTION_ID);
    }

    private static ReferenceDataType resolvedType(Set<ReferenceField> referenceFields,
                                                  Set<ReferenceField> nullableReferenceFields,
                                                  Set<BooleanField> booleanFields, Set<TextField> textFields) {
        return new ReferenceDataType("widgets", Set.of(), "classpath:x.json", referenceFields, booleanFields, true,
                                     nullableReferenceFields, textFields);
    }

    /**
     * A declared type whose records carry no reference fields.
     */
    public static TypeProperties type(String name, List<String> aliases, String fixture) {
        return new TypeProperties(name, aliases, fixture, List.of(), List.of(), null, List.of(), List.of());
    }

    public static ReferenceDataTypeRegistry registry(TypeProperties... types) {
        return new ReferenceDataTypeRegistry(new ReferenceDataProperties(List.of(types)));
    }

    /**
     * A registry with no types declared, as when {@code jo.reference-data.types} is unset.
     */
    public static ReferenceDataTypeRegistry emptyRegistry() {
        return new ReferenceDataTypeRegistry(new ReferenceDataProperties(null));
    }
}
