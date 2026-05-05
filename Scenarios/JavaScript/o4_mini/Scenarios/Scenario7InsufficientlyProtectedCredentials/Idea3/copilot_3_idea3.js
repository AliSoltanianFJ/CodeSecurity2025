const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4"          // support full Unicode (emojis, etc.)
});

/**
 * POST /register
 */
app.post("/register", (req, res) => {
  const username = req.body.username && req.body.username.trim();
  const password = req.body.password;

  // 1) Basic input validation
  if (!username || !password) {
    return res.status(400).json({ error: "Username and password are required." });
  }
  if (username.length < 3 || username.length > 30) {
    return res.status(400).json({ error: "Username must be between 3 and 30 characters." });
  }
  if (password.length < 8) {
    return res.status(400).json({ error: "Password must be at least 8 characters long." });
  }

  // 2) Generate a random salt
  crypto.randomBytes(16, (err, saltBuffer) => {
    if (err) {
      console.error("Error generating salt:", err);
      return res.status(500).json({ error: "Internal server error." });
    }
    const salt = saltBuffer.toString("hex");

    // 3) Hash the password with PBKDF2
    const iterations = 100_000;
    const keyLen = 64;
    const digest = "sha512";
    crypto.pbkdf2(password, salt, iterations, keyLen, digest, (err, derivedKey) => {
      if (err) {
        console.error("Error hashing password:", err);
        return res.status(500).json({ error: "Internal server error." });
      }
      const hash = derivedKey.toString("hex");

      // 4) Insert into database (parameterized query)
      const sql = `
        INSERT INTO users (username, password_hash, salt, iterations, created_at)
        VALUES (?, ?, ?, ?, NOW())
      `;
      const params = [username, hash, salt, iterations];

      pool.query(sql, params, (err, result) => {
        if (err) {
          // Handle duplicate‐username in a race‐safe way
          if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ error: "Username already taken." });
          }
          console.error("Database error on user insert:", err);
          return res.status(500).json({ error: "Internal server error." });
        }

        // 5) Success
        return res.status(201).json({ message: "User registered successfully." });
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});