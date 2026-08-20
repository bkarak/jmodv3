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

    /** Velocity alias used by GetSet / SQL templates. */
    public String getVarName() {
        return name;
    }

    /** Java source type for generated fields. */
    public String getTypeName() {
        return ExternalRefs.toJavaSourceType(type);
    }

    public String getAccessorSuffix() {
        if (name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
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
