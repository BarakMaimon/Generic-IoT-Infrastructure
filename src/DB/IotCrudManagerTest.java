package DB;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.*;

import javax.json.Json;
import javax.json.JsonObject;
import static org.junit.jupiter.api.Assertions.*;

class IotCrudManagerTest {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private IotCrudManager iotCrudManager;

    @BeforeEach
    void setUp() {
        iotCrudManager = new IotCrudManager(CONNECTION_STRING);
    }
    @AfterEach
    void tearDown() {
        // Perform cleanup tasks here, such as deleting test data
        cleanUpTestData();
    }

    private void cleanUpTestData() {

        try (MongoClient mongoClient = MongoClients.create(CONNECTION_STRING)) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase("TestCompany");

            mongoDatabase.getCollection("dummy").drop();
            mongoDatabase.getCollection("TestProduct_users").drop();
            mongoDatabase.getCollection("TestProduct_updates").drop();
        }
    }

    @Test
    void registerCompanyCRUD() {
        JsonObject companyData = createCompanyData();
        assertDoesNotThrow(() -> iotCrudManager.registerCompanyCRUD(companyData));
    }

    @Test
    void registerProductCRUD() {
        JsonObject productData = createProductData();
        assertDoesNotThrow(() -> iotCrudManager.registerProductCRUD(productData));
    }

    @Test
    void registerIotCRUD() {
        JsonObject iotData = createIotData();
        JsonObject result = iotCrudManager.registerIotCRUD(iotData);
        assertNotNull(result);
        assertNotNull(result.getString("_id"));
        assertEquals(iotData.getString("CompanyName"), result.getString("CompanyName"));
    }

    @Test
    void updateCRUD() {
        JsonObject updateData = createUpdateData();
        JsonObject result = iotCrudManager.updateCRUD(updateData);
        assertNotNull(result);
        assertEquals(updateData.getString("_id"), result.getString("_id"));
    }

    // Helper methods to create sample data for testing
    private JsonObject createCompanyData() {
        return Json.createObjectBuilder()
                .add("CompanyName", "TestCompany")
                .build();
    }

    private JsonObject createProductData() {
        return Json.createObjectBuilder()
                .add("CompanyName", "TestCompany")
                .add("ProductName", "TestProduct")
                .build();
    }

    private JsonObject createIotData() {
        return Json.createObjectBuilder()
                .add("CompanyName", "TestCompany")
                .add("ProductName", "TestProduct")
                .add("SomeField", "SomeValue")
                .build();
    }

    private JsonObject createUpdateData() {
        return Json.createObjectBuilder()
                .add("CompanyName", "TestCompany")
                .add("ProductName", "TestProduct")
                .add("_id", "someObjectId") // Replace with a valid MongoDB ObjectId
                .add("UpdatedField", "UpdatedValue")
                .build();
    }
}