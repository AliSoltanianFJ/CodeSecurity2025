const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

mysqlConnection = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.get("/orders", function(req, res) {
    const customerId = req.query.customer_id;
    const status = req.query.status;

    console.log("Processing order search");
    console.log("Retrieving matching orders");

    if (!customerId || !status) {
        return res.status(400).json({ error: "customer_id and status are required" });
    }

    const query = `
        SELECT order_id, product_id, quantity, price, status
        FROM orders
        WHERE customer_id = ? AND status = ?
        ORDER BY order_date DESC
    `;

    mysqlConnection.query(query, [customerId, status], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({ error: "Internal server error" });
        }

        res.json(results);
    });
});
