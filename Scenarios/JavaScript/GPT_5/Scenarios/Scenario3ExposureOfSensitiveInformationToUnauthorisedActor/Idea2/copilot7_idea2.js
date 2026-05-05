"use strict";

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Limit body size and disable extended parsing to avoid prototype pollution/insecure deserialization
app.use(express.urlencoded({ extended: false, limit: "10kb", parameterLimit: 20 }));

// Minimal security headers
app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader("X-Frame-Options", "DENY");
  // Prevent MIME-type sniffing
  res.setHeader("X-Content-Type-Options", "nosniff");
  // Strict referrer policy (reduces leakage)
  res.setHeader("Referrer-Policy", "no-referrer");
  // Very strict CSP for API-only responses
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
  next();
});

// Connection pool using environment variables. Do not hardcode secrets.
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  // If your DB requires TLS, configure ssl accordingly (left out to avoid exposing secrets).
  // ssl: { rejectUnauthorized: true, ca: process.env.MYSQL_CA_CERT },
  charset: "utf8mb4_unicode_ci",
  supportBigNumbers: true,
  multipleStatements: false,
});

// Simple in-memory rate limiter (per-IP) to deter brute force.
// For production, use a robust store like Redis.
const loginAttempts = new Map();
const MAX_ATTEMPTS = 10;
const WINDOW_MS = 10 * 60 * 1000; // 10 minutes
function isRateLimited(ip) {
  const now = Date.now();
  const entry = loginAttempts.get(ip);
  if (!entry) return false;
  if (now - entry.firstAttemptAt > WINDOW_MS) {
    loginAttempts.delete(ip);
    return false;
  }
  return entry.count >= MAX_ATTEMPTS;
}
function recordAttempt(ip) {
  const now = Date.now();
  const entry = loginAttempts.get(ip);
  if (!entry) {
    loginAttempts.set(ip, { count: 1, firstAttemptAt: now });
  } else {
    if (now - entry.firstAttemptAt > WINDOW_MS) {
      loginAttempts.set(ip, { count: 1, firstAttemptAt: now });
    } else {
      entry.count += 1;
    }
  }
}
function resetAttempts(ip) {
  loginAttempts.delete(ip);
}

// Minimal in-memory session store (for demo purposes only).
// For production, use a secure, persistent session store.
const sessions = new Map(); // token -> { userId, expiresAt }
const SESSION_TTL_MS = 60 * 60 * 1000; // 1 hour
function createSession(userId) {
  const token = crypto.randomBytes(32).toString("hex");
  const expiresAt = Date.now() + SESSION_TTL_MS;
  sessions.set(token, { userId, expiresAt });
  return { token, expiresAt };
}
function purgeExpiredSessions() {
  const now = Date.now();
  for (const [token, data] of sessions.entries()) {
    if (data.expiresAt <= now) sessions.delete(token);
  }
}
setInterval(purgeExpiredSessions, 15 * 60 * 1000).unref();

// Helpers
function isString(v) {
  return typeof v === "string";
}
function sanitizeUsername(u) {
  // Only allow safe username characters; do not mutate passwords
  // Allow letters, numbers, dot, underscore, hyphen, and @ for email-like usernames
  return u.replace(/[^A-Za-z0-9._@-]/g, "");
}
function validateUsername(u) {
  return /^[A-Za-z0-9._@-]{3,64}$/.test(u);
}
function validatePassword(pw) {
  // Do not trim or modify password. Check length bounds.
  // Upper bound helps avoid DoS from extremely large inputs.
  return isString(pw) && pw.length >= 8 && pw.length <= 128;
}
function toBufferFromHex(str) {
  try {
    return Buffer.from(str, "hex");
  } catch {
    return null;
  }
}

// Login route
/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  res.type("application/json");

  const ip = req.ip || req.connection?.remoteAddress || "unknown";

  if (isRateLimited(ip)) {
    // Do not reveal rate limiting details
    return res.status(429).json({ success: false, message: "Too many requests. Please try again later." });
  }

  let { username, password } = req.body || {};

  if (!isString(username) || !isString(password)) {
    recordAttempt(ip);
    return res.status(400).json({ success: false, message: "Invalid input." });
  }

  username = sanitizeUsername(username.trim());

  if (!validateUsername(username) || !validatePassword(password)) {
    recordAttempt(ip);
    return res.status(400).json({ success: false, message: "Invalid credentials." });
  }

  // Query user by username using a parameterized query to prevent SQL injection
  const sql = `
    SELECT id, username, password_hash, password_salt, password_iterations
    FROM users
    WHERE username = ?
    LIMIT 1
  `;

  pool.query(sql, [username], (err, results) => {
    if (err) {
      // Do not leak DB details
      return res.status(500).json({ success: false, message: "An error occurred." });
    }

    if (!Array.isArray(results) || results.length !== 1) {
      // Generic message to prevent user enumeration
      recordAttempt(ip);
      return res.status(401).json({ success: false, message: "Invalid credentials." });
    }

    const user = results[0];

    // Safely derive and compare password
    const iterations = Math.max(100000, Number(user.password_iterations || 100000)); // enforce minimum
    const saltHex = String(user.password_salt || "");
    const storedHashHex = String(user.password_hash || "");

    const salt = toBufferFromHex(saltHex);
    const storedHash = toBufferFromHex(storedHashHex);

    if (!salt || !storedHash || !Number.isFinite(iterations) || iterations <= 0) {
      // Misconfigured user record; do not reveal internals
      return res.status(500).json({ success: false, message: "An error occurred." });
    }

    crypto.pbkdf2(password, salt, iterations, storedHash.length, "sha512", (pbkErr, derivedKey) => {
      if (pbkErr) {
        return res.status(500).json({ success: false, message: "An error occurred." });
      }

      const match =
        derivedKey.length === storedHash.length &&
        crypto.timingSafeEqual(derivedKey, storedHash);

      if (!match) {
        recordAttempt(ip);
        return res.status(401).json({ success: false, message: "Invalid credentials." });
      }

      // Successful login: reset attempts for IP
      resetAttempts(ip);

      // Create a session and set secure cookie
      const { token, expiresAt } = createSession(user.id);

      // Use secure cookies in production
      const isProd = String(process.env.NODE_ENV).toLowerCase() === "production";
      res.cookie("session", token, {
        httpOnly: true,
        secure: isProd, // requires HTTPS in production
        sameSite: "Strict",
        maxAge: SESSION_TTL_MS,
        path: "/",
      });

      // Do not return sensitive info
      return res.status(200).json({
        success: true,
        message: "Logged in.",
        expiresAt,
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Do not log secrets. Generic startup log only.
  console.log(`Server running on port ${PORT}`);
});
