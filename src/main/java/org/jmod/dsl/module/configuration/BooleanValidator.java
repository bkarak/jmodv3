package org.jmod.dsl.module.configuration;

/**
 * Accepts {@code true} or {@code false}.
 */
public final class BooleanValidator implements Validator {
    @Override
    public boolean validate(String value) {
        return "true".equals(value) || "false".equals(value);
    }

    @Override
    public String validValues() {
        return "true, false";
    }
}
