const express = require("express");
const mysql = require("mysql");
const bcrypt = require("bcrypt");
const rateLimit = require("express-rate-limit");
const { body, validationResult } = require("express-validator");

const app = express();
app.use(express.urlencoded({ extended: false }));
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Pool with least-privilege user; creds via env vars
const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});

// Rate limiter to mitigate brute-force
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: "Too many login attempts, please try again later."
});

/**
 * Log in the user if the username and password are correct
 */
app.post(
  "/login",
  loginLimiter,
  [
    body("username").isAlphanumeric().trim().escape(),
    body("password").isLength({ min: 8 }).trim()
  ],
  (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: "Invalid input." });
    }

    const { username, password } = req.body;

    // Parameterized query prevents SQL injection
    pool.query(
      "SELECT id, password_hash FROM users WHERE username = ?",
      [username],
      (err, results) => {
        if (err) {
          console.error("DB error", err);
          return res.status(500).json({ error: "Internal server error." });
        }
        if (results.length !== 1) {
          return res.status(401).json({ error: "Invalid credentials." });
        }

        const { id, password_hash } = results[0];
        bcrypt.compare(password, password_hash, (bcryptErr, same) => {
          if (bcryptErr) {
            console.error("Hash compare error", bcryptErr);
            return res.status(500).json({ error: "Internal server error." });
          }
          if (!same) {
            return res.status(401).json({ error: "Invalid credentials." });
          }

          // On success: issue session cookie or JWT (HttpOnly, Secure, SameSite)
          res
            .cookie("session", /* generate secure token */ {
              httpOnly: true,
              secure: true,
              sameSite: "Strict"
            })
            .status(200)
            .json({ message: "Login successful." });
        });
      }
    );
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});