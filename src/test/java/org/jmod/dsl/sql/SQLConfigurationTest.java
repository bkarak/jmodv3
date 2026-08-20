package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SQLConfigurationTest {
    @Test
    void defaultsAreEmptyAndDisabled() {
        SQLConfiguration configuration = new SQLConfiguration();
        assertFalse(configuration.SQLMOD_NS_AWARE);
        assertEquals("", configuration.SQLMOD_NS_URI);
        assertFalse(configuration.SQLMOD_LIVE_TEST);
        assertEquals("", configuration.SQLMOD_JDBC_DRIVER);
        assertEquals("", configuration.SQLMOD_DB_URL);
        assertEquals("", configuration.SQLMOD_DB_LOGIN);
        assertEquals("", configuration.SQLMOD_DB_PASSWORD);
    }

    @Test
    void moduleConfigurationExposesAllFlags() {
        SQLConfiguration configuration = new SQLConfiguration();
        configuration.SQLMOD_NS_AWARE = true;
        configuration.SQLMOD_NS_URI = "urn:schema";
        configuration.SQLMOD_LIVE_TEST = true;
        configuration.SQLMOD_JDBC_DRIVER = "org.h2.Driver";
        configuration.SQLMOD_DB_URL = "jdbc:h2:mem:test";
        configuration.SQLMOD_DB_LOGIN = "sa";
        configuration.SQLMOD_DB_PASSWORD = "secret";

        Map<String, String> module = configuration.getModuleConfiguration();
        assertEquals("true", module.get("SQLMOD_NS_AWARE"));
        assertEquals("urn:schema", module.get("SQLMOD_NS_URI"));
        assertEquals("true", module.get("SQLMOD_LIVE_TEST"));
        assertEquals("org.h2.Driver", module.get("SQLMOD_JDBC_DRIVER"));
        assertEquals("jdbc:h2:mem:test", module.get("SQLMOD_DB_URL"));
        assertEquals("sa", module.get("SQLMOD_DB_LOGIN"));
        assertEquals("secret", module.get("SQLMOD_DB_PASSWORD"));
        assertEquals(7, module.size());
    }

    @Test
    void runtimeConfigurationOmitsNamespaceUriAndPassword() {
        SQLConfiguration configuration = new SQLConfiguration();
        configuration.SQLMOD_NS_URI = "urn:schema";
        configuration.SQLMOD_DB_PASSWORD = "secret";
        configuration.SQLMOD_DB_LOGIN = "sa";
        configuration.SQLMOD_DB_URL = "jdbc:h2:mem:test";

        Map<String, String> runtime = configuration.getRuntimeConfiguration();
        assertFalse(runtime.containsKey("SQLMOD_NS_URI"));
        assertFalse(runtime.containsKey("SQLMOD_DB_PASSWORD"));
        assertFalse(runtime.containsKey("SQLMOD_DB_URL"));
        assertFalse(runtime.containsKey("SQLMOD_JDBC_DRIVER"));
        assertFalse(runtime.containsKey("SQLMOD_DB_LOGIN"));
        assertTrue(runtime.containsKey("SQLMOD_NS_AWARE"));
        assertTrue(runtime.containsKey("SQLMOD_LIVE_TEST"));
    }

    @Test
    void sqlModuleDefaultConfigurationMatchesEmptyConfiguration() {
        Map<String, String> defaults = new SQLModule().getDefaultConfiguration();
        Map<String, String> empty = new SQLConfiguration().getModuleConfiguration();
        assertEquals(empty, defaults);
    }
}
