package org.jmod.compiler.source;

import java.util.Set;

/**
 * Character scanner for the lightweight J% header grammar.
 */
final class JmodLexer {
    private static final Set<String> KEYWORDS = Set.of(
            "package", "import", "static", "external", "extends",
            "public", "protected", "private", "abstract", "final", "native",
            "synchronized", "transient", "volatile", "strictfp");

    enum Kind {
        PACKAGE, IMPORT, STATIC, EXTERNAL, EXTENDS,
        PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL, NATIVE,
        SYNCHRONIZED, TRANSIENT, VOLATILE, STRICTFP,
        IDENTIFIER, DOT, SEMI, STAR, LT, GT, LBRACK, RBRACK, LBRACE,
        EOF
    }

    static final class Token {
        final Kind kind;
        final String text;
        final int line;
        final int column;

        Token(Kind kind, String text, int line, int column) {
            this.kind = kind;
            this.text = text;
            this.line = line;
            this.column = column;
        }
    }

    private final String source;
    private int pos;
    private int line = 1;
    private int column = 1;

    JmodLexer(String source) {
        this.source = source;
    }

    Token nextToken() throws ParseException {
        skipWhitespaceAndComments();
        if (pos >= source.length()) {
            return new Token(Kind.EOF, "", line, column);
        }
        int tokenLine = line;
        int tokenColumn = column;
        char c = peek();
        switch (c) {
            case '.':
                advance();
                return new Token(Kind.DOT, ".", tokenLine, tokenColumn);
            case ';':
                advance();
                return new Token(Kind.SEMI, ";", tokenLine, tokenColumn);
            case '*':
                advance();
                return new Token(Kind.STAR, "*", tokenLine, tokenColumn);
            case '<':
                advance();
                return new Token(Kind.LT, "<", tokenLine, tokenColumn);
            case '>':
                advance();
                return new Token(Kind.GT, ">", tokenLine, tokenColumn);
            case '[':
                advance();
                return new Token(Kind.LBRACK, "[", tokenLine, tokenColumn);
            case ']':
                advance();
                return new Token(Kind.RBRACK, "]", tokenLine, tokenColumn);
            case '{':
                advance();
                return new Token(Kind.LBRACE, "{", tokenLine, tokenColumn);
            default:
                break;
        }
        if (isIdentifierStart(c)) {
            String ident = readIdentifier();
            Kind kind = keywordKind(ident);
            return new Token(kind, ident, tokenLine, tokenColumn);
        }
        throw new ParseException("unexpected character '" + c + "'", tokenLine, tokenColumn);
    }

    /**
     * Reads a brace-balanced DSL body. {@code openingConsumed} is true when the
     * opening {@code '{'} has already been taken as a token.
     */
    String readBalancedBody(boolean openingConsumed) throws ParseException {
        if (!openingConsumed) {
            skipWhitespaceAndComments();
            if (pos >= source.length() || peek() != '{') {
                throw new ParseException("expected '{' to start external body", line, column);
            }
            advance();
        }
        int start = pos;
        int depth = 1;
        while (pos < source.length() && depth > 0) {
            char c = peek();
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String body = source.substring(start, pos);
                    advance();
                    return body;
                }
            }
            advance();
        }
        throw new ParseException("unclosed external body", line, column);
    }

    int getLine() {
        return line;
    }

    int getColumn() {
        return column;
    }

    private Kind keywordKind(String ident) {
        if (!KEYWORDS.contains(ident)) {
            return Kind.IDENTIFIER;
        }
        return Kind.valueOf(ident.toUpperCase());
    }

    private void skipWhitespaceAndComments() throws ParseException {
        while (pos < source.length()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
                continue;
            }
            if (c == '/' && pos + 1 < source.length()) {
                char next = source.charAt(pos + 1);
                if (next == '/') {
                    skipLineComment();
                    continue;
                }
                if (next == '*') {
                    skipBlockComment();
                    continue;
                }
            }
            break;
        }
    }

    private void skipLineComment() {
        while (pos < source.length() && peek() != '\n') {
            advance();
        }
    }

    private void skipBlockComment() throws ParseException {
        advance();
        advance();
        while (pos + 1 < source.length() && !(peek() == '*' && source.charAt(pos + 1) == '/')) {
            advance();
        }
        if (pos + 1 >= source.length()) {
            throw new ParseException("unclosed block comment", line, column);
        }
        advance();
        advance();
    }

    private String readIdentifier() {
        int start = pos;
        advance();
        while (pos < source.length() && isIdentifierPart(peek())) {
            advance();
        }
        return source.substring(start, pos);
    }

    private boolean isIdentifierStart(char c) {
        return c == '_' || c == '$' || Character.isLetter(c);
    }

    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || Character.isDigit(c);
    }

    private char peek() {
        return source.charAt(pos);
    }

    private void advance() {
        if (pos < source.length() && source.charAt(pos) == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        pos++;
    }
}
