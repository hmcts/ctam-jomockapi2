package uk.gov.hmcts.ctam.jo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

/**
 * Declared reference-data types ({@code jo.reference-data.types}). Adding a type means adding an entry here
 * and a fixture file, with no endpoint code (Principle VII). {@code reference-fields} lists the reference
 * fields ({@code type_id}, {@code parent_id}, {@code jurisdiction_id}) the type's records carry; it defaults
 * to none.
 */
@ConfigurationProperties("jo.reference-data")
public record ReferenceDataProperties(List<TypeProperties> types) {

    public ReferenceDataProperties {
        types = types == null ? List.of() : List.copyOf(types);
    }

    public record TypeProperties(String name, List<String> aliases, String fixture, List<String> referenceFields) {

        public TypeProperties {
            aliases = copyOf(name, "aliases", aliases);
            referenceFields = copyOf(name, "reference-fields", referenceFields);
        }

        /**
         * An immutable copy, failing with a message naming the type and list on an empty entry, which
         * {@link List#copyOf} would reject with a bare {@link NullPointerException}.
         */
        private static List<String> copyOf(String name, String list, List<String> values) {
            if (values == null) {
                return List.of();
            }
            if (values.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Invalid reference-data type " + name + ": empty entry in " + list);
            }
            return List.copyOf(values);
        }
    }
}
