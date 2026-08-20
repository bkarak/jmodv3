package org.jmod.dsl.module;

/**
 * Type mapping between Java and a hosted DSL.
 */
public interface TypeMapping {
    boolean isCompatible(String javaType, String dslType);

    String javaToDSL(String javaType);

    String dslToJava(String dslType);
}
