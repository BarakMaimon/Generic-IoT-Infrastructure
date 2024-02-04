package DB;
//TODO read,update and delete
import javax.json.Json;
import javax.json.JsonObject;
import java.sql.*;
import java.util.Arrays;

public class AdminDBManager {
    private final CompanyCRUDImpl companyCRUD;
    private final ProductCRUDImpl productCRUD;
    private final String mySqlUrl;
    private final String username;
    private final String password;


    public AdminDBManager(String mySqlUrl, String username, String password) throws SQLException {
        this.mySqlUrl = mySqlUrl;
        this.username = username;
        this.password = password;
        String queryNewDB = "CREATE DATABASE IF NOT EXISTS Admin;";

        try (Connection connection = DriverManager.getConnection(this.mySqlUrl, this.username, this.password);
             Statement statement = connection.createStatement()) {

            statement.execute(queryNewDB);
        }
        this.companyCRUD = new CompanyCRUDImpl();
        this.productCRUD = new ProductCRUDImpl();
    }


    public JsonObject registerComapny(JsonObject data) throws SQLException {
        return this.companyCRUD.createCRUD(data);
    }


    public JsonObject registerProduct(JsonObject data) {
        return this.productCRUD.createCRUD(data);
    }


    interface CRUD {
        JsonObject createCRUD(JsonObject data) throws SQLException;

        JsonObject readCRUD(JsonObject data);

        JsonObject updateCRUD(JsonObject data);

        JsonObject deleteCRUD(JsonObject data);
    }

    private class CompanyCRUDImpl implements CRUD {
        public CompanyCRUDImpl() throws SQLException {
            if (checkIfTableExists("Companies")) {return;}
            createTable("Companies", "company_name", "VARCHAR(32)", false);
            addField("Companies", "company_address", "VARCHAR(255)", true);
            addField("Companies", "contact_name", "VARCHAR(32)", true);
            addField("Companies", "contact_phone", "VARCHAR(32)", true);
            addField("Companies", "contact_email", "VARCHAR(255)", true);
            addField("Companies", "service_fee", "BIGINT UNSIGNED", true);
        }

        @Override
        public JsonObject createCRUD(JsonObject companyData) throws SQLException {

            String urlToDB = String.format("%s/%s", mySqlUrl, "Admin");
            String[] fields = {"company_name", "company_address", "contact_name",
                    "contact_phone", "contact_email", "service_fee"};


            String placeholders = String.join(", ", Arrays.stream(fields).map(f -> "?").toArray(String[]::new));
            String record = String.format("INSERT INTO Companies(%s) VALUES (%s);", setArrayToString(fields), placeholders);

            try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
                 PreparedStatement statement = connection.prepareStatement(record)) {
                for (int i = 0; i < fields.length; i++) {
                    statement.setString(i + 1, companyData.getString(fields[i]));
                }

                statement.executeUpdate();
            } catch (SQLException e) {
                return Json.createObjectBuilder().add("statusCode", 500).build();
            }

            //TODO add credit card


            return Json.createObjectBuilder().add("statusCode", 200).build();
        }

        @Override
        public JsonObject readCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject deleteCRUD(JsonObject data) {
            return null;
        }
    }

    private class ProductCRUDImpl implements CRUD {

        public ProductCRUDImpl() throws SQLException {

            if (checkIfTableExists("Products")) {return;}
            createTable("Products", "product_id", "BIGINT UNSIGNED", true);
            addField("Products", "company_name", "VARCHAR(32)", true);
            addField("Products", "product_name", "VARCHAR(32)", true);
            addField("Products", "product_description", "TEXT", true);
            addForeignKey("Products", "company_name", "Companies");
        }

        @Override
        public JsonObject createCRUD(JsonObject data) {

            String urlToDB = String.format("%s/%s", mySqlUrl, "Admin");
            String[] fields = {"company_name" , "product_name", "product_description"};
            String placeholders = String.join(", ", Arrays.stream(fields).map(f -> "?").toArray(String[]::new));
            String record = String.format("INSERT INTO Products(%s) VALUES (%s);", setArrayToString(fields), placeholders);

            try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
                 PreparedStatement statement = connection.prepareStatement(record)) {
                for (int i = 0; i < fields.length; i++) {
                    statement.setString(i + 1, data.getString(fields[i]));
                }

                statement.executeUpdate();
            } catch (SQLException e) {
                return Json.createObjectBuilder().add("statusCode", 500).build();
            }
            return Json.createObjectBuilder().add("statusCode", 200).build();
        }

        @Override
        public JsonObject readCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject deleteCRUD(JsonObject data) {
            return null;
        }
    }

    /*===============================================================================================================*/
    /*===============================================================================================================*/
    /*===============================================================================================================*/

    private boolean checkIfTableExists(String tableName) {
        try (Connection connection = DriverManager.getConnection(this.mySqlUrl, this.username, this.password)) {

            // get db metadata
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            // Get a list of all tables in the connected database.
            ResultSet tables = databaseMetaData.getTables(null, null, tableName, null);

            // Check if the table name that you are looking for is in the list.
            return tables.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private String setArrayToString(String[] strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : strings) {
            stringBuilder.append(str);
            stringBuilder.append(", ");
        }

        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private void createTable(String tableName, String primaryKeyName, String primaryKeyType, boolean autoIncrement) throws SQLException {
        String urlToDB = String.format("%s/%s", this.mySqlUrl, "Admin");

        String autoInc = autoIncrement ? "AUTO_INCREMENT" : "";
        String createNewTable = String.format("CREATE TABLE IF NOT EXISTS %s(%s %s %s PRIMARY KEY);",
                tableName, primaryKeyName, primaryKeyType, autoInc);

        try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
             Statement statement = connection.createStatement()) {

            statement.execute(createNewTable);
        } catch (SQLException e) {
            throw new SQLException(String.format("create table %s failed: ", tableName) + e.getMessage());
        }
    }

    private void addField(String tableName, String fieldName, String fieldType, boolean isNotNull) throws SQLException {
        String urlToDB = String.format("%s/%s", this.mySqlUrl, "Admin");

        String notNull = isNotNull ? "NOT NULL" : "";
        String alterNewTable = String.format("ALTER TABLE %s add %s %s %s ;",
                tableName, fieldName, fieldType, notNull);

        try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
             Statement statement = connection.createStatement()) {

            statement.execute(alterNewTable);
        } catch (SQLException e) {
            throw new SQLException(String.format("adding field %s failed: ", fieldName) + e.getMessage());
        }
    }

    private void addForeignKey(String alterableName, String fieldName, String referencedTableName) throws SQLException {
        String urlToDB = String.format("%s/%s", this.mySqlUrl, "Admin");

        String foreignKeyName = String.format("%s_%s_foreign", alterableName, fieldName);
        String foreignKey = String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY(%s) REFERENCES %s(%s);",
                alterableName, foreignKeyName, fieldName, referencedTableName, fieldName);

        try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
             Statement statement = connection.createStatement()) {

            statement.execute(foreignKey);
        } catch (SQLException e) {
            throw new SQLException(String.format("adding foreign key: %s reference from %s to %s failed: ",
                    fieldName, referencedTableName, alterableName) + e.getMessage());
        }
    }

}
