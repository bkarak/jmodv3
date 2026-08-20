package org.jmod.symbol;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleList;

/**
 * Compiler symbol table: built-in external/configuration types plus user-defined types.
 */
public final class SymbolTable {
    private final Map<String, Symbol> byQualifiedName = new LinkedHashMap<>();

    public void registerModules(ModuleList modules) {
        for (Module module : modules.getModules()) {
            for (Type type : module.getExternalTypes()) {
                add(new Symbol(type, SymbolKind.EXTERNAL, null, null));
            }
            Type configuration = module.getConfigurationType();
            if (configuration != null && !configuration.getName().isEmpty()) {
                add(new Symbol(configuration, SymbolKind.CONFIGURATION, null, null));
            }
        }
    }

    public void add(Symbol symbol) {
        byQualifiedName.put(symbol.getType().getQualifiedName(), symbol);
    }

    public void addExternal(CodeUnit unit) {
        String pkg = unit.getPackageName();
        Type type = new Type(pkg, unit.getExternalTypeName());
        add(new Symbol(type, SymbolKind.EXTERNAL, unit, unit.getSourceFile() == null
                ? null
                : unit.getSourceFile().getFile()));
    }

    public void addJavaType(Type type, SymbolKind kind, File source) {
        add(new Symbol(type, kind, null, source));
    }

    public Symbol get(String qualifiedName) {
        return byQualifiedName.get(qualifiedName);
    }

    public Symbol get(Type type) {
        return byQualifiedName.get(type.getQualifiedName());
    }

    public Type resolve(String name, CodeUnit unit) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.indexOf('.') >= 0) {
            Symbol direct = get(name);
            return direct == null ? Type.parse(name) : direct.getType();
        }
        if (unit != null) {
            for (String imported : unit.getImports()) {
                if (imported.endsWith("." + name)) {
                    Symbol symbol = get(imported);
                    return symbol == null ? Type.parse(imported) : symbol.getType();
                }
                if (imported.endsWith(".*")) {
                    String candidate = imported.substring(0, imported.length() - 1) + name;
                    Symbol symbol = get(candidate);
                    if (symbol != null) {
                        return symbol.getType();
                    }
                }
            }
            if (!unit.getPackageName().isEmpty()) {
                Symbol samePackage = get(unit.getPackageName() + "." + name);
                if (samePackage != null) {
                    return samePackage.getType();
                }
            }
        }
        List<Symbol> matches = findBySimpleName(name);
        if (matches.size() == 1) {
            return matches.get(0).getType();
        }
        return new Type("", name);
    }

    public List<Symbol> findBySimpleName(String simpleName) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol symbol : byQualifiedName.values()) {
            if (symbol.getType().getName().equals(simpleName)) {
                result.add(symbol);
            }
        }
        return result;
    }

    public Collection<Symbol> all() {
        return byQualifiedName.values();
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("----- Symbol Table Dump -----").append(System.lineSeparator());
        for (Symbol symbol : byQualifiedName.values()) {
            sb.append(symbol).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
