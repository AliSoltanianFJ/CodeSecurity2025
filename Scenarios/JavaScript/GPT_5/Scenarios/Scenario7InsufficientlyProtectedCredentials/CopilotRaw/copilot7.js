const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const scryptAsync = util.promisify(crypto.scrypt);

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 3306,
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  multipleStatements: false,
});

// Create users table if it does not exist
pool.query(
  `
  CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(191) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
`,
  (err) => {
    if (err) {
      console.error("DB init error:", err);
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

    if (!username || !password) {
      return res.status(400).send("Username and password are required.");
    }

    // Basic input validation for username
    if (!/^[a-zA-Z0-9_\.@-]{3,64}$/.test(username)) {
      return res.status(400).send("Invalid username.");
    }

    if (password.length < 8 || password.length > 128) {
      return res.status(400).send("Password must be 8-128 characters.");
    }

    // Derive a secure password hash using scrypt with per-user salt
    const salt = crypto.randomBytes(16);
    const N = 16384;
    const r = 8;
    const p = 1;
    const keyLen = 64;

    const derivedKey = await scryptAsync(password, salt, keyLen, {
      N,
      r,
      p,
      maxmem: 128 * 1024 * 1024,
    });

    // Store a single credential string: algorithm$params$salt$hash (base64)
    const credential = `scrypt$${N}$${r}$${p}$${salt.toString("base64")}$${Buffer.from(
      derivedKey
    ).toString("base64")}`;

    // Insert with parameterized query to prevent SQL injection
    pool.query(
      "INSERT INTO users (username, password) VALUES (?, ?)",
      [username, credential],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already exists.");
          }
          console.error("DB insert error:", err);
          return res.status(500).send("Registration failed.");
        }
        return res.status(201).send("User registered.");
      }
    );
  } catch (e) {
    console.error("Register handler error:", e);
    return res.status(500).send("Registration failed.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
