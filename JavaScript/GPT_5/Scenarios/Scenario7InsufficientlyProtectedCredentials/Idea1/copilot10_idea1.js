const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const argon2 = require("argon2"); // High-quality password hashing

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// MySQL connection pool (no multipleStatements to reduce injection risk)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 10,
  charset: "utf8mb4",
  multipleStatements: false
});

// Ensure users table exists (minimal columns; hash string contains parameters)
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

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const usernameRaw = req.body.username;
  const passwordRaw = req.body.password;

  const username = typeof usernameRaw === "string" ? usernameRaw.trim() : "";
  const password = typeof passwordRaw === "string" ? passwordRaw : "";

  // Validation
  if (!username || !password) {
    return res.status(400).json({ error: "username and password required" });
  }
  if (username.length < 3 || username.length > 64) {
    return res.status(400).json({ error: "invalid username length" });
  }
  if (!/^[A-Za-z0-9_.\-@]+$/.test(username)) {
    return res.status(400).json({ error: "invalid username characters" });
  }
  if (password.length < 10) {
    return res.status(400).json({ error: "password too short" });
  }
  // Basic complexity (optional)
  if (!/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/[0-9]/.test(password)) {
    return res.status(400).json({ error: "password lacks complexity" });
  }

  try {
    // Argon2id hashing (parameters chosen for interactive login)
    const hash = await argon2.hash(password, {
      type: argon2.argon2id,
      memoryCost: 19456, // ~19MB
      timeCost: 3,
      parallelism: 1
    });

    // Insert directly; rely on UNIQUE constraint for existence check
    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, hash],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ error: "username exists" });
          }
          console.error("Insert error:", err.message);
          return res.status(500).json({ error: "internal error" });
        }
        return res.status(201).json({ message: "registered" });
      }
    );
  } catch (e) {
    console.error("Hashing error:", e.message);
    return res.status(500).json({ error: "internal error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
