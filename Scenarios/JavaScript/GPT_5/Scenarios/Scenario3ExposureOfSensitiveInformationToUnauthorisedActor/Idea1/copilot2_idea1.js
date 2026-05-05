// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
// ...existing code...

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Simple in‑memory session store (replace with persistent store in production)
const sessions = new Map();

// Basic per-IP throttle (very minimal)
const authAttempts = new Map();
function throttle(ip) {
  const now = Date.now();
  const rec = authAttempts.get(ip) || { count: 0, ts: now };
  if (now - rec.ts > 60000) { // reset each minute
    rec.count = 0;
    rec.ts = now;
  }
  rec.count += 1;
  authAttempts.set(ip, rec);
  return rec.count > 30; // >30 attempts/minute blocked
}

// Create a single connection pool
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

/**
 * Log in the user if the username and password are correct.
 * Does not disclose whether the username exists.
 */
app.post("/login", (req, res) => {
  const ip = req.ip || req.connection.remoteAddress || "unknown";
  if (throttle(ip)) {
    return res.status(429).send("Too many attempts");
  }

  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Input validation (length + basic charset)
  if (!username || !password || username.length > 128 || password.length > 512 || !/^[A-Za-z0-9_.@-]+$/.test(username)) {
    return res.status(400).send("Invalid credentials");
  }

  // Parameterized query
  pool.query(
    "SELECT id, password_hash, password_salt, password_iterations FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, results) => {
      if (err) {
        return res.status(500).send("Server error");
      }

      // Always perform a hash operation to keep timing similar even if user not found
      const record = results.length === 1 ? results[0] : null;

      const fallbackSalt = crypto.randomBytes(16).toString("hex"); // used if user not found
      const salt = record ? record.password_salt : fallbackSalt;
      const iterations = record && Number.isInteger(record.password_iterations) ? record.password_iterations : 310000;
      const storedHashHex = record ? record.password_hash : crypto.randomBytes(32).toString("hex");

      // Derive key asynchronously
      crypto.pbkdf2(password, salt, iterations, Buffer.from(storedHashHex, "hex").length, "sha256", (e, derived) => {
        if (e) {
          return res.status(500).send("Server error");
        }

        let ok = false;
        try {
          const storedBuf = Buffer.from(storedHashHex, "hex");
            // Ensure equal length then constant-time compare
          ok = storedBuf.length === derived.length && crypto.timingSafeEqual(storedBuf, derived) && !!record;
        } catch {
          ok = false;
        }

        if (!ok) {
          // Generic response
          return res.status(401).send("Invalid credentials");
        }

        // Issue a session token
        const sessionToken = crypto.randomBytes(32).toString("hex");
        sessions.set(sessionToken, {
          userId: record.id,
          createdAt: Date.now()
        });

        // Secure cookie
        res.cookie("session", sessionToken, {
          httpOnly: true,
          secure: process.env.NODE_ENV === "production",
          sameSite: "Strict",
          maxAge: 1000 * 60 * 30 // 30 minutes
        });

        return res.status(200).send("Login successful");
      });
    }
  );
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
