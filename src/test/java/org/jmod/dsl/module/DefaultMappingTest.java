package org.jmod.dsl.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmod.dsl.sql.SQLTypeMapping;
import org.junit.jupiter.api.Test;

class DefaultMappingTest {
    @Test
    void dominantTypeWins() {
        DefaultMapping mapping = new DefaultMapping();
        mapping.addJavaToDSL("java.lang.String", "char", false);
        mapping.addJavaToDSL("java.lang.String", "varchar", true);
        mapping.addDSLToJava("varchar", "java.lang.String", true);

        assertTrue(mapping.isCompatible("java.lang.String", "char"));
        assertEquals("varchar", mapping.javaToDSL("java.lang.String"));
        assertNull(mapping.javaToDSL("varchar"));
        assertEquals("java.lang.String", mapping.dslToJava("varchar"));
    }

    @Test
    void sqlTableAcceptsIntAndString() {
        SQLTypeMapping mapping = new SQLTypeMapping();
        assertTrue(mapping.acceptsJavaType("int"));
        assertTrue(mapping.acceptsJavaType("Integer"));
        assertTrue(mapping.acceptsJavaType("java.lang.String"));
        assertFalse(mapping.acceptsJavaType("java.net.URI"));
        assertEquals("setInt", mapping.setterFor("int"));
        assertEquals("setString", mapping.setterFor("String"));
    }

    @Test
    void sqlMappingCoversJavaAndJdbcTypes() {
        SQLTypeMapping mapping = new SQLTypeMapping();
        String[] javaTypes = {
                "boolean", "byte", "short", "int", "long", "float", "double", "char",
                "Boolean", "Character", "String", "Object", "Number",
                "java.math.BigDecimal", "BigInteger",
                "byte[]",
                "java.sql.Date", "java.sql.Time", "java.sql.Timestamp",
                "java.sql.Blob", "java.sql.Clob", "java.sql.NClob",
                "java.sql.Array", "java.sql.Ref", "java.sql.Struct",
                "java.sql.RowId", "java.sql.SQLXML",
                "java.net.URL", "java.util.UUID", "java.util.Calendar",
                "java.time.LocalDate", "java.time.LocalTime", "java.time.LocalDateTime",
                "java.time.OffsetTime", "java.time.OffsetDateTime",
                "java.time.Instant", "java.time.ZonedDateTime",
                "java.io.InputStream", "java.io.Reader"
        };
        for (String javaType : javaTypes) {
            assertTrue(mapping.acceptsJavaType(javaType), javaType);
        }
        assertTrue(mapping.isCompatible("int", "INTEGER"));
        assertTrue(mapping.isCompatible("java.lang.String", "NVARCHAR"));
        assertTrue(mapping.isCompatible("java.sql.Blob", "BLOB"));
        assertTrue(mapping.isCompatible("java.time.LocalDate", "DATE"));
        assertEquals("INTEGER", mapping.javaToDSL("int"));
        assertEquals("VARCHAR", mapping.javaToDSL("String"));
        assertEquals("int", mapping.dslToJava("INTEGER"));
        assertEquals("setDate", mapping.setterFor("java.sql.Date"));
        assertEquals("setTime", mapping.setterFor("Time"));
        assertEquals("setBlob", mapping.setterFor("Blob"));
        assertEquals("setURL", mapping.setterFor("java.net.URL"));
        assertEquals("setObject", mapping.setterFor("LocalDate"));
        assertEquals("setBinaryStream", mapping.setterFor("InputStream"));
        assertFalse(mapping.acceptsJavaType("java.net.URI"));
    }
}
