package org.jmod.dsl.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jmod.dsl.module.ExternalBaseType;

/**
 * Runtime external base type for SQL queries (JDBC prepared statements).
 */
public abstract class SQLQuery<T extends SQLConfiguration> extends ExternalBaseType<T> {
    protected String sqlStatement = "";

    protected SQLQuery(T configuration) {
        super(configuration);
    }

    public String getSQLStatement() {
        return sqlStatement;
    }

    public abstract PreparedStatement getStatement(Connection c) throws SQLException;
}
