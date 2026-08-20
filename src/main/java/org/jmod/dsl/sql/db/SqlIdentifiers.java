package org.jmod.dsl.sql.db;

import java.util.LinkedHashSet;
import java.util.Set;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 * Collects table and column identifiers from a parsed SQL statement.
 */
public final class SqlIdentifiers {
    private SqlIdentifiers() {
    }

    public static Set<String> tables(Statement statement) {
        Set<String> names = new LinkedHashSet<>();
        for (String table : new TablesNamesFinder<>().getTables(statement)) {
            String normalized = DbSchema.normalize(table);
            if (!normalized.isEmpty()) {
                names.add(normalized);
            }
        }
        return names;
    }

    public static Set<String> columns(Statement statement) {
        ColumnFinder finder = new ColumnFinder();
        finder.getTables(statement);
        return finder.columns;
    }

    private static final class ColumnFinder extends TablesNamesFinder<Void> {
        private final Set<String> columns = new LinkedHashSet<>();

        @Override
        public <S> Void visit(Column column, S context) {
            String name = DbSchema.normalize(column.getUnquotedColumnName());
            if ("*".equals(name) || name.isEmpty()) {
                return super.visit(column, context);
            }
            String table = column.getTable() == null ? "" : DbSchema.normalize(column.getTable().getFullyQualifiedName());
            columns.add(table.isEmpty() ? name : table + "." + name);
            return super.visit(column, context);
        }

        @Override
        public <S> Void visit(Table table, S context) {
            return super.visit(table, context);
        }
    }
}
