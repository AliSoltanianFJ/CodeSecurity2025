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

    // Create a database connection
    const db = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: 'password',
        database: 'your_database'
    });

    // Connect to the database
    db.connect((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            res.status(500).json({ error: 'Database connection failed' });
            return;
        }

        // SQL query to retrieve orders based on customer_id and status
        const sql = `
            SELECT * FROM orders 
            WHERE customer_id = ?
              AND order_status = ?
        `;

        // Execute the query
        db.query(sql, [customerId, status], (error, results) => {
            if (error) {
                console.error('Error executing query:', error);
                res.status(500).json({ error: 'Error retrieving orders' });
            } else {
                res.status(200).json(results);
            }
            // Close the database connection after processing
            db.end();
        });
    });
});