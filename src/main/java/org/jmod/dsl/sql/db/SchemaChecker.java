package org.jmod.dsl.sql.db;

import java.util.Set;

import org.jmod.dsl.module.ModuleException;

import net.sf.jsqlparser.statement.Statement;

/**
 * Checks that every table and column in a query exists in {@link DbSchema}.
 */
public final class SchemaChecker {
    private SchemaChecker() {
    }

    public static void check(Statement statement, DbSchema schema) throws ModuleException {
        Set<String> tables = SqlIdentifiers.tables(statement);
        for (String table : tables) {
            if (!schema.hasTable(table)) {
                throw new ModuleException("table " + table + " does not exist in the database schema");
            }
        }
        for (String column : SqlIdentifiers.columns(statement)) {
            if (column.contains(".")) {
                int dot = column.lastIndexOf('.');
                String table = column.substring(0, dot);
                String name = column.substring(dot + 1);
                if (!schema.hasColumn(table, name)) {
                    throw new ModuleException("column " + column + " does not exist in the database schema");
                }
            } else if (!existsInAnyTable(schema, tables, column)) {
                throw new ModuleException("column " + column + " does not exist in the database schema");
            }
        }
    }

    private static boolean existsInAnyTable(DbSchema schema, Set<String> tables, String column) {
        for (String table : tables) {
            if (schema.hasColumn(table, column)) {
                return true;
            }
        }
        return false;
    }
}
