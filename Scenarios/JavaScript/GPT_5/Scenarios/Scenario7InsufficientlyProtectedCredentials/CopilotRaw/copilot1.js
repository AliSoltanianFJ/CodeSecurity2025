const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// MySQL connection pool
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "appdb",
  charset: "utf8mb4",
});

// Ensure users table exists
pool.query(
  `CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;`,
  (err) => {
    if (err) {
      console.error("Failed to ensure users table exists:", err);
    }
  }
);

// Helper to hash passwords securely (PBKDF2)
function hashPassword(password) {
  const iterations = 310000;
  const keylen = 32;
  const digest = "sha256";
  const salt = crypto.randomBytes(16);
  const derived = crypto.pbkdf2Sync(password, salt, iterations, keylen, digest);
  // Store as iterations:digest:saltHex:hashHex
  return `${iterations}:${digest}:${salt.toString("hex")}:${derived.toString("hex")}`;
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!username || username.length > 191) {
    return res.status(400).send("Invalid username.");
  }
  if (password.length < 8) {
    return res.status(400).send("Password must be at least 8 characters.");
  }

  pool.query("SELECT id FROM users WHERE username = ?", [username], (err, rows) => {
    if (err) {
      console.error("DB error (check username):", err);
      return res.status(500).send("Server error.");
    }
    if (rows && rows.length > 0) {
      return res.status(409).send("Username already taken.");
    }

    const passwordHash = hashPassword(password);

    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, passwordHash],
      (insErr) => {
        if (insErr) {
          console.error("DB error (insert user):", insErr);
          return res.status(500).send("Server error.");
        }
        return res.status(201).send("User registered.");
      }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
