"use strict";

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Body parsing
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  res.setHeader("Cache-Control", "no-store");
  next();
});

// MySQL connection pool (no insecure defaults; configure via env)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 10,
  multipleStatements: false,
  charset: "utf8mb4",
});

// Ensure users table (separate salt, hash, metadata)
pool.query(
  `
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARBINARY(96) NOT NULL,
  password_salt VARBINARY(32) NOT NULL,
  algorithm VARCHAR(32) NOT NULL,
  params VARCHAR(128) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
`,
  (err) => {
    if (err) console.error("Table init error.");
  }
);

// Helper: secure scrypt hashing (with optional pepper)
function hashPassword(password, cb) {
  const salt = crypto.randomBytes(32); // 256-bit salt
  const pepper = process.env.PASSWORD_PEPPER || ""; // Optional server-side secret
  // Strong scrypt params (increase N if acceptable for latency)
  const N = 32768;
  const r = 8;
  const p = 1;
  const keyLen = 64; // 512-bit
  crypto.scrypt(password + pepper, salt, keyLen, { N, r, p }, (err, derivedKey) => {
    if (err) return cb(err);
    cb(null, {
      algorithm: "scrypt",
      params: `N=${N},r=${r},p=${p},len=${keyLen}`,
      salt,
      hash: derivedKey,
    });
  });
}

// Basic password strength checks
function passwordStrong(pw) {
  if (typeof pw !== "string" || pw.length < 12) return false;
  const hasUpper = /[A-Z]/.test(pw);
  const hasLower = /[a-z]/.test(pw);
  const hasDigit = /\d/.test(pw);
  const hasSymbol = /[^A-Za-z0-9]/.test(pw);
  return hasUpper && hasLower && hasDigit && hasSymbol;
}

// Registration route
app.post("/register", (req, res) => {
  const usernameRaw = req.body.username;
  const passwordRaw = req.body.password;

  const username =
    typeof usernameRaw === "string" ? usernameRaw.trim() : "";
  const password =
    typeof passwordRaw === "string" ? passwordRaw : "";

  // Validate username
  if (
    !username ||
    username.length < 3 ||
    username.length > 64 ||
    !/^[A-Za-z0-9_.@\-]+$/.test(username)
  ) {
    return res.status(400).json({ error: "Invalid input." });
  }

  // Validate password strength
  if (!passwordStrong(password)) {
    return res.status(400).json({ error: "Weak password." });
  }

  // Check uniqueness (race condition still handled by unique index)
  pool.query(
    "SELECT id FROM users WHERE username = ? LIMIT 1",
    [username],
    (selectErr, rows) => {
      if (selectErr) {
        return res.status(500).json({ error: "Server error." });
      }
      if (rows && rows.length > 0) {
        return res.status(409).json({ error: "Username taken." });
      }

      hashPassword(password, (hashErr, result) => {
        if (hashErr) {
          return res.status(500).json({ error: "Server error." });
        }

        const { algorithm, params, salt, hash } = result;
        // Store binary safely with parameterized query
        pool.query(
          "INSERT INTO users (username, password_hash, password_salt, algorithm, params) VALUES (?, ?, ?, ?, ?)",
          [username, hash, salt, algorithm, params],
          (insertErr) => {
            if (insertErr) {
              if (insertErr.code === "ER_DUP_ENTRY") {
                return res.status(409).json({ error: "Username taken." });
              }
              return res.status(500).json({ error: "Server error." });
            }
            return res.status(201).json({ message: "Registered." });
          }
        );
      });
    }
  );
});

const PORT = Number(process.env.PORT || 3000);
app.listen(PORT, () => {
  console.log(`Listening on ${PORT}`);
});
