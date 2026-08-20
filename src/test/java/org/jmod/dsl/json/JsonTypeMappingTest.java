package org.jmod.dsl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonTypeMappingTest {
    @Test
    void acceptsJsonValueTypesAndArrays() {
        JsonTypeMapping mapping = new JsonTypeMapping();
        assertTrue(mapping.acceptsJavaType("String"));
        assertTrue(mapping.acceptsJavaType("int"));
        assertTrue(mapping.acceptsJavaType("boolean"));
        assertTrue(mapping.acceptsJavaType("double"));
        assertTrue(mapping.acceptsJavaType("String[]"));
        assertTrue(mapping.acceptsJavaType("int[]"));
        assertFalse(mapping.acceptsJavaType("byte[]"));
        assertFalse(mapping.acceptsJavaType("java.net.URI"));
        assertTrue(mapping.isCompatible("int", "integer"));
        assertTrue(mapping.isCompatible("String", "string"));
        assertTrue(mapping.isCompatible("int[]", "array"));
        assertFalse(mapping.isCompatible("int", "string"));
        assertEquals("1", mapping.jsonLiteral("int"));
        assertEquals("\"a\"", mapping.jsonLiteral("String"));
        assertEquals("[\"a\"]", mapping.jsonLiteral("String[]"));
    }
}
