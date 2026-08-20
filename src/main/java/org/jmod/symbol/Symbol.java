package org.jmod.symbol;

import java.io.File;

import org.jmod.compiler.source.CodeUnit;

/**
 * One type recorded in the compiler symbol table.
 */
public final class Symbol {
    private final Type type;
    private final SymbolKind kind;
    private final CodeUnit codeUnit;
    private final File sourceFile;

    public Symbol(Type type, SymbolKind kind, CodeUnit codeUnit, File sourceFile) {
        this.type = type;
        this.kind = kind;
        this.codeUnit = codeUnit;
        this.sourceFile = sourceFile;
    }

    public Type getType() {
        return type;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public CodeUnit getCodeUnit() {
        return codeUnit;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public String toString() {
        return type.getQualifiedName() + " (" + kindLabel() + ")";
    }

    private String kindLabel() {
        switch (kind) {
            case EXTERNAL:
                return "External";
            case CONFIGURATION:
                return "Java, Configuration";
            case JAVA:
            default:
                return "Java";
        }
    }
}
