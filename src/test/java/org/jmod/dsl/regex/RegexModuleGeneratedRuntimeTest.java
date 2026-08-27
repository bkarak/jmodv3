package org.jmod.dsl.regex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegexModuleGeneratedRuntimeTest {
    @Test
    void generatedClassRunsTheValidatedPattern(@TempDir Path temp) throws Exception {
        // File body: a\\.b|\\d+ — Java-string-style escapes. The pattern that
        // must run is a\.b|\d+ (literal dot, digit class), i.e. exactly the
        // text the module validated. This pins validation and execution to the
        // same pattern across the whole pipeline: parse, validate, generate,
        // javac, load, match.
        Path classes = compileExternal(temp,
                "public external DotOrDigits extends Regex<RegexConfiguration> {\n"
                        + "a\\\\.b|\\\\d+\n"
                        + "}\n");
        Object regex = load(classes, "DotOrDigits").getConstructor().newInstance();
        var matches = regex.getClass().getMethod("matches", CharSequence.class);
        assertTrue((Boolean) matches.invoke(regex, "a.b"));
        assertFalse((Boolean) matches.invoke(regex, "axb"));
        assertTrue((Boolean) matches.invoke(regex, "42"));
    }

    private static Path compileExternal(Path temp, String source) throws Exception {
        Path file = temp.resolve("input.jmod");
        Files.writeString(file, source);
        CodeUnit unit = JmodParser.parse(file.toFile());
        SourceFile sourceFile = new SourceFile(file.toFile(), true);
        sourceFile.setOutputDir(temp.toFile());
        sourceFile.setPackageName(unit.getPackageName());
        sourceFile.setTypeName(unit.getExternalTypeName());
        unit.setSourceFile(sourceFile);
        assertTrue(new RegexModule().evaluate(unit, Map.of("REGEX_ENGINE", "jdk")));

        Path generated = sourceFile.getCanonicalOutputFile().toPath();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "JDK javac is required to compile generated regex classes");
        Path classes = temp.resolve("classes");
        Files.createDirectories(classes);
        StringWriter errors = new StringWriter();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                null, null, StandardCharsets.UTF_8)) {
            Boolean ok = compiler.getTask(
                    errors,
                    fileManager,
                    null,
                    List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString()),
                    null,
                    fileManager.getJavaFileObjects(generated.toFile())).call();
            assertTrue(Boolean.TRUE.equals(ok), errors.toString());
        }
        return classes;
    }

    private static Class<?> load(Path classes, String name) throws Exception {
        URLClassLoader loader = new URLClassLoader(
                new URL[] {classes.toUri().toURL()},
                Regex.class.getClassLoader());
        return Class.forName(name, true, loader);
    }
}
