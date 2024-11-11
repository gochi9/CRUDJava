package gmail.vladimir.managers;

import gmail.vladimir.db.DatabaseType;

import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {

    public String buildJdbcUrl(DatabaseType dbType, String host, String port, String dbName, String extraParams) {
        String url;
        switch (dbType) {
            case MYSQL:
            case MARIADB:
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
                break;
            case POSTGRESQL:
                url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                break;
            case HYPERSQL:
                url = "jdbc:hsqldb:hsql://" + host + ":" + port + "/" + dbName;
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type for JDBC URL: " + dbType);
        }

        if (!(extraParams != null && !extraParams.trim().isEmpty()))
            return url;

        if (!extraParams.startsWith("?") && !extraParams.startsWith("&"))
            extraParams = "?" + extraParams;

        url += extraParams;

        return url;
    }

    public Map<String, String> getConnectionParams(DatabaseType dbType, Map<String, String> inputValues) {
        Map<String, String> connectionParams = new HashMap<>();
        switch (dbType) {
            case MYSQL:
            case POSTGRESQL:
            case MARIADB:
            case HYPERSQL:
                String host = inputValues.get("host");
                String port = inputValues.get("port");
                String dbName = inputValues.get("dbName");
                String username = inputValues.get("username");
                String password = inputValues.get("password");
                String extraParams = inputValues.get("extraParams");

                String jdbcUrl = buildJdbcUrl(dbType, host, port, dbName, extraParams);
                connectionParams.put("jdbcUrl", jdbcUrl);
                connectionParams.put("username", username);
                connectionParams.put("password", password);
                break;
            case SQLITE:
                String dbFilePath = inputValues.get("dbFilePath");
                connectionParams.put("dbFilePath", dbFilePath);
                break;
            case MONGODB:
                String uri = inputValues.get("uri");
                String mongoDbName = inputValues.get("dbName");
                connectionParams.put("uri", uri);
                connectionParams.put("dbName", mongoDbName);
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        return connectionParams;
    }
}