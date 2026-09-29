package uk.gov.hmcts.ctam.jo.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ctam.jo.config.ReferenceDataProperties;
import uk.gov.hmcts.ctam.jo.entity.ReferenceDataType;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedReferenceDataTypeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The ONLY place reference-data names and deprecated aliases are resolved (spec FR-005).
 *
 * <p>Built once from configuration. Any invalid declaration (bad name, duplicate name or alias, missing
 * fixture) throws {@link IllegalStateException}, so the application does not start.
 */
@Component
public class ReferenceDataTypeRegistry {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-z][a-z_]*$");

    private final List<ReferenceDataType> types;

    private final Map<String, ReferenceDataType> byAttributeName;

    private final List<String> canonicalAttributeNames;

    private final List<String> deprecatedAttributeNames;

    private final List<String> supportedAttributeNames;

    public ReferenceDataTypeRegistry(ReferenceDataProperties properties) {
        List<ReferenceDataType> declared = new ArrayList<>();
        Map<String, ReferenceDataType> lookup = new LinkedHashMap<>();
        List<String> canonicalNames = new ArrayList<>();
        List<String> aliases = new ArrayList<>();

        for (ReferenceDataProperties.TypeProperties typeProperties : properties.types()) {
            ReferenceDataType type = toType(typeProperties);
            declared.add(type);
            register(lookup, type.getName(), type);
            canonicalNames.add(type.getName());
            for (String alias : type.getAliases()) {
                register(lookup, alias, type);
                aliases.add(alias);
            }
        }

        List<String> supported = new ArrayList<>(canonicalNames);
        supported.addAll(aliases);
        this.types = List.copyOf(declared);
        this.byAttributeName = Collections.unmodifiableMap(lookup);
        this.canonicalAttributeNames = List.copyOf(canonicalNames);
        this.deprecatedAttributeNames = List.copyOf(aliases);
        this.supportedAttributeNames = List.copyOf(supported);
    }

    /**
     * Resolves a canonical attribute name or a deprecated alias. The match is exact and case-sensitive.
     *
     * @throws UnsupportedReferenceDataTypeException if nothing matches
     */
    public ReferenceDataType resolve(String attributeName) {
        ReferenceDataType type = attributeName == null ? null : byAttributeName.get(attributeName);
        if (type == null) {
            throw new UnsupportedReferenceDataTypeException(attributeName);
        }
        return type;
    }

    /**
     * Canonical names first, then aliases, in configuration order.
     */
    public List<String> supportedAttributeNames() {
        return supportedAttributeNames;
    }

    /**
     * The canonical attribute name of every type, in configuration order.
     */
    public List<String> canonicalAttributeNames() {
        return canonicalAttributeNames;
    }

    /**
     * Every deprecated alias, in configuration order.
     */
    public List<String> deprecatedAttributeNames() {
        return deprecatedAttributeNames;
    }

    /**
     * Every declared type, in configuration order.
     */
    public List<ReferenceDataType> types() {
        return types;
    }

    private static ReferenceDataType toType(ReferenceDataProperties.TypeProperties properties) {
        String name = properties.name();
        requireValidName(name, "type name");
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        for (String alias : properties.aliases()) {
            requireValidName(alias, "alias of " + name);
            if (alias.equals(name)) {
                throw new IllegalStateException(
                        "Invalid reference-data type " + name + ": lists its own name as an alias");
            }
            if (!aliases.add(alias)) {
                throw new IllegalStateException("Invalid reference-data type " + name + ": duplicate alias " + alias);
            }
        }
        if (properties.fixture() == null || properties.fixture().isBlank()) {
            throw new IllegalStateException("Invalid reference-data type " + name + ": fixture is required");
        }
        return new ReferenceDataType(name, Collections.unmodifiableSet(aliases), properties.fixture());
    }

    private static void requireValidName(String value, String role) {
        if (value == null || !VALID_NAME.matcher(value).matches()) {
            throw new IllegalStateException(
                    "Invalid reference-data " + role + " '" + value + "': must match " + VALID_NAME.pattern());
        }
    }

    private static void register(Map<String, ReferenceDataType> lookup, String attributeName, ReferenceDataType type) {
        ReferenceDataType existing = lookup.putIfAbsent(attributeName, type);
        if (existing != null) {
            throw new IllegalStateException("Invalid reference-data configuration: '" + attributeName
                    + "' is declared by both " + existing.getName() + " and " + type.getName());
        }
    }
}
