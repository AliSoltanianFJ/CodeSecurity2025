"use strict";

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcrypt");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const validator = require("validator");

const app = express();

// Security middleware
app.use(helmet({
  contentSecurityPolicy: {
    useDefaults: true,
    directives: {
      "default-src": ["'none'"]
    }
  },
  frameguard: { action: "deny" },
  hidePoweredBy: true,
  xssFilter: true
}));

app.use(express.json({ limit: "10kb" }));
app.use(express.urlencoded({ extended: true, limit: "10kb" }));

// Additional manual header hardening
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=()");
  next();
});

// Basic rate limiter to mitigate brute force
const limiter = rateLimit({
  windowMs: 60_000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false
});
app.use(limiter);

// Create MySQL pool (credentials via environment variables; never hardcode)
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4",
  supportBigNumbers: true
});

// Safe query helper
function runQuery(sql, params) {
  return new Promise((resolve, reject) => {
    pool.query(sql, params, (err, results) => {
      if (err) return reject(err);
      resolve(results);
    });
  });
}

// Input validation helpers
function validateUsername(u) {
  if (typeof u !== "string") return false;
  if (u.length < 3 || u.length > 32) return false;
  if (!/^[A-Za-z0-9_]+$/.test(u)) return false;
  return true;
}

function validatePassword(pw) {
  if (typeof pw !== "string") return false;
  if (pw.length < 12 || pw.length > 128) return false;
  // Require at least 3 classes
  let classes = 0;
  if (/[a-z]/.test(pw)) classes++;
  if (/[A-Z]/.test(pw)) classes++;
  if (/[0-9]/.test(pw)) classes++;
  if (/[^A-Za-z0-9]/.test(pw)) classes++;
  return classes >= 3;
}

// Uniform response to avoid leaking internal details
function safeFail(res) {
  return res.status(400).json({ ok: false, message: "Registration failed." });
}

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  try {
    // Extract raw inputs
    const rawUsername = req.body?.username;
    const rawPassword = req.body?.password;

    // Basic presence checks
    if (!rawUsername || !rawPassword) return safeFail(res);

    // Trim and canonicalize
    const username = validator.whitelist(rawUsername.trim(), "A-Za-z0-9_");
    const password = rawPassword; // Do not modify password string (only validate)

    // Validate
    if (!validateUsername(username) || !validatePassword(password)) {
      return safeFail(res);
    }

    // Check if user exists (constant time style: do not disclose which failed)
    const existing = await runQuery("SELECT id FROM users WHERE username = ? LIMIT 1", [username]);
    if (existing.length > 0) {
      // Avoid username enumeration
      return safeFail(res);
    }

    // Strong password hashing (bcrypt with cost 12; adjust as needed)
    const saltRounds = 12;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Insert user
    await runQuery(
      "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())",
      [username, passwordHash]
    );

    // Success (no sensitive data)
    return res.status(201).json({ ok: true, message: "Account created." });
  } catch (e) {
    // Log minimal error internally (avoid sensitive output). Example placeholder:
    // console.error("Registration error:", e); // If logging, ensure secure log handling.
    return safeFail(res);
  }
});

// Generic not found handler
app.use((req, res) => {
  res.status(404).json({ ok: false, message: "Not found." });
});

// Central error handler
app.use((err, req, res, next) => { // eslint-disable-line no-unused-vars
  // console.error("Unhandled error:", err);
  res.status(500).json({ ok: false, message: "Server error." });
});

const PORT = Number(process.env.PORT || 3000);
app.listen(PORT, () => {
  // Avoid printing sensitive config
  console.log(`Server running on port ${PORT}`);
});
