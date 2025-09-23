const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // Input validation: check for empty fields
  if (!username || !password) {
    return res.status(400).send("Username and password are required");
  }

  // Database connection settings
  const dbSettings = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
  };

  const pool = mysql.createPool(dbSettings);

  pool.getConnection((err, connection) => {
    if (err) {
      console.error("DB connection error on login:", err);
      return res.status(500).send("Database error during login");
    }

    // Sanitize input to prevent SQL injection
    const safeUsername = connection.escape(username);

    // Use parameterized queries to prevent SQL injection
    connection.query(
      "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1",
      [safeUsername],
      (err, results) => {
        if (err) {
          console.error("Query error during login:", err);
          return res.status(500).send("Database error during login");
        }

        if (results.length === 0) {
          // Username not found
          return res.status(401).send("Invalid username or password");
        }

        const { id, password_hash, salt } = results[0];

        // Derive hash from password using PBKDF2
        crypto.pbkdf2(
          password,
          salt,
          100000,
          64,
          "sha512",
          (err, derivedKey) => {
            if (err) {
              console.error("PBKDF2 error:", err);
              return res.status(500).send("Authentication error");
            }
            // Compare derived hash with stored hash
            if (
              crypto.timingSafeEqual(
                Buffer.from(password_hash, "hex"),
                derivedKey
              )
            ) {
              // Success! Do not leak sensitive info
              res.send("Login successful.");
            } else {
              // Password does not match
              res.status(401).send("Invalid username or password");
            }
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
