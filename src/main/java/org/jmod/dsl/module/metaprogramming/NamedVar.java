package org.jmod.dsl.module.metaprogramming;

/**
 * Velocity helper exposing {@code getVarName()} as in the SQL code-generation template.
 */
public final class NamedVar {
    private final String varName;

    public NamedVar(String varName) {
        this.varName = varName;
    }

    public String getVarName() {
        return varName;
    }
}
