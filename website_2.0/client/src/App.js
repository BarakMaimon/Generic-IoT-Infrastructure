import React, { useState } from "react";
import logo from "./indexPhoto.jpg";
import "./App.css";

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <p className="Title">Generic IOT Infrastructure Project</p>
        <p>Barak Maimon</p>
        <img src={logo} className="App-logo" alt="logo" />
      </header>
      <Menu />
    </div>
  );
}

function Menu() {
  const [selectedForm, setSelectedForm] = useState(null);

  const handleFormClick = (formType) => {
    setSelectedForm(formType);
  };

  return (
    <div className="Menu">
      <h1>Register to our system</h1>
      <h3>Choose what to do</h3>

      <div className="Button-menu">
        <button className="Button" onClick={() => handleFormClick("product")}>
          Register Product
        </button>
        <button className="Button" onClick={() => handleFormClick("company")}>
          Register Company
        </button>
      </div>

      <hr />

      <div className="Form-holder">
        {selectedForm === "product" && <ProductForm />}
        {selectedForm === "company" && <CompanyForm />}
      </div>
    </div>
  );
}

function Input({ label, type, onChange }) {
  return (
    <tr className="record-holder">
      <td>
        <label>{label}:</label>
      </td>
      <td>
        <input
          name={label.replace(/\s+/g, "").toLowerCase()}
          onChange={onChange}
          type={type}
        />
      </td>
    </tr>
  );
}

function Form({ formType, fields, onSubmit, response }) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  const [formData, setFormData] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  return (
    <form className="Form" onSubmit={handleSubmit}>
      {fields.map((field) => (
        <Input
          key={field.label}
          label={field.label}
          type={field.type}
          onChange={handleChange}
        />
      ))}
      <button type="submit" className="submit-button">
        Submit
      </button>
      <br />
      {response && <label className="response-data">{response.message}</label>}
    </form>
  );
}

function ProductForm() {
  const fields = [
    { label: "Company_Name", type: "text" },
    { label: "Product_Name", type: "text" },
    { label: "Product_Description", type: "text" },
  ];

  const [response, setResponse] = useState(null);

  const handleSubmit = async (formData) => {
    try {
      const response = await fetch("/Product", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const responseData = await response.json();
      setResponse(responseData);
    } catch (error) {
      setResponse("Registred failed");
    }
  };

  return (
    <Form
      formType="product"
      fields={fields}
      onSubmit={handleSubmit}
      response={response}
    />
  );
}

function CompanyForm() {
  const fields = [
    { label: "Company_Name", type: "text" },
    { label: "Company_Address", type: "text" },
    { label: "Contact_Name", type: "text" },
    { label: "Contact_Phone", type: "tel" },
    { label: "Contact_Email", type: "email" },
    { label: "Service_Fee", type: "number" },
    { label: "Card_Number", type: "number" },
    { label: "Card_Holder_Name", type: "text" },
    { label: "EX_Date", type: "date" },
    { label: "CVV", type: "number" },
  ];

  const [response, setResponse] = useState(null);

  const handleSubmit = async (formData) => {
    try {
      const response = await fetch("/Company", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const responseData = await response.json();
      setResponse(responseData);
    } catch (error) {
      setResponse("Registred failed");
    }
  };

  return (
    <Form
      formType="company"
      fields={fields}
      onSubmit={handleSubmit}
      response={response}
    />
  );
}

export default App;
