package org.jmod;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Vendored {@code jmod-ports} fixtures from
 * <a href="https://github.com/bkarak/jmod-ports">bkarak/jmod-ports</a>.
 */
public final class JmodPortsFixtures {
    /**
     * Original sources that are not valid J% (typos such as {@code imporg}).
     */
    public static final Set<String> KNOWN_BROKEN = Set.of(
            "benchmarking/jmod-sql-compiler/jmod-simple/org/jmod/CustomerSelect.jmod",
            "benchmarking/jmod-sql-compiler/jmod-ns-aware/org/jmod/CustomerSelect.jmod",
            "benchmarking/jmod-sql-security/jmod-simple/org/jmod/CustomerSelect.jmod");

    private JmodPortsFixtures() {
    }

    public static Path root() {
        URL resource = JmodPortsFixtures.class.getResource("/jmod-ports");
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

    public static String relative(Path jmod) {
        return root().relativize(jmod).toString().replace('\\', '/');
    }

    public static List<Path> allJmodFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(root())) {
            return walk.filter(path -> path.toString().endsWith(".jmod")).sorted().toList();
        }
    }

    public static List<Path> sqlModuleFiles() throws IOException {
        return allJmodFiles().stream()
                .filter(path -> !KNOWN_BROKEN.contains(relative(path)))
                .filter(path -> kind(path) == Kind.SQL)
                .toList();
    }

    public static List<Path> regexModuleFiles() throws IOException {
        return allJmodFiles().stream()
                .filter(path -> !KNOWN_BROKEN.contains(relative(path)))
                .filter(path -> kind(path) == Kind.REGEX)
                .toList();
    }

    public static Stream<Arguments> sqlModuleArguments() throws IOException {
        return sqlModuleFiles().stream().map(path -> Arguments.of(relative(path), path));
    }

    public static Stream<Arguments> regexModuleArguments() throws IOException {
        return regexModuleFiles().stream().map(path -> Arguments.of(relative(path), path));
    }

    public static Stream<Arguments> allJmodArguments() throws IOException {
        return allJmodFiles().stream().map(path -> Arguments.of(relative(path), path));
    }

    public static Kind kind(Path jmod) {
        try {
            String text = Files.readString(jmod, StandardCharsets.UTF_8);
            if (text.contains("extends SQLQuery")) {
                return Kind.SQL;
            }
            if (text.contains("extends Regex")) {
                return Kind.REGEX;
            }
            return Kind.UNKNOWN;
        } catch (IOException e) {
            throw new IllegalStateException(jmod.toString(), e);
        }
    }

    public enum Kind {
        SQL, REGEX, UNKNOWN
    }
}
