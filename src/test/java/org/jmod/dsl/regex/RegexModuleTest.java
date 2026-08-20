package org.jmod.dsl.regex;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

class RegexModuleTest {
    @Test
    void rejectsInvalidPattern(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, "public external Bad extends Regex<RegexConfiguration> {\n[0-9\n}\n");
        RegexModule module = new RegexModule();
        assertThrows(ModuleException.class, () -> module.evaluate(unit, Map.of("REGEX_ENGINE", "jdk")));
    }

    @Test
    void rejectsUnknownEngine(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, "public external Ok extends Regex<RegexConfiguration> {\na+\n}\n");
        RegexModule module = new RegexModule();
        ModuleException error = assertThrows(ModuleException.class,
                () -> module.evaluate(unit, Map.of("REGEX_ENGINE", "pcre")));
        assertTrue(error.getMessage().contains("unsupported regex engine"));
    }

    @Test
    void generatesJavaClass(@TempDir Path temp) throws Exception {
        CodeUnit unit = unit(temp, """
                package p;
                import org.jmod.dsl.regex.Regex;
                import org.jmod.dsl.regex.RegexConfiguration;
                public external NumberRegex extends Regex<RegexConfiguration> {
                [0-9]+
                }
                """);
        assertTrue(new RegexModule().evaluate(unit, Map.of("REGEX_ENGINE", "jdk")));
        String generated = Files.readString(temp.resolve("p/NumberRegex.java"));
        assertTrue(generated.contains("class NumberRegex extends Regex<RegexConfiguration>"));
        assertTrue(generated.contains("static final String _regex"));
        assertTrue(generated.contains("[0-9]+"));
        assertFalse(generated.contains("external"));
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
