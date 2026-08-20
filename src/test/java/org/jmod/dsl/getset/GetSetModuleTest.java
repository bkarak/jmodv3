package org.jmod.dsl.getset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GetSetModuleTest {
    @Test
    void generatesGettersAndSetters(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                package beans;
                public external Person extends GetSetType<GetSetConfiguration> {
                #[name]<String> #[age]<int>
                }
                """);
        assertTrue(new GetSetModule().evaluate(unit, Map.of(
                "GS_GEN_GETTER", "true",
                "GS_GEN_SETTER", "true")));
        String generated = Files.readString(temp.resolve("beans/Person.java"));
        assertTrue(generated.contains("class Person extends GetSetType<GetSetConfiguration>"));
        assertTrue(generated.contains("private String name"));
        assertTrue(generated.contains("private int age"));
        assertTrue(generated.contains("public String getName()"));
        assertTrue(generated.contains("public int getAge()"));
        assertTrue(generated.contains("public void setName(String v)"));
        assertTrue(generated.contains("public void setAge(int v)"));
        assertFalse(generated.contains("external"));
    }

    @Test
    void canOmitSetters(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                public external Flag extends GetSetType {
                #[on]<boolean>
                }
                """);
        assertTrue(new GetSetModule().evaluate(unit, Map.of(
                "GS_GEN_GETTER", "true",
                "GS_GEN_SETTER", "false")));
        String generated = Files.readString(temp.resolve("Flag.java"));
        assertTrue(generated.contains("getOn()"));
        assertFalse(generated.contains("setOn("));
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
