package uk.gov.hmcts.ctam.jo.repository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;
import uk.gov.hmcts.ctam.jo.entity.BooleanField;
import uk.gov.hmcts.ctam.jo.entity.DeclaredField;
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
import java.util.function.Predicate;

/**
 * Loads every declared type's JSON fixture once, at startup, and validates it (data-model.md §2). Any invalid
 * fixture throws {@link IllegalStateException}, so the data is never partly served.
 */
@Repository
public class FixtureReferenceDataRepository implements ReferenceDataRepository {

    private static final TypeReference<List<ReferenceDataRecord>> RECORD_LIST = new TypeReference<>() {
    };

    private static final List<String> DATE_FIELDS = List.of("created_at", "updated_at", "start_date", "end_date");

    private final Map<String, SortedMap<Long, ReferenceDataRecord>> recordsByType;

    private final Map<String, List<ReferenceDataRecord>> sortedRecordsByType;

    public FixtureReferenceDataRepository(ReferenceDataTypeRegistry registry, ResourceLoader resourceLoader,
                                          JsonMapper jsonMapper) {
        // Each field must hold its own JSON type: ids are integers (no rounding of fractions, no parsing of strings),
        // names are strings, booleans are JSON booleans (no 0/1 or "true"). Timestamps and dates are checked separately
        // (requireDateStrings).
        JsonMapper strictMapper = jsonMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Boolean, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
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
            byte[] json = in.readAllBytes();
            records = mapper.readValue(json, RECORD_LIST);
            if (records != null) {
                requireDateStrings(fixture, mapper.readTree(json));
            }
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Invalid reference-data fixture " + fixture + ": " + e.getMessage(), e);
        }
        if (records == null) {
            throw invalid(fixture, "must be a JSON array of records");
        }

        TreeMap<Long, ReferenceDataRecord> byId = new TreeMap<>();
        // Only types whose names must be unique track them.
        Set<String> names = type.isUniqueNames() ? new HashSet<>() : null;
        for (int position = 1; position <= records.size(); position++) {
            ReferenceDataRecord record = records.get(position - 1);
            if (record == null) {
                throw invalid(fixture, "null record");
            }
            if (record.getId() == null) {
                throw invalid(fixture, "record at position " + position + ": id is required");
            }
            validate(fixture, record);
            validateReferenceFields(fixture, type, record);
            validateBooleanFields(fixture, type, record);
            if (byId.putIfAbsent(record.getId(), record) != null) {
                throw invalid(fixture, record.getId(), "duplicate id");
            }
            if (names != null && !names.add(record.getName())) {
                throw invalid(fixture, record.getId(), "duplicate name");
            }
        }
        return Collections.unmodifiableSortedMap(byId);
    }

    private static void validate(String fixture, ReferenceDataRecord record) {
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
     * Timestamps and dates must be ISO-8601 strings; a null is left to {@link #validate}, which knows which fields
     * are required. Jackson would otherwise
     * read an epoch number as an {@code Instant} and an array such as {@code [2024, 1, 1]} as a {@code LocalDate},
     * and no mapper setting turns that off.
     */
    private static void requireDateStrings(String fixture, JsonNode records) {
        for (int position = 1; position <= records.size(); position++) {
            JsonNode record = records.get(position - 1);
            for (String field : DATE_FIELDS) {
                JsonNode value = record.get(field);
                if (value != null && !value.isString() && !value.isNull()) {
                    throw invalid(fixture, "record at position " + position + ": " + field
                                           + " must be an ISO-8601 string");
                }
            }
        }
    }

    /**
     * A field the type declares must hold a positive id in every record; a field it doesn't declare must be absent.
     */
    private static void validateReferenceFields(String fixture, ReferenceDataType type, ReferenceDataRecord record) {
        validateDeclaredFields(fixture, type, record, "reference field", ReferenceField.values(),
                               type.getReferenceFields(), value -> value > 0, "must be greater than 0");
    }

    /**
     * A boolean field the type declares must be present in every record; one it doesn't declare must be absent. The
     * value's JSON type is checked while reading.
     */
    private static void validateBooleanFields(String fixture, ReferenceDataType type, ReferenceDataRecord record) {
        validateDeclaredFields(fixture, type, record, "boolean field", BooleanField.values(),
                               type.getBooleanFields(), value -> true, null);
    }

    /**
     * The rule every kind of declared field shares: required in each record of a type that declares it, absent from
     * every other type's records. {@code valid} is any further rule on a present value, reported as {@code rule}.
     */
    private static <T> void validateDeclaredFields(String fixture, ReferenceDataType type, ReferenceDataRecord record,
                                                   String kind, DeclaredField<T>[] fields,
                                                   Set<? extends DeclaredField<T>> declared, Predicate<T> valid,
                                                   String rule) {
        for (DeclaredField<T> field : fields) {
            T value = field.readFrom(record);
            if (!declared.contains(field)) {
                if (value != null) {
                    throw invalid(fixture, record.getId(), field.fieldName() + " is not a " + kind + " of "
                                                           + type.getName());
                }
            } else if (value == null) {
                throw invalid(fixture, record.getId(), field.fieldName() + " is required");
            } else if (!valid.test(value)) {
                throw invalid(fixture, record.getId(), field.fieldName() + " " + rule);
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
