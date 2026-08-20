package org.jmod.dsl.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import org.jmod.dsl.module.ModuleException;

/**
 * Executes a default-literal SQL statement against a live JDBC database.
 */
public final class LiveJdbc {
    private LiveJdbc() {
    }

    public static void execute(String sql, Map<String, String> context) throws ModuleException {
        String url = value(context, "SQLMOD_DB_URL");
        String driver = value(context, "SQLMOD_JDBC_DRIVER");
        String login = value(context, "SQLMOD_DB_LOGIN");
        String password = context == null ? "" : context.getOrDefault("SQLMOD_DB_PASSWORD", "");
        if (url.isEmpty() || driver.isEmpty() || login.isEmpty()) {
            throw new ModuleException("live testing is enabled, but it is not properly configured");
        }
        try {
            Class.forName(driver);
            try (Connection connection = DriverManager.getConnection(url, login, password);
                    Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        } catch (Exception e) {
            throw new ModuleException("live DB test failed: " + e.getMessage(), e);
        }
    }

    private static String value(Map<String, String> context, String key) {
        if (context == null) {
            return "";
        }
        String value = context.get(key);
        return value == null ? "" : value;
    }
}
