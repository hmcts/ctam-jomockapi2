package uk.gov.hmcts.ctam.jo.mappers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataApiResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataMapperTest {

    private final ReferenceDataMapper mapper = Mappers.getMapper(ReferenceDataMapper.class);

    @Test
    void mapsEveryField() {
        ReferenceDataResponse response = mapper.toResponse(record(70, "Ended", LocalDate.parse("2025-03-31")));

        assertThat(response).isEqualTo(new ReferenceDataResponse(70, "Ended",
                Instant.parse("2024-01-15T09:00:00Z"), Instant.parse("2024-06-03T10:30:00Z"),
                LocalDate.parse("2024-01-01"), LocalDate.parse("2025-03-31")));
    }

    @Test
    void keepsANullEndDate() {
        assertThat(mapper.toResponse(record(10, "Current", null)).endDate()).isNull();
    }

    @Test
    void keepsListOrder() {
        List<ReferenceDataResponse> responses = mapper.toResponses(
                List.of(record(30, "C", null), record(10, "A", null), record(20, "B", null)));

        assertThat(responses).extracting(ReferenceDataResponse::id).containsExactly(30L, 10L, 20L);
    }

    @Test
    void serialisesWithSnakeCaseFixedOrderAndExplicitNullEndDate() {
        ReferenceDataApiResponse body = new ReferenceDataApiResponse(List.of(mapper.toResponse(record(10, "A", null))));

        assertThat(JsonMapper.builder().build().writeValueAsString(body)).isEqualTo(
                "{\"results\":[{\"id\":10,\"name\":\"A\",\"created_at\":\"2024-01-15T09:00:00Z\","
                        + "\"updated_at\":\"2024-06-03T10:30:00Z\",\"start_date\":\"2024-01-01\",\"end_date\":null}]}");
    }

    private static ReferenceDataRecord record(long id, String name, LocalDate endDate) {
        return ReferenceDataRecord.builder()
                .id(id)
                .name(name)
                .createdAt(Instant.parse("2024-01-15T09:00:00Z"))
                .updatedAt(Instant.parse("2024-06-03T10:30:00Z"))
                .startDate(LocalDate.parse("2024-01-01"))
                .endDate(endDate)
                .build();
    }
}
