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
    void nullAliasesAndFieldsMeanNone() {
        ReferenceDataType type =
                new ReferenceDataType("widgets", null, "classpath:x.json", null, null, true, null, null);

        assertThat(type.getAliases()).isEmpty();
        assertThat(type.getReferenceFields()).isEmpty();
        assertThat(type.getBooleanFields()).isEmpty();
        assertThat(type.getNullableReferenceFields()).isEmpty();
        assertThat(type.getTextFields()).isEmpty();
    }

    @Test
    void keepsTheBooleanFields() {
        ReferenceDataType type = new ReferenceDataType("widgets", null, "classpath:x.json", null,
                                                       EnumSet.of(BooleanField.SALARIED), true, null, null);

        assertThat(type.getBooleanFields()).containsExactly(BooleanField.SALARIED);
    }

    @Test
    void keepsTheCallersOrder() {
        ReferenceDataType type = new ReferenceDataType("widgets", new LinkedHashSet<>(List.of("widget", "old_widget")),
                "classpath:x.json", EnumSet.of(ReferenceField.JURISDICTION_ID, ReferenceField.TYPE_ID), null, false,
                null, null);

        assertThat(type.getAliases()).containsExactly("widget", "old_widget");
        assertThat(type.getReferenceFields()).containsExactly(ReferenceField.TYPE_ID, ReferenceField.JURISDICTION_ID);
        assertThat(type.isUniqueNames()).isFalse();
    }

    @Test
    void isNotChangedByLaterChangesToTheCallersSets() {
        Set<String> aliases = new LinkedHashSet<>(List.of("widget"));
        Set<ReferenceField> fields = EnumSet.of(ReferenceField.TYPE_ID);
        Set<BooleanField> booleanFields = EnumSet.noneOf(BooleanField.class);
        final ReferenceDataType type = new ReferenceDataType("widgets", aliases, "classpath:x.json", fields,
                                                             booleanFields, true, null, null);

        aliases.add("gadget");
        fields.add(ReferenceField.PARENT_ID);
        booleanFields.add(BooleanField.SALARIED);

        assertThat(type.getAliases()).containsExactly("widget");
        assertThat(type.getReferenceFields()).containsExactly(ReferenceField.TYPE_ID);
        assertThat(type.getBooleanFields()).isEmpty();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> type.getAliases().add("gadget"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> type.getBooleanFields().add(BooleanField.SALARIED));
    }

    @Test
    void copiesTheNullableReferenceFields() {
        Set<ReferenceField> fields = EnumSet.allOf(ReferenceField.class);
        Set<ReferenceField> nullable = EnumSet.of(ReferenceField.PARENT_ID);
        final ReferenceDataType type = new ReferenceDataType("widgets", null, "classpath:x.json", fields, null, false,
                                                             nullable, null);

        nullable.add(ReferenceField.TYPE_ID);

        assertThat(type.getNullableReferenceFields()).containsExactly(ReferenceField.PARENT_ID);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> type.getNullableReferenceFields().add(ReferenceField.JURISDICTION_ID));
    }

    @Test
    void copiesTheTextFields() {
        Set<TextField> textFields = EnumSet.of(TextField.INTEGRATION_CODE);
        final ReferenceDataType type = new ReferenceDataType("widgets", null, "classpath:x.json", null, null, true,
                                                             null, textFields);

        textFields.clear();

        assertThat(type.getTextFields()).containsExactly(TextField.INTEGRATION_CODE);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> type.getTextFields().remove(TextField.INTEGRATION_CODE));
    }
}
