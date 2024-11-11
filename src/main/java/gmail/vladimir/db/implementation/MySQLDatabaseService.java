package gmail.vladimir.db.implementation;

import gmail.vladimir.db.AbstractRelationalDatabaseService;

import java.sql.SQLException;

public class MySQLDatabaseService extends AbstractRelationalDatabaseService {

    @Override
    protected void loadDriver() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }
}
