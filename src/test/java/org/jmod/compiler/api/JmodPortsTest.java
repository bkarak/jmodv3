package org.jmod.compiler.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jmod.JmodPortsFixtures;
import org.jmod.cmd.CompilerOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Compiles the historical application ports from
 * <a href="https://github.com/bkarak/jmod-ports">bkarak/jmod-ports</a>.
 */
class JmodPortsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("org.jmod.JmodPortsFixtures#allJmodArguments")
    void compilesEachPortFile(String relative, Path jmod, @TempDir Path scratch) throws Exception {
        assumeFalse(relative.contains("jmod-live-database"),
                "live-database ports need a running JDBC server; covered by SQLModuleSchemaAndLiveTest");
        Path input = scratch.resolve("in");
        Path output = scratch.resolve("out");
        Files.createDirectories(input);
        Files.copy(jmod, input.resolve(jmod.getFileName()));
        Path parent = jmod.getParent();
        if (parent != null) {
            try (Stream<Path> siblings = Files.list(parent)) {
                for (Path sibling : siblings.toList()) {
                    String name = sibling.getFileName().toString();
                    if (name.endsWith("Configuration.java") || name.endsWith(".sql")) {
                        Files.copy(sibling, input.resolve(name));
                    }
                }
            }
        }
        StringWriter log = new StringWriter();
        boolean ok = compile(input, output, log);
        if (JmodPortsFixtures.KNOWN_BROKEN.contains(relative)) {
            assertFalse(ok, relative + " is a known-broken original and should not compile:\n" + log);
            return;
        }
        if (!ok) {
            fail(relative + " failed to compile:\n" + log);
        }
        assertTrue(generatedJavaExists(output), relative + " produced no Java output\n" + log);
    }

    @Test
    void compilesEachPortTree(@TempDir Path output) throws Exception {
        List<String> ports = List.of(
                "address-book/jmod",
                "examj/jmod",
                "jcrontab/jmod",
                "sdriver/jmod",
                "RUBiS/jmod");
        for (String port : ports) {
            Path input = JmodPortsFixtures.root().resolve(port);
            Path dest = output.resolve(port.replace('/', '_'));
            Files.createDirectories(dest);
            StringWriter log = new StringWriter();
            boolean ok = compile(input, dest, log);
            if (!ok) {
                fail(port + " failed to compile:\n" + log);
            }
        }
    }

    private static boolean compile(Path input, Path output, StringWriter log) {
        Compiler compiler = new Compiler();
        Map<CompilerOption, String> options = new EnumMap<>(CompilerOption.class);
        options.put(CompilerOption.OPT_INPUT_DIR, input.toString());
        options.put(CompilerOption.OPT_OUTPUT_DIR, output.toString());
        return compiler.compile(options, new String[0], log);
    }

    private static boolean generatedJavaExists(Path output) {
        try (Stream<Path> stream = Files.walk(output)) {
            return stream.anyMatch(path -> path.toString().endsWith(".java"));
        } catch (Exception e) {
            return false;
        }
    }
}
