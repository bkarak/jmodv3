package org.jmod.compiler.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExternalRefTest {
    @Test
    void extractsQualifiedAndArrayTypes() throws Exception {
        List<ExternalRef> refs = ExternalRefs.extractAll(
                "#[name]<java.lang.String> and #[ids]<int[]>");
        assertEquals(2, refs.size());
        assertEquals("java.lang.String", refs.get(0).getType());
        assertEquals("int[]", refs.get(1).getType());
        List<ExternalRef> unique = ExternalRefs.uniqueParameters(refs);
        assertEquals(2, unique.size());
    }

    @Test
    void reusesSameNameAndTypeAsOneParameter() throws Exception {
        List<ExternalRef> all = ExternalRefs.extractAll(
                "tax = #[tax]<int> or tax = #[tax]<int> + 1");
        assertEquals(2, all.size());
        List<ExternalRef> unique = ExternalRefs.uniqueParameters(all);
        assertEquals(1, unique.size());
        assertEquals("tax", unique.get(0).getName());
        assertEquals("where ? or tax = ? + 1",
                ExternalRefs.replaceWithPlaceholders("where #[tax]<int> or tax = #[tax]<int> + 1"));
    }

    @Test
    void rejectsConflictingTypesForTheSameName() {
        List<ExternalRef> all = ExternalRefs.extractAll("#[tax]<int> #[tax]<java.lang.String>");
        assertThrows(ParseException.class, () -> ExternalRefs.uniqueParameters(all));
    }

    @Test
    void defaultsUntypedReferenceToString() throws Exception {
        List<ExternalRef> refs = ExternalRefs.extractAll("WHERE ID = #[id]");
        assertEquals(1, refs.size());
        assertEquals("id", refs.get(0).getName());
        assertEquals("java.lang.String", refs.get(0).getType());
        assertEquals("WHERE ID = ?", ExternalRefs.replaceWithPlaceholders("WHERE ID = #[id]"));
    }

    @Test
    void replacesInArrayWithGroupedPlaceholderAndExpandMarker() {
        assertEquals("where id in (?)",
                ExternalRefs.replaceWithPlaceholders("where id in #[ids]<int[]>"));
        assertEquals("where id in (#EXPAND:ids)",
                ExternalRefs.replaceWithCodegenSql("where id in #[ids]<int[]>"));
        assertEquals("where blob = ?",
                ExternalRefs.replaceWithPlaceholders("where blob = #[blob]<byte[]>"));
        assertEquals("where blob = ?",
                ExternalRefs.replaceWithCodegenSql("where blob = #[blob]<byte[]>"));
    }
}
