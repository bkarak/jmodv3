package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.jmod.dsl.module.ModuleException;
import org.jmod.symbol.Type;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SQLModuleTest {
    @Test
    void rejectsInvalidSql(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Bad extends SQLQuery<SQLConfiguration> {
                selct * frm t
                }
                """);
        assertThrows(ModuleException.class, () -> new SQLModule().evaluate(unit, Map.of()));
    }

    @Test
    void rejectsUnknownJavaType(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Bad extends SQLQuery<SQLConfiguration> {
                select * from t where id = #[id]<java.net.URI>
                }
                """);
        ModuleException error = assertThrows(ModuleException.class,
                () -> new SQLModule().evaluate(unit, Map.of()));
        assertTrue(error.getMessage().contains("unsupported Java type"));
        assertTrue(error.getMessage().contains("java.net.URI"));
        assertTrue(error.getMessage().contains("'id'"));
    }

    @Test
    void generatesPreparedStatement(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                package examples.simplesql;
                import org.jmod.dsl.sql.SQLQuery;
                public external SelectExample extends SQLQuery<SQLConfiguration> {
                select * from sqlexample where sqle_primary = #[prim]<int>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of()));
        String generated = Files.readString(temp.resolve("examples/simplesql/SelectExample.java"));
        assertTrue(generated.contains("class SelectExample extends SQLQuery<SQLConfiguration>"));
        assertTrue(generated.contains("prepareStatement"));
        assertTrue(generated.contains("setInt(_jmod_idx++, prim)"));
        assertTrue(generated.contains("public SelectExample(int prim)"));
        assertTrue(generated.contains("where sqle_primary = ?"));
    }

    @Test
    void generatesJdbcSettersForTemporalAndUrl(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Mixed extends SQLQuery<SQLConfiguration> {
                select * from t where d = #[d]<java.sql.Date>
                  and tm = #[tm]<java.sql.Time>
                  and u = #[u]<java.net.URL>
                  and n = #[n]<java.math.BigDecimal>
                }
                """);
        assertTrue(new SQLModule().evaluate(unit, Map.of()));
        String generated = Files.readString(temp.resolve("Mixed.java"));
        assertTrue(generated.contains("setDate(_jmod_idx++, d)"));
        assertTrue(generated.contains("setTime(_jmod_idx++, tm)"));
        assertTrue(generated.contains("setURL(_jmod_idx++, u)"));
        assertTrue(generated.contains("setBigDecimal(_jmod_idx++, n)"));
        assertTrue(generated.contains("java.sql.Date d"));
        assertTrue(generated.contains("java.net.URL u"));
    }

    @Nested
    class Metadata {
        @Test
        void describesSqlModule() {
            SQLModule module = new SQLModule();
            assertEquals("SQLModule", module.getName());
            assertEquals("1.0", module.getVersion());
            assertTrue(module.getDescription().contains("JDBC"));
            assertTrue(module.getAuthor().contains("Karakoidas"));
            assertEquals("SQLModule - " + module.getDescription(), module.toString());
            assertFalse(module.isDefaultModule());
        }

        @Test
        void exportsSqlQueryAndConfigurationTypes() {
            SQLModule module = new SQLModule();
            Type[] types = module.getExternalTypes();
            assertEquals(1, types.length);
            assertEquals("org.jmod.dsl.sql.SQLQuery", types[0].getQualifiedName());
            assertEquals("org.jmod.dsl.sql.SQLConfiguration",
                    module.getConfigurationType().getQualifiedName());
            assertInstanceOf(SQLTypeMapping.class, module.getTypeMap());
        }
    }

    @Nested
    class Syntax {
        @ParameterizedTest(name = "{0}")
        @MethodSource("org.jmod.dsl.sql.SQLModuleTest#validStatements")
        void acceptsValidSql(String name, String sql, @TempDir Path temp) throws Exception {
            String generated = evaluate(temp, "public external Q extends SQLQuery {\n" + sql + "\n}\n");
            assertTrue(generated.contains("prepareStatement"), name);
            assertTrue(generated.contains("this.sqlStatement = \""), name);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.jmod.dsl.sql.SQLModuleTest#invalidStatements")
        void rejectsInvalidSql(String name, String sql, @TempDir Path temp) throws Exception {
            CodeUnit unit = unit(temp, "public external Bad extends SQLQuery {\n" + sql + "\n}\n");
            ModuleException error = assertThrows(ModuleException.class,
                    () -> new SQLModule().evaluate(unit, Map.of()), name);
            assertTrue(error.getMessage().startsWith("invalid SQL:"), error.getMessage());
            assertNotNull(error.getCause());
        }

        @Test
        void emptyBodyIsAcceptedAsEmptyStatement(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, "public external Empty extends SQLQuery {\n}\n");
            assertTrue(generated.contains("this.sqlStatement = \"\""));
            assertTrue(generated.contains("public Empty()"));
        }
    }

    @Nested
    class Placeholders {
        @Test
        void parameterlessQueryHasEmptyConstructor(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external AllRows extends SQLQuery {
                    select * from items
                    }
                    """);
            assertTrue(generated.contains("public AllRows()"));
            assertTrue(generated.contains("this.sqlStatement = \"select * from items\""));
            assertFalse(generated.contains("private "));
            assertFalse(generated.contains("pstmnt.set"));
        }

        @Test
        void untypedPlaceholderDefaultsToString(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external ByName extends SQLQuery {
                    select * from users where nickname = #[nickname]
                    }
                    """);
            assertTrue(generated.contains("public ByName(String nickname)"));
            assertTrue(generated.contains("private String nickname"));
            assertTrue(generated.contains("setString(_jmod_idx++, nickname)"));
            assertTrue(generated.contains("where nickname = ?"));
        }

        @Test
        void repeatedPlaceholderBecomesOneConstructorArgument(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Range extends SQLQuery {
                    select * from t where min_val = #[bound]<int> or max_val = #[bound]<int>
                    }
                    """);
            assertTrue(generated.contains("public Range(int bound)"));
            assertTrue(generated.contains("setInt(_jmod_idx++, bound)"));
            assertEquals(2, generated.split("setInt\\(_jmod_idx\\+\\+, bound\\)", -1).length - 1);
            assertEquals(1, generated.split("private int bound", -1).length - 1);
        }

        @Test
        void indexesSettersInSourceOrder(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external MixedOrder extends SQLQuery {
                    insert into t (a, b, c) values (#[a]<int>, #[b]<String>, #[c]<long>)
                    }
                    """);
            assertTrue(generated.contains("public MixedOrder(int a, String b, long c)"));
            int a = generated.indexOf("setInt(_jmod_idx++, a)");
            int b = generated.indexOf("setString(_jmod_idx++, b)");
            int c = generated.indexOf("setLong(_jmod_idx++, c)");
            assertTrue(a >= 0 && a < b && b < c, generated);
        }

        @ParameterizedTest(name = "{0} uses {1}")
        @CsvSource({
                "boolean, setBoolean, boolean",
                "Boolean, setBoolean, Boolean",
                "byte, setByte, byte",
                "short, setShort, short",
                "int, setInt, int",
                "Integer, setInt, Integer",
                "long, setLong, long",
                "float, setFloat, float",
                "double, setDouble, double",
                "String, setString, String",
                "java.math.BigDecimal, setBigDecimal, java.math.BigDecimal",
                "byte[], setBytes, byte[]",
                "java.sql.Date, setDate, java.sql.Date",
                "java.sql.Time, setTime, java.sql.Time",
                "java.sql.Timestamp, setTimestamp, java.sql.Timestamp",
                "java.sql.Blob, setBlob, java.sql.Blob",
                "java.sql.Clob, setClob, java.sql.Clob",
                "java.sql.NClob, setNClob, java.sql.NClob",
                "java.sql.Array, setArray, java.sql.Array",
                "java.sql.Ref, setRef, java.sql.Ref",
                "java.sql.RowId, setRowId, java.sql.RowId",
                "java.sql.SQLXML, setSQLXML, java.sql.SQLXML",
                "java.net.URL, setURL, java.net.URL",
                "java.io.InputStream, setBinaryStream, java.io.InputStream",
                "java.io.Reader, setCharacterStream, java.io.Reader",
                "char, setObject, char",
                "Character, setObject, Character",
                "java.math.BigInteger, setObject, java.math.BigInteger",
                "java.time.LocalDate, setObject, java.time.LocalDate",
                "java.time.LocalTime, setObject, java.time.LocalTime",
                "java.time.LocalDateTime, setObject, java.time.LocalDateTime",
                "java.time.OffsetDateTime, setObject, java.time.OffsetDateTime",
                "java.util.UUID, setObject, java.util.UUID",
                "java.util.Calendar, setObject, java.util.Calendar",
                "java.sql.Struct, setObject, java.sql.Struct",
                "Object, setObject, Object"
        })
        void generatesSetterAndConstructorType(String javaType, String setter, String sourceType,
                @TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Typed extends SQLQuery {
                    select * from t where col = #[p]<%s>
                    }
                    """.formatted(javaType));
            assertTrue(generated.contains(setter + "(_jmod_idx++, p)"), generated);
            assertTrue(generated.contains("public Typed(" + sourceType + " p)"), generated);
            assertTrue(generated.contains("private " + sourceType + " p"), generated);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "java.net.URI",
                "java.util.List",
                "java.net.URI[]",
                "StringBuilder",
                "java.sql.Connection"
        })
        void rejectsUnsupportedPlaceholderType(String javaType, @TempDir Path temp) throws Exception {
            CodeUnit unit = unit(temp, """
                    public external Bad extends SQLQuery {
                    select * from t where col = #[p]<%s>
                    }
                    """.formatted(javaType));
            ModuleException error = assertThrows(ModuleException.class,
                    () -> new SQLModule().evaluate(unit, Map.of()));
            assertTrue(error.getMessage().contains("unsupported Java type"));
            assertTrue(error.getMessage().contains(javaType));
        }

        @Test
        void literalQuestionMarksAreNotBound(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external MixedMarks extends SQLQuery {
                    select * from t where id = ? and name = #[name]<String>
                    }
                    """);
            assertTrue(generated.contains("id = ? and name = ?"));
            assertTrue(generated.contains("setString(_jmod_idx++, name)"));
            assertFalse(generated.contains("setString(_jmod_idx++, 2"));
        }

        @Test
        void expandsJavaArrayInList(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external ByNames extends SQLQuery {
                    select * from users where nickname in #[names]<String[]>
                    }
                    """);
            assertTrue(generated.contains("public ByNames(String[] names)"));
            assertTrue(generated.contains("private String[] names"));
            assertTrue(generated.contains("where nickname in (#EXPAND:names)"));
            assertTrue(generated.contains("SqlIn.expand(this.sqlStatement, \"names\", names)"));
            assertTrue(generated.contains("for (int _jmod_names = 0; _jmod_names < names.length; _jmod_names++)"));
            assertTrue(generated.contains("setString(_jmod_idx++, names[_jmod_names])"));
            assertFalse(generated.contains("setString(_jmod_idx++, names)"));
        }

        @Test
        void expandsPrimitiveIntArrayInList(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external ByIds extends SQLQuery {
                    select * from t where id in #[ids]<int[]>
                    }
                    """);
            assertTrue(generated.contains("public ByIds(int[] ids)"));
            assertTrue(generated.contains("where id in (#EXPAND:ids)"));
            assertTrue(generated.contains("setInt(_jmod_idx++, ids[_jmod_ids])"));
        }

        @Test
        void byteArrayRemainsBlobParameter(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external ByBlob extends SQLQuery {
                    select * from t where payload = #[payload]<byte[]>
                    }
                    """);
            assertTrue(generated.contains("where payload = ?"));
            assertFalse(generated.contains("#EXPAND:"));
            assertTrue(generated.contains("setBytes(_jmod_idx++, payload)"));
        }
    }

    @Nested
    class Codegen {
        @Test
        void collapsesWhitespaceInGeneratedSqlLiteral(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Spaced extends SQLQuery {
                    select
                        *
                    from
                        items
                    where
                        id = #[id]<int>
                    }
                    """);
            assertTrue(generated.contains("this.sqlStatement = \"select * from items where id = ?\""));
        }

        @Test
        void escapesQuotesInSqlLiteral(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Quoted extends SQLQuery {
                    select * from t where name = 'O''Brien' and id = #[id]<int>
                    }
                    """);
            assertTrue(generated.contains("name = 'O''Brien' and id = ?")
                    || generated.contains("name = 'O\\'Brien' and id = ?")
                    || generated.contains("O''Brien"));
        }

        @Test
        void omitsPackageDeclarationForDefaultPackage(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external DefaultPkg extends SQLQuery {
                    select 1
                    }
                    """);
            assertFalse(generated.startsWith("package "));
            assertTrue(generated.contains("import org.jmod.dsl.sql.SQLQuery"));
            assertTrue(generated.contains("import java.sql.*"));
            assertTrue(generated.contains("import org.jmod.dsl.sql.SQLConfiguration"));
        }

        @Test
        void writesPackageDeclarationWhenPresent(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    package demo.sql;
                    public external Packaged extends SQLQuery {
                    select 1
                    }
                    """);
            assertTrue(generated.startsWith("package demo.sql;"));
            assertTrue(Files.exists(temp.resolve("demo/sql/Packaged.java")));
        }

        @Test
        void assignsFieldsFromConstructorParameters(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Assign extends SQLQuery {
                    select * from t where a = #[left]<int> and b = #[right]<String>
                    }
                    """);
            assertTrue(generated.contains("this.left = left"));
            assertTrue(generated.contains("this.right = right"));
            assertTrue(generated.contains("super(new org.jmod.dsl.sql.SQLConfiguration())"));
            assertTrue(generated.contains("String sql = SqlIn.expand(this.sqlStatement"));
            assertTrue(generated.contains("PreparedStatement pstmnt = c.prepareStatement(sql)"));
            assertTrue(generated.contains("return pstmnt"));
        }

        @Test
        void requiresSourceFile(@TempDir Path temp) throws Exception {
            CodeUnit unit = JmodParser.parse("""
                    public external MissingSource extends SQLQuery {
                    select 1
                    }
                    """);
            ModuleException error = assertThrows(ModuleException.class,
                    () -> new SQLModule().evaluate(unit, Map.of()));
            assertTrue(error.getMessage().contains("missing source file"));
            assertTrue(error.getMessage().contains("MissingSource"));
        }
    }

    @Nested
    class ConfigurationResolution {
        @Test
        void defaultsToSqlConfigurationWhenTypeArgumentOmitted(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Bare extends SQLQuery {
                    select 1
                    }
                    """);
            assertTrue(generated.contains("extends SQLQuery<SQLConfiguration>"));
            assertTrue(generated.contains("import org.jmod.dsl.sql.SQLConfiguration"));
            assertTrue(generated.contains("super(new org.jmod.dsl.sql.SQLConfiguration())"));
        }

        @Test
        void usesImportedConfigurationType(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    import org.jmod.dsl.sql.SQLQuery;
                    import com.acme.AppConf;
                    public external Imported extends SQLQuery<AppConf> {
                    select 1
                    }
                    """);
            assertTrue(generated.contains("import com.acme.AppConf"));
            assertTrue(generated.contains("extends SQLQuery<AppConf>"));
            assertTrue(generated.contains("super(new com.acme.AppConf())"));
        }

        @Test
        void usesFullyQualifiedConfigurationType(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Qualified extends SQLQuery<org.example.CustomConf> {
                    select 1
                    }
                    """);
            assertTrue(generated.contains("import org.example.CustomConf"));
            assertTrue(generated.contains("extends SQLQuery<CustomConf>"));
            assertTrue(generated.contains("super(new org.example.CustomConf())"));
        }

        @Test
        void qualifiesSimpleConfigurationWithPackageName(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    package demo.app;
                    public external LocalConfQuery extends SQLQuery<AppConf> {
                    select 1
                    }
                    """);
            assertTrue(generated.contains("import demo.app.AppConf"));
            assertTrue(generated.contains("extends SQLQuery<AppConf>"));
            assertTrue(generated.contains("super(new demo.app.AppConf())"));
        }

        @Test
        void defaultPackageSimpleNameFallsBackToBuiltinConfiguration(@TempDir Path temp) throws Exception {
            String generated = evaluate(temp, """
                    public external Builtin extends SQLQuery<SQLConfiguration> {
                    select 1
                    }
                    """);
            assertTrue(generated.contains("import org.jmod.dsl.sql.SQLConfiguration"));
            assertTrue(generated.contains("extends SQLQuery<SQLConfiguration>"));
        }
    }

    static Stream<Arguments> validStatements() {
        return Stream.of(
                Arguments.of("select", "select * from items"),
                Arguments.of("select distinct", "select distinct name from items"),
                Arguments.of("in list array", "select * from items where id in #[ids]<int[]>"),
                Arguments.of("subquery",
                        "select * from items where id in (select item_id from bids where amount > #[min]<int>)"),
                Arguments.of("group having",
                        "select category, count(*) from items group by category having count(*) > #[min]<int>"),
                Arguments.of("order limit", "select * from items order by id desc limit #[n]<int>"),
                Arguments.of("union",
                        "select id from a where n = #[n]<int> union select id from b where n = #[n]<int>"),
                Arguments.of("insert", "insert into t (a, b) values (#[a]<int>, #[b]<String>)"),
                Arguments.of("update", "update t set name = #[name]<String> where id = #[id]<long>"),
                Arguments.of("delete", "delete from t where id = #[id]<int>"),
                Arguments.of("quoted identifier", "select * from \"Items\" where \"Id\" = #[id]<int>"),
                Arguments.of("string literal", "select * from t where name = 'ok' and id = #[id]<int>"),
                Arguments.of("comment", "select * from t -- keep active rows\nwhere id = #[id]<int>"));
    }

    static Stream<Arguments> invalidStatements() {
        return Stream.of(
                Arguments.of("typo keywords", "selct * frm t"),
                Arguments.of("prose", "not sql at all"),
                Arguments.of("truncated from", "select * from"),
                Arguments.of("unclosed parenthesis", "select * from t where ("),
                Arguments.of("truncated insert", "insert into t"),
                Arguments.of("update without set", "update t set"));
    }

    private static String evaluate(Path temp, String source) throws Exception {
        CodeUnit unit = unit(temp, source);
        assertTrue(new SQLModule().evaluate(unit, Map.of()));
        return Files.readString(unit.getSourceFile().getCanonicalOutputFile().toPath());
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
