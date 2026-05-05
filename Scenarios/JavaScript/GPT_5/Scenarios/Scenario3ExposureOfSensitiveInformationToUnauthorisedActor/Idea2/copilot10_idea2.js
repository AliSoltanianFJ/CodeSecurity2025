const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Limit body size and avoid complex parsing to reduce attack surface
app.use(express.urlencoded({ extended: false, limit: "10kb" }));
app.use(express.json({ limit: "10kb" }));

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
  res.setHeader("Permissions-Policy", "geolocation=(), microphone=()");
  next();
});

// MySQL connection pool using environment variables (do not hardcode secrets)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
  charset: "UTF8MB4_GENERAL_CI",
});

// Simple in-memory session store (example only; use a proper store for production)
const sessions = new Map();

// Basic rate limiter for login attempts per IP
const loginAttempts = new Map();
function allowLoginAttempt(ip) {
  const now = Date.now();
  const windowMs = 15 * 60 * 1000; // 15 minutes
  const maxAttempts = 10;

  const entry = (loginAttempts.get(ip) || []).filter(ts => now - ts < windowMs);
  entry.push(now);
  loginAttempts.set(ip, entry);
  return entry.length <= maxAttempts;
}

// Input validation helpers
function sanitizeUsername(u) {
  if (typeof u !== "string") return "";
  const v = u.trim();
  if (v.length < 3 || v.length > 64) return "";
  // Allow common username characters: letters, digits, dot, underscore, hyphen, at
  if (!/^[A-Za-z0-9._@-]+$/.test(v)) return "";
  return v;
}
function isValidPassword(pw) {
  if (typeof pw !== "string") return false;
  // Do not transform the password; just validate length bounds
  return pw.length >= 8 && pw.length <= 128;
}

// Utility to perform timing-safe password verification with scrypt
function verifyPasswordWithScrypt(password, saltHex, storedHashHex, cb) {
  const saltToUse = Buffer.isBuffer(saltHex) ? saltHex : Buffer.from(saltHex, "hex");
  const storedBuf = Buffer.isBuffer(storedHashHex) ? storedHashHex : Buffer.from(storedHashHex, "hex");

  crypto.scrypt(
    password,
    saltToUse,
    64,
    { N: 16384, r: 8, p: 1, maxmem: 128 * 1024 * 1024 },
    (err, derivedKey) => {
      if (err) return cb(err, false);

      let ok = false;
      try {
        // Compare in constant time if lengths match
        if (storedBuf.length === derivedKey.length) {
          ok = crypto.timingSafeEqual(storedBuf, derivedKey);
        } else {
          ok = false;
        }
      } catch {
        ok = false;
      }
      cb(null, ok);
    }
  );
}

// Dummy verification to normalize response time for non-existent users
function dummyVerify(password, cb) {
  const dummySalt = crypto.randomBytes(16);
  crypto.scrypt(
    password,
    dummySalt,
    64,
    { N: 16384, r: 8, p: 1, maxmem: 128 * 1024 * 1024 },
    () => cb(null, false)
  );
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const ip = req.ip || req.connection?.remoteAddress || "unknown";

  if (!allowLoginAttempt(ip)) {
    // Generic message to avoid giving hints
    return res.status(429).json({ message: "Too many attempts. Try again later." });
  }

  const usernameRaw = req.body?.username;
  const passwordRaw = req.body?.password;

  if (typeof usernameRaw !== "string" || typeof passwordRaw !== "string") {
    return res.status(400).json({ message: "Invalid input." });
  }

  const username = sanitizeUsername(usernameRaw);
  const password = passwordRaw;

  if (!username || !isValidPassword(password)) {
    // Generic message to avoid account enumeration
    return res.status(400).json({ message: "Invalid credentials." });
  }

  // Parameterized query prevents SQL injection
  pool.query(
    "SELECT id, username, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, results) => {
      if (err) {
        // Do not leak internal errors
        return res.status(500).json({ message: "Server error." });
      }

      const user = Array.isArray(results) && results.length ? results[0] : null;

      if (!user || !user.password_hash || !user.password_salt) {
        // Perform dummy verify to mitigate timing attacks
        return dummyVerify(password, () => res.status(401).json({ message: "Invalid credentials." }));
      }

      try {
        verifyPasswordWithScrypt(
          password,
          user.password_salt,
          user.password_hash,
          (vErr, ok) => {
            if (vErr) {
              return res.status(500).json({ message: "Server error." });
            }
            if (!ok) {
              return res.status(401).json({ message: "Invalid credentials." });
            }

            // Create a secure session token and set it as an HttpOnly cookie
            const sid = crypto.randomBytes(32).toString("hex");
            sessions.set(sid, { userId: user.id, createdAt: Date.now() });

            const cookieOpts = {
              httpOnly: true,
              sameSite: "Strict",
              secure: process.env.NODE_ENV === "production", // set true in production (HTTPS)
              maxAge: 60 * 60 * 1000, // 1 hour
              path: "/",
            };

            res.cookie("sid", sid, cookieOpts);
            return res.status(200).json({ message: "Logged in." });
          }
        );
      } catch {
        return res.status(500).json({ message: "Server error." });
      }
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Do not log sensitive data
  console.log(`Server running on port ${PORT}`);
});
