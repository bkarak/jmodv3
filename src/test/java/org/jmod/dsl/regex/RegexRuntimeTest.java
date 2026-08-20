package org.jmod.dsl.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegexRuntimeTest {
    @Test
    void matchesIpAddressPattern() {
        Regex<RegexConfiguration> regex = new Regex<>("([0-9]{1,3}\\.){3}[0-9]{1,3}", new RegexConfiguration());
        assertTrue(regex.matches("127.0.0.1"));
        assertEquals(State.FOUND, regex.find("host 10.0.0.2 ok"));
    }
}
