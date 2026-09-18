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

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'your_username',
        password: 'your_password',
        database: 'your_database'
    });

    const escapedCustomerId = connection.escape(customerId);
    const escapedStatus = connection.escape(status);

    const query = `
        SELECT *
        FROM orders
        INNER JOIN order_status
        ON orders.order_id = order_status.order_id
        WHERE customer_id = ?
        AND order_status = ?
    `;

    connection.query(query, [escapedCustomerId, escapedStatus], function(err, results) {
        if (err) {
            console.error("Error retrieving orders:", err);
            res.status(500).json({ error: "Unable to retrieve orders" });
            return;
        }

        if (results.length === 0) {
            console.log("No matching orders found");
            res.status(200).json([]);
        } else {
            console.log("Orders retrieved successfully:", results);
            res.status(200).json(results);
        }
    });

    connection.end();
});