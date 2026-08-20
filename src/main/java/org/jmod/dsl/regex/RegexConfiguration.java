package org.jmod.dsl.regex;

import java.util.Map;

import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.configuration.StringArrayValidator;

/**
 * Compile-time configuration for the regular-expression module (JDK {@code Pattern} only).
 */
public class RegexConfiguration extends ExternalConfiguration {
    public String REGEX_ENGINE = "jdk";
    public String REGEX_OUTPUT = "java";

    public RegexConfiguration() {
        validators.put("REGEX_ENGINE", new StringArrayValidator("jdk"));
        validators.put("REGEX_OUTPUT", new StringArrayValidator("java"));
    }

    @Override
    public Map<String, String> getRuntimeConfiguration() {
        Map<String, String> runtime = getModuleConfiguration();
        runtime.remove("REGEX_OUTPUT");
        return runtime;
    }
}
