package uk.gov.hmcts.ctam.jo.repository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties;
import uk.gov.hmcts.ctam.jo.entity.AppointmentRecord;
import uk.gov.hmcts.ctam.jo.entity.AuthorisationRecord;
import uk.gov.hmcts.ctam.jo.entity.JudiciaryRoleRecord;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedAppointment;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedAuthorisation;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedJudiciaryRole;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolderRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.repository.PeopleDatasetValidator.FixtureRuleException;
import uk.gov.hmcts.ctam.jo.repository.PeopleDatasetValidator.PeopleReferences;
import uk.gov.hmcts.ctam.jo.services.AppointmentStatus;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the configured People fixture once, at start-up, validates it and resolves every name it refers to (013
 * research R6, data-model.md § 3 and § 5). An invalid fixture throws {@link IllegalStateException}, so the service
 * never starts with data it hasn't checked (FR-046). The office holders are held in their FR-012 order, each list
 * inside them in its FR-025 order.
 */
@Repository
public class FixturePeopleRepository implements PeopleRepository {

    private static final Comparator<OfficeHolder> HOLDER_ORDER =
            Comparator.comparing(OfficeHolder::getLastUpdated).thenComparing(OfficeHolder::getId);

    private static final Comparator<ResolvedAppointment> APPOINTMENT_ORDER =
            Comparator.comparing(ResolvedAppointment::getStartDate)
                    .thenComparingLong(ResolvedAppointment::getAppointmentId);

    private static final Comparator<ResolvedJudiciaryRole> JUDICIARY_ROLE_ORDER =
            Comparator.comparing(ResolvedJudiciaryRole::getStartDate)
                    .thenComparingLong(ResolvedJudiciaryRole::getJudiciaryRoleId);

    private static final Comparator<ResolvedAuthorisation> AUTHORISATION_ORDER =
            Comparator.comparing(ResolvedAuthorisation::getStartDate)
                    .thenComparingLong(ResolvedAuthorisation::getAuthorisationId);

    private final List<OfficeHolder> officeHolders;

    private final Map<String, OfficeHolder> byId;

    public FixturePeopleRepository(PeopleProperties properties, ReferenceDataTypeRegistry registry,
                                   ReferenceDataRepository referenceData, AppointmentStatus appointmentStatus,
                                   ResourceLoader resourceLoader, JsonMapper jsonMapper) {
        String fixture = properties.fixture();
        List<OfficeHolder> loaded;
        try {
            loaded = load(fixture, properties, registry, referenceData, appointmentStatus, resourceLoader,
                          StrictFixtureJson.mapper(jsonMapper));
        } catch (FixtureRuleException broken) {
            // The rule's message is the whole diagnosis; its stack is only the validator's.
            throw new IllegalStateException("Invalid People fixture " + fixture + ": " + broken.getMessage());
        }
        List<OfficeHolder> ordered = new ArrayList<>(loaded);
        ordered.sort(HOLDER_ORDER);
        Map<String, OfficeHolder> index = new HashMap<>();
        ordered.forEach(holder -> index.put(holder.getId(), holder));
        this.officeHolders = List.copyOf(ordered);
        this.byId = Map.copyOf(index);
    }

    @Override
    public List<OfficeHolder> findAll() {
        return officeHolders;
    }

    @Override
    public Optional<OfficeHolder> findById(String lowerCaseId) {
        return Optional.ofNullable(byId.get(lowerCaseId));
    }

    private static List<OfficeHolder> load(String fixture, PeopleProperties properties,
                                           ReferenceDataTypeRegistry registry, ReferenceDataRepository referenceData,
                                           AppointmentStatus appointmentStatus, ResourceLoader resourceLoader,
                                           JsonMapper mapper) {
        JsonNode array = readArray(fixture, resourceLoader, mapper);
        if (array.isEmpty()) {
            // Nothing refers to reference data, so no type needs to be served (EC-013).
            return List.of();
        }
        PeopleReferences references =
                PeopleDatasetValidator.resolveTypes(properties.referenceTypes(), registry, referenceData);
        PeopleDatasetValidator validator = new PeopleDatasetValidator(references, appointmentStatus);
        List<OfficeHolder> holders = new ArrayList<>();
        for (int position = 1; position <= array.size(); position++) {
            JsonNode node = array.get(position - 1);
            validator.checkShape(node, position);
            OfficeHolderRecord record;
            try {
                record = mapper.treeToValue(node, OfficeHolderRecord.class);
            } catch (JacksonException e) {
                throw PeopleDatasetValidator.atPosition(position, e.getOriginalMessage());
            }
            validator.checkRecord(record);
            holders.add(resolve(record, references));
        }
        return holders;
    }

