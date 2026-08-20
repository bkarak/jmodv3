package org.jmod.dsl.sql.db;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jmod.compiler.source.ExternalRef;
import org.jmod.compiler.source.ExternalRefs;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.sql.SQLTypeMapping;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;

/**
 * Checks that every table and column in a query exists in {@link DbSchema},
 * and that bound {@code #[name]<JavaType>} placeholders match the column SQL type.
 */
public final class SchemaChecker {
    private SchemaChecker() {
    }

    public static void check(Statement statement, DbSchema schema) throws ModuleException {
        check(statement, schema, List.of(), null);
    }

    public static void check(Statement statement, DbSchema schema, List<ExternalRef> placeholders,
            SQLTypeMapping mapping) throws ModuleException {
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
        if (mapping == null || placeholders == null || placeholders.isEmpty()) {
            return;
        }
        List<Column> bound = PlaceholderBindings.columns(statement);
        int n = Math.min(bound.size(), placeholders.size());
        for (int i = 0; i < n; i++) {
            Column column = bound.get(i);
            if (column == null) {
                continue;
            }
            checkPlaceholderType(placeholders.get(i), column, schema, tables, mapping);
        }
    }

    private static void checkPlaceholderType(ExternalRef placeholder, Column column, DbSchema schema,
            Set<String> tables, SQLTypeMapping mapping) throws ModuleException {
        Set<String> sqlTypes = sqlTypesOf(schema, tables, column);
        if (sqlTypes.isEmpty()) {
            return;
        }
        String javaType = placeholder.getType();
        for (String sqlType : sqlTypes) {
            if (!mapping.isCompatible(javaType, sqlType)) {
                throw new ModuleException("type incompatibility: #[" + placeholder.getName() + "]<"
                        + javaType + "> is not compatible with column " + displayName(column)
                        + " (" + sqlType + ")");
            }
        }
    }

    private static Set<String> sqlTypesOf(DbSchema schema, Set<String> tables, Column column) {
        Set<String> types = new LinkedHashSet<>();
        String name = DbSchema.normalize(column.getUnquotedColumnName());
        String table = column.getTable() == null
                ? ""
                : DbSchema.normalize(column.getTable().getFullyQualifiedName());
        if (!table.isEmpty()) {
            String sqlType = schema.columnType(table, name);
            if (sqlType != null && !sqlType.isEmpty()) {
                types.add(sqlType);
            }
            return types;
        }
        for (String referenced : tables) {
            String sqlType = schema.columnType(referenced, name);
            if (sqlType != null && !sqlType.isEmpty()) {
                types.add(sqlType);
            }
        }
        return types;
    }

    private static String displayName(Column column) {
        String name = column.getUnquotedColumnName();
        if (column.getTable() == null || column.getTable().getFullyQualifiedName() == null
                || column.getTable().getFullyQualifiedName().isBlank()) {
            return name;
        }
        return column.getTable().getFullyQualifiedName() + "." + name;
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
