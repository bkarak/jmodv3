package org.jmod.compiler.source;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmod.dsl.module.ConfigurationResolver;
import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.configuration.ConfigFields;
import org.jmod.dsl.module.configuration.FileUriValidator;
import org.jmod.symbol.Type;

/**
 * Loads compile-time configuration maps from classpath types and user sources.
 */
public final class ConfigLoader {
    private static final Pattern FIELD = Pattern.compile(
            "(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?"
                    + "(boolean|Boolean|int|Integer|long|Long|float|Float|double|Double|"
                    + "String|java\\.lang\\.String)\\s+"
                    + "(\\w+)\\s*=\\s*([^;]+);");

    private static final Pattern CLASS_HEADER = Pattern.compile(
            "\\bclass\\s+(\\w+)\\s+extends\\s+([\\w.]+)");

    private ConfigLoader() {
    }

    public static Map<String, String> load(CodeUnit unit, Module module, List<SourceFile> javaFiles)
            throws ModuleException {
        Type moduleDefault = module.getConfigurationType();
        Map<String, String> context = new LinkedHashMap<>(module.getDefaultConfiguration());
        overlayClasspath(context, moduleDefault);

        Type userConfig = ConfigurationResolver.resolve(unit, moduleDefault);
        if (userConfig == null || userConfig.getName().isEmpty()) {
            return context;
        }

        boolean onClasspath = overlayClasspath(context, userConfig);
        boolean fromSource = overlaySourceHierarchy(context, userConfig, javaFiles, new HashSet<>());
        if (ConfigurationResolver.isDeclared(unit) && !onClasspath && !fromSource) {
            throw new ModuleException("cannot load configuration type '"
                    + userConfig.getQualifiedName() + "': not on the classpath and no matching .java source");
        }
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

    private static boolean overlayClasspath(Map<String, String> context, Type type) {
        ExternalConfiguration instance = instantiate(type);
        if (instance == null) {
            return false;
        }
        context.putAll(ConfigFields.read(instance));
        return true;
    }

    private static ExternalConfiguration instantiate(Type type) {
        if (type == null || type.getName().isEmpty()) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(type.getQualifiedName());
            if (!ExternalConfiguration.class.isAssignableFrom(cls)) {
                return null;
            }
            return (ExternalConfiguration) cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean overlaySourceHierarchy(Map<String, String> context, Type type,
            List<SourceFile> javaFiles, Set<String> visited) {
        if (type == null || type.getName().isEmpty() || javaFiles == null) {
            return false;
        }
        String key = type.getQualifiedName();
        if (!visited.add(key)) {
            return false;
        }
        SourceMatch match = findSource(type, javaFiles);
        if (match == null) {
            return false;
        }
        Type parent = parentType(match.source, match.pkg);
        if (parent != null && instantiate(parent) == null) {
            overlaySourceHierarchy(context, parent, javaFiles, visited);
        } else if (parent != null) {
            overlayClasspath(context, parent);
        }
        overlayFields(context, match.source, match.file.getFile().getParentFile());
        return true;
    }

    private static SourceMatch findSource(Type type, List<SourceFile> javaFiles) {
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
                if (found.equals(type)) {
                    return new SourceMatch(file, source, pkg);
                }
            } catch (IOException ignored) {
                // skip unreadable files
            }
        }
        return null;
    }

    private static void overlayFields(Map<String, String> context, String source, File baseDir) {
        Matcher fields = FIELD.matcher(source);
        while (fields.find()) {
            String key = fields.group(2);
            String value = parseJavaValue(fields.group(3));
            if ("SQLMOD_NS_URI".equals(key)) {
                File resolved = FileUriValidator.toFile(value, baseDir);
                if (resolved != null) {
                    value = resolved.getAbsolutePath();
                }
            }
            context.put(key, value);
        }
    }

    private static Type parentType(String source, String pkg) {
        Matcher extendsMatch = CLASS_HEADER.matcher(source);
        if (!extendsMatch.find()) {
            return null;
        }
        String parent = extendsMatch.group(2);
        Type builtin = ConfigurationResolver.builtin(parent);
        if (parent.indexOf('.') >= 0) {
            return Type.parse(parent);
        }
        Matcher imports = Pattern.compile("(?m)^\\s*import\\s+([\\w.]+)\\s*;").matcher(source);
        while (imports.find()) {
            String imported = imports.group(1);
            if (imported.endsWith("." + parent)) {
                return Type.parse(imported);
            }
        }
        if (builtin != null) {
            return builtin;
        }
        if (pkg != null && !pkg.isEmpty()) {
            return new Type(pkg, parent);
        }
        return new Type("", parent);
    }

    private static String parseJavaValue(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"")) {
            int end = trimmed.indexOf('"', 1);
            if (end > 0) {
                return trimmed.substring(1, end);
            }
        }
        return stripComment(trimmed);
    }

    private static String stripComment(String value) {
        int line = value.indexOf("//");
        if (line >= 0) {
            value = value.substring(0, line).trim();
        }
        int block = value.indexOf("/*");
        if (block >= 0) {
            value = value.substring(0, block).trim();
        }
        return value;
    }

    private record SourceMatch(SourceFile file, String source, String pkg) {
    }
}
