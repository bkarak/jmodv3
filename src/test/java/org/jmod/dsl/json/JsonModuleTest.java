package org.jmod.dsl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.jmod.dsl.module.ModuleException;
import org.jmod.symbol.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonModuleTest {
    @Test
    void describesJsonModule() {
        JsonModule module = new JsonModule();
        assertEquals("Json", module.getName());
        assertTrue(module.getDescription().contains("JSON"));
        assertEquals("1.0", module.getVersion());
        Type[] types = module.getExternalTypes();
        assertEquals(1, types.length);
        assertEquals("org.jmod.dsl.json.JsonObject", types[0].getQualifiedName());
        assertEquals("org.jmod.dsl.json.JsonConfiguration",
                module.getConfigurationType().getQualifiedName());
        assertInstanceOf(JsonTypeMapping.class, module.getTypeMap());
    }

    @Test
    void rejectsInvalidJson(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Bad extends JsonObject {
                { "name": }
                }
                """);
        ModuleException error = assertThrows(ModuleException.class,
                () -> new JsonModule().evaluate(unit, Map.of()));
        assertTrue(error.getMessage().startsWith("invalid JSON:"), error.getMessage());
    }

    @Test
    void rejectsEmptyBody(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Empty extends JsonObject {
                }
                """);
        ModuleException error = assertThrows(ModuleException.class,
                () -> new JsonModule().evaluate(unit, Map.of()));
        assertTrue(error.getMessage().contains("empty"));
    }

    @Test
    void rejectsUnknownJavaType(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Bad extends JsonObject {
                { "id": #[id]<java.net.URI> }
                }
                """);
        ModuleException error = assertThrows(ModuleException.class,
                () -> new JsonModule().evaluate(unit, Map.of()));
        assertTrue(error.getMessage().contains("unsupported Java type"));
        assertTrue(error.getMessage().contains("java.net.URI"));
    }

    @Test
    void generatesJsonClass(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                package examples.simplejson;
                public external Person extends JsonObject<JsonConfiguration> {
                { "name": #[name]<String>, "age": #[age]<int> }
                }
                """);
        assertTrue(new JsonModule().evaluate(unit, Map.of()));
        String generated = Files.readString(temp.resolve("examples/simplejson/Person.java"));
        assertTrue(generated.contains("class Person extends JsonObject<JsonConfiguration>"));
        assertTrue(generated.contains("public Person(String name, int age)"));
        assertTrue(generated.contains("__JMOD_name__"));
        assertTrue(generated.contains("__JMOD_age__"));
        assertTrue(generated.contains("render(\"name\", name, \"age\", age)"));
        assertTrue(generated.contains(", null)"));
        assertFalse(generated.contains("external"));
        assertFalse(generated.contains("#["));
    }

    @Test
    void schemaAwareAcceptsMatchingTypes(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("person.schema.json");
        Files.writeString(schema, """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "age": { "type": "integer" }
                  },
                  "required": ["name", "age"]
                }
                """);
        CodeUnit unit = unit(temp, """
                public external Person extends JsonObject {
                { "name": #[name]<String>, "age": #[age]<int> }
                }
                """);
        assertTrue(new JsonModule().evaluate(unit, Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", schema.toUri().toString())));
        String generated = Files.readString(temp.resolve("Person.java"));
        assertTrue(generated.contains("type"));
        assertFalse(generated.contains(", null)"));
    }

    @Test
    void schemaAwareRejectsJavaTypeMismatch(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("person.schema.json");
        Files.writeString(schema, """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  },
                  "required": ["age"]
                }
                """);
        CodeUnit unit = unit(temp, """
                public external Bad extends JsonObject {
                { "age": #[age]<String> }
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new JsonModule().evaluate(unit, Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", schema.toUri().toString())));
        assertTrue(error.getMessage().contains("JSON Schema validation failed"), error.getMessage());
    }

    @Test
    void schemaAwareRejectsMissingRequiredProperty(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("person.schema.json");
        Files.writeString(schema, """
                {
                  "type": "object",
                  "required": ["name", "age"],
                  "properties": {
                    "name": { "type": "string" },
                    "age": { "type": "integer" }
                  }
                }
                """);
        CodeUnit unit = unit(temp, """
                public external Bad extends JsonObject {
                { "name": #[name]<String> }
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new JsonModule().evaluate(unit, Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", schema.toUri().toString())));
        assertTrue(error.getMessage().contains("JSON Schema validation failed"), error.getMessage());
    }

    @Test
    void schemaAwareRejectsUnknownSchemaFile(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Bad extends JsonObject {
                { "n": 1 }
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new JsonModule().evaluate(unit, Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", temp.resolve("missing.json").toUri().toString())));
        assertTrue(error.getMessage().contains("invalid JSON Schema"));
    }

    private static CodeUnit unit(Path temp, String source) throws Exception {
        Path file = temp.resolve("input.jmod");
        Files.writeString(file, source);
        CodeUnit unit = JmodParser.parse(file.toFile());
        SourceFile sourceFile = new SourceFile(file.toFile(), true);
        sourceFile.setOutputDir(temp.toFile());
        sourceFile.setPackageName(unit.getPackageName());
        sourceFile.setTypeName(unit.getExternalTypeName());
        unit.setSourceFile(sourceFile);
        return unit;
    }
}
