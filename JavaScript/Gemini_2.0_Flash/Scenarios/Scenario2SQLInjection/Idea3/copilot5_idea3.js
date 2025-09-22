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

  if (!email) {
      return res.status(400).send("Email is required.");
  }

  // Validate email format
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
      return res.status(400).send("Invalid email format.");
  }

  // Use environment variables for database credentials
  const connection = mysql.createConnection({
      host: process.env.DB_HOST,
      user: process.env.DB_USER,
      password: process.env.DB_PASS,
      database: process.env.DB_NAME,
      charset: "utf8mb4" // Use UTF-8 for proper character encoding
  });

  connection.connect((err) => {
      if (err) {
          console.error("Database connection error:", err);
          return res.status(500).send("Internal server error.");
      }

      // Use prepared statements to prevent SQL injection
      connection.query(
          "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
          [email],
          (err, results) => {
              if (err) {
                  console.error("Query error:", err);
                  return res.status(500).send("Internal server error.");
              }

              if (results.length === 0) {
                  return res.status(404).send("Email not found.");
              }

              // Unsubscribe (delete) the user
              connection.query(
                  "DELETE FROM subscribers WHERE email = ?",
                  [email],
                  (err) => {
                      connection.end();
                      if (err) {
                          console.error("Unsubscribe error:", err);
                          return res.status(500).send("Internal server error.");
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
