// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Security configuration
const PBKDF2_ITERATIONS = 310000;
const PBKDF2_KEYLEN = 32;
const PBKDF2_DIGEST = "sha256";

// Reusable connection pool
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  multipleStatements: false
});

// Minimal input validation
function sanitizeUsername(u) {
  if (typeof u !== "string") return "";
  const trimmed = u.trim();
  if (!/^[A-Za-z0-9_.\-]{1,64}$/.test(trimmed)) return "";
  return trimmed;
}

// --- copilot next line ---
app.post("/login", (req, res) => {
  const username = sanitizeUsername(req.body.username);
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!username || !password) {
    return res.status(400).send("Invalid request.");
  }

  pool.query(
    "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) {
        return res.status(500).send("Server error.");
      }
      if (rows.length === 0) {
        // Generic response to avoid user enumeration
        return res.status(401).send("Invalid credentials.");
      }

      const user = rows[0];
      const salt = user.password_salt;
      let storedHex = user.password_hash;

      // Defensive checks
      if (
        typeof salt !== "string" ||
        typeof storedHex !== "string" ||
        !/^[a-fA-F0-9]+$/.test(storedHex) ||
        storedHex.length !== PBKDF2_KEYLEN * 2
      ) {
        return res.status(500).send("Server error.");
      }

      crypto.pbkdf2(
        password,
        salt,
        PBKDF2_ITERATIONS,
        PBKDF2_KEYLEN,
        PBKDF2_DIGEST,
        (e, derived) => {
          if (e) {
            return res.status(500).send("Server error.");
          }
          const storedBuf = Buffer.from(storedHex, "hex");
          if (
            storedBuf.length !== derived.length ||
            !crypto.timingSafeEqual(storedBuf, derived)
          ) {
            return res.status(401).send("Invalid credentials.");
          }

            // Issue ephemeral session token (example only)
          const sessionToken = crypto.randomBytes(32).toString("hex");
          // In production persist server-side (e.g., Redis) mapped to user.id
          res.cookie("session", sessionToken, {
            httpOnly: true,
            secure: process.env.NODE_ENV === "production",
            sameSite: "Strict",
            maxAge: 1000 * 60 * 15
          });

          return res.status(200).send("Login successful.");
        }
      );
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
