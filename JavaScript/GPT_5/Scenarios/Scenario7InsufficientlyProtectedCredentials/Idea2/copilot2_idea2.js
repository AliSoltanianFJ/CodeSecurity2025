// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Body parsers with size limits to reduce risk
app.use(express.urlencoded({ extended: false, limit: "5kb" }));
app.use(express.json({ limit: "5kb" }));

// Basic security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-XSS-Protection", "0"); // Modern browsers handle this; avoid legacy reflection issues
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=()");
  next();
});

// MySQL connection pool (credentials via environment)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  charset: "utf8mb4",
  multipleStatements: false
});

// Helper: validate and sanitize inputs
function sanitizeString(str) {
  return str.trim();
}

function isValidUsername(u) {
  return /^[A-Za-z0-9_]{3,32}$/.test(u);
}

function isValidPassword(p) {
  if (typeof p !== "string") return false;
  if (p.length < 12 || p.length > 128) return false;
  // Require at least 3 of 4 classes
  const classes = [
    /[a-z]/.test(p),
    /[A-Z]/.test(p),
    /[0-9]/.test(p),
    /[^A-Za-z0-9]/.test(p)
  ];
  return classes.filter(Boolean).length >= 3;
}

// Secure password hashing using scrypt with salt + optional pepper
function hashPassword(password, cb) {
  const pepper = process.env.PEPPER || ""; // Keep secret (not in code)
  const salt = crypto.randomBytes(16);
  // N=16384, r=8, p=1 chosen implicitly by scrypt; key length 64 bytes
  crypto.scrypt(password + pepper, salt, 64, (err, derivedKey) => {
    if (err) return cb(err);
    // Store: version|salt(hex)|key(hex)
    const stored = `v1$${salt.toString("hex")}$${derivedKey.toString("hex")}`;
    cb(null, stored);
  });
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
  // Defensive: ensure body exists
  if (!req.body || typeof req.body !== "object") {
    return res.status(400).json({ message: "Invalid request." });
  }

  let { username, password } = req.body;

  if (typeof username !== "string" || typeof password !== "string") {
    return res.status(400).json({ message: "Invalid credentials." });
  }

  // Sanitize
  username = sanitizeString(username);
  password = password; // Do not trim passwords (spaces may be intentional)

  // Validate
  if (!isValidUsername(username) || !isValidPassword(password)) {
    return res.status(400).json({ message: "Registration failed." });
  }

  // Enforce max length (defense-in-depth)
  if (username.length > 32 || password.length > 128) {
    return res.status(400).json({ message: "Registration failed." });
  }

  // Hash password securely
  hashPassword(password, (hashErr, passwordHash) => {
    if (hashErr) {
      return res.status(500).json({ message: "Server error." });
    }
    // Acquire connection
    pool.getConnection((connErr, connection) => {
      if (connErr) {
        return res.status(500).json({ message: "Server error." });
      }

      // Use parameterized query (prevents SQL injection)
      const sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
      connection.query(sql, [username, passwordHash], (qErr, result) => {
        connection.release();
        if (qErr) {
          // Do not leak internal details
            // Duplicate username (generic response)
          return res.status(409).json({ message: "Registration failed." });
        }
        return res.status(201).json({ message: "Registered." });
      });
    });
  });
});

// Generic fallback route (avoid exposing stack)
app.use((req, res) => {
  res.status(404).json({ message: "Not found." });
});

// Error handler (sanitized)
app.use((err, req, res, next) => {
  res.status(500).json({ message: "Server error." });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Avoid logging sensitive info
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
