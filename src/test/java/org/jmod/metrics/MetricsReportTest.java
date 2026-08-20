package org.jmod.metrics;

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
    void compilerWritesMetricsXml(@TempDir Path output) throws Exception {
        Path metrics = output.resolve("metrics.xml");
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, "examples/simpleregex");
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        options.put(CompilerOption.OPT_METRICS, metrics.toString());
        options.put(CompilerOption.OPT_NO_JAVAC, "true");
        assertTrue(compiler.compile(options, new String[0], new java.io.StringWriter()));
        String xml = Files.readString(metrics);
        assertTrue(xml.contains("<NumberOfDSLTypes>"));
        assertTrue(xml.contains("<LinesOfDSLCode>"));
        assertTrue(xml.contains("<FileCount>"));
    }
}
