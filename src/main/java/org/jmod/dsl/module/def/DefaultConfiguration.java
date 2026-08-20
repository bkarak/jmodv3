package org.jmod.dsl.module.def;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;

/**
 * Placeholder configuration used by {@code DefaultModule}.
 */
public class DefaultConfiguration extends ExternalConfiguration {
    public DefaultConfiguration() {
    }

    @Override
    protected Map<String, String> getModuleConfiguration() {
        return new LinkedHashMap<>();
    }

    @Override
    public Map<String, String> getRuntimeConfiguration() {
        return getModuleConfiguration();
    }
}
