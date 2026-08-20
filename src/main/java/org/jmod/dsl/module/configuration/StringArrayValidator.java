package org.jmod.dsl.module.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Accepts one of a fixed set of strings.
 */
public class StringArrayValidator implements Validator {
    private final List<String> allowed;

    public StringArrayValidator(String... allowed) {
        this.allowed = Arrays.asList(allowed);
    }

    @Override
    public boolean validate(String value) {
        return allowed.contains(value);
    }

    @Override
    public String validValues() {
        return String.join(", ", allowed);
    }
}
