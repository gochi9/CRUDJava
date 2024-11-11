package gmail.vladimir.db.implementation;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import gmail.vladimir.db.IDatabaseService;
import gmail.vladimir.db.ISqlResultHandler;
import gmail.vladimir.db.QueryResult;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class MongoDBDatabaseService implements IDatabaseService {

    protected static final Logger logger = LoggerFactory.getLogger(MongoDBDatabaseService.class);
    private volatile MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private String dbName;

    @Override
    public void connect(Map<String, String> connectionParams) throws SQLException {
        String uri = connectionParams.get("uri");
        dbName = connectionParams.get("dbName");

        try {
            mongoClient = MongoClients.create(uri);
            mongoDatabase = mongoClient.getDatabase(dbName);
        }
        catch (Exception e) {
            throw new SQLException("Failed to connect to MongoDB", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));
    }

    @Override
    public void disconnect() {
        if (mongoClient == null)
            return;

        mongoClient.close();
        mongoClient = null;
        logger.info("Disconnected from database.");
    }

    @Override
    public List<String> listTables() {
        try {
            return mongoDatabase.listCollectionNames().into(new ArrayList<>());
        }
        catch (Exception e) {
            throw new RuntimeException("Error listing collections", e);
        }
    }

    @Override
    public Map<String, String> getTableSchema(String tableName) throws SQLException {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection(tableName);
            Map<String, String> schema = new LinkedHashMap<>();
            List<Document> samples = collection.find().limit(100).into(new ArrayList<>());
            for (Document doc : samples)
            for (Map.Entry<String, Object> entry : doc.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!schema.containsKey(key))
                    schema.put(key, value != null ? value.getClass().getSimpleName() : "Object");
            }

            return schema;
        }
        catch (Exception e) {
            throw new SQLException("Error getting collection schema", e);
        }
    }

    public List<Map<String, Object>> getTableData(String tableName, int limit, int offset) throws SQLException {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection(tableName);
            List<Map<String, Object>> data = new ArrayList<>();
            collection.find().skip(offset).limit(limit).forEach((Consumer<Document>) doc -> data.add(doc));
            return data;
        }
        catch (Exception e) {
            throw new SQLException("Error fetching data", e);
        }
    }

    @Override
    public void createTable(String tableName, String tableSchema) {
        try {
            mongoDatabase.createCollection(tableName);
            logger.info("Collection '{}' created successfully.", tableName);
        }
        catch (MongoCommandException e) {
            logger.error("Error creating collection '{}': {}", tableName, e.getErrorMessage());
        }
    }

    @Override
    public void deleteTable(String tableName) throws SQLException {
        try {
            mongoDatabase.getCollection(tableName).drop();
        }
        catch (Exception e) {
            throw new SQLException("Error deleting collection", e);
        }
    }

    @Override
    public void insertData(String tableName, Map<String, Object> data) throws SQLException {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection(tableName);
            collection.insertOne(new Document(data));
        }
        catch (Exception e) {
            throw new SQLException("Error inserting data", e);
        }
    }

    @Override
    public void updateData(String tableName, Map<String, Object> data, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection(tableName);
            collection.updateOne(
                    new Document(primaryKeyColumn, primaryKeyValue),
                    new Document("$set", new Document(data))
            );
        }
        catch (Exception e) {
            throw new SQLException("Error updating data", e);
        }
    }

    @Override
    public void deleteData(String tableName, String primaryKeyColumn, Object primaryKeyValue) throws SQLException {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection(tableName);
            collection.deleteOne(new Document(primaryKeyColumn, primaryKeyValue));
        }
        catch (Exception e) {
            throw new SQLException("Error deleting data", e);
        }
    }

    @Override
    public int getTotalEntries(String tableName) throws SQLException {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection(tableName);
            return (int) collection.countDocuments();
        }
        catch (Exception e) {
            throw new SQLException("Error getting total entries", e);
        }
    }

    @Override
    public String getPrimaryKeyColumn(String tableName) {
        return "";
    }

    @Override
    public int getNextId(String tableName, String primaryKeyColumn) {
        return 0;
    }

    @Override
    public boolean executeSqlCommand(String sql, ISqlResultHandler handler) {
        return false;
    }

    @Override
    public int getTableEntryCount(String tableName) {
        return 0;
    }

    @Override
    public QueryResult getQueryResultFromResultSet(ResultSet rs) {
        return null;
    }

    @Override
    public String getDatabaseName() {
        return dbName;
    }
}
