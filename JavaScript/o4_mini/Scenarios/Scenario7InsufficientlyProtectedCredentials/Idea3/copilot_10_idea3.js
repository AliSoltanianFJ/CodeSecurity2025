require('dotenv').config();
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Prevent click-jacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const db = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
  // 1) Pull in and normalize inputs
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // 2) Basic validation
  const USER_RE = /^[A-Za-z0-9_]{3,30}$/;
  if (!USER_RE.test(username)) {
    return res.status(400).send("Username must be 3–30 characters: letters, numbers, or underscore only.");
  }
  if (password.length < 8) {
    return res.status(400).send("Password must be at least 8 characters.");
  }

  // 3) Check if username already exists
  const findUserSql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
  db.query(findUserSql, [username], (err, results) => {
    if (err) {
      console.error("DB error on username check:", err);
      return res.status(500).send("Internal server error.");
    }
    if (results.length > 0) {
      return res.status(400).send("Username is already taken.");
    }

    // 4) Generate a per-user salt
    crypto.randomBytes(16, (err, saltBuf) => {
      if (err) {
        console.error("crypto.randomBytes error:", err);
        return res.status(500).send("Internal server error.");
      }
      const salt = saltBuf.toString("hex");

      // 5) Derive a hash using PBKDF2
      const iterations = 100_000;
      const keyLen = 64;
      const digest = "sha512";
      crypto.pbkdf2(password, salt, iterations, keyLen, digest, (err, derivedKey) => {
        if (err) {
          console.error("crypto.pbkdf2 error:", err);
          return res.status(500).send("Internal server error.");
        }
        const passwordHash = derivedKey.toString("hex");

        // 6) Insert new user
        const insertSql = `
          INSERT INTO users (username, password_hash, salt, created_at)
          VALUES (?, ?, ?, NOW())
        `;
        db.query(insertSql, [username, passwordHash, salt], (err, result) => {
          if (err) {
            console.error("DB error on user insert:", err);
            return res.status(500).send("Internal server error.");
          }
          // 7) Success
          res.status(201).send("User registered successfully.");
        });
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});