const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Basic security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// MySQL connection pool (adjust env vars in deployment)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 10,
  multipleStatements: false
});

// Ensure users table exists
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
 * /register
 * Secure user registration with scrypt password hashing.
 */
app.post("/register", (req, res) => {
  const usernameRaw = req.body && typeof req.body.username === "string" ? req.body.username : "";
  const passwordRaw = req.body && typeof req.body.password === "string" ? req.body.password : "";

  const username = usernameRaw.trim();
  const password = passwordRaw;

  // Validate username
  if (!username || username.length < 3 || username.length > 64) {
    return res.status(400).json({ error: "invalid username length" });
  }
  if (!/^[A-Za-z0-9_.\-@]+$/.test(username)) {
    return res.status(400).json({ error: "invalid username characters" });
  }

  // Validate password (length + basic complexity)
  if (password.length < 12 ||
      !/[A-Z]/.test(password) ||
      !/[a-z]/.test(password) ||
      !/[0-9]/.test(password) ||
      !/[^\w\s]/.test(password)) {
    return res.status(400).json({ error: "weak password" });
  }

  // Check uniqueness
  pool.query("SELECT id FROM users WHERE username = ? LIMIT 1", [username], (selErr, rows) => {
    if (selErr) {
      console.error("Lookup error:", selErr.message);
      return res.status(500).json({ error: "internal error" });
    }
    if (rows && rows.length) {
      return res.status(409).json({ error: "username exists" });
    }

    const salt = crypto.randomBytes(16); // 128-bit salt
    const scryptParams = { N: 16384, r: 8, p: 1 };
    const keyLen = 64; // 512-bit derived key

    crypto.scrypt(password, salt, keyLen, scryptParams, (hashErr, derivedKey) => {
      if (hashErr) {
        console.error("Hash error:", hashErr.message);
        return res.status(500).json({ error: "internal error" });
      }

      // Format: algo$N$r$p$saltB64$hashB64
      const passwordHash = [
        "scrypt",
        scryptParams.N,
        scryptParams.r,
        scryptParams.p,
        salt.toString("base64"),
        derivedKey.toString("base64")
      ].join("$");

      pool.query(
        "INSERT INTO users (username, password_hash) VALUES (?, ?)",
        [username, passwordHash],
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
  console.log(`Server running on port ${PORT}`);
});
