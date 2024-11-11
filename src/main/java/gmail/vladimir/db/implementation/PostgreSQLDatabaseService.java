package gmail.vladimir.db.implementation;

import gmail.vladimir.db.AbstractRelationalDatabaseService;

import java.sql.SQLException;

public class PostgreSQLDatabaseService extends AbstractRelationalDatabaseService {

    @Override
    protected void loadDriver() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }
}
