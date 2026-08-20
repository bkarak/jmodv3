package org.jmod.dsl.module;

import java.util.Map;
import java.util.stream.Collectors;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.symbol.Type;

/**
 * Compile-time entry point for a DSL module.
 */
public abstract class Module {
    protected Module() {
    }

    public boolean isDefaultModule() {
        return false;
    }

    public static String exportContext(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    @Override
    public String toString() {
        return getName() + " - " + getDescription();
    }

    public abstract Type getConfigurationType();

    public abstract Map<String, String> getDefaultConfiguration();

    public ExternalConfiguration newConfiguration() {
        return null;
    }

    public abstract boolean evaluate(CodeUnit cu, Map<String, String> context) throws ModuleException;

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getVersion();

    public abstract String getAuthor();

    public abstract Type[] getExternalTypes();

    public abstract TypeMapping getTypeMap();
}
