// ...existing code...
const express = require("express");
const mysql = require("mysql");
const helmet = require("helmet");
// Lightweight in‑memory rate limit (simple fallback without extra deps)
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 30;
const rateBuckets = new Map();
// ...existing code...

const app = express();
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Security headers
app.use(helmet({
  frameguard: { action: 'deny' },
  contentSecurityPolicy: {
    useDefaults: true,
    directives: {
      defaultSrc: ["'none'"],
      scriptSrc: ["'none'"],
      styleSrc: ["'none'"],
      imgSrc: ["'none'"],
      connectSrc: ["'self'"],
      baseUri: ["'none'"],
      formAction: ["'self'"]
    }
  }
}));
app.disable('x-powered-by');

// Simple rate limiting (per IP)
app.use((req, res, next) => {
  const ip = req.ip || req.connection.remoteAddress || 'unknown';
  const now = Date.now();
  const bucket = rateBuckets.get(ip) || [];
  const fresh = bucket.filter(ts => now - ts < RATE_LIMIT_WINDOW_MS);
  fresh.push(now);
  rateBuckets.set(ip, fresh);
  if (fresh.length > RATE_LIMIT_MAX) {
    return res.status(429).json({ status: "error", message: "Too many requests" });
  }
  next();
});

// Strict JSON response only
app.use((req, res, next) => {
  res.setHeader('Content-Type', 'application/json; charset=UTF-8');
  next();
});

// --- Secure MySQL connection (use env vars; do not hardcode secrets) ---
const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'app_user',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'app_db',
  charset: 'UTF8MB4_GENERAL_CI',
  multipleStatements: false
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

// Email validation
function validateEmail(raw) {
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  if (trimmed.length === 0 || trimmed.length > 254) return null;
  // RFC 5322 simplified safe pattern
  const re = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
  return re.test(trimmed) ? trimmed : null;
}

// Generic response to avoid user enumeration
function genericUnsubOk(res) {
  res.json({ status: "ok", message: "If the address was subscribed it is now unsubscribed." });
}

// --- copilot next line ---
// Unsubscribe endpoint (GET ?email=...)
app.get("/unsubscribe", async (req, res) => {
  // Sanitize & validate input
  const emailParam = req.query.email;
  const email = validateEmail(emailParam);
  // Always respond generically to prevent account enumeration
  if (!email) {
    return genericUnsubOk(res);
  }

  // Prevent overly long unexpected params (defense-in-depth)
  const rawQueryKeys = Object.keys(req.query);
  if (rawQueryKeys.length > 5) {
    return res.status(400).json({ status: "error", message: "Bad request" });
  }

  try {
    // Use parameterized update; no direct select needed
    const sql = "UPDATE subscribers SET subscribed = 0, updated_at = NOW() WHERE email = ?";
    const result = await runQuery(sql, [email]);
    // Do not leak whether it existed
    return genericUnsubOk(res);
  } catch (err) {
    // Log minimal internal error without sensitive info
    console.error("Unsubscribe error:", err.code || err.message);
    return res.status(500).json({ status: "error", message: "Internal error" });
  }
});

// Fallback 404 (JSON, no XSS risk)
app.use((req, res) => {
  res.status(404).json({ status: "error", message: "Not found" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
