const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { promisify } = require("util");

const scrypt = promisify(crypto.scrypt);

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Basic security header (already present)
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Configure MySQL connection pool via environment variables
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  multipleStatements: false,
  timezone: "Z",
});

// Ensure the users table exists
pool.query(
  `CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`,
  (err) => {
    if (err) {
      console.error("Failed to ensure users table exists:", err);
      process.exit(1);
    }
  }
);

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  try {
    const username = (req.body.username || "").trim();
    const password = req.body.password || "";

    // Basic validation
    if (!username || !password) {
      return res.status(400).json({ message: "Username and password are required." });
    }
    if (!/^[a-zA-Z0-9._-]{3,32}$/.test(username)) {
      return res.status(400).json({ message: "Username must be 3-32 chars (letters, digits, . _ -)." });
    }
    if (password.length < 12) {
      return res.status(400).json({ message: "Password must be at least 12 characters long." });
    }

    // Securely hash the password using scrypt with a random salt
    const salt = crypto.randomBytes(16); // 128-bit salt
    const keyLen = 64; // 512-bit derived key
    const derivedKey = await scrypt(password, salt, keyLen); // default cost params are strong

    // Store as algorithm:salt_hex:hash_hex
    const passwordHash = `scrypt:${salt.toString("hex")}:${Buffer.from(derivedKey).toString("hex")}`;

    // Insert user with parameterized query
    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, passwordHash],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ message: "Username already exists." });
          }
          console.error("DB insert error:", err);
          return res.status(500).json({ message: "Internal server error." });
        }
        return res.status(201).json({ message: "User registered successfully." });
      }
    );
  } catch (e) {
    console.error("Registration error:", e);
    return res.status(500).json({ message: "Internal server error." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
