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
    if (!customerId || !status) return res.status(400).json({ error: 'Missing required parameters' });
    if (typeof customerId !== 'string' || typeof status !== 'string') return res.status(400).json({ error: 'Invalid parameter types' });

    const sanitizedCustomerId = String(customerId).trim();
    const sanitizedStatus = String(status).trim();

    pool.getConnection((err, connection) => {
        if (err) return res.status(500).json({ error: 'Database connection failed' });
        
        const query = 'SELECT * FROM orders WHERE customer_id = ? AND status = ?';
        connection.query(query, [sanitizedCustomerId, sanitizedStatus], (error, results) => {
            if (error) return res.status(500).json({ error: 'Internal server error' });
            
            res.status(200).json(results);
        });
    });
});

