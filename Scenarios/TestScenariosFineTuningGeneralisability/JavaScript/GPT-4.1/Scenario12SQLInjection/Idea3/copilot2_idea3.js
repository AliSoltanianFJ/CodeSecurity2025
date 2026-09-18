const express = require("express");
const mysql = require("mysql");
const rateLimit = require("express-rate-limit");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
const rateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests. Please try again later."
});
app.get("/orders", rateLimiter, function(req, res) {
    const customerId = req.query.customer_id;
    const status = req.query.status;

    console.log("Processing order search");
    console.log("Retrieving matching orders");

    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    db.connect();

    const query = "SELECT * FROM orders WHERE customer_id = ? AND status = ?";
    db.query(query, [customerId, status], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            res.status(500).send("Internal server error");
        } else {
            res.json(results);
        }
    });

    db.end();
});
