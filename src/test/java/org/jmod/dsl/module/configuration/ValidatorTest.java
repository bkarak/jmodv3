package org.jmod.dsl.module.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmod.dsl.regex.RegexConfiguration;
import org.jmod.dsl.sql.SQLConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidatorTest {
    @Test
    void booleanAndIntegerValidators() {
        assertTrue(new BooleanValidator().validate("true"));
        assertFalse(new BooleanValidator().validate("yes"));
        assertTrue(new IntegerValidator().validate("42"));
        assertFalse(new IntegerValidator().validate("1.5"));
        assertTrue(new StringArrayValidator("jdk").validate("jdk"));
        assertFalse(new StringArrayValidator("jdk").validate("pcre"));
    }

    @Test
    void fileUriValidatorRequiresExistingFile(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("schema.sql");
        Files.writeString(file, "create table t (id int);");
        FileUriValidator validator = new FileUriValidator();
        assertTrue(validator.validate(file.toUri().toString()));
        assertTrue(validator.validate(file.toString()));
        assertTrue(FileUriValidator.toFile("file://./schema.sql", temp.toFile()).isFile());
        assertTrue(FileUriValidator.toFile("./schema.sql", temp.toFile()).isFile());
        assertFalse(validator.validate("http://example.com/schema.sql"));
        assertFalse(validator.validate(temp.resolve("missing.sql").toString()));
    }

    @Test
    void sqlValidatorsDependOnFlags(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table t (id int);");
        SQLConfiguration configuration = new SQLConfiguration();
        assertTrue(configuration.isValid(Map.of(
                "SQLMOD_NS_AWARE", "false",
                "SQLMOD_NS_URI", "",
                "SQLMOD_LIVE_TEST", "false",
                "SQLMOD_JDBC_DRIVER", "",
                "SQLMOD_DB_URL", "",
                "SQLMOD_DB_LOGIN", "",
                "SQLMOD_DB_PASSWORD", "")));
        assertFalse(configuration.isValid(Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", "not-a-file",
                "SQLMOD_LIVE_TEST", "false",
                "SQLMOD_JDBC_DRIVER", "",
                "SQLMOD_DB_URL", "",
                "SQLMOD_DB_LOGIN", "",
                "SQLMOD_DB_PASSWORD", "")));
        assertTrue(configuration.isValid(Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString(),
                "SQLMOD_LIVE_TEST", "false",
                "SQLMOD_JDBC_DRIVER", "",
                "SQLMOD_DB_URL", "",
                "SQLMOD_DB_LOGIN", "",
                "SQLMOD_DB_PASSWORD", "")));
    }

    @Test
    void regexConfigurationRejectsUnknownEngine() {
        RegexConfiguration configuration = new RegexConfiguration();
        assertTrue(configuration.isValid(Map.of("REGEX_ENGINE", "jdk", "REGEX_OUTPUT", "java")));
        assertFalse(configuration.isValid(Map.of("REGEX_ENGINE", "pcre", "REGEX_OUTPUT", "java")));
    }

    @Test
    void jsonSchemaUriRequiredWhenAware(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("s.json");
        Files.writeString(schema, "{ \"type\": \"object\" }");
        org.jmod.dsl.json.JsonConfiguration configuration = new org.jmod.dsl.json.JsonConfiguration();
        assertTrue(configuration.isValid(Map.of(
                "JSONMOD_SCHEMA_AWARE", "false",
                "JSONMOD_SCHEMA_URI", "")));
        assertFalse(configuration.isValid(Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", "not-a-file")));
        assertTrue(configuration.isValid(Map.of(
                "JSONMOD_SCHEMA_AWARE", "true",
                "JSONMOD_SCHEMA_URI", schema.toUri().toString())));
    }
}
