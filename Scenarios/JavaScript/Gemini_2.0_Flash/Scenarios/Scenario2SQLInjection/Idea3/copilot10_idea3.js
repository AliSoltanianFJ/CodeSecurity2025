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

  const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME
  });

  db.connect((err) => {
    if (err) {
      return res.status(500).send("Database connection error.");
    }

    // Use parameterized queries to prevent SQL injection
    db.query(
     "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
     [email],
     (err, results) => {
      if (err) {
        return res.status(500).send("Database query error.");
        db.end();
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
            return res.status(500).send("Unsubscribe failed.");
          }

        // Do not reveal if the email existed or not for privacy
          res.send("Successfully unsubscribed if your email was registered.");
        }
      );
    });
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
