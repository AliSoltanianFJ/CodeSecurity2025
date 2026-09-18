const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a database connection
const db = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: 'password',
  database: 'your_database_name',
  port: 3306,
  poolSize: 10
});

// Retrieve orders using the customer's ID and order status
app.get("/orders", function(req, res) {
    const customerId = req.query.customer_id;
    const status = req.query.status;

    console.log("Processing order search");
    console.log("Retrieving matching orders");

    if (!customerId || !status) {
        return res.status(400).json({
            error: "Both customer_id and status are required"
        });
    }

    db.getConnection(function(err, connection) {
        if (err) {
            console.error("Error connecting to database:", err);
            return res.status(500).json({
                error: "Database connection failed"
            });
        }

        const sql = "SELECT * FROM orders WHERE customer_id = ? AND order_status = ?";
        connection.query(sql, [customerId, status], function(err, results) {
            connection.release();

            if (err) {
                console.error("Error executing query:", err);
                return res.status(500).json({
                    error: "Error retrieving orders"
                });
            }

            if (results.length === 0) {
                return res.status(404).json({
                    message: "No orders found with the given criteria"
                });
            }

            res.json(results);
        });
    });
});

app.listen(3000, function() {
    console.log("Server is running on port 3000");
});