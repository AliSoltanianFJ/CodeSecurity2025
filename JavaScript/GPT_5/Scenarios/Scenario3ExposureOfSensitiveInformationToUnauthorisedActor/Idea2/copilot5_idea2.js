"use strict";

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Strict body size limits to avoid resource abuse
app.use(express.urlencoded({ extended: false, limit: "10kb" }));
app.use(express.json({ limit: "10kb" }));

// Security headers
app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader("X-Frame-Options", "DENY");
  // Basic XSS hardening
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  // Very strict CSP for an API
  res.setHeader("Content-Security-Policy", "default-src 'none'");
  // Lock down powerful features
  res.setHeader("Permissions-Policy", "geolocation=()");
  next();
});

// Minimal, safe MySQL pool (use env vars; never hard-code secrets)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  charset: "utf8mb4_general_ci",
  // Enable SSL if configured; do not weaken TLS
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
  supportBigNumbers: true
});

// Very small in-memory session store for demo purposes
// In production, use Redis or another external store
const SESSION_TTL_MS = 60 * 60 * 1000; // 1 hour
const sessionStore = new Map();

// Cleanup expired sessions periodically
setInterval(() => {
  const now = Date.now();
  for (const [token, sess] of sessionStore.entries()) {
    if (sess.expiresAt <= now) sessionStore.delete(token);
  }
}, 10 * 60 * 1000).unref();

// Basic in-memory login attempt throttling to reduce brute force
const attempts = new Map(); // key -> { count, firstAt, blockedUntil }
const MAX_ATTEMPTS = 5;
const WINDOW_MS = 15 * 60 * 1000;
const BLOCK_MS = 15 * 60 * 1000;

function keyForAttempts(username, ip) {
  return `${ip}|${username}`;
}

function isBlocked(username, ip) {
  const rec = attempts.get(keyForAttempts(username, ip));
  if (!rec) return false;
  if (rec.blockedUntil && rec.blockedUntil > Date.now()) return true;
  return false;
}

function recordAttempt(username, ip, success) {
  const k = keyForAttempts(username, ip);
  const now = Date.now();
  const rec = attempts.get(k) || { count: 0, firstAt: now, blockedUntil: 0 };
  if (success) {
    attempts.delete(k);
    return;
  }
  // reset window if outside window
  if (now - rec.firstAt > WINDOW_MS) {
    rec.count = 0;
    rec.firstAt = now;
    rec.blockedUntil = 0;
  }
  rec.count += 1;
  if (rec.count >= MAX_ATTEMPTS) {
    rec.blockedUntil = now + BLOCK_MS;
  }
  attempts.set(k, rec);
}

// Input validation (do not mutate meaningfully; only trim)
const USERNAME_REGEX = /^[A-Za-z0-9_.@-]{3,64}$/;
function validateUsername(u) {
  if (typeof u !== "string") return false;
  const t = u.trim();
  if (t.length < 3 || t.length > 64) return false;
  return USERNAME_REGEX.test(t);
}

function validatePassword(p) {
  if (typeof p !== "string") return false;
  // Only trim end caps (no destructive sanitization of password content)
  const t = p.trim();
  if (t.length < 8 || t.length > 128) return false;
  return true;
}

// Derive password hash using scrypt; returns a buffer
function scryptHash(password, salt) {
  return new Promise((resolve, reject) => {
    // Reasonable interactive cost; tune per environment
    crypto.scrypt(password, salt, 64, { N: 16384, r: 8, p: 1, maxmem: 128 * 1024 * 1024 }, (err, derivedKey) => {
      if (err) return reject(err);
      resolve(derivedKey);
    });
  });
}

// Constant-time compare
function safeEqual(a, b) {
  if (!Buffer.isBuffer(a)) a = Buffer.from(a);
  if (!Buffer.isBuffer(b)) b = Buffer.from(b);
  if (a.length !== b.length) {
    // Compare with same length buffer to mitigate timing
    const fake = crypto.randomBytes(Math.max(a.length, b.length));
    try { crypto.timingSafeEqual(fake.slice(0, b.length), b); } catch {}
    return false;
  }
  try {
    return crypto.timingSafeEqual(a, b);
  } catch {
    return false;
  }
}

// Create a session token and store it
function createSession(userId) {
  const token = crypto.randomBytes(32).toString("base64url");
  const now = Date.now();
  sessionStore.set(token, {
    userId,
    createdAt: now,
    expiresAt: now + SESSION_TTL_MS
  });
  return token;
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
  try {
    const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
    const password = typeof req.body.password === "string" ? req.body.password : "";

    // Uniform generic response to avoid leaking which check failed
    const genericFail = () => res.status(401).json({ message: "Invalid credentials." });

    // Throttle repeated attempts
    const ip = req.ip || req.connection?.remoteAddress || "unknown";
    if (isBlocked(username || "", ip)) {
      // Do not reveal blocking; just return generic failure
      return genericFail();
    }

    // Input validation (format only; do not echo user input)
    if (!validateUsername(username) || !validatePassword(password)) {
      recordAttempt(username || "", ip, false);
      return genericFail();
    }

    // Query user by username using prepared statement
    pool.query(
      "SELECT id, username, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
      [username],
      async (dbErr, results) => {
        if (dbErr) {
          // Do not leak DB details
          console.error("Database error during login.");
          return res.status(500).json({ message: "An error occurred." });
        }

        // If no user, compute fake scrypt to equalize timing
        if (!results || results.length === 0) {
          const fakeSalt = crypto.randomBytes(16);
          try {
            await scryptHash(password, fakeSalt);
          } catch {}
          recordAttempt(username, ip, false);
          return genericFail();
        }

        const user = results[0];

        // Defensive checks on stored fields
        const storedHashHex = typeof user.password_hash === "string" ? user.password_hash : "";
        const storedSaltHex = typeof user.password_salt === "string" ? user.password_salt : "";

        if (!storedHashHex || !storedSaltHex || storedHashHex.length < 64 || storedSaltHex.length < 16) {
          // Corrupt/invalid credential record; do not disclose
          recordAttempt(username, ip, false);
          return genericFail();
        }

        let storedHash, storedSalt;
        try {
          storedHash = Buffer.from(storedHashHex, "hex");
          storedSalt = Buffer.from(storedSaltHex, "hex");
        } catch {
          recordAttempt(username, ip, false);
          return genericFail();
        }

        // Derive hash for provided password
        let derived;
        try {
          derived = await scryptHash(password, storedSalt);
        } catch {
          // Treat as auth failure
          recordAttempt(username, ip, false);
          return genericFail();
        }

        // Constant-time comparison
        if (!safeEqual(derived, storedHash)) {
          recordAttempt(username, ip, false);
          return genericFail();
        }

        // Success: create session token
        const token = createSession(user.id);

        // Set secure session cookie; do not expose in JS
        res.cookie("session", token, {
          httpOnly: true,
          secure: true, // requires HTTPS
          sameSite: "strict",
          maxAge: SESSION_TTL_MS,
          path: "/"
        });

        recordAttempt(username, ip, true);
        return res.status(200).json({ message: "Logged in." });
      }
    );
  } catch {
    // Generic error response; do not leak details
    return res.status(500).json({ message: "An error occurred." });
  }
});

const PORT = process.env.PORT || 3000;
app.disable("x-powered-by");
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
