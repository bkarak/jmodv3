package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SQLQueryTest {
    @Test
    void storesConfigurationAndSqlText() {
        SQLConfiguration configuration = new SQLConfiguration();
        configuration.SQLMOD_DB_LOGIN = "tester";
        RecordingQuery query = new RecordingQuery(configuration);
        query.sqlStatement = "select 1";

        assertSame(configuration, query.getConfiguration());
        assertEquals("select 1", query.getSQLStatement());
        assertEquals("tester", query.getModuleRuntime().get("SQLMOD_DB_LOGIN"));
        assertTrue(query.getModuleRuntime().containsKey("SQLMOD_JDBC_DRIVER"));
    }

    @Test
    void runtimeMapDoesNotExposePassword() {
        SQLConfiguration configuration = new SQLConfiguration();
        configuration.SQLMOD_DB_PASSWORD = "secret";
        Map<String, String> runtime = new RecordingQuery(configuration).getModuleRuntime();
        assertFalse(runtime.containsKey("SQLMOD_DB_PASSWORD"));
    }

    private static final class RecordingQuery extends SQLQuery<SQLConfiguration> {
        RecordingQuery(SQLConfiguration configuration) {
            super(configuration);
        }

        @Override
        public PreparedStatement getStatement(Connection c) throws SQLException {
            return null;
        }
    }
}
