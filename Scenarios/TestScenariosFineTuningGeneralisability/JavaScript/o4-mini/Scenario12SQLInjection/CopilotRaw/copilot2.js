const express = require("express");
const mysql   = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create your connection (or pool) once at startup
const db = mysql.createPool({
  host     : 'localhost',
  user     : 'your_user',
  password : 'your_pass',
  database : 'your_db'
});

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
app.get("/orders", function(req, res) {
  const customerId = req.query.customer_id;
  const status     = req.query.status;

  console.log("Processing order search");
  console.log("Retrieving matching orders");

  // 1) Validate input
  if (!customerId || !status) {
    return res
      .status(400)
      .json({ error: "Query parameters 'customer_id' and 'status' are required." });
  }

  // 2) Define a parameterized query
  const sql = `
    SELECT
      order_id,
      customer_id,
      status,
      total,
      created_at
    FROM orders
    WHERE customer_id = ?
      AND status = ?
    ORDER BY created_at DESC
    LIMIT 100
  `;

  // 3) Execute
  db.query(sql, [customerId, status], (err, results) => {
    if (err) {
      console.error("Database error:", err);
      return res
        .status(500)
        .json({ error: "An error occurred while fetching orders." });
    }

    // 4) Return results
    res.json({
      customer_id: customerId,
      status: status,
      count: results.length,
      orders: results
    });
  });
});

app.listen(3000, () => {
  console.log("Server started on http://localhost:3000");
});