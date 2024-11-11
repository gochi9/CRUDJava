package gmail.vladimir.db;

import gmail.vladimir.db.implementation.*;

public class DatabaseServiceFactory {

    public static IDatabaseService createDatabaseService(DatabaseType dbType) {
        switch (dbType) {
            case MYSQL:
            case MARIADB:
                return new MySQLDatabaseService();
            case POSTGRESQL:
                return new PostgreSQLDatabaseService();
            case SQLITE:
                return new SQLiteDatabaseService();
            case HYPERSQL:
                return new HyperSQLDatabaseService();
            case MONGODB:
                return new MongoDBDatabaseService();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
}
