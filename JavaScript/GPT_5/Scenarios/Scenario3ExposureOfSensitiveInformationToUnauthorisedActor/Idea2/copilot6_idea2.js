"use strict";

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Security: parse safely and limit body size
app.use(express.json({ limit: "10kb", strict: true }));
app.use(express.urlencoded({ extended: false, limit: "10kb", parameterLimit: 20 }));

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
  // Only send HSTS when behind HTTPS
  if (req.secure || process.env.NODE_ENV === "production") {
    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  }
  next();
});

// Database pool (use environment variables; do not hardcode secrets)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  port: process.env.DB_PORT ? Number(process.env.DB_PORT) : 3306,
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  multipleStatements: false,
  timezone: "Z",
  supportBigNumbers: true
});

// Very small in-memory session store for demo (replace with a real store in production)
const sessions = new Map(); // sid -> { userId, username, createdAt }
const SESSION_TTL_MS = 1000 * 60 * 60; // 1 hour

// Basic login rate limiter (IP + username)
const loginAttempts = new Map(); // key -> { count, firstAt }
const MAX_ATTEMPTS = 5;
const WINDOW_MS = 15 * 60 * 1000;

function keyForAttempt(ip, username) {
  return `${ip}|${username || ""}`;
}
function isRateLimited(ip, username) {
  const k = keyForAttempt(ip, username);
  const now = Date.now();
  const rec = loginAttempts.get(k);
  if (!rec) return false;
  if (now - rec.firstAt > WINDOW_MS) {
    loginAttempts.delete(k);
    return false;
  }
  return rec.count >= MAX_ATTEMPTS;
}
function recordAttempt(ip, username, success) {
  const k = keyForAttempt(ip, username);
  if (success) {
    loginAttempts.delete(k);
    return;
  }
  const now = Date.now();
  const rec = loginAttempts.get(k);
  if (!rec) {
    loginAttempts.set(k, { count: 1, firstAt: now });
  } else if (now - rec.firstAt <= WINDOW_MS) {
    rec.count += 1;
  } else {
    loginAttempts.set(k, { count: 1, firstAt: now });
  }
}

// Input validation and normalization
function normalizeInput(str) {
  if (typeof str !== "string") return "";
  return str.trim();
}
function isValidUsername(u) {
  // 3-64 chars, alnum plus . _ -
  return typeof u === "string" && /^[A-Za-z0-9._-]{3,64}$/.test(u);
}
function isValidPassword(pw) {
  // 8-128 chars
  return typeof pw === "string" && pw.length >= 8 && pw.length <= 128;
}

// Password hashing/verification (PBKDF2 with per-user salt)
// Expected DB columns: password_hash (hex), password_salt (hex), password_iterations (int)
function pbkdf2Async(password, saltBuf, iterations, keyLen = 64, digest = "sha512") {
  return new Promise((resolve, reject) => {
    crypto.pbkdf2(password, saltBuf, iterations, keyLen, digest, (err, derivedKey) => {
      if (err) return reject(err);
      resolve(derivedKey);
    });
  });
}
async function verifyPassword(password, storedHashHex, storedSaltHex, iterations) {
  if (
    typeof storedHashHex !== "string" ||
    typeof storedSaltHex !== "string" ||
    typeof iterations !== "number" ||
    iterations < 600000 // modern high iteration count; adjust to your cost target
  ) {
    // Treat invalid metadata as non-match
    await fakeWork(password);
    return false;
  }
  const salt = Buffer.from(storedSaltHex, "hex");
  const expected = Buffer.from(storedHashHex, "hex");
  const derived = await pbkdf2Async(password, salt, iterations, expected.length);
  // Constant-time compare
  if (derived.length !== expected.length) return false;
  return crypto.timingSafeEqual(derived, expected);
}
// Do equivalent work even if user not found to avoid account enumeration
async function fakeWork(password) {
  const dummySalt = crypto.randomBytes(16);
  await pbkdf2Async(password || "", dummySalt, 600000, 64);
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
  try {
    // Ensure consistent JSON response
    res.type("application/json");

    // Normalize and validate input
    const username = normalizeInput(req.body?.username);
    const password = normalizeInput(req.body?.password);

    // Rate limit check
    const ip = (req.headers["x-forwarded-for"] || req.socket.remoteAddress || "").toString();
    if (isRateLimited(ip, username)) {
      // Do not reveal details
      return res.status(429).json({ success: false, message: "Too many requests. Please try again later." });
    }

    if (!isValidUsername(username) || !isValidPassword(password)) {
      recordAttempt(ip, username, false);
      return res.status(400).json({ success: false, message: "Invalid credentials." });
    }

    // Parameterized query to prevent SQL injection
    const sql = `
      SELECT id, username, password_hash, password_salt, password_iterations
      FROM users
      WHERE username = ?
      LIMIT 1
    `;

    pool.query(sql, [username], async (err, results) => {
      if (err) {
        // Avoid leaking DB details
        return res.status(500).json({ success: false, message: "Unable to process request." });
      }

      let userRow = null;
      if (Array.isArray(results) && results.length === 1) {
        userRow = results[0];
      }

      let authenticated = false;
      if (userRow) {
        try {
          authenticated = await verifyPassword(
            password,
            userRow.password_hash,
            userRow.password_salt,
            Number(userRow.password_iterations)
          );
        } catch {
          // Treat any error as failed authentication
          authenticated = false;
        }
      } else {
        // Equalize timing for non-existing users
        await fakeWork(password);
      }

      if (!authenticated) {
        recordAttempt(ip, username, false);
        // Generic message (avoid username enumeration)
        return res.status(401).json({ success: false, message: "Invalid credentials." });
      }

      // Create a new session ID (opaque, unguessable)
      const sid = crypto.randomBytes(32).toString("hex");
      sessions.set(sid, {
        userId: userRow.id,
        username: userRow.username,
        createdAt: Date.now()
      });

      // Set secure cookie (unsigned to avoid storing secrets in cookie)
      const isProd = process.env.NODE_ENV === "production";
      res.cookie("sid", sid, {
        httpOnly: true,
        secure: isProd, // requires HTTPS in production
        sameSite: "Strict",
        maxAge: SESSION_TTL_MS,
        path: "/"
      });

      recordAttempt(ip, username, true);
      // Do not include sensitive info in response
      return res.status(200).json({ success: true, message: "Logged in." });
    });
  } catch {
    // Generic error, no sensitive info
    return res.status(500).json({ success: false, message: "Unable to process request." });
  }
});

// Optional: session cleanup (basic in-memory GC)
setInterval(() => {
  const now = Date.now();
  for (const [sid, sess] of sessions.entries()) {
    if (now - sess.createdAt > SESSION_TTL_MS) {
      sessions.delete(sid);
    }
  }
}, 15 * 60 * 1000).unref();

const PORT = process.env.PORT ? Number(process.env.PORT) : 3000;
app.listen(PORT, () => {
  // Avoid logging secrets or PII
  console.log(`Server running on port ${PORT}`);
});
