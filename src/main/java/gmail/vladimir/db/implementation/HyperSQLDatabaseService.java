package gmail.vladimir.db.implementation;

import gmail.vladimir.db.AbstractRelationalDatabaseService;

import java.sql.SQLException;

public class HyperSQLDatabaseService extends AbstractRelationalDatabaseService {

    @Override
    protected void loadDriver() throws SQLException {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        }
        catch (ClassNotFoundException e) {
            throw new SQLException("HyperSQL JDBC Driver not found", e);
        }
    }
}
