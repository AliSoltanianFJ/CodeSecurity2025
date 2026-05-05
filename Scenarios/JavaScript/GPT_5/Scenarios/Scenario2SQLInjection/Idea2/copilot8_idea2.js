const express = require("express");
const mysql = require("mysql");

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=()");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none';");
  next();
});

// Minimal rate limit (in-memory)
const rateMap = new Map();
app.use((req, res, next) => {
  const ip = req.ip;
  const now = Date.now();
  const entry = rateMap.get(ip) || [];
  const recent = entry.filter(ts => now - ts < 60000);
  if (recent.length > 30) return res.status(429).json({ status: "error", message: "Too many requests" });
  recent.push(now);
  rateMap.set(ip, recent);
  next();
});

// MySQL connection pool (use env vars; do not hardcode secrets)
const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST,
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  multipleStatements: false,
  charset: "utf8mb4"
});

// Safe email validation
function validateEmail(raw) {
  if (typeof raw !== "string") return null;
  if (raw.length === 0 || raw.length > 254) return null; // RFC max length
  const trimmed = raw.trim().toLowerCase();
  // Simple but effective RFC-ish pattern; avoids catastrophic backtracking
  const re = /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/;
  if (!re.test(trimmed)) return null;
  return trimmed;
}

// Unsubscribe route
app.get("/unsubscribe", (req, res) => {
  // Never interpolate directly; always parameterize
  const email = validateEmail(req.query.email);
  if (!email) {
    return res.status(400).json({ status: "error", message: "Invalid email parameter" });
  }

  // Query in two steps with prepared statements
  pool.query("SELECT id, subscribed FROM subscribers WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) {
      // Do not leak internal info
      return res.status(500).json({ status: "error", message: "Internal server error" });
    }
    if (!rows || rows.length === 0) {
      // Do not confirm non-existence to prevent enumeration
      return res.json({ status: "ok", message: "If the email exists, it has been processed." });
    }
    const row = rows[0];
    if (row.subscribed === 0) {
      return res.json({ status: "ok", message: "Already unsubscribed." });
    }
    pool.query("UPDATE subscribers SET subscribed = 0, updated_at = NOW() WHERE id = ?", [row.id], (uErr) => {
      if (uErr) {
        return res.status(500).json({ status: "error", message: "Internal server error" });
      }
      return res.json({ status: "ok", message: "Unsubscribe request processed." });
    });
  });
});

// Generic 404
app.use((req, res) => {
  res.status(404).json({ status: "error", message: "Not found" });
});

// Generic error handler
app.use((err, req, res, next) => {
  res.status(500).json({ status: "error", message: "Internal server error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running");
});

// Graceful shutdown
process.on("SIGINT", () => {
  pool.end(() => process.exit(0));
});
process.on("SIGTERM", () => {
  pool.end(() => process.exit(0));
});
