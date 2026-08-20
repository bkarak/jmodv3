package org.jmod.dsl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JsonTemplateTest {
    @Test
    void expandsNamedMarkers() {
        assertEquals("{\"name\":\"Ada\",\"ok\":true}",
                JsonTemplate.expand("{\"name\":__JMOD_name__,\"ok\":__JMOD_ok__}",
                        "name", "Ada", "ok", true));
    }

    @Test
    void encodesStrings() {
        assertEquals("{\"n\":\"a\\\"b\"}",
                JsonTemplate.expand("{\"n\":__JMOD_n__}", "n", "a\"b"));
    }

    @Test
    void rejectsMissingValue() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonTemplate.expand("{\"n\":__JMOD_n__}"));
    }
}
