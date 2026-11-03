package uk.gov.hmcts.ctam.jo.repository;

import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties;
import uk.gov.hmcts.ctam.jo.entity.AppointmentRecord;
import uk.gov.hmcts.ctam.jo.entity.AuthorisationRecord;
import uk.gov.hmcts.ctam.jo.entity.JudiciaryRoleRecord;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolderRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;
import uk.gov.hmcts.ctam.jo.services.AppointmentStatus;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;
import uk.gov.hmcts.ctam.jo.util.StrictFormats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The start-up rules for any People fixture (013 FR-046, data-model.md § 5, V-2 to V-10). The first failure stops
 * start-up with a {@link FixtureRuleException} naming the rule and the office holder's id, or the record's position
 * when its id is missing or malformed. Rules about the default dataset only (FR-031, FR-035, FR-036) are not checked
 * here.
 */
public final class PeopleDatasetValidator {

    private static final Pattern WORK_PHONE = Pattern.compile("^020 7946 0[0-9]{3}$");

    private static final String RESERVED_DOMAIN = ".example";

    /**
     * The JSON kinds a fixture value may have (V-2).
     */
    enum Kind {
        STRING("a string"), TEXT("a non-blank, trimmed string"), INTEGER("an integer"), BOOLEAN("a boolean"),
        DATE("a YYYY-MM-DD date"), ARRAY("an array");

        private final String description;

        Kind(String description) {
            this.description = description;
        }
    }

    /**
     * One required key: its kind, and whether it may be null.
     */
    private record Key(Kind kind, boolean nullable) {
    }

    private static final Map<String, Key> OFFICE_HOLDER_KEYS = keys(
            "id", Kind.STRING, false,
            "last_updated", Kind.DATE, false,
            "per_id", Kind.INTEGER, false,
            "personal_code", Kind.STRING, false,
            "title", Kind.TEXT, false,
            "known_as", Kind.TEXT, false,
            "surname", Kind.TEXT, false,
            "fullname", Kind.TEXT, false,
            "initials", Kind.TEXT, false,
            "post_nominals", Kind.TEXT, true,
            "email", Kind.TEXT, false,
            "email_personal", Kind.TEXT, true,
            "gender_id", Kind.INTEGER, false,
            "show_gender", Kind.BOOLEAN, false,
            "work_phone", Kind.TEXT, true,
            "disability", Kind.BOOLEAN, false,
            "retirement_date", Kind.DATE, true,
            "leaving_on", Kind.DATE, true,
            "appointments", Kind.ARRAY, false,
            "judiciary_roles", Kind.ARRAY, false,
            "authorisations", Kind.ARRAY, false);

    private static final Map<String, Key> APPOINTMENT_KEYS = keys(
            "appointment_id", Kind.INTEGER, false,
            "role_name_id", Kind.INTEGER, false,
            "base_location_id", Kind.INTEGER, false,
            "contract_type_id", Kind.INTEGER, false,
            "is_principal", Kind.BOOLEAN, false,
            "start_date", Kind.DATE, false,
            "end_date", Kind.DATE, true,
            "circuit", Kind.TEXT, true,
            "bench", Kind.TEXT, true,
            "advisory_committee_area", Kind.TEXT, true);

    private static final Map<String, Key> JUDICIARY_ROLE_KEYS = keys(
            "judiciary_role_id", Kind.INTEGER, false,
            "judiciary_role_name_id", Kind.INTEGER, false,
            "start_date", Kind.DATE, false,
            "end_date", Kind.DATE, true);

    private static final Map<String, Key> AUTHORISATION_KEYS = keys(
            "authorisation_id", Kind.INTEGER, false,
            "appointment_id", Kind.INTEGER, false,
            "ticket_id", Kind.INTEGER, false,
            "start_date", Kind.DATE, false,
            "end_date", Kind.DATE, true);

    /**
     * The nested lists and the keys of their entries, in the fixture's key order. Ordered, so the first failure
     * reported is the same on every start (FR-046, Principle V).
     */
    private static final Map<String, Map<String, Key>> LIST_KEYS = listKeys();

    private final PeopleReferences references;

    private final Set<String> ids = new HashSet<>();

    private final Set<Long> perIds = new HashSet<>();

    private final Set<String> personalCodes = new HashSet<>();

    private final Set<Long> appointmentIds = new HashSet<>();

