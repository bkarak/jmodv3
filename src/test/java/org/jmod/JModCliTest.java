package org.jmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class JModCliTest {
    @Test
    void printsModuleList() {
        StringWriter out = new StringWriter();
        int status = JMod.run(new String[] {"-ml"}, new PrintWriter(out, true), new PrintWriter(new StringWriter()));
        assertEquals(0, status);
        assertTrue(out.toString().contains("Regex"));
        assertTrue(out.toString().contains("SQLModule"));
        assertTrue(out.toString().contains("Default module"));
    }

    @Test
    void printsHelp() {
        StringWriter out = new StringWriter();
        int status = JMod.run(new String[] {"-h"}, new PrintWriter(out, true), new PrintWriter(new StringWriter()));
        assertEquals(0, status);
        assertTrue(out.toString().contains("Usage"));
    }

    @Test
    void requiresInputDir() {
        StringWriter err = new StringWriter();
        int status = JMod.run(new String[] {}, new PrintWriter(new StringWriter()), new PrintWriter(err, true));
        assertEquals(2, status);
        assertTrue(err.toString().contains("input-dir"));
    }
}
