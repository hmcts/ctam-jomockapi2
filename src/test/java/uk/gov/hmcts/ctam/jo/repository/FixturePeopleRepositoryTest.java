package uk.gov.hmcts.ctam.jo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties;
import uk.gov.hmcts.ctam.jo.config.PeopleProperties.ReferenceTypes;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties.TypeProperties;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedAppointment;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedAuthorisation;
import uk.gov.hmcts.ctam.jo.entity.OfficeHolder.ResolvedJudiciaryRole;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.entity.ReferenceField;
import uk.gov.hmcts.ctam.jo.services.ReferenceDataTypeRegistry;
import uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.FIRST_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.MINIMAL;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.SECOND_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.properties;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.referenceData;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.registry;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.repository;

/**
 * Loading, name resolution, ordering and every start-up rule (013 FR-046, data-model.md § 5), against the shipped
 * reference data.
 */
class FixturePeopleRepositoryTest {

    private static final String INVALID = "classpath:people/invalid/";

    @Test
    void theMinimalFixtureLoadsInLastUpdatedThenIdOrder() {
        FixturePeopleRepository repository = repository(MINIMAL);

        assertThat(repository.findAll()).extracting(OfficeHolder::getId).containsExactly(FIRST_ID, SECOND_ID);
        assertThat(repository.findById(SECOND_ID)).get().isSameAs(repository.findAll().get(1));
        assertThat(repository.findById("33333333-3333-4333-8333-333333333333")).isEmpty();
    }

    @Test
    void everyDerivedNameIsTheReferenceRecordsName() {
        OfficeHolder first = repository(MINIMAL).findById(FIRST_ID).orElseThrow();

        assertThat(first.getSex()).isEqualTo(name("genders", 10));
        ResolvedAppointment current = first.getAppointments().get(1);
        assertThat(current.getAppointmentId()).isEqualTo(700002);
        ReferenceDataRecord base = record("base_locations", 190);
        assertThat(current.getRoleName()).isEqualTo(name("appointment_titles", 10));
        assertThat(current.getContractType()).isEqualTo(name("contract_types", 120));
        assertThat(current.getBaseLocation()).isEqualTo(base.getName());
        assertThat(current.getCourtName()).isEqualTo(base.getName());
        assertThat(current.getCourtType()).isEqualTo(name("location_types", base.getTypeId()));
        assertThat(current.getLocation()).isEqualTo(name("locations", base.getParentId()));
        assertThat(current.getType()).isEqualTo(name("jurisdictions", base.getJurisdictionId()));
        assertThat(first.getJudiciaryRoles().get(0).getName()).isEqualTo(name("judiciary_roles", 10));
        ResolvedAuthorisation tribunals = first.getAuthorisationsWithDates().get(2);
        assertThat(tribunals.getTicketId()).isEqualTo(365);
        assertThat(tribunals.getTicket()).isEqualTo(name("tickets", 365));
        assertThat(tribunals.getJurisdictionId()).isEqualTo(34);
        assertThat(tribunals.getJurisdiction()).isEqualTo("Tribunals");
    }

    @Test
    void listsAreInTheirFr025OrderWhateverTheFixtureOrder() {
        OfficeHolder first = repository(MINIMAL).findById(FIRST_ID).orElseThrow();

        assertThat(first.getAppointments()).extracting(ResolvedAppointment::getAppointmentId)
                .containsExactly(700001L, 700002L);
        assertThat(first.getJudiciaryRoles()).extracting(ResolvedJudiciaryRole::getJudiciaryRoleId)
                .containsExactly(600001L, 600002L);
        assertThat(first.getAuthorisationsWithDates()).extracting(ResolvedAuthorisation::getAuthorisationId)
                .containsExactly(800001L, 800002L, 800003L);
    }

    // quickstart.md § 5 and T048 start the service with the reference date moved to 2030-01-01.
    @Test
    void theDefaultFixtureIsValidWithTheReferenceDateMovedTo2030() {
        PeopleProperties shipped = properties(PeopleFixtures.DEFAULT);
        PeopleProperties moved = new PeopleProperties(shipped.fixture(), LocalDate.parse("2030-01-01"),
                                                      shipped.referenceTypes());

        assertThat(repository(moved, registry()).findAll()).hasSize(120);
    }

