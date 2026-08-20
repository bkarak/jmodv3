package org.jmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JModCliTest {
    @Test
    void printsModuleList() {
        StringWriter out = new StringWriter();
        int status = JMod.run(new String[] {"-l"}, new PrintWriter(out, true), new PrintWriter(new StringWriter()));
        assertEquals(0, status);
        assertTrue(out.toString().contains("Regex"));
        assertTrue(out.toString().contains("SQLModule"));
        assertTrue(out.toString().contains("GetSet"));
        assertTrue(out.toString().contains("Json"));
    }

    @Test
    void printsModuleListWithLongOption() {
        StringWriter out = new StringWriter();
        int status = JMod.run(new String[] {"--module-list"}, new PrintWriter(out, true),
                new PrintWriter(new StringWriter()));
        assertEquals(0, status);
        assertTrue(out.toString().contains("SQLModule"));
    }

    @Test
    void printsCompilerContextWithoutInput() {
        StringWriter out = new StringWriter();
        int status = JMod.run(new String[] {"-c"}, new PrintWriter(out, true), new PrintWriter(new StringWriter()));
        assertEquals(0, status);
        assertTrue(out.toString().contains("Compiler Context Dump"));
    }

    @Test
    void helpListsGnuFlags() {
        StringWriter out = new StringWriter();
        int status = JMod.run(new String[] {"--help"}, new PrintWriter(out, true), new PrintWriter(new StringWriter()));
        assertEquals(0, status);
        String help = out.toString();
        assertTrue(help.contains("-i, --input-dir=DIR"));
        assertTrue(help.contains("-o, --output-dir=DIR"));
        assertTrue(help.contains("-m, --metrics=FILE"));
        assertTrue(help.contains("-w, --work-dir=DIR"));
        assertTrue(help.contains("-e, --print-external-context"));
        assertTrue(help.contains("-n, --no-javac"));
        assertTrue(help.contains("--compile-with-javac"));
        assertFalse(help.contains("--output-xml"));
        assertFalse(help.contains("-xml"));
        assertFalse(help.contains("XML"));
    }

    @Test
    void requiresInputDir() {
        StringWriter err = new StringWriter();
        int status = JMod.run(new String[] {}, new PrintWriter(new StringWriter()), new PrintWriter(err, true));
        assertEquals(2, status);
        assertTrue(err.toString().contains("missing input directory"));
        assertTrue(err.toString().contains("--help"));
    }

    @Test
    void rejectsUnknownLongOption() {
        StringWriter err = new StringWriter();
        int status = JMod.run(new String[] {"--output-xml"}, new PrintWriter(new StringWriter()),
                new PrintWriter(err, true));
        assertEquals(2, status);
        assertTrue(err.toString().contains("unrecognized option '--output-xml'"));
    }

    @Test
    void acceptsEqualsFormAndOperands(@TempDir Path output) {
        StringWriter err = new StringWriter();
        int status = JMod.run(new String[] {
                "--output-dir=" + output,
                "--no-javac",
                "-i", "examples/simpleregex"
        }, new PrintWriter(new StringWriter()), new PrintWriter(err, true));
        assertEquals(0, status, err.toString());
        assertTrue(Files.exists(output.resolve("examples/simpleregex/IpAddress.java")));
    }

    @Test
    void acceptsPositionalInput(@TempDir Path output) {
        StringWriter err = new StringWriter();
        int status = JMod.run(new String[] {
                "-o", output.toString(),
                "-n",
                "examples/simpleregex"
        }, new PrintWriter(new StringWriter()), new PrintWriter(err, true));
        assertEquals(0, status, err.toString());
        assertTrue(Files.exists(output.resolve("examples/simpleregex/IpAddress.java")));
    }
}
