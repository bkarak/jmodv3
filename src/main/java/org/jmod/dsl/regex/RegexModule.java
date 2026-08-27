package org.jmod.dsl.regex;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JavaStrings;
import org.jmod.dsl.module.DefaultMapping;
import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.TypeMapping;
import org.jmod.dsl.module.metaprogramming.BaseVelocityWriter;
import org.jmod.symbol.Type;

/**
 * Regular-expression DSL module (JDK {@link Pattern} backend).
 */
public class RegexModule extends Module {
    @Override
    public Type getConfigurationType() {
        return new Type("org.jmod.dsl.regex", "RegexConfiguration");
    }

    @Override
    public Map<String, String> getDefaultConfiguration() {
        return new RegexConfiguration().getModuleConfiguration();
    }

    @Override
    public ExternalConfiguration newConfiguration() {
        return new RegexConfiguration();
    }

    @Override
    public boolean evaluate(CodeUnit cu, Map<String, String> context) throws ModuleException {
        String engine = context == null ? "jdk" : context.getOrDefault("REGEX_ENGINE", "jdk");
        if (!"jdk".equalsIgnoreCase(engine)) {
            throw new ModuleException("unsupported regex engine '" + engine + "'; only jdk is supported");
        }
        String body = cu.getDslBody() == null ? "" : cu.getDslBody().trim();
        // The template embeds the body in a Java string literal, so the pattern
        // the runtime sees is that literal's unescaped value. Validate exactly
        // that text: compiling the raw body would accept patterns that differ
        // from the one that runs, and let illegal escapes surface as javac
        // errors inside generated code.
        String runtimePattern;
        try {
            runtimePattern = JavaStrings.unescape(JavaStrings.escape(body));
        } catch (IllegalArgumentException e) {
            throw new ModuleException("invalid regular expression: " + e.getMessage()
                    + "; the body is embedded in a Java string literal, so write"
                    + " Java-style backslashes (\\\\. for an escaped dot)", e);
        }
        try {
            Pattern.compile(runtimePattern);
        } catch (PatternSyntaxException e) {
            throw new ModuleException("invalid regular expression: " + e.getDescription()
                    + " near index " + e.getIndex(), e);
        }
        Type configurationType = resolveConfigurationType(cu);
        String configurationFqcn = configurationType.getQualifiedName();
        String configurationSimple = configurationType.getName();
        BaseVelocityWriter writer = new BaseVelocityWriter("templates/regex.vm", getName());
        writer.add("PACKAGE", cu.getPackageName());
        writer.add("CLASSNAME", cu.getExternalTypeName());
        writer.add("CLASS_CONFIGURATION", configurationFqcn);
        writer.add("CLASS_CONFIGURATION_SIMPLE", configurationSimple);
        writer.add("REGEX", JavaStrings.escape(body));
        if (cu.getSourceFile() == null) {
            throw new ModuleException("missing source file for " + cu.getExternalTypeName());
        }
        if (!writer.save(cu.getSourceFile().getCanonicalOutputFile())) {
            throw new ModuleException("failed to write generated source for " + cu.getExternalTypeName());
        }
        return true;
    }

    @Override
    public String getName() {
        return "Regex";
    }

    @Override
    public String getDescription() {
        return "Regular expression library module";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "Vassilios Karakoidas (bkarak@aueb.gr)";
    }

    @Override
    public Type[] getExternalTypes() {
        return new Type[] {new Type("org.jmod.dsl.regex", "Regex")};
    }

    @Override
    public TypeMapping getTypeMap() {
        return new DefaultMapping() {
            @Override
            public boolean isCompatible(String javaType, String dslType) {
                return javaType != null && javaType.equals(dslType);
            }

            @Override
            public String javaToDSL(String javaType) {
                return javaType;
            }

            @Override
            public String dslToJava(String dslType) {
                return dslType;
            }
        };
    }

}