    @Test
    void anEmptyFixtureLoadsWithoutAnyReferenceType() {
        PeopleProperties empty = new PeopleProperties("classpath:people/empty.json", LocalDate.parse("2026-01-01"),
                                                      typesNaming("widgets"));

        assertThat(repository(empty, registry()).findAll()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        not-an-array.json | must be a JSON array of office holders
        missing-key.json | work_phone is required (1)
        null-required.json | surname is required (1)
        unknown-key.json | nickname is not a known key (1)
        string-per-id.json | per_id must be an integer (1)
        record-not-an-object.json | must be a JSON object (record at position 1)
        missing-id.json | id must be a lower-case UUID (record at position 1)
        entry-not-an-object.json | appointments[0] must be a JSON object (1)
        untrimmed-title.json | title must be a non-blank, trimmed string (1)
        impossible-date.json | appointments[0].start_date must be a YYYY-MM-DD date (1)
        string-boolean.json | show_gender must be a boolean (1)
        personal-email-domain.json | email_personal must use the reserved .example domain (1)
        upper-case-id.json | id must be a lower-case UUID (record at position 1)
        two-lists-broken.json | appointments[0].end_date is required (1)
        bad-uuid.json | id must be a lower-case UUID (record at position 1)
        per-id-zero.json | per_id must be greater than 0 (1)
        personal-code-letters.json | personal_code must be digits only (1)
        duplicate-id.json | duplicate id (1)
        duplicate-per-id.json | duplicate per_id (2)
        duplicate-personal-code.json | duplicate personal_code (2)
        duplicate-appointment-id.json | duplicate appointment_id 700001 (2)
        duplicate-authorisation-id.json | duplicate authorisation_id 800001 (2)
        duplicate-judiciary-role-id.json | duplicate judiciary_role_id 600001 (1)
        real-email-domain.json | email must use the reserved .example domain (1)
        phone-outside-range.json | work_phone must be in the reserved range 020 7946 0000 to 020 7946 0999 (1)
        no-appointments.json | at least one appointment is required (2)
        two-current-principals.json | exactly one current appointment must be principal (1)
        no-current-principal.json | exactly one current appointment must be principal (1)
        start-after-end.json | start_date must not be after end_date (1)
        last-updated-too-early.json | last_updated must not be before any start_date or end_date (2)
        unknown-gender.json | gender_id 99 is not a served record (1)
        unknown-role-name.json | role_name_id 99999 is not a served record (1)
        unknown-contract-type.json | contract_type_id 99999 is not a served record (1)
        unknown-base-location.json | base_location_id 99999 is not a served record (1)
        unknown-judiciary-role.json | judiciary_role_name_id 99999 is not a served record (1)
        unknown-ticket.json | ticket_id 99999 is not a served record (1)
        foreign-appointment.json | authorisation_id 800003 names an appointment_id that is not this office holder's (1)
        authorisation-before-appointment.json | authorisation_id 800003 starts before its appointment (1)
        authorisation-open-after-appointment-ends.json | authorisation_id 800001 ends after its appointment (1)
        """)
    void everyBrokenRuleStopsStartUpNamingTheRuleAndTheRecord(String file, String rule) {
        // (1) and (2) stand for the office holders of minimal.json.
        String expected = rule.replace("(1)", "(office holder " + FIRST_ID + ")")
                .replace("(2)", "(office holder " + SECOND_ID + ")");
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(INVALID + file))
                .withMessage("Invalid People fixture " + INVALID + file + ": " + expected);
    }

    @Test
    void fixtureThatIsNotJsonStopsStartUp() {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository(INVALID + "not-json.json"))
                .withMessageStartingWith("Invalid People fixture " + INVALID + "not-json.json: not valid JSON: ");
    }

    @Test
    void missingFixtureStopsStartUp() {
        assertThatIllegalStateException()
                .isThrownBy(() -> repository("classpath:people/missing.json"))
                .withMessage("Invalid People fixture classpath:people/missing.json: resource not found");
    }

    @Test
    void referenceTypeThatIsNotConfiguredStopsStartUp() {
        PeopleProperties widgets = new PeopleProperties(MINIMAL, LocalDate.parse("2026-01-01"),
                                                        typesNaming("widgets"));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(widgets, registry()))
                .withMessage("Invalid People fixture " + MINIMAL + ": jo.people.reference-types.gender-id 'widgets' "
                             + "is not a configured reference-data type");
    }

    @Test
    void baseLocationTypeThatDoesNotDeclareParentIdStopsStartUp() {
        List<TypeProperties> types = new ArrayList<>();
        for (ReferenceDataType type : registry().types()) {
            List<String> referenceFields = type.getReferenceFields().stream()
                    .map(ReferenceField::fieldName)
                    .filter(field -> !type.getName().equals("base_locations") || !field.equals("parent_id"))
                    .toList();
            types.add(new TypeProperties(type.getName(), List.of(), type.getFixture(), referenceFields, List.of(),
                                         type.isUniqueNames(), List.of(), List.of()));
        }
        ReferenceDataTypeRegistry withoutParent = new ReferenceDataTypeRegistry(
                new ReferenceDataProperties(types));

        assertThatIllegalStateException()
                .isThrownBy(() -> repository(properties(MINIMAL), withoutParent))
                .withMessage("Invalid People fixture " + MINIMAL + ": jo.people.reference-types.base-location-id "
                             + "'base_locations' does not declare parent_id");
    }

    private static ReferenceTypes typesNaming(String genderType) {
        ReferenceTypes shipped = properties(MINIMAL).referenceTypes();
        return new ReferenceTypes(genderType, shipped.roleNameId(), shipped.contractTypeId(), shipped.baseLocationId(),
                                  shipped.baseLocationTypeId(), shipped.baseLocationParentId(),
                                  shipped.jurisdictionId(), shipped.judiciaryRoleNameId(), shipped.ticketId(),
                                  shipped.ticketCategoryId());
    }

    private static ReferenceDataRecord record(String type, long id) {
        return referenceData().findById(registry().resolve(type), id).orElseThrow();
    }

    private static String name(String type, long id) {
        return record(type, id).getName();
    }
}
