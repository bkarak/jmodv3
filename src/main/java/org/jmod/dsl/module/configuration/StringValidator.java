package org.jmod.dsl.module.configuration;

/**
 * Accepts any non-blank string.
 */
public final class StringValidator implements Validator {
    @Override
    public boolean validate(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String validValues() {
        return "non-empty string";
    }
}
