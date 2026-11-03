package uk.gov.hmcts.ctam.jo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * One dated authorisation: a ticket held under one of the office holder's appointments (013 spec § Records).
 *
 * @param authorisationId the authorisation's id
 * @param appointmentId   the appointment it is held under
 * @param jurisdiction    the name of the jurisdiction of the ticket's category
 * @param jurisdictionId  the id of that jurisdiction
 * @param ticket          the ticket's name
 * @param ticketId        the ticket's id
 * @param startDate       when it was granted
 * @param endDate         when it ended; null while held
 */
@Schema(description = "A ticket held under one of the office holder's appointments.")
@JsonPropertyOrder({"authorisation_id", "appointment_id", "jurisdiction", "jurisdiction_id", "ticket", "ticket_id",
    "start_date", "end_date"})
public record AuthorisationsResponse(
        @Schema(description = "The authorisation's id.", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "800044")
        @JsonProperty("authorisation_id")
        long authorisationId,

        @Schema(description = "The appointment_id of the appointment it is held under.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "700031")
        @JsonProperty("appointment_id")
        long appointmentId,

        @Schema(description = "The name of the jurisdiction of the ticket's category.",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Courts")
        @JsonProperty("jurisdiction")
        String jurisdiction,

        @Schema(description = "The id of the jurisdiction of the ticket's category.", format = "int64",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
        @JsonProperty("jurisdiction_id")
        long jurisdictionId,

        @Schema(description = "The ticket's name.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Civil Authorisation")
        @JsonProperty("ticket")
        String ticket,

        @Schema(description = "The ticket's id.", format = "int64", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "294")
        @JsonProperty("ticket_id")
        long ticketId,

        @Schema(description = "When the authorisation was granted.", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "2019-04-01")
        @JsonProperty("start_date")
        LocalDate startDate,

        @Schema(description = "When the authorisation ended. Null while it is held.", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "2025-03-31")
        @JsonProperty("end_date")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        LocalDate endDate) {
}
