const express = require("express");
const mysql = require("mysql");

const app = express();

// Replace with your actual DB config
const connection = mysql.createConnection({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_db'
});

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

    // Validate input
    if (!customerId || !status) {
        return res.status(400).json({error: "customer_id and status are required query parameters"});
    }

    const sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
    connection.query(sql, [customerId, status], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({error: "Database query error"});
        }
        res.json(results);
    });
});

app.listen(3000, () => {
  console.log("Server running on port 3000");
});