package org.jmod.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import org.jmod.cmd.CompilerOption;
import org.jmod.compiler.api.Compiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetricsReportTest {
    @Test
    void compilerWritesMetricsJson(@TempDir Path output) throws Exception {
        Path metrics = output.resolve("metrics.json");
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, "examples/simpleregex");
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        options.put(CompilerOption.OPT_METRICS, metrics.toString());
        options.put(CompilerOption.OPT_NO_JAVAC, "true");
        assertTrue(compiler.compile(options, new String[0], new java.io.StringWriter()));
        String json = Files.readString(metrics);
        assertTrue(json.contains("\"dslTypes\""));
        assertTrue(json.contains("\"dslLines\""));
        assertTrue(json.contains("\"files\""));
        assertTrue(json.contains("\"units\""));
        assertFalse(json.contains("<metrics>"));
    }

    @Test
    void jsonStringEscapesControlCharacters() {
        assertTrue(MetricsReport.jsonString("a\"b\\c").contains("\\\""));
        assertTrue(MetricsReport.jsonString("a\"b\\c").contains("\\\\"));
        assertEquals("\"a\\nb\"", MetricsReport.jsonString("a\nb"));
    }
}
