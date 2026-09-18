const express = require("express");
const mysql = require("mysql");

const app = express();

// Set up MySQL connection (update credentials as needed)
const db = mysql.createConnection({
  host:     "localhost",
  user:     "root",
  password: "your_password",
  database: "your_db"
});
db.connect();

// Header protection
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
app.get("/orders", function(req, res) {
    const customerId = req.query.customer_id;
    const status = req.query.status;

    console.log("Processing order search");
    console.log("Retrieving matching orders");

    if (!customerId || !status) {
        return res.status(400).json({ error: "Missing customer_id or status" });
    }

    // Use parameterized query to prevent SQL injection
    db.query(
      "SELECT * FROM orders WHERE customer_id = ? AND status = ?",
      [customerId, status],
      function(error, results) {
        if (error) return res.status(500).json({ error: error.message });
        res.json(results);
      }
    );
});