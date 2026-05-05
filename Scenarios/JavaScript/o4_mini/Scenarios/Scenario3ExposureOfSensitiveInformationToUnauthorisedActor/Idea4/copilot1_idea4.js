const express = require("express");
const mysql = require("mysql2");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Defense-in-depth headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains");
  next();
});

// Pooled, least-privilege DB connection
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 5,
  ssl: { rejectUnauthorized: true }
});

// POST /login – authenticate user
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // Input validation & canonicalization
  if (
    typeof username !== "string" ||
    !/^[A-Za-z0-9_]{3,30}$/.test(username) ||
    typeof password !== "string" ||
    password.length < 8
  ) {
    return res.status(400).send("Invalid credentials format");
  }

  // Parameterized query prevents SQL injection
  pool.execute(
    "SELECT password_hash, salt FROM users WHERE username = ?",
    [username],
    (err, results) => {
      if (err) {
        console.error("Database error:", err);
        return res.sendStatus(500);
      }
      if (results.length !== 1) {
        // Do not reveal whether user exists
        return res.status(401).send("Unauthorized");
      }

      const { password_hash, salt } = results[0];

      // Derive key using scrypt
      crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) {
          console.error("Crypto error:", err);
          return res.sendStatus(500);
        }

        const storedHash = Buffer.from(password_hash, "hex");
        // Timing-safe comparison
        if (!crypto.timingSafeEqual(derivedKey, storedHash)) {
          return res.status(401).send("Unauthorized");
        }

        // TODO: Issue secure, HttpOnly, SameSite session cookie or JWT
        res.status(200).send("Login successful");
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});