package org.jmod.dsl.json;

import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.configuration.BooleanValidator;
import org.jmod.dsl.module.configuration.FileUriValidator;
import org.jmod.dsl.module.configuration.RequiredWhen;

/**
 * Compile-time configuration for the JSON module.
 */
public class JsonConfiguration extends ExternalConfiguration {
    public boolean JSONMOD_SCHEMA_AWARE = false;
    public String JSONMOD_SCHEMA_URI = "";

    public JsonConfiguration() {
        validators.put("JSONMOD_SCHEMA_AWARE", new BooleanValidator());
        validators.put("JSONMOD_SCHEMA_URI",
                new RequiredWhen("JSONMOD_SCHEMA_AWARE", new FileUriValidator()));
    }

    @Override
    public Map<String, String> getRuntimeConfiguration() {
        Map<String, String> runtime = getModuleConfiguration();
        runtime.remove("JSONMOD_SCHEMA_URI");
        return runtime;
    }
}
