package org.jmod.compiler.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JmodParserTest {
    @Test
    void parsesRegexExternalWithNestedBraces() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                package examples.simpleregex;
                import org.jmod.dsl.regex.Regex;
                import org.jmod.dsl.regex.RegexConfiguration;
                public external IpAddress extends Regex<RegexConfiguration> {
                ([0-9]{1,3}\\\\.){3}[0-9]{1,3}
                }
                """);
        assertEquals("examples.simpleregex", unit.getPackageName());
        assertEquals("IpAddress", unit.getExternalTypeName());
        assertEquals("Regex", unit.getBaseTypeName());
        assertEquals("RegexConfiguration", unit.getConfigurationTypeName());
        assertTrue(unit.getDslBody().contains("{1,3}"));
        assertTrue(unit.getImports().contains("org.jmod.dsl.regex.Regex"));
        assertEquals(1, unit.getModifiers().size());
        assertEquals("public", unit.getModifiers().get(0));
    }

    @Test
    void parsesSqlExternalReferences() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                package examples.simplesql;
                import org.jmod.dsl.sql.SQLQuery;
                public external SelectExample extends SQLQuery<SimpleConf> {
                select * from t where id = #[id]<int>
                }
                """);
        assertEquals(1, unit.getUniqueParameters().size());
        assertEquals("id", unit.getUniqueParameters().get(0).getName());
        assertEquals("int", unit.getUniqueParameters().get(0).getType());
    }

    @Test
    void allowsOmittedConfigurationTypeArgument() throws Exception {
        CodeUnit unit = JmodParser.parse("""
                import org.jmod.dsl.sql.SQLQuery;
                public external AuthQuery extends SQLQuery {
                SELECT id FROM users WHERE nickname = #[nickname]<java.lang.String>
                }
                """);
        assertEquals("SQLQuery", unit.getBaseTypeName());
        assertEquals(null, unit.getConfigurationTypeName());
        assertEquals(1, unit.getUniqueParameters().size());
    }

    @Test
    void rejectsIllegalModifier() {
        ParseException error = assertThrows(ParseException.class, () -> JmodParser.parse("""
                private external Bad extends Regex<RegexConfiguration> {
                a+
                }
                """));
        assertTrue(error.getMessage().contains("illegal modifier"));
    }

    @Test
    void rejectsUnclosedBody() {
        assertThrows(ParseException.class, () -> JmodParser.parse("""
                public external Bad extends Regex<RegexConfiguration> {
                a+
                """));
    }
}
