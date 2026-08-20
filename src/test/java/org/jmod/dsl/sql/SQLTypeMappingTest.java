package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SQLTypeMappingTest {
    private SQLTypeMapping mapping;

    @BeforeEach
    void createMapping() {
        mapping = new SQLTypeMapping();
    }

    @ParameterizedTest(name = "{0} is accepted")
    @ValueSource(strings = {
            "boolean", "Boolean", "java.lang.Boolean",
            "byte", "Byte", "java.lang.Byte",
            "short", "Short", "java.lang.Short",
            "int", "Integer", "java.lang.Integer",
            "long", "Long", "java.lang.Long",
            "float", "Float", "java.lang.Float",
            "double", "Double", "java.lang.Double",
            "char", "Character", "java.lang.Character",
            "String", "java.lang.String",
            "Object", "java.lang.Object",
            "Number", "java.lang.Number",
            "BigDecimal", "java.math.BigDecimal",
            "BigInteger", "java.math.BigInteger",
            "byte[]",
            "int[]",
            "Integer[]",
            "String[]",
            "long[]",
            "Date", "java.util.Date", "java.sql.Date",
            "Time", "java.sql.Time",
            "Timestamp", "java.sql.Timestamp", "java.util.Timestamp",
            "Calendar", "java.util.Calendar",
            "UUID", "java.util.UUID",
            "Blob", "java.sql.Blob",
            "Clob", "java.sql.Clob",
            "NClob", "java.sql.NClob",
            "Array", "java.sql.Array",
            "Ref", "java.sql.Ref",
            "Struct", "java.sql.Struct",
            "RowId", "java.sql.RowId",
            "SQLXML", "java.sql.SQLXML",
            "URL", "java.net.URL",
            "InputStream", "java.io.InputStream",
            "Reader", "java.io.Reader",
            "LocalDate", "java.time.LocalDate",
            "LocalTime", "java.time.LocalTime",
            "LocalDateTime", "java.time.LocalDateTime",
            "OffsetTime", "java.time.OffsetTime",
            "OffsetDateTime", "java.time.OffsetDateTime",
            "Instant", "java.time.Instant",
            "ZonedDateTime", "java.time.ZonedDateTime"
    })
    void acceptsStandardJavaAndJdbcTypes(String javaType) {
        assertTrue(mapping.acceptsJavaType(javaType), javaType);
    }

    @ParameterizedTest(name = "{0} is rejected")
    @ValueSource(strings = {
            "java.net.URI",
            "java.util.List",
            "java.util.Optional",
            "java.net.URI[]",
            "void",
            "Void",
            "java.lang.StringBuilder",
            "java.sql.Connection",
            "java.sql.ResultSet",
            "Map",
            "unknown.Type"
    })
    void rejectsNonJdbcJavaTypes(String javaType) {
        assertFalse(mapping.acceptsJavaType(javaType), javaType);
    }

    @Test
    void acceptsJavaTypeIgnoresSurroundingWhitespace() {
        assertTrue(mapping.acceptsJavaType("  int  "));
        assertTrue(mapping.acceptsJavaType(" java.lang.String "));
        assertTrue(mapping.acceptsJavaType("byte []"));
        assertTrue(mapping.acceptsJavaType("String []"));
        assertTrue(mapping.acceptsJavaType("int []"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "boolean, setBoolean",
            "java.lang.Boolean, setBoolean",
            "Boolean, setBoolean",
            "byte, setByte",
            "java.lang.Byte, setByte",
            "short, setShort",
            "java.lang.Short, setShort",
            "int, setInt",
            "Integer, setInt",
            "java.lang.Integer, setInt",
            "long, setLong",
            "java.lang.Long, setLong",
            "float, setFloat",
            "java.lang.Float, setFloat",
            "double, setDouble",
            "java.lang.Double, setDouble",
            "String, setString",
            "java.lang.String, setString",
            "java.math.BigDecimal, setBigDecimal",
            "BigDecimal, setBigDecimal",
            "byte[], setBytes",
            "java.sql.Date, setDate",
            "java.sql.Time, setTime",
            "Time, setTime",
            "java.sql.Timestamp, setTimestamp",
            "Timestamp, setTimestamp",
            "java.sql.Blob, setBlob",
            "Blob, setBlob",
            "java.sql.Clob, setClob",
            "Clob, setClob",
            "java.sql.NClob, setNClob",
            "NClob, setNClob",
            "java.sql.Array, setArray",
            "Array, setArray",
            "java.sql.Ref, setRef",
            "Ref, setRef",
            "java.sql.RowId, setRowId",
            "RowId, setRowId",
            "java.sql.SQLXML, setSQLXML",
            "SQLXML, setSQLXML",
            "java.net.URL, setURL",
            "URL, setURL",
            "java.io.InputStream, setBinaryStream",
            "InputStream, setBinaryStream",
            "java.io.Reader, setCharacterStream",
            "Reader, setCharacterStream"
    })
    void usesDedicatedJdbcSetter(String javaType, String setter) {
        assertEquals(setter, mapping.setterFor(javaType));
    }

    @ParameterizedTest(name = "{0} → setObject")
    @ValueSource(strings = {
            "char", "Character", "java.lang.Character",
            "Object", "java.lang.Object",
            "Number", "java.lang.Number",
            "BigInteger", "java.math.BigInteger",
            "Date", "java.util.Date",
            "Calendar", "java.util.Calendar",
            "UUID", "java.util.UUID",
            "java.sql.Struct", "Struct",
            "java.time.LocalDate", "LocalDate",
            "java.time.LocalTime", "LocalTime",
            "java.time.LocalDateTime", "LocalDateTime",
            "java.time.OffsetTime", "OffsetTime",
            "java.time.OffsetDateTime", "OffsetDateTime",
            "java.time.Instant", "Instant",
            "java.time.ZonedDateTime", "ZonedDateTime"
    })
    void usesSetObjectWhenNoDedicatedSetterExists(String javaType) {
        assertEquals("setObject", mapping.setterFor(javaType));
    }

    @Test
    void unknownJavaTypeStillGetsSetObject() {
        assertEquals("setObject", mapping.setterFor("com.example.Unknown"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "boolean, BOOLEAN",
            "java.lang.Boolean, BOOLEAN",
            "byte, TINYINT",
            "short, SMALLINT",
            "int, INTEGER",
            "java.lang.Integer, INTEGER",
            "long, BIGINT",
            "float, REAL",
            "double, DOUBLE",
            "char, CHAR",
            "java.lang.Character, CHAR",
            "String, VARCHAR",
            "java.lang.String, VARCHAR",
            "java.math.BigDecimal, NUMERIC",
            "java.lang.Number, NUMERIC",
            "java.math.BigInteger, NUMERIC",
            "byte[], VARBINARY",
            "java.sql.Blob, VARBINARY",
            "java.sql.Date, DATE",
            "java.time.LocalDate, DATE",
            "java.sql.Time, TIME",
            "java.time.LocalTime, TIME",
            "java.sql.Timestamp, TIMESTAMP",
            "java.util.Date, TIMESTAMP",
            "java.util.Calendar, TIMESTAMP",
            "java.time.LocalDateTime, TIMESTAMP",
            "java.time.Instant, TIMESTAMP",
            "java.time.OffsetTime, TIME_WITH_TIMEZONE",
            "java.time.OffsetDateTime, TIMESTAMP_WITH_TIMEZONE",
            "java.time.ZonedDateTime, TIMESTAMP_WITH_TIMEZONE",
            "java.sql.Clob, CLOB",
            "java.sql.NClob, NCLOB",
            "java.sql.Array, ARRAY",
            "java.sql.Ref, REF",
            "java.sql.Struct, STRUCT",
            "java.sql.RowId, ROWID",
            "java.sql.SQLXML, SQLXML",
            "java.net.URL, DATALINK",
            "java.lang.Object, JAVA_OBJECT",
            "java.util.UUID, CHAR",
            "java.io.InputStream, LONGVARBINARY",
            "java.io.Reader, LONGVARCHAR"
    })
    void mapsJavaTypeToDominantSqlType(String javaType, String sqlType) {
        assertEquals(sqlType, mapping.javaToDSL(javaType));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "BOOLEAN, boolean",
            "BIT, boolean",
            "TINYINT, byte",
            "SMALLINT, short",
            "INTEGER, int",
            "INT, int",
            "MEDIUMINT, int",
            "YEAR, int",
            "SERIAL, int",
            "BIGSERIAL, long",
            "REAL, float",
            "DOUBLE, double",
            "FLOAT, double",
            "DATE, java.sql.Date",
            "TIME, java.sql.Time",
            "TIMESTAMP, java.sql.Timestamp",
            "DATETIME, java.sql.Timestamp",
            "ARRAY, java.sql.Array",
            "REF, java.sql.Ref",
            "STRUCT, java.sql.Struct",
            "ROWID, java.sql.RowId",
            "SQLXML, java.sql.SQLXML",
            "XML, java.sql.SQLXML",
            "DATALINK, java.net.URL",
            "JAVA_OBJECT, java.lang.Object",
            "TIME_WITH_TIMEZONE, java.time.OffsetTime",
            "TIMESTAMP_WITH_TIMEZONE, java.time.OffsetDateTime"
    })
    void mapsUnambiguousSqlTypeToDominantJavaType(String sqlType, String javaType) {
        assertEquals(javaType, mapping.dslToJava(sqlType));
        assertEquals(javaType, mapping.dslToJava(sqlType.toLowerCase()));
    }

    @ParameterizedTest
    @CsvSource({
            "DOUBLE PRECISION, double",
            "TIME WITH TIME ZONE, java.time.OffsetTime",
            "TIMESTAMP WITH TIME ZONE, java.time.OffsetDateTime",
            "CHARACTER VARYING, java.lang.String"
    })
    void mapsSpacedJdbcTypeNames(String sqlType, String javaType) {
        assertEquals(javaType, mapping.dslToJava(sqlType));
        assertTrue(mapping.isCompatible(javaType, sqlType));
    }

    @ParameterizedTest(name = "{0} compatible with {1}")
    @MethodSource("compatiblePairs")
    void reportsCompatibleJavaAndSqlTypes(String javaType, String sqlType) {
        assertTrue(mapping.isCompatible(javaType, sqlType), javaType + " vs " + sqlType);
        assertTrue(mapping.isCompatible(javaType, sqlType.toLowerCase()), javaType + " vs " + sqlType.toLowerCase());
    }

    static Stream<Arguments> compatiblePairs() {
        return Stream.of(
                Arguments.of("int", "INTEGER"),
                Arguments.of("int", "INT"),
                Arguments.of("int", "MEDIUMINT"),
                Arguments.of("java.lang.Integer", "YEAR"),
                Arguments.of("boolean", "BIT"),
                Arguments.of("boolean", "BOOL"),
                Arguments.of("java.lang.String", "VARCHAR"),
                Arguments.of("java.lang.String", "NVARCHAR"),
                Arguments.of("java.lang.String", "TEXT"),
                Arguments.of("java.lang.String", "JSON"),
                Arguments.of("java.lang.String", "CLOB"),
                Arguments.of("java.lang.String", "CHAR"),
                Arguments.of("char", "CHAR"),
                Arguments.of("char", "NCHAR"),
                Arguments.of("double", "FLOAT"),
                Arguments.of("double", "DOUBLE PRECISION"),
                Arguments.of("float", "REAL"),
                Arguments.of("java.sql.Blob", "BLOB"),
                Arguments.of("byte[]", "LONGBLOB"),
                Arguments.of("java.sql.Date", "DATE"),
                Arguments.of("java.time.LocalDate", "DATE"),
                Arguments.of("java.time.OffsetTime", "TIME WITH TIME ZONE"),
                Arguments.of("java.time.ZonedDateTime", "TIMESTAMP_WITH_TIMEZONE"),
                Arguments.of("java.net.URL", "DATALINK"),
                Arguments.of("java.util.UUID", "VARCHAR"),
                Arguments.of("java.io.Reader", "NCLOB"),
                Arguments.of("int[]", "INTEGER"),
                Arguments.of("String[]", "VARCHAR"),
                Arguments.of("java.lang.String[]", "TEXT"));
    }

    @ParameterizedTest
    @CsvSource({
            "float, FLOAT",
            "int, VARCHAR",
            "java.lang.String, INTEGER",
            "java.sql.Date, TIME",
            "boolean, BIGINT",
            "java.net.URL, VARCHAR",
            "java.sql.Blob, CLOB",
            "int[], VARCHAR",
            "String[], INTEGER"
    })
    void rejectsIncompatiblePairs(String javaType, String sqlType) {
        assertFalse(mapping.isCompatible(javaType, sqlType), javaType + " vs " + sqlType);
    }

    @Test
    void unknownMappingsAreNullAndIncompatible() {
        assertNull(mapping.javaToDSL("java.net.URI"));
        assertNull(mapping.dslToJava("GEOMETRY"));
        assertFalse(mapping.isCompatible("int", "GEOMETRY"));
        assertFalse(mapping.isCompatible("int", ""));
    }

    @Test
    void lastBindWinsForOverloadedSqlReverseMapping() {
        assertEquals("java.util.UUID", mapping.dslToJava("CHAR"));
        assertEquals("java.util.UUID", mapping.dslToJava("VARCHAR"));
        assertEquals("java.io.Reader", mapping.dslToJava("CLOB"));
        assertEquals("java.io.Reader", mapping.dslToJava("NCLOB"));
        assertEquals("java.math.BigInteger", mapping.dslToJava("NUMERIC"));
        assertEquals("java.math.BigInteger", mapping.dslToJava("BIGINT"));
        assertEquals("java.io.InputStream", mapping.dslToJava("LONGVARBINARY"));
        assertEquals("java.io.Reader", mapping.dslToJava("LONGVARCHAR"));
    }
}
