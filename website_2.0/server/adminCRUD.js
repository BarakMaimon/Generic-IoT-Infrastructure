const { Sequelize, DataTypes } = require("sequelize");

const sequelize = new Sequelize("Admin", "root", "Bm27102000", {
  host: "localhost",
  dialect: "mysql",
});

// Define Company model
const Company = sequelize.define("Company", {
  company_name: {
    type: DataTypes.STRING(32),
    allowNull: false,
    primaryKey: true,
  },
  company_address: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  contact_name: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  contact_phone: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  contact_email: {
    type: DataTypes.STRING(255),
    allowNull: false,
  },
  service_fee: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: false,
  },
});

// Define CreditCardDetails model
const CreditCardDetails = sequelize.define("CreditCardDetails", {
  creditCard_id: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: false,
    primaryKey: true,
    autoIncrement: true,
  },
  company_name: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  card_number: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: false,
  },
  card_holder_name: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  ex_date: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  cvv: {
    type: DataTypes.STRING(3),
    allowNull: false,
  },
});

// Define Products model
const Products = sequelize.define("Products", {
  product_id: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: false,
    primaryKey: true,
    autoIncrement: true,
  },
  company_name: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  product_name: {
    type: DataTypes.STRING(32),
    allowNull: false,
  },
  product_description: {
    type: DataTypes.TEXT,
    allowNull: false,
  },
});

// Define associations between models
Company.hasMany(CreditCardDetails, { foreignKey: "company_name" });
CreditCardDetails.belongsTo(Company, { foreignKey: "company_name" });

Company.hasMany(Products, { foreignKey: "company_name" });
Products.belongsTo(Company, { foreignKey: "company_name" });

// Synchronize models with the database
sequelize.sync({ alter: true }).then(() => {
  console.log("Database and tables created!");
});

const createCompany = async (jsonBody) => {
  try {
    // Extract data for Company
    const {
      company_name,
      company_address,
      contact_name,
      contact_phone,
      contact_email,
      service_fee,
    } = jsonBody;

    // Create a new Company record
    const company = await Company.create({
      company_name,
      company_address,
      contact_name,
      contact_phone,
      contact_email,
      service_fee,
    });

    // Extract data for CreditCardDetails
    const { card_number, card_holder_name, ex_date, cvv } = jsonBody;

    // Create a corresponding CreditCardDetails record
    await CreditCardDetails.create({
      company_name: company.company_name,
      card_number,
      card_holder_name,
      ex_date,
      cvv,
    });

    // Return the created Company record
    return company;
  } catch (error) {
    // Handle errors
    console.error("Error creating Company:", error);
    throw error; // Rethrow the error to handle it at a higher level if needed
  }
};

const createProduct = async (jsonBody) => {
  try {
    const product = await Products.create(jsonBody);
    return product;
  } catch (error) {
    console.error("Error creating Company:", error);
    throw error; // Rethrow the error to handle it at a higher level if needed
  }
};

module.exports = {
  createCompany,
  createProduct,
};
