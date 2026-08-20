package org.jmod.dsl.getset;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.configuration.BooleanValidator;

/**
 * Compile-time configuration for getter/setter generation.
 */
public class GetSetConfiguration extends ExternalConfiguration {
    public String GS_PACKAGE = "";
    public boolean GS_GEN_GETTER = true;
    public boolean GS_GEN_SETTER = true;

    public GetSetConfiguration() {
        validators.put("GS_GEN_GETTER", new BooleanValidator());
        validators.put("GS_GEN_SETTER", new BooleanValidator());
    }

    @Override
    public Map<String, String> getModuleConfiguration() {
        Map<String, String> conf = new LinkedHashMap<>();
        conf.put("GS_PACKAGE", GS_PACKAGE);
        conf.put("GS_GEN_GETTER", Boolean.toString(GS_GEN_GETTER));
        conf.put("GS_GEN_SETTER", Boolean.toString(GS_GEN_SETTER));
        return conf;
    }

    @Override
    public Map<String, String> getRuntimeConfiguration() {
        return new LinkedHashMap<>();
    }
}
