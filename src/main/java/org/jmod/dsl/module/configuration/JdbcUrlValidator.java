package org.jmod.dsl.module.configuration;

/**
 * Accepts a JDBC URL.
 */
public final class JdbcUrlValidator implements Validator {
    @Override
    public boolean validate(String value) {
        return value != null && value.startsWith("jdbc:");
    }

    @Override
    public String validValues() {
        return "JDBC URL starting with jdbc:";
    }
}
