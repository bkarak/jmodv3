package org.jmod.dsl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class SqlInTest {
    @Test
    void expandsNamedMarkerToPlaceholders() throws Exception {
        assertEquals("select * from t where id in (?,?,?)",
                SqlIn.expand("select * from t where id in (#EXPAND:ids)", "ids", new int[] {1, 2, 3}));
    }

    @Test
    void leavesSqlWithoutMarkersUnchanged() throws Exception {
        assertEquals("select * from t where id = ?",
                SqlIn.expand("select * from t where id = ?", "ids", new int[] {1}));
    }

    @Test
    void rejectsEmptyOrNullArray() {
        assertThrows(SQLException.class,
                () -> SqlIn.expand("select * from t where id in (#EXPAND:ids)", "ids", new int[0]));
        assertThrows(SQLException.class,
                () -> SqlIn.expand("select * from t where id in (#EXPAND:ids)", "ids", null));
    }

    @Test
    void expandsTheSameNameTwice() throws Exception {
        assertEquals("a in (?,?) or b in (?,?)",
                SqlIn.expand("a in (#EXPAND:ids) or b in (#EXPAND:ids)", "ids", new String[] {"x", "y"}));
    }
}
