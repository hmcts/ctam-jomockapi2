package uk.gov.hmcts.ctam.jo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Declared reference-data types ({@code jo.reference-data.types}). Adding a type means adding an entry here
 * and a fixture file, with no endpoint code (Principle VII). {@code reference-fields} lists the reference
 * fields ({@code type_id}, {@code parent_id}, {@code jurisdiction_id}, {@code ticket_category_id}) the type's
 * records carry; it defaults to none. {@code boolean-fields} lists the boolean fields (e.g. {@code salaried}) the
 * type's records carry; it defaults to none. {@code unique-names} says whether record names must be unique within
 * the type; it defaults to true. {@code nullable-reference-fields} lists the declared reference fields that may be
 * null; it defaults to none. {@code text-fields} lists the text fields (e.g. {@code integration_code}) the type's
 * records carry; it defaults to none.
 */
@ConfigurationProperties("jo.reference-data")
public record ReferenceDataProperties(List<TypeProperties> types) {

    public ReferenceDataProperties {
        types = copyOf(types);
    }

    public record TypeProperties(String name, List<String> aliases, String fixture, List<String> referenceFields,
                                 List<String> booleanFields, Boolean uniqueNames,
                                 List<String> nullableReferenceFields, List<String> textFields) {

        public TypeProperties {
            aliases = copyOf(aliases);
            referenceFields = copyOf(referenceFields);
            booleanFields = copyOf(booleanFields);
            uniqueNames = uniqueNames == null || uniqueNames;
            nullableReferenceFields = copyOf(nullableReferenceFields);
            textFields = copyOf(textFields);
        }
    }

    /**
     * An unmodifiable copy that keeps empty entries, so the registry can reject them with its usual
     * {@link IllegalStateException} rather than {@link List#copyOf} failing during binding.
     */
    private static <T> List<T> copyOf(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
