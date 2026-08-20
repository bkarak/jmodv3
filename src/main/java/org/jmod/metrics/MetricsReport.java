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
 * Writes a compact XML metrics report for a compilation unit set.
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
            units.append("    <unit id=\"").append(escape(id)).append("\" file=\"")
                    .append(escape(source.getFile().getPath())).append("\">\n");
            units.append("      <LinesOfCode>").append(stats.getLoc()).append("</LinesOfCode>\n");
            units.append("      <LinesOfComments>").append(stats.getLocom()).append("</LinesOfComments>\n");
            units.append("      <LineCount>").append(stats.getTotal()).append("</LineCount>\n");
            units.append("    </unit>\n");
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
        StringBuilder xml = new StringBuilder();
        xml.append("<metrics>\n  <project>\n");
        xml.append("    <FileCount>").append(files).append("</FileCount>\n");
        xml.append("    <LinesOfCode>").append(loc).append("</LinesOfCode>\n");
        xml.append("    <LinesOfComments>").append(locom).append("</LinesOfComments>\n");
        xml.append("    <LineCount>").append(total).append("</LineCount>\n");
        xml.append("    <NumberOfClasses>").append(classes).append("</NumberOfClasses>\n");
        xml.append("    <NumberOfEnumerations>").append(enums).append("</NumberOfEnumerations>\n");
        xml.append("    <NumberOfInterfaces>").append(interfaces).append("</NumberOfInterfaces>\n");
        xml.append("    <NumberOfDSLTypes>").append(dslTypes).append("</NumberOfDSLTypes>\n");
        xml.append("    <LinesOfDSLCode>").append(dslLines).append("</LinesOfDSLCode>\n");
        xml.append("  </project>\n");
        xml.append(units);
        xml.append("</metrics>\n");
        File parent = destination.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.writeString(destination.toPath(), xml.toString(), StandardCharsets.UTF_8);
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

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
