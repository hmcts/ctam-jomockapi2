package uk.gov.hmcts.ctam.jo.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataTypeTest {

    @Test
    void nullAliasesAndReferenceFieldsMeanNone() {
        ReferenceDataType type = new ReferenceDataType("widgets", null, "classpath:x.json", null);

        assertThat(type.getAliases()).isEmpty();
        assertThat(type.getReferenceFields()).isEmpty();
    }

    @Test
    void keepsDeclaredAliasesAndReferenceFields() {
        ReferenceDataType type = new ReferenceDataType(
                "widgets", Set.of("widget"), "classpath:x.json", Set.of(ReferenceField.TYPE_ID));

        assertThat(type.getAliases()).containsExactly("widget");
        assertThat(type.getReferenceFields()).containsExactly(ReferenceField.TYPE_ID);
    }
}
