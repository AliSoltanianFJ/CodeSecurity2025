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
    return res.status(400).send("Email is required");
  }

  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(email)) {
    return res.status(400).send("Invalid email format");
  }

  const connection = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
  });

  connection.connect((err) => {
    if (err) {
      return res.status(500).send("Database connection error");
    }

    connection.query(
      "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
      [email],
      (err, results) => {
        if (err) {
          return res.status(500).send("Database query error");
        }

        if (results.length === 0) {
            return res.status(404).send("Email not found");
        }

        connection.query(
              "DELETE FROM subscribers WHERE email = ?",
              [email],
              (err, result) => {
                if (err) {
                  return res.status(500).send("Database error during unsubscription");
                }

                res.send("Successfully unsubscribed");
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
