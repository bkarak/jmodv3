package org.jmod.dsl.regex;

/**
 * A captured match subsequence.
 */
public final class Match {
    private final String value;

    public Match(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value == null ? "" : value;
    }
}
