package uk.gov.hmcts.ctam.jo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

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

        /**
         * The constructor configuration binds through, chosen explicitly because the record has two.
         */
        @ConstructorBinding
        public TypeProperties {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            referenceFields = referenceFields == null ? List.of() : List.copyOf(referenceFields);
        }

        /**
         * A type whose records carry no reference fields.
         */
        public TypeProperties(String name, List<String> aliases, String fixture) {
            this(name, aliases, fixture, List.of());
        }
    }
}
