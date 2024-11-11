package gmail.vladimir.managers;

import gmail.vladimir.db.IDatabaseService;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MongoTableManager {
    private final IDatabaseService dbService;
    private final String tableName;
    private final String primaryKeyColumn = "_id";

    public MongoTableManager(IDatabaseService dbService, String tableName) {
        this.dbService = dbService;
        this.tableName = tableName;
    }

    public List<Map<String, Object>> getSortedData(String selectedSort) throws SQLException {
        List<Map<String, Object>> tableData = dbService.getTableData(tableName, Integer.MAX_VALUE, 0);

        if (selectedSort != null && !"ALL".equals(selectedSort)) {
            if (selectedSort.startsWith("Field: ")) {
                String fieldToSort = selectedSort.substring("Field: ".length());
                tableData = tableData.stream()
                        .filter(row -> row.containsKey(fieldToSort))
                        .sorted(Comparator.comparing(row -> {
                            Object val = row.get(fieldToSort);
                            return val != null ? val.toString() : "";
                        }))
                        .collect(Collectors.toList());
            } else if (selectedSort.startsWith("Fields: ")) {
                String fieldsCombination = selectedSort.substring("Fields: ".length());
                Set<String> fieldsSet = new HashSet<>(Arrays.asList(fieldsCombination.split(", ")));
                tableData = tableData.stream()
                        .filter(row -> {
                            Set<String> rowFields = row.keySet().stream()
                                    .filter(field -> !field.equals(primaryKeyColumn))
                                    .collect(Collectors.toSet());
                            return rowFields.equals(fieldsSet);
                        })
                        .collect(Collectors.toList());
            }
        }

        return tableData;
    }

    public Set<String> getAllFieldNames() throws SQLException {
        List<Map<String, Object>> tableData = dbService.getTableData(tableName, Integer.MAX_VALUE, 0);
        Set<String> allFields = new LinkedHashSet<>();
        for (Map<String, Object> row : tableData) {
            allFields.addAll(row.keySet());
        }
        return allFields;
    }

    public void deleteEntry(Object primaryKeyValue) throws SQLException {
        dbService.deleteData(tableName, primaryKeyColumn, primaryKeyValue);
    }

    public void saveEntry(Map<String, Object> entryData, Map<String, Object> existingData) throws SQLException {
        if (existingData == null) {
            dbService.insertData(tableName, entryData);
        } else {
            dbService.updateData(tableName, entryData, primaryKeyColumn, existingData.get(primaryKeyColumn));
        }
    }

    public List<Map<String, Object>> getTableData() throws SQLException {
        return dbService.getTableData(tableName, Integer.MAX_VALUE, 0);
    }

    public void massDeleteEntries(String selectedField, String condition) throws SQLException {
        List<Map<String, Object>> tableData = getTableData();

        for (Map<String, Object> row : tableData) {
            if (selectedField == null || "ALL".equals(selectedField) || row.containsKey(selectedField)) {
                if (condition.isEmpty() || (row.get(selectedField) != null && row.get(selectedField).toString().equals(condition))) {
                    deleteEntry(row.get(primaryKeyColumn));
                }
            }
        }
    }

    public void massAddField(String newFieldName, String prefillValue) throws SQLException {
        List<Map<String, Object>> tableData = getTableData();

        for (Map<String, Object> row : tableData) {
            if (!row.containsKey(newFieldName)) {
                row.put(newFieldName, prefillValue);
                dbService.updateData(tableName, row, primaryKeyColumn, row.get(primaryKeyColumn));
            }
        }
    }

    public void massModifyField(String selectedField, String newValue, boolean onlyIfEmpty) throws SQLException {
        List<Map<String, Object>> tableData = getTableData();

        for (Map<String, Object> row : tableData) {
            if (selectedField == null || "ALL".equals(selectedField)) {
                for (String field : row.keySet()) {
                    if (field.equals(primaryKeyColumn)) continue;
                    if (onlyIfEmpty && row.get(field) != null && !row.get(field).toString().isEmpty())
                        continue;
                    row.put(field, newValue);
                }
            } else {
                if (row.containsKey(selectedField)) {
                    if (onlyIfEmpty && row.get(selectedField) != null && !row.get(selectedField).toString().isEmpty())
                        continue;
                    row.put(selectedField, newValue);
                }
            }
            dbService.updateData(tableName, row, primaryKeyColumn, row.get(primaryKeyColumn));
        }
    }
}