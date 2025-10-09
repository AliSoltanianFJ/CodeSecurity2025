const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { promisify } = require("util");

const scrypt = promisify(crypto.scrypt);
const app = express();

// 1) Ensure secure configs via ENV
["DB_HOST", "DB_USER", "DB_PASS", "DB_NAME"].forEach((key) => {
  if (!process.env[key]) {
    console.error(`ERROR: Missing required env var ${key}`);
    process.exit(1);
  }
});

// 2) DB pool with least-privilege user
const pool = mysql.createPool({
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit:    5,
  queueLimit:         0
});

app.use(express.urlencoded({ extended: false }));
app.use(express.json());

// 3) Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.post("/register", async (req, res) => {
  try {
    const username = (req.body.username || "").trim();
    const password = req.body.password || "";

    // 4) Username whitelist: 3-30 chars, alphanumeric + underscore
    if (!/^[A-Za-z0-9_]{3,30}$/.test(username)) {
      return res.status(400).send("Invalid username or password.");
    }

    // 5) Strong password policy: min 12 chars, upper, lower, digit, special
    const pwdPolicy = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{12,}$/;
    if (!pwdPolicy.test(password)) {
      return res.status(400).send("Invalid username or password.");
    }

    // 6) Check for existing user (prevents duplicates, mitigates enumeration)
    const [existing] = await new Promise((resolve, reject) => {
      pool.query(
        "SELECT 1 FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, rows) => (err ? reject(err) : resolve(rows))
      );
    });
    if (existing) {
      // Uniform message
      return res.status(409).send("Registration failed.");
    }

    // 7) Hash password with per-user salt using scrypt
    const salt = crypto.randomBytes(16).toString("hex");
    const derivedKey = await scrypt(password, salt, 64);
    const passwordHash = `${salt}:${derivedKey.toString("hex")}`;

    // 8) Parameterized INSERT
    await new Promise((resolve, reject) => {
      pool.query(
        "INSERT INTO users (username, password_hash) VALUES (?, ?)",
        [username, passwordHash],
        (err) => (err ? reject(err) : resolve())
      );
    });

    // 9) 201 Created on success
    res.status(201).send("User registered.");
  } catch (err) {
    // 10) Do NOT leak stack or DB errors
    console.error("Registration error:", err);
    res.status(500).send("Server error.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));