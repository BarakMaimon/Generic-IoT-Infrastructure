const express = require("express");
const { createCompany, createProduct } = require("./adminCRUD");

const PORT = process.env.PORT || 3001;

const app = express();
app.use(express.json()); // Middleware to parse JSON requests
const resources = new Map();
resources.set("/Company", createCompany);
resources.set("/Prodcut", createProduct);

// Custom middleware function to make a new HTTP request
const forwardToServer = async (req, res, next) => {
  try {
    if (
      req.method === "POST" &&
      (req.url === "/Company" || req.url === "/Product")
    ) {
      const targetURL = `http://localhost:8085${req.url}`;

      console.log(`Received POST request to ${req.url}`);
      console.log(req.body);

      resources.get(req.url)(req.body);

      (async () => {
        const rawResponse = await fetch(targetURL, {
          method: "POST",
          headers: {
            Accept: "application/json",
            "Content-Type": "application/json",
          },
          body: JSON.stringify(req.body),
        });
        const content = rawResponse["status"];

        console.log(content);
        if (content >= 400 && content < 500) {
          res.json({ message: "Register failed" });
        } else if (content >= 200 && content < 300) {
          res.json({ message: "Register succesed" });
        }
      })();
    } else {
      // Continue with the regular route handling for other endpoints
      next();
    }
  } catch (error) {
    console.error("Error forwarding request:", error);
    res.status(500).json({ error: "Internal Server Error" });
  }
};

// Use the custom middleware for forwarding requests to "/Company" or "/Product"
app.use(forwardToServer);

app.listen(PORT, () => {
  console.log(`Server listening on ${PORT}`);
});
