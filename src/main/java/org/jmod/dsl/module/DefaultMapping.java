package org.jmod.dsl.module;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default type-mapping implementation with dominant and compatible rules.
 */
public class DefaultMapping implements TypeMapping {
    private final Map<String, String> javaToDslDominant = new LinkedHashMap<>();
    private final Map<String, String> dslToJavaDominant = new LinkedHashMap<>();
    private final Map<String, java.util.Set<String>> javaToDslCompatible = new LinkedHashMap<>();
    private final Map<String, java.util.Set<String>> dslToJavaCompatible = new LinkedHashMap<>();

    public void addJavaToDSL(String javaType, String dslType, boolean isDominant) {
        javaToDslCompatible.computeIfAbsent(javaType, k -> new java.util.LinkedHashSet<>()).add(dslType);
        if (isDominant) {
            javaToDslDominant.put(javaType, dslType);
        }
    }

    public void addDSLToJava(String dslType, String javaType, boolean isDominant) {
        dslToJavaCompatible.computeIfAbsent(dslType, k -> new java.util.LinkedHashSet<>()).add(javaType);
        if (isDominant) {
            dslToJavaDominant.put(dslType, javaType);
        }
    }

    @Override
    public boolean isCompatible(String javaType, String dslType) {
        java.util.Set<String> dslTypes = javaToDslCompatible.get(javaType);
        if (dslTypes != null && dslTypes.contains(dslType)) {
            return true;
        }
        java.util.Set<String> javaTypes = dslToJavaCompatible.get(dslType);
        return javaTypes != null && javaTypes.contains(javaType);
    }

    @Override
    public String javaToDSL(String javaType) {
        return javaToDslDominant.get(javaType);
    }

    @Override
    public String dslToJava(String dslType) {
        return dslToJavaDominant.get(dslType);
    }

    public boolean isKnownJavaType(String javaType) {
        return javaToDslCompatible.containsKey(javaType) || javaToDslDominant.containsKey(javaType);
    }
}
