package org.jmod.compiler.source;

/**
 * Syntax error while parsing a {@code .jmod} compilation unit.
 */
public class ParseException extends Exception {
    private final int line;
    private final int column;

    public ParseException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public ParseException(String message) {
        this(message, -1, -1);
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String getMessage() {
        if (line < 0) {
            return super.getMessage();
        }
        return line + ":" + column + ": " + super.getMessage();
    }
}
