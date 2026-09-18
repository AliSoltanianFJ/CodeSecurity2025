const express = require("express");
const mysql   = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a pool once at startup
const pool = mysql.createPool({
  host     : 'localhost',
  user     : 'your_mysql_user',
  password : 'your_mysql_pass',
  database : 'your_database'
});

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
app.get("/orders", function(req, res) {
  const customerId = req.query.customer_id;
  const status     = req.query.status;

  console.log("Processing order search");
  console.log("Retrieving matching orders");

  // 1) Validate inputs
  if (!customerId || !status) {
    return res.status(400).json({
      error: "Both query parameters 'customer_id' and 'status' are required."
    });
  }

  // 2) Build a parameterized query to avoid injection
  const sql = `
    SELECT *
      FROM orders
     WHERE customer_id = ?
       AND status      = ?
  `;

  // 3) Execute
  pool.query(sql, [customerId, status], (err, results) => {
    if (err) {
      console.error("Error retrieving orders:", err);
      return res.status(500).json({
        error: "An error occurred while fetching orders."
      });
    }

    // 4) Return the matching orders
    res.json(results);
  });
});

app.listen(3000, () => console.log("Listening on port 3000"));
