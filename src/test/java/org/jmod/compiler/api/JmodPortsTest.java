package org.jmod.compiler.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
    /**
     * Original sources that are not valid J% (typos such as {@code imporg} in the published ports).
     */
    private static final Set<String> KNOWN_BROKEN = Set.of(
            "benchmarking/jmod-sql-compiler/jmod-simple/org/jmod/CustomerSelect.jmod",
            "benchmarking/jmod-sql-compiler/jmod-ns-aware/org/jmod/CustomerSelect.jmod",
            "benchmarking/jmod-sql-security/jmod-simple/org/jmod/CustomerSelect.jmod");

    static Stream<Path> jmodFiles() throws Exception {
        return Files.walk(portsRoot())
                .filter(path -> path.toString().endsWith(".jmod"))
                .sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("jmodFiles")
    void compilesEachPortFile(Path jmod, @TempDir Path scratch) throws Exception {
        String relative = portsRoot().relativize(jmod).toString().replace('\\', '/');
        Path input = scratch.resolve("in");
        Path output = scratch.resolve("out");
        Files.createDirectories(input);
        Files.copy(jmod, input.resolve(jmod.getFileName()));
        Path parent = jmod.getParent();
        if (parent != null) {
            try (Stream<Path> siblings = Files.list(parent)) {
                for (Path sibling : siblings.toList()) {
                    String name = sibling.getFileName().toString();
                    if (name.endsWith("Configuration.java")) {
                        Files.copy(sibling, input.resolve(name));
                    }
                }
            }
        }
        StringWriter log = new StringWriter();
        boolean ok = compile(input, output, log);
        if (KNOWN_BROKEN.contains(relative)) {
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
                "RUBiS/jmod",
                "benchmarking/jmod-sql-compiler/jmod-live-database");
        for (String port : ports) {
            Path input = portsRoot().resolve(port);
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

    private static Path portsRoot() {
        URL resource = JmodPortsTest.class.getResource("/jmod-ports");
        if (resource == null) {
            Path fallback = Path.of("src/test/resources/jmod-ports");
            if (Files.isDirectory(fallback)) {
                return fallback.toAbsolutePath();
            }
            throw new IllegalStateException("jmod-ports fixtures are missing");
        }
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
