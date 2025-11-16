const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Parse body with strict limits to prevent large payloads
app.use(express.json({ limit: "10kb", strict: true }));
app.use(express.urlencoded({ extended: false, limit: "10kb", parameterLimit: 50 }));

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
  // Strict CSP (no inline scripts, no external sources)
  res.setHeader(
    "Content-Security-Policy",
    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"
  );
  // Only effective over HTTPS; safe to always set
  res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  next();
});

// Database pool (use environment variables; do not hard-code secrets)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  charset: "utf8mb4",
  // If you have TLS configured, set DB_SSL=true and provide proper CA in your environment
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
  supportBigNumbers: true,
  dateStrings: true
});

// Simple in-memory rate limit (avoid brute force). For production use a robust store.
const loginAttempts = new Map(); // key: ip, value: { count, first, last }
const MAX_ATTEMPTS = 10;
const WINDOW_MS = 5 * 60 * 1000; // 5 minutes

function rateLimitExceeded(ip) {
  const now = Date.now();
  const rec = loginAttempts.get(ip);
  if (!rec) {
    loginAttempts.set(ip, { count: 1, first: now, last: now });
    return false;
  }
  // Reset window
  if (now - rec.first > WINDOW_MS) {
    loginAttempts.set(ip, { count: 1, first: now, last: now });
    return false;
  }
  rec.count += 1;
  rec.last = now;
  loginAttempts.set(ip, rec);
  return rec.count > MAX_ATTEMPTS;
}

// Periodically clean the attempts map
setInterval(() => {
  const now = Date.now();
  for (const [ip, rec] of loginAttempts.entries()) {
    if (now - rec.first > WINDOW_MS) loginAttempts.delete(ip);
  }
}, 60_000).unref();

// Minimal in-memory session store
const sessions = new Map(); // key: sessionId, value: { userId, created, expires }
const SESSION_TTL_MS = 60 * 60 * 1000; // 1 hour

setInterval(() => {
  const now = Date.now();
  for (const [sid, sess] of sessions.entries()) {
    if (sess.expires <= now) sessions.delete(sid);
  }
}, 60_000).unref();

// Helpers
const SCRYPT_PARAMS = { N: 16384, r: 8, p: 1, maxmem: 64 * 1024 * 1024 };
const KEYLEN = 64;

function scryptAsync(password, salt) {
  return new Promise((resolve, reject) => {
    crypto.scrypt(password, salt, KEYLEN, SCRYPT_PARAMS, (err, derivedKey) => {
      if (err) return reject(err);
      resolve(derivedKey);
    });
  });
}

// Pre-compute a dummy hash to mitigate user enumeration and timing attacks
const DUMMY_SALT = Buffer.from("b3f1c9f8762be76f", "hex");
const DUMMY_HASH = crypto.scryptSync("invalid_password", DUMMY_SALT, KEYLEN, SCRYPT_PARAMS);

function isValidUsername(value) {
  if (typeof value !== "string") return false;
  const v = value.trim();
  if (v.length < 3 || v.length > 64) return false;
  // Allow common safe username chars
  return /^[A-Za-z0-9_.\-@]+$/.test(v);
}

function isValidPassword(value) {
  if (typeof value !== "string") return false;
  // Do not restrict charset; only length to mitigate abuse
  return value.length >= 8 && value.length <= 1024;
}

function sanitizeUsername(value) {
  // Trim and normalize whitespace; do not reflect back to client anyway
  return value.trim();
}

// Generic auth response with no user enumeration
function sendAuthFailure(res) {
  // Respond with 401 and generic message
  res.status(401).type("application/json").send({ success: false, message: "Invalid credentials." });
}

// Create a secure session cookie
function setSessionCookie(req, res, userId) {
  const sid = crypto.randomBytes(32).toString("hex");
  const now = Date.now();
  sessions.set(sid, { userId, created: now, expires: now + SESSION_TTL_MS });

  const isSecure = req.secure || (req.headers["x-forwarded-proto"] || "").toString().includes("https");

  // Build Set-Cookie with security flags
  const cookieParts = [
    `sid=${sid}`,
    "Path=/",
    "HttpOnly",
    "SameSite=Strict",
    `Max-Age=${Math.floor(SESSION_TTL_MS / 1000)}`
  ];
  if (isSecure) cookieParts.push("Secure");
  res.setHeader("Set-Cookie", cookieParts.join("; "));
}

