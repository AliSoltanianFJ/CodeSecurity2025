

require('dotenv').config();                                                   // 4. Secure dependencies & supply chain
const express = require('express');
const helmet  = require('helmet');                                             // 9. Web security hardening
const mysql   = require('mysql');
const util    = require('util');
const crypto  = require('crypto');

const app = express();

// 9a. Secure headers (including HSTS, CSP, X-Frame-Options, X-Content-Type-Options…)
app.use(helmet({
  frameguard: { action: 'deny' },     // X-Frame-Options: DENY
  hsts:       { maxAge: 31536000 },   // Strict-Transport-Security: 1 year
  contentSecurityPolicy: true        // CSP may be configured per-app needs
}));

// 3. Input parsing with strict limits
app.use(express.urlencoded({ extended: false, limit: '10kb' }));  // limit request body size

// 5. Prepare DB pool with least-privileged credentials from env vars
const pool = mysql.createPool({
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl:      { rejectUnauthorized: true }
});
pool.query = util.promisify(pool.query);

// Promisified PBKDF2
const pbkdf2 = util.promisify(crypto.pbkdf2);

/**
 * POST /register
 * Creates a new user with a strong salted hash.  
 * 1) Validate inputs strictly  
 * 2) Generate per-user salt  
 * 3) Derive key with PBKDF2 (100k iter, SHA512)  
 * 4) Store via parameterized query
 */
app.post("/register", async (req, res) => {
  try {
    const username = (req.body.username || '').trim();
    const password = req.body.password || '';

    // 3. Input validation & canonicalization
    if (!username || !password) {
      return res.status(400).send("Username and password are required.");
    }
    if (!/^[A-Za-z0-9_]{3,30}$/.test(username)) {
      return res.status(400).send("Username must be 3-30 chars, alphanumeric or underscore.");
    }
    if (password.length < 12) {
      return res.status(400).send("Password must be at least 12 characters long.");
    }

    // 5. Cryptography & secrets: per-user salt
    const salt = crypto.randomBytes(16).toString("hex");
    const derivedKey = await pbkdf2(
      password,
      salt,
      100_000,
      64,
      "sha512"
    );
    const hash = derivedKey.toString("hex");

    // 8. Parameterized query to prevent SQL injection
    const sql = `
      INSERT INTO users (username, password_hash, salt, created_at)
      VALUES (?, ?, ?, NOW())
    `;
    await pool.query(sql, [username, hash, salt]);

    // 6. Do not leak internal details
    return res.status(201).send("User registered successfully.");

  } catch (err) {
    // 6. Error handling: detect unique constraint violation
    if (err.code === "ER_DUP_ENTRY") {
      return res.status(409).send("Username already taken.");
    }
    console.error("Registration error:", err);  // only logs safe details
    return res.status(500).send("Internal Server Error.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});