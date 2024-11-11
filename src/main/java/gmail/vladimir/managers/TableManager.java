package gmail.vladimir.managers;

import gmail.vladimir.db.IDatabaseService;

import java.sql.SQLException;
import java.util.List;

public class TableManager {

    private final IDatabaseService dbService;

    public TableManager(IDatabaseService dbService) {
        this.dbService = dbService;
    }

    public void createTable(String tableName, String tableSchema) throws SQLException {
        dbService.createTable(tableName, tableSchema);
    }

    public List<String> listTables() throws SQLException {
        return dbService.listTables();
    }

    public void deleteTable(String tableName) throws SQLException {
        dbService.deleteTable(tableName);
    }
}

