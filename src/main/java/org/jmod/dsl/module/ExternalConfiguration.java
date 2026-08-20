package org.jmod.dsl.module;

import java.util.Map;

/**
 * Base class for compile-time / runtime module configuration types.
 */
public abstract class ExternalConfiguration {
    protected abstract Map<String, String> getModuleConfiguration();

    public abstract Map<String, String> getRuntimeConfiguration();
}
