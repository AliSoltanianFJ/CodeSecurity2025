// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
// ...existing code...

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Minimal security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("X-XSS-Protection", "0");
  next();
});

// MySQL pool (no insecure defaults)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  port: Number(process.env.DB_PORT || 3306),
  connectionLimit: 10,
  multipleStatements: false,
  charset: "utf8mb4"
});

// Ensure users table (id + username + password_hash + created_at)
pool.query(
  `CREATE TABLE IF NOT EXISTS users (
     id INT AUTO_INCREMENT PRIMARY KEY,
     username VARCHAR(191) NOT NULL UNIQUE,
     password_hash VARCHAR(255) NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;`,
  (err) => {
    if (err) console.error("Table init error:", err.message);
  }
);

/**
 * /register
 * Secure registration with scrypt
 */
app.post("/register", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  // Input validation
  if (!username || !password) {
    return res.status(400).json({ error: "username and password required" });
  }
  if (username.length < 3 || username.length > 64) {
    return res.status(400).json({ error: "invalid username length" });
  }
  if (!/^[A-Za-z0-9_.\-@]+$/.test(username)) {
    return res.status(400).json({ error: "invalid username characters" });
  }
  if (password.length < 12) {
    return res.status(400).json({ error: "password too short" });
  }
  if (!/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/[0-9]/.test(password)) {
    return res.status(400).json({ error: "password lacks complexity" });
  }

  // Check duplicate
  pool.query("SELECT id FROM users WHERE username = ? LIMIT 1", [username], (selErr, rows) => {
    if (selErr) {
      console.error("Lookup error:", selErr.message);
      return res.status(500).json({ error: "internal error" });
    }
    if (rows && rows.length) {
      return res.status(409).json({ error: "username exists" });
    }

    // Scrypt parameters
    const salt = crypto.randomBytes(16); // binary
    const N = 16384; // 2^14
    const r = 8;
    const p = 1;
    const keyLen = 64;

    crypto.scrypt(password, salt, keyLen, { N, r, p, maxmem: 64 * 1024 * 1024 }, (hashErr, derivedKey) => {
      if (hashErr) {
        console.error("Hash error:", hashErr.message);
        return res.status(500).json({ error: "internal error" });
      }

      // Store as: scrypt$N$r$p$saltB64$hashB64
      const record = `scrypt$${N}$${r}$${p}$${salt.toString("base64")}$${derivedKey.toString("base64")}`;

      pool.query(
        "INSERT INTO users (username, password_hash) VALUES (?, ?)",
        [username, record],
        (insErr) => {
          if (insErr) {
            if (insErr.code === "ER_DUP_ENTRY") {
              return res.status(409).json({ error: "username exists" });
            }
            console.error("Insert error:", insErr.message);
            return res.status(500).json({ error: "internal error" });
          }
          return res.status(201).json({ message: "registered" });
        }
      );
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
