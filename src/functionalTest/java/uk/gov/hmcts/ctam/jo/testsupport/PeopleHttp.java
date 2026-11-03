package uk.gov.hmcts.ctam.jo.testsupport;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ContractPaths.PEOPLE;

/**
 * Real-HTTP calls and the field lists of 013's Records section, shared by the People functional tests. Raw requests go
 * through {@link ReferenceDataHttp}, which is route-neutral.
 */
public final class PeopleHttp {

    /**
     * Every {@code CurrentOfficeHolderResponse} field, in the Swagger's order.
     */
    public static final List<String> OFFICE_HOLDER_FIELDS = List.of("id", "last_updated", "per_id", "personal_code",
            "title", "known_as", "surname", "fullname", "initials", "post_nominals", "email", "sex", "gender",
            "gender_id", "work_phone", "disability", "email_personal", "retirement_date", "leaving_on", "appointments",
            "judiciary_roles", "authorisations", "authorisations_current", "authorisations_with_dates");

    /**
     * The office-holder fields left out when unset (EC-009).
     */
    public static final List<String> OPTIONAL_OFFICE_HOLDER_FIELDS = List.of("gender", "email_personal");

    public static final List<String> APPOINTMENT_FIELDS = List.of("appointment_id", "role_name", "role_name_id",
            "type", "court_name", "court_type", "circuit", "bench", "advisory_committee_area", "location",
            "base_location", "base_location_id", "is_principal", "start_date", "end_date", "contract_type",
            "contract_type_id");

    public static final List<String> OPTIONAL_APPOINTMENT_FIELDS = List.of("circuit", "bench",
            "advisory_committee_area");

    public static final List<String> JUDICIARY_ROLE_FIELDS =
            List.of("name", "judiciary_role_id", "judiciary_role_name_id", "start_date", "end_date");

    public static final List<String> SUMMARY_FIELDS = List.of("jurisdiction", "tickets");

    public static final List<String> AUTHORISATION_FIELDS = List.of("authorisation_id", "appointment_id",
            "jurisdiction", "jurisdiction_id", "ticket", "ticket_id", "start_date", "end_date");

    public static final List<String> PAGINATION_FIELDS =
            List.of("results", "pages", "current_page", "results_per_page", "more_pages");

    private final ReferenceDataHttp http;

    public PeopleHttp(IntSupplier port) {
        this.http = new ReferenceDataHttp(port);
    }

    /**
     * An authorised GET of {@code /api/v1/people} plus {@code suffix} (a query or a path), sent exactly as given.
     */
    public ResponseEntity<String> get(String suffix) {
        return http.get(PEOPLE + suffix);
    }

    public ResponseEntity<String> getAccepting(String suffix, String accept) {
        return http.getAccepting(PEOPLE + suffix, accept);
    }

    /**
     * A request with the given Authorization header, or none when it is null.
     */
    public ResponseEntity<String> send(HttpMethod method, String suffix, String authorization) {
        return http.send(method, PEOPLE + suffix, authorization);
    }

    /**
     * A reference-data collection's records.
     */
    public JsonNode referenceData(String path) {
        ResponseEntity<String> response = http.get(path);
        assertThat(response.getStatusCode().value()).as(path).isEqualTo(200);
        return ReferenceDataHttp.json(response).get("results");
    }

    /**
     * An authorised GET that must return {@code 200}, parsed.
     */
    public JsonNode ok(String suffix) {
        ResponseEntity<String> response = get(suffix);
        assertThat(response.getStatusCode().value()).as(suffix).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        return ReferenceDataHttp.json(response);
    }

    /**
     * Every office holder a search matches, by walking every page.
     */
    public List<JsonNode> searchAll(String updatedSince, long perPage, boolean includePrevious) {
        List<JsonNode> all = new ArrayList<>();
        long page = 1;
        boolean more = true;
        while (more) {
            JsonNode body = ok("?updated_since=" + updatedSince + "&per_page=" + perPage + "&page=" + page
                               + "&include_previous_appointments=" + includePrevious);
            body.get("results").forEach(all::add);
            more = body.get("pagination").get("more_pages").asBoolean();
            page++;
        }
        return all;
    }

    /**
     * The ids of a JSON array of office holders, or of a list of them, in order.
     */
    public static List<String> idsInOrder(Iterable<JsonNode> officeHolders) {
        List<String> ids = new ArrayList<>();
        officeHolders.forEach(holder -> ids.add(holder.get("id").asString()));
        return ids;
    }
}
