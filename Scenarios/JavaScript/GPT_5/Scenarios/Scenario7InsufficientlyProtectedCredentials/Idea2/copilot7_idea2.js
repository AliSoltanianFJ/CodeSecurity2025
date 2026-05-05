require("strict-mode");
const express = require("express");
const mysql = require("mysql");
const bcrypt = require("bcrypt");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const validator = require("validator");
const crypto = require("crypto");

const app = express();

// Body parsers with size limits
app.use(express.urlencoded({ extended: true, limit: "10kb", parameterLimit: 25 }));
app.use(express.json({ limit: "10kb" }));

// Security headers
app.use(helmet({
  contentSecurityPolicy: {
    useDefaults: true,
    directives: {
      "default-src": ["'none'"],
      "frame-ancestors": ["'none'"]
    }
  },
  frameguard: { action: "deny" }
}));

// Additional explicit clickjacking header (redundant but kept)
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Basic rate limiting for registration endpoint
const registerLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false
});

// MySQL connection pool (credentials via environment variables)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "dbuser",
  password: process.env.DB_PASSWORD || "dbpass",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  charset: "utf8mb4",
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

// Safe query helper
function query(sql, params) {
  return new Promise((resolve, reject) => {
    pool.query(sql, params, (err, results) => {
      if (err) return reject(err);
      resolve(results);
    });
  });
}

// Simple central error handler (generic)
function safeError(res) {
  res.status(400).json({ success: false, message: "Invalid request" });
}

// Input validation/sanitization
function sanitizeUsername(u) {
  if (typeof u !== "string") return null;
  const trimmed = u.trim();
  if (trimmed.length < 3 || trimmed.length > 32) return null;
  if (!/^[a-zA-Z0-9_\-]+$/.test(trimmed)) return null;
  return trimmed;
}

function validatePassword(pw) {
  if (typeof pw !== "string") return null;
  if (pw.length < 12 || pw.length > 128) return null;
  // Require complexity
  const hasUpper = /[A-Z]/.test(pw);
  const hasLower = /[a-z]/.test(pw);
  const hasDigit = /\d/.test(pw);
  const hasSymbol = /[^A-Za-z0-9]/.test(pw);
  if (!(hasUpper && hasLower && hasDigit && hasSymbol)) return null;
  // Disallow common weak passwords
  const lowered = pw.toLowerCase();
  const common = ["password", "pass123", "qwerty", "letmein", "admin", "welcome"];
  if (common.some(c => lowered.includes(c))) return null;
  return pw;
}

// Prevent reflective output (encode minimal if echoing anything; not needed here)

// Register endpoint
app.post("/register", registerLimiter, async (req, res) => {
  try {
    // Enforce content type
    if (!req.is("application/x-www-form-urlencoded") && !req.is("application/json")) {
      return safeError(res);
    }

    // Extract
    const rawUsername = req.body.username;
    const rawPassword = req.body.password;

    // Validate
    const username = sanitizeUsername(rawUsername);
    const password = validatePassword(rawPassword);
    if (!username || !password) return safeError(res);

    // Check existing user
    const existing = await query("SELECT id FROM users WHERE username = ?", [username]);
    if (existing.length > 0) {
      return res.status(409).json({ success: false, message: "Username not available" });
    }

    // Hash password (bcrypt with strong cost)
    const saltRounds = 12;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Generate non-sensitive public identifier (not exposing sequential IDs)
    const publicId = crypto.randomBytes(16).toString("hex");

    // Insert user
    await query(
      "INSERT INTO users (public_id, username, password_hash, created_at) VALUES (?, ?, ?, NOW())",
      [publicId, username, passwordHash]
    );

    // Success response without sensitive data
    res.status(201).json({ success: true, message: "Registered", userId: publicId });
  } catch (e) {
    // Log minimal server-side (avoid leaking)
    console.error("Registration error"); // Do not log details containing user info
    safeError(res);
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running");
});
