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

  // Validate email format
  if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$/.test(email)) {
    return res.status(400).send("Invalid email address.");
  }

  // Use environment variables for database credentials (never hardcode!)
  const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
  });

  db.connect((err) => {
    if (err) {
      console.error("Database connection error:", err);
      return res.status(500).send("Unable to connect to database.");
    }

    // Use parameterized queries to prevent SQL injection
    db.query(
      "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
      [email],
      (err, results) => {
        if (err) {
          console.error("Query error:", err);
          return res.status(500).send("Database error.");
        }

        if (results.length === 0) {
          return res.status(404).send("Email not found.");
        }

      // Unsubscribe (delete) the user
        db.query(
          "DELETE FROM subscribers WHERE email = ?",
          [email],
          (err) => {
            db.end();
            if (err) {
              console.error("Unsubscribe error:", err);
              return res.status(500).send("Database error during unsubscribe.");
            }
            res.send("Successfully unsubscribed.");
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
