package org.jmod.dsl.module;

import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.dsl.module.def.DefaultConfiguration;
import org.jmod.symbol.Type;

/**
 * Dummy module used for error recovery when no DSL module matches.
 */
public class DefaultModule extends Module {
    public DefaultModule() {
    }

    @Override
    public boolean isDefaultModule() {
        return true;
    }

    @Override
    public boolean evaluate(CodeUnit cu, Map<String, String> context) {
        return true;
    }

    @Override
    public String getAuthor() {
        return "Vassilios Karakoidas (bkarak@aueb.gr)";
    }

    @Override
    public String getDescription() {
        return "Default External Module (dummy implementation)";
    }

    @Override
    public TypeMapping getTypeMap() {
        return new DefaultMapping();
    }

    @Override
    public String getName() {
        return "Default module";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public Type[] getExternalTypes() {
        return new Type[] {};
    }

    @Override
    public Type getConfigurationType() {
        return new Type("org.jmod.dsl.module.def", "DefaultConfiguration");
    }

    @Override
    public Map<String, String> getDefaultConfiguration() {
        return new DefaultConfiguration().getRuntimeConfiguration();
    }
}
