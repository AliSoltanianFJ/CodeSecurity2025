// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Basic hardening headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "same-origin");
  next();
});

// Connection pool (reused)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  multipleStatements: false
});

// Simple in‑memory session store (replace with robust store in production)
const sessions = new Map();

// Uniform error response helper
function invalid(res) {
  return res.status(401).json({ error: "Invalid credentials" });
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = (req.body.username || "").trim();
    const password = req.body.password || "";
    // --- copilot next line ---
    // Input validation (avoid user enumeration + unreasonable sizes)
    if (
      !username ||
      !password ||
      username.length > 64 ||
      password.length > 128 ||
      !/^[A-Za-z0-9_.-]+$/.test(username) ||
      password.length < 8
    ) {
      return res.status(400).json({ error: "Invalid input" });
    }

    // Fetch user record with constant-time style flow
    pool.query(
      "SELECT id, password_hash, password_salt, iterations FROM users WHERE username = ? LIMIT 1",
      [username],
      (err, rows) => {
        if (err) {
          console.error("DB error:", err);
          return res.status(500).json({ error: "Server error" });
        }

        // If user not found, fabricate a comparable hash operation to reduce timing differences
        const user = rows.length === 1 ? rows[0] : null;
        const salt = user ? user.password_salt : crypto.randomBytes(16).toString("hex");
        const storedHex = user ? user.password_hash : crypto.randomBytes(32).toString("hex");
        const iterations = user ? Math.min(Math.max(user.iterations || 310000, 100000), 500000) : 310000;
        const storedBuf = Buffer.from(storedHex, "hex");

        crypto.pbkdf2(password, salt, iterations, storedBuf.length, "sha256", (e, derived) => {
          if (e) {
            console.error("PBKDF2 error:", e);
            return res.status(500).json({ error: "Server error" });
          }

          let authenticated = false;
          if (storedBuf.length === derived.length) {
            try {
              authenticated = crypto.timingSafeEqual(storedBuf, derived) && !!user;
            } catch {
              authenticated = false;
            }
          }

          if (!authenticated) {
            return invalid(res);
          }

          // Create session token
          const sessionId = crypto.randomBytes(32).toString("hex");
            sessions.set(sessionId, {
              uid: user.id,
              created: Date.now()
            });

          // Set cookie (Secure flag only if behind TLS; SameSite helps mitigate CSRF)
          res.cookie("sid", sessionId, {
            httpOnly: true,
            sameSite: "Strict",
            secure: req.secure || req.headers["x-forwarded-proto"] === "https",
            maxAge: 1000 * 60 * 30 // 30 minutes
          });

          return res.status(200).json({ message: "Login successful" });
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
