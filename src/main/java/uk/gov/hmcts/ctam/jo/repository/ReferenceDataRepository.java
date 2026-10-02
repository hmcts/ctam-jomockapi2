package uk.gov.hmcts.ctam.jo.repository;

import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the records of a reference-data type.
 */
public interface ReferenceDataRepository {

    /** Every record of the type, in ascending id order. */
    List<ReferenceDataRecord> findAll(ReferenceDataType type);

    Optional<ReferenceDataRecord> findById(ReferenceDataType type, long id);
}
