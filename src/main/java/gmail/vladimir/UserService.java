package gmail.vladimir;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    private final Connection connection;

    public UserService(String url, String user, String password) throws SQLException {
        this.connection = DatabaseConnection.getMySQLConnection(url, user, password);
    }

    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next())
                tables.add(rs.getString("TABLE_NAME"));
        }
        return tables;
    }

    public Map<String, String> getTableSchema(String tableName) throws SQLException {
        Map<String, String> schema = new LinkedHashMap<>();
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next())
                schema.put(rs.getString("COLUMN_NAME"), rs.getString("TYPE_NAME"));
        }
        return schema;
    }

    public List<Map<String, Object>> getTableData(String tableName) throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = String.format("SELECT * FROM `%s`", tableName);

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++)
                    row.put(rsMeta.getColumnName(i), rs.getObject(i));

                data.add(row);
            }
        }
        return data;
    }

    public void createTable(String tableName, String tableSchema) throws SQLException {
        executeUpdate(String.format("CREATE TABLE `%s` (%s)", tableName, tableSchema));
    }

    public void deleteTable(String tableName) throws SQLException {
        executeUpdate(String.format("DROP TABLE `%s`", tableName));
    }

    public void insertData(String tableName, Map<String, Object> data) throws SQLException {
        String columns = String.join(", ", data.keySet());
        String placeholders = String.join(", ", data.keySet().stream().map(key -> "?").toArray(String[]::new));
        String sql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", tableName, columns, placeholders);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setPreparedStatementParameters(stmt, data.values().toArray());
            stmt.executeUpdate();
        }
    }

    public void updateData(String tableName, Map<String, Object> data, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        String setClause = String.join(", ", data.keySet().stream().map(key -> key + " = ?").toArray(String[]::new));
        String sql = String.format("UPDATE `%s` SET %s WHERE `%s` = ?", tableName, setClause, primaryKeyColumn);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            Object[] params = appendToArray(data.values().toArray(), primaryKeyValue);
            setPreparedStatementParameters(stmt, params);
            stmt.executeUpdate();
        }
    }

    public void deleteData(String tableName, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(String.format("DELETE FROM `%s` WHERE `%s` = ?", tableName, primaryKeyColumn))) {
            stmt.setObject(1, primaryKeyValue);
            stmt.executeUpdate();
        }
    }

    public int getTotalEntries(String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed())
            connection.close();
    }

    private void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private void setPreparedStatementParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++)
            stmt.setObject(i + 1, params[i]);
    }

    private Object[] appendToArray(Object[] array, Object value) {
        Object[] newArray = new Object[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = value;
        return newArray;
    }
}
