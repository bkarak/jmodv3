package org.jmod.dsl.module.configuration;

import java.util.Map;

/**
 * Runs {@code inner} only when {@code flagKey} is {@code true} in the configuration map.
 */
public final class RequiredWhen implements Validator {
    private final String flagKey;
    private final Validator inner;

    public RequiredWhen(String flagKey, Validator inner) {
        this.flagKey = flagKey;
        this.inner = inner;
    }

    @Override
    public boolean validate(String value) {
        return inner.validate(value);
    }

    @Override
    public boolean validate(String value, Map<String, String> context) {
        if (context == null || !Boolean.parseBoolean(context.getOrDefault(flagKey, "false"))) {
            return true;
        }
        return inner.validate(value, context);
    }

    @Override
    public String validValues() {
        return inner.validValues() + " (required when " + flagKey + "=true)";
    }
}
