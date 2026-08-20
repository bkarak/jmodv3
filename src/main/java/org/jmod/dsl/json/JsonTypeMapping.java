package org.jmod.dsl.json;

import org.jmod.compiler.source.ExternalRefs;
import org.jmod.dsl.module.DefaultMapping;

/**
 * Java types that can be spliced into JSON values, mapped to JSON Schema types.
 */
public final class JsonTypeMapping extends DefaultMapping {

    public JsonTypeMapping() {
        bind(new String[] {"boolean", "java.lang.Boolean"}, "boolean");
        bind(new String[] {"byte", "java.lang.Byte", "short", "java.lang.Short",
                        "int", "java.lang.Integer", "long", "java.lang.Long",
                        "java.math.BigInteger"},
                "integer", "number");
        bind(new String[] {"float", "java.lang.Float", "double", "java.lang.Double",
                        "java.math.BigDecimal", "java.lang.Number"},
                "number");
        bind(new String[] {"char", "java.lang.Character", "java.lang.String"},
                "string");
    }

    public boolean acceptsJavaType(String javaType) {
        if (ExternalRefs.isInArrayType(javaType)) {
            return acceptsJavaType(ExternalRefs.elementType(javaType));
        }
        String canonical = ExternalRefs.canonicalType(javaType);
        return isKnownJavaType(canonical) || isKnownJavaType(javaType.trim().replace(" ", ""));
    }

    @Override
    public boolean isCompatible(String javaType, String dslType) {
        if (ExternalRefs.isInArrayType(javaType)) {
            return "array".equalsIgnoreCase(dslType)
                    || isCompatible(ExternalRefs.elementType(javaType), dslType);
        }
        String java = ExternalRefs.canonicalType(javaType);
        String json = dslType == null ? "" : dslType.trim().toLowerCase();
        return super.isCompatible(java, json)
                || super.isCompatible(java, dslType)
                || super.isCompatible(javaType, json);
    }

    public String jsonLiteral(String javaType) {
        if (ExternalRefs.isInArrayType(javaType)) {
            return "[" + jsonLiteral(ExternalRefs.elementType(javaType)) + "]";
        }
        switch (ExternalRefs.canonicalType(javaType)) {
            case "boolean":
            case "java.lang.Boolean":
                return "true";
            case "byte":
            case "java.lang.Byte":
            case "short":
            case "java.lang.Short":
            case "int":
            case "java.lang.Integer":
            case "long":
            case "java.lang.Long":
            case "java.math.BigInteger":
                return "1";
            case "float":
            case "java.lang.Float":
            case "double":
            case "java.lang.Double":
            case "java.math.BigDecimal":
            case "java.lang.Number":
                return "0.0";
            default:
                return "\"a\"";
        }
    }

    private void bind(String[] javas, String... jsonTypes) {
        for (String java : javas) {
            addJavaToDSL(java, jsonTypes[0], true);
            for (int i = 1; i < jsonTypes.length; i++) {
                addJavaToDSL(java, jsonTypes[i], false);
            }
        }
        for (String json : jsonTypes) {
            addDSLToJava(json, javas[0], true);
            for (int i = 1; i < javas.length; i++) {
                addDSLToJava(json, javas[i], false);
            }
        }
    }
}
