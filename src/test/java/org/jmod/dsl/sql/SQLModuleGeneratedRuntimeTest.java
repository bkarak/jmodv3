package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
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

class SQLModuleGeneratedRuntimeTest {
    @Test
    void generatedClassBindsParametersInOrder(@TempDir Path temp) throws Exception {
        Path generated = compileExternal(temp, """
                public external BindOrder extends SQLQuery {
                select * from t where id = #[id]<int> and name = #[name]<String> and flag = #[flag]<boolean>
                }
                """);

        Object query = load(temp, generated, "BindOrder")
                .getConstructor(int.class, String.class, boolean.class)
                .newInstance(7, "alice", true);

        assertEquals("select * from t where id = ? and name = ? and flag = ?",
                query.getClass().getMethod("getSQLStatement").invoke(query));

        RecordingJdbc jdbc = new RecordingJdbc();
        query.getClass().getMethod("getStatement", Connection.class).invoke(query, jdbc.connection());
        assertEquals(List.of(
                "prepareStatement:select * from t where id = ? and name = ? and flag = ?",
                "setInt(1, 7)",
                "setString(2, alice)",
                "setBoolean(3, true)"), jdbc.calls);
    }

    @Test
    void generatedClassReusesRepeatedPlaceholder(@TempDir Path temp) throws Exception {
        Path generated = compileExternal(temp, """
                public external SameBound extends SQLQuery {
                select * from t where min_val = #[bound]<long> or max_val = #[bound]<long>
                }
                """);
        Object query = load(temp, generated, "SameBound")
                .getConstructor(long.class)
                .newInstance(9L);
        RecordingJdbc jdbc = new RecordingJdbc();
        query.getClass().getMethod("getStatement", Connection.class).invoke(query, jdbc.connection());
        assertEquals(List.of(
                "prepareStatement:select * from t where min_val = ? or max_val = ?",
                "setLong(1, 9)",
                "setLong(2, 9)"), jdbc.calls);
    }

    @Test
    void generatedClassUsesSetObjectForJavaTime(@TempDir Path temp) throws Exception {
        Path generated = compileExternal(temp, """
                public external ByDate extends SQLQuery {
                select * from t where created = #[created]<java.time.LocalDate>
                }
                """);
        java.time.LocalDate date = java.time.LocalDate.of(2024, 1, 2);
        Object query = load(temp, generated, "ByDate")
                .getConstructor(java.time.LocalDate.class)
                .newInstance(date);
        RecordingJdbc jdbc = new RecordingJdbc();
        query.getClass().getMethod("getStatement", Connection.class).invoke(query, jdbc.connection());
        assertTrue(jdbc.calls.get(0).startsWith("prepareStatement:"));
        assertEquals("setObject(1, " + date + ")", jdbc.calls.get(1));
    }

    @Test
    void generatedParameterlessQueryOnlyPrepares(@TempDir Path temp) throws Exception {
        Path generated = compileExternal(temp, """
                public external AllRows extends SQLQuery {
                select * from items
                }
                """);
        Object query = load(temp, generated, "AllRows").getConstructor().newInstance();
        RecordingJdbc jdbc = new RecordingJdbc();
        Object statement = query.getClass().getMethod("getStatement", Connection.class)
                .invoke(query, jdbc.connection());
        assertTrue(Proxy.isProxyClass(statement.getClass()));
        assertEquals(List.of("prepareStatement:select * from items"), jdbc.calls);
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
        assertTrue(new SQLModule().evaluate(unit, Map.of()));

        Path generated = sourceFile.getCanonicalOutputFile().toPath();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "JDK javac is required to compile generated SQL classes");
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

    private static Class<?> load(Path temp, Path classes, String name) throws Exception {
        URLClassLoader loader = new URLClassLoader(
                new URL[] {classes.toUri().toURL()},
                SQLQuery.class.getClassLoader());
        return Class.forName(name, true, loader);
    }

    private static final class RecordingJdbc {
        private final List<String> calls = new ArrayList<>();

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName()) && args != null && args.length == 1) {
                            calls.add("prepareStatement:" + args[0]);
                            return preparedStatement();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement preparedStatement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            calls.add(method.getName() + "(" + format(args) + ")");
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private static String format(Object[] args) {
            if (args == null || args.length == 0) {
                return "";
            }
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    text.append(", ");
                }
                text.append(args[i]);
            }
            return text.toString();
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0f;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
