package gmail.vladimir.db;

public enum DatabaseType {
    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    SQLITE("SQLite"),
    MARIADB("MariaDB"),
    HYPERSQL("HyperSQL"),
    MONGODB("MongoDB");

    private final String displayName;

    DatabaseType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
