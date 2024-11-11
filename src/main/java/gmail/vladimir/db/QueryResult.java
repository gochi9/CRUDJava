package gmail.vladimir.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryResult {
    private List<Map<String, Object>> data;
    private LinkedHashMap<String, String> columnTypes;

    public QueryResult(List<Map<String, Object>> data, LinkedHashMap<String, String> columnTypes) {
        this.data = data;
        this.columnTypes = columnTypes;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public LinkedHashMap<String, String> getColumnTypes() {
        return columnTypes;
    }
}
