package uk.gov.hmcts.ctam.jo.mappers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.ctam.jo.domain.ReferenceDataResponse;
import uk.gov.hmcts.ctam.jo.domain.ReferenceFieldValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.CREATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.END_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.START_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.UPDATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.nullableParentResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.nullableParentType;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.plainType;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.record;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.recordBuilder;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.referencingResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.referencingType;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.salariedResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.salariedType;

class ReferenceDataMapperTest {

    private final ReferenceDataMapper mapper = Mappers.getMapper(ReferenceDataMapper.class);

    @Test
    void mapsEveryField() {
        ReferenceDataResponse response = mapper.toResponse(record(70, "Ended", END_DATE), plainType());

        assertThat(response).isEqualTo(
                new ReferenceDataResponse(70, "Ended", null, null, null, null, CREATED_AT, UPDATED_AT, START_DATE,
                                          END_DATE));
    }

    @Test
    void mapsTheReferenceFields() {
        ReferenceDataResponse response = mapper.toResponse(
                recordBuilder(10, "Referencing").typeId(8L).parentId(1722L).jurisdictionId(84L).build(),
                referencingType());

        assertThat(response).isEqualTo(referencingResponse(10, "Referencing", 8, 1722, 84));
    }

    @Test
    void mapsANullValueOfANullableReferenceFieldToAPresentNull() {
        ReferenceDataResponse response = mapper.toResponse(
                recordBuilder(10, "Top level").typeId(6L).jurisdictionId(84L).build(), nullableParentType());

        assertThat(response).isEqualTo(nullableParentResponse(10, "Top level", 6, 84));
        assertThat(response.parentId()).isEqualTo(new ReferenceFieldValue(null));
    }

    @Test
    void mapsAValueOfANullableReferenceFieldLikeAnyOther() {
        ReferenceDataResponse response = mapper.toResponse(
                recordBuilder(10, "Child").typeId(8L).parentId(1722L).jurisdictionId(84L).build(),
                nullableParentType());

        assertThat(response).isEqualTo(referencingResponse(10, "Child", 8, 1722, 84));
    }

    @Test
    void leavesOutTheReferenceFieldsTheTypeDoesNotDeclare() {
        ReferenceDataResponse response = mapper.toResponse(record(10, "Plain"), plainType());

        assertThat(response.typeId()).isNull();
        assertThat(response.parentId()).isNull();
        assertThat(response.jurisdictionId()).isNull();
    }

    @Test
    void mapsTheBooleanFields() {
        assertThat(mapper.toResponse(recordBuilder(10, "Paid").salaried(true).build(), salariedType()))
                .isEqualTo(salariedResponse(10, "Paid", true));
        assertThat(mapper.toResponse(recordBuilder(20, "Unpaid").salaried(false).build(), salariedType()))
                .isEqualTo(salariedResponse(20, "Unpaid", false));
        assertThat(mapper.toResponse(record(30, "Plain"), plainType()).salaried()).isNull();
    }

    @Test
    void keepsANullEndDate() {
        assertThat(mapper.toResponse(record(10, "Current", null), plainType()).endDate()).isNull();
    }

    @Test
    void keepsListOrder() {
        List<ReferenceDataResponse> responses = mapper.toResponses(
                List.of(record(30, "C", null), record(10, "A", null), record(20, "B", null)), plainType());

        assertThat(responses).extracting(ReferenceDataResponse::id).containsExactly(30L, 10L, 20L);
    }

    @Test
    void keepsListOrderWithTheTypesReferenceFields() {
        List<ReferenceDataResponse> responses = mapper.toResponses(
                List.of(recordBuilder(30, "C").typeId(8L).parentId(10L).jurisdictionId(84L).build(),
                        recordBuilder(10, "A").typeId(6L).jurisdictionId(84L).build()),
                nullableParentType());

        assertThat(responses).containsExactly(referencingResponse(30, "C", 8, 10, 84),
                                              nullableParentResponse(10, "A", 6, 84));
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toResponse(null, plainType())).isNull();
        assertThat(mapper.toResponses(null, plainType())).isNull();
    }
}
