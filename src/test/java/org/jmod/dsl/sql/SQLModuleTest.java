package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.jmod.dsl.module.ModuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        assertTrue(generated.contains("setInt(1, prim)"));
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
        assertTrue(generated.contains("setDate(1, d)"));
        assertTrue(generated.contains("setTime(2, tm)"));
        assertTrue(generated.contains("setURL(3, u)"));
        assertTrue(generated.contains("setBigDecimal(4, n)"));
        assertTrue(generated.contains("java.sql.Date d"));
        assertTrue(generated.contains("java.net.URL u"));
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
