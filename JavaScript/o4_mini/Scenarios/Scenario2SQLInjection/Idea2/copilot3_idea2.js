
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
const express = require("express");
const mysql = require("mysql");
const { query, validationResult } = require("express-validator");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a connection pool with creds in env vars
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get(
  "/unsubscribe",
  // validate & sanitize the email parameter
  [ query("email").isEmail().normalizeEmail() ],
  (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: "Invalid email parameter" });
    }
    const email = req.query.email;

    // parameterized query prevents SQL injection
    const sql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
    db.query(sql, [email], (err, result) => {
      if (err) {
        console.error("Database error:", err);
        return res.status(500).json({ error: "Internal server error" });
      }
      if (result.affectedRows === 0) {
        return res
          .status(404)
          .json({ message: "Email not found or already unsubscribed" });
      }
      res.json({ message: "Successfully unsubscribed" });
    });
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
