package org.jmod.compiler.source;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.Module;
import org.jmod.symbol.Type;

/**
 * Loads compile-time configuration maps from classpath types and user sources.
 */
public final class ConfigLoader {
    private static final Pattern FIELD = Pattern.compile(
            "(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?"
                    + "(boolean|int|float|String|java\\.lang\\.String)\\s+"
                    + "(\\w+)\\s*=\\s*([^;]+);");

    private static final Pattern CLASS_HEADER = Pattern.compile(
            "\\bclass\\s+(\\w+)\\s+extends\\s+([\\w.]+)");

    private ConfigLoader() {
    }

    public static Map<String, String> load(CodeUnit unit, Module module, List<SourceFile> javaFiles) {
        Map<String, String> context = new LinkedHashMap<>(module.getDefaultConfiguration());
        overlayClasspath(context, module.getConfigurationType());
        Type userConfig = Type.parse(resolveUserConfigName(unit));
        overlayClasspath(context, userConfig);
        overlaySource(context, userConfig, javaFiles);
        return context;
    }

    public static String scanPackage(String source) {
        Matcher matcher = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    public static String scanClassName(String source) {
        Matcher matcher = CLASS_HEADER.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    public static boolean looksLikeConfiguration(String source) {
        return source.contains("extends") && (source.contains("Configuration")
                || source.contains("ExternalConfiguration"));
    }

    private static String resolveUserConfigName(CodeUnit unit) {
        String name = unit.getConfigurationTypeName();
        if (name == null) {
            return "";
        }
        if (name.indexOf('.') >= 0) {
            return name;
        }
        for (String imported : unit.getImports()) {
            if (imported.endsWith("." + name)) {
                return imported;
            }
        }
        if (!unit.getPackageName().isEmpty()) {
            return unit.getPackageName() + "." + name;
        }
        return name;
    }

    private static void overlayClasspath(Map<String, String> context, Type type) {
        if (type == null || type.getName().isEmpty()) {
            return;
        }
        try {
            Class<?> cls = Class.forName(type.getQualifiedName());
            if (!ExternalConfiguration.class.isAssignableFrom(cls)) {
                return;
            }
            ExternalConfiguration instance = (ExternalConfiguration) cls.getDeclaredConstructor().newInstance();
            context.putAll(instance.getRuntimeConfiguration());
            // include compile-only keys too
            for (Field field : cls.getFields()) {
                Object value = field.get(instance);
                if (value != null) {
                    context.put(field.getName(), String.valueOf(value));
                }
            }
            for (Field field : cls.getDeclaredFields()) {
                if (!context.containsKey(field.getName()) && isConfigFieldType(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(instance);
                    if (value != null) {
                        context.put(field.getName(), String.valueOf(value));
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // User configuration types are not on the classpath until javac runs.
        }
    }

    private static boolean isConfigFieldType(Class<?> type) {
        return type == boolean.class || type == int.class || type == float.class
                || type == Boolean.class || type == Integer.class || type == Float.class
                || type == String.class;
    }

    private static void overlaySource(Map<String, String> context, Type type, List<SourceFile> javaFiles) {
        if (type == null || javaFiles == null) {
            return;
        }
        for (SourceFile file : javaFiles) {
            if (file.isExternal()) {
                continue;
            }
            try {
                String source = Files.readString(file.getPath(), StandardCharsets.UTF_8);
                String pkg = scanPackage(source);
                String className = scanClassName(source);
                if (className.isEmpty()) {
                    continue;
                }
                Type found = new Type(pkg, className);
                if (!found.equals(type) && !className.equals(type.getName())) {
                    continue;
                }
                Matcher extendsMatch = CLASS_HEADER.matcher(source);
                if (extendsMatch.find()) {
                    overlayClasspath(context, Type.parse(resolveParent(source, pkg, extendsMatch.group(2))));
                }
                Matcher fields = FIELD.matcher(source);
                while (fields.find()) {
                    context.put(fields.group(2), unquote(fields.group(3).trim()));
                }
            } catch (IOException ignored) {
                // skip unreadable files
            }
        }
    }

    private static String resolveParent(String source, String pkg, String parent) {
        if (parent.indexOf('.') >= 0) {
            return parent;
        }
        Matcher imports = Pattern.compile("(?m)^\\s*import\\s+([\\w.]+)\\s*;").matcher(source);
        while (imports.find()) {
            String imported = imports.group(1);
            if (imported.endsWith("." + parent)) {
                return imported;
            }
        }
        if ("SQLConfiguration".equals(parent)) {
            return "org.jmod.dsl.sql.SQLConfiguration";
        }
        if ("RegexConfiguration".equals(parent)) {
            return "org.jmod.dsl.regex.RegexConfiguration";
        }
        if ("ExternalConfiguration".equals(parent)) {
            return "org.jmod.dsl.module.ExternalConfiguration";
        }
        if (!pkg.isEmpty()) {
            return pkg + "." + parent;
        }
        return parent;
    }

    private static String unquote(String raw) {
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
