package org.jmod.dsl.sql.db;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.configuration.FileUriValidator;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

/**
 * Tables and columns loaded from a {@code CREATE TABLE} schema script.
 */
public final class DbSchema {
    private final Map<String, Set<String>> tables = new LinkedHashMap<>();

    public static DbSchema load(String uri) throws ModuleException {
        return load(uri, null);
    }

    public static DbSchema load(String uri, File baseDir) throws ModuleException {
        File file = FileUriValidator.toFile(uri, baseDir);
        if (file == null || !file.isFile()) {
            throw new ModuleException("invalid DB schema: " + uri);
        }
        try {
            String ddl = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Statements statements = CCJSqlParserUtil.parseStatements(ddl);
            DbSchema schema = new DbSchema();
            for (Statement statement : statements) {
                if (statement instanceof CreateTable createTable) {
                    schema.add(createTable);
                }
            }
            if (schema.tables.isEmpty()) {
                throw new ModuleException("DB schema contains no CREATE TABLE statements: " + uri);
            }
            return schema;
        } catch (ModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new ModuleException("could not parse DB schema: " + uri + " (" + e.getMessage() + ")", e);
        }
    }

    private void add(CreateTable createTable) {
        String table = normalize(createTable.getTable().getFullyQualifiedName());
        Set<String> columns = tables.computeIfAbsent(table, key -> new LinkedHashSet<>());
        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition column : createTable.getColumnDefinitions()) {
                columns.add(normalize(column.getColumnName()));
            }
        }
    }

    public boolean hasTable(String tableName) {
        String wanted = normalize(tableName);
        if (tables.containsKey(wanted)) {
            return true;
        }
        String simple = simpleName(wanted);
        for (String table : tables.keySet()) {
            if (simpleName(table).equals(simple)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasColumn(String tableName, String columnName) {
        String column = normalize(columnName);
        Set<String> columns = columnsOf(tableName);
        return columns != null && columns.contains(column);
    }

    private Set<String> columnsOf(String tableName) {
        String wanted = normalize(tableName);
        if (tables.containsKey(wanted)) {
            return tables.get(wanted);
        }
        String simple = simpleName(wanted);
        for (Map.Entry<String, Set<String>> entry : tables.entrySet()) {
            if (simpleName(entry.getKey()).equals(simple)) {
                return entry.getValue();
            }
        }
        return null;
    }

    static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("\"", "").replace("`", "").replace("[", "").replace("]", "")
                .trim().toLowerCase(Locale.ROOT);
    }

    private static String simpleName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot < 0 ? qualified : qualified.substring(dot + 1);
    }
}
