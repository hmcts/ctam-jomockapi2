package uk.gov.hmcts.ctam.jo.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses a response body into a JSON tree for assertions. Reading a tree needs no application configuration,
 * so every suite shares this one mapper instead of building or injecting its own.
 */
public final class JsonTrees {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonTrees() {
    }

    public static JsonNode parse(String json) {
        return MAPPER.readTree(json);
    }
}
