const express = require("express");
const mysql = require("mysql2/promise");
const crypto = require("crypto");
const { promisify } = require("util");

const app = express();
app.use(express.urlencoded({ extended: false }));

// Defense-in-depth HTTP headers
app.use((req, res, next) => {
  res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  res.setHeader("Content-Security-Policy", "default-src 'none'; form-action 'self'; base-uri 'self'");
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// Pinned, least-privilege pool via env vars
const dbPool = mysql.createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER,           // only INSERT on users table
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  ssl: { rejectUnauthorized: true },
  connectionLimit: 5
});

const scrypt = promisify(crypto.scrypt);

// register URL
app.post("/register", async (req, res) => {
  try {
    // 1. Input validation
    const username = (req.body.username || "").trim();
    const password = req.body.password || "";
    const usernameRegex = /^[A-Za-z0-9_]{3,30}$/;
    if (!usernameRegex.test(username) || password.length < 12) {
      return res.status(400).send("Invalid input.");
    }

    // 2. Salt & hash password with scrypt
    const salt = crypto.randomBytes(16).toString("hex");
    const derivedKey = await scrypt(password, salt, 64);
    const passwordHash = derivedKey.toString("hex");

    // 3. Parameterized INSERT
    const sql = `
      INSERT INTO users (username, password_hash, salt)
      VALUES (?, ?, ?)
    `;
    await dbPool.execute(sql, [username, passwordHash, salt]);

    // 4. Success (generic message)
    res.status(201).send("User registered.");
  } catch (err) {
    // 5. Handle duplicates & generic errors
    if (err.code === "ER_DUP_ENTRY") {
      return res.status(409).send("Username already exists.");
    }
    // log.err(err)  <-- hook into secure audit/log system
    res.status(500).send("Server error.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});