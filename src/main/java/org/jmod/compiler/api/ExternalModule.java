package org.jmod.compiler.api;

import org.jmod.dsl.module.Module;
import org.jmod.symbol.Type;

/**
 * Public view of a registered external module.
 */
public final class ExternalModule {
    private final Module module;

    public ExternalModule(Module m) {
        this.module = m;
    }

    public String getAuthor() {
        return module.getAuthor();
    }

    public String getDescription() {
        return module.getDescription();
    }

    public Type[] getExternalTypes() {
        return module.getExternalTypes();
    }

    public Type getConfigurationType() {
        return module.getConfigurationType();
    }

    public String getName() {
        return module.getName();
    }

    public Module unwrap() {
        return module;
    }
}
