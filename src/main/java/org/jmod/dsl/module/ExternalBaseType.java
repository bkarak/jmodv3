package org.jmod.dsl.module;

import java.util.Map;

/**
 * Runtime base type for all generated external types.
 */
public class ExternalBaseType<T extends ExternalConfiguration> {
    protected T configuration;

    protected ExternalBaseType(T configuration) {
        this.configuration = configuration;
    }

    public T getConfiguration() {
        return configuration;
    }

    public Map<String, String> getModuleRuntime() {
        if (configuration == null) {
            return Map.of();
        }
        return configuration.getRuntimeConfiguration();
    }
}
