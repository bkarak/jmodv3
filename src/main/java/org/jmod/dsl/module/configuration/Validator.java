package org.jmod.dsl.module.configuration;

import java.util.Map;

/**
 * Compile-time check for a single configuration value.
 */
public interface Validator {
    boolean validate(String value);

    default boolean validate(String value, Map<String, String> context) {
        return validate(value);
    }

    String validValues();
}
