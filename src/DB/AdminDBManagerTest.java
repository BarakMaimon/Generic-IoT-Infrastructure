package DB;

import org.junit.jupiter.api.*;

import javax.json.Json;
import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminDBManagerTest {
    private static final String mySqlUrl = "jdbc:mysql://localhost:3306";
    private static final String username = "root";
    private static final String password = "Bm27102000";
    private static AdminDBManager adminDBManager;

    @BeforeAll
    public static void setUp() throws SQLException {
        // Replace the connection details with your own database details
        adminDBManager = new AdminDBManager(mySqlUrl, username, password);
    }
    @AfterAll
    public static void tearDown() throws SQLException {
        // Remove the test database for AdminCRUD
        try (Connection connection = DriverManager.getConnection(mySqlUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS Admin;");
        }
    }

    @Test
    @Order(1)
    public void testRegisterCompany() throws SQLException {
        JsonObject companyData = Json.createObjectBuilder()
                .add("company_name", "TestCompany")
                .add("company_address", "TestAddress")
                .add("contact_name", "TestContact")
                .add("contact_phone", "123456789")
                .add("contact_email", "test@example.com")
                .add("service_fee", "1000")
                .build();

        JsonObject result = adminDBManager.registerComapny(companyData);
        Assertions.assertEquals(200, result.getInt("statusCode"));
    }

    @Test
    @Order(2)
    public void testRegisterProduct() throws SQLException {
        JsonObject productData = Json.createObjectBuilder()
                .add("company_name", "TestCompany")
                .add("product_name", "TestProduct")
                .add("product_description", "TestDescription")
                .build();

        JsonObject result = adminDBManager.registerProduct(productData);
        Assertions.assertEquals(200, result.getInt("statusCode"));
    }

    // Add more tests for read, update, and delete operations as needed

}