const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const argon2 = require("argon2");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "same-origin");
  next();
});

// Single connection (adjust to pool if high concurrency needed)
const db = mysql.createConnection({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "secure_app",
  multipleStatements: false
});

db.connect(err => {
  if (err) {
    console.error("DB connect failed:", err.message);
    process.exit(1);
  }
  const ddl = `
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(64) NOT NULL UNIQUE,
      password_hash VARBINARY(128) NOT NULL,
      hash_algorithm VARCHAR(32) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;`;
  db.query(ddl, e => {
    if (e) {
      console.error("Schema init failed:", e.message);
      process.exit(1);
    }
  });
});

// Helper: validate username
function isUsernameValid(u) {
  return typeof u === "string" &&
    /^[A-Za-z0-9_.\-@]{3,64}$/.test(u);
}

// Helper: validate password complexity
function isPasswordValid(p) {
  return typeof p === "string" &&
    p.length >= 12 &&
    /[a-z]/.test(p) &&
    /[A-Z]/.test(p) &&
    /[0-9]/.test(p) &&
    /[^A-Za-z0-9]/.test(p);
}

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const username = (typeof req.body.username === "string" ? req.body.username.trim() : "");
  const password = (typeof req.body.password === "string" ? req.body.password : "");

  if (!isUsernameValid(username)) {
    return res.status(400).json({ error: "invalid username" });
  }
  if (!isPasswordValid(password)) {
    return res.status(400).json({ error: "weak password" });
  }

  // Optional pepper (do NOT log)
  const pepper = process.env.PEPPER || "";
  const toHash = password + pepper;

  // Pre-check existence (race handled by unique index)
  db.query("SELECT id FROM users WHERE username = ? LIMIT 1", [username], async (selErr, rows) => {
    if (selErr) {
      return res.status(500).json({ error: "internal" });
    }
    if (rows && rows.length) {
      return res.status(409).json({ error: "taken" });
    }

    try {
      // Argon2id with explicit params
      const hash = await argon2.hash(toHash, {
        type: argon2.argon2id,
        memoryCost: 19456, // ~19 MB
        timeCost: 2,
        parallelism: 1
      });

      // Store as raw bytes (decode base64 section) OR keep full encoded string
      // Here keep encoded string (UTF-8) -> convert to Buffer
      const hashBuf = Buffer.from(hash, "utf8");

      db.query(
        "INSERT INTO users (username, password_hash, hash_algorithm) VALUES (?, ?, ?)",
        [username, hashBuf, "argon2id"],
        (insErr) => {
          if (insErr) {
            if (insErr.code === "ER_DUP_ENTRY") {
              return res.status(409).json({ error: "taken" });
            }
            return res.status(500).json({ error: "internal" });
          }
          return res.status(201).json({ message: "registered" });
        }
      );
    } catch (hashErr) {
      return res.status(500).json({ error: "internal" });
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
