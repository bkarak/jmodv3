package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.jmod.dsl.module.ModuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLModuleSchemaAndLiveTest {
    @Test
    void schemaAwareQueryAcceptsKnownTable(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int, customer_name varchar(42));");
        CodeUnit unit = unit(temp, """
                public external CustomerSelect extends SQLQuery {
                select * from customer where cust_id = #[id]<int>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
    }

    @Test
    void schemaAwareQueryRejectsUnknownTable(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int);");
        CodeUnit unit = unit(temp, """
                public external Bad extends SQLQuery {
                select * from orders
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
        assertTrue(error.getMessage().contains("does not exist"));
    }

    @Test
    void schemaAwareQueryRejectsUnknownColumn(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int);");
        CodeUnit unit = unit(temp, """
                public external Bad extends SQLQuery {
                select missing from customer
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
        assertTrue(error.getMessage().contains("column"));
    }

    @Test
    void schemaAwareQueryRejectsJavaTypeIncompatibleWithColumn(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int, customer_name varchar(42));");
        CodeUnit unit = unit(temp, """
                public external Bad extends SQLQuery {
                select * from customer where cust_id = #[id]<String>
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
        assertTrue(error.getMessage().contains("type incompatibility"));
        assertTrue(error.getMessage().contains("cust_id"));
        assertTrue(error.getMessage().contains("String"));
    }

    @Test
    void schemaAwareQueryAcceptsMatchingColumnType(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int, customer_name varchar(42));");
        CodeUnit unit = unit(temp, """
                public external CustomerByName extends SQLQuery {
                select * from customer where customer_name = #[name]<String> and cust_id = #[id]<int>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
    }

    @Test
    void schemaAwareQueryRejectsIncompatibleInListElementType(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int, customer_name varchar(42));");
        CodeUnit unit = unit(temp, """
                public external Bad extends SQLQuery {
                select * from customer where customer_name in #[ids]<int[]>
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
        assertTrue(error.getMessage().contains("type incompatibility"));
        assertTrue(error.getMessage().contains("int[]"));
    }

    @Test
    void schemaAwareQueryAcceptsInListMatchingColumnType(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int, customer_name varchar(42));");
        CodeUnit unit = unit(temp, """
                public external CustomerIn extends SQLQuery {
                select * from customer where cust_id in #[ids]<int[]>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
    }

    @Test
    void schemaAwareUpdateAndInsertCheckAssignedColumnTypes(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table customer (cust_id int, customer_name varchar(42));");
        CodeUnit update = unit(temp, """
                public external Rename extends SQLQuery {
                update customer set customer_name = #[name]<int> where cust_id = #[id]<int>
                }
                """);
        ModuleException updateError = assertThrows(ModuleException.class,
                () -> new SQLModule().evaluate(update, Map.of(
                        "SQLMOD_NS_AWARE", "true",
                        "SQLMOD_NS_URI", schema.toUri().toString())));
        assertTrue(updateError.getMessage().contains("type incompatibility"));

        CodeUnit insert = unit(temp, """
                public external AddCustomer extends SQLQuery {
                insert into customer (cust_id, customer_name) values (#[id]<int>, #[name]<String>)
                }
                """);
        assertTrue(new SQLModule().evaluate(insert, Map.of(
                "SQLMOD_NS_AWARE", "true",
                "SQLMOD_NS_URI", schema.toUri().toString())));
    }

    @Test
    void liveJdbcExecutesSelect(@TempDir Path temp) throws Exception {
        String url = "jdbc:h2:mem:jmodlive;DB_CLOSE_DELAY=-1";
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("create table items (id int, name varchar(32))");
            statement.execute("insert into items values (1, 'x')");
        }
        CodeUnit unit = unit(temp, """
                public external ItemSelect extends SQLQuery {
                select * from items where id = #[id]<int>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_LIVE_TEST", "true",
                "SQLMOD_DB_URL", url,
                "SQLMOD_JDBC_DRIVER", "org.h2.Driver",
                "SQLMOD_DB_LOGIN", "sa",
                "SQLMOD_DB_PASSWORD", "")));
    }

    @Test
    void liveJdbcFailsWhenTableMissing(@TempDir Path temp) throws Exception {
        String url = "jdbc:h2:mem:jmodmissing;DB_CLOSE_DELAY=-1";
        CodeUnit unit = unit(temp, """
                public external Missing extends SQLQuery {
                select * from no_such_table
                }
                """);
        ModuleException error = assertThrows(ModuleException.class, () -> new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_LIVE_TEST", "true",
                "SQLMOD_DB_URL", url,
                "SQLMOD_JDBC_DRIVER", "org.h2.Driver",
                "SQLMOD_DB_LOGIN", "sa",
                "SQLMOD_DB_PASSWORD", "")));
        assertTrue(error.getMessage().toLowerCase().contains("live"));
    }

    @Test
    void liveJdbcExecutesInList(@TempDir Path temp) throws Exception {
        String url = "jdbc:h2:mem:jmodinlist;DB_CLOSE_DELAY=-1";
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("create table items (id int, name varchar(32))");
            statement.execute("insert into items values (1, 'x')");
        }
        CodeUnit unit = unit(temp, """
                public external ItemIn extends SQLQuery {
                select * from items where id in #[ids]<int[]>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of(
                "SQLMOD_LIVE_TEST", "true",
                "SQLMOD_DB_URL", url,
                "SQLMOD_JDBC_DRIVER", "org.h2.Driver",
                "SQLMOD_DB_LOGIN", "sa",
                "SQLMOD_DB_PASSWORD", "")));
    }

    @Test
    void rejectsInvalidLiveConfiguration() {
        SQLConfiguration configuration = new SQLConfiguration();
        Map<String, String> cfg = Map.of(
                "SQLMOD_NS_AWARE", "false",
                "SQLMOD_NS_URI", "",
                "SQLMOD_LIVE_TEST", "true",
                "SQLMOD_JDBC_DRIVER", "",
                "SQLMOD_DB_URL", "not-jdbc",
                "SQLMOD_DB_LOGIN", "",
                "SQLMOD_DB_PASSWORD", "");
        assertTrue(configuration.validationErrors(cfg).stream()
                .anyMatch(error -> error.contains("SQLMOD_DB_URL")));
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
