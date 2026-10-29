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
import uk.gov.hmcts.ctam.jo.entity.TextField;
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

    private static final List<String> DATE_FIELDS = List.of("created_at", "updated_at", "start_date", "end_date");

    private final Map<String, SortedMap<Long, ReferenceDataRecord>> recordsByType;

    private final Map<String, List<ReferenceDataRecord>> sortedRecordsByType;

    public FixtureReferenceDataRepository(ReferenceDataTypeRegistry registry, ResourceLoader resourceLoader,
                                          JsonMapper jsonMapper) {
        // Each field must hold its own JSON type: ids are integers (no rounding of fractions, no parsing of strings),
        // names and text fields are strings, booleans are JSON booleans (no 0/1 or "true"). Timestamps and dates are
        // checked separately (requireDateStrings).
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
        JsonNode tree = null;
        try (InputStream in = resource.getInputStream()) {
            byte[] json = in.readAllBytes();
            records = mapper.readValue(json, RECORD_LIST);
            if (records != null) {
                tree = mapper.readTree(json);
                requireDateStrings(fixture, tree);
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
            requireReferenceKeys(fixture, type, record.getId(), tree.get(position - 1));
            validate(fixture, record);
            validateReferenceFields(fixture, type, record);
            validateBooleanFields(fixture, type, record);
            validateTextFields(fixture, type, record);
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
        requireTrimmedText(fixture, id, "name", record.getName(), "is required");
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
     * Which reference-field keys a record may hold, read from its JSON, since a parsed record can't tell a missing key
     * from a null one (data-model.md § 2). A field the type declares must be present, and may be {@code null} only if
     * the type declares it nullable; a field it doesn't declare must be absent, not even {@code null}.
     */
    private static void requireReferenceKeys(String fixture, ReferenceDataType type, long id, JsonNode record) {
        for (ReferenceField field : ReferenceField.values()) {
            JsonNode value = record.get(field.fieldName());
            if (!type.getReferenceFields().contains(field)) {
                if (value != null) {
                    throw invalid(fixture, id, field.fieldName() + " is not a reference field of " + type.getName());
                }
            } else if (value == null || (value.isNull() && !type.getNullableReferenceFields().contains(field))) {
                throw invalid(fixture, id, field.fieldName() + " is required");
            }
        }
    }

    /**
     * Every reference-field value present must be a positive id. Whether each key may be missing or null is checked
     * on the record's JSON first ({@link #requireReferenceKeys}), so only the values are left to check here.
     */
    private static void validateReferenceFields(String fixture, ReferenceDataType type, ReferenceDataRecord record) {
        for (ReferenceField field : type.getReferenceFields()) {
            Long value = field.readFrom(record);
            if (value != null && value <= 0) {
                throw invalid(fixture, record.getId(), field.fieldName() + " must be greater than 0");
            }
        }
    }

    /**
     * A boolean field the type declares must be present in every record; one it doesn't declare must be absent. The
     * value's JSON type is checked while reading.
     */
    private static void validateBooleanFields(String fixture, ReferenceDataType type, ReferenceDataRecord record) {
        validateDeclaredFields(fixture, type, record, "boolean field", BooleanField.values(), type.getBooleanFields());
    }

    /**
     * A text field the type declares must be present in every record, as a non-empty string with no leading or
     * trailing whitespace, as for names; one it doesn't declare must be absent. The value's JSON type is checked while
     * reading.
     */
    private static void validateTextFields(String fixture, ReferenceDataType type, ReferenceDataRecord record) {
        validateDeclaredFields(fixture, type, record, "text field", TextField.values(), type.getTextFields());
        for (TextField field : type.getTextFields()) {
            requireTrimmedText(fixture, record.getId(), field.fieldName(), field.readFrom(record), "must not be blank");
        }
    }

    /**
     * The rule every text value the mock serves follows, names and text fields alike: not blank, and no leading or
     * trailing whitespace. {@code blankProblem} completes the message for a missing or blank value.
     */
    private static void requireTrimmedText(String fixture, long id, String field, String value, String blankProblem) {
        if (value == null || value.isBlank()) {
            throw invalid(fixture, id, field + " " + blankProblem);
        }
        if (!value.equals(value.strip())) {
            throw invalid(fixture, id, field + " must be trimmed");
        }
    }

    /**
     * The rule every kind of declared field read from the parsed record shares (boolean and text fields): required in
     * each record of a type that declares it, absent from every other type's records.
     */
    private static <T> void validateDeclaredFields(String fixture, ReferenceDataType type, ReferenceDataRecord record,
                                                   String kind, DeclaredField<T>[] fields,
                                                   Set<? extends DeclaredField<T>> declared) {
        for (DeclaredField<T> field : fields) {
            T value = field.readFrom(record);
            if (!declared.contains(field)) {
                if (value != null) {
                    throw invalid(fixture, record.getId(), field.fieldName() + " is not a " + kind + " of "
                                                           + type.getName());
                }
            } else if (value == null) {
                throw invalid(fixture, record.getId(), field.fieldName() + " is required");
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
