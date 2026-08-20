package org.jmod.dsl.json;

import org.jmod.dsl.module.ExternalBaseType;
import org.jmod.dsl.module.ModuleException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Runtime external base type for JSON objects (and other JSON values).
 */
public abstract class JsonObject<T extends JsonConfiguration> extends ExternalBaseType<T> {
    protected final String jsonTemplate;
    private final String schemaJson;

    protected JsonObject(T configuration, String jsonTemplate, String schemaJson) {
        super(configuration);
        this.jsonTemplate = jsonTemplate == null ? "" : jsonTemplate;
        this.schemaJson = blankToNull(schemaJson);
    }

    public String getJsonTemplate() {
        return jsonTemplate;
    }

    public abstract String toJson();

    public JsonNode toJsonNode() {
        try {
            return JsonSupport.parse(toJson());
        } catch (JsonProcessingException e) {
            throw new JsonValidationException("invalid generated JSON: " + e.getOriginalMessage(), e);
        }
    }

    protected String render(Object... namesAndValues) {
        String json = JsonTemplate.expand(jsonTemplate, namesAndValues);
        if (schemaJson != null) {
            try {
                JsonSchemas.validate(schemaJson, json);
            } catch (ModuleException e) {
                throw new JsonValidationException(e.getMessage(), e);
            }
        }
        return json;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
