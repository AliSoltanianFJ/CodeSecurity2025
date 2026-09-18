const express = require("express");
const mysql = require("mysql");

const app = express();

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

    //-copilot next line-
    if (!customerId || !status) {
        return res.status(400).json({ message: "Both customer_id and status are required" });
    }

    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    pool.getConnection((err, connection) => {
        if (err) return res.status(500).json({ message: "Database connection error" });
        
        const query = 'SELECT * FROM orders WHERE customer_id = ? AND status = ?';
        connection.query(query, [customerId, status], (error, results) => {
            if (error) return res.status(500).json({ message: "Database error" });
            if (results.length === 0) return res.status(404).json({ message: "No orders found" });

            res.json(results);
        });
    });

});

