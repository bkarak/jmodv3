package org.jmod.dsl.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jmod.dsl.module.configuration.ConfigFields;
import org.jmod.dsl.module.configuration.Validator;

/**
 * Base class for compile-time / runtime module configuration types.
 * Subclass public fields are the policy; {@link #getRuntimeConfiguration()}
 * is what generated external instances expose after specialization.
 */
public abstract class ExternalConfiguration {
    protected final Map<String, Validator> validators = new LinkedHashMap<>();

    public Map<String, String> getModuleConfiguration() {
        return ConfigFields.read(this);
    }

    public Map<String, String> getRuntimeConfiguration() {
        return getModuleConfiguration();
    }

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
