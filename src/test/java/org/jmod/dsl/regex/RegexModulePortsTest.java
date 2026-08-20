package org.jmod.dsl.regex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jmod.JmodPortsFixtures;
import org.jmod.compiler.source.CodeUnit;
import org.jmod.compiler.source.JmodParser;
import org.jmod.compiler.source.SourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RegexModulePortsTest {
    @Test
    void vendorsHistoricalRegexPorts() throws Exception {
        assertTrue(JmodPortsFixtures.regexModuleFiles().size() >= 4,
                "expected the vendored sdriver regex ports");
        assertTrue(JmodPortsFixtures.regexModuleFiles().stream()
                .allMatch(path -> JmodPortsFixtures.relative(path).contains("sdriver")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.jmod.JmodPortsFixtures#regexModuleArguments")
    void evaluatesEachRegexPort(String relative, Path jmod, @TempDir Path temp) throws Exception {
        CodeUnit unit = parse(jmod, temp);
        assertTrue(new RegexModule().evaluate(unit, Map.of("REGEX_ENGINE", "jdk")), relative);
        Path generatedFile = unit.getSourceFile().getCanonicalOutputFile().toPath();
        assertTrue(Files.exists(generatedFile), relative);
        String generated = Files.readString(generatedFile);

        assertTrue(generated.contains("class " + unit.getExternalTypeName() + " extends Regex"), relative);
        assertTrue(generated.contains("static final String _regex"), relative);
        assertTrue(generated.contains("super(_regex, new"), relative);
        assertFalse(generated.contains("external"), relative);
        assertFalse(unit.getDslBody().isBlank());
        assertTrue(generated.contains(unit.getDslBody().trim())
                || generated.contains(unit.getDslBody().trim().replace("\\", "\\\\")), relative);
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
