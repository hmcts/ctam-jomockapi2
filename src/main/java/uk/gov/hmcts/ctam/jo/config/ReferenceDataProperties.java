package uk.gov.hmcts.ctam.jo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Declared reference-data types ({@code jo.reference-data.types}). Adding a type means adding an entry here
 * and a fixture file, with no endpoint code (Principle VII).
 */
@ConfigurationProperties("jo.reference-data")
public record ReferenceDataProperties(List<TypeProperties> types) {

    public ReferenceDataProperties {
        types = types == null ? List.of() : List.copyOf(types);
    }

    public record TypeProperties(String name, List<String> aliases, String fixture) {

        public TypeProperties {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }
}
