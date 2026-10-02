package uk.gov.hmcts.ctam.jo.testsupport;

import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        return new ReferenceDataResponse(id, name, CREATED_AT, UPDATED_AT, START_DATE, null);
    }

    public static ReferenceDataResponse endedResponse(long id, String name) {
        return new ReferenceDataResponse(id, name, CREATED_AT, ENDED_UPDATED_AT, START_DATE, END_DATE);
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
