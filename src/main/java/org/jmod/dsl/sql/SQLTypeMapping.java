package org.jmod.dsl.sql;

import org.jmod.compiler.source.ExternalRefs;
import org.jmod.dsl.module.DefaultMapping;

/**
 * Java ↔ SQL type mapping: Java primitives/wrappers plus JDBC 4.2 / {@code java.sql.Types}.
 */
public final class SQLTypeMapping extends DefaultMapping {

    public SQLTypeMapping() {
        bind(new String[] {"boolean", "java.lang.Boolean"},
                "BOOLEAN", "BIT", "BOOL");
        bind(new String[] {"byte", "java.lang.Byte"},
                "TINYINT");
        bind(new String[] {"short", "java.lang.Short"},
                "SMALLINT");
        bind(new String[] {"int", "java.lang.Integer"},
                "INTEGER", "INT", "MEDIUMINT", "YEAR", "SERIAL");
        bind(new String[] {"long", "java.lang.Long"},
                "BIGINT", "BIGSERIAL");
        bind(new String[] {"float", "java.lang.Float"},
                "REAL");
        bind(new String[] {"double", "java.lang.Double"},
                "DOUBLE", "FLOAT", "DOUBLE PRECISION");
        bind(new String[] {"char", "java.lang.Character"},
                "CHAR", "NCHAR");

        bind(new String[] {"java.lang.String"},
                "VARCHAR", "CHAR", "LONGVARCHAR", "NCHAR", "NVARCHAR", "LONGNVARCHAR",
                "CLOB", "NCLOB", "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT",
                "ENUM", "SET", "JSON", "CHARACTER", "CHARACTER VARYING");

        bind(new String[] {"java.math.BigDecimal", "java.lang.Number"},
                "NUMERIC", "DECIMAL", "DEC", "NUMBER");
        bind(new String[] {"java.math.BigInteger"},
                "NUMERIC", "DECIMAL", "BIGINT");

        bind(new String[] {"byte[]", "java.sql.Blob"},
                "VARBINARY", "BINARY", "LONGVARBINARY", "BLOB",
                "TINYBLOB", "MEDIUMBLOB", "LONGBLOB");

        bind(new String[] {"java.sql.Date", "java.time.LocalDate"},
                "DATE");
        bind(new String[] {"java.sql.Time", "java.time.LocalTime"},
                "TIME");
        bind(new String[] {"java.sql.Timestamp", "java.util.Date", "java.util.Calendar",
                        "java.time.LocalDateTime", "java.time.Instant"},
                "TIMESTAMP", "DATETIME");
        bind(new String[] {"java.time.OffsetTime"},
                "TIME_WITH_TIMEZONE", "TIME WITH TIME ZONE");
        bind(new String[] {"java.time.OffsetDateTime", "java.time.ZonedDateTime"},
                "TIMESTAMP_WITH_TIMEZONE", "TIMESTAMP WITH TIME ZONE");

        bind(new String[] {"java.sql.Clob"}, "CLOB");
        bind(new String[] {"java.sql.NClob"}, "NCLOB");
        bind(new String[] {"java.sql.Array"}, "ARRAY");
        bind(new String[] {"java.sql.Ref"}, "REF");
        bind(new String[] {"java.sql.Struct"}, "STRUCT");
        bind(new String[] {"java.sql.RowId"}, "ROWID");
        bind(new String[] {"java.sql.SQLXML"}, "SQLXML", "XML");
        bind(new String[] {"java.net.URL"}, "DATALINK");
        bind(new String[] {"java.lang.Object"}, "JAVA_OBJECT", "OTHER");
        bind(new String[] {"java.util.UUID"}, "CHAR", "VARCHAR", "OTHER");
        bind(new String[] {"java.io.InputStream"}, "LONGVARBINARY", "BLOB", "BINARY");
        bind(new String[] {"java.io.Reader"}, "LONGVARCHAR", "CLOB", "NCLOB");
    }

    public boolean acceptsJavaType(String javaType) {
        String canonical = ExternalRefs.canonicalType(javaType);
        return isKnownJavaType(canonical) || isKnownJavaType(javaType.trim().replace(" ", ""));
    }

