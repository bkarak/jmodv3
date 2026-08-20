package org.jmod.dsl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
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

class JsonModuleGeneratedRuntimeTest {
    @Test
    void generatedClassRendersJson(@TempDir Path temp) throws Exception {
        Path classes = compileExternal(temp, """
                public external Person extends JsonObject {
                { "name": #[name]<String>, "age": #[age]<int> }
                }
                """);
        Object person = load(classes, "Person")
                .getConstructor(String.class, int.class)
                .newInstance("Ada", 36);
        assertEquals("{\"name\":\"Ada\",\"age\":36}",
                compact((String) person.getClass().getMethod("toJson").invoke(person)));
    }

    @Test
    void generatedClassValidatesAgainstEmbeddedSchema(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("minmax.json");
        Files.writeString(schema, """
                {
                  "type": "object",
                  "properties": { "age": { "type": "integer", "minimum": 0 } },
                  "required": ["age"]
                }
                """);
        Path classes = compileExternal(temp, """
                public external Aged extends JsonObject {
                { "age": #[age]<int> }
                }
                """, Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", schema.toUri().toString()));
        Object valid = load(classes, "Aged").getConstructor(int.class).newInstance(3);
        assertEquals("{\"age\":3}", compact((String) valid.getClass().getMethod("toJson").invoke(valid)));

        Object invalid = load(classes, "Aged").getConstructor(int.class).newInstance(-1);
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> invalid.getClass().getMethod("toJson").invoke(invalid));
        assertTrue(error.getCause() instanceof JsonValidationException);
        assertTrue(error.getCause().getMessage().contains("JSON Schema"));
    }

    private static Path compileExternal(Path temp, String source) throws Exception {
        return compileExternal(temp, source, Map.of());
    }

    private static Path compileExternal(Path temp, String source, Map<String, String> cfg) throws Exception {
        Path file = temp.resolve("input.jmod");
        Files.writeString(file, source);
        CodeUnit unit = JmodParser.parse(file.toFile());
        SourceFile sourceFile = new SourceFile(file.toFile(), true);
        sourceFile.setOutputDir(temp.toFile());
        sourceFile.setPackageName(unit.getPackageName());
        sourceFile.setTypeName(unit.getExternalTypeName());
        unit.setSourceFile(sourceFile);
        assertTrue(new JsonModule().evaluate(unit, cfg));

        Path generated = sourceFile.getCanonicalOutputFile().toPath();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "JDK javac is required to compile generated JSON classes");
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
                JsonObject.class.getClassLoader());
        return Class.forName(name, true, loader);
    }

    private static String compact(String json) {
        return json.replaceAll("\\s+", "");
    }
}
