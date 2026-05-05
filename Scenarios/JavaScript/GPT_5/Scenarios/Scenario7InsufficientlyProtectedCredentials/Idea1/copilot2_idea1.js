// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
// ...existing code...

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=()");
  res.setHeader("Cache-Control", "no-store");
  next();
});

// MySQL pool (table assumed pre-created with columns:
// id, username (UNIQUE), password_hash VARBINARY(96),
// password_salt VARBINARY(32), scrypt_N INT, scrypt_r INT,
// scrypt_p INT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  port: Number(process.env.DB_PORT || 3306),
  connectionLimit: 10,
  multipleStatements: false,
});

// Basic in-memory rate limit (per IP)
const rateMap = new Map();
function rateLimit(key, limit, windowMs) {
  const now = Date.now();
  const entry = rateMap.get(key) || { count: 0, ts: now };
  if (now - entry.ts > windowMs) {
    entry.count = 0;
    entry.ts = now;
  }
  entry.count++;
  rateMap.set(key, entry);
  return entry.count <= limit;
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
  if (!rateLimit(req.ip, 20, 60_000)) {
    return res.status(429).json({ error: "too many requests" });
  }

  const username =
    typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password =
    typeof req.body.password === "string" ? req.body.password : "";

  // Validation
  if (!username || !password) {
    return res.status(400).json({ error: "username and password required" });
  }
  if (
    username.length < 3 ||
    username.length > 48 ||
    !/^[A-Za-z0-9_.\-@]+$/.test(username)
  ) {
    return res.status(400).json({ error: "invalid username" });
  }
  if (
    password.length < 12 ||
    !/[A-Z]/.test(password) ||
    !/[a-z]/.test(password) ||
    !/[0-9]/.test(password) ||
    !/[^\w]/.test(password)
  ) {
    return res
      .status(400)
      .json({ error: "weak password (need upper, lower, digit, symbol, len>=12)" });
  }

  // Check duplicate first
  pool.query(
    "SELECT id FROM users WHERE username = ? LIMIT 1",
    [username],
    (selErr, rows) => {
      if (selErr) {
        return res.status(500).json({ error: "internal error" });
      }
      if (rows && rows.length) {
        return res.status(409).json({ error: "username exists" });
      }

      // Scrypt parameters (moderate memory usage)
      const scryptParams = { N: 1 << 15, r: 8, p: 1 }; // N=32768
      const salt = crypto.randomBytes(32);

      crypto.scrypt(
        password,
        salt,
        64,
        scryptParams,
        (hashErr, derivedKey) => {
          if (hashErr) {
            return res.status(500).json({ error: "internal error" });
          }

          const sql =
            "INSERT INTO users (username, password_hash, password_salt, scrypt_N, scrypt_r, scrypt_p) VALUES (?, ?, ?, ?, ?, ?)";
          const params = [
            username,
            derivedKey,
            salt,
            scryptParams.N,
            scryptParams.r,
            scryptParams.p,
          ];

            pool.query(sql, params, (insErr) => {
              if (insErr) {
                if (insErr.code === "ER_DUP_ENTRY") {
                  return res.status(409).json({ error: "username exists" });
                }
                return res.status(500).json({ error: "internal error" });
              }
              return res.status(201).json({ message: "registered" });
            });
        }
      );
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
