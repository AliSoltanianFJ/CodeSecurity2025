

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();


// Request body limits and safe parsing
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

// Minimal secure headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// Database connection pool (use environment variables; do not hardcode secrets)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  connectionLimit: 10,
  charset: "utf8mb4_general_ci",
  // Enable SSL if configured (recommended in production)
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
  supportBigNumbers: true
});

// Basic in-memory rate limiter to slow brute-force (per IP)
const loginAttempts = new Map();
const RL_WINDOW_MS = 5 * 60 * 1000; // 5 minutes
const RL_MAX_ATTEMPTS = 10;

function isRateLimited(key) {
  const now = Date.now();
  const entry = loginAttempts.get(key);
  if (!entry) return false;
  if (now - entry.first > RL_WINDOW_MS) {
    loginAttempts.delete(key);
    return false;
  }
  return entry.count >= RL_MAX_ATTEMPTS;
}

function recordAttempt(key, success) {
  const now = Date.now();
  const entry = loginAttempts.get(key);
  if (success) {
    // On success, clear attempts to avoid locking user out
    if (entry) loginAttempts.delete(key);
    return;
  }
  if (!entry || now - entry.first > RL_WINDOW_MS) {
    loginAttempts.set(key, { count: 1, first: now });
  } else {
    entry.count += 1;
  }
}

// Input validation helpers
function isValidUsername(u) {
  return (
    typeof u === "string" &&
    u.length >= 3 &&
    u.length <= 32 &&
    /^[A-Za-z0-9._-]+$/.test(u)
  );
}

function isValidPassword(p) {
  return typeof p === "string" && p.length >= 8 && p.length <= 128;
}

// Password verification using PBKDF2 (salted, iterative, constant-time compare)
// Assumes DB stores per-user salt and hash in hex columns: password_salt, password_hash
const PBKDF2_ITERATIONS = 310000;
const PBKDF2_KEYLEN = 32;
const PBKDF2_DIGEST = "sha256";

// Avoid user enumeration timing differences by computing a fake hash when user not found
function verifyPasswordWithPBKDF2(password, userRecord, cb) {
  try {
    const salt = userRecord
      ? Buffer.from(userRecord.password_salt, "hex")
      : crypto.randomBytes(16); // fake salt for non-existent user

    crypto.pbkdf2(
      password,
      salt,
      PBKDF2_ITERATIONS,
      PBKDF2_KEYLEN,
      PBKDF2_DIGEST,
      (err, derivedKey) => {
        if (err) return cb(err, false);

        if (!userRecord) {
          // No user: we still did the work; always return false
          return cb(null, false);
        }

        const storedHash = Buffer.from(userRecord.password_hash, "hex");
        if (storedHash.length !== derivedKey.length) {
          // Mismatched size; treat as invalid
          return cb(null, false);
        }

        const match = crypto.timingSafeEqual(storedHash, derivedKey);
        return cb(null, match);
      }
    );
  } catch (e) {
    return cb(e, false);
  }
}

/**
 * Log in the user if the username and password are correct.
 * - Validates and sanitizes input
 * - Uses parameterized SQL (avoids injection)
 * - Uses PBKDF2 with per-user salt and timing-safe compare
 * - Avoids reflecting user input (prevents XSS)
 * - Provides generic error messages (no info leaks)
 * - Applies rate limiting per IP
 */
app.post("/login", (req, res) => {
  res.type("application/json");

  const clientKey = req.ip || "unknown";

  if (isRateLimited(clientKey)) {
    // Do not reveal limitation details beyond generic message
    return res.status(429).json({ error: "Too many requests. Please try again later." });
  }

  const rawUsername = typeof req.body.username === "string" ? req.body.username : "";
  const rawPassword = typeof req.body.password === "string" ? req.body.password : "";

  // Trim without reflecting back to the user
  const username = rawUsername.trim();
  const password = rawPassword;

  // Validate inputs
  if (!isValidUsername(username) || !isValidPassword(password)) {
    recordAttempt(clientKey, false);
    return res.status(400).json({ error: "Invalid credentials." });
  }

  // Parameterized query to prevent SQL injection
  const sql = `
    SELECT id, username, password_hash, password_salt
    FROM users
    WHERE username = ?
    LIMIT 1
  `;

  pool.query(sql, [username], (dbErr, results) => {
    if (dbErr) {
      // Do not leak DB details
      return res.status(500).json({ error: "Server error." });
    }

    const user = Array.isArray(results) && results.length > 0 ? results[0] : null;

    // Verify password (constant-time)
    verifyPasswordWithPBKDF2(password, user, (verr, ok) => {
      if (verr) {
        return res.status(500).json({ error: "Server error." });
      }

      if (!ok) {
        recordAttempt(clientKey, false);
        // Generic response prevents username enumeration
        return res.status(401).json({ error: "Invalid credentials." });
      }

      // Successful login
      recordAttempt(clientKey, true);

      // Issue a minimal, secure, non-identifying response.
      // Session management/token issuance is intentionally omitted here to avoid insecure defaults.
      return res.status(200).json({ success: true });
    });
  });
});

const PORT = Number(process.env.PORT || 3000);
app.listen(PORT, () => {
  // Do not log sensitive data
  console.log(`Server running on port ${PORT}`);
});
