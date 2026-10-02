package uk.gov.hmcts.ctam.jo.mappers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.CREATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.END_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.START_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.UPDATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.record;

class ReferenceDataMapperTest {

    private final ReferenceDataMapper mapper = Mappers.getMapper(ReferenceDataMapper.class);

    @Test
    void mapsEveryField() {
        ReferenceDataResponse response = mapper.toResponse(record(70, "Ended", END_DATE));

        assertThat(response).isEqualTo(
                new ReferenceDataResponse(70, "Ended", CREATED_AT, UPDATED_AT, START_DATE, END_DATE));
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
}
