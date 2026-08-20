package org.jmod.compiler.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jmod.dsl.module.ModuleException;
import org.jmod.dsl.sql.SQLModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {
    @Test
    void overlaysUserConfigurationFromSource(@TempDir Path temp) throws Exception {
        Path schema = temp.resolve("schema.sql");
        Files.writeString(schema, "create table t (id int);");
        Path conf = temp.resolve("SimpleConf.java");
        Files.writeString(conf, """
                package demo;
                import org.jmod.dsl.sql.SQLConfiguration;
                public class SimpleConf extends SQLConfiguration {
                    public boolean SQLMOD_NS_AWARE = true; // enable schema
                    public String SQLMOD_NS_URI = "file://./schema.sql";
                }
                """);
        Path jmod = temp.resolve("Query.jmod");
        Files.writeString(jmod, """
                package demo;
                import org.jmod.dsl.sql.SQLQuery;
                public external Query extends SQLQuery<SimpleConf> {
                select * from t
                }
                """);
        CodeUnit unit = JmodParser.parse(jmod.toFile());
        unit.setSourceFile(new SourceFile(jmod.toFile(), true));
        Map<String, String> cfg = ConfigLoader.load(unit, new SQLModule(),
                List.of(new SourceFile(conf.toFile(), false)));
        assertEquals("true", cfg.get("SQLMOD_NS_AWARE"));
        assertTrue(cfg.get("SQLMOD_NS_URI").endsWith("schema.sql"));
        assertTrue(Files.isRegularFile(Path.of(cfg.get("SQLMOD_NS_URI"))));
        assertEquals("false", cfg.get("SQLMOD_LIVE_TEST"));
    }

    @Test
    void walksConfigurationInheritanceInSource(@TempDir Path temp) throws Exception {
        Path parent = temp.resolve("AppConf.java");
        Files.writeString(parent, """
                package demo;
                import org.jmod.dsl.sql.SQLConfiguration;
                public class AppConf extends SQLConfiguration {
                    public boolean SQLMOD_LIVE_TEST = true;
                    public String SQLMOD_JDBC_DRIVER = "org.h2.Driver";
                    public String SQLMOD_DB_URL = "jdbc:h2:mem:cfg";
                    public String SQLMOD_DB_LOGIN = "sa";
                }
                """);
        Path child = temp.resolve("LocalConf.java");
        Files.writeString(child, """
                package demo;
                public class LocalConf extends AppConf {
                    public boolean SQLMOD_NS_AWARE = true;
                }
                """);
        Path jmod = temp.resolve("Query.jmod");
        Files.writeString(jmod, """
                package demo;
                public external Query extends SQLQuery<LocalConf> {
                select 1
                }
                """);
        CodeUnit unit = JmodParser.parse(jmod.toFile());
        Map<String, String> cfg = ConfigLoader.load(unit, new SQLModule(), List.of(
                new SourceFile(parent.toFile(), false),
                new SourceFile(child.toFile(), false)));
        assertEquals("true", cfg.get("SQLMOD_NS_AWARE"));
        assertEquals("true", cfg.get("SQLMOD_LIVE_TEST"));
        assertEquals("org.h2.Driver", cfg.get("SQLMOD_JDBC_DRIVER"));
        assertEquals("jdbc:h2:mem:cfg", cfg.get("SQLMOD_DB_URL"));
    }

    @Test
    void usesClasspathForBuiltInConfiguration() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                import org.jmod.dsl.sql.SQLQuery;
                import org.jmod.dsl.sql.SQLConfiguration;
                public external Query extends SQLQuery<SQLConfiguration> {
                select 1
                }
                """);
        Map<String, String> cfg = ConfigLoader.load(unit, new SQLModule(), List.of());
        assertEquals("false", cfg.get("SQLMOD_NS_AWARE"));
        assertEquals("", cfg.get("SQLMOD_NS_URI"));
    }

    @Test
    void omittedConfigurationTypeKeepsModuleDefaults() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                import org.jmod.dsl.sql.SQLQuery;
                public external Query extends SQLQuery {
                select 1
                }
                """);
        Map<String, String> cfg = ConfigLoader.load(unit, new SQLModule(), List.of());
        assertEquals(new SQLModule().getDefaultConfiguration(), cfg);
    }

    @Test
    void failsWhenDeclaredConfigurationCannotBeLoaded() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                package demo;
                public external Query extends SQLQuery<MissingConf> {
                select 1
                }
                """);
        ModuleException error = assertThrows(ModuleException.class,
                () -> ConfigLoader.load(unit, new SQLModule(), List.of()));
        assertTrue(error.getMessage().contains("MissingConf"));
        assertTrue(error.getMessage().contains("cannot load configuration type"));
    }
}
