const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Simple in-memory session store (demo purposes only)
const sessions = new Map();

// Create a reusable connection pool
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "change_me",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  charset: "utf8mb4"
});

// Constant parameters (align with how hashes were originally created)
const DEFAULT_ITERATIONS = 310000;
const KEY_LEN = 32;
const DIGEST = "sha256";
// Static fake values to equalize timing for nonexistent users
const FAKE_SALT = Buffer.alloc(16, 0);
const FAKE_HASH = Buffer.alloc(KEY_LEN, 0);

function timingSafeCompare(a, b) {
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

/**
 * Login endpoint: validates input, fetches user, derives hash, issues session.
 * Expects a users table with columns:
 *   id INT, username VARCHAR UNIQUE, password_hash HEX, password_salt HEX, password_iter INT (optional)
 */
app.post("/login", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic input validation
  if (!username || !password ||
      username.length > 150 ||
      password.length > 1024 ||
      !/^[A-Za-z0-9._-]+$/.test(username)) {
    return res.status(400).send("Invalid credentials");
  }

  // Query user
  pool.query(
    "SELECT id, password_hash, password_salt, password_iter FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, results) => {
      if (err) {
        return res.status(500).send("Server error");
      }

      let userRecord = null;
      if (results && results.length === 1) {
        userRecord = results[0];
      }

      // Prepare parameters (real or fake)
      const saltHex = userRecord ? userRecord.password_salt : FAKE_SALT.toString("hex");
      const hashHex = userRecord ? userRecord.password_hash : FAKE_HASH.toString("hex");
      const iterations = userRecord && userRecord.password_iter
        ? Math.min(Math.max(parseInt(userRecord.password_iter, 10), 100000), 600000)
        : DEFAULT_ITERATIONS;

      let saltBuf, storedHashBuf;
      try {
        saltBuf = Buffer.from(saltHex, "hex");
        storedHashBuf = Buffer.from(hashHex, "hex");
        if (saltBuf.length < 8 || storedHashBuf.length !== KEY_LEN) {
          // Malformed stored data
          return res.status(500).send("Server error");
        }
      } catch {
        return res.status(500).send("Server error");
      }

      // Derive asynchronously to avoid blocking event loop
      crypto.pbkdf2(password, saltBuf, iterations, storedHashBuf.length, DIGEST, (e, derived) => {
        if (e) {
          return res.status(500).send("Server error");
        }

        const ok = userRecord && timingSafeCompare(storedHashBuf, derived);
        if (!ok) {
          // Uniform response
            return res.status(401).send("Invalid credentials");
        }

        // Issue session token
        const sessionId = crypto.randomBytes(32).toString("hex");
        sessions.set(sessionId, {
          userId: userRecord.id,
          username,
          createdAt: Date.now()
        });

        // Set secure cookie (adjust secure flag if not behind HTTPS in dev)
        res.cookie("sid", sessionId, {
          httpOnly: true,
          secure: true,
          sameSite: "strict",
          path: "/",
          maxAge: 1000 * 60 * 30 // 30 minutes
        });

        return res.status(200).send("Login successful");
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
