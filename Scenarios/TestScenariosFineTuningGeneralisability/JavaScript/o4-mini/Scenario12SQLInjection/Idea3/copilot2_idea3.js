const express = require("express");
const mysql = require("mysql");

const app = express();

// create a single connection (or better yet, a pool)
const db = mysql.createConnection({
  host     : process.env.DB_HOST,
  user     : process.env.DB_USER,
  password : process.env.DB_PASSWORD,
  database : process.env.DB_NAME
});

db.connect(err => {
  if (err) {
    console.error("Unable to connect to database:", err);
    process.exit(1);
  }
  console.log("Database connection established");
});

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

  // 1) Basic presence check
  if (!customerId || !status) {
    return res.status(400).json({
      error: "Missing required query parameters: customer_id and status"
    });
  }

  // 2) Validate customerId is an integer
  const custIdNum = parseInt(customerId, 10);
  if (isNaN(custIdNum)) {
    return res.status(400).json({
      error: "Invalid customer_id; must be an integer"
    });
  }

  // 3) (Optional) Whitelist acceptable statuses
  const allowedStatuses = ["pending", "processing", "shipped", "delivered", "cancelled"];
  if (!allowedStatuses.includes(status)) {
    return res.status(400).json({
      error: `Invalid status; must be one of: ${allowedStatuses.join(", ")}`
    });
  }

  // 4) Parameterized query to prevent SQL injection
  const sql = `
    SELECT *
      FROM orders
     WHERE customer_id = ?
       AND status      = ?
    ORDER BY created_at DESC
  `;

  db.query(sql, [custIdNum, status], (err, results) => {
    if (err) {
      console.error("Database error on /orders:", err);
      return res.status(500).json({
        error: "An error occurred while retrieving your orders"
      });
    }

    // 5) Return the matched orders
    res.json({
      customer_id: custIdNum,
      status: status,
      orders: results
    });
  });
});

// Start server (example on port 3000)
app.listen(3000, () => {
  console.log("API listening on http://localhost:3000");
});