package org.jmod.compiler.source;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser for a single-external-type {@code .jmod} file.
 */
public final class JmodParser {
    private static final Set<String> ALLOWED_MODIFIERS = Set.of("public", "static", "final");
    private static final Set<String> FORBIDDEN_MODIFIERS = Set.of(
            "protected", "private", "abstract", "native", "synchronized",
            "transient", "volatile", "strictfp");

    private final JmodLexer lexer;
    private JmodLexer.Token token;

    private JmodParser(String source) throws ParseException {
        this.lexer = new JmodLexer(source);
        this.token = lexer.nextToken();
    }

    public static CodeUnit parse(File file) throws ParseException, IOException {
        String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return parse(source);
    }

    public static CodeUnit parse(String source) throws ParseException {
        return new JmodParser(source).parseCompilationUnit();
    }

    private CodeUnit parseCompilationUnit() throws ParseException {
        CodeUnit unit = new CodeUnit();
        if (match(JmodLexer.Kind.PACKAGE)) {
            unit.setPackageName(parseQualifiedIdentifier());
            expect(JmodLexer.Kind.SEMI, "expected ';' after package declaration");
        }
        while (match(JmodLexer.Kind.IMPORT)) {
            boolean staticImport = match(JmodLexer.Kind.STATIC);
            String imported = parseQualifiedIdentifier();
            if (match(JmodLexer.Kind.DOT) && match(JmodLexer.Kind.STAR)) {
                imported = imported + ".*";
            }
            if (staticImport) {
                imported = "static " + imported;
            }
            expect(JmodLexer.Kind.SEMI, "expected ';' after import");
            unit.addImport(imported);
        }
        parseExternalDeclaration(unit);
        if (token.kind != JmodLexer.Kind.EOF) {
            throw error("unexpected input after external declaration");
        }
        List<ExternalRef> all = ExternalRefs.extractAll(unit.getDslBody());
        unit.setExternalReferences(all);
        unit.setUniqueParameters(ExternalRefs.uniqueParameters(all));
        return unit;
    }

    private void parseExternalDeclaration(CodeUnit unit) throws ParseException {
        while (isModifier(token.kind)) {
            String modifier = token.text;
            if (FORBIDDEN_MODIFIERS.contains(modifier)) {
                throw error("illegal modifier '" + modifier + "' on external type");
            }
            if (!ALLOWED_MODIFIERS.contains(modifier)) {
                throw error("unsupported modifier '" + modifier + "' on external type");
            }
            unit.addModifier(modifier);
            advance();
        }
        expect(JmodLexer.Kind.EXTERNAL, "expected 'external'");
        if (token.kind != JmodLexer.Kind.IDENTIFIER) {
            throw error("expected external type name");
        }
        unit.setExternalTypeName(token.text);
        advance();
        expect(JmodLexer.Kind.EXTENDS, "expected 'extends'");
        String baseType = parseQualifiedIdentifier();
        unit.setBaseTypeName(baseType);
        // Historical ports omit the configuration type argument (thesis Type is optional).
        if (match(JmodLexer.Kind.LT)) {
            String configuration = parseQualifiedIdentifier();
            unit.setConfigurationTypeName(configuration);
            expect(JmodLexer.Kind.GT, "expected '>' after configuration type");
        }
        if (token.kind != JmodLexer.Kind.LBRACE) {
            throw error("expected '{' to start external body");
        }
        String body = lexer.readBalancedBody(true);
        token = lexer.nextToken();
        unit.setDslBody(trimEdges(body));
    }

    private String parseQualifiedIdentifier() throws ParseException {
        if (token.kind != JmodLexer.Kind.IDENTIFIER) {
            throw error("expected identifier");
        }
        StringBuilder name = new StringBuilder(token.text);
        advance();
        while (match(JmodLexer.Kind.DOT)) {
            if (token.kind != JmodLexer.Kind.IDENTIFIER) {
                throw error("expected identifier after '.'");
            }
            name.append('.').append(token.text);
            advance();
        }
        return name.toString();
    }

    private boolean isModifier(JmodLexer.Kind kind) {
        return kind == JmodLexer.Kind.PUBLIC
                || kind == JmodLexer.Kind.PROTECTED
                || kind == JmodLexer.Kind.PRIVATE
                || kind == JmodLexer.Kind.ABSTRACT
                || kind == JmodLexer.Kind.FINAL
                || kind == JmodLexer.Kind.NATIVE
                || kind == JmodLexer.Kind.SYNCHRONIZED
                || kind == JmodLexer.Kind.TRANSIENT
                || kind == JmodLexer.Kind.VOLATILE
                || kind == JmodLexer.Kind.STRICTFP
                || kind == JmodLexer.Kind.STATIC;
    }

    private boolean match(JmodLexer.Kind kind) throws ParseException {
        if (token.kind == kind) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(JmodLexer.Kind kind, String message) throws ParseException {
        if (token.kind != kind) {
            throw error(message);
        }
        advance();
    }

    private void advance() throws ParseException {
        token = lexer.nextToken();
    }

    private ParseException error(String message) {
        return new ParseException(message, token.line, token.column);
    }

    private static String trimEdges(String body) {
        int start = 0;
        int end = body.length();
        while (start < end && (body.charAt(start) == '\n' || body.charAt(start) == '\r')) {
            start++;
        }
        while (end > start && (body.charAt(end - 1) == '\n' || body.charAt(end - 1) == '\r'
                || body.charAt(end - 1) == ' ' || body.charAt(end - 1) == '\t')) {
            end--;
        }
        return body.substring(start, end);
    }
}
