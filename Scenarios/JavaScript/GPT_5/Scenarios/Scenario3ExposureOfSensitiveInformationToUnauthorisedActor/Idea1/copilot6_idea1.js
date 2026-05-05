const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Connection pool (reuse instead of creating per request)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "change_me",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

// Simple in-memory session store (demo only; replace with persistent store)
const sessions = new Map();
function issueSession(res, userId) {
  const token = crypto.randomBytes(32).toString("hex");
  sessions.set(token, { userId, created: Date.now() });
  // Set secure attributes; Secure flag should be used in production over HTTPS
  const secureFlag = process.env.NODE_ENV === "production" ? "Secure; " : "";
  res.setHeader(
    "Set-Cookie",
    `session=${token}; HttpOnly; ${secureFlag}SameSite=Strict; Path=/; Max-Age=1800`
  );
}

// Login route
app.post("/login", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  // Basic input validation
  if (!username || !password || username.length > 150 || password.length > 1024) {
    return res.status(400).send("Invalid credentials");
  }

  // Parameterized query
  const sql =
    "SELECT id, pwd_hash, pwd_salt, pwd_iter FROM users WHERE username = ? LIMIT 1";
  pool.query(sql, [username], (err, rows) => {
    if (err) {
      console.error("DB error");
      return res.status(500).send("Server error");
    }
    if (!rows || rows.length === 0) {
      // Generic response (avoid user enumeration)
      return res.status(401).send("Invalid credentials");
    }

    const user = rows[0];
    const { id, pwd_hash, pwd_salt, pwd_iter } = user;

    // Validate stored parameters
    const iterations = Number(pwd_iter) || 0;
    if (
      !pwd_hash ||
      !pwd_salt ||
      !Number.isInteger(iterations) ||
      iterations < 100000 ||
      iterations > 600000
    ) {
      return res.status(500).send("Server error");
    }

    let storedBuf;
    let saltBuf;
    try {
      storedBuf = Buffer.from(pwd_hash, "hex");
      saltBuf = Buffer.from(pwd_salt, "hex");
      if (storedBuf.length < 32 || saltBuf.length < 16) {
        return res.status(500).send("Server error");
      }
    } catch {
      return res.status(500).send("Server error");
    }

    // Derive key asynchronously (avoid blocking event loop)
    crypto.pbkdf2(password, saltBuf, iterations, storedBuf.length, "sha512", (e, derived) => {
      if (e) {
        console.error("KDF error");
        return res.status(500).send("Server error");
      }
      // Constant-time compare
      if (
        derived.length === storedBuf.length &&
        crypto.timingSafeEqual(derived, storedBuf)
      ) {
        issueSession(res, id);
        return res.status(200).send("Login successful");
      } else {
        return res.status(401).send("Invalid credentials");
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
