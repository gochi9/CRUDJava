package gmail.vladimir.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ISqlResultHandler {
    void handleResultSet(ResultSet rs) throws SQLException;
    void handleUpdateCount(int updateCount);
    void handleMessage(String message);
}

