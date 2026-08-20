package org.jmod.dsl.module.configuration;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

/**
 * Accepts an existing local file as a {@code file:} URI or filesystem path.
 * Relative values such as {@code file://./schema.sql} are resolved against a base
 * directory (the configuration source folder when the compiler loads config).
 */
public final class FileUriValidator implements Validator {
    @Override
    public boolean validate(String value) {
        File file = toFile(value);
        return file != null && file.isFile();
    }

    public static File toFile(String value) {
        return toFile(value, new File("."));
    }

    public static File toFile(String value, File baseDir) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return null;
        }
        File base = baseDir != null ? baseDir : new File(".");
        try {
            String path = stripFileScheme(trimmed);
            if (path == null) {
                return Path.of(URI.create(trimmed)).toFile();
            }
            File file = new File(path);
            if (!file.isAbsolute()) {
                file = new File(base, path);
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return a filesystem path, or {@code null} when {@code value} is a hierarchical
     *         {@code file:} URI that should be parsed with {@link URI}
     */
    static String stripFileScheme(String value) {
        if (!value.startsWith("file:")) {
            return value;
        }
        String rest = value.substring("file:".length());
        if (rest.startsWith("///") || (rest.startsWith("/") && !rest.startsWith("//"))) {
            return null;
        }
        if (rest.startsWith("//")) {
            String afterAuthority = rest.substring(2);
            if (afterAuthority.startsWith("localhost/") || afterAuthority.startsWith("localhost\\")) {
                return afterAuthority.substring("localhost".length());
            }
            return afterAuthority;
        }
        return rest;
    }

    @Override
    public String validValues() {
        return "existing local file (file:/path, file://./relative, or filesystem path)";
    }
}
