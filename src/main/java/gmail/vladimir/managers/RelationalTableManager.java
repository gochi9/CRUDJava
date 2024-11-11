package gmail.vladimir.managers;

import gmail.vladimir.db.IDatabaseService;
import gmail.vladimir.db.ISqlResultHandler;
import gmail.vladimir.db.QueryResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RelationalTableManager {
    private final IDatabaseService dbService;
    private final String tableName;
    private final String primaryKeyColumn;
    private Map<String, String> tableSchema;
    private final Set<String> numericDataTypes;

    public RelationalTableManager(IDatabaseService dbService, String tableName) throws SQLException {
        this.dbService = dbService;
        this.tableName = tableName;
        this.tableSchema = dbService.getTableSchema(tableName);
        if (this.tableSchema == null) {
            this.tableSchema = Collections.emptyMap();
        }
        this.primaryKeyColumn = dbService.getPrimaryKeyColumn(tableName);
        this.numericDataTypes = new HashSet<>(Arrays.asList(
                "INT", "INTEGER", "SMALLINT", "BIGINT", "DOUBLE", "FLOAT",
                "REAL", "DECIMAL", "NUMERIC"
        ));
    }

    public Map<String, String> getTableSchema() {
        return tableSchema;
    }

    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public Set<String> getNumericDataTypes() {
        return numericDataTypes;
    }

    public int getTotalEntries() throws SQLException {
        return dbService.getTableEntryCount(tableName);
    }

    public List<Map<String, Object>> getTableData(int entriesPerPage, int offset) throws SQLException {
        return dbService.getTableData(tableName, entriesPerPage, offset);
    }

    public void deleteEntry(Object primaryKeyValue) throws SQLException {
        dbService.deleteData(tableName, primaryKeyColumn, primaryKeyValue);
    }

    public void saveEntry(Map<String, Object> newData, Map<String, Object> existingData) throws SQLException {
        if (existingData == null) {
            dbService.insertData(tableName, newData);
        } else {
            dbService.updateData(tableName, newData, primaryKeyColumn, existingData.get(primaryKeyColumn));
        }
    }

    public int getNextPrimaryKeyValue() throws SQLException {
        return dbService.getNextId(tableName, primaryKeyColumn);
    }

    public boolean executeSqlCommand(String sqlCommand, ISqlResultHandler handler) throws SQLException {
        return dbService.executeSqlCommand(sqlCommand, handler);
    }

    public QueryResult getQueryResultFromResultSet(ResultSet rs) throws SQLException {
        return dbService.getQueryResultFromResultSet(rs);
    }
}

