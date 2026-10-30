package uk.gov.hmcts.ctam.jo.domain;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.CREATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.START_DATE;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.UPDATED_AT;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.fullResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.nullableParentResponse;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.response;
import static uk.gov.hmcts.ctam.jo.testsupport.ReferenceDataFixtures.value;

/**
 * A reference field has three serialised forms: left out (no value object), {@code null} (a value object holding
 * null) and a number.
 */
class ReferenceFieldValueTest {

    private static final String DATES = "\"created_at\":\"2024-01-15T09:00:00Z\","
                                        + "\"updated_at\":\"2024-06-03T10:30:00Z\","
                                        + "\"start_date\":\"2024-01-01\",\"end_date\":null}";

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void leavesOutReferenceFieldsWithoutAValueObject() {
        assertThat(mapper.writeValueAsString(response(10, "Plain")))
                .isEqualTo("{\"id\":10,\"name\":\"Plain\"," + DATES);
    }

    @Test
    void writesAValueAsANumber() {
        ReferenceDataResponse response = new ReferenceDataResponse(10, "Typed", new ReferenceFieldValue(8L), null, null,
                                                                   null, null, null, null, CREATED_AT, UPDATED_AT,
                                                                   START_DATE, null);

        assertThat(mapper.writeValueAsString(response))
                .isEqualTo("{\"id\":10,\"name\":\"Typed\",\"type_id\":8," + DATES);
    }

    @Test
    void writesAValueObjectHoldingNullAsAPresentNull() {
        assertThat(mapper.writeValueAsString(nullableParentResponse(10, "Top level", 6, 84)))
                .isEqualTo("{\"id\":10,\"name\":\"Top level\",\"type_id\":6,\"parent_id\":null,\"jurisdiction_id\":84,"
                           + DATES);
    }

    @Test
    void keepsTheFieldOrder() {
        assertThat(mapper.writeValueAsString(fullResponse(10, "Full", 8, 1722, 84, true)))
                .isEqualTo("{\"id\":10,\"name\":\"Full\",\"type_id\":8,\"parent_id\":1722,\"jurisdiction_id\":84,"
                           + "\"salaried\":true," + DATES);
    }

    /**
     * No type declares both a text field and a boolean field, but the order holds for every field together.
     */
    @Test
    void keepsTheFieldOrderWithEveryField() {
        ReferenceDataResponse response = new ReferenceDataResponse(10, "Every", value(8), value(1722), value(84),
                                                                   value(31), value(48), "WIDGET_A", true, CREATED_AT,
                                                                   UPDATED_AT, START_DATE, null);

        assertThat(mapper.writeValueAsString(response))
                .isEqualTo("{\"id\":10,\"name\":\"Every\",\"type_id\":8,\"parent_id\":1722,\"jurisdiction_id\":84,"
                           + "\"ticket_category_id\":31,\"parent_category_id\":48,\"integration_code\":\"WIDGET_A\","
                           + "\"salaried\":true," + DATES);
    }
}
