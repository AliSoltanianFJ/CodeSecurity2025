const express = require("express");
const mysql   = require("mysql");

const app = express();

// create a pool once, reuse for all requests
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : "localhost",
  user            : "your_db_user",
  password        : "your_db_password",
  database        : "your_db_name"
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
app.get("/orders", function(req, res) {
  const customerId = req.query.customer_id;
  const status     = req.query.status;

  console.log("Processing order search");
  console.log("Retrieving matching orders");

  // 1) validate inputs
  if (!customerId || !status) {
    return res
      .status(400)
      .json({ error: "Missing required query parameters: customer_id and status" });
  }

  // 2) parameterized query to avoid SQL injection
  const sql = `
    SELECT *
      FROM orders
     WHERE customer_id = ?
       AND status      = ?
  `;

  pool.query(sql, [customerId, status], (err, results) => {
    if (err) {
      console.error("Error querying orders:", err);
      return res.status(500).json({ error: "Database query failed" });
    }

    // 3) return the array of matching orders (could be empty)
    res.json(results);
  });
});

app.listen(3000, () => {
  console.log("Server listening on port 3000");
});
