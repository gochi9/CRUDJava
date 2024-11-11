package gmail.vladimir.db;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Used for similar databases that use mostly the same format, e.g., SQLite, MariaDB, etc.
public abstract class AbstractRelationalDatabaseService implements IDatabaseService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRelationalDatabaseService.class);
    protected volatile Connection connection;
    protected String jdbcUrl;
    protected String username;
    protected String password;
    protected String dbName;

    @Override
    public void connect(Map<String, String> connectionParams) throws SQLException {
        this.jdbcUrl = connectionParams.get("jdbcUrl");
        this.username = connectionParams.get("username");
        this.password = connectionParams.get("password");

        loadDriver();

        logger.info("Connecting to database...");
        connection = DriverManager.getConnection(jdbcUrl, username, password);
        logger.info("Connected to database.");
        this.dbName = connection.getCatalog();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { disconnect(); }
            catch (SQLException e) { logger.error(e.getMessage(), e); }
        }));
    }

    protected abstract void loadDriver() throws SQLException;

    @Override
    public void disconnect() throws SQLException {
        if (connection == null || connection.isClosed())
            return;

        connection.close();
        connection = null;
        logger.info("Disconnected from database.");
    }

    private static final String[] types = {"TABLE"};

    @Override
    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();

        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", types)) {
            while (rs.next())
                tables.add(rs.getString("TABLE_NAME"));
        }

        return tables;
    }

    @Override
    public Map<String, String> getTableSchema(String tableName) throws SQLException {
        Map<String, String> schema = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                if (columnSize > 0)
                    dataType += "(" + columnSize + ")";

                schema.put(columnName, dataType);
            }
        }
        return schema;
    }

    @Override
    public List<Map<String, Object>> getTableData(String tableName, int limit, int offset) throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = String.format("SELECT * FROM %s LIMIT ? OFFSET ?", tableName);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int columnCount = rsMeta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++)
                        row.put(rsMeta.getColumnName(i), rs.getObject(i));
                    data.add(row);
                }
            }
        }
        return data;
    }

    @Override
    public void createTable(String tableName, String tableSchema) throws SQLException {
        String sql;
        if (tableName == null && tableSchema != null)
            sql = tableSchema;

        else if (tableName != null && tableSchema != null)
            sql = String.format("CREATE TABLE %s (%s)", tableName, tableSchema);
        else
            throw new SQLException("Table name and schema cannot both be null.");

        try (Statement stmt = connection.createStatement())
            {stmt.executeUpdate(sql);}
    }

    @Override
    public String getPrimaryKeyColumn(String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
            if (rs.next())
                return rs.getString("COLUMN_NAME");
        }
        return null;
    }

    @Override
    public void deleteTable(String tableName) throws SQLException {
        String sql = String.format("DROP TABLE %s", tableName);
        try (Statement stmt = connection.createStatement())
            {stmt.executeUpdate(sql);}
    }

    @Override
    public void insertData(String tableName, Map<String, Object> data) throws SQLException {
        String columns = String.join(", ", data.keySet());
        String placeholders = String.join(", ", Collections.nCopies(data.size(), "?"));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setPreparedStatementParameters(stmt, data.values().toArray());
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateData(String tableName, Map<String, Object> data, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        String setClause = String.join(", ", data.keySet().stream().map(key -> key + " = ?").toArray(String[]::new));
        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", tableName, setClause, primaryKeyColumn);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Object[] params = appendToArray(data.values().toArray(), primaryKeyValue);
            setPreparedStatementParameters(stmt, params);
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteData(String tableName, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, primaryKeyColumn);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, primaryKeyValue);
            stmt.executeUpdate();
        }
    }

    @Override
    public int getTotalEntries(String tableName) throws SQLException {
        String query = String.format("SELECT COUNT(*) FROM %s", tableName);
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    @Override
    public int getNextId(String tableName, String primaryKeyColumn) throws SQLException {
        String sql = String.format("SELECT MAX(%s) FROM %s", primaryKeyColumn, tableName);
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if(!rs.next())
                return 1; //tble is empty

            int maxId = rs.getInt(1);
            if (rs.wasNull())
                return 1; //table is empty

            else
                return maxId + 1;
        }
    }

    @Override
    public boolean executeSqlCommand(String sql, ISqlResultHandler handler) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet)
                try (ResultSet rs = stmt.getResultSet()) {
                    handler.handleResultSet(rs);
                }

            else {
                int updateCount = stmt.getUpdateCount();
                handler.handleUpdateCount(updateCount);
            }
            return hasResultSet;
        }
    }

    @Override
    public int getTableEntryCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);

            else
                throw new SQLException("Unable to retrieve table entry count.");
        }
    }

    @Override
    public QueryResult getQueryResultFromResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        LinkedHashMap<String, String> columnTypes = new LinkedHashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            String columnTypeName = metaData.getColumnTypeName(i);
            columnTypes.put(columnName, columnTypeName);
        }

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (String columnName : columnTypes.keySet()) {
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }
            results.add(row);
        }
        return new QueryResult(results, columnTypes);
    }

    @Override
    public String getDatabaseName() {
        return dbName;
    }

    protected void setPreparedStatementParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++)
            stmt.setObject(i + 1, params[i]);
    }

    protected Object[] appendToArray(Object[] array, Object value) {
        Object[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[array.length] = value;
        return newArray;
    }
}