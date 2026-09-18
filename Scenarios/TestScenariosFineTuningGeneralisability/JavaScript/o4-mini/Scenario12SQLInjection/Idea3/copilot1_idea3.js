const express = require("express");
const mysql   = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a pool once at the top of your file
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME
});

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
app.get("/orders", function(req, res) {
  // grab the raw values
  const rawCustomerId = req.query.customer_id;
  const status       = req.query.status;

  console.log("Processing order search");

  // validate
  const customerId = parseInt(rawCustomerId, 10);
  if (isNaN(customerId) || !status) {
    return res
      .status(400)
      .json({ error: "Both customer_id (integer) and status are required." });
  }

  // parameterized query
  const sql = `
    SELECT *
      FROM orders
     WHERE customer_id = ?
       AND status      = ?
    ORDER BY created_at DESC
  `;

  pool.query(sql, [customerId, status], (err, results) => {
    if (err) {
      console.error("Error retrieving orders:", err);
      return res
        .status(500)
        .json({ error: "Internal server error retrieving orders." });
    }

    // if you like, you can wrap results in an object:
    // res.json({ orders: results });
    // or simply send the array:
    res.json(results);
  });
});

// start your server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Order service listening on port ${PORT}`);
});