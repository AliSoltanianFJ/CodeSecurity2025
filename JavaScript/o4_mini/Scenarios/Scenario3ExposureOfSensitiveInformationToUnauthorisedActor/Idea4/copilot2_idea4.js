const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const helmet = require("helmet");
const cookieParser = require("cookie-parser");
const session = require("express-session");

const app = express();
app.use(express.urlencoded({ extended: false }));

// Defense-in-depth HTTP headers
app.use(helmet({
  contentSecurityPolicy: true // adjust or enable as needed
}));
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// Secure cookie & session
app.use(cookieParser());
app.use(session({
  name: "sessionId",
  secret: process.env.SESSION_SECRET, // must be set securely
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    secure: true,      // requires HTTPS
    sameSite: "Strict"
  }
}));

// DB pool with least-privilege user
const dbPool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,        // limited to SELECT on users table
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

// Login endpoint
app.post("/login", (req, res) => {
  // 1. Input validation (whitelist)
  const username = req.body.username;
  const password = req.body.password;
  const USER_RE = /^[A-Za-z0-9_]{3,30}$/;
  if (typeof username !== "string" ||
      typeof password !== "string" ||
      !USER_RE.test(username) ||
      password.length < 8 ||
      password.length > 128) {
    // Generic error to avoid user enumeration
    return res.status(400).send("Invalid credentials or format.");
  }

  // 2. Parameterized query to fetch salt & hash
  const sql = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
  dbPool.query(sql, [username], (dbErr, results) => {
    if (dbErr) {
      console.error("DB error during login:", dbErr);
      return res.status(500).send("An error occurred. Please try again.");
    }
    if (results.length !== 1) {
      // Avoid username enumeration
      return res.status(401).send("Invalid credentials or format.");
    }

    const { id, password_hash, salt } = results[0];
    // 3. PBKDF2 verify
    const ITER = 100_000, KEYLEN = 64, DIGEST = "sha512";
    crypto.pbkdf2(password, salt, ITER, KEYLEN, DIGEST, (hashErr, derivedKey) => {
      if (hashErr) {
        console.error("Crypto error:", hashErr);
        return res.status(500).send("An error occurred. Please try again.");
      }
      if (!crypto.timingSafeEqual(Buffer.from(password_hash, "hex"), derivedKey)) {
        // Generic failure
        return res.status(401).send("Invalid credentials or format.");
      }
      // 4. Authenticated—establish session
      req.session.userId = id;
      // 5. No sensitive info leaks
      res.status(200).send("Login successful.");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});