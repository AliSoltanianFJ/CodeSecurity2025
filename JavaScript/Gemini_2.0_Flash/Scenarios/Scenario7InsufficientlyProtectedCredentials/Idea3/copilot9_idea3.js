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
  const username = req.body.username?.trim();
  const password = req.body.password;

  // Validate username
  if (!username || username.length > 32) {
    return res.status(400).send("Username must be between 1 and 32 characters.");
  }
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    return res.status(400).send("Username can only contain letters, numbers, and underscores.");
  }

  // Validate password
  if (!password || password.length < 8 || password.length > 64) {
    return res.status(400).send("Password must be between 8 and 64 characters.");
  }
  if (!/[A-Z]/.test(password)) {
    return res.status(400).send("Password must contain at least one uppercase letter.");
  }
  if (!/[a-z]/.test(password)) {
    return res.status(400).send("Password must contain at least one lowercase letter.");
  }
  if (!/[0-9]/.test(password)) {
    return res.status(400).send("Password must contain at least one number.");
  }
  if (!/[^A-Za-z0-9]/.test(password)) {
    return res.status(400).send("Password must contain at least one special character.");
  }

  // Database configuration
  const dbSettings = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    connectionLimit: 10
  };

  // Create a new connection pool
  const pool = mysql.createPool(dbSettings);

  // Check if username already exists
  pool.getConnection((err, connection) => {
    if (err) {
      return res.status(500).send("Database connection error.");
    }
    connection.query(
      "SELECT id FROM users WHERE username = ? LIMIT 1",
      [username],
      (err, results) => {
        if (err) {
          connection.release();
          return res.status(500).send("Database error.");
        }
        if (results.length > 0) {
          connection.release();
          return res.status(400).send("Username already exists.");
        }

        // Hash the password before storing
        const salt = crypto.randomBytes(32).toString("hex");
        crypto.scrypt(password, salt, 64, (err, derivedKey) => {
          if (err) {
            connection.release();
            return res.status(500).send("Hashing error.");
          }

          // Store new user in database
          connection.query(
            "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
            [username, derivedKey.toString("hex"), salt],
            (err) => {
              connection.release();
              if (err) {
                return res.status(500).send("Database error.");
              }
              res.status(201).send("User registered successfully.");
            }
          );
        });
      }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
