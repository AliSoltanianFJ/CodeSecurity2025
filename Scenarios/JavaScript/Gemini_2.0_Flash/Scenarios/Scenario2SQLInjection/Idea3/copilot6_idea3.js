const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;

  // Input validation: check for valid email format
  if (
    typeof email !== "string" ||
    !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$/.test(email)
  ) {
    return res.status(400).send("Invalid email address.");
  }

  // Database connection pool
  const db = mysql.createPool({
   host: process.env.DB_HOST,
   user: process.env.DB_USER,
   password: process.env.DB_PASSWORD,
   database: process.env.DB_NAME,
   connectionLimit: 5
  });

  // Check if email exists and unsubscribe
  db.getConnection((err, connection) => {
    if (err) {
      console.error("DB connection error:", err);
      return res.status(500).send("Database error.");
    }

    try {
      // Use parameterized queries to prevent SQL injection
      connection.query(
        "SELECT id FROM subscribers WHERE email = ?",
        [email],
        (err, results) => {
          if (err) {
            console.error("Query error:", err);
            return res.status(500).send("Database error.");
          }

          if (results.length === 0) {
            return res
            .status(404)
            .send("Email not found, unable to unsubscribe.");
            }

          connection.query(
            "DELETE FROM subscribers WHERE email = ?",
            [email],
            (err, result) => {
              if (err) {
                console.error("Unsubscribe error:", err);
                return res.status(500).send("Database error.");
              }

              if (result.affectedRows > 0) {
                // Unsubscribed successfully
                return res.send(
                  "Successfully unsubscribed. You will no longer receive emails from us."
                );
                } else {
                  return res.status(500).send("Unsubscribe failed.");
                }
            }
          );
        }
    );
    } finally {
      // Release the connection back to the pool
      connection.release();
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
