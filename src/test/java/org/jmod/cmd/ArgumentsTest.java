package org.jmod.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

class ArgumentsTest {
    @Test
    void parsesSeparatedShortAndLongValues() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"-i", "src", "--output-dir", "out"});
        assertEquals("src", parsed.options().get(CompilerOption.OPT_INPUT_DIR));
        assertEquals("out", parsed.options().get(CompilerOption.OPT_OUTPUT_DIR));
        assertTrue(parsed.operands().isEmpty());
    }

    @Test
    void parsesLongOptionEquals() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"--input-dir=src", "--metrics=report.json"});
        assertEquals("src", parsed.options().get(CompilerOption.OPT_INPUT_DIR));
        assertEquals("report.json", parsed.options().get(CompilerOption.OPT_METRICS));
    }

    @Test
    void parsesAttachedShortArgument() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"-osbuild"});
        assertEquals("sbuild", parsed.options().get(CompilerOption.OPT_OUTPUT_DIR));
    }

    @Test
    void clustersShortFlags() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"-njs"});
        assertTrue(parsed.has(CompilerOption.OPT_NO_JAVAC));
        assertTrue(parsed.has(CompilerOption.OPT_JMOD_ONLY));
        assertTrue(parsed.has(CompilerOption.OPT_SYMBOL_TABLE));
    }

    @Test
    void clusteredFlagThenAttachedValue() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"-nmdump.json"});
        assertTrue(parsed.has(CompilerOption.OPT_NO_JAVAC));
        assertEquals("dump.json", parsed.options().get(CompilerOption.OPT_METRICS));
    }

    @Test
    void repeatsInputDirectories() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"-i", "a", "--input-dir", "b"});
        assertEquals("a" + File.pathSeparator + "b", parsed.options().get(CompilerOption.OPT_INPUT_DIR));
    }

    @Test
    void uniqueLongPrefixMatches() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"--no-jav"});
        assertTrue(parsed.has(CompilerOption.OPT_NO_JAVAC));
    }

    @Test
    void ambiguousLongPrefixIsRejected() {
        UsageException error = assertThrows(UsageException.class, () -> Arguments.parse(new String[] {"--co"}));
        assertTrue(error.getMessage().contains("unrecognized option '--co'"));
    }

    @Test
    void doubleDashEndsOptionParsing() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"-n", "--", "-i", "looks-like-flag"});
        assertTrue(parsed.has(CompilerOption.OPT_NO_JAVAC));
        assertFalse(parsed.has(CompilerOption.OPT_INPUT_DIR));
        assertEquals(java.util.List.of("-i", "looks-like-flag"), parsed.operands());
    }

    @Test
    void collectsOperandsAmongOptions() throws Exception {
        Arguments parsed = Arguments.parse(new String[] {"src", "-o", "out", "extra.jmod"});
        assertEquals("out", parsed.options().get(CompilerOption.OPT_OUTPUT_DIR));
        assertEquals(java.util.List.of("src", "extra.jmod"), parsed.operands());
    }

    @Test
    void rejectsUnknownShortOption() {
        UsageException error = assertThrows(UsageException.class, () -> Arguments.parse(new String[] {"-z"}));
        assertTrue(error.getMessage().contains("invalid option -- 'z'"));
    }

    @Test
    void rejectsMissingShortArgument() {
        UsageException error = assertThrows(UsageException.class, () -> Arguments.parse(new String[] {"-i"}));
        assertTrue(error.getMessage().contains("option requires an argument -- 'i'"));
    }

    @Test
    void rejectsMissingLongArgument() {
        UsageException error = assertThrows(UsageException.class, () -> Arguments.parse(new String[] {"--output-dir"}));
        assertTrue(error.getMessage().contains("option '--output-dir' requires an argument"));
    }

    @Test
    void rejectsArgumentOnFlag() {
        UsageException error = assertThrows(UsageException.class,
                () -> Arguments.parse(new String[] {"--no-javac=true"}));
        assertTrue(error.getMessage().contains("doesn't allow an argument"));
    }
}
