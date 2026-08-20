package org.jmod.dsl.module.configuration;

/**
 * Accepts a decimal integer.
 */
public final class IntegerValidator implements Validator {
    @Override
    public boolean validate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String validValues() {
        return "integer";
    }
}
