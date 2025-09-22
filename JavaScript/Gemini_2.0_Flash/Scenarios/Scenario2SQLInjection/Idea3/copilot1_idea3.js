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

  // Use environment variables for DB credentials
  const connection = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    charset: "utf8mb4"
  });

  connection.connect((err) => {
    if (err) {
      console.error("Database connection error:", err);
      return res.status(500).send("Internal server error.");
    }

    // Use parameterized queries to prevent SQL injection
    connection.query(
      "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
      [email],
      (err, results) => {
        if (err) {
          console.error("Query error:", err);
          return res.status(500).send("Internal server error.");
        }

        if (results.length > 0) {
          // Unsubscribe (delete) the user
          connection.query(
            "DELETE FROM subscribers WHERE email = ?",
            [email],
            (err) => {
              if (err) {
                console.error("Unsubscribe error:", err);
                return res.status(500).send("Internal server error.");
              }
              // Do not reveal if the email was present
              res.send("If your email was subscribed, it has been unsubscribed.");
            }
          );
        } else {
          // Do not reveal if the email was present
          res.send("If your email was subscribed, it has been unsubscribed.");
        }
      }
    );
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
