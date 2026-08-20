package org.jmod.dsl.getset;

import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.dsl.module.DefaultMapping;
import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.TypeMapping;
import org.jmod.dsl.module.metaprogramming.BaseVelocityWriter;
import org.jmod.symbol.Type;

/**
 * Generates Java fields plus optional getters and setters from {@code #[name]<Type>}.
 */
public class GetSetModule extends Module {
    @Override
    public Type getConfigurationType() {
        return new Type("org.jmod.dsl.getset", "GetSetConfiguration");
    }

    @Override
    public Map<String, String> getDefaultConfiguration() {
        return new GetSetConfiguration().getModuleConfiguration();
    }

    @Override
    public ExternalConfiguration newConfiguration() {
        return new GetSetConfiguration();
    }

    @Override
    public boolean evaluate(CodeUnit cu, Map<String, String> context) throws ModuleException {
        Map<String, String> cfg = context == null ? Map.of() : context;
        Type configurationType = resolveConfigurationType(cu);
        String configurationFqcn = configurationType.getQualifiedName();
        String configurationSimple = configurationType.getName();
        BaseVelocityWriter writer = new BaseVelocityWriter("templates/getset.vm", getName());
        writer.add("PACKAGE", cu.getPackageName());
        writer.add("CLASSNAME", cu.getExternalTypeName());
        writer.add("CLASS_CONFIGURATION", configurationFqcn);
        writer.add("CLASS_CONFIGURATION_SIMPLE", configurationSimple);
        writer.add("GS_EXTREFS", cu.getUniqueParameters());
        writer.add("GS_GEN_GETTER", cfg.getOrDefault("GS_GEN_GETTER", "true"));
        writer.add("GS_GEN_SETTER", cfg.getOrDefault("GS_GEN_SETTER", "true"));
        if (cu.getSourceFile() == null) {
            throw new ModuleException("missing source file for " + cu.getExternalTypeName());
        }
        if (!writer.save(cu.getSourceFile().getCanonicalOutputFile())) {
            throw new ModuleException("failed to write generated source for " + cu.getExternalTypeName());
        }
        return true;
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

    @Override
    public String getName() {
        return "GetSet";
    }

    @Override
    public String getDescription() {
        return "Getter - Setter Generator";
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
        return new Type[] {new Type("org.jmod.dsl.getset", "GetSetType")};
    }
}