// Safe query wrapper using placeholders
function getUserByUsername(username) {
  return new Promise((resolve, reject) => {
    pool.query(
      "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
      [username],
      (err, results) => {
        if (err) return reject(err);
        if (!Array.isArray(results) || results.length === 0) return resolve(null);
        // Expect base64-encoded hash and salt in DB
        const row = results[0];
        resolve({
          id: row.id,
          password_hash: typeof row.password_hash === "string" ? row.password_hash : "",
          password_salt: typeof row.password_salt === "string" ? row.password_salt : ""
        });
      }
    );
  });
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
  try {
    // Enforce content types for safety (form or JSON)
    const ct = (req.headers["content-type"] || "").toLowerCase();
    if (!ct.includes("application/json") && !ct.includes("application/x-www-form-urlencoded")) {
      return res.status(415).type("application/json").send({ success: false, message: "Unsupported Media Type." });
    }

    // Rate limiting
    const ip = req.ip || req.connection?.remoteAddress || "unknown";
    if (rateLimitExceeded(ip)) {
      // Use same generic failure to avoid leakage
      return sendAuthFailure(res);
    }

    const usernameRaw = req.body?.username;
    const passwordRaw = req.body?.password;

    if (!isValidUsername(usernameRaw) || !isValidPassword(passwordRaw)) {
      // Perform dummy hash to normalize timing
      await scryptAsync("invalid", DUMMY_SALT);
      return sendAuthFailure(res);
    }

    const username = sanitizeUsername(usernameRaw);
    const password = passwordRaw;

    // Look up user
    let user = null;
    try {
      user = await getUserByUsername(username);
    } catch (_dbErr) {
      // Do not leak DB errors
      return res.status(503).type("application/json").send({ success: false, message: "Service unavailable." });
    }

    if (!user) {
      // Dummy verification to mitigate timing attacks
      const dummy = await scryptAsync(password, DUMMY_SALT);
      // Ensure constant-time compare
      const ok = crypto.timingSafeEqual(
        Buffer.from(DUMMY_HASH.subarray(0, Math.min(DUMMY_HASH.length, dummy.length))),
        Buffer.from(dummy.subarray(0, Math.min(DUMMY_HASH.length, dummy.length)))
      );
      // Regardless of ok, fail
      return sendAuthFailure(res);
    }

    // Verify password using scrypt with stored salt
    let saltBuf, storedHashBuf;
    try {
      saltBuf = Buffer.from(user.password_salt, "base64");
      storedHashBuf = Buffer.from(user.password_hash, "base64");
      if (saltBuf.length < 8 || storedHashBuf.length !== KEYLEN) {
        // Corrupt or unexpected format
        return sendAuthFailure(res);
      }
    } catch {
      return sendAuthFailure(res);
    }

    const derived = await scryptAsync(password, saltBuf);

    // Constant-time comparison
    const sameLength = storedHashBuf.length === derived.length;
    let match = false;
    if (sameLength) {
      match = crypto.timingSafeEqual(storedHashBuf, derived);
    } else {
      // Compare equal-length prefixes to keep timing similar
      const minLen = Math.min(storedHashBuf.length, derived.length);
      match = crypto.timingSafeEqual(storedHashBuf.subarray(0, minLen), derived.subarray(0, minLen)) && false;
    }

    if (!match) {
      return sendAuthFailure(res);
    }

    // Success: create session cookie
    setSessionCookie(req, res, user.id);

    // Do not include private information
    return res.status(200).type("application/json").send({ success: true, message: "Logged in." });
  } catch {
    // Generic server error without leaking details
    return res.status(500).type("application/json").send({ success: false, message: "Unexpected error." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running.");
});
