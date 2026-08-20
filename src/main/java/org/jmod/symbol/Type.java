package org.jmod.symbol;

import java.util.Objects;

/**
 * A named type in the J% symbol table (package + simple name).
 */
public final class Type {
    private final String packageName;
    private final String name;

    public Type(String packageName, String name) {
        this.packageName = packageName == null ? "" : packageName;
        this.name = Objects.requireNonNull(name, "name");
    }

    public static Type parse(String qualified) {
        if (qualified == null || qualified.isBlank()) {
            return new Type("", "");
        }
        int lastDot = qualified.lastIndexOf('.');
        if (lastDot < 0) {
            return new Type("", qualified);
        }
        return new Type(qualified.substring(0, lastDot), qualified.substring(lastDot + 1));
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        if (packageName.isEmpty()) {
            return name;
        }
        return packageName + "." + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Type)) {
            return false;
        }
        Type type = (Type) o;
        return packageName.equals(type.packageName) && name.equals(type.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, name);
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }
}
