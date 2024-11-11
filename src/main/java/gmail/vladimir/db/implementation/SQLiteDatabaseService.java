package gmail.vladimir.db.implementation;

import gmail.vladimir.db.AbstractRelationalDatabaseService;

import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.Map;

public class SQLiteDatabaseService extends AbstractRelationalDatabaseService {

    private String dbFilePath;

    @Override
    protected void loadDriver() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found", e);
        }
    }

    @Override
    public void connect(Map<String, String> connectionParams) throws SQLException {
        dbFilePath = connectionParams.get("dbFilePath");
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
        loadDriver();
        logger.info("Connecting to SQLite database...");
        connection = DriverManager.getConnection(jdbcUrl);
        logger.info("Connected to SQLite database.");
    }

    @Override
    public String getDatabaseName() {
        return dbFilePath == null ? "null" : dbFilePath;
    }
}
