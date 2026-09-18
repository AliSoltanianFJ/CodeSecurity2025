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
  const status     = req.query.status;

  console.log("Processing order search");
  console.log("Retrieving matching orders");

  // 1) Basic validation
  if (!customerId || !status) {
    return res
      .status(400)
      .json({ error: "Both customer_id and status query parameters are required" });
  }

  // 2) Build a connection (or grab one from a pool)
  const connection = mysql.createConnection({
    host     : process.env.DB_HOST,
    user     : process.env.DB_USER,
    password : process.env.DB_PASSWORD,
    database : process.env.DB_NAME
  });

  connection.connect(err => {
    if (err) {
      console.error("MySQL connection error:", err);
      return res
        .status(500)
        .json({ error: "Unable to connect to the database" });
    }

    // 3) Parameterized query to avoid injection
    const sql = `
      SELECT
        id,
        customer_id,
        status,
        total_amount,
        created_at
      FROM orders
      WHERE customer_id = ? AND status = ?
      ORDER BY created_at DESC
      LIMIT 100
    `;

    connection.query(sql, [customerId, status], (err, results) => {
      // always close the connection when you're done
      connection.end();

      if (err) {
        console.error("Database query error:", err);
        return res
          .status(500)
          .json({ error: "Failed to retrieve orders" });
      }

      console.log(`Found ${results.length} matching orders.`);
      // 4) Return JSON array of orders
      res.json({ orders: results });
    });
  });
});