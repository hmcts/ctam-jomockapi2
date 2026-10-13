package uk.gov.hmcts.ctam.jo.repository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Loads every declared type's JSON fixture once, at startup, and validates it (data-model.md §2). Any invalid
 * fixture throws {@link IllegalStateException}, so the data is never partly served.
 */
@Repository
public class FixtureReferenceDataRepository implements ReferenceDataRepository {

    private static final TypeReference<List<ReferenceDataRecord>> RECORD_LIST = new TypeReference<>() {
    };

    private final Map<String, SortedMap<Long, ReferenceDataRecord>> recordsByType;

    private final Map<String, List<ReferenceDataRecord>> sortedRecordsByType;

    public FixtureReferenceDataRepository(ReferenceDataTypeRegistry registry, ResourceLoader resourceLoader,
                                          JsonMapper jsonMapper) {
        // Ids must be JSON integers: no rounding of fractions and no parsing of strings.
        JsonMapper strictMapper = jsonMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();
        Map<String, SortedMap<Long, ReferenceDataRecord>> byType = new HashMap<>();
        Map<String, List<ReferenceDataRecord>> sorted = new HashMap<>();
        for (ReferenceDataType type : registry.types()) {
            SortedMap<Long, ReferenceDataRecord> records = load(type, resourceLoader, strictMapper);
            byType.put(type.getName(), records);
            sorted.put(type.getName(), List.copyOf(records.values()));
        }
        this.recordsByType = Map.copyOf(byType);
        this.sortedRecordsByType = Map.copyOf(sorted);
    }

    @Override
    public List<ReferenceDataRecord> findAll(ReferenceDataType type) {
        return recordsFor(sortedRecordsByType, type);
    }

    @Override
    public Optional<ReferenceDataRecord> findById(ReferenceDataType type, long id) {
        return Optional.ofNullable(recordsFor(recordsByType, type).get(id));
    }

    private static <T> T recordsFor(Map<String, T> map, ReferenceDataType type) {
        T records = map.get(type.getName());
        if (records == null) {
            throw new IllegalArgumentException("Reference-data type is not loaded: " + type.getName());
        }
        return records;
    }

    private static SortedMap<Long, ReferenceDataRecord> load(ReferenceDataType type, ResourceLoader resourceLoader,
                                                             JsonMapper mapper) {
        String fixture = type.getFixture();
        Resource resource = resourceLoader.getResource(fixture);
        if (!resource.exists()) {
            throw invalid(fixture, "resource not found");
        }
        List<ReferenceDataRecord> records;
        try (InputStream in = resource.getInputStream()) {
            records = mapper.readValue(in, RECORD_LIST);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Invalid reference-data fixture " + fixture + ": " + e.getMessage(), e);
        }
        if (records == null) {
            throw invalid(fixture, "must be a JSON array of records");
        }

        TreeMap<Long, ReferenceDataRecord> byId = new TreeMap<>();
        Set<String> names = new HashSet<>();
        for (ReferenceDataRecord record : records) {
            if (record == null) {
                throw invalid(fixture, "null record");
            }
            validate(fixture, record);
            validateReferenceFields(fixture, type, record);
            if (byId.putIfAbsent(record.getId(), record) != null) {
                throw invalid(fixture, record.getId(), "duplicate id");
            }
            if (type.isUniqueNames() && !names.add(record.getName())) {
                throw invalid(fixture, record.getId(), "duplicate name");
            }
        }
        return Collections.unmodifiableSortedMap(byId);
    }

    private static void validate(String fixture, ReferenceDataRecord record) {
        if (record.getId() == null) {
            throw invalid(fixture, "record without an id");
        }
        long id = record.getId();
        if (id <= 0) {
            throw invalid(fixture, id, "id must be greater than 0");
        }
        String name = record.getName();
        if (name == null || name.isBlank()) {
            throw invalid(fixture, id, "name is required");
        }
        if (!name.equals(name.strip())) {
            throw invalid(fixture, id, "name must be trimmed");
        }
        if (record.getCreatedAt() == null) {
            throw invalid(fixture, id, "created_at is required");
        }
        if (record.getUpdatedAt() == null) {
            throw invalid(fixture, id, "updated_at is required");
        }
        if (record.getStartDate() == null) {
            throw invalid(fixture, id, "start_date is required");
        }
        if (record.getCreatedAt().getNano() != 0 || record.getUpdatedAt().getNano() != 0) {
            throw invalid(fixture, id, "timestamps must be whole seconds");
        }
        if (record.getUpdatedAt().isBefore(record.getCreatedAt())) {
            throw invalid(fixture, id, "updated_at must not be before created_at");
        }
        if (record.getEndDate() != null && record.getEndDate().isBefore(record.getStartDate())) {
            throw invalid(fixture, id, "end_date must not be before start_date");
        }
    }

    /**
     * A field the type declares must hold a positive id in every record; a field it doesn't declare must be absent.
     */
    private static void validateReferenceFields(String fixture, ReferenceDataType type, ReferenceDataRecord record) {
        for (ReferenceField field : ReferenceField.values()) {
            Long value = field.readFrom(record);
            if (!type.getReferenceFields().contains(field)) {
                if (value != null) {
                    throw invalid(fixture, record.getId(), field.fieldName() + " is not a reference field of "
                                                           + type.getName());
                }
            } else if (value == null) {
                throw invalid(fixture, record.getId(), field.fieldName() + " is required");
            } else if (value <= 0) {
                throw invalid(fixture, record.getId(), field.fieldName() + " must be greater than 0");
            }
        }
    }

    private static IllegalStateException invalid(String fixture, long id, String rule) {
        return invalid(fixture, "record id " + id + ": " + rule);
    }

    private static IllegalStateException invalid(String fixture, String problem) {
        return new IllegalStateException("Invalid reference-data fixture " + fixture + ": " + problem);
    }
}
