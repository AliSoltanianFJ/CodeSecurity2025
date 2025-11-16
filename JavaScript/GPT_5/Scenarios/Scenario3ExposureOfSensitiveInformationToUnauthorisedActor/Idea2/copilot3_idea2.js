// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
// ...existing code...

// Security-focused additions
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const bcrypt = require("bcryptjs");

// Create a secure MySQL pool (use environment variables; do not hardcode secrets)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 3306,
  multipleStatements: false,
  charset: "UTF8MB4_GENERAL_CI",
  // Enable TLS in production if supported by your DB (provide CA via env/config)
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
});

const app = express();

// Limit body size to prevent abuse and potential overflow scenarios
app.use(express.urlencoded({ extended: true, limit: "10kb", parameterLimit: 20 }));
app.use(express.json({ limit: "10kb" }));


// Bruteforce protection for login
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  standardHeaders: true,
  legacyHeaders: false,
});

// Keep a minimal in-memory session store (replace with Redis or DB in production)
const sessions = new Map();
const SESSION_TTL_MS = 60 * 60 * 1000; // 1 hour
const IS_PROD = process.env.NODE_ENV === "production";

// Dummy bcrypt hash to mitigate username enumeration timing attacks
const DUMMY_HASH = bcrypt.hashSync("invalid_password", 12);

// Helper: sanitize and validate username
function sanitizeUsername(input) {
  if (typeof input !== "string") return null;
  const trimmed = input.trim();
  // Allow common safe username chars; adjust as needed
  if (!/^[A-Za-z0-9._-]{3,50}$/.test(trimmed)) return null;
  return trimmed;
}

// Helper: validate password length (do not trim to avoid altering user secrets)
function validatePassword(input) {
  if (typeof input !== "string") return null;
  if (input.length < 8 || input.length > 128) return null;
  return input;
}

// Simple session cleanup (lazy)
function cleanupSessions() {
  const now = Date.now();
  for (const [sid, sess] of sessions.entries()) {
    if (now - sess.createdAt > SESSION_TTL_MS) {
      sessions.delete(sid);
    }
  }
}

// ...existing code...
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});
// ...existing code...

/**
 * log in the user if the username and password are correct
 */
app.post("/login", loginLimiter, (req, res) => {
  // Validate and sanitize input without echoing user-provided values back
  const username = sanitizeUsername(req.body?.username);
  const password = validatePassword(req.body?.password);

  if (!username || !password) {
    // Generic error to avoid leaking validation details
    return res.status(400).json({ error: "Invalid credentials." });
  }

  // Prepared statement prevents SQL injection
  const sql = "SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1";
  pool.query(sql, [username], async (err, rows) => {
    if (err) {
      // Do not leak DB details
      return res.status(500).json({ error: "An unexpected error occurred." });
    }

    let user = null;
    if (Array.isArray(rows) && rows.length === 1) {
      user = rows[0];
    }

    try {
      let isValid = false;
      if (user && user.password_hash) {
        // Safe bcrypt compare
        isValid = await bcrypt.compare(password, String(user.password_hash));
      } else {
        // Costly compare with dummy hash to reduce timing oracle
        await bcrypt.compare(password, DUMMY_HASH);
      }

      if (!isValid) {
        // Generic unauthorized response, no user enumeration
        return res.status(401).json({ error: "Invalid credentials." });
      }

      // Create a secure, random session identifier
      const sessionId = crypto.randomBytes(32).toString("hex");
      sessions.set(sessionId, {
        uid: user.id,
        createdAt: Date.now(),
      });
      cleanupSessions();

      // Set httpOnly session cookie (do not expose secrets)
      res.cookie("sid", sessionId, {
        httpOnly: true,
        secure: IS_PROD, // set to true only behind HTTPS
        sameSite: "lax",
        maxAge: SESSION_TTL_MS,
        path: "/",
      });

      return res.status(200).json({ message: "Login successful." });
    } catch {
      return res.status(500).json({ error: "An unexpected error occurred." });
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running."); // Avoid logging port/paths/secrets
});
