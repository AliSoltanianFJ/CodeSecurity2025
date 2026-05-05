// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
// ...existing code...

const helmet = require("helmet");

const app = express();

// Strict body size limits (helps prevent resource exhaustion / overflow-like issues)
app.use(express.json({ limit: "10kb" }));
app.use(express.urlencoded({ extended: true, limit: "10kb", type: "application/x-www-form-urlencoded" }));
app.use(helmet({
  frameguard: { action: 'deny' },
  xssFilter: true,
  hidePoweredBy: true
}));

// Additional manual header (already present originally)
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// MySQL connection via environment variables (never hardcode secrets)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER || "secure_user",
  password: process.env.DB_PASS || "changeMe!",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 5,
  charset: "utf8mb4"
});

// Basic input sanitization (neutralize characters often used in XSS)
function sanitize(str) {
  return str
    .replace(/[<>&"'\/]/g, c => ({
      "<": "&lt;",
      ">": "&gt;",
      "&": "&amp;",
      '"': "&quot;",
      "'": "&#39;",
      "/": "&#47;"
    }[c]));
}

// Validate structure of body to avoid unsafe deserialization assumptions
function isPlainObject(o) {
  return o && typeof o === "object" && Object.getPrototypeOf(o) === Object.prototype;
}

// Password policy check
function validPassword(pw) {
  if (typeof pw !== "string") return false;
  if (pw.length < 12 || pw.length > 128) return false;
  const complexity = [
    /[a-z]/.test(pw),
    /[A-Z]/.test(pw),
    /[0-9]/.test(pw),
    /[^A-Za-z0-9]/.test(pw)
  ];
  return complexity.every(Boolean);
}

// Username policy check
function validUsername(un) {
  return typeof un === "string" && /^[A-Za-z0-9_]{3,32}$/.test(un);
}

// Secure password hashing (PBKDF2)
function hashPassword(password, cb) {
  const salt = crypto.randomBytes(16).toString("hex");
  const iterations = 310000; // Current recommended high iteration count
  crypto.pbkdf2(password, salt, iterations, 64, "sha512", (err, derivedKey) => {
    if (err) return cb(err);
    // Format: iterations:salt:hash
    cb(null, `${iterations}:${salt}:${derivedKey.toString("hex")}`);
  });
}

// Register handler
// ...existing code...
app.post("/register", (req, res) => {
  try {
    if (!isPlainObject(req.body)) {
      return res.status(400).send("Invalid body.");
    }

    let { username, password } = req.body;

    if (!validUsername(username) || !validPassword(password)) {
      return res.status(400).send("Invalid credentials format.");
    }

    // Sanitize (avoid reflecting raw input)
    username = sanitize(username);

    // Check if user already exists (prepared statement)
    pool.query("SELECT id FROM users WHERE username = ?", [username], (err, rows) => {
      if (err) {
        return res.status(500).send("Server error.");
      }
      if (rows && rows.length > 0) {
        return res.status(409).send("Username taken.");
      }

      hashPassword(password, (hashErr, hashed) => {
        if (hashErr) {
          return res.status(500).send("Server error.");
        }

        // Insert user (never store raw password)
        pool.query(
          "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())",
          [username, hashed],
          (insErr) => {
            if (insErr) {
              return res.status(500).send("Server error.");
            }
            // Generic success response (no sensitive info)
            return res.status(201).send("Registered.");
          }
        );
      });
    });
  } catch {
    return res.status(500).send("Server error.");
  }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
