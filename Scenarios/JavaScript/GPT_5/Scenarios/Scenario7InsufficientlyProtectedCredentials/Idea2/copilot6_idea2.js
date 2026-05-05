const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const rateLimit = require("express-rate-limit");

// --- Secure DB pool (use environment variables; never hard‑code secrets) ---
const dbPool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "secure_user",
  password: process.env.DB_PASSWORD || "change_me",
  database: process.env.DB_NAME || "appdb",
  charset: "utf8mb4",
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

const app = express();

// Body parsers with tight limits
app.use(express.urlencoded({ extended: false, limit: "1kb", parameterLimit: 5 }));
app.use(express.json({ limit: "1kb" }));

// Clickjacking protection (already partly present)
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Basic rate limiting
const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false
});
app.use(limiter);

// Minimal output encoding helper (for any future text output)
function safeOutput(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

// Validate and sanitize username/password
function validateCredentials(username, password) {
  if (typeof username !== "string" || typeof password !== "string") {
    return "Invalid input types.";
  }
  // Trim
  username = username.trim();
  password = password.trim();

  // Length checks
  if (username.length < 3 || username.length > 32) return "Username length invalid.";
  if (password.length < 12 || password.length > 128) return "Password length invalid.";

  // Whitelist username characters
  if (!/^[a-zA-Z0-9_.-]+$/.test(username)) return "Username has invalid characters.";

  // Basic password complexity
  if (!/[A-Z]/.test(password) ||
      !/[a-z]/.test(password) ||
      !/[0-9]/.test(password) ||
      !/[^A-Za-z0-9]/.test(password)) {
    return "Password complexity insufficient.";
  }

  return { username, password };
}

// Secure password hashing with scrypt
function hashPassword(password) {
  return new Promise((resolve, reject) => {
    const salt = crypto.randomBytes(16);
    const scryptParams = { N: 16384, r: 8, p: 1 }; // Reasonable cost
    crypto.scrypt(password, salt, 64, scryptParams, (err, derivedKey) => {
      if (err) return reject(err);
      const record = [
        "scrypt",
        salt.toString("base64"),
        derivedKey.toString("base64"),
        scryptParams.N,
        scryptParams.r,
        scryptParams.p
      ].join("$");
      resolve(record);
    });
  });
}

// Constant-time compare (for future use)
function safeCompare(a, b) {
  const aBuf = Buffer.from(a);
  const bBuf = Buffer.from(b);
  if (aBuf.length !== bBuf.length) return false;
  return crypto.timingSafeEqual(aBuf, bBuf);
}

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  // Do not reflect raw user input
  const rawUsername = req.body?.username;
  const rawPassword = req.body?.password;

  const validated = validateCredentials(rawUsername, rawPassword);
  if (typeof validated === "string") {
    return res.status(400).json({ success: false, message: "Invalid credentials." });
  }
  const { username, password } = validated;

  try {
    // Check existing user (parameterized prevents SQL injection)
    dbPool.query("SELECT id FROM users WHERE username = ? LIMIT 1", [username], async (err, rows) => {
      if (err) {
        return res.status(500).json({ success: false, message: "Server error." });
      }
      if (rows && rows.length > 0) {
        return res.status(409).json({ success: false, message: "Registration failed." });
      }

      let passwordRecord;
      try {
        passwordRecord = await hashPassword(password);
      } catch {
        return res.status(500).json({ success: false, message: "Server error." });
      }

      // Insert new user securely
      dbPool.query(
        "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())",
        [username, passwordRecord],
        (insertErr) => {
          if (insertErr) {
            return res.status(500).json({ success: false, message: "Server error." });
          }
          // Generic success; do not expose sensitive data
            res.status(201).json({ success: true, message: "Registered." });
        }
      );
    });
  } catch {
    return res.status(500).json({ success: false, message: "Server error." });
  }
});

// Health endpoint (no sensitive info)
app.get("/health", (req, res) => {
  res.json({ status: "ok" });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
