package org.jmod.compiler.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extraction and replacement of {@code #[name]<Type>} external references.
 */
public final class ExternalRefs {
    public static final Pattern PATTERN = Pattern.compile(
            "#\\[([A-Za-z_][A-Za-z0-9_]*)\\](?:<([^>]+)>)?");
    public static final String DEFAULT_TYPE = "java.lang.String";

    private ExternalRefs() {
    }

    public static List<ExternalRef> extractAll(String body) {
        List<ExternalRef> refs = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(body);
        while (matcher.find()) {
            int[] lineCol = lineColumn(body, matcher.start());
            String type = matcher.group(2) == null || matcher.group(2).isBlank()
                    ? DEFAULT_TYPE
                    : matcher.group(2).trim();
            refs.add(new ExternalRef(matcher.group(1), type, lineCol[0], lineCol[1]));
        }
        return refs;
    }

    /**
     * Unique constructor parameters in first-occurrence order.
     * Throws if the same name is declared with two different types.
     */
    public static List<ExternalRef> uniqueParameters(List<ExternalRef> all) throws ParseException {
        Map<String, ExternalRef> unique = new LinkedHashMap<>();
        for (ExternalRef ref : all) {
            ExternalRef previous = unique.get(ref.getName());
            if (previous == null) {
                unique.put(ref.getName(), ref);
            } else if (!canonicalType(previous.getType()).equals(canonicalType(ref.getType()))) {
                throw new ParseException(
                        "external reference '" + ref.getName() + "' declared with conflicting types "
                                + previous.getType() + " and " + ref.getType(),
                        ref.getLine(),
                        ref.getColumn());
            }
        }
        return new ArrayList<>(unique.values());
    }

    public static String replaceWithPlaceholders(String body) {
        return PATTERN.matcher(body).replaceAll("?");
    }

    public static String replaceWithLiterals(String body, java.util.function.Function<String, String> literalForType) {
        Matcher matcher = PATTERN.matcher(body);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            result.append(body, last, matcher.start());
            String type = matcher.group(2) == null || matcher.group(2).isBlank()
                    ? DEFAULT_TYPE
                    : matcher.group(2).trim();
            result.append(literalForType.apply(type));
            last = matcher.end();
        }
        result.append(body.substring(last));
        return result.toString();
    }

    public static String canonicalType(String type) {
        String trimmed = type.trim().replace(" ", "");
        if (trimmed.indexOf('.') >= 0) {
            if ("java.util.Timestamp".equals(trimmed)) {
                return "java.sql.Timestamp";
            }
            return trimmed;
        }
        switch (trimmed) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "boolean":
            case "char":
            case "byte[]":
                return trimmed;
            case "Byte":
                return "java.lang.Byte";
            case "Short":
                return "java.lang.Short";
            case "Integer":
                return "java.lang.Integer";
            case "Long":
                return "java.lang.Long";
            case "Float":
                return "java.lang.Float";
            case "Double":
                return "java.lang.Double";
            case "Boolean":
                return "java.lang.Boolean";
            case "Character":
                return "java.lang.Character";
            case "String":
                return "java.lang.String";
            case "Object":
                return "java.lang.Object";
            case "Number":
                return "java.lang.Number";
            case "BigDecimal":
                return "java.math.BigDecimal";
            case "BigInteger":
                return "java.math.BigInteger";
            case "Date":
                return "java.util.Date";
            case "Calendar":
                return "java.util.Calendar";
            case "UUID":
                return "java.util.UUID";
            case "Time":
                return "java.sql.Time";
            case "Timestamp":
                return "java.sql.Timestamp";
            case "Blob":
                return "java.sql.Blob";
            case "Clob":
                return "java.sql.Clob";
            case "NClob":
                return "java.sql.NClob";
            case "Array":
                return "java.sql.Array";
            case "Ref":
                return "java.sql.Ref";
            case "Struct":
                return "java.sql.Struct";
            case "RowId":
                return "java.sql.RowId";
            case "SQLXML":
                return "java.sql.SQLXML";
            case "URL":
                return "java.net.URL";
            case "InputStream":
                return "java.io.InputStream";
            case "Reader":
                return "java.io.Reader";
            case "LocalDate":
                return "java.time.LocalDate";
            case "LocalTime":
                return "java.time.LocalTime";
            case "LocalDateTime":
                return "java.time.LocalDateTime";
            case "OffsetTime":
                return "java.time.OffsetTime";
            case "OffsetDateTime":
                return "java.time.OffsetDateTime";
            case "Instant":
                return "java.time.Instant";
            case "ZonedDateTime":
                return "java.time.ZonedDateTime";
            default:
                return trimmed;
        }
    }

    public static String toJavaSourceType(String type) {
        String canonical = canonicalType(type);
        if (canonical.startsWith("java.lang.") && canonical.indexOf('.', "java.lang.".length()) < 0) {
            return canonical.substring("java.lang.".length());
        }
        return type.trim();
    }

    public static List<ExternalRef> empty() {
        return Collections.emptyList();
    }

    private static int[] lineColumn(String text, int index) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < index; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[] {line, column};
    }
}
