package org.jmod.dsl.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.dsl.getset.GetSetModule;
import org.jmod.dsl.regex.RegexModule;
import org.jmod.dsl.sql.SQLModule;
import org.jmod.symbol.SymbolTable;
import org.jmod.symbol.Type;

/**
 * Registry of available external modules.
 */
public final class ModuleList {
    private final List<Module> modules = new ArrayList<>();

    public ModuleList() {
        modules.add(new DefaultModule());
        modules.add(new RegexModule());
        modules.add(new SQLModule());
        modules.add(new GetSetModule());
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void register(Module module) {
        modules.add(module);
    }

    public Module findFor(CodeUnit unit, SymbolTable symbols) {
        Type resolved = symbols.resolve(unit.getBaseTypeName(), unit);
        if (resolved == null) {
            return null;
        }
        for (Module module : modules) {
            if (module.isDefaultModule()) {
                continue;
            }
            for (Type exported : module.getExternalTypes()) {
                if (exported.equals(resolved) || exported.getName().equals(unit.getBaseTypeName())) {
                    return module;
                }
            }
        }
        return null;
    }
}
