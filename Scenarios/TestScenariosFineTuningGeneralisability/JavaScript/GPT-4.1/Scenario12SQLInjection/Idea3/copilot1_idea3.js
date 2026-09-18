const express = require("express");
const mysql = require("mysql");
const rateLimit = require("express-rate-limit");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10 // Adjust as needed
});

const ordersRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests. Please try again later."
});

app.get("/orders", ordersRateLimiter, (req, res) => {
    const customerId = req.query.customer_id;
    const status = req.query.status;

    if (!customerId || !status) {
        return res.status(400).json({ error: 'customer_id and status are required' });
    }

    const ordersQuery = `
        SELECT id, customer_id, status, total_amount, created_at
        FROM orders
        WHERE customer_id = ? AND status = ?
        ORDER BY created_at DESC
    `;

    pool.query(ordersQuery, [customerId, status], (err, results) => {
        if (err) {
            console.error('Error retrieving orders:', err);
            return res.status(500).json({ error: 'Database error' });
        }

        res.json(results);
    });
});
