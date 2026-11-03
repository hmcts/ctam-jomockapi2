package uk.gov.hmcts.ctam.jo.repository;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

/**
 * The strict JSON reading every checked-in fixture shares (research R6). Unknown properties fail, and each field must
 * hold its own JSON type: ids are integers (no rounding of fractions, no parsing of strings), names and text fields are
 * strings, booleans are JSON booleans (no 0/1 or "true"). Dates are checked by each repository on the JSON tree, since
 * no mapper setting stops Jackson reading an epoch number or an array as a date.
 */
public final class StrictFixtureJson {

    private StrictFixtureJson() {
    }

    /**
     * A strict mapper built from {@code base}, keeping the rest of its configuration.
     */
    public static JsonMapper mapper(JsonMapper base) {
        return base.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Boolean, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();
    }
}
