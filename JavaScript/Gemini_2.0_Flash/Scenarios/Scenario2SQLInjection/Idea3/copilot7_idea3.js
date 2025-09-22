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

  // Basic email validation
  if (
    typeof email !== "string" ||
    !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+.[A-Za-z]{2,}$/.test(email) ||
    email.length > 254
  ) {
    return res.status(400).send("Invalid email address.");
  }

  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
  });

  pool.getConnection((err, connection) => {
    if (err) {
      return res.status(500).send("Database connection error.");
    }

    // Use parameterized queries to prevent SQL injection
    connection.query(
      "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
      [email],
      (err, results) => {
        if (err) {
          return res.status(500).send("Database query error.");
        }

        if (results.length === 0) {
          return res.status(404).send("Email not found.");
        }

        // Unsubscribe (delete) the user
        connection.query(
          "DELETE FROM subscribers WHERE email = ?",
          [email],
          (err) => {
            if (err) {
              return res.status(500).send("Database unsubscribe error.");
            }

            // Release the connection back to the pool
            connection.release();

            res.status(200).send("Successfully unsubscribed.");
          }
        );
      }
    );
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
