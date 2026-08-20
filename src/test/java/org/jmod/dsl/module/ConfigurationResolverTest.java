package org.jmod.dsl.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.symbol.Type;
import org.junit.jupiter.api.Test;

class ConfigurationResolverTest {
    @Test
    void omittedTypeUsesModuleDefault() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                public external Query extends SQLQuery {
                select 1
                }
                """);
        Type resolved = ConfigurationResolver.resolve(unit, new Type("org.jmod.dsl.sql", "SQLConfiguration"));
        assertEquals("org.jmod.dsl.sql.SQLConfiguration", resolved.getQualifiedName());
        assertFalse(ConfigurationResolver.isDeclared(unit));
    }

    @Test
    void importBeatsSamePackage() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                package demo;
                import com.acme.AppConf;
                public external Query extends SQLQuery<AppConf> {
                select 1
                }
                """);
        Type resolved = ConfigurationResolver.resolve(unit, new Type("org.jmod.dsl.sql", "SQLConfiguration"));
        assertEquals("com.acme.AppConf", resolved.getQualifiedName());
    }

    @Test
    void defaultPackageBuiltinSimpleName() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                public external Query extends SQLQuery<SQLConfiguration> {
                select 1
                }
                """);
        Type resolved = ConfigurationResolver.resolve(unit, new Type("org.jmod.dsl.sql", "SQLConfiguration"));
        assertEquals("org.jmod.dsl.sql.SQLConfiguration", resolved.getQualifiedName());
    }

    @Test
    void packagedSimpleNameStaysInPackage() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                package demo.app;
                public external Query extends SQLQuery<AppConf> {
                select 1
                }
                """);
        Type resolved = ConfigurationResolver.resolve(unit, new Type("org.jmod.dsl.sql", "SQLConfiguration"));
        assertEquals("demo.app.AppConf", resolved.getQualifiedName());
    }
}
