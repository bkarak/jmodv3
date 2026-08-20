package org.jmod.compiler.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import org.jmod.cmd.CompilerOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndToEndCompilerTest {
    @Test
    void compilesSimpleregexExample(@TempDir Path output) {
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, "examples/simpleregex");
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        StringWriter log = new StringWriter();
        assertTrue(compiler.compile(options, new String[0], log), log.toString());
        Path generated = output.resolve("examples/simpleregex/IpAddress.java");
        assertTrue(Files.exists(generated));
        String source = read(generated);
        assertTrue(source.contains("class IpAddress extends Regex<RegexConfiguration>"));
        assertTrue(source.contains("new org.jmod.dsl.regex.RegexConfiguration()"));
        assertTrue(Files.exists(output.resolve("examples/simpleregex/IpAddress.class")));
        assertTrue(Files.exists(output.resolve("examples/simpleregex/Main.class")));
    }

    @Test
    void compilesSimplesqlExample(@TempDir Path output) {
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, "examples/simplesql");
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        StringWriter log = new StringWriter();
        assertTrue(compiler.compile(options, new String[0], log), log.toString());
        String source = read(output.resolve("examples/simplesql/SelectExample.java"));
        assertTrue(source.contains("prepareStatement"));
        assertTrue(source.contains("setInt(_jmod_idx++, prim)"));
        assertTrue(source.contains("where sqle_primary = ?"));
        assertTrue(Files.exists(output.resolve("examples/simplesql/SelectExample.class")));
        assertTrue(Files.exists(output.resolve("examples/simplesql/SimpleConf.class")));
    }

    @Test
    void failsWhenDeclaredConfigurationSourceIsMissing(@TempDir Path input, @TempDir Path output)
            throws Exception {
        Files.writeString(input.resolve("Query.jmod"), """
                package demo;
                import org.jmod.dsl.sql.SQLQuery;
                public external Query extends SQLQuery<MissingConf> {
                select 1
                }
                """);
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, input.toString());
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        StringWriter log = new StringWriter();
        assertFalse(compiler.compile(options, new String[0], log), log.toString());
        assertTrue(log.toString().contains("cannot load configuration type"), log.toString());
        assertTrue(log.toString().contains("MissingConf"), log.toString());
    }

    @Test
    void failsOnInvalidRegex(@TempDir Path input, @TempDir Path output) throws Exception {
        Files.writeString(input.resolve("Bad.jmod"), """
                import org.jmod.dsl.regex.Regex;
                import org.jmod.dsl.regex.RegexConfiguration;
                public external Bad extends Regex<RegexConfiguration> {
                [0-9
                }
                """);
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, input.toString());
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        StringWriter log = new StringWriter();
        assertFalse(compiler.compile(options, new String[0], log));
        assertTrue(log.toString().toLowerCase().contains("regular expression")
                || log.toString().toLowerCase().contains("unclosed"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new AssertionError(path + ": " + e.getMessage(), e);
        }
    }
}
