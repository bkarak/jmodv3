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
}