    /**
     * V-1: the fixture exists and is a JSON array, possibly empty.
     */
    private static JsonNode readArray(String fixture, ResourceLoader resourceLoader, JsonMapper mapper) {
        Resource resource = resourceLoader.getResource(fixture);
        if (!resource.exists()) {
            throw new FixtureRuleException("resource not found");
        }
        JsonNode array;
        try (InputStream in = resource.getInputStream()) {
            array = mapper.readTree(in);
        } catch (IOException | JacksonException e) {
            throw new FixtureRuleException("not valid JSON: " + e.getMessage());
        }
        if (array == null || !array.isArray()) {
            throw new FixtureRuleException("must be a JSON array of office holders");
        }
        return array;
    }

    /**
     * Attaches every name the record refers to (013 data-model.md § 3) and puts each list in its FR-025 order. Every
     * reference has already been checked to resolve.
     */
    private static OfficeHolder resolve(OfficeHolderRecord record, PeopleReferences references) {
        return OfficeHolder.builder()
                .id(record.getId())
                .lastUpdated(record.getLastUpdated())
                .perId(record.getPerId())
                .personalCode(record.getPersonalCode())
                .title(record.getTitle())
                .knownAs(record.getKnownAs())
                .surname(record.getSurname())
                .fullname(record.getFullname())
                .initials(record.getInitials())
                .postNominals(record.getPostNominals())
                .email(record.getEmail())
                .emailPersonal(record.getEmailPersonal())
                .sex(name(references.gender(record.getGenderId())))
                .showGender(record.getShowGender())
                .genderId(record.getGenderId())
                .workPhone(record.getWorkPhone())
                .disability(record.getDisability())
                .retirementDate(record.getRetirementDate())
                .leavingOn(record.getLeavingOn())
                .appointments(record.getAppointments().stream()
                                      .map(entry -> resolve(entry, references))
                                      .sorted(APPOINTMENT_ORDER)
                                      .toList())
                .judiciaryRoles(record.getJudiciaryRoles().stream()
                                        .map(entry -> resolve(entry, references))
                                        .sorted(JUDICIARY_ROLE_ORDER)
                                        .toList())
                .authorisationsWithDates(record.getAuthorisations().stream()
                                                 .map(entry -> resolve(entry, references))
                                                 .sorted(AUTHORISATION_ORDER)
                                                 .toList())
                .build();
    }

    private static ResolvedAppointment resolve(AppointmentRecord entry, PeopleReferences references) {
        ReferenceDataRecord base = references.baseLocation(entry.getBaseLocationId()).orElseThrow();
        return ResolvedAppointment.builder()
                .appointmentId(entry.getAppointmentId())
                .roleName(name(references.roleName(entry.getRoleNameId())))
                .roleNameId(entry.getRoleNameId())
                .type(name(references.jurisdiction(base.getJurisdictionId())))
                .courtName(base.getName())
                .courtType(name(references.baseLocationType(base.getTypeId())))
                .circuit(entry.getCircuit())
                .bench(entry.getBench())
                .advisoryCommitteeArea(entry.getAdvisoryCommitteeArea())
                .location(name(references.baseLocationParent(base.getParentId())))
                .baseLocation(base.getName())
                .baseLocationId(entry.getBaseLocationId())
                .principal(entry.getIsPrincipal())
                .startDate(entry.getStartDate())
                .endDate(entry.getEndDate())
                .contractType(name(references.contractType(entry.getContractTypeId())))
                .contractTypeId(entry.getContractTypeId())
                .build();
    }

    private static ResolvedJudiciaryRole resolve(JudiciaryRoleRecord entry, PeopleReferences references) {
        return ResolvedJudiciaryRole.builder()
                .name(name(references.judiciaryRoleName(entry.getJudiciaryRoleNameId())))
                .judiciaryRoleId(entry.getJudiciaryRoleId())
                .judiciaryRoleNameId(entry.getJudiciaryRoleNameId())
                .startDate(entry.getStartDate())
                .endDate(entry.getEndDate())
                .build();
    }

    private static ResolvedAuthorisation resolve(AuthorisationRecord entry, PeopleReferences references) {
        ReferenceDataRecord ticket = references.ticket(entry.getTicketId()).orElseThrow();
        long jurisdictionId = references.ticketCategory(ticket.getTicketCategoryId()).orElseThrow().getJurisdictionId();
        return ResolvedAuthorisation.builder()
                .authorisationId(entry.getAuthorisationId())
                .appointmentId(entry.getAppointmentId())
                .jurisdiction(name(references.jurisdiction(jurisdictionId)))
                .jurisdictionId(jurisdictionId)
                .ticket(ticket.getName())
                .ticketId(entry.getTicketId())
                .startDate(entry.getStartDate())
                .endDate(entry.getEndDate())
                .build();
    }

    private static String name(Optional<ReferenceDataRecord> resolved) {
        return resolved.orElseThrow().getName();
    }
}
