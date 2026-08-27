package org.jmod.compiler.source;

/**
 * Escapes DSL text for embedding in a Java string literal without doubling
 * existing backslashes (matching the thesis code-generation examples).
 */
public final class JavaStrings {
    private JavaStrings() {
    }

    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * The runtime value of a Java string literal whose quoted content is
     * {@code literal} — the inverse of what javac does when it compiles the
     * text {@link #escape} emits. Two phases, matching JLS 3.3 and 3.10.7:
     * unicode escapes first (a backslash is eligible only when preceded by an
     * even number of backslashes), then the string escape sequences.
     *
     * @throws IllegalArgumentException on an escape javac would reject —
     *         an unknown sequence such as {@code \.}, a malformed unicode
     *         escape, or a trailing lone backslash. The message names the
     *         offending sequence.
     */
    public static String unescape(String literal) {
        if (literal == null) {
            return "";
        }
        return decodeStringEscapes(decodeUnicodeEscapes(literal));
    }

    private static String decodeUnicodeEscapes(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int backslashRun = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && backslashRun % 2 == 0 && i + 1 < s.length() && s.charAt(i + 1) == 'u') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) == 'u') {
                    j++;
                }
                if (j + 4 > s.length()) {
                    throw new IllegalArgumentException("illegal unicode escape '" + s.substring(i) + "'");
                }
                String hex = s.substring(j, j + 4);
                int value;
                try {
                    value = Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("illegal unicode escape '\\u" + hex + "'");
                }
                char decoded = (char) value;
                sb.append(decoded);
                backslashRun = decoded == '\\' ? backslashRun + 1 : 0;
                i = j + 3;
                continue;
            }
            sb.append(c);
            backslashRun = c == '\\' ? backslashRun + 1 : 0;
        }
        return sb.toString();
    }

    private static String decodeStringEscapes(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= s.length()) {
                throw new IllegalArgumentException("lone '\\' at end of body");
            }
            char next = s.charAt(++i);
            switch (next) {
                case 'b' -> sb.append('\b');
                case 's' -> sb.append(' ');
                case 't' -> sb.append('\t');
                case 'n' -> sb.append('\n');
                case 'f' -> sb.append('\f');
                case 'r' -> sb.append('\r');
                case '"' -> sb.append('"');
                case '\'' -> sb.append('\'');
                case '\\' -> sb.append('\\');
                default -> {
                    if (next >= '0' && next <= '7') {
                        int value = next - '0';
                        int digits = next <= '3' ? 2 : 1;
                        while (digits > 0 && i + 1 < s.length()
                                && s.charAt(i + 1) >= '0' && s.charAt(i + 1) <= '7') {
                            value = value * 8 + (s.charAt(++i) - '0');
                            digits--;
                        }
                        sb.append((char) value);
                    } else {
                        throw new IllegalArgumentException("illegal escape sequence '\\" + next + "'");
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Full Java string-literal escaping, including backslashes. Use for JSON
     * templates and schemas; {@link #escape} leaves {@code \} unchanged for regex.
     */
    public static String escapeJava(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
