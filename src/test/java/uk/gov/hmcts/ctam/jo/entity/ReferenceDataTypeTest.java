package uk.gov.hmcts.ctam.jo.entity;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ReferenceDataTypeTest {

    @Test
    void nullAliasesAndReferenceFieldsMeanNone() {
        ReferenceDataType type = new ReferenceDataType("widgets", null, "classpath:x.json", null, true);

        assertThat(type.getAliases()).isEmpty();
        assertThat(type.getReferenceFields()).isEmpty();
    }

    @Test
    void keepsTheCallersOrder() {
        ReferenceDataType type = new ReferenceDataType("widgets", new LinkedHashSet<>(List.of("widget", "old_widget")),
                "classpath:x.json", EnumSet.of(ReferenceField.JURISDICTION_ID, ReferenceField.TYPE_ID), false);

        assertThat(type.getAliases()).containsExactly("widget", "old_widget");
        assertThat(type.getReferenceFields()).containsExactly(ReferenceField.TYPE_ID, ReferenceField.JURISDICTION_ID);
        assertThat(type.isUniqueNames()).isFalse();
    }

    @Test
    void isNotChangedByLaterChangesToTheCallersSets() {
        Set<String> aliases = new LinkedHashSet<>(List.of("widget"));
        Set<ReferenceField> fields = EnumSet.of(ReferenceField.TYPE_ID);
        ReferenceDataType type = new ReferenceDataType("widgets", aliases, "classpath:x.json", fields, true);

        aliases.add("gadget");
        fields.add(ReferenceField.PARENT_ID);

        assertThat(type.getAliases()).containsExactly("widget");
        assertThat(type.getReferenceFields()).containsExactly(ReferenceField.TYPE_ID);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> type.getAliases().add("gadget"));
    }
}
