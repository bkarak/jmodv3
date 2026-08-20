package org.jmod.dsl.regex;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;

/**
 * Compile-time configuration for the regular-expression module.
 */
public class RegexConfiguration extends ExternalConfiguration {
    protected String REGEX_ENGINE = "jdk";
    protected String REGEX_OUTPUT = "java";

    public RegexConfiguration() {
    }

    @Override
    protected Map<String, String> getModuleConfiguration() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("REGEX_ENGINE", REGEX_ENGINE);
        result.put("REGEX_OUTPUT", REGEX_OUTPUT);
        return result;
    }

    @Override
    public Map<String, String> getRuntimeConfiguration() {
        Map<String, String> runtime = getModuleConfiguration();
        runtime.remove("REGEX_OUTPUT");
        return runtime;
    }
}
