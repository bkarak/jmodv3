package org.jmod.dsl.json;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.module.configuration.FileUriValidator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;

/**
 * Loads JSON Schema documents and validates instances.
 */
public final class JsonSchemas {
    private JsonSchemas() {
    }

    public static String load(String uri, File baseDir) throws ModuleException {
        File file = FileUriValidator.toFile(uri, baseDir);
        if (file == null || !file.isFile()) {
            throw new ModuleException("invalid JSON Schema: " + uri);
        }
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            parseSchema(text);
            return text;
        } catch (ModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new ModuleException("could not read JSON Schema: " + uri + " (" + e.getMessage() + ")", e);
        }
    }

    public static JsonNode parseSchema(String schemaJson) throws ModuleException {
        try {
            JsonNode node = JsonSupport.parse(schemaJson);
            if (node == null || node.isMissingNode() || node.isNull()) {
                throw new ModuleException("JSON Schema is empty");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new ModuleException("invalid JSON Schema: " + e.getOriginalMessage(), e);
        }
    }

    public static void validate(String schemaJson, String instanceJson) throws ModuleException {
        try {
            validate(parseSchema(schemaJson), JsonSupport.parse(instanceJson));
        } catch (JsonProcessingException e) {
            throw new ModuleException("invalid JSON: " + e.getOriginalMessage(), e);
        }
    }

    public static void validate(JsonNode schemaNode, JsonNode instance) throws ModuleException {
        JsonSchema schema = factory(schemaNode).getSchema(schemaNode);
        Set<ValidationMessage> errors = schema.validate(instance);
        if (!errors.isEmpty()) {
            String detail = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ModuleException("JSON Schema validation failed: " + detail);
        }
    }

    private static JsonSchemaFactory factory(JsonNode schemaNode) {
        SpecVersion.VersionFlag version;
        try {
            version = SpecVersionDetector.detect(schemaNode);
        } catch (Exception e) {
            version = SpecVersion.VersionFlag.V202012;
        }
        if (version == null) {
            version = SpecVersion.VersionFlag.V202012;
        }
        return JsonSchemaFactory.getInstance(version);
    }
}
