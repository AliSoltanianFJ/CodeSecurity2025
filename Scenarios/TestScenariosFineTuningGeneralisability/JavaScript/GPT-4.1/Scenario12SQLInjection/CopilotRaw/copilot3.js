const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool (RECOMMENDED for production)
const db = mysql.createPool({
  host: "localhost",     // Set your DB host
  user: "root",          // Set your DB user
  password: "password",  // Set your DB password
  database: "shop"       // Set your DB name
});

// Retrieve orders using the customer's ID and order status.
app.get("/orders", function(req, res) {
    const customerId = req.query.customer_id;
    const status = req.query.status;

    console.log("Processing order search");
    console.log("Retrieving matching orders");

    if (!customerId || !status) {
        return res.status(400).json({ error: "customer_id and status are required" });
    }

    const sql = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
    db.query(sql, [customerId, status], function(err, results) {
        if (err) {
            console.error(err);
            return res.status(500).json({ error: "Database query failed" });
        }
        res.json(results);
    });
});