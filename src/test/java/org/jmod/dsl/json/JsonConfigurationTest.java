package org.jmod.dsl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonConfigurationTest {
    @Test
    void defaultsDisableSchema() {
        JsonConfiguration configuration = new JsonConfiguration();
        assertFalse(configuration.JSONMOD_SCHEMA_AWARE);
        assertEquals("", configuration.JSONMOD_SCHEMA_URI);
    }

    @Test
    void runtimeOmitsSchemaUri() {
        JsonConfiguration configuration = new JsonConfiguration();
        configuration.JSONMOD_SCHEMA_AWARE = true;
        configuration.JSONMOD_SCHEMA_URI = "file://./person.schema.json";
        Map<String, String> module = configuration.getModuleConfiguration();
        assertEquals("true", module.get("JSONMOD_SCHEMA_AWARE"));
        assertEquals("file://./person.schema.json", module.get("JSONMOD_SCHEMA_URI"));
        Map<String, String> runtime = configuration.getRuntimeConfiguration();
        assertTrue(runtime.containsKey("JSONMOD_SCHEMA_AWARE"));
        assertFalse(runtime.containsKey("JSONMOD_SCHEMA_URI"));
    }

    @Test
    void jsonModuleDefaultConfigurationMatchesEmptyConfiguration() {
        assertEquals(new JsonConfiguration().getModuleConfiguration(),
                new JsonModule().getDefaultConfiguration());
    }
}
