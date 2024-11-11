package gmail.vladimir.db;

//import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

//@Service
public interface IDatabaseService {

    void connect(Map<String, String> connectionParams) throws SQLException;

    void disconnect() throws SQLException;

    List<String> listTables() throws SQLException;

    Map<String, String> getTableSchema(String tableName) throws SQLException;

    List<Map<String, Object>> getTableData(String tableName, int limit, int offset) throws SQLException;

    void createTable(String tableName, String tableSchema) throws SQLException;

    void deleteTable(String tableName) throws SQLException;

    void insertData(String tableName, Map<String, Object> data) throws SQLException;

    void updateData(String tableName, Map<String, Object> data, String primaryKeyColumn, Object primaryKeyValue) throws SQLException;

    void deleteData(String tableName, String primaryKeyColumn, Object primaryKeyValue) throws SQLException;

    int getTotalEntries(String tableName) throws SQLException;

    String getPrimaryKeyColumn(String tableName) throws SQLException;

    int getNextId(String tableName, String primaryKeyColumn) throws SQLException;

    boolean executeSqlCommand(String sql, ISqlResultHandler handler) throws SQLException;

    int getTableEntryCount(String tableName) throws SQLException;

    QueryResult getQueryResultFromResultSet(ResultSet rs) throws SQLException;

    String getDatabaseName();
}
