package org.jmod.metrics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.SourceFile;

/**
 * Writes a compact JSON metrics report for a compilation unit set.
 */
public final class MetricsReport {
    private static final Pattern TYPE_DECL = Pattern.compile("\\b(class|interface|enum)\\s+\\w+");

    private MetricsReport() {
    }

    public static void write(File destination, List<SourceFile> sources, List<CodeUnit> externals)
            throws IOException {
        int files = sources.size();
        int dslTypes = externals.size();
        int dslLines = 0;
        for (CodeUnit unit : externals) {
            dslLines += lineCount(unit.getDslBody());
        }
        int loc = 0;
        int locom = 0;
        int total = 0;
        int classes = 0;
        int enums = 0;
        int interfaces = 0;
        StringBuilder units = new StringBuilder();
        boolean firstUnit = true;
        for (SourceFile source : sources) {
            LineStatistics stats = new LineStatistics(source.getFile());
            loc += stats.getLoc();
            locom += stats.getLocom();
            total += stats.getTotal();
            String id = source.getPackageName().replace('.', '/');
            if (!id.isEmpty()) {
                id += "/";
            }
            id += source.getTypeName().isEmpty() ? source.getFile().getName() : source.getTypeName();
            if (!firstUnit) {
                units.append(",\n");
            }
            firstUnit = false;
            units.append("    {\n");
            units.append("      \"id\": ").append(jsonString(id)).append(",\n");
            units.append("      \"file\": ").append(jsonString(source.getFile().getPath())).append(",\n");
            units.append("      \"linesOfCode\": ").append(stats.getLoc()).append(",\n");
            units.append("      \"linesOfComments\": ").append(stats.getLocom()).append(",\n");
            units.append("      \"lineCount\": ").append(stats.getTotal()).append('\n');
            units.append("    }");
            if (!source.isExternal() && source.getFile().getName().endsWith(".java")) {
                String text = Files.readString(source.getPath(), StandardCharsets.UTF_8);
                Matcher matcher = TYPE_DECL.matcher(text);
                while (matcher.find()) {
                    switch (matcher.group(1)) {
                        case "class" -> classes++;
                        case "enum" -> enums++;
                        case "interface" -> interfaces++;
                        default -> {
                        }
                    }
                }
            }
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"files\": ").append(files).append(",\n");
        json.append("  \"linesOfCode\": ").append(loc).append(",\n");
        json.append("  \"linesOfComments\": ").append(locom).append(",\n");
        json.append("  \"lineCount\": ").append(total).append(",\n");
        json.append("  \"classes\": ").append(classes).append(",\n");
        json.append("  \"enumerations\": ").append(enums).append(",\n");
        json.append("  \"interfaces\": ").append(interfaces).append(",\n");
        json.append("  \"dslTypes\": ").append(dslTypes).append(",\n");
        json.append("  \"dslLines\": ").append(dslLines).append(",\n");
        json.append("  \"units\": [\n");
        json.append(units);
        if (units.length() > 0) {
            json.append('\n');
        }
        json.append("  ]\n");
        json.append("}\n");
        File parent = destination.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.writeString(destination.toPath(), json.toString(), StandardCharsets.UTF_8);
    }

    private static int lineCount(String body) {
        if (body == null || body.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
