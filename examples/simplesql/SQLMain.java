package examples.simplesql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLMain {
    public PreparedStatement prepare(Connection connection, int primaryKey) throws SQLException {
        SelectExample query = new SelectExample(primaryKey);
        return query.getStatement(connection);
    }
}
