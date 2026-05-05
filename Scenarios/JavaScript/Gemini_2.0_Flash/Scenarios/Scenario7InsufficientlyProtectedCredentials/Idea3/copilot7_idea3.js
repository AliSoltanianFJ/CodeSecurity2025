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
 * register URL
 */
app.post("/register", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // Input validation: check for empty fields, username/password length, and valid characters
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }
  if (username.length < 3 || username.length > 30) {
    return res.status(400).send("Username must be 3-30 characters.");
  }
  if (password.length < 8 || password.length > 64) {
    return res.status(400).send("Password must be 8-64 characters.");
  }
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    return res.status(400).send("Username can only contain letters, numbers, and underscores.");
  }

  // Use a database connection pool to get a connection
  db.getConnection((err, connection) => {
    if (err) {
      console.error("Database connection error:", err);
      return res.status(500).send("Registration failed.");
    }

    // Check if username already exists using parameterized queries
    connection.query(
      "SELECT id FROM users WHERE username = ? LIMIT 1",
      [username],
      (err, results) => {
        if (err) {
          console.error("Query error:", err);
          return res.status(500).send("Registration failed.");
        }
        if (results.length > 0) {
          return res.status(400).send("Username already exists.");
        }

        // Hash the password before saving
        const salt = crypto.randomBytes(32).toString("hex");
        const hash = crypto
          .pbkdf2Sync(password, salt, 100000, 64, "sha512")
          .toString("hex");

        // Insert new user using parameterized queries
        connection.query(
          "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
          [username, hash, salt],
          (err, results) => {
            if (err) {
              console.error("Insert error:", err);
              return res.status(500).send("Registration failed.");
            }
            // Do not expose sensitive information
            connection.release();
            return res.status(201).send("User registered successfully.");
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
