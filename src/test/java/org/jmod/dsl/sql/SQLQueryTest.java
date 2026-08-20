package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SQLQueryTest {
    @Test
    void storesConfigurationAndSqlText() {
        SQLConfiguration configuration = new SQLConfiguration();
        configuration.SQLMOD_NS_AWARE = true;
        RecordingQuery query = new RecordingQuery(configuration);
        query.sqlStatement = "select 1";

        assertSame(configuration, query.getConfiguration());
        assertEquals("select 1", query.getSQLStatement());
        assertEquals("true", query.getModuleRuntime().get("SQLMOD_NS_AWARE"));
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
