
import javax.json.Json;
import javax.json.JsonObject;
import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class AdminDBManager {
	private final CompanyCRUDImpl companyCRUD;
	private final ProductCRUDImpl productCRUD;
	private final CreditCardCrudImpl creditCardCRUD;
	private final String mySqlUrl;
	private final String username;
	private final String password;

	public AdminDBManager(String mySqlUrl, String username, String password)
			throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		this.mySqlUrl = mySqlUrl;
		this.username = username;
		this.password = password;

		// Class.forName("com.mysql.jdbc.Driver");
		String queryNewDB = "CREATE DATABASE IF NOT EXISTS Admin;";

		try (Connection connection = DriverManager.getConnection(this.mySqlUrl, this.username, this.password);
				Statement statement = connection.createStatement()) {

			statement.execute(queryNewDB);
		}
		this.companyCRUD = new CompanyCRUDImpl();
		this.productCRUD = new ProductCRUDImpl();
		this.creditCardCRUD = new CreditCardCrudImpl();
	}

	public JsonObject registerComapny(JsonObject data) throws SQLException {
		return this.companyCRUD.createCRUD(data);
	}

	public JsonObject registerProduct(JsonObject data) {
		return this.productCRUD.createCRUD(data);
	}
	
	
	public boolean areEntitiesRegistered(String tableName, Map<String, Object> columnValues) {
		String urlToDB = String.format("%s/%s", mySqlUrl, "Admin");
        boolean areRegistered = false;

        // Build the SQL query dynamically based on the provided column names
        StringBuilder queryBuilder = new StringBuilder("SELECT 1 FROM ");
        queryBuilder.append(tableName).append(" WHERE ");

        // Prepare the PreparedStatement parameters
        int paramCount = 0;

        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            if (paramCount > 0) {
                queryBuilder.append(" AND ");
            }

            queryBuilder.append(entry.getKey()).append(" = ?");

            paramCount++;
        }

        String query = queryBuilder.toString();

        try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             Statement statement = connection.createStatement()) {


            int paramIndex = 1;

            for (Object value : columnValues.values()) {
                if (value instanceof String) {
                    preparedStatement.setString(paramIndex, (String) value);
                } else if (value instanceof Integer) {
                    preparedStatement.setInt(paramIndex, (Integer) value);
                } else if (value instanceof Double) {
                    preparedStatement.setDouble(paramIndex, (Double) value);
                }
                // Add more type checks and corresponding setXXX methods as needed

                paramIndex++;
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    areRegistered = true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return areRegistered;
    }

	

	interface CRUD {
		JsonObject createCRUD(JsonObject data) throws SQLException;

		JsonObject readCRUD(JsonObject data);

		JsonObject updateCRUD(JsonObject data);

		JsonObject deleteCRUD(JsonObject data);
	}

	private class CompanyCRUDImpl implements CRUD {
		public CompanyCRUDImpl() throws SQLException {
			if (checkIfTableExists("Companies")) {
				return;
			}
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
			String[] company_fields = { "company_name", "company_address", "contact_name", "contact_phone", "contact_email",
					"service_fee" };

			String placeholders = String.join(", ", Arrays.stream(company_fields).map(f -> "?").toArray(String[]::new));
			String record = String.format("INSERT INTO Companies(%s) VALUES (%s);", setArrayToString(company_fields),
					placeholders);

			try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
					PreparedStatement statement = connection.prepareStatement(record)) {
				for (int i = 0; i < company_fields.length; i++) {
					statement.setString(i + 1, companyData.getString(company_fields[i]));
				}
				

				statement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				return Json.createObjectBuilder().add("status_cwode", 500).build();
			}
			
			return creditCardCRUD.createCRUD(companyData);

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

			if (checkIfTableExists("Products")) {
				return;
			}
			createTable("Products", "product_id", "BIGINT UNSIGNED", true);
			addField("Products", "company_name", "VARCHAR(32)", true);
			addField("Products", "product_name", "VARCHAR(32)", true);
			addField("Products", "product_description", "TEXT", true);
			addForeignKey("Products", "company_name", "Companies");
		}

		@Override
		public JsonObject createCRUD(JsonObject data) {

			String urlToDB = String.format("%s/%s", mySqlUrl, "Admin");
			String[] fields = { "company_name", "product_name", "product_description" };
			String placeholders = String.join(", ", Arrays.stream(fields).map(f -> "?").toArray(String[]::new));
			String record = String.format("INSERT INTO Products(%s) VALUES (%s);", setArrayToString(fields),
					placeholders);

			try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
					PreparedStatement statement = connection.prepareStatement(record)) {
				for (int i = 0; i < fields.length; i++) {
					statement.setString(i + 1, data.getString(fields[i]));
				}
				

				statement.executeUpdate();
			} catch (SQLException e) {
				return Json.createObjectBuilder().add("status_code", 500).build();
			}
			return Json.createObjectBuilder().add("status_code", 200).build();
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
	
	private class CreditCardCrudImpl implements CRUD{
		
		public CreditCardCrudImpl() throws SQLException {

			if (checkIfTableExists("CreditCardDetails")) {
				return;
			}
			createTable("CreditCardDetails", "creditCard_id", "BIGINT UNSIGNED", true);
			addField("CreditCardDetails", "company_name", "VARCHAR(32)", true);
			addField("CreditCardDetails", "card_number", "BIGINT UNSIGNED", true);
			addField("CreditCardDetails", "card_holder_name", "VARCHAR(32)", true);
			addField("CreditCardDetails", "ex_date", "VARCHAR(32)", true);
			addField("CreditCardDetails", "CVV", "VARCHAR(3)", true);
			addForeignKey("CreditCardDetails", "company_name", "Companies");
		}

		@Override
		public JsonObject createCRUD(JsonObject data) throws SQLException {
			String urlToDB = String.format("%s/%s", mySqlUrl, "Admin");
			String[] cards_fields = { "company_name","card_holder_name", "card_number", "ex_date", "CVV" };

			String placeholders = String.join(", ", Arrays.stream(cards_fields).map(f -> "?").toArray(String[]::new));
			String record = String.format("INSERT INTO CreditCardDetails (%s) VALUES (%s);", setArrayToString(cards_fields),
					placeholders);

			try (Connection connection = DriverManager.getConnection(urlToDB, username, password);
					PreparedStatement statement = connection.prepareStatement(record)) {
				for (int i = 0; i < cards_fields.length; i++) {

					statement.setString(i + 1, data.getString(cards_fields[i]));
				}
				

				statement.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				return Json.createObjectBuilder().add("status_code", 500).build();
			}

			
			return Json.createObjectBuilder().add("status_code", 200).build();
		}

		@Override
		public JsonObject readCRUD(JsonObject data) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public JsonObject updateCRUD(JsonObject data) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public JsonObject deleteCRUD(JsonObject data) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	/*
	 * =============================================================================
	 * ==================================
	 */
	/*
	 * =============================================================================
	 * ==================================
	 */
	/*
	 * =============================================================================
	 * ==================================
	 */

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

	private void createTable(String tableName, String primaryKeyName, String primaryKeyType, boolean autoIncrement)
			throws SQLException {
		String urlToDB = String.format("%s/%s", this.mySqlUrl, "Admin");

		String autoInc = autoIncrement ? "AUTO_INCREMENT" : "";
		String createNewTable = String.format("CREATE TABLE IF NOT EXISTS %s(%s %s %s PRIMARY KEY);", tableName,
				primaryKeyName, primaryKeyType, autoInc);

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
		String alterNewTable = String.format("ALTER TABLE %s add %s %s %s ;", tableName, fieldName, fieldType, notNull);

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
			throw new SQLException(String.format("adding foreign key: %s reference from %s to %s failed: ", fieldName,
					referencedTableName, alterableName) + e.getMessage());
		}
	}

}
