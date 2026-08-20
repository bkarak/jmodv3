package org.jmod.dsl.module.configuration;

/**
 * Accepts a JDBC driver class that is loadable from the current classpath.
 */
public final class JdbcDriverValidator implements Validator {
    @Override
    public boolean validate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Class.forName(value);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String validValues() {
        return "JDBC driver class name on the classpath";
    }
}
