const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Parse URL-encoded form bodies and JSON
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Minimal security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// MySQL connection pool
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 10,
  multipleStatements: false,
});

// Create users table if it does not exist
pool.query(
  `
  CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARBINARY(64) NOT NULL,
    password_salt VARBINARY(16) NOT NULL,
    iterations INT NOT NULL,
    algorithm VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
`,
  (err) => {
    if (err) {
      console.error("Failed to ensure users table exists:", err.message);
    } else {
      console.log("Users table is ready.");
    }
  }
);

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  // Basic input validation
  if (!username || !password) {
    return res.status(400).json({ error: "username and password are required" });
  }
  if (username.length < 3 || username.length > 64) {
    return res.status(400).json({ error: "invalid username length" });
  }
  if (!/^[A-Za-z0-9_.\-@]+$/.test(username)) {
    return res.status(400).json({ error: "invalid username characters" });
  }
  if (password.length < 8) {
    return res.status(400).json({ error: "password must be at least 8 characters" });
  }

  const salt = crypto.randomBytes(16);
  const iterations = 310000; // NIST/OWASP-aligned
  const keyLen = 64; // 512-bit
  const digest = "sha512";
  const algorithm = "pbkdf2_sha512";

  crypto.pbkdf2(password, salt, iterations, keyLen, digest, (err, derivedKey) => {
    if (err) {
      console.error("Error hashing password:", err.message);
      return res.status(500).json({ error: "internal error" });
    }

    const sql =
      "INSERT INTO users (username, password_hash, password_salt, iterations, algorithm) VALUES (?, ?, ?, ?, ?)";
    const params = [username, derivedKey, salt, iterations, algorithm];

    pool.query(sql, params, (dbErr) => {
      if (dbErr) {
        // Handle duplicate username
        if (dbErr.code === "ER_DUP_ENTRY") {
          return res.status(409).json({ error: "username already exists" });
        }
        console.error("Database error during registration:", dbErr.message);
        return res.status(500).json({ error: "internal error" });
      }

      return res.status(201).json({ message: "registered" });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
