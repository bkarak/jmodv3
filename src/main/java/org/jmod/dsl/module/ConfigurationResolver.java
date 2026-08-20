package org.jmod.dsl.module;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.symbol.Type;

/**
 * Resolves the configuration type named on an {@code external} declaration.
 * Import, same-package, and built-in simple names follow the same rules for
 * every module so codegen and {@code ConfigLoader} cannot diverge.
 */
public final class ConfigurationResolver {
    private static final Map<String, Type> BUILTINS = new LinkedHashMap<>();

    static {
        BUILTINS.put("SQLConfiguration", new Type("org.jmod.dsl.sql", "SQLConfiguration"));
        BUILTINS.put("RegexConfiguration", new Type("org.jmod.dsl.regex", "RegexConfiguration"));
        BUILTINS.put("GetSetConfiguration", new Type("org.jmod.dsl.getset", "GetSetConfiguration"));
        BUILTINS.put("JsonConfiguration", new Type("org.jmod.dsl.json", "JsonConfiguration"));
        BUILTINS.put("DefaultConfiguration", new Type("org.jmod.dsl.module.def", "DefaultConfiguration"));
        BUILTINS.put("ExternalConfiguration", new Type("org.jmod.dsl.module", "ExternalConfiguration"));
    }

    private ConfigurationResolver() {
    }

    public static Type builtin(String simpleName) {
        return simpleName == null ? null : BUILTINS.get(simpleName);
    }

    /**
     * @param moduleDefault the module's built-in configuration type (used when
     *        the {@code external} omits {@code <Conf>})
     */
    public static Type resolve(CodeUnit unit, Type moduleDefault) {
        if (unit == null) {
            return moduleDefault;
        }
        String name = unit.getConfigurationTypeName();
        if (name == null || name.isBlank()) {
            return moduleDefault;
        }
        if (name.indexOf('.') >= 0) {
            return Type.parse(name);
        }
        for (String imported : unit.getImports()) {
            if (imported.endsWith("." + name) && !imported.contains("*")) {
                return Type.parse(imported);
            }
        }
        if (unit.getPackageName() != null && !unit.getPackageName().isEmpty()) {
            return new Type(unit.getPackageName(), name);
        }
        Type builtin = builtin(name);
        if (builtin != null) {
            return builtin;
        }
        if (moduleDefault != null && name.equals(moduleDefault.getName())) {
            return moduleDefault;
        }
        return moduleDefault != null ? moduleDefault : new Type("", name);
    }

    public static boolean isDeclared(CodeUnit unit) {
        if (unit == null) {
            return false;
        }
        String name = unit.getConfigurationTypeName();
        return name != null && !name.isBlank();
    }
}
