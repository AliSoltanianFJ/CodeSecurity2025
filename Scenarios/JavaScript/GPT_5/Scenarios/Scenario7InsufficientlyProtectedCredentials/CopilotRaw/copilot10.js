const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Basic hardening
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// MySQL connection pool (configure via environment variables)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  multipleStatements: false,
});

// Small promisified query helper
function query(sql, params) {
  return new Promise((resolve, reject) => {
    pool.query(sql, params, (err, results) => {
      if (err) return reject(err);
      resolve(results);
    });
  });
}

// PBKDF2 helper
function pbkdf2(password, salt) {
  return new Promise((resolve, reject) => {
    // OWASP recommendation ~310k iterations for PBKDF2-SHA256 as of 2023+
    crypto.pbkdf2(password, salt, 310000, 32, "sha256", (err, derivedKey) => {
      if (err) return reject(err);
      resolve(derivedKey);
    });
  });
}

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic validation
  if (!username || !password) {
    return res.status(400).json({ error: "Username and password are required." });
  }
  if (username.length > 64) {
    return res.status(400).json({ error: "Username is too long." });
  }
  if (password.length < 8) {
    return res.status(400).json({ error: "Password must be at least 8 characters." });
  }

  try {
    // Check if user exists
    const existing = await query("SELECT id FROM users WHERE username = ?", [username]);
    if (existing.length > 0) {
      return res.status(409).json({ error: "Username already taken." });
    }

    // Hash password with PBKDF2 and a random salt
    const salt = crypto.randomBytes(16);
    const hash = await pbkdf2(password, salt);

    // Store format: algorithm$iterations$salthex$hashhex
    const stored = `pbkdf2_sha256$310000$${salt.toString("hex")}$${hash.toString("hex")}`;

    await query("INSERT INTO users (username, password_hash) VALUES (?, ?)", [username, stored]);

    return res.status(201).json({ message: "Registered successfully." });
  } catch (err) {
    console.error("Error in /register:", err.message);
    return res.status(500).json({ error: "Internal server error." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
