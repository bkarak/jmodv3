package org.jmod.dsl.sql;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code (#EXPAND:name)} IN-list markers to {@code (?,?,?)} using
 * the runtime length of a Java array. JDBC has no portable {@code IN ?} binding.
 */
public final class SqlIn {
    private static final Pattern MARKER = Pattern.compile("\\(#EXPAND:([A-Za-z_][A-Za-z0-9_]*)\\)");

    private SqlIn() {
    }

    public static String expand(String sql, Object... namesAndArrays) throws SQLException {
        if (sql == null || !sql.contains("#EXPAND:")) {
            return sql;
        }
        Map<String, Integer> lengths = lengths(namesAndArrays);
        Matcher matcher = MARKER.matcher(sql);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            Integer length = lengths.get(name);
            if (length == null) {
                throw new SQLException("IN list '" + name + "' was not provided");
            }
            if (length <= 0) {
                throw new SQLException("IN list '" + name + "' is empty");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement("(" + placeholders(length) + ")"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Map<String, Integer> lengths(Object[] namesAndArrays) throws SQLException {
        Map<String, Integer> lengths = new LinkedHashMap<>();
        if (namesAndArrays == null) {
            return lengths;
        }
        if (namesAndArrays.length % 2 != 0) {
            throw new SQLException("SqlIn.expand expects name/array pairs");
        }
        for (int i = 0; i < namesAndArrays.length; i += 2) {
            String name = String.valueOf(namesAndArrays[i]);
            lengths.put(name, arrayLength(name, namesAndArrays[i + 1]));
        }
        return lengths;
    }

    static int arrayLength(String name, Object array) throws SQLException {
        if (array == null) {
            throw new SQLException("IN list '" + name + "' is empty");
        }
        if (!array.getClass().isArray()) {
            throw new SQLException("IN list '" + name + "' is not an array");
        }
        return java.lang.reflect.Array.getLength(array);
    }

    private static String placeholders(int length) {
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
        return sql.toString();
    }
}
