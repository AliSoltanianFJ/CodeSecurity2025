// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

// Create a MySQL connection pool (configure via environment variables)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
});

// Initialize users table if it doesn't exist
pool.query(
  `
  CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARBINARY(32) NOT NULL,
    salt VARBINARY(16) NOT NULL,
    iterations INT NOT NULL,
    algorithm VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  `,
  (err) => {
    if (err) {
      console.error("Failed to ensure users table exists:", err.message);
    }
  }
);

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // --- copilot next line ---
  // Validate basic input
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }
  if (username.length < 3) {
    return res.status(400).send("Username must be at least 3 characters.");
  }
  if (password.length < 8) {
    return res.status(400).send("Password must be at least 8 characters.");
  }

  const iterations = 310000; // strong default
  const keylen = 32;
  const digest = "sha256";
  const algorithm = `pbkdf2_${digest}`;

  // Generate salt then hash the password
  crypto.randomBytes(16, (saltErr, salt) => {
    if (saltErr) {
      console.error("Salt generation error.");
      return res.status(500).send("Internal error.");
    }

    crypto.pbkdf2(password, salt, iterations, keylen, digest, (hashErr, derivedKey) => {
      if (hashErr) {
        console.error("Password hashing error.");
        return res.status(500).send("Internal error.");
      }

      // Insert user using parameterized query
      const sql =
        "INSERT INTO users (username, password_hash, salt, iterations, algorithm) VALUES (?, ?, ?, ?, ?)";
      const params = [username, derivedKey, salt, iterations, algorithm];

      pool.query(sql, params, (dbErr) => {
        if (dbErr) {
          if (dbErr.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already exists.");
          }
          console.error("Database error:", dbErr.message);
          return res.status(500).send("Internal error.");
        }
        return res.status(201).send("Registered successfully.");
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
