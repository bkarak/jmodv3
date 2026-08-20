package org.jmod.dsl.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jmod.dsl.module.configuration.Validator;

/**
 * Base class for compile-time / runtime module configuration types.
 */
public abstract class ExternalConfiguration {
    protected final Map<String, Validator> validators = new LinkedHashMap<>();

    public abstract Map<String, String> getModuleConfiguration();

    public abstract Map<String, String> getRuntimeConfiguration();

    public boolean isValid(Map<String, String> conf) {
        return validationErrors(conf).isEmpty();
    }

    public List<String> validationErrors(Map<String, String> conf) {
        Map<String, String> values = conf == null ? Map.of() : conf;
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, Validator> entry : validators.entrySet()) {
            String key = entry.getKey();
            String value = values.getOrDefault(key, "");
            if (!entry.getValue().validate(value, values)) {
                errors.add(key + " value '" + value + "' is invalid; expected "
                        + entry.getValue().validValues());
            }
        }
        return errors;
    }
}