    @Override
    public boolean isCompatible(String javaType, String dslType) {
        String java = ExternalRefs.canonicalType(javaType);
        String sql = normalizeSql(dslType);
        return super.isCompatible(java, sql)
                || super.isCompatible(java, dslType)
                || super.isCompatible(javaType, sql);
    }

    @Override
    public String javaToDSL(String javaType) {
        String canonical = ExternalRefs.canonicalType(javaType);
        String mapped = super.javaToDSL(canonical);
        return mapped != null ? mapped : super.javaToDSL(javaType);
    }

    @Override
    public String dslToJava(String dslType) {
        String mapped = super.dslToJava(normalizeSql(dslType));
        return mapped != null ? mapped : super.dslToJava(dslType);
    }

    public String setterFor(String javaType) {
        switch (ExternalRefs.canonicalType(javaType)) {
            case "boolean":
            case "java.lang.Boolean":
                return "setBoolean";
            case "byte":
            case "java.lang.Byte":
                return "setByte";
            case "short":
            case "java.lang.Short":
                return "setShort";
            case "int":
            case "java.lang.Integer":
                return "setInt";
            case "long":
            case "java.lang.Long":
                return "setLong";
            case "float":
            case "java.lang.Float":
                return "setFloat";
            case "double":
            case "java.lang.Double":
                return "setDouble";
            case "java.lang.String":
                return "setString";
            case "java.math.BigDecimal":
                return "setBigDecimal";
            case "byte[]":
                return "setBytes";
            case "java.sql.Date":
                return "setDate";
            case "java.sql.Time":
                return "setTime";
            case "java.sql.Timestamp":
                return "setTimestamp";
            case "java.sql.Blob":
                return "setBlob";
            case "java.sql.Clob":
                return "setClob";
            case "java.sql.NClob":
                return "setNClob";
            case "java.sql.Array":
                return "setArray";
            case "java.sql.Ref":
                return "setRef";
            case "java.sql.RowId":
                return "setRowId";
            case "java.sql.SQLXML":
                return "setSQLXML";
            case "java.net.URL":
                return "setURL";
            case "java.io.InputStream":
                return "setBinaryStream";
            case "java.io.Reader":
                return "setCharacterStream";
            default:
                return "setObject";
        }
    }

    public String defaultLiteral(String javaType) {
        switch (ExternalRefs.canonicalType(javaType)) {
            case "boolean":
            case "java.lang.Boolean":
                return "false";
            case "byte":
            case "java.lang.Byte":
            case "short":
            case "java.lang.Short":
            case "int":
            case "java.lang.Integer":
            case "long":
            case "java.lang.Long":
                return "1";
            case "float":
            case "java.lang.Float":
            case "double":
            case "java.lang.Double":
            case "java.math.BigDecimal":
            case "java.math.BigInteger":
            case "java.lang.Number":
                return "0.0";
            case "java.sql.Date":
            case "java.time.LocalDate":
                return "'2000-01-01'";
            case "java.sql.Time":
            case "java.time.LocalTime":
                return "'23:59:59'";
            case "java.sql.Timestamp":
            case "java.util.Date":
            case "java.util.Calendar":
            case "java.time.LocalDateTime":
            case "java.time.Instant":
            case "java.time.OffsetDateTime":
            case "java.time.ZonedDateTime":
                return "'2000-01-01 23:59:59'";
            default:
                return "'a'";
        }
    }

    /**
     * @param javas Java types; the first is dominant for each SQL type
     * @param sqls JDBC / vendor SQL type names; the first is dominant for each Java type
     */
    private void bind(String[] javas, String... sqls) {
        for (String java : javas) {
            addJavaToDSL(java, sqls[0], true);
            addJavaToDSL(java, sqls[0].toLowerCase(), false);
            for (int i = 1; i < sqls.length; i++) {
                addJavaToDSL(java, sqls[i], false);
                addJavaToDSL(java, sqls[i].toLowerCase(), false);
            }
        }
        for (String sql : sqls) {
            addDSLToJava(sql, javas[0], true);
            addDSLToJava(sql.toLowerCase(), javas[0], true);
            for (int i = 1; i < javas.length; i++) {
                addDSLToJava(sql, javas[i], false);
                addDSLToJava(sql.toLowerCase(), javas[i], false);
            }
        }
    }

    private static String normalizeSql(String dslType) {
        return dslType == null ? "" : dslType.trim().toLowerCase();
    }
}