    private final Set<Long> authorisationIds = new HashSet<>();

    private final Set<Long> judiciaryRoleIds = new HashSet<>();

    private final AppointmentStatus appointmentStatus;

    /**
     * A validator for one fixture load: it remembers the ids it has seen, so each record is checked against the ones
     * before it (V-4).
     */
    PeopleDatasetValidator(PeopleReferences references, AppointmentStatus appointmentStatus) {
        this.references = references;
        this.appointmentStatus = appointmentStatus;
    }

    /**
     * Thrown for a broken rule; the repository adds the fixture's location.
     */
    static final class FixtureRuleException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        FixtureRuleException(String message) {
            super(message);
        }
    }

    /**
     * V-8: the configured reference types exist and declare the fields that are read from them. Checked only for a
     * non-empty fixture, since an empty one names nothing (EC-013).
     */
    static PeopleReferences resolveTypes(PeopleProperties.ReferenceTypes types, ReferenceDataTypeRegistry registry,
                                         ReferenceDataRepository repository) {
        Map<String, ReferenceDataType> byName = new HashMap<>();
        registry.types().forEach(type -> byName.put(type.getName(), type));
        PeopleReferences resolved = new PeopleReferences(repository,
                type(byName, "gender-id", types.genderId()),
                type(byName, "role-name-id", types.roleNameId()),
                type(byName, "contract-type-id", types.contractTypeId()),
                type(byName, "base-location-id", types.baseLocationId()),
                type(byName, "base-location-type-id", types.baseLocationTypeId()),
                type(byName, "base-location-parent-id", types.baseLocationParentId()),
                type(byName, "jurisdiction-id", types.jurisdictionId()),
                type(byName, "judiciary-role-name-id", types.judiciaryRoleNameId()),
                type(byName, "ticket-id", types.ticketId()),
                type(byName, "ticket-category-id", types.ticketCategoryId()));
        requireDeclares(resolved.baseLocationIdType(), "base-location-id", ReferenceField.TYPE_ID,
                        ReferenceField.PARENT_ID, ReferenceField.JURISDICTION_ID);
        requireDeclares(resolved.ticketIdType(), "ticket-id", ReferenceField.TICKET_CATEGORY_ID);
        requireDeclares(resolved.ticketCategoryIdType(), "ticket-category-id", ReferenceField.JURISDICTION_ID);
        return resolved;
    }

    private static ReferenceDataType type(Map<String, ReferenceDataType> byName, String key, String name) {
        ReferenceDataType type = byName.get(name);
        if (type == null) {
            throw new FixtureRuleException("jo.people.reference-types." + key + " '" + name
                                           + "' is not a configured reference-data type");
        }
        return type;
    }

    private static void requireDeclares(ReferenceDataType type, String key, ReferenceField... fields) {
        for (ReferenceField field : fields) {
            if (!type.getReferenceFields().contains(field)) {
                throw new FixtureRuleException("jo.people.reference-types." + key + " '" + type.getName()
                                               + "' does not declare " + field.fieldName());
            }
        }
    }

    /**
     * V-2 and the id format of V-3, on the record's JSON: every key present, of its kind, null only where allowed, and
     * no other key. Run before binding, since a bound record can't tell a missing key from a null one.
     */
    void checkShape(JsonNode record, int position) {
        if (!record.isObject()) {
            throw atPosition(position, "must be a JSON object");
        }
        JsonNode id = record.get("id");
        if (id == null || !id.isString() || !StrictFormats.isLowerCaseUuid(id.asString())) {
            throw atPosition(position, "id must be a lower-case UUID");
        }
        String owner = id.asString();
        checkKeys(record, OFFICE_HOLDER_KEYS, "", owner);
        for (Map.Entry<String, Map<String, Key>> list : LIST_KEYS.entrySet()) {
            JsonNode entries = record.get(list.getKey());
            for (int index = 0; index < entries.size(); index++) {
                String entryName = list.getKey() + "[" + index + "]";
                JsonNode entry = entries.get(index);
                if (!entry.isObject()) {
                    throw forHolder(owner, entryName + " must be a JSON object");
                }
                checkKeys(entry, list.getValue(), entryName + ".", owner);
            }
        }
    }

    private static void checkKeys(JsonNode node, Map<String, Key> keys, String prefix, String owner) {
        for (String name : node.propertyNames()) {
            if (!keys.containsKey(name)) {
                throw forHolder(owner, prefix + name + " is not a known key");
            }
        }
        keys.forEach((name, key) -> {
            JsonNode value = node.get(name);
            if (value == null || value.isNull() && !key.nullable()) {
                throw forHolder(owner, prefix + name + " is required");
            }
            if (!value.isNull() && !hasKind(value, key.kind())) {
                throw forHolder(owner, prefix + name + " must be " + key.kind().description);
            }
        });
    }

    private static boolean hasKind(JsonNode value, Kind kind) {
        return switch (kind) {
            case STRING -> value.isString();
            case TEXT -> value.isString() && !value.asString().isBlank()
                         && value.asString().equals(value.asString().strip());
            case INTEGER -> value.isIntegralNumber() && value.canConvertToLong();
            case BOOLEAN -> value.isBoolean();
            case DATE -> value.isString() && StrictFormats.strictDate(value.asString()).isPresent();
            case ARRAY -> value.isArray();
        };
    }

    /**
     * V-3 to V-7, V-9 and V-10 for one bound record whose shape {@link #checkShape} has accepted.
     */
    void checkRecord(OfficeHolderRecord record) {
        String id = record.getId();
        checkIdentity(record, id);
        checkContactDetails(record, id);
        checkAppointments(record, id);
        checkDates(record, id);
        checkReferences(record, id);
        checkAuthorisations(record, id);
    }

    private void checkIdentity(OfficeHolderRecord record, String id) {
        requirePositive(id, "per_id", record.getPerId());
        if (!StrictFormats.isDigits(record.getPersonalCode())) {
            throw forHolder(id, "personal_code must be digits only");
        }
        record.getAppointments().forEach(entry -> {
            requirePositive(id, "appointment_id", entry.getAppointmentId());
            requirePositive(id, "role_name_id", entry.getRoleNameId());
            requirePositive(id, "base_location_id", entry.getBaseLocationId());
            requirePositive(id, "contract_type_id", entry.getContractTypeId());
        });
        record.getJudiciaryRoles().forEach(entry -> {
            requirePositive(id, "judiciary_role_id", entry.getJudiciaryRoleId());
            requirePositive(id, "judiciary_role_name_id", entry.getJudiciaryRoleNameId());
        });
        record.getAuthorisations().forEach(entry -> {
            requirePositive(id, "authorisation_id", entry.getAuthorisationId());
            requirePositive(id, "appointment_id", entry.getAppointmentId());
            requirePositive(id, "ticket_id", entry.getTicketId());
        });

        requireNew(id, "id", ids.add(id));
        requireNew(id, "per_id", perIds.add(record.getPerId()));
        requireNew(id, "personal_code", personalCodes.add(record.getPersonalCode()));
        record.getAppointments().forEach(entry -> requireNew(id, "appointment_id " + entry.getAppointmentId(),
                                                             appointmentIds.add(entry.getAppointmentId())));
        record.getAuthorisations().forEach(entry -> requireNew(id, "authorisation_id " + entry.getAuthorisationId(),
                                                               authorisationIds.add(entry.getAuthorisationId())));
        record.getJudiciaryRoles().forEach(entry -> requireNew(id, "judiciary_role_id " + entry.getJudiciaryRoleId(),
                                                               judiciaryRoleIds.add(entry.getJudiciaryRoleId())));
    }

    private static void requirePositive(String id, String field, long value) {
        if (value <= 0) {
            throw forHolder(id, field + " must be greater than 0");
        }
    }

    private static void requireNew(String id, String what, boolean added) {
        if (!added) {
            throw forHolder(id, "duplicate " + what);
        }
    }

    private static void checkContactDetails(OfficeHolderRecord record, String id) {
        if (!record.getEmail().endsWith(RESERVED_DOMAIN)) {
            throw forHolder(id, "email must use the reserved .example domain");
        }
        if (record.getEmailPersonal() != null && !record.getEmailPersonal().endsWith(RESERVED_DOMAIN)) {
            throw forHolder(id, "email_personal must use the reserved .example domain");
        }
        if (record.getWorkPhone() != null && !WORK_PHONE.matcher(record.getWorkPhone()).matches()) {
            throw forHolder(id, "work_phone must be in the reserved range 020 7946 0000 to 020 7946 0999");
        }
    }

    /**
     * V-6: at least one appointment; exactly one current principal appointment when any is current.
     */
    private void checkAppointments(OfficeHolderRecord record, String id) {
        if (record.getAppointments().isEmpty()) {
            throw forHolder(id, "at least one appointment is required");
        }
        List<AppointmentRecord> current = record.getAppointments().stream()
                .filter(entry -> appointmentStatus.isCurrent(entry.getEndDate()))
                .toList();
        long principal = current.stream().filter(AppointmentRecord::getIsPrincipal).count();
        if (!current.isEmpty() && principal != 1) {
            throw forHolder(id, "exactly one current appointment must be principal");
        }
    }

    /**
     * V-7: each start date is on or before its end date, and last_updated is on or after every start and end date.
     */
    private static void checkDates(OfficeHolderRecord record, String id) {
        List<LocalDate[]> spans = new ArrayList<>();
        record.getAppointments().forEach(entry -> spans.add(span(entry.getStartDate(), entry.getEndDate())));
        record.getJudiciaryRoles().forEach(entry -> spans.add(span(entry.getStartDate(), entry.getEndDate())));
        record.getAuthorisations().forEach(entry -> spans.add(span(entry.getStartDate(), entry.getEndDate())));
        for (LocalDate[] span : spans) {
            if (span[1] != null && span[0].isAfter(span[1])) {
                throw forHolder(id, "start_date must not be after end_date");
            }
            if (Stream.of(span).anyMatch(date -> date != null && date.isAfter(record.getLastUpdated()))) {
                throw forHolder(id, "last_updated must not be before any start_date or end_date");
            }
        }
    }

    private static LocalDate[] span(LocalDate start, LocalDate end) {
        return new LocalDate[] {start, end};
    }

    /**
     * V-9: every reference resolves, including those read from the base location and from the ticket's category.
     */
    private void checkReferences(OfficeHolderRecord record, String id) {
        references.gender(record.getGenderId()).orElseThrow(() -> unresolved(id, "gender_id", record.getGenderId()));
        for (AppointmentRecord entry : record.getAppointments()) {
            references.roleName(entry.getRoleNameId())
                    .orElseThrow(() -> unresolved(id, "role_name_id", entry.getRoleNameId()));
            references.contractType(entry.getContractTypeId())
                    .orElseThrow(() -> unresolved(id, "contract_type_id", entry.getContractTypeId()));
            ReferenceDataRecord base = references.baseLocation(entry.getBaseLocationId())
                    .orElseThrow(() -> unresolved(id, "base_location_id", entry.getBaseLocationId()));
            if (base.getParentId() == null) {
                throw forHolder(id, "base_location_id " + base.getId() + " has no parent_id");
            }
            references.baseLocationType(base.getTypeId())
                    .orElseThrow(() -> unresolved(id, "type_id of base_location_id " + base.getId(), base.getTypeId()));
            references.baseLocationParent(base.getParentId()).orElseThrow(
                    () -> unresolved(id, "parent_id of base_location_id " + base.getId(), base.getParentId()));
            references.jurisdiction(base.getJurisdictionId()).orElseThrow(
                    () -> unresolved(id, "jurisdiction_id of base_location_id " + base.getId(),
                                     base.getJurisdictionId()));
        }
        for (JudiciaryRoleRecord entry : record.getJudiciaryRoles()) {
            references.judiciaryRoleName(entry.getJudiciaryRoleNameId())
                    .orElseThrow(() -> unresolved(id, "judiciary_role_name_id", entry.getJudiciaryRoleNameId()));
        }
        for (AuthorisationRecord entry : record.getAuthorisations()) {
            ReferenceDataRecord ticket = references.ticket(entry.getTicketId())
                    .orElseThrow(() -> unresolved(id, "ticket_id", entry.getTicketId()));
            ReferenceDataRecord category = references.ticketCategory(ticket.getTicketCategoryId()).orElseThrow(
                    () -> unresolved(id, "ticket_category_id of ticket_id " + ticket.getId(),
                                     ticket.getTicketCategoryId()));
            if (category.getJurisdictionId() == null) {
                throw forHolder(id, "the category of ticket_id " + ticket.getId() + " has no jurisdiction_id");
            }
            references.jurisdiction(category.getJurisdictionId()).orElseThrow(
                    () -> unresolved(id, "jurisdiction_id of the category of ticket_id " + ticket.getId(),
                                     category.getJurisdictionId()));
        }
    }

    /**
     * V-10: each authorisation is under one of the office holder's own appointments and within its dates.
     */
    private static void checkAuthorisations(OfficeHolderRecord record, String id) {
        Map<Long, AppointmentRecord> byId = new HashMap<>();
        record.getAppointments().forEach(entry -> byId.put(entry.getAppointmentId(), entry));
        for (AuthorisationRecord entry : record.getAuthorisations()) {
            AppointmentRecord parent = byId.get(entry.getAppointmentId());
            if (parent == null) {
                throw forHolder(id, "authorisation_id " + entry.getAuthorisationId()
                                    + " names an appointment_id that is not this office holder's");
            }
            if (entry.getStartDate().isBefore(parent.getStartDate())) {
                throw forHolder(id, "authorisation_id " + entry.getAuthorisationId()
                                    + " starts before its appointment");
            }
            if (parent.getEndDate() != null
                    && (entry.getEndDate() == null || entry.getEndDate().isAfter(parent.getEndDate()))) {
                throw forHolder(id, "authorisation_id " + entry.getAuthorisationId() + " ends after its appointment");
            }
        }
    }

    private static FixtureRuleException unresolved(String id, String field, Long value) {
        return forHolder(id, field + " " + value + " is not a served record");
    }

    private static FixtureRuleException forHolder(String id, String rule) {
        return new FixtureRuleException(rule + " (office holder " + id + ")");
    }

    static FixtureRuleException atPosition(int position, String rule) {
        return new FixtureRuleException(rule + " (record at position " + position + ")");
    }

    private static Map<String, Map<String, Key>> listKeys() {
        Map<String, Map<String, Key>> lists = new LinkedHashMap<>();
        lists.put("appointments", APPOINTMENT_KEYS);
        lists.put("judiciary_roles", JUDICIARY_ROLE_KEYS);
        lists.put("authorisations", AUTHORISATION_KEYS);
        return Collections.unmodifiableMap(lists);
    }

    private static Map<String, Key> keys(Object... specs) {
        Map<String, Key> keys = new LinkedHashMap<>();
        for (int index = 0; index < specs.length; index += 3) {
            keys.put((String) specs[index], new Key((Kind) specs[index + 1], (Boolean) specs[index + 2]));
        }
        return keys;
    }

    /**
     * The configured reference-data types each People reference names (research R4), and lookups in them.
     */
    record PeopleReferences(ReferenceDataRepository repository, ReferenceDataType genderIdType,
                            ReferenceDataType roleNameIdType, ReferenceDataType contractTypeIdType,
                            ReferenceDataType baseLocationIdType, ReferenceDataType baseLocationTypeIdType,
                            ReferenceDataType baseLocationParentIdType, ReferenceDataType jurisdictionIdType,
                            ReferenceDataType judiciaryRoleNameIdType, ReferenceDataType ticketIdType,
                            ReferenceDataType ticketCategoryIdType) {

        Optional<ReferenceDataRecord> gender(Long id) {
            return find(genderIdType, id);
        }

        Optional<ReferenceDataRecord> roleName(Long id) {
            return find(roleNameIdType, id);
        }

        Optional<ReferenceDataRecord> contractType(Long id) {
            return find(contractTypeIdType, id);
        }

        Optional<ReferenceDataRecord> baseLocation(Long id) {
            return find(baseLocationIdType, id);
        }

        Optional<ReferenceDataRecord> baseLocationType(Long id) {
            return find(baseLocationTypeIdType, id);
        }

        Optional<ReferenceDataRecord> baseLocationParent(Long id) {
            return find(baseLocationParentIdType, id);
        }

        Optional<ReferenceDataRecord> jurisdiction(Long id) {
            return find(jurisdictionIdType, id);
        }

        Optional<ReferenceDataRecord> judiciaryRoleName(Long id) {
            return find(judiciaryRoleNameIdType, id);
        }

        Optional<ReferenceDataRecord> ticket(Long id) {
            return find(ticketIdType, id);
        }

        Optional<ReferenceDataRecord> ticketCategory(Long id) {
            return find(ticketCategoryIdType, id);
        }

        private Optional<ReferenceDataRecord> find(ReferenceDataType type, Long id) {
            return id == null ? Optional.empty() : repository.findById(type, id);
        }
    }
}
