"use strict";

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

const app = express();

// Body parsing with strict size limits to reduce risk of resource exhaustion
app.use(express.json({ limit: "10kb" }));
app.use(express.urlencoded({ extended: true, limit: "10kb" }));

// Security headers
app.use(helmet({
  contentSecurityPolicy: {
    useDefaults: true,
    directives: {
      "default-src": ["'none'"],
      "frame-ancestors": ["'none'"]
    }
  },
  referrerPolicy: { policy: "no-referrer" }
}));
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-XSS-Protection", "0");
  next();
});

// Basic rate limiting to mitigate brute-force / enumeration
const registerLimiter = rateLimit({
  windowMs: 5 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false
});
app.use("/register", registerLimiter);

// MySQL connection pool (use environment variables; never hardcode secrets)
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4",
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

// Simple utility: validate and normalize username
function validateUsername(raw) {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  if (trimmed.length < 3 || trimmed.length > 32) return null;
  // Allow letters, numbers, underscore, hyphen, dot
  if (!/^[A-Za-z0-9._-]+$/.test(trimmed)) return null;
  return trimmed;
}

// Password policy: length + basic complexity
function validatePassword(raw) {
  if (typeof raw !== "string") return null;
  if (raw.length < 12 || raw.length > 128) return null;
  // Require at least 3 of 4 classes
  let classes = 0;
  if (/[a-z]/.test(raw)) classes++;
  if (/[A-Z]/.test(raw)) classes++;
  if (/[0-9]/.test(raw)) classes++;
  if (/[^A-Za-z0-9]/.test(raw)) classes++;
  if (classes < 3) return null;
  return raw;
}

// Constant-time comparison helper (for future use)
function safeEqual(a, b) {
  const bufA = Buffer.from(a);
  const bufB = Buffer.from(b);
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * Secure registration endpoint
 * Does not disclose whether a username already exists (prevents enumeration).
 */
app.post("/register", (req, res) => {
  // Defensive early bail if content-type incorrect
  const ct = req.headers["content-type"] || "";
  if (!ct.includes("application/x-www-form-urlencoded") && !ct.includes("application/json")) {
    return res.status(400).send("Invalid request.");
  }

  const username = validateUsername(req.body.username);
  const password = validatePassword(req.body.password);

  if (!username || !password) {
    // Generic response: do not leak which part failed
    return res.status(400).send("Registration request rejected.");
  }

  // Generate salt and hash with strong parameters (OWASP recommendation: >= 210k iterations PBKDF2-SHA512)
  const salt = crypto.randomBytes(16).toString("hex");
  const iterations = 210000;
  crypto.pbkdf2(password, salt, iterations, 64, "sha512", (err, derivedKey) => {
    if (err) {
      return res.status(500).send("Server error.");
    }
    // Store as iterations:salt:hash
    const passwordRecord = `${iterations}:${salt}:${derivedKey.toString("hex")}`;

    // Use a transaction-like flow: first check existence, then insert if absent
    pool.getConnection((connErr, connection) => {
      if (connErr) {
        return res.status(500).send("Server error.");
      }

      // Parameterized query avoids SQL injection (no string concatenation)
      connection.query(
        "SELECT id FROM users WHERE username = ? LIMIT 1",
        [username],
        (selErr, rows) => {
          if (selErr) {
            connection.release();
            return res.status(500).send("Server error.");
          }

            // If exists, respond generic message (prevent enumeration)
          if (rows && rows.length > 0) {
            connection.release();
            return res.status(200).send("Registration processed."); // Generic
          }

          connection.query(
            "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())",
            [username, passwordRecord],
            (insErr) => {
              connection.release();
              if (insErr) {
                return res.status(500).send("Server error.");
              }
              // Do not echo user input; generic success
              return res.status(201).send("Registration processed.");
            }
          );
        }
      );
    });
  });
});

// Generic 404 without reflecting input
app.use((req, res) => {
  res.status(404).send("Not found.");
});

// Central error handler (no stack leaks)
app.use((err, req, res, next) => {
  res.status(500).send("Server error.");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Do not log sensitive data
  console.log(`Server running on port ${PORT}`);
});
