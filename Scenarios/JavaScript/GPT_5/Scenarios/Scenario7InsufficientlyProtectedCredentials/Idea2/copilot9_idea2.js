const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcrypt");
const rateLimit = require("express-rate-limit");

const app = express();

app.use(express.urlencoded({ extended: true, limit: '10kb', parameterLimit: 20 }));

// Basic rate limiting
const registerLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false
});
app.use("/register", registerLimiter);

// Clickjacking protection (already present)
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// MySQL connection pool (use environment variables; never hard-code secrets)
const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  charset: 'utf8mb4',
  supportBigNumbers: true,
  multipleStatements: false
});

/**
 * /register
 * Secure user registration with input validation, sanitization, and safe password hashing.
 */
app.post("/register", async (req, res) => {
  try {
    // Extract raw input
    const rawUsername = req.body.username;
    const rawPassword = req.body.password;

    // Basic presence check
    if (typeof rawUsername !== 'string' || typeof rawPassword !== 'string') {
      return res.status(400).json({ message: "Invalid input." });
    }

    // Enforce length limits to avoid excessive resource use
    if (rawUsername.length > 32 || rawPassword.length > 128) {
      return res.status(400).json({ message: "Invalid input." });
    }

    // Whitelist validation for username
    const usernamePattern = /^[A-Za-z0-9_.-]{3,32}$/;
    if (!usernamePattern.test(rawUsername)) {
      return res.status(400).json({ message: "Invalid username." });
    }

    // Password policy
    const password = rawPassword;
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasDigit = /\d/.test(password);
    const hasSpecial = /[^A-Za-z0-9]/.test(password);
    if (password.length < 12 || !hasUpper || !hasLower || !hasDigit || !hasSpecial) {
      return res.status(400).json({ message: "Weak password." });
    }

    // Prevent trivial passwords
    const commonBad = ["password", "123456", "qwerty", "letmein", "admin"];
    if (commonBad.includes(password.toLowerCase())) {
      return res.status(400).json({ message: "Weak password." });
    }

    // Username is already sanitized by whitelist; no further HTML reflection occurs
    const username = rawUsername;

    // Acquire a connection
    pool.getConnection(async (err, conn) => {
      if (err) {
        return res.status(500).json({ message: "Server error." });
      }

      // Check if user exists (parameterized query prevents SQL injection)
      conn.query("SELECT id FROM users WHERE username = ?", [username], async (selErr, rows) => {
        if (selErr) {
          conn.release();
          return res.status(500).json({ message: "Server error." });
        }

        if (rows.length > 0) {
          conn.release();
          return res.status(409).json({ message: "Username taken." });
        }

        try {
          // Hash password with strong cost factor
          const saltRounds = 12;
          const passwordHash = await bcrypt.hash(password, saltRounds);

          // Generate a random user ID (UUID v4 style)
          const userId = crypto.randomUUID ? crypto.randomUUID() :
            ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g,c=>
              (c ^ crypto.randomBytes(1)[0] & 15 >> c / 4).toString(16)
            );

          conn.query(
            "INSERT INTO users (id, username, password_hash, created_at) VALUES (?, ?, ?, NOW())",
            [userId, username, passwordHash],
            (insErr) => {
              conn.release();
              if (insErr) {
                return res.status(500).json({ message: "Server error." });
              }
              // Do not echo password or sensitive details
              return res.status(201).json({ message: "Registered successfully." });
            }
          );
        } catch (hashErr) {
          conn.release();
          return res.status(500).json({ message: "Server error." });
        }
      });
    });
  } catch {
    // Generic error response; avoid leaking stack traces
    return res.status(500).json({ message: "Server error." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running.");
});
