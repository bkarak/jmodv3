package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmod.JmodPortsFixtures;
import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.ExternalRef;
import org.jmod.compiler.source.ExternalRefs;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SQLModulePortsTest {
    @Test
    void vendorsHistoricalSqlPorts() throws Exception {
        assertTrue(JmodPortsFixtures.sqlModuleFiles().size() >= 50,
                "expected the vendored jmod-ports SQL queries");
        assertTrue(JmodPortsFixtures.sqlModuleFiles().stream()
                .anyMatch(path -> JmodPortsFixtures.relative(path).contains("address-book")));
        assertTrue(JmodPortsFixtures.sqlModuleFiles().stream()
                .anyMatch(path -> JmodPortsFixtures.relative(path).contains("RUBiS")));
        assertTrue(JmodPortsFixtures.sqlModuleFiles().stream()
                .anyMatch(path -> JmodPortsFixtures.relative(path).contains("examj")));
        assertTrue(JmodPortsFixtures.sqlModuleFiles().stream()
                .anyMatch(path -> JmodPortsFixtures.relative(path).contains("jcrontab")));
        assertTrue(JmodPortsFixtures.sqlModuleFiles().stream()
                .anyMatch(path -> JmodPortsFixtures.relative(path).contains("sdriver")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.jmod.JmodPortsFixtures#sqlModuleArguments")
    void evaluatesEachSqlPort(String relative, Path jmod, @TempDir Path temp) throws Exception {
        CodeUnit unit = parse(jmod, temp);
        assertTrue(new SQLModule().evaluate(unit, Map.of()), relative);
        Path generatedFile = unit.getSourceFile().getCanonicalOutputFile().toPath();
        assertTrue(Files.exists(generatedFile), relative);
        String generated = Files.readString(generatedFile);

        assertTrue(generated.contains("class " + unit.getExternalTypeName() + " extends SQLQuery"), relative);
        assertTrue(generated.contains("prepareStatement"), relative);
        assertTrue(generated.contains("this.sqlStatement = \""), relative);
        assertFalse(generated.contains("external"), relative);
        assertFalse(generated.contains("#["), relative);

        SQLTypeMapping mapping = new SQLTypeMapping();
        int index = 1;
        for (ExternalRef occurrence : unit.getExternalReferences()) {
            String setter = mapping.setterFor(occurrence.getType()) + "(" + index + ", "
                    + occurrence.getName() + ")";
            assertTrue(generated.contains(setter), relative + " missing " + setter + "\n" + generated);
            index++;
        }
        for (ExternalRef param : unit.getUniqueParameters()) {
            String javaType = ExternalRefs.toJavaSourceType(param.getType());
            assertTrue(generated.contains("private " + javaType + " " + param.getName()), relative);
            assertTrue(generated.contains("this." + param.getName() + " = " + param.getName()), relative);
        }
    }

    private static CodeUnit parse(Path jmod, Path temp) throws Exception {
        CodeUnit unit = JmodParser.parse(jmod.toFile());
        SourceFile sourceFile = new SourceFile(jmod.toFile(), true);
        sourceFile.setOutputDir(temp.toFile());
        sourceFile.setPackageName(unit.getPackageName());
        sourceFile.setTypeName(unit.getExternalTypeName());
        unit.setSourceFile(sourceFile);
        return unit;
    }
}
