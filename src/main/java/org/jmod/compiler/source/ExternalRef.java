package org.jmod.compiler.source;

import java.util.Objects;

/**
 * A {@code #[name]<Type>} placeholder bridging Java values into DSL code.
 */
public final class ExternalRef {
    private final String name;
    private final String type;
    private final int line;
    private final int column;

    public ExternalRef(String name, String type, int line, int column) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type).trim();
        this.line = line;
        this.column = column;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "#[" + name + "]<" + type + ">";
    }
}
