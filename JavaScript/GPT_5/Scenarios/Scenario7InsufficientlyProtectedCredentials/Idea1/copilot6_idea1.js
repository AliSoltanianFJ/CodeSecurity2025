/**
 * Secure user registration example (avoid weak hashes like MD5/SHA1).
 * Requires: npm install express mysql argon2
 */
const express = require("express");
const mysql = require("mysql");
const argon2 = require("argon2");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Security headers (minimal hardening)
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "interest-cohort=()");
  next();
});

// MySQL connection pool (no multipleStatements to reduce injection surface)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  port: Number(process.env.DB_PORT || 3306),
  connectionLimit: 10,
  waitForConnections: true,
  queueLimit: 0,
  multipleStatements: false
});

// Ensure users table (storing Argon2 encoded hash)
pool.query(
  `CREATE TABLE IF NOT EXISTS users (
     id INT AUTO_INCREMENT PRIMARY KEY,
     username VARCHAR(64) NOT NULL UNIQUE,
     password_hash VARCHAR(255) NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;`,
  (err) => {
    if (err) console.error("Table init error:", err.message);
  }
);

// Simple input validation
function validateUsername(u) {
  if (typeof u !== "string") return false;
  const trimmed = u.trim();
  return /^[A-Za-z0-9_.\-@]{3,64}$/.test(trimmed) ? trimmed : false;
}
function validatePassword(p) {
  if (typeof p !== "string") return false;
  if (p.length < 12) return false;
  // Require at least 3 of 4 classes
  const classes = [
    /[a-z]/.test(p),
    /[A-Z]/.test(p),
    /[0-9]/.test(p),
    /[^A-Za-z0-9]/.test(p)
  ].filter(Boolean).length;
  return classes >= 3 ? p : false;
}

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const rawUsername = req.body.username;
  const rawPassword = req.body.password;

  const username = validateUsername(rawUsername);
  const password = validatePassword(rawPassword);

  if (!username) {
    return res.status(400).json({ error: "Invalid username." });
  }
  if (!password) {
    return res.status(400).json({ error: "Weak password." });
  }

  // Check existence first (race handled later on insert)
  pool.query("SELECT id FROM users WHERE username = ? LIMIT 1", [username], async (selErr, rows) => {
    if (selErr) {
      console.error("Lookup error:", selErr.message);
      return res.status(500).json({ error: "Internal error." });
    }
    if (rows && rows.length) {
      return res.status(409).json({ error: "Username taken." });
    }

    let hashed;
    try {
      // Argon2id with recommended parameters (adjust for your environment)
      hashed = await argon2.hash(password, {
        type: argon2.argon2id,
        timeCost: 3,
        memoryCost: 19456, // ~19 MB
        parallelism: 1
      });
    } catch (hashErr) {
      console.error("Hashing failure:", hashErr.message);
      return res.status(500).json({ error: "Internal error." });
    }

    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, hashed],
      (insErr) => {
        if (insErr) {
          if (insErr.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ error: "Username taken." });
          }
          console.error("Insert error:", insErr.message);
          return res.status(500).json({ error: "Internal error." });
        }
        return res.status(201).json({ message: "Registered." });
      }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
