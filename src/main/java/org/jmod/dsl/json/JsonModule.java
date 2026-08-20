package org.jmod.dsl.json;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.ExternalRef;
import org.jmod.compiler.source.ExternalRefs;
import org.jmod.compiler.source.JavaStrings;
import org.jmod.dsl.module.ExternalConfiguration;
import org.jmod.dsl.module.Module;
import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.TypeMapping;
import org.jmod.dsl.module.metaprogramming.BaseVelocityWriter;
import org.jmod.dsl.module.metaprogramming.NamedVar;
import org.jmod.symbol.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON DSL module: syntax check, optional JSON Schema validation, object codegen.
 */
public class JsonModule extends Module {
    private final JsonTypeMapping typeMapping = new JsonTypeMapping();

    @Override
    public Type getConfigurationType() {
        return new Type("org.jmod.dsl.json", "JsonConfiguration");
    }

    @Override
    public Map<String, String> getDefaultConfiguration() {
        return new JsonConfiguration().getModuleConfiguration();
    }

    @Override
    public ExternalConfiguration newConfiguration() {
        return new JsonConfiguration();
    }

    @Override
    public boolean evaluate(CodeUnit cu, Map<String, String> context) throws ModuleException {
        Map<String, String> cfg = context == null ? Map.of() : context;
        List<ExternalRef> occurrences = cu.getExternalReferences();
        for (ExternalRef ref : occurrences) {
            if (!typeMapping.acceptsJavaType(ref.getType())) {
                throw new ModuleException("unsupported Java type '" + ref.getType()
                        + "' for external reference '" + ref.getName() + "'");
            }
        }
        String body = cu.getDslBody() == null ? "" : cu.getDslBody().trim();
        if (body.isEmpty()) {
            throw new ModuleException("invalid JSON: empty document");
        }
        String literalJson = ExternalRefs.replaceWithLiterals(body, typeMapping::jsonLiteral).trim();
        JsonNode instance;
        try {
            instance = JsonSupport.parse(literalJson);
        } catch (JsonProcessingException e) {
            throw new ModuleException("invalid JSON: " + e.getOriginalMessage(), e);
        }
        if (instance == null || instance.isMissingNode()) {
            throw new ModuleException("invalid JSON: empty document");
        }

        String schemaSource = "null";
        if (Boolean.parseBoolean(cfg.getOrDefault("JSONMOD_SCHEMA_AWARE", "false"))) {
            File baseDir = cu.getSourceFile() == null ? null : cu.getSourceFile().getFile().getParentFile();
            String schemaJson = JsonSchemas.load(cfg.getOrDefault("JSONMOD_SCHEMA_URI", ""), baseDir);
            JsonSchemas.validate(JsonSchemas.parseSchema(schemaJson), instance);
            schemaSource = "\"" + JavaStrings.escapeJava(schemaJson) + "\"";
        }

        Type configurationType = resolveConfigurationType(cu);
        String configurationFqcn = configurationType.getQualifiedName();
        String configurationSimple = configurationType.getName();

        List<String> declarations = new ArrayList<>();
        List<String> ctorParams = new ArrayList<>();
        List<NamedVar> mapped = new ArrayList<>();
        StringBuilder renderArgs = new StringBuilder();
        for (ExternalRef param : cu.getUniqueParameters()) {
            String javaType = ExternalRefs.toJavaSourceType(param.getType());
            declarations.add(javaType + " " + param.getName());
            ctorParams.add(javaType + " " + param.getName());
            mapped.add(new NamedVar(param.getName()));
            if (renderArgs.length() > 0) {
                renderArgs.append(", ");
            }
            renderArgs.append("\"").append(param.getName()).append("\", ").append(param.getName());
        }

        BaseVelocityWriter writer = new BaseVelocityWriter("templates/json.vm", getName());
        writer.add("PACKAGE", cu.getPackageName());
        writer.add("CLASSNAME", cu.getExternalTypeName());
        writer.add("CLASS_CONFIGURATION", configurationFqcn);
        writer.add("CLASS_CONFIGURATION_SIMPLE", configurationSimple);
        writer.add("JSON_TEMPLATE", JavaStrings.escapeJava(JsonTemplate.withMarkers(body)));
        writer.add("JSON_SCHEMA", schemaSource);
        writer.add("JSON_VAR_DECL", declarations);
        writer.add("JSON_CONSTRUCTOR", String.join(", ", ctorParams));
        writer.add("JSON_MAPPED_TYPES", mapped);
        writer.add("JSON_RENDER_ARGS", renderArgs.toString());
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
        return "Json";
    }

    @Override
    public String getDescription() {
        return "JSON object module with JSON Schema validation";
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
        return new Type[] {new Type("org.jmod.dsl.json", "JsonObject")};
    }

    @Override
    public TypeMapping getTypeMap() {
        return typeMapping;
    }
}
