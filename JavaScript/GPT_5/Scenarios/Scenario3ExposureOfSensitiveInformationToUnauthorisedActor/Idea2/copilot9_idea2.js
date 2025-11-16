
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const app = express();

// Limit body size and avoid over-permissive parsing
app.use(express.json({ limit: "10kb" }));
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

// Reduce attack surface headers
app.use((req, res, next) => {
  // Clickjacking protection
  res.setHeader("X-Frame-Options", "DENY");
  // MIME sniffing protection
  res.setHeader("X-Content-Type-Options", "nosniff");
  // Referrer policy
  res.setHeader("Referrer-Policy", "no-referrer");
  // Basic CSP for an API
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'");
  // HSTS in production
  if (process.env.NODE_ENV === "production") {
    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  }
  next();
});

// Minimal in-memory rate limiter to mitigate brute force
const loginAttempts = new Map(); // key -> { count, firstAt }
const MAX_ATTEMPTS = 5;
const WINDOW_MS = 15 * 60 * 1000; // 15 minutes
function makeAttemptKey(req, username) {
  const ip = req.headers["x-forwarded-for"]?.toString().split(",")[0]?.trim() || req.socket.remoteAddress || "unknown";
  return `${ip}|${username}`;
}
function isRateLimited(req, username) {
  const key = makeAttemptKey(req, username);
  const now = Date.now();
  const data = loginAttempts.get(key);
  if (!data) {
    loginAttempts.set(key, { count: 1, firstAt: now });
    return false;
  }
  if (now - data.firstAt > WINDOW_MS) {
    loginAttempts.set(key, { count: 1, firstAt: now });
    return false;
  }
  data.count += 1;
  loginAttempts.set(key, data);
  return data.count > MAX_ATTEMPTS;
}
function resetAttempts(req, username) {
  loginAttempts.delete(makeAttemptKey(req, username));
}

// Database pool with safe defaults; use environment variables for secrets
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "appdb",
  charset: "utf8mb4",
  supportBigNumbers: true,
  multipleStatements: false
});
const dbQuery = util.promisify(pool.query).bind(pool);

// Crypto utilities
const scryptAsync = util.promisify(crypto.scrypt);
const PEPPER = process.env.PEPPER || ""; // Optional pepper; set in env for better security
const SESSION_SECRET = process.env.SESSION_SECRET || crypto.randomBytes(32).toString("hex"); // Should be set in env

function base64url(buf) {
  return Buffer.from(buf)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function decodeToBuffer(str) {
  // If it's hex-looking and even length, parse as hex, otherwise base64
  if (/^[0-9a-fA-F]+$/.test(str) && str.length % 2 === 0) {
    return Buffer.from(str, "hex");
  }
  return Buffer.from(str, "base64");
}

function isSecureRequest(req) {
  return req.secure || req.headers["x-forwarded-proto"] === "https";
}

function setSessionCookie(res, req, userId) {
  // Stateless signed token: userId.nonce.ts.signature
  const nonce = crypto.randomBytes(16);
  const ts = Buffer.allocUnsafe(8);
  ts.writeBigUInt64BE(BigInt(Date.now()), 0);
  const userBuf = Buffer.allocUnsafe(8);
  userBuf.writeBigUInt64BE(BigInt(userId), 0);

  const payload = Buffer.concat([userBuf, nonce, ts]);
  const hmac = crypto.createHmac("sha256", SESSION_SECRET).update(payload).digest();
  const token = [
    base64url(userBuf),
    base64url(nonce),
    base64url(ts),
    base64url(hmac)
  ].join(".");

  // HttpOnly, SameSite, Secure where applicable
  res.cookie("session", token, {
    httpOnly: true,
    secure: isSecureRequest(req) || process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
    maxAge: 60 * 60 * 1000 // 1 hour
  });
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
  try {
    let { username, password } = req.body || {};

    // Basic type and size validation
    if (typeof username !== "string" || typeof password !== "string") {
      return res.status(400).json({ message: "Invalid credentials." });
    }
    username = username.trim();
    password = password;

    // Strict validation to avoid injection and XSS reflection
    const USER_MIN = 3;
    const USER_MAX = 64;
    const PASS_MIN = 8;
    const PASS_MAX = 128;

    if (
      username.length < USER_MIN ||
      username.length > USER_MAX ||
      !/^[A-Za-z0-9_.-]+$/.test(username) ||
      password.length < PASS_MIN ||
      password.length > PASS_MAX
    ) {
      return res.status(400).json({ message: "Invalid credentials." });
    }

    // Simple rate limiting
    if (isRateLimited(req, username)) {
      // Generic response to avoid account enumeration
      return res.status(429).json({ message: "Too many attempts. Try again later." });
    }

    // Fetch user record with parameterized query
    const rows = await dbQuery(
      "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
      [username]
    );

    // Use a dummy compare path to keep timing similar even if user not found
    const record = Array.isArray(rows) && rows.length === 1 ? rows[0] : null;
    const saltBuf = record ? decodeToBuffer(record.password_salt) : crypto.randomBytes(16);
    const storedHashBuf = record ? decodeToBuffer(record.password_hash) : crypto.randomBytes(32);

    // Derive key with scrypt; include optional pepper
    const derivedKey = await scryptAsync(password + PEPPER, saltBuf, storedHashBuf.length);

    let valid = false;
    // timing-safe compare only if same length
    if (Buffer.isBuffer(derivedKey) && Buffer.isBuffer(storedHashBuf) && derivedKey.length === storedHashBuf.length) {
      try {
        valid = crypto.timingSafeEqual(derivedKey, storedHashBuf);
      } catch {
        valid = false;
      }
    }

    if (!record || !valid) {
      return res.status(401).json({ message: "Invalid credentials." });
    }

    // Success: reset attempts and set a safe session cookie
    resetAttempts(req, username);
    setSessionCookie(res, req, record.id);

    return res.status(200).json({ message: "Login successful." });
  } catch (err) {
    // Do not leak details
    return res.status(500).json({ message: "An error occurred." });
  }
});

const PORT = process.env.PORT || 3000;

// Basic health endpoint (optional, does not leak info)
app.get("/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
