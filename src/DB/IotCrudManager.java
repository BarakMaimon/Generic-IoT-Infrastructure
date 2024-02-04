package DB;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map;

public class IotCrudManager {
    private final IotCRUDImpl iotCRUDImpl;
    private final UpdateCRUDImpl updateCRUDImpl;
    private final String connectionString;
    private final String USERS_COLLECTION_NAME_SPACE = "%s_users";
    private final String UPDATES_COLLECTION_NAME_SPACE = "%s_updates";
    private final String DB_KEY = "company_name";
    private final String COLLECTION_KEY = "product_name";

    public IotCrudManager(String connectionString) {
        this.connectionString = connectionString;
        this.iotCRUDImpl = new IotCRUDImpl();
        this.updateCRUDImpl = new UpdateCRUDImpl();
    }

    public void registerCompanyCRUD(JsonObject data) {
        String DBName = data.getString(DB_KEY);

        try (MongoClient mongoClient = MongoClients.create(this.connectionString)) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DBName);
            mongoDatabase.createCollection("dummy");
            Document dummy = new Document().append("dummy", "dummy");
            mongoDatabase.getCollection("dummy").insertOne(dummy);
        }
    }

    public void registerProductCRUD(JsonObject data) {

        String DBName = data.getString(DB_KEY);
        String productName = data.getString(COLLECTION_KEY);
        Document dummy = new Document().append("dummy", "dummy");
        try (MongoClient mongoClient = MongoClients.create(this.connectionString)) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DBName);
            mongoDatabase.createCollection(String.format(USERS_COLLECTION_NAME_SPACE, productName));
            mongoDatabase.createCollection(String.format(UPDATES_COLLECTION_NAME_SPACE, productName));
            mongoDatabase.getCollection(String.format(USERS_COLLECTION_NAME_SPACE, productName)).insertOne(dummy);
            mongoDatabase.getCollection(String.format(UPDATES_COLLECTION_NAME_SPACE, productName)).insertOne(dummy);
        }
    }

    public JsonObject registerIotCRUD(JsonObject data) {
        return iotCRUDImpl.createCRUD(data);
    }

    public JsonObject updateCRUD(JsonObject data) {
        return updateCRUDImpl.createCRUD(data);
    }

    interface CRUD {
        JsonObject createCRUD(JsonObject data);

        JsonObject readCRUD(JsonObject data);

        JsonObject updateCRUD(JsonObject data);

        JsonObject deleteCRUD(JsonObject data);
    }

    private class IotCRUDImpl implements CRUD {

        @Override
        public JsonObject createCRUD(JsonObject data) {

            Document document = Document.parse(data.toString());

            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(USERS_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));

                usersCollection.insertOne(document);
                return convertDocumentToJson(document);
            }
        }

        @Override
        public JsonObject readCRUD(JsonObject data) {

            try (MongoClient mongoClient = MongoClients.create(connectionString)) {

                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(USERS_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));


                Document IOTDoc = usersCollection.find().first();
                if (null == IOTDoc) {
                    return null;
                }
                return convertDocumentToJson(IOTDoc);
            }
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) {

            Document Doc = Document.parse(data.toString());
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {

                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(USERS_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));
                usersCollection.updateOne(Filters.eq("_id", data.getString("_id")), new Document("$set", Doc));
                return data;
            }
        }

        @Override
        public JsonObject deleteCRUD(JsonObject data) {

            try (MongoClient mongoClient = MongoClients.create(connectionString)) {

                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(USERS_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));
                Document document = usersCollection.findOneAndDelete(Filters.eq("_id", data.getString("_id")));
                if (document == null) {
                    return null;
                }
                return convertDocumentToJson(document);
            }
        }
    }

    private class UpdateCRUDImpl implements CRUD {

        @Override
        public JsonObject createCRUD(JsonObject data) {

            Document document = Document.parse(data.toString());
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> updatesCollection = mongoDatabase.getCollection(
                        String.format(UPDATES_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));

                updatesCollection.insertOne(document);
                return convertDocumentToJson(document);
            }
        }

        @Override
        public JsonObject readCRUD(JsonObject data) {

            try (MongoClient mongoClient = MongoClients.create(connectionString)) {

                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(UPDATES_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));


                Document IOTDoc = usersCollection.find().first();
                if (null == IOTDoc) {
                    return null;
                }
                return convertDocumentToJson(IOTDoc);
            }
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) {

              Document Doc = Document.parse(data.toString());
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {

                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(UPDATES_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));
                usersCollection.updateOne(Filters.eq("_id", data.getString("_id")), new Document("$set", Doc));
                return data;
            }
        }

        @Override
        public JsonObject deleteCRUD(JsonObject data) {

            try (MongoClient mongoClient = MongoClients.create(connectionString))
            {
                MongoDatabase mongoDatabase = mongoClient.getDatabase(data.getString(DB_KEY));
                MongoCollection<Document> usersCollection = mongoDatabase.getCollection(
                        String.format(UPDATES_COLLECTION_NAME_SPACE, data.getString(COLLECTION_KEY)));
                Document document = usersCollection.findOneAndDelete(Filters.eq("_id", data.getString("_id")));
                if (document == null) {
                    return null;
                }
                return convertDocumentToJson(document);
            }
        }
    }


    private static JsonObject convertDocumentToJson(Document document) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            // Convert each entry in the Document to JSON
            addJsonEntry(jsonObjectBuilder, entry.getKey(), entry.getValue());
        }

        return jsonObjectBuilder.build();
    }

    private static void addJsonEntry(JsonObjectBuilder jsonObjectBuilder, String key, Object value) {
        if (value instanceof Document) {
            // If the value is another Document, recursively convert it
            jsonObjectBuilder.add(key, convertDocumentToJson((Document) value));
        } else {
            // Otherwise, add the value directly
            jsonObjectBuilder.add(key, value.toString());
        }
    }


}
