package uk.gov.hmcts.ctam.jo.domain;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.mappers.PeopleMapper;
import uk.gov.hmcts.ctam.jo.testsupport.JsonTrees;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.FIRST_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.MINIMAL;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.SECOND_ID;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.repository;
import static uk.gov.hmcts.ctam.jo.testsupport.PeopleFixtures.mapper;

/**
 * The People response models write exactly the spec's fields, in the Swagger's order (013 spec § Records, EC-009).
 */
class PeopleSerialisationTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final PeopleMapper mapper = mapper();

    private JsonNode person(String id) {
        return JsonTrees.parse(MAPPER.writeValueAsString(
                mapper.toResponse(repository(MINIMAL).findById(id).orElseThrow(), true)));
    }

    @Test
    void anOfficeHolderWithEveryOptionalFieldSetWritesEveryFieldInOrder() {
        JsonNode first = person(FIRST_ID);

        assertThat(first.propertyNames()).containsExactly("id", "last_updated", "per_id", "personal_code", "title",
                "known_as", "surname", "fullname", "initials", "post_nominals", "email", "sex", "gender", "gender_id",
                "work_phone", "disability", "email_personal", "retirement_date", "leaving_on", "appointments",
                "judiciary_roles", "authorisations", "authorisations_current", "authorisations_with_dates");
        assertThat(first.get("last_updated").asString()).isEqualTo("2026-07-01");
        assertThat(first.get("leaving_on").isNull()).isTrue();
        assertThat(first.get("appointments").get(0).propertyNames()).containsExactly("appointment_id", "role_name",
                "role_name_id", "type", "court_name", "court_type", "bench", "advisory_committee_area", "location",
                "base_location", "base_location_id", "is_principal", "start_date", "end_date", "contract_type",
                "contract_type_id");
        assertThat(first.get("appointments").get(1).propertyNames()).contains("circuit").doesNotContain("bench");
        assertThat(first.get("appointments").get(1).get("end_date").isNull()).isTrue();
        assertThat(first.get("judiciary_roles").get(1).propertyNames())
                .containsExactly("name", "judiciary_role_id", "judiciary_role_name_id", "start_date", "end_date");
        assertThat(first.get("judiciary_roles").get(1).get("end_date").isNull()).isTrue();
        assertThat(first.get("authorisations").get(0).propertyNames()).containsExactly("jurisdiction", "tickets");
        assertThat(first.get("authorisations_with_dates").get(0).propertyNames()).containsExactly("authorisation_id",
                "appointment_id", "jurisdiction", "jurisdiction_id", "ticket", "ticket_id", "start_date", "end_date");
        assertThat(first.get("authorisations_with_dates").get(2).get("end_date").isNull()).isTrue();
        assertThat(first.get("appointments").get(0).get("start_date").asString()).isEqualTo("2010-01-01");
    }

    @Test
    void optionalFieldsAreLeftOutAndNullableRequiredFieldsWrittenAsNull() {
        JsonNode second = person(SECOND_ID);

        assertThat(second.propertyNames()).doesNotContain("gender", "email_personal");
        for (String nullable : List.of("post_nominals", "work_phone", "retirement_date", "leaving_on")) {
            assertThat(second.has(nullable)).as(nullable).isTrue();
            assertThat(second.get(nullable).isNull()).as(nullable).isTrue();
        }
        assertThat(second.get("appointments").get(0).propertyNames())
                .doesNotContain("circuit", "bench", "advisory_committee_area");
    }

    @Test
    void theSearchResponseHasResultsThenPagination() {
        PeopleApiResponse response = new PeopleApiResponse(List.of(), new PaginationResponse(0, 0, 1, 50, false));

        JsonNode body = JsonTrees.parse(MAPPER.writeValueAsString(response));

        assertThat(body.propertyNames()).containsExactly("results", "pagination");
        assertThat(body.get("pagination").propertyNames())
                .containsExactly("results", "pages", "current_page", "results_per_page", "more_pages");
    }
}
