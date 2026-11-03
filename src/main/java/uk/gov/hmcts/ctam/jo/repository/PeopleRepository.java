package uk.gov.hmcts.ctam.jo.repository;

import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the office holders.
 */
public interface PeopleRepository {

    /**
     * Every office holder, by ascending last_updated, then id (013 FR-012).
     */
    List<OfficeHolder> findAll();

    /**
     * The office holder with this id, which must already be in lower case.
     */
    Optional<OfficeHolder> findById(String lowerCaseId);
}
