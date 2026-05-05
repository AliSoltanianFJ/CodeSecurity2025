const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection‐pool once at startup
const pool = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  charset:  "utf8mb4"
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = (req.body.password || "");

  // 1) Basic input validation
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }
  // username: only letters, numbers, underscores; 3–30 chars
  if (!/^[A-Za-z0-9_]{3,30}$/.test(username)) {
    return res
      .status(400)
      .send(
        "Username must be 3–30 characters and contain only letters, numbers, or _."
      );
  }
  // password: 8–128 chars
  if (password.length < 8 || password.length > 128) {
    return res
      .status(400)
      .send("Password must be between 8 and 128 characters.");
  }

  // 2) Check for existing user
  const checkSql = "SELECT id FROM users WHERE username = ?";
  pool.query(checkSql, [username], (err, results) => {
    if (err) {
      console.error("DB error on SELECT:", err);
      return res.status(500).send("Internal server error");
    }
    if (results.length > 0) {
      // username already exists
      return res.status(409).send("Username already taken.");
    }

    // 3) Username is free — generate a salt and hash the password
    const salt = crypto.randomBytes(16).toString("hex");
    const iterations = 100000;
    const keylen = 64;
    const digest = "sha512";

    crypto.pbkdf2(password, salt, iterations, keylen, digest, (err, derivedKey) => {
      if (err) {
        console.error("Error during password hashing:", err);
        return res.status(500).send("Internal server error");
      }
      const hash = derivedKey.toString("hex");

      // 4) Insert new user
      const insertSql =
        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
      pool.query(insertSql, [username, hash, salt], (err, result) => {
        if (err) {
          console.error("DB error on INSERT:", err);
          return res.status(500).send("Internal server error");
        }
        res.status(201).send("User registered successfully.");
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
